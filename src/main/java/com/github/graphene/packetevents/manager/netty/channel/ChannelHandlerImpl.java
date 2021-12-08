package com.github.graphene.packetevents.manager.netty.channel;

import com.github.retrooper.packetevents.netty.channel.ChannelHandlerAbstract;
import com.github.retrooper.packetevents.netty.channel.ChannelHandlerContextAbstract;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

public class ChannelHandlerImpl implements ChannelHandlerAbstract {
    private final ChannelHandler channelHandler;

    public ChannelHandlerImpl(Object rawChannelHandler) {
        this.channelHandler = (ChannelHandler) rawChannelHandler;
    }

    @Override
    public Object rawChannelHandler() {
        return channelHandler;
    }

    @Override
    public void handlerAdded(ChannelHandlerContextAbstract ctx) throws Exception {
        channelHandler.handlerAdded((ChannelHandlerContext) ctx.rawChannelHandlerContext());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContextAbstract ctx) throws Exception {
        channelHandler.handlerRemoved((ChannelHandlerContext) ctx.rawChannelHandlerContext());
    }
}
