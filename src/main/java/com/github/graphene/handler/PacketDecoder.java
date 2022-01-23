package com.github.graphene.handler;

import com.github.graphene.player.Player;
import com.github.graphene.util.ServerUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufAbstract;
import com.github.retrooper.packetevents.netty.channel.ChannelHandlerContextAbstract;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.player.User;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class PacketDecoder extends ByteToMessageDecoder {
    public User user;
    public final Player player;

    public PacketDecoder(User user, Player player) {
        this.user = user;
        this.player = player;
    }

    public void handle(ChannelHandlerContextAbstract ctx, ByteBufAbstract byteBuf, List<Object> output) {
        ByteBufAbstract transformedBuf = ctx.alloc().buffer().writeBytes(byteBuf);
        try {
            int firstReaderIndex = transformedBuf.readerIndex();
            PacketReceiveEvent packetReceiveEvent = new PacketReceiveEvent(player.getState(), ctx.channel(), user, player, transformedBuf);
            int readerIndex = transformedBuf.readerIndex();
            PacketEvents.getAPI().getEventManager().callEvent(packetReceiveEvent, () -> transformedBuf.readerIndex(readerIndex));
            if (!packetReceiveEvent.isCancelled()) {
                if (packetReceiveEvent.getLastUsedWrapper() != null) {
                    packetReceiveEvent.getByteBuf().clear();
                    packetReceiveEvent.getLastUsedWrapper().writeVarInt(packetReceiveEvent.getPacketId());
                    packetReceiveEvent.getLastUsedWrapper().writeData();
                }
                transformedBuf.readerIndex(firstReaderIndex);
                output.add(transformedBuf.retain().rawByteBuf());
            }
        } finally {
            transformedBuf.release();
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) {
        if (byteBuf.readableBytes() != 0) {
            try {
                handle(PacketEvents.getAPI().getNettyManager().wrapChannelHandlerContext(ctx), PacketEvents.getAPI().getNettyManager().wrapByteBuf(byteBuf), out);
            } catch (Exception e) {
                ctx.channel().close();
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (player.getState() == ConnectionState.PLAY) {
            ServerUtil.handlePlayerLeave(player);
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
