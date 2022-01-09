package com.github.graphene.block;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;

public class Block {
    private WrappedBlockState state;
    private Vector3i blockPosition;

    public Block(WrappedBlockState state, Vector3i blockPosition) {
        this.state = state;
        this.blockPosition = blockPosition;
    }

    public WrappedBlockState getState() {
        return state;
    }

    public void setState(WrappedBlockState state) {
        this.state = state;
    }

    public Vector3i getBlockPosition() {
        return blockPosition;
    }

    public void setBlockPosition(Vector3i blockPosition) {
        this.blockPosition = blockPosition;
    }
}
