package com.github.graphene.packetevents.manager.player;

import com.github.graphene.user.User;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.netty.buffer.ByteBufAbstract;
import com.github.retrooper.packetevents.netty.channel.ChannelAbstract;
import com.github.retrooper.packetevents.protocol.gameprofile.GameProfile;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.jetbrains.annotations.NotNull;

public class PlayerManagerImpl implements PlayerManager {
    @Override
    public int getPing(@NotNull Object player) {
        return -1;
    }

    //TODO See if we can still put it in the map
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
    public void sendPacket(ChannelAbstract channel, ByteBufAbstract byteBuf) {
        if (channel.isOpen()) {
            channel.writeAndFlush(byteBuf);
        }
    }

    @Override
    public GameProfile getGameProfile(@NotNull Object player) {
        ChannelAbstract channel = PacketEvents.getAPI().getNettyManager().wrapChannel(((User) player).getChannel());
        return getGameProfile(channel);
    }

    @Override
    public ChannelAbstract getChannel(@NotNull Object player) {
        return PacketEvents.getAPI().getNettyManager().wrapChannel(((User) player).getChannel());
    }
}