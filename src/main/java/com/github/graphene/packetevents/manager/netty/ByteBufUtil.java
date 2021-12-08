package com.github.graphene.packetevents.manager.netty;

import com.github.graphene.packetevents.manager.netty.buffer.ByteBufImpl;
import com.github.retrooper.packetevents.netty.buffer.ByteBufAbstract;
import io.netty.buffer.Unpooled;

public class ByteBufUtil {
    public static ByteBufAbstract wrappedBuffer(byte[] bytes) {
        return new ByteBufImpl(Unpooled.wrappedBuffer(bytes));
    }

    public static ByteBufAbstract copiedBuffer(byte[] bytes) {
        return new ByteBufImpl(Unpooled.copiedBuffer(bytes));
    }

    public static ByteBufAbstract buffer() {
        return new ByteBufImpl(Unpooled.buffer());
    }

    public static ByteBufAbstract buffer(int initialCapacity) {
        return new ByteBufImpl(Unpooled.buffer(initialCapacity));
    }

    public static ByteBufAbstract buffer(int initialCapacity, int maxCapacity) {
        return new ByteBufImpl(Unpooled.buffer(initialCapacity, maxCapacity));
    }

    public static ByteBufAbstract directBuffer() {
        return new ByteBufImpl(Unpooled.directBuffer());
    }

    public static ByteBufAbstract directBuffer(int initialCapacity) {
        return new ByteBufImpl(Unpooled.directBuffer(initialCapacity));
    }

    public static ByteBufAbstract directBuffer(int initialCapacity, int maxCapacity) {
        return new ByteBufImpl(Unpooled.directBuffer(initialCapacity, maxCapacity));
    }

    public static ByteBufAbstract compositeBuffer() {
        return new ByteBufImpl(Unpooled.compositeBuffer());
    }

    public static ByteBufAbstract compositeBuffer(int maxNumComponents) {
        return new ByteBufImpl(Unpooled.compositeBuffer(maxNumComponents));
    }
}
