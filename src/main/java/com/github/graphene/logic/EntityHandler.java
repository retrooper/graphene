package com.github.graphene.logic;

import com.github.graphene.Graphene;
import com.github.graphene.user.User;
import com.github.graphene.util.entity.ClientSettings;
import com.github.graphene.util.entity.EntityInformation;
import com.github.graphene.util.entity.Location;
import com.github.graphene.util.entity.UpdateType;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import com.github.retrooper.packetevents.wrapper.play.server.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EntityHandler implements PacketListener {

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        User user = (User) event.getPlayer();
        assert user != null;
        EntityInformation entityInformation = user.getEntityInformation();

        if (user.getState() == ConnectionState.PLAY) {
            if (event.getPacketType() == PacketType.Play.Client.CLIENT_SETTINGS) {
                user.setClientSettings(new ClientSettings(new WrapperPlayClientSettings(event)));
                entityInformation.addUpdateTotal(UpdateType.METADATA);
            } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
                WrapperPlayClientPlayerPosition positionWrapper = new WrapperPlayClientPlayerPosition(event);
                Vector3d position = positionWrapper.getPosition();

                entityInformation.setPosition(position.getX(), position.getY(), position.getZ());
                entityInformation.setOnGround(positionWrapper.isOnGround());
                entityInformation.addUpdateTotal(UpdateType.POSITION);
                entityInformation.addUpdateTotal(UpdateType.GROUND);
            } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                WrapperPlayClientPlayerPositionAndRotation positionRotationWrapper = new WrapperPlayClientPlayerPositionAndRotation(event);
                Vector3d position = positionRotationWrapper.getPosition();

                entityInformation.setPosition(position.getX(), position.getY(), position.getZ());
                entityInformation.setAngle(wrapAngleTo180(positionRotationWrapper.getYaw()), positionRotationWrapper.getPitch());
                entityInformation.setOnGround(positionRotationWrapper.isOnGround());
                entityInformation.addUpdateTotal(UpdateType.POSITION_ANGLE);
                entityInformation.addUpdateTotal(UpdateType.GROUND);
            } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
                WrapperPlayClientPlayerRotation rotationWrapper = new WrapperPlayClientPlayerRotation(event);

                entityInformation.setAngle(wrapAngleTo180(rotationWrapper.getYaw()), rotationWrapper.getPitch());
                entityInformation.setOnGround(rotationWrapper.isOnGround());
                entityInformation.addUpdateTotal(UpdateType.ANGLE);
                entityInformation.addUpdateTotal(UpdateType.GROUND);
            } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING) {
                WrapperPlayClientPlayerFlying<?> flyingWrapper = new WrapperPlayClientPlayerFlying<>(event);
                entityInformation.setOnGround(flyingWrapper.isOnGround());
                entityInformation.addUpdateTotal(UpdateType.GROUND);
            } else if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
                WrapperPlayClientEntityAction entityActionWrapper = new WrapperPlayClientEntityAction(event);

                if (entityActionWrapper.getEntityId() == user.getEntityId()) {
                    switch (entityActionWrapper.getAction()) {
                        case START_SNEAKING -> entityInformation.setSneaking(true);
                        case STOP_SNEAKING -> entityInformation.setSneaking(false);
                        case START_SPRINTING -> entityInformation.setSprinting(true);
                        case STOP_SPRINTING -> entityInformation.setSprinting(false);
                    }

                    entityInformation.addUpdateTotal(UpdateType.METADATA);
                }
            } else if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
                WrapperPlayClientAnimation animationWrapper = new WrapperPlayClientAnimation(event);
                WrapperPlayServerEntityAnimation.EntityAnimationType animation = animationWrapper.getHand() == InteractionHand.MAIN_HAND ? WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM : WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFFHAND;
                WrapperPlayServerEntityAnimation entityAnimationWrapper = new WrapperPlayServerEntityAnimation(user.getEntityId(), animation);

                for (User lUser : Graphene.USERS) {
                    if (lUser.getEntityId() != user.getEntityId()) {
                        PacketEvents.getAPI().getPlayerManager().sendPacket(lUser, entityAnimationWrapper);
                    }
                }
            }
        }
    }
    
    //TODO Analyse the way we tick
    public static void onTick() {
        for (User user : Graphene.USERS) {
            EntityInformation entityInformation = user.getEntityInformation();
            if (!entityInformation.getTotalUpdates().isEmpty()) {
                Queue<UpdateType> totalUpdates = entityInformation.getTotalUpdates();
                Location position = entityInformation.getPosition();
                Location lastPosition = entityInformation.getLastPosition();
                Location angle = entityInformation.getAngle();
                List<PacketWrapper<?>> packetQueue = new ArrayList<>();

                for (UpdateType updateType : totalUpdates) {
                    switch (updateType) {
                        case POSITION:
                            if (shouldSendEntityTeleport(position, lastPosition)) {
                                packetQueue.add(getEntityTeleport(user));
                            } else {
                                double deltaX = (position.getX() * 32 - lastPosition.getX() * 32) * 128;
                                double deltaY = (position.getY() * 32 - lastPosition.getY() * 32) * 128;
                                double deltaZ = (position.getZ() * 32 - lastPosition.getZ() * 32) * 128;

                                packetQueue.add(new WrapperPlayServerEntityRelativeMove(user.getEntityId(), deltaX, deltaY, deltaZ, entityInformation.isOnGround()));

                            }

                            break;
                        case POSITION_ANGLE:
                            if (shouldSendEntityTeleport(position, lastPosition)) {
                                packetQueue.add(getEntityTeleport(user));
                            } else {
                                double deltaX = (position.getX() * 32 - lastPosition.getX() * 32) * 128;
                                double deltaY = (position.getY() * 32 - lastPosition.getY() * 32) * 128;
                                double deltaZ = (position.getZ() * 32 - lastPosition.getZ() * 32) * 128;

                                packetQueue.add(new WrapperPlayServerEntityRelativeMoveAndRotation(user.getEntityId(), deltaX, deltaY, deltaZ, (byte) angle.getYaw(), (byte) angle.getPitch(), entityInformation.isOnGround()));
                                packetQueue.add(new WrapperPlayServerEntityHeadLook(user.getEntityId(), (byte) angle.getYaw()));
                            }

                            break;
                        case ANGLE:
                            packetQueue.add(new WrapperPlayServerEntityRotation(user.getEntityId(), (byte) angle.getYaw(), (byte) angle.getPitch(), entityInformation.isOnGround()));
                            packetQueue.add(new WrapperPlayServerEntityHeadLook(user.getEntityId(), (byte) angle.getYaw()));

                            break;
                        case METADATA:
                            // packetQueue.add(getEntityMetadata(user));

                            break;
                        case LATENCY:
                            List<WrapperPlayServerPlayerInfo.PlayerData> playerDataList = new ArrayList<>();

                            playerDataList.add(new WrapperPlayServerPlayerInfo.PlayerData(null, null, null, (int) user.getLatency()));

                            packetQueue.add(new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.UPDATE_LATENCY, user.getUUID(), playerDataList));

                            break;
                    }

                    for (User lUser : Graphene.USERS) {
                        if (user.getEntityId() != lUser.getEntityId()) {
                            for (PacketWrapper<?> wrapper : packetQueue) {
                                PacketEvents.getAPI().getPlayerManager().sendPacket(lUser, wrapper);
                            }
                        }
                    }
                }

                if (totalUpdates.contains(UpdateType.POSITION) || totalUpdates.contains(UpdateType.POSITION_ANGLE)) {
                    entityInformation.resetLastPosition();
                }

                entityInformation.resetTotalUpdates();
            }
        }
    }

    public static void onLogin(User user) {
        for (User lUser : Graphene.USERS) {
            if (user.getEntityId() == lUser.getEntityId()) {
                sendEntities(user, Graphene.USERS);
            } else {
                Queue<User> users = new ConcurrentLinkedQueue<>();
                users.add(user);
                sendEntities(lUser, users);
            }
        }
    }

    public static void sendEntities(User user, Queue<User> users) {
        //Only operate if 2 or more users are online
        if (users.size() >= 2) {
            for (User lUser : users) {
                if (lUser.getEntityId() != user.getEntityId()) {
                    Location location = lUser.getEntityInformation().getPosition();
                    Location angle = lUser.getEntityInformation().getAngle();

                    WrapperPlayServerSpawnPlayer spawnPlayer = new WrapperPlayServerSpawnPlayer(lUser.getEntityId(), lUser.getUUID(), new Vector3d(location.getX(), location.getY(), location.getZ()), angle.getYaw(), angle.getPitch(), new ArrayList<>());
                    WrapperPlayServerEntityMetadata entityMetadata = getEntityMetadata(lUser);

                    PacketEvents.getAPI().getPlayerManager().sendPacket(user, spawnPlayer);
//                    PacketEvents.getAPI().getPlayerManager().sendPacket(user, entityMetadata);
                }
            }
        }
    }

    public static float wrapAngleTo180(float num) {
        float val = num % 360;

        if (val >= 180) {
            val -= 360;
        } else if (val < -180) {
            val += 360;
        }

        return val;
    }

    public static boolean shouldSendEntityTeleport(Location position, Location lastPosition) {
        return Math.abs(position.getX() - lastPosition.getX()) > 8 || Math.abs(position.getY() - lastPosition.getY()) > 8 || Math.abs(position.getZ() - lastPosition.getZ()) > 8 || (position.getX() - lastPosition.getX()) == 0 || (position.getY() - lastPosition.getY()) == 0 || (position.getZ() - lastPosition.getZ()) == 0;
    }

    public static WrapperPlayServerEntityTeleport getEntityTeleport(User user) {
        EntityInformation entityInformation = user.getEntityInformation();
        Location position = entityInformation.getPosition();
        boolean onGround = entityInformation.isOnGround();
        Location angle = entityInformation.getAngle();

        return new WrapperPlayServerEntityTeleport(user.getEntityId(), new Vector3d(position.getX(), position.getY(), position.getZ()), angle.getYaw(), angle.getPitch(), onGround);
    }

    public static WrapperPlayServerEntityMetadata getEntityMetadata(User user) {
        List<EntityData> information = new ArrayList<>();
        EntityInformation entityInformation = user.getEntityInformation();
        ClientSettings clientSettings = user.getClientSettings();

        information.add(new EntityData(17, EntityDataTypes.BYTE, clientSettings.getDisplayedSkinParts()));
        information.add(new EntityData(18, EntityDataTypes.BYTE, (byte)clientSettings.getMainHand().getId()));

        byte flagBitmask = 0;

        if (entityInformation.isOnFire()) flagBitmask |= 0x01;
        if (entityInformation.isSneaking()) flagBitmask |= 0x02;
        if (entityInformation.isSprinting()) flagBitmask |= 0x08;

        information.add(new EntityData(0, EntityDataTypes.BYTE, flagBitmask));
        information.add(new EntityData(6, EntityDataTypes.ENTITY_POSE, entityInformation.isSneaking() ? EntityPose.CROUCHING : EntityPose.STANDING));

        return new WrapperPlayServerEntityMetadata(user.getEntityId(), information);
    }

}
