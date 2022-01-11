package com.github.graphene.util;

public class Vector2i {
    private int x, z;

    public Vector2i(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector2i) {
            return ((Vector2i) obj).x == x && ((Vector2i) obj).z == z;
        }
        return false;
    }
}