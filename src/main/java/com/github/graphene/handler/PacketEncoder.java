package com.github.graphene.handler;

import com.github.graphene.player.Player;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.impl.PacketSendEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufAbstract;
import com.github.retrooper.packetevents.netty.channel.ChannelHandlerContextAbstract;
import com.github.retrooper.packetevents.protocol.player.User;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("RedundantThrows")
public class PacketEncoder extends MessageToByteEncoder<ByteBuf> {
    public final Player player;
    public User user;
    private List<Runnable> postTasks = new ArrayList<>();

    public PacketEncoder(User user, Player player) {
        this.user = user;
        this.player = player;
    }

    public void handle(ChannelHandlerContextAbstract ctx, ByteBufAbstract byteBuf) {
        int firstReaderIndex = byteBuf.readerIndex();
        PacketSendEvent packetSendEvent = new PacketSendEvent(ctx.channel(), user, player, byteBuf);
        int readerIndex = byteBuf.readerIndex();
        PacketEvents.getAPI().getEventManager().callEvent(packetSendEvent, () -> byteBuf.readerIndex(readerIndex));
        if (!packetSendEvent.isCancelled()) {
            if (packetSendEvent.getLastUsedWrapper() != null) {
                packetSendEvent.getByteBuf().clear();
                packetSendEvent.getLastUsedWrapper().writeVarInt(packetSendEvent.getPacketId());
                packetSendEvent.getLastUsedWrapper().writeData();
            }
            byteBuf.readerIndex(firstReaderIndex);
            postTasks.addAll(packetSendEvent.getPostTasks());
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
        if (!msg.isReadable())return;
        out.writeBytes(msg);
        handle(PacketEvents.getAPI().getNettyManager().wrapChannelHandlerContext(ctx),
                PacketEvents.getAPI().getNettyManager().wrapByteBuf(out));
    }
}
