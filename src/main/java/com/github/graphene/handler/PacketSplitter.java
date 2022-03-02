package com.github.graphene.handler;

import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

import java.util.List;

public class PacketSplitter extends ByteToMessageDecoder {
    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) {
        byteBuf.markReaderIndex();
        byte[] bytes = new byte[3];
        for (int i = 0; i < bytes.length; i++) {
            if (!byteBuf.isReadable()) {
                byteBuf.resetReaderIndex();
                return;
            }
            bytes[i] = byteBuf.readByte();
            if (bytes[i] >= 0) {
                ByteBuf newBB = Unpooled.wrappedBuffer(bytes);
                try {
                    int len = ByteBufHelper.readVarInt(newBB);
                    if (byteBuf.readableBytes() < len) {
                        byteBuf.resetReaderIndex();
                        return;
                    }
                    out.add(byteBuf.readBytes(len));
                    return;
                } finally {
                    newBB.release();
                }
            }
        }
        throw new CorruptedFrameException("length wider than 21-bit");
    }
}
