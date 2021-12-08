package com.github.graphene.packetevents.manager.netty.channel;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;

public class ChannelInboundHandlerUtil {
    public static void handlerChannelRead(Object handler, Object ctx, Object msg) {
        try {
            ((ChannelInboundHandler)handler).channelRead((ChannelHandlerContext) ctx, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
