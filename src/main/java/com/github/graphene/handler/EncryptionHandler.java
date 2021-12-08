package com.github.graphene.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import javax.crypto.Cipher;
import java.util.List;

public class EncryptionHandler extends MessageToByteEncoder<ByteBuf> {
    private final Cipher cipher;
    private byte[] heap = new byte[0];

    public EncryptionHandler(Cipher cipher) {
        this.cipher = cipher;
    }

    private byte[] asBytes(ByteBuf buffer) {
        int len = buffer.readableBytes();
        if (this.heap.length < len)
            this.heap = new byte[len];
        buffer.readBytes(heap, 0, len);
        return heap;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        int len = msg.readableBytes();
        byte[] bytes = asBytes(msg);
        int size = cipher.getOutputSize(len);
        if (heap.length < size) {
            heap = new byte[size];
        }
        out.writeBytes(heap, 0, cipher.update(bytes, 0, len, heap));
    }
}
