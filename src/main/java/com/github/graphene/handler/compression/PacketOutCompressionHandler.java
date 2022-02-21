package com.github.graphene.handler.compression;

import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.zip.Deflater;

public class PacketOutCompressionHandler extends MessageToByteEncoder<ByteBuf> {
    private int compressionThreshold;

    public PacketOutCompressionHandler(int compressionThreshold) {
        this.compressionThreshold = compressionThreshold;
    }

    public int getCompressionThreshold() {
        return compressionThreshold;
    }

    public void setCompressionThreshold(int compressionThreshold) {
        this.compressionThreshold = compressionThreshold;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        msg.resetReaderIndex().resetWriterIndex();
        int dataLength = ByteBufHelper.readVarInt(msg);
        ByteBuf uncompressedDataBuf = msg.readBytes(msg.readableBytes() - msg.readerIndex());
        byte[] uncompressedData = new byte[uncompressedDataBuf.readableBytes()];
        uncompressedDataBuf.readBytes(uncompressedData);
        byte[] bytes = new byte[65535];
        Deflater compressor = new Deflater();
        compressor.setInput(uncompressedData);
        compressor.finish();
        int compressedLength = compressor.deflate(bytes);
        compressor.end();

        ByteBufHelper.writeVarInt(out, dataLength);
        int packetLength = out.readableBytes() + bytes.length;
        out.clear();
        ByteBufHelper.writeVarInt(out, packetLength);
        ByteBufHelper.writeVarInt(out, dataLength);
        out.writeBytes(bytes);
    }
}
