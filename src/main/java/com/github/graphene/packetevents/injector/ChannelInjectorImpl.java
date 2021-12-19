package com.github.graphene.packetevents.injector;

import com.github.graphene.handler.PacketDecoder;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.netty.channel.ChannelAbstract;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import io.netty.channel.Channel;
import org.jetbrains.annotations.Nullable;

public class ChannelInjectorImpl implements ChannelInjector {
    @Override
    public boolean isBound() {
        return true;
    }

    @Override
    public @Nullable ConnectionState getConnectionState(ChannelAbstract channel) {
        Channel ch = (Channel) channel.rawChannel();
        PacketDecoder decoder = (PacketDecoder) ch.pipeline().get(PacketEvents.DECODER_NAME);
        return decoder.user.getState();
    }

    @Override
    public void changeConnectionState(ChannelAbstract channel, @Nullable ConnectionState packetState) {
        Channel ch = (Channel) channel.rawChannel();
        PacketDecoder decoder = (PacketDecoder) ch.pipeline().get(PacketEvents.DECODER_NAME);
        decoder.user.setState(packetState);
    }

    @Override
    public void inject() {

    }

    @Override
    public void eject() {

    }

    @Override
    public void injectPlayer(Object player, @Nullable ConnectionState connectionState) {

    }

    @Override
    public void ejectPlayer(Object player) {

    }

    @Override
    public boolean hasInjected(Object player) {
        return false;
    }
}
