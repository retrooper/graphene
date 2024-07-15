package com.github.graphene.handler;

import com.github.graphene.player.Player;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
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
        Object buffer = PacketEventsImplHelper.handleServerBoundPacket(ctx.channel(), user, player, byteBuf, true);
        out.add(ByteBufHelper.retain(buffer));
    }
}
