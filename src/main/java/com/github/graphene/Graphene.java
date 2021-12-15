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
import java.util.logging.Logger;

public class Graphene {
    public static final String SERVER_VERSION_NAME = "1.18";
    public static final int SERVER_PROTOCOL_VERSION = ServerVersion.V_1_18.getProtocolVersion();
    public static final int MAX_PLAYERS = 100;
    public static int ONLINE_PLAYERS = 5;
    public static final String SERVER_DESCRIPTION = "Graphene Server";
    public static final Logger LOGGER = Logger.getLogger(Graphene.class.getSimpleName());
    //Generate 1024 bit RSA keypair
    public static final KeyPair KEY_PAIR = generateKeyPair();

    public static final int PORT = 25565;

    public static void main(String[] args) throws Exception {
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
                        public void initChannel(SocketChannel ch) throws Exception {
                            System.out.println("Connecting!");
                            //This is called when a socket connects
                            User user = new User(ch, ConnectionState.HANDSHAKING);
                            PacketDecoder decoder = new PacketDecoder(user);
                            PacketEncoder encoder = new PacketEncoder(user);
                            ch.pipeline()
                                    //.addLast("cipher_handler", new CipherHandler())
                                    .addLast("packet_splitter", new PacketSplitter())
                                    .addLast(PacketEvents.DECODER_NAME, decoder)
                                    .addLast("packet_prepender", new PacketPrepender())
                                    .addLast(PacketEvents.ENCODER_NAME, encoder);
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

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
