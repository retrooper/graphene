package com.github.graphene.packetevents.listener;

import com.github.graphene.Main;
import com.github.graphene.user.User;
import com.github.graphene.util.ChunkUtil;
import com.github.graphene.util.entity.ClientSettings;
import com.github.graphene.util.entity.EntityInformation;
import com.github.graphene.util.entity.UpdateType;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.entity.data.provider.EntityDataProvider;
import com.github.retrooper.packetevents.protocol.entity.data.provider.PlayerDataProvider;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.player.HumanoidArm;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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

                //Update the chunk cache(set to cobble stone always for now) TODO get block in hand
                ChunkUtil.setBlockStateByPosition(blockPlacement.getBlockPosition(),
                        cobbleStone);
                entityInformation.queueUpdate(UpdateType.BLOCK_PLACE);
            } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
                WrapperPlayClientPlayerDigging playerDigging = new WrapperPlayClientPlayerDigging(event);
                if (playerDigging.getAction() == WrapperPlayClientPlayerDigging.Action.FINISHED_DIGGING) {
                    entityInformation.setLastBlockActionPosition(playerDigging.getBlockPosition());

                    //Update the chunk cache(set to air)
                    ChunkUtil.setBlockStateByPosition(playerDigging.getBlockPosition(),
                            WrappedBlockState.getByGlobalId(0));
                    entityInformation.queueUpdate(UpdateType.BLOCK_DIG);
                }
            } else if (event.getPacketType() == PacketType.Play.Client.CLIENT_SETTINGS) {
                user.setClientSettings(new ClientSettings(new WrapperPlayClientSettings(event)));
                entityInformation.queueUpdate(UpdateType.METADATA);
            } else if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
                WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
                Location location = flying.getLocation();
                if (flying.hasPositionChanged() && flying.hasRotationChanged()) {
                    entityInformation.setLocation(location);
                    entityInformation.queueUpdate(UpdateType.POSITION_ANGLE);
                } else if (flying.hasPositionChanged()) {
                    entityInformation.setLocation(new Location(location.getPosition(), entityInformation.getLocation().getYaw(), entityInformation.getLocation().getPitch()));
                    //entityInformation.getLocation().setPosition(location.getPosition());
                    entityInformation.queueUpdate(UpdateType.POSITION);
                } else if (flying.hasRotationChanged()) {
                    entityInformation.getLocation().setYaw(location.getYaw());
                    entityInformation.getLocation().setPitch(location.getPitch());
                    entityInformation.queueUpdate(UpdateType.ANGLE);
                }
                entityInformation.setOnGround(flying.isOnGround());
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
                List<PacketWrapper<?>> packetQueue = new ArrayList<>();
                Location currentLocation = entityInformation.getLocation();
                for (UpdateType updateType : queuedUpdates) {
                    switch (updateType) {
                        case POSITION: {
                            Vector3d deltaPosition = currentLocation.getPosition().subtract(entityInformation.getTickLocation().getPosition());
                            System.out.println("delta movement by user " + user.getUsername() + " : " + deltaPosition.toString());
                            if (!deltaPosition.equals(Vector3d.zero())) {
                                if (shouldSendEntityTeleport(deltaPosition)) {
                                    WrapperPlayServerEntityTeleport teleport =
                                            new WrapperPlayServerEntityTeleport(user.getEntityId(),
                                                    currentLocation, user.getEntityInformation().isOnGround());
                                    packetQueue.add(teleport);
                                } else {
                                    double deltaX = deltaPosition.getX();
                                    double deltaY = deltaPosition.getY();
                                    double deltaZ = deltaPosition.getZ();
                                    System.out.println("dx: " + deltaX);

                                    packetQueue.add(new WrapperPlayServerEntityRelativeMove(user.getEntityId(),
                                            deltaX, deltaY, deltaZ, entityInformation.isOnGround()));

                                }

                            }
                            break;
                        }
                        case POSITION_ANGLE: {
                            Vector3d deltaPosition = currentLocation.getPosition().subtract(entityInformation.getTickLocation().getPosition());
                            //System.out.println("dmA: " + deltaPosition.toString());
                            if (shouldSendEntityTeleport(deltaPosition)) {
                                WrapperPlayServerEntityTeleport teleport =
                                        new WrapperPlayServerEntityTeleport(user.getEntityId(),
                                                currentLocation, user.getEntityInformation().isOnGround());
                                packetQueue.add(teleport);
                            } else {
                                double deltaX = deltaPosition.getX();
                                double deltaY = deltaPosition.getY();
                                double deltaZ = deltaPosition.getZ();

                                packetQueue.add(new WrapperPlayServerEntityRelativeMoveAndRotation(user.getEntityId(),
                                        deltaX, deltaY, deltaZ, (byte) currentLocation.getYaw(),
                                        (byte) currentLocation.getPitch(), entityInformation.isOnGround()));


                                byte headYaw = (byte) ((int) (currentLocation.getYaw() * 256.0F / 360.0F));
                                packetQueue.add(new WrapperPlayServerEntityHeadLook(user.getEntityId(), headYaw));
                            }

                            break;
                        }
                        case ANGLE:
                            packetQueue.add(new WrapperPlayServerEntityRotation(user.getEntityId(),
                                    (byte) currentLocation.getYaw(), (byte) currentLocation.getPitch(), entityInformation.isOnGround()));
                            byte headYaw = (byte) ((int) (currentLocation.getYaw() * 256.0F / 360.0F));
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
                    //entityInformation.setTickLocation(new Location(0, 0, 0, 0, 0));
                    entityInformation.setTickLocation(entityInformation.getLocation());
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

                    List<Equipment> equipment = new ArrayList<>();
                    ItemStack item = user.inventory[0];
                    if (item == null) {
                        item = ItemStack.builder().type(ItemTypes.AIR).amount(1).build();
                    }
                    //Allow single item constructor
                    equipment.add(new Equipment(EquipmentSlot.MAINHAND, item));
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

    public static boolean shouldSendEntityTeleport(Vector3d deltaMovement) {
        //return Math.abs(deltaMovement.getX()) > 8 || Math.abs(deltaMovement.getY()) > 8 || Math.abs(deltaMovement.getZ()) > 8;
        return true;
    }

    public static WrapperPlayServerEntityTeleport getEntityTeleport(User user) {
        EntityInformation entityInformation = user.getEntityInformation();
        Location location = entityInformation.getLocation();
        boolean onGround = entityInformation.isOnGround();

        return new WrapperPlayServerEntityTeleport(user.getEntityId(), location, onGround);
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
                        .customName(Component.text("noice").color(NamedTextColor.RED).asComponent())
                        .customNameVisible(true)
                        .pose(entityPose).build();
        return new WrapperPlayServerEntityMetadata(targetEntityId, playerDataProvider.encode());
    }

}
