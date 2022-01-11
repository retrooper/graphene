package com.github.graphene.packetevents.listener;

import com.github.graphene.Main;
import com.github.graphene.user.User;
import com.github.graphene.util.Vector2i;
import com.github.graphene.util.entity.ClientSettings;
import com.github.graphene.util.entity.EntityInformation;
import com.github.graphene.util.entity.UpdateType;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.chat.Color;
import com.github.retrooper.packetevents.protocol.chat.component.impl.TextComponent;
import com.github.retrooper.packetevents.protocol.entity.data.provider.EntityDataProvider;
import com.github.retrooper.packetevents.protocol.entity.data.provider.PlayerDataProvider;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.HumanoidArm;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.MathUtil;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import com.github.retrooper.packetevents.wrapper.play.server.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class EntityHandler implements PacketListener {
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        User user = (User) event.getPlayer();
        assert user != null;
        EntityInformation entityInformation = user.getEntityInformation();
        if (user.getState() == ConnectionState.PLAY) {
            if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
                WrapperPlayClientPlayerBlockPlacement blockPlacement = new WrapperPlayClientPlayerBlockPlacement(event);
                entityInformation.setLastBlockActionPosition(blockPlacement.getBlockPosition());
                WrappedBlockState cobbleStone = WrappedBlockState.getByString("minecraft:cobblestone");
                entityInformation.setLastBlockActionData(cobbleStone);

                //Update the chunk cache
                //TODO Round down
                int chunkX = blockPlacement.getBlockPosition().getX() / 16;
                int chunkZ = blockPlacement.getBlockPosition().getZ() / 16;
                System.out.println("cx: " + chunkX + " cz: " + chunkZ);
                Vector2i chunkCoord = new Vector2i(chunkX, chunkZ);
                for (Vector2i c : Main.CHUNKS.keySet()) {
                    if (c.equals(chunkCoord)) {
                        Column column = Main.CHUNKS.get(c);
                        int chunkIndex = MathUtil.floor(blockPlacement.getBlockPosition().getY() / 16.0);
                        BaseChunk chunk = column.getChunks()[chunkIndex];
                        chunk.set(blockPlacement.getBlockPosition().getX(), blockPlacement.getBlockPosition().getY() + 1,
                                blockPlacement.getBlockPosition().getZ(), cobbleStone.getGlobalId());
                        entityInformation.queueUpdate(UpdateType.BLOCK_PLACE);
                        break;
                    }
                }
            }
            else if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
                WrapperPlayClientPlayerDigging playerDigging = new WrapperPlayClientPlayerDigging(event);
                if (playerDigging.getAction() == WrapperPlayClientPlayerDigging.Action.FINISHED_DIGGING) {
                    entityInformation.setLastBlockActionPosition(playerDigging.getBlockPosition());

                    //Update the chunk cache
                    int chunkX = playerDigging.getBlockPosition().getX() / 16;
                    int chunkZ = playerDigging.getBlockPosition().getZ() / 16;
                    Vector2i chunkCoord = new Vector2i(chunkX, chunkZ);
                    for (Vector2i c : Main.CHUNKS.keySet()) {
                        if (c.equals(chunkCoord)) {
                            Column column = Main.CHUNKS.get(c);
                            int chunkIndex = MathUtil.floor(playerDigging.getBlockPosition().getY() / 16.0);
                            BaseChunk chunk = column.getChunks()[chunkIndex];
                            chunk.set(playerDigging.getBlockPosition().getX(), playerDigging.getBlockPosition().getY() + 1,
                                    playerDigging.getBlockPosition().getZ(), 0);
                            entityInformation.queueUpdate(UpdateType.BLOCK_DIG);
                        }
                    }
                }
            }
            else if (event.getPacketType() == PacketType.Play.Client.CLIENT_SETTINGS) {
                user.setClientSettings(new ClientSettings(new WrapperPlayClientSettings(event)));
                entityInformation.queueUpdate(UpdateType.METADATA);
            } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
                WrapperPlayClientPlayerPosition positionWrapper = new WrapperPlayClientPlayerPosition(event);
                Vector3d position = positionWrapper.getPosition();
                entityInformation.getLocation().setPosition(position);
                entityInformation.setOnGround(positionWrapper.isOnGround());
                entityInformation.queueUpdate(UpdateType.POSITION);
                entityInformation.queueUpdate(UpdateType.GROUND);
            } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                WrapperPlayClientPlayerPositionAndRotation positionRotationWrapper = new WrapperPlayClientPlayerPositionAndRotation(event);
                Location location = new Location(positionRotationWrapper.getPosition(),
                        positionRotationWrapper.getYaw(), positionRotationWrapper.getPitch());
                entityInformation.setLocation(location);
                entityInformation.setOnGround(positionRotationWrapper.isOnGround());
                entityInformation.queueUpdate(UpdateType.POSITION_ANGLE);
                entityInformation.queueUpdate(UpdateType.GROUND);
            } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
                WrapperPlayClientPlayerRotation rotationWrapper = new WrapperPlayClientPlayerRotation(event);
                entityInformation.getLocation().setYaw(rotationWrapper.getYaw());
                entityInformation.getLocation().setPitch(rotationWrapper.getPitch());
                entityInformation.setOnGround(rotationWrapper.isOnGround());
                entityInformation.queueUpdate(UpdateType.ANGLE);
                entityInformation.queueUpdate(UpdateType.GROUND);
            } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING) {
                WrapperPlayClientPlayerFlying flyingWrapper = new WrapperPlayClientPlayerFlying(event);
                entityInformation.setOnGround(flyingWrapper.isOnGround());
                entityInformation.queueUpdate(UpdateType.GROUND);
            } else if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
                WrapperPlayClientEntityAction entityActionWrapper = new WrapperPlayClientEntityAction(event);
                if (entityActionWrapper.getEntityId() == user.getEntityId()) {
                    switch (entityActionWrapper.getAction()) {
                        case START_SNEAKING -> entityInformation.setSneaking(true);
                        case STOP_SNEAKING -> entityInformation.setSneaking(false);
                        case START_SPRINTING -> entityInformation.setSprinting(true);
                        case STOP_SPRINTING -> entityInformation.setSprinting(false);
                    }

                    entityInformation.queueUpdate(UpdateType.METADATA);
                }
            } else if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
                WrapperPlayClientAnimation animationWrapper = new WrapperPlayClientAnimation(event);
                WrapperPlayServerEntityAnimation.EntityAnimationType animation = animationWrapper.getHand() == InteractionHand.MAIN_HAND ? WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM : WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFFHAND;
                WrapperPlayServerEntityAnimation entityAnimationWrapper = new WrapperPlayServerEntityAnimation(user.getEntityId(), animation);

                for (User lUser : Main.USERS) {
                    if (lUser.getEntityId() != user.getEntityId()) {
                        lUser.sendPacket(entityAnimationWrapper);
                    }
                }
            }
        }
    }

    public static void onTick() {
        for (User user : Main.USERS) {
            EntityInformation entityInformation = user.getEntityInformation();
            if (!entityInformation.getQueuedUpdates().isEmpty()) {
                Queue<UpdateType> queuedUpdates = entityInformation.getQueuedUpdates();
                Location currentLocation = entityInformation.getLocation();
                Location lastPosition = entityInformation.getLastPosition();
                List<PacketWrapper<?>> packetQueue = new ArrayList<>();

                for (UpdateType updateType : queuedUpdates) {
                    switch (updateType) {
                        case POSITION:
                            if (shouldSendEntityTeleport(currentLocation, lastPosition)) {
                                packetQueue.add(getEntityTeleport(user));
                            } else {
                                double deltaX = currentLocation.getX() - lastPosition.getX();
                                double deltaY = currentLocation.getY() - lastPosition.getY();
                                double deltaZ = currentLocation.getZ() - lastPosition.getZ();

                                packetQueue.add(new WrapperPlayServerEntityRelativeMove(user.getEntityId(),
                                        deltaX, deltaY, deltaZ, entityInformation.isOnGround()));

                            }

                            break;
                        case POSITION_ANGLE:
                            if (shouldSendEntityTeleport(currentLocation, lastPosition)) {
                                packetQueue.add(getEntityTeleport(user));
                            } else {
                                double deltaX = currentLocation.getX() - lastPosition.getX();
                                double deltaY = currentLocation.getY() - lastPosition.getY();
                                double deltaZ = currentLocation.getZ() - lastPosition.getZ();

                                packetQueue.add(new WrapperPlayServerEntityRelativeMoveAndRotation(user.getEntityId(),
                                        deltaX, deltaY, deltaZ, (byte) currentLocation.getYaw(),
                                        (byte) currentLocation.getPitch(), entityInformation.isOnGround()));


                                byte headYaw =  (byte) ((int)(currentLocation.getYaw() * 256.0F / 360.0F));
                                packetQueue.add(new WrapperPlayServerEntityHeadLook(user.getEntityId(), headYaw));
                            }

                            break;
                        case ANGLE:
                            packetQueue.add(new WrapperPlayServerEntityRotation(user.getEntityId(),
                                    (byte) currentLocation.getYaw(), (byte) currentLocation.getPitch(), entityInformation.isOnGround()));
                            byte headYaw =  (byte) ((int)(currentLocation.getYaw() * 256.0F / 360.0F));
                            packetQueue.add(new WrapperPlayServerEntityHeadLook(user.getEntityId(), headYaw));

                            break;
                        case METADATA:
                            packetQueue.add(getEntityMetadata(user.getEntityId(), user));
                            break;
                        case LATENCY:
                            List<WrapperPlayServerPlayerInfo.PlayerData> playerDataList = new ArrayList<>();

                            playerDataList.add(new WrapperPlayServerPlayerInfo.PlayerData(null, user.getGameProfile(), null, (int) user.getLatency()));

                            packetQueue.add(new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.UPDATE_LATENCY, playerDataList));

                            break;

                        case BLOCK_DIG:
                            //Set to air
                            if (entityInformation.getLastBlockActionPosition() != null) {
                                WrapperPlayServerBlockChange blockChange = new WrapperPlayServerBlockChange(entityInformation.getLastBlockActionPosition(), 0);
                                packetQueue.add(blockChange);
                            }
                            break;

                        case BLOCK_PLACE:

                            if (entityInformation.getLastBlockActionPosition() != null) {
                                WrapperPlayServerBlockChange blockChange = new WrapperPlayServerBlockChange(entityInformation.getLastBlockActionPosition(), entityInformation.getLastBlockActionData().getGlobalId());
                                packetQueue.add(blockChange);
                            }
                    }

                    for (User lUser : Main.USERS) {
                        if (user.getEntityId() != lUser.getEntityId()) {
                            for (PacketWrapper<?> wrapper : packetQueue) {
                                lUser.sendPacket(wrapper);
                            }
                        }
                    }
                }

                if (queuedUpdates.contains(UpdateType.POSITION) || queuedUpdates.contains(UpdateType.POSITION_ANGLE)) {
                    entityInformation.resetLastPosition();
                }

                entityInformation.resetQueuedUpdates();
            }
        }
    }

    public static void onLogin(User user) {
        for (User p : Main.USERS) {
            if (user.getEntityId() == p.getEntityId()) {
                //Spawn us for others.
                spawnUser(user, Main.USERS, 2);
            } else {
                //Spawn the others for us.
                //As an optimization, we set the users to have one player.
                Queue<User> users = new LinkedList<>();
                users.add(user);
                spawnUser(p, users, 1);
            }
        }
    }

    public static void spawnUser(User user, Queue<User> onlinePlayers, int minToOperate) {
        if (onlinePlayers.size() >= minToOperate) {
            for (User onlinePlayer : onlinePlayers) {
                if (onlinePlayer.getEntityId() != user.getEntityId()) {
                    Location location = onlinePlayer.getEntityInformation().getLocation();
                    Location newLocation = new Location(location.getPosition(), location.getYaw(), location.getPitch());

                    WrapperPlayServerSpawnPlayer spawnPlayer = new WrapperPlayServerSpawnPlayer(onlinePlayer.getEntityId(), onlinePlayer.getGameProfile().getId(), newLocation);
                    PacketEvents.getAPI().getPlayerManager().sendPacket(user, spawnPlayer);

                    //Inform us about our own entity metadata
                    WrapperPlayServerEntityMetadata metadata = getEntityMetadata(user.getEntityId(), user);
                    metadata.prepareForSend();
                    metadata.getBuffer().retain();
                    user.sendPacket(metadata);
                    metadata.getBuffer().retain();
                    onlinePlayer.sendPacket(metadata);

                    List<WrapperPlayServerEntityEquipment.Equipment> equipment = new ArrayList<>();
                    ItemStack item = user.inventory[0];
                    if (item == null) {
                        item = ItemStack.builder().type(ItemTypes.AIR).amount(1).build();
                    }
                    //Allow single item constructor
                    equipment.add(new WrapperPlayServerEntityEquipment.Equipment(WrapperPlayServerEntityEquipment.EquipmentSlot.MAINHAND, item));
                    //Show them what we have
                    WrapperPlayServerEntityEquipment equipmentPacket = new WrapperPlayServerEntityEquipment(user.getEntityId(), equipment);
                    onlinePlayer.sendPacket(equipmentPacket);
                    //Show us what they have
                    WrapperPlayServerEntityEquipment equipmentPacket2 = new WrapperPlayServerEntityEquipment(onlinePlayer.getEntityId(), equipment);
                    user.sendPacket(equipmentPacket2);
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
        Location location = entityInformation.getLocation();
        boolean onGround = entityInformation.isOnGround();

        return new WrapperPlayServerEntityTeleport(user.getEntityId(), new Vector3d(location.getX(), location.getY(), location.getZ()), location.getYaw(), location.getPitch(), onGround);
    }

    public static WrapperPlayServerEntityMetadata getEntityMetadata(int targetEntityId, User user) {
        EntityInformation entityInformation = user.getEntityInformation();
        ClientSettings clientSettings = user.getClientSettings();

        EntityPose entityPose = entityInformation.isSneaking() ? EntityPose.CROUCHING : EntityPose.STANDING;

        EntityDataProvider playerDataProvider =
                PlayerDataProvider.builderPlayer()
                        .mainArm(HumanoidArm.RIGHT)
                        .skinParts(clientSettings.getVisibleSkinSections())
                        .handActive(true)
                        .hand(InteractionHand.MAIN_HAND)
                        .crouching(entityInformation.isSneaking())
                        .sprinting(entityInformation.isSprinting())
                        .onFire(entityInformation.isOnFire())
                        .hasGravity(true)
                        .invisible(entityInformation.isInvisible())
                        .customName(TextComponent.builder().text("noice").color(Color.RED).build())
                        .customNameVisible(true)
                        .pose(entityPose).build();
        return new WrapperPlayServerEntityMetadata(targetEntityId, playerDataProvider.encode());
    }

}
