package com.github.graphene.util.entity;

public class Location {

    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    public Location(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Location(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Location(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

}
