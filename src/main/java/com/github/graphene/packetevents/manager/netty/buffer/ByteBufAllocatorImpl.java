package com.github.graphene.packetevents.manager.netty.buffer;

import com.github.retrooper.packetevents.netty.buffer.ByteBufAbstract;
import com.github.retrooper.packetevents.netty.buffer.ByteBufAllocatorAbstract;
import io.netty.buffer.ByteBufAllocator;

public class ByteBufAllocatorImpl implements ByteBufAllocatorAbstract {
    private final ByteBufAllocator byteBufAllocator;

    public ByteBufAllocatorImpl(Object rawByteBufAllocator) {
        this.byteBufAllocator = (ByteBufAllocator) rawByteBufAllocator;
    }

    @Override
    public Object rawByteBufAllocator() {
        return byteBufAllocator;
    }

    @Override
    public ByteBufAbstract buffer() {
        return new ByteBufImpl(byteBufAllocator.buffer());
    }

    @Override
    public ByteBufAbstract buffer(int initialCapacity) {
        return new ByteBufImpl(byteBufAllocator.buffer(initialCapacity));
    }

    @Override
    public ByteBufAbstract buffer(int initialCapacity, int maxCapacity) {
        return new ByteBufImpl(byteBufAllocator.buffer(initialCapacity, maxCapacity));
    }

    @Override
    public ByteBufAbstract ioBuffer() {
        return new ByteBufImpl(byteBufAllocator.ioBuffer());
    }

    @Override
    public ByteBufAbstract ioBuffer(int initialCapacity) {
        return new ByteBufImpl(byteBufAllocator.ioBuffer(initialCapacity));
    }

    @Override
    public ByteBufAbstract ioBuffer(int initialCapacity, int maxCapacity) {
        return new ByteBufImpl(byteBufAllocator.ioBuffer(initialCapacity, maxCapacity));
    }

    @Override
    public ByteBufAbstract heapBuffer() {
        return new ByteBufImpl(byteBufAllocator.heapBuffer());
    }

    @Override
    public ByteBufAbstract heapBuffer(int initialCapacity) {
        return new ByteBufImpl(byteBufAllocator.heapBuffer(initialCapacity));
    }

    @Override
    public ByteBufAbstract heapBuffer(int initialCapacity, int maxCapacity) {
        return new ByteBufImpl(byteBufAllocator.heapBuffer(initialCapacity, maxCapacity));
    }

    @Override
    public ByteBufAbstract directBuffer() {
        return new ByteBufImpl(byteBufAllocator.directBuffer());
    }

    @Override
    public ByteBufAbstract directBuffer(int initialCapacity) {
        return new ByteBufImpl(byteBufAllocator.directBuffer(initialCapacity));
    }

    @Override
    public ByteBufAbstract directBuffer(int initialCapacity, int maxCapacity) {
        return new ByteBufImpl(byteBufAllocator.directBuffer(initialCapacity, maxCapacity));
    }

    @Override
    public ByteBufAbstract compositeBuffer() {
        return new ByteBufImpl(byteBufAllocator.compositeBuffer());
    }

    @Override
    public ByteBufAbstract compositeBuffer(int maxNumComponents) {
        return new ByteBufImpl(byteBufAllocator.compositeBuffer(maxNumComponents));
    }

    @Override
    public ByteBufAbstract compositeHeapBuffer() {
        return new ByteBufImpl(byteBufAllocator.compositeHeapBuffer());
    }

    @Override
    public ByteBufAbstract compositeHeapBuffer(int maxNumComponents) {
        return new ByteBufImpl(byteBufAllocator.compositeHeapBuffer(maxNumComponents));
    }

    @Override
    public ByteBufAbstract compositeDirectBuffer() {
        return new ByteBufImpl(byteBufAllocator.compositeDirectBuffer());
    }

    @Override
    public ByteBufAbstract compositeDirectBuffer(int maxNumComponents) {
        return new ByteBufImpl(byteBufAllocator.compositeDirectBuffer(maxNumComponents));
    }

    @Override
    public boolean isDirectBufferPooled() {
        return byteBufAllocator.isDirectBufferPooled();
    }
}
