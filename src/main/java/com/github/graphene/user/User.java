package com.github.graphene.user;

import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import io.netty.channel.Channel;

import java.util.UUID;

public class User {
    private final Channel channel;
    private String username;
    private UUID uuid;
    private ConnectionState state;
    private ClientVersion clientVersion;

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

    public void forceDisconnect() {
        channel.close();
    }

}
