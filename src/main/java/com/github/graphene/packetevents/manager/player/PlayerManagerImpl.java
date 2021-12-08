package com.github.graphene.packetevents.manager.player;

import com.github.graphene.user.User;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerAttributeObject;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.netty.buffer.ByteBufAbstract;
import com.github.retrooper.packetevents.netty.channel.ChannelAbstract;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.gameprofile.WrappedGameProfile;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManagerImpl implements PlayerManager {

    @Override
    public <T extends PlayerAttributeObject> T getAttributeOrDefault(UUID uuid, Class<T> clazz, T defaultReturnValue) {
        Map<Class<? extends PlayerAttributeObject>, PlayerAttributeObject> attributes = PLAYER_ATTRIBUTES.get(uuid);
        if (attributes != null) {
            return (T) attributes.get(clazz);
        } else {
            attributes = new HashMap<>();
            attributes.put(defaultReturnValue.getClass(), defaultReturnValue);
            PLAYER_ATTRIBUTES.put(uuid, attributes);
            return defaultReturnValue;
        }
    }

    @Override
    public <T extends PlayerAttributeObject> T getAttribute(UUID uuid, Class<T> clazz) {
        Map<Class<? extends PlayerAttributeObject>, PlayerAttributeObject> attributes = PLAYER_ATTRIBUTES.get(uuid);
        if (attributes != null) {
            return (T) attributes.get(clazz);
        } else {
            PLAYER_ATTRIBUTES.put(uuid, new HashMap<>());
            return null;
        }
    }

    @Override
    public <T extends PlayerAttributeObject> void setAttribute(UUID uuid, T attribute) {
        Map<Class<? extends PlayerAttributeObject>, PlayerAttributeObject> attributes = PLAYER_ATTRIBUTES.computeIfAbsent(uuid, k -> new HashMap<>());
        attributes.put(attribute.getClass(), attribute);
    }

    @Override
    public ConnectionState getConnectionState(@NotNull Object player) {
        return getConnectionState(getChannel(player));
    }

    @Override
    public ConnectionState getConnectionState(ChannelAbstract channel) {
        ConnectionState connectionState = CONNECTION_STATES.get(channel);
        if (connectionState == null) {
            connectionState = PacketEvents.getAPI().getInjector().getConnectionState(channel);
            if (connectionState == null) {
                connectionState = ConnectionState.PLAY;
            }
            CONNECTION_STATES.put(channel, connectionState);
        }
        return connectionState;
    }

    @Override
    public void changeConnectionState(ChannelAbstract channel, ConnectionState connectionState) {
        CONNECTION_STATES.put(channel, connectionState);
        PacketEvents.getAPI().getInjector().changeConnectionState(channel, connectionState);
    }

    @Override
    public int getPing(@NotNull Object player) {
        return -1;
    }

    @Override
    public @NotNull ClientVersion getClientVersion(@NotNull Object player) {
        User user = (User) player;
        return user.getClientVersion();
    }


    @Override
    public ClientVersion getClientVersion(ChannelAbstract channel) {
        ClientVersion version = CLIENT_VERSIONS.get(channel);
        if (version == null || !version.isResolved()) {
            int protocolVersion = PacketEvents.getAPI().getServerManager().getVersion().getProtocolVersion();
            version = ClientVersion.getClientVersionByProtocolVersion(protocolVersion);
            CLIENT_VERSIONS.put(channel, version);
        }
        return version;
    }

    @Override
    public void setClientVersion(ChannelAbstract channel, ClientVersion version) {
        CLIENT_VERSIONS.put(channel, version);
    }

    @Override
    public void setClientVersion(@NotNull Object player, ClientVersion version) {
        setClientVersion(getChannel(player), version);
    }

    @Override
    public void sendPacket(ChannelAbstract channel, ByteBufAbstract byteBuf) {
        if (channel.isOpen()) {
            channel.pipeline().context(PacketEvents.ENCODER_NAME).writeAndFlush(byteBuf);
        }
    }

    @Override
    public void sendPacket(ChannelAbstract channel, PacketWrapper<?> wrapper) {
        wrapper.prepareForSend();
        sendPacket(channel, wrapper.buffer);
    }

    @Override
    public void sendPacket(@NotNull Object player, ByteBufAbstract byteBuf) {
        ChannelAbstract channel = getChannel(player);
        sendPacket(channel, byteBuf);
    }

    @Override
    public void sendPacket(@NotNull Object player, PacketWrapper<?> wrapper) {
        wrapper.prepareForSend();
        ChannelAbstract channel = getChannel(player);
        sendPacket(channel, wrapper.buffer);
    }

    @Override
    public WrappedGameProfile getGameProfile(@NotNull Object player) {
        return null;
    }

    @Override
    public boolean isGeyserPlayer(@NotNull Object player) {
        return false;
    }

    @Override
    public boolean isGeyserPlayer(UUID uuid) {
        return false;
    }

    @Override
    public ChannelAbstract getChannel(@NotNull Object player) {
        return PacketEvents.getAPI().getNettyManager().wrapChannel(((User)player).getChannel());
    }

    @Override
    public ChannelAbstract getChannel(String username) {
        return CHANNELS.get(username);
    }

    @Override
    public void setChannel(String username, ChannelAbstract channel) {
        CHANNELS.put(username, channel);
    }

    @Override
    public void setChannel(@NotNull Object player, ChannelAbstract channel) {
        setChannel(((User) player).getUsername(), channel);
    }
}