package com.github.graphene.packetevents.manager.server;

import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufAbstract;
import com.github.retrooper.packetevents.netty.channel.ChannelAbstract;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

public class ServerManagerImpl implements ServerManager {
    private ServerVersion serverVersion;

    @Override
    public ServerVersion resolveVersionNoCache() {
        return ServerVersion.V_1_18;
    }

    @Override
    public ServerVersion getVersion() {
        if (serverVersion == null) {
            serverVersion = resolveVersionNoCache();
        }
        return serverVersion;
    }

    @Override
    public void receivePacket(ChannelAbstract channel, ByteBufAbstract byteBuf) {

    }

    @Override
    public void receivePacket(Object player, ByteBufAbstract byteBuf) {

    }

    @Override
    public void receivePacket(Object player, PacketWrapper<?> wrapper) {

    }

    @Override
    public void receivePacket(ChannelAbstract channel, PacketWrapper<?> wrapper) {

    }

    @Override
    public boolean isGeyserAvailable() {
        return false;
    }
}
