package com.github.graphene.packetevents.manager.netty.channel;

import com.github.graphene.packetevents.manager.netty.channel.pipeline.ChannelPipelineImpl;
import com.github.retrooper.packetevents.netty.buffer.ByteBufAbstract;
import com.github.retrooper.packetevents.netty.channel.ChannelAbstract;
import com.github.retrooper.packetevents.netty.channel.pipeline.ChannelPipelineAbstract;
import io.netty.channel.Channel;

import java.net.SocketAddress;


public class ChannelImpl implements ChannelAbstract {
    private final Channel channel;

    public ChannelImpl(Object rawChannel) {
        this.channel = (Channel) rawChannel;
    }

    @Override
    public Object rawChannel() {
        return channel;
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public boolean isRegistered() {
        return channel.isRegistered();
    }

    @Override
    public boolean isActive() {
        return channel.isActive();
    }

    @Override
    public SocketAddress localAddress() {
        return channel.localAddress();
    }

    @Override
    public SocketAddress remoteAddress() {
        return channel.remoteAddress();
    }

    @Override
    public boolean isWritable() {
        return channel.isWritable();
    }

    @Override
    public ChannelPipelineAbstract pipeline() {
        return new ChannelPipelineImpl(channel.pipeline());
    }

    @Override
    public void write(Object msg) {
        if (msg instanceof ByteBufAbstract) {
            msg = ((ByteBufAbstract) msg).rawByteBuf();
        }
        channel.write(msg);
    }

    @Override
    public void writeAndFlush(Object msg) {
        if (msg instanceof ByteBufAbstract) {
            msg = ((ByteBufAbstract) msg).rawByteBuf();
        }
        channel.writeAndFlush(msg);
    }

    @Override
    public ChannelAbstract flush() {
        return new ChannelImpl(channel.flush());
    }
}
