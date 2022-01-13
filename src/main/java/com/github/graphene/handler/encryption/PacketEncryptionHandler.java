package com.github.graphene.handler.encryption;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.jetbrains.annotations.Nullable;

import javax.crypto.Cipher;

public class PacketEncryptionHandler extends MessageToByteEncoder<ByteBuf> {
    private final Cipher cipher;
    private byte[] heap = new byte[0];

    public PacketEncryptionHandler(@Nullable Cipher cipher) {
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
        if (cipher != null) {
            int len = msg.readableBytes();
            byte[] bytes = asBytes(msg);
            int size = cipher.getOutputSize(len);
            if (heap.length < size) {
                heap = new byte[size];
            }
            out.writeBytes(heap, 0, cipher.update(bytes, 0, len, heap));
        }
        else {
            out.writeBytes(msg);
        }
    }
}
