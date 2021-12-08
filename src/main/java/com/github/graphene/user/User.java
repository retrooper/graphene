package com.github.graphene.user;

import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import io.netty.channel.Channel;

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

    public void forceDisconnect() {
        channel.close();
    }

}
