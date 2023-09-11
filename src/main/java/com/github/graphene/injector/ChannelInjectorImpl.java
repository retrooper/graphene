package com.github.graphene.injector;

import com.github.graphene.handler.PacketDecoder;
import com.github.graphene.handler.PacketEncoder;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.player.User;
import io.netty.channel.Channel;
import org.jetbrains.annotations.Nullable;

public class ChannelInjectorImpl implements ChannelInjector {

    @Override
    public boolean isProxy() {
        return false;
    }

    @Override
    public boolean isServerBound() {
        return true;
    }

    @Override
    public void inject() {

    }

    @Override
    public void uninject() {

    }

    @Override
    public void updateUser(Object channel, User user) {
        Channel ch = (Channel) channel;
        PacketDecoder decoder = (PacketDecoder) ch.pipeline().get(PacketEvents.DECODER_NAME);
        decoder.user = user;
        PacketEncoder encoder = (PacketEncoder) ch.pipeline().get(PacketEvents.ENCODER_NAME);
        encoder.user = user;
    }

    @Override
    public void setPlayer(Object o, Object o1) {

    }
}
