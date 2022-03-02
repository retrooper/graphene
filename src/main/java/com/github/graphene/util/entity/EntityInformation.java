package com.github.graphene.util.entity;

import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EntityInformation {
    private UUID uuid;
    private Location location;
    private Location tickLocation;
    private boolean onGround;
    private boolean sneaking;
    private boolean sprinting;
    private boolean onFire;
    private boolean invisible;
    private final Queue<UpdateType> queuedUpdates;
    private float health;
    private int food;
    private float saturation;
    private boolean groundUpdate;
    private Vector3i lastBlockActionPosition;
    private WrappedBlockState lastBlockActionData;

    public EntityInformation(Location spawnLocation) {
        this.location = spawnLocation;
        this.tickLocation = spawnLocation;
        health = 20.0f;
        food = 20;
        saturation = 5.0f;

        groundUpdate = true;
        queuedUpdates = new ConcurrentLinkedQueue<>();
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Location getTickLocation() {
        return tickLocation;
    }

    public void setTickLocation(Location tickLocation) {
        this.tickLocation = tickLocation;
    }


    public Vector3i getLastBlockActionPosition() {
        return lastBlockActionPosition;
    }

    public void setLastBlockActionPosition(Vector3i lastBlockActionPosition) {
        this.lastBlockActionPosition = lastBlockActionPosition;
    }

    public WrappedBlockState getLastBlockActionData() {
        return lastBlockActionData;
    }

    public void setLastBlockActionData(WrappedBlockState lastBlockActionData) {
        this.lastBlockActionData = lastBlockActionData;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public boolean isSneaking() {
        return this.sneaking;
    }

    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }

    public boolean isSprinting() {
        return this.sprinting;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public boolean isOnFire() {
        return this.onFire;
    }

    public void setOnFire(boolean onFire) {
        this.onFire = onFire;
    }

    public boolean isInvisible() {
        return this.invisible;
    }

    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
    }

    public void queueUpdate(UpdateType updateType) {
        if (!queuedUpdates.contains(updateType)) queuedUpdates.add(updateType);

        if (queuedUpdates.contains(UpdateType.POSITION_ANGLE))
            queuedUpdates.removeIf(type -> type == UpdateType.ANGLE || type == UpdateType.POSITION);
    }

    public Queue<UpdateType> getQueuedUpdates() {
        return queuedUpdates;
    }

    public void resetQueuedUpdates() {
        queuedUpdates.clear();
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
