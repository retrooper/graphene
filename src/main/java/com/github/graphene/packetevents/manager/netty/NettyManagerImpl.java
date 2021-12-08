package com.github.graphene.packetevents.manager.netty;

import com.github.graphene.packetevents.manager.netty.buffer.ByteBufImpl;
import com.github.graphene.packetevents.manager.netty.channel.ChannelHandlerContextImpl;
import com.github.graphene.packetevents.manager.netty.channel.ChannelImpl;
import com.github.retrooper.packetevents.netty.NettyManager;
import com.github.retrooper.packetevents.netty.buffer.ByteBufAbstract;
import com.github.retrooper.packetevents.netty.channel.ChannelAbstract;
import com.github.retrooper.packetevents.netty.channel.ChannelHandlerContextAbstract;

public class NettyManagerImpl implements NettyManager {
    @Override
    public ByteBufAbstract wrappedBuffer(byte[] bytes) {
        return ByteBufUtil.wrappedBuffer(bytes);
    }

    @Override
    public ByteBufAbstract copiedBuffer(byte[] bytes) {
        return ByteBufUtil.copiedBuffer(bytes);
    }

    @Override
    public ByteBufAbstract buffer() {
        return ByteBufUtil.buffer();
    }

    @Override
    public ByteBufAbstract buffer(int initialCapacity) {
        return ByteBufUtil.buffer(initialCapacity);
    }

    @Override
    public ByteBufAbstract buffer(int initialCapacity, int maxCapacity) {
        return ByteBufUtil.buffer(initialCapacity, maxCapacity);
    }

    @Override
    public ByteBufAbstract directBuffer() {
        return ByteBufUtil.directBuffer();
    }

    @Override
    public ByteBufAbstract directBuffer(int initialCapacity) {
        return ByteBufUtil.directBuffer(initialCapacity);
    }

    @Override
    public ByteBufAbstract directBuffer(int initialCapacity, int maxCapacity) {
        return ByteBufUtil.directBuffer(initialCapacity, maxCapacity);
    }

    @Override
    public ByteBufAbstract compositeBuffer() {
        return ByteBufUtil.compositeBuffer();
    }

    @Override
    public ByteBufAbstract compositeBuffer(int maxNumComponents) {
        return ByteBufUtil.compositeBuffer(maxNumComponents);
    }

    @Override
    public ByteBufAbstract wrapByteBuf(Object byteBuf) {
        return new ByteBufImpl(byteBuf);
    }

    @Override
    public ChannelAbstract wrapChannel(Object channel) {
        return new ChannelImpl(channel);
    }

    @Override
    public ChannelHandlerContextAbstract wrapChannelHandlerContext(Object ctx) {
        return new ChannelHandlerContextImpl(ctx);
    }
}
