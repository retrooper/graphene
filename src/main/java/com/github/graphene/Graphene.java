package com.github.graphene;

import com.github.graphene.handler.PacketDecoder;
import com.github.graphene.handler.PacketEncoder;
import com.github.graphene.handler.PacketPrepender;
import com.github.graphene.handler.PacketSplitter;
import com.github.graphene.logic.EntityHandler;
import com.github.graphene.packetevents.GraphenePacketEventsBuilder;
import com.github.graphene.user.User;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.jetbrains.annotations.NotNull;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

public class Graphene {
    public static volatile boolean shouldTick = true;
    public static String SERVER_VERSION_NAME;
    public static int SERVER_PROTOCOL_VERSION;
    public static final int MAX_PLAYERS = 100;
    public static final String SERVER_DESCRIPTION = "Graphene Server";
    public static final Logger LOGGER = Logger.getLogger(Graphene.class.getSimpleName());
    //Generate 1024 bit RSA keypair
    public static final KeyPair KEY_PAIR = generateKeyPair();
    public static int TOTAL_THREADS = (Runtime.getRuntime().availableProcessors() * 2) - 1;
    public static final ThreadPoolExecutor WORKER_THREADS = (ThreadPoolExecutor) Executors.newFixedThreadPool(TOTAL_THREADS);
    public static final ThreadPoolExecutor DEDICATED_TICK_THREAD = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    public static final int PORT = 25565;
    public static final Queue<User> USERS = new ConcurrentLinkedQueue<>();
    public static long totalTicks = 0L;
    private static long lastTickTime = 0L;

    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
        assert KEY_PAIR != null;
        PacketEvents.setAPI(GraphenePacketEventsBuilder.build(new GraphenePacketEventsBuilder.Plugin("graphene")));
        PacketEvents.getAPI().load();
        PacketEvents.getAPI().init();
        SERVER_VERSION_NAME = PacketEvents.getAPI().getServerManager().getVersion().getReleaseName();
        SERVER_PROTOCOL_VERSION = PacketEvents.getAPI().getServerManager().getVersion().getProtocolVersion();
        Graphene.LOGGER.info("Starting Graphene server " + SERVER_VERSION_NAME);
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @SuppressWarnings("RedundantThrows")
                        @Override
                        public void initChannel(@NotNull SocketChannel channel) throws Exception {
                            User user = new User(channel, ConnectionState.HANDSHAKING);
                            PacketDecoder decoder = new PacketDecoder(user);
                            PacketEncoder encoder = new PacketEncoder(user);
                            channel.pipeline()
                                    .addLast("packet_splitter", new PacketSplitter())
                                    .addLast(PacketEvents.DECODER_NAME, decoder)
                                    .addLast("packet_prepender", new PacketPrepender())
                                    .addLast(PacketEvents.ENCODER_NAME, encoder);
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            Graphene.LOGGER.info("Starting KeepAliveScheduler on worker threads...");
            WORKER_THREADS.execute(Graphene::runKeepAlives);

            Graphene.LOGGER.info("Starting tick system...");
            DEDICATED_TICK_THREAD.execute(Graphene::tick);

            Graphene.LOGGER.info("Server started on *:" + PORT + " (" + (Runtime.getRuntime().availableProcessors() * 2) + " worker threads)");

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(PORT).sync();

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
        PacketEvents.getAPI().terminate();
    }

    public static void tick() {
        while (shouldTick) {
            long curTime = System.currentTimeMillis();
            long elapsedTime = curTime - lastTickTime;

            if (elapsedTime >= 50L) {
                totalTicks += 1;
                lastTickTime = curTime;

                EntityHandler.onTick();
            }
        }
    }

    public static void runKeepAlives() {
        for (User user : USERS) {
            if ((System.currentTimeMillis() - user.getKeepAliveTimer()) > 3000L) {
                long elapsedTime = System.currentTimeMillis() - user.getLastKeepAliveTime();

                if (elapsedTime > 30000L) {
                    user.kick("Timed out.");
                    Graphene.LOGGER.info(user.getUsername() + " was kicked for not responding to keep alives!");
                    break;
                }

                WrapperPlayServerKeepAlive keepAlive = new WrapperPlayServerKeepAlive((long) Math.floor(Math.random() * Integer.MAX_VALUE));
                PacketEvents.getAPI().getPlayerManager().sendPacket(user, keepAlive);

                user.setKeepAliveTimer(System.currentTimeMillis());
                user.setSendKeepAliveTime(System.currentTimeMillis());
            }
        }

        WORKER_THREADS.execute(Graphene::runKeepAlives);
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            // If an error is thrown then shutdown because we
            // literally can't start the server without it, also
            // stops IntelliJ from asking to assert not null on keys.
            System.out.println("Failed to generate RSA-1024 key, cannot start server!");
            System.exit(2);
            return null;
        }
    }
}
