package com.github.graphene;

import com.github.graphene.entity.ItemEntity;
import com.github.graphene.handler.PacketDecoder;
import com.github.graphene.handler.PacketEncoder;
import com.github.graphene.handler.PacketPrepender;
import com.github.graphene.handler.PacketSplitter;
import com.github.graphene.injector.ChannelInjectorImpl;
import com.github.graphene.listener.*;
import com.github.graphene.player.Player;
import com.github.graphene.util.ChunkUtil;
import com.github.graphene.util.Vector2i;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.channel.ChannelAbstract;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import io.github.retrooper.packetevents.factory.netty.BuildData;
import io.github.retrooper.packetevents.factory.netty.NettyPacketEventsBuilder;
import io.github.retrooper.packetevents.manager.player.PlayerManagerImpl;
import io.github.retrooper.packetevents.manager.server.ServerManagerImpl;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.jetbrains.annotations.NotNull;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Main {
    public static boolean shouldTick = true;
    public static volatile boolean shouldRunKeepAliveLoop = true;
    public static String SERVER_VERSION_NAME;
    public static int SERVER_PROTOCOL_VERSION;
    public static final int MAX_PLAYERS = 1000;
    public static final String SERVER_DESCRIPTION = "Graphene Server";
    public static final Logger LOGGER = Logger.getLogger(Main.class.getSimpleName());
    //Generate 1024 bit RSA keypair
    public static final KeyPair KEY_PAIR = generateKeyPair();
    public static final ExecutorService WORKER_THREADS = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    public static final int PORT = 25999;
    public static final Queue<Player> PLAYERS = new ConcurrentLinkedQueue<>();
    public static long totalTicks = 0L;
    public static boolean ONLINE_MODE = true;
    public static int ENTITIES = 0;
    public static final Queue<ItemEntity> ITEM_ENTITIES = new ConcurrentLinkedQueue<>();
    public static final Map<Vector2i, Column> CHUNKS = new ConcurrentHashMap<>();


    //Need to store items players have in hand;
    //Entity equipment packets need to be sent showing what items users have in hand
    //Store world blocks
    //Store last entity positions
    public static void main(String[] args) throws Exception {
        BuildData data = new BuildData("graphene");
        ChannelInjector injector = new ChannelInjectorImpl();

        ServerManagerImpl serverManager = new ServerManagerImpl() {
            @Override
            public ServerVersion getVersion() {
                return ServerVersion.getLatest();
            }
        };

        PlayerManagerImpl playerManager = new PlayerManagerImpl() {
            @Override
            public int getPing(@NotNull Object player) {
                return (int) ((Player) player).getLatency();
            }

            @Override
            public ChannelAbstract getChannel(@NotNull Object player) {
                return PacketEvents.getAPI().getNettyManager().wrapChannel(((Player) player).getChannel());
            }
        };

        PacketEvents.setAPI(NettyPacketEventsBuilder.build(data, injector,
                serverManager, playerManager));
        PacketEvents.getAPI().getSettings().debug(true).bStats(true);
        PacketEvents.getAPI().load();
        PacketEvents.getAPI().getEventManager()
                .registerListener(new ServerListPingListener(), PacketListenerPriority.LOWEST, true);
        PacketEvents.getAPI().getEventManager()
                .registerListener(new LoginListener(ONLINE_MODE), PacketListenerPriority.LOWEST, true);
        PacketEvents.getAPI().getEventManager()
                .registerListener(new KeepAliveListener(), PacketListenerPriority.LOWEST, true);
        PacketEvents.getAPI().getEventManager()
                .registerListener(new EntityHandler(), PacketListenerPriority.LOWEST, false);
        PacketEvents.getAPI().getEventManager()
                .registerListener(new InputListener(), PacketListenerPriority.LOWEST, true);
        PacketEvents.getAPI().init();
        SERVER_VERSION_NAME = PacketEvents.getAPI().getServerManager().getVersion().getReleaseName();
        SERVER_PROTOCOL_VERSION = PacketEvents.getAPI().getServerManager().getVersion().getProtocolVersion();
        Main.LOGGER.info("Starting Graphene Server. Version: " + SERVER_VERSION_NAME + ". Online mode: " + ONLINE_MODE);

        Main.LOGGER.info("Preparing chunks...");
        ChunkUtil.generateChunkColumns(1, 1, true);
        Main.LOGGER.info("Binding to port... " + PORT);
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
                            ChannelAbstract ch = PacketEvents.getAPI().getNettyManager().wrapChannel(channel);
                            User user = new User(ch, ConnectionState.HANDSHAKING, null, new UserProfile(null, null));
                            PacketEvents.getAPI().getPlayerManager().USERS.put(ch, user);
                            Player player = new Player(user);
                            PacketDecoder decoder = new PacketDecoder(user, player);
                            PacketEncoder encoder = new PacketEncoder(user, player);
                            channel.pipeline()
                                    .addLast("decryption_handler", new ChannelHandlerAdapter() {
                                    })
                                    .addLast("packet_splitter", new PacketSplitter())
                                    .addLast(PacketEvents.DECODER_NAME, decoder)
                                    .addLast("encryption_handler", new ChannelHandlerAdapter() {
                                    })
                                    .addLast("packet_prepender", new PacketPrepender())
                                    .addLast(PacketEvents.ENCODER_NAME, encoder);
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            WORKER_THREADS.execute(Main::runKeepAliveLoop);

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(PORT).sync();
            Main.LOGGER.info("(" + (Runtime.getRuntime().availableProcessors()) + " worker threads)");

            Main.runTickLoop();

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

    public static void runTickLoop() {
        //1000ms / 50ms = 20 ticks per second
        while (shouldTick) {
            totalTicks += 1;
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            EntityHandler.onTick();
        }
    }

    public static void runKeepAliveLoop() {
        while (shouldRunKeepAliveLoop) {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (Player player : PLAYERS) {
                if ((System.currentTimeMillis() - player.getKeepAliveTimer()) > 3000L) {
                    long elapsedTime = System.currentTimeMillis() - player.getLastKeepAliveTime();

                    if (elapsedTime > 30000L) {
                        player.kick("Timed out.");
                        Main.LOGGER.info(player.getUsername() + " was kicked for not responding to keep alives!");
                        break;
                    }

                    WrapperPlayServerKeepAlive keepAlive = new WrapperPlayServerKeepAlive((long) Math.floor(Math.random() * Integer.MAX_VALUE));
                    player.sendPacket(keepAlive);

                    player.setKeepAliveTimer(System.currentTimeMillis());
                    player.setSendKeepAliveTime(System.currentTimeMillis());
                }
            }
        }
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
            Main.LOGGER.severe("Failed to generate RSA-1024 key, cannot start server!");
            System.exit(2);
            return null;
        }
    }
}
