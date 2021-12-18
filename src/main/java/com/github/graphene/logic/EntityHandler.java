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
import com.github.retrooper.packetevents.netty.buffer.ByteBufAbstract;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.HumanoidArm;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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
                WrapperPlayClientPosition positionWrapper = new WrapperPlayClientPosition(event);
                Vector3d position = positionWrapper.getPosition();

                entityInformation.setPosition(position.getX(), position.getY(), position.getZ());
                entityInformation.setOnGround(positionWrapper.isOnGround());
                entityInformation.addUpdateTotal(UpdateType.POSITION);
                entityInformation.addUpdateTotal(UpdateType.GROUND);
            } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                WrapperPlayClientPositionRotation positionRotationWrapper = new WrapperPlayClientPositionRotation(event);
                Vector3d position = positionRotationWrapper.getPosition();

                entityInformation.setPosition(position.getX(), position.getY(), position.getZ());
                entityInformation.setAngle(positionRotationWrapper.getYaw(), positionRotationWrapper.getPitch());
                entityInformation.setOnGround(positionRotationWrapper.isOnGround());
                entityInformation.addUpdateTotal(UpdateType.POSITION_ANGLE);
                entityInformation.addUpdateTotal(UpdateType.GROUND);
            } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
                WrapperPlayClientRotation rotationWrapper = new WrapperPlayClientRotation(event);

                entityInformation.setAngle(rotationWrapper.getYaw(), rotationWrapper.getPitch());
                entityInformation.setOnGround(rotationWrapper.isOnGround());
                entityInformation.addUpdateTotal(UpdateType.ANGLE);
                entityInformation.addUpdateTotal(UpdateType.GROUND);
            } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING) {
                WrapperPlayClientFlying flyingWrapper = new WrapperPlayClientFlying(event);

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

    public static void onTick() {
        for (User user : Graphene.USERS) {
            EntityInformation entityInformation = user.getEntityInformation();

            if (entityInformation.getTotalUpdates().size() > 1) {
                List<UpdateType> totalUpdates = entityInformation.getTotalUpdates();
                Location position = entityInformation.getPosition();
                Location lastPosition = entityInformation.getLastPosition();
                Location angle = entityInformation.getAngle();

                List<PacketWrapper> packetQueue = new ArrayList<>();

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

                                packetQueue.add(new WrapperPlayServerEntityRelativeMoveAndLook(user.getEntityId(), deltaX, deltaY, deltaZ, (byte) angle.getYaw(), (byte) angle.getPitch(), entityInformation.isOnGround());
                            }

                            break;
                    }
                }
            }
        }
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



}
