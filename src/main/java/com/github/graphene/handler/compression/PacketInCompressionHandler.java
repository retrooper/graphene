package com.github.graphene.handler.compression;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;
import java.util.zip.Deflater;

public class PacketInCompressionHandler extends MessageToMessageDecoder<ByteBuf> {
    private int compressionThreshold;

    public PacketInCompressionHandler(int compressionThreshold) {
        this.compressionThreshold = compressionThreshold;
    }

    public int getCompressionThreshold() {
        return compressionThreshold;
    }

    public void setCompressionThreshold(int compressionThreshold) {
        this.compressionThreshold = compressionThreshold;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        
    }
    
}
