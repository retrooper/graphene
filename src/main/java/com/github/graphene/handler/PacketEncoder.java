package com.github.graphene.handler;

import com.github.graphene.user.User;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.impl.PacketSendEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufAbstract;
import com.github.retrooper.packetevents.netty.channel.ChannelHandlerContextAbstract;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class PacketEncoder extends MessageToByteEncoder<ByteBuf> {
    public final User user;

    public PacketEncoder(User user) {
        this.user = user;
        //the packet decryptor is excellent
    }

    public void handle(ChannelHandlerContextAbstract ctx, ByteBufAbstract transformedBuf, ByteBuf output) {
        try {
            int firstReaderIndex = transformedBuf.readerIndex();
            PacketSendEvent packetSendEvent = new PacketSendEvent(ctx.channel(), user, transformedBuf);
            int readerIndex = transformedBuf.readerIndex();
            PacketEvents.getAPI().getEventManager().callEvent(packetSendEvent, () -> {
                transformedBuf.readerIndex(readerIndex);
            });
            if (!packetSendEvent.isCancelled()) {
                if (packetSendEvent.getLastUsedWrapper() != null) {
                    packetSendEvent.getByteBuf().clear();
                    packetSendEvent.getLastUsedWrapper().writeVarInt(packetSendEvent.getPacketId());
                    packetSendEvent.getLastUsedWrapper().writeData();
                }
                transformedBuf.readerIndex(firstReaderIndex);
                output.writeBytes((ByteBuf) transformedBuf.retain().rawByteBuf());
                if (packetSendEvent.getPostTask() != null) {
                    ((ChannelHandlerContext) ctx.rawChannelHandlerContext()).newPromise().addListener(f -> {
                        packetSendEvent.getPostTask().run();
                    });
                }
            }
        } finally {
            transformedBuf.release();
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        handle(PacketEvents.getAPI().getNettyManager().wrapChannelHandlerContext(ctx),
                PacketEvents.getAPI().getNettyManager().wrapByteBuf(msg), out);
    }
}
