package com.github.graphene.packetevents.manager.player;

import com.github.graphene.player.Player;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.netty.buffer.ByteBufAbstract;
import com.github.retrooper.packetevents.netty.channel.ChannelAbstract;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import org.jetbrains.annotations.NotNull;

public class PlayerManagerImpl implements PlayerManager {
    @Override
    public int getPing(@NotNull Object player) {
        return (int) ((Player) player).getLatency();
    }

    //TODO See if we can still put it in the map
    @Override
    public @NotNull ClientVersion getClientVersion(@NotNull Object player) {
        return ((Player) player).getClientVersion();
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
    public void sendPacket(ChannelAbstract channel, ByteBufAbstract byteBuf) {
        if (channel.isOpen()) {
            channel.writeAndFlush(byteBuf);
        }
    }

    @Override
    public void sendPacketSilently(ChannelAbstract channel, ByteBufAbstract byteBuf) {
        if (channel.isOpen()) {
            channel.pipeline().context(PacketEvents.ENCODER_NAME).writeAndFlush(byteBuf);
        }
    }

    @Override
    public User getUser(@NotNull Object player) {
        ChannelAbstract channel = getChannel(player);
        return getUser(channel);
    }

    @Override
    public ChannelAbstract getChannel(@NotNull Object player) {
        return PacketEvents.getAPI().getNettyManager().wrapChannel(((Player) player).getChannel());
    }
}