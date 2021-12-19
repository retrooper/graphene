package com.github.graphene.handler.compression;

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
        int dataLength = readVarInt(msg);
        ByteBuf uncompressedDataBuf = msg.readBytes(msg.readableBytes() - msg.readerIndex());
        byte[] uncompressedData = new byte[uncompressedDataBuf.readableBytes()];
        uncompressedDataBuf.readBytes(uncompressedData);
        byte[] bytes = new byte[65535];
        Deflater compressor = new Deflater();
        compressor.setInput(uncompressedData);
        compressor.finish();
        int compressedLength = compressor.deflate(bytes);
        compressor.end();

        writeVarInt(out, dataLength);
        int packetLength = out.readableBytes() + bytes.length;
        out.clear();
        writeVarInt(out, packetLength);
        writeVarInt(out, dataLength);
        out.writeBytes(bytes);
    }

    private void writeVarInt(ByteBuf buffer, int value) {
        while ((value & -128) != 0) {
            buffer.writeByte(value & 127 | 128);
            value >>>= 7;
        }

        buffer.writeByte(value);
    }

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

}
