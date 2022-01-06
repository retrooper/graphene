package com.github.graphene.util.entity;

import com.github.retrooper.packetevents.protocol.world.Location;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EntityInformation {

    private UUID uuid;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private boolean onGround;
    private final Queue<UpdateType> totalUpdates;
    private double lastX;
    private double lastY;
    private double lastZ;
    private boolean onFire;
    private boolean sneaking;
    private boolean sprinting;
    private float health;
    private int food;
    private float saturation;
    private double groundX;
    private double groundY;
    private double groundZ;
    private boolean groundUpdate;

    public EntityInformation(Location spawnLocation) {
        x = spawnLocation.getX();
        y = spawnLocation.getY();
        z = spawnLocation.getZ();

        lastX = spawnLocation.getX();
        lastY = spawnLocation.getY();
        lastZ = spawnLocation.getZ();

        groundX = spawnLocation.getX();
        groundY = spawnLocation.getY();
        groundZ = spawnLocation.getZ();

        yaw = spawnLocation.getYaw();
        pitch = spawnLocation.getPitch();

        onFire = false;
        health = 20.0f;
        food = 20;
        saturation = 5.0f;

        sneaking = false;
        sprinting = false;

        groundUpdate = true;
        totalUpdates = new ConcurrentLinkedQueue<>();
    }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Location getPosition() {
        return new Location(x, y, z, 0, 0);
    }

    public void resetLastPosition() {
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
    }

    public void setAngle(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Location getAngle() {
        return new Location(0, 0, 0, this.yaw, this.pitch);
    }

    public void setGroundPosition() {
        this.groundX = this.x;
        this.groundY = this.y;
        this.groundZ = this.z;
    }

    public Location getGroundPosition() {
        return new Location(this.groundX, this.groundY, this.groundZ, 0, 0);
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public Location getLastPosition() {
        return new Location(this.lastX, this.lastY, this.lastZ, 0, 0);
    }

    public void addUpdateTotal(UpdateType updateType) {
        if (!totalUpdates.contains(updateType)) totalUpdates.add(updateType);

        if (totalUpdates.contains(UpdateType.POSITION_ANGLE))
            totalUpdates.removeIf(type -> type == UpdateType.ANGLE || type == UpdateType.POSITION);
    }

    public Queue<UpdateType> getTotalUpdates() {
        return totalUpdates;
    }

    public void resetTotalUpdates() {
        totalUpdates.clear();
    }

    public boolean isOnFire() {
        return onFire;
    }

    public void setOnFire(boolean onFire) {
        this.onFire = onFire;
    }

    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public void setFood(int food) {
        this.food = food;
    }

    public int getFood() {
        return food;
    }

    public void setHealth(float health) {
        this.health = health;
    }

    public float getHealth() {
        return health;
    }

    public float getSaturation() {
        return saturation;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    public void startGroundUpdates() {
        this.groundUpdate = true;
    }

    public void stopGroundUpdates() {
        this.groundUpdate = false;
    }

    public boolean getGroundUpdate() {
        return groundUpdate;
    }

}
