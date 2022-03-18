package com.github.graphene.handler;

import com.github.graphene.player.Player;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.EventCreationUtil;
import com.github.retrooper.packetevents.util.PacketEventsImplHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class PacketEncoder extends MessageToByteEncoder<ByteBuf> {
    public final Player player;
    public User user;

    public PacketEncoder(User user, Player player) {
        this.user = user;
        this.player = player;
    }
    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        if (msg.isReadable()) {
            out.writeBytes(msg);
            PacketEventsImplHelper.handleClientBoundPacket(ctx.channel(), user, player, out);
        }
    }
}
