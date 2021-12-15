package com.github.graphene.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class PacketPrepender extends MessageToByteEncoder<ByteBuf> {
    public int getVarIntSize(int len) {
        for (int i = 1; i < 5; i++) {
            if ((len & -1 << i * 7) == 0)
                return i;
        }
        return 5;
    }

    public void writeVarInt(ByteBuf buffer, int value) {
        while ((value & -128) != 0) {
            buffer.writeByte(value & 127 | 128);
            value >>>= 7;
        }

        buffer.writeByte(value);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        //Prefix the packet with its length
        //Structure:
        /*
         * Length = VarInt
         * ID = VarInt
         * Data = Bytes
         */
        int length = msg.readableBytes();
        int varIntSize = getVarIntSize(length);
        if (varIntSize > 3) {
            throw new IllegalStateException("Something went wrong in the prepender!");
        }
        out.ensureWritable(varIntSize + length);
        writeVarInt(out, length);
        out.writeBytes(msg, msg.readerIndex(), length);
    }
}
