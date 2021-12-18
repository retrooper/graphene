package com.github.graphene;

import com.github.graphene.handler.PacketDecoder;
import com.github.graphene.handler.PacketEncoder;
import com.github.graphene.handler.PacketPrepender;
import com.github.graphene.handler.PacketSplitter;
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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

public class Graphene {
    public static final String SERVER_VERSION_NAME = ServerVersion.getLatest().getReleaseName();
    public static final int SERVER_PROTOCOL_VERSION = ServerVersion.getLatest().getProtocolVersion();
    public static final int MAX_PLAYERS = 100;
    public static final String SERVER_DESCRIPTION = "Graphene Server";
    public static final Logger LOGGER = Logger.getLogger(Graphene.class.getSimpleName());
    //Generate 1024 bit RSA keypair
    public static final KeyPair KEY_PAIR = generateKeyPair();
    // Prefer 8 total worker threads over less.
    public static int TOTAL_THREADS = Math.max(Runtime.getRuntime().availableProcessors() * 2, 8);
    public static final ThreadPoolExecutor WORKER_THREADS = (ThreadPoolExecutor) Executors.newFixedThreadPool(TOTAL_THREADS);
    public static final int PORT = 25565;
    public static final Queue<User> USERS = new ConcurrentLinkedQueue<>();
    public static long totalTicks = 0L;
    private static long lastTickTime = 0L;

    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
        PacketEvents.setAPI(GraphenePacketEventsBuilder.build(new GraphenePacketEventsBuilder.Plugin("graphene")));
        PacketEvents.getAPI().load();
        PacketEvents.getAPI().init();
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) throws Exception {
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
            WORKER_THREADS.execute(Graphene::tick);

            Graphene.LOGGER.info("Minecraft server started on *:" + PORT + " (" + (Runtime.getRuntime().availableProcessors() * 2) + " worker threads)");

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
        long curTime = System.currentTimeMillis();
        long elapsedTime = curTime - lastTickTime;

        if (elapsedTime >= 50) {
            totalTicks += 1;
            lastTickTime = curTime;

            if (elapsedTime != 50L) System.out.println(elapsedTime + "ms");
        }

        WORKER_THREADS.execute(Graphene::tick);
    }

    public static void runKeepAlives() {
        for (User user : USERS) {
            if ((System.currentTimeMillis() - user.getKeepAliveTimer()) > 2000L) {
                long elapsedTime = System.currentTimeMillis() - user.getLastKeepAliveTime();

                if (elapsedTime > 30000L) {
                    user.kick("Timed out.");
                    Graphene.LOGGER.info(user.getUsername() + " was kicked for not responding to keep alives!");
                    break;
                }

                WrapperPlayServerKeepAlive keepAlive = new WrapperPlayServerKeepAlive((long) Math.floor(Math.random() * 2147483647));
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
            return null;
        }
    }
}
