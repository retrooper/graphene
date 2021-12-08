package com.github.graphene.handler;

import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufAbstract;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

import java.util.List;

public class PacketSplitter extends ByteToMessageDecoder {
    private static int readVarInt(ByteBuf byteBuf) {
        byte b0;
        int i = 0;
        int j = 0;
        do {
            b0 = byteBuf.readByte();
            i |= (b0 & Byte.MAX_VALUE) << j++ * 7;
            if (j > 5)
                throw new RuntimeException("VarInt too big");
        } while ((b0 & 128) == 128);
        return i;
    }

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
                    int len = readVarInt(newBB);
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
