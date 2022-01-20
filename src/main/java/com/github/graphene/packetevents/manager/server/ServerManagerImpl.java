package com.github.graphene.packetevents.manager.server;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufAbstract;
import com.github.retrooper.packetevents.netty.channel.ChannelAbstract;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

public class ServerManagerImpl implements ServerManager {

    @Override
    public ServerVersion getVersion() {
        return ServerVersion.getLatest();
    }


    @Override
    public void receivePacket(ChannelAbstract channel, ByteBufAbstract byteBuf) {
        //TODO impl
    }

    @Override
    public void receivePacketSilently(ChannelAbstract channel, ByteBufAbstract byteBuf) {
        channel.pipeline().context(PacketEvents.DECODER_NAME).fireChannelRead(byteBuf);
    }
}
