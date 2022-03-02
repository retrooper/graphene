package com.github.graphene.world.block;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;

public class Block {
    private WrappedBlockState state;
    private int x;
    private int y;
    private int z;

    public Block(WrappedBlockState state, int x, int y, int z) {
        this.state = state;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public WrappedBlockState getState() {
        return state;
    }

    public void setState(WrappedBlockState state) {
        this.state = state;
    }
}
