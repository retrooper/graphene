package com.github.graphene.user;

import com.github.graphene.user.textures.TextureProperty;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.chat.component.impl.TextComponent;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerDisconnect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class User {
    public static int ENTITY_COUNT = 0;
    private final Channel channel;
    private String username;
    private UUID uuid;
    private ConnectionState state;
    private ClientVersion clientVersion;
    private final int entityID = ENTITY_COUNT++;
    private GameMode gameMode = GameMode.SURVIVAL;
    private GameMode previousGameMode = null;
    private String serverID = "";
    private byte[] verifyToken;
    private String serverAddress;
    private List<TextureProperty> textureProperties = new ArrayList<>();

    public User(Channel channel, ConnectionState state) {
        this.channel = channel;
        this.state = state;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public ConnectionState getState() {
        return state;
    }

    public void setState(ConnectionState state) {
        this.state = state;
    }

    public ClientVersion getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(ClientVersion clientVersion) {
        this.clientVersion = clientVersion;
    }

    public int getEntityId() {
        return entityID;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public GameMode getPreviousGameMode() {
        return previousGameMode;
    }

    public void setPreviousGameMode(GameMode previousGameMode) {
        this.previousGameMode = previousGameMode;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public String getServerId() {
        return serverID;
    }

    public void setServerId(String serverID) {
        this.serverID = serverID;
    }

    public byte[] getVerifyToken() {
        return verifyToken;
    }

    public void setVerifyToken(byte[] verifyToken) {
        this.verifyToken = verifyToken;
    }

    public List<TextureProperty> getTextureProperties() {
        return textureProperties;
    }

    public InetSocketAddress getAddress() {
        return (InetSocketAddress) channel.remoteAddress();
    }
    
    public void forceDisconnect() {
        channel.close();
    }

    public void kickLogin(String reason) {
        TextComponent KickMessageBuilder = new TextComponent();
        KickMessageBuilder.setText(reason);

        WrapperLoginServerDisconnect disconnect = new WrapperLoginServerDisconnect(KickMessageBuilder.buildJson().toString());

        PacketEvents.getAPI().getPlayerManager().sendPacket(getChannel(), disconnect);
    }

    public void kick(String reason) {
        // add translation components PLEASE
        TextComponent KickMessageBuilder = new TextComponent();
        KickMessageBuilder.setText(reason);

        WrapperPlayServerDisconnect disconnect = new WrapperPlayServerDisconnect(KickMessageBuilder.buildJson().toString());

        PacketEvents.getAPI().getPlayerManager().sendPacket(getChannel(), disconnect);
    }

}
