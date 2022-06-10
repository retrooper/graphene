package com.github.graphene.handler;

import com.github.graphene.player.Player;
import com.github.graphene.util.ServerUtil;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.PacketEventsImplHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;

public class PacketDecoder extends MessageToMessageDecoder<ByteBuf> {
    public User user;
    public final Player player;

    public PacketDecoder(User user, Player player) {
        this.user = user;
        this.player = player;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
        if (byteBuf.isReadable()) {
            ByteBuf outputBuffer = ctx.alloc().buffer().writeBytes(byteBuf);
            try {
                PacketEventsImplHelper.handleServerBoundPacket(ctx.channel(), user, player, outputBuffer, true);
                if (outputBuffer.isReadable()) {
                    out.add(outputBuffer.retain());
                }
            }
            finally {
                outputBuffer.release();
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ServerUtil.handlePlayerQuit(user, player);
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
