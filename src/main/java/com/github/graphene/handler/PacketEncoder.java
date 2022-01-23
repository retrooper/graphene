package com.github.graphene.handler;

import com.github.graphene.user.User;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.impl.PacketSendEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufAbstract;
import com.github.retrooper.packetevents.netty.channel.ChannelHandlerContextAbstract;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("RedundantThrows")
public class PacketEncoder extends MessageToByteEncoder<ByteBuf> {
    public final User user;
    private List<Runnable> postTasks = new ArrayList<>();

    public PacketEncoder(User user) {
        this.user = user;
    }

    public void handle(ChannelHandlerContextAbstract ctx, ByteBufAbstract transformedBuf, ByteBuf output) {
        try {
            int firstReaderIndex = transformedBuf.readerIndex();
            PacketSendEvent packetSendEvent = new PacketSendEvent(ctx.channel(), user, transformedBuf);
            int readerIndex = transformedBuf.readerIndex();
            PacketEvents.getAPI().getEventManager().callEvent(packetSendEvent, () -> transformedBuf.readerIndex(readerIndex));
            if (!packetSendEvent.isCancelled()) {
                if (packetSendEvent.getLastUsedWrapper() != null) {
                    packetSendEvent.getByteBuf().clear();
                    packetSendEvent.getLastUsedWrapper().writeVarInt(packetSendEvent.getPacketId());
                    packetSendEvent.getLastUsedWrapper().writeData();
                }
                transformedBuf.readerIndex(firstReaderIndex);
                output.writeBytes((ByteBuf) transformedBuf.retain().rawByteBuf());
                postTasks.addAll(packetSendEvent.getPostTasks());
            }
        } finally {
            transformedBuf.release();
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!postTasks.isEmpty()) {
            List<Runnable> postTasks = new ArrayList<>(this.postTasks);
            this.postTasks.clear();
            promise.addListener(f -> {
                for (Runnable task : postTasks) {
                    task.run();
                }
            });
        }
        super.write(ctx, msg, promise);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        handle(PacketEvents.getAPI().getNettyManager().wrapChannelHandlerContext(ctx),
                PacketEvents.getAPI().getNettyManager().wrapByteBuf(msg), out);
    }
}
