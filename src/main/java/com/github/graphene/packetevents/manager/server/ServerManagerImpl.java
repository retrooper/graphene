package com.github.graphene.packetevents.manager.server;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufAbstract;
import com.github.retrooper.packetevents.netty.channel.ChannelAbstract;

public class ServerManagerImpl implements ServerManager {

    @Override
    public ServerVersion getVersion() {
        //TODO Test 1.9.4 and see if chunk data works
        return ServerVersion.getLatest();
    }


    @Override
    public void receivePacket(ChannelAbstract channel, ByteBufAbstract byteBuf) {
        //TODO impl?
        if (channel.isOpen()) {
            channel.pipeline().fireChannelRead(byteBuf);
        }
    }

    @Override
    public void receivePacketSilently(ChannelAbstract channel, ByteBufAbstract byteBuf) {
        if (channel.isOpen()) {
            channel.pipeline().context(PacketEvents.DECODER_NAME).fireChannelRead(byteBuf);
        }
    }
}
