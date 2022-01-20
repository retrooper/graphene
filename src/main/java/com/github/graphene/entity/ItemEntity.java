package com.github.graphene.entity;

import com.github.graphene.Main;
import com.github.graphene.user.User;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.data.provider.EntityDataProvider;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;

public class ItemEntity {
    public static double PICKUP_DISTANCE = 2;
    private final int entityId;
    private final UUID uuid;
    private Vector3d position;
    private ItemStack item;
    private long lastSpawnTime;

    public ItemEntity(Vector3d position, ItemStack item) {
        this.entityId = Main.ENTITIES++;
        this.uuid = UUID.randomUUID();
        this.position = position;
        this.item = item;
    }

    public int getEntityId() {
        return entityId;
    }

    public UUID getUUID() {
        return uuid;
    }

    public Vector3d getPosition() {
        return position;
    }

    public void setPosition(Vector3d position) {
        this.position = position;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public void pickup(User user, Queue<User> target) {
        //Half a second
        if ((System.currentTimeMillis() - lastSpawnTime) >= 1000L) {
            for (User player : target) {
                WrapperPlayServerCollectItem collectItem = new WrapperPlayServerCollectItem(entityId, user.getEntityId(), getItem().getAmount());
                WrapperPlayServerDestroyEntities destroyEntities = new WrapperPlayServerDestroyEntities(getEntityId());
                player.sendPacket(destroyEntities);
                player.sendPacket(collectItem);
            }
            Main.ITEM_ENTITIES.remove(this);
            ItemStack currentItem = user.getCurrentItem();
            if (currentItem == null) {
                user.setCurrentItem(getItem());
            }
            else {
                //TODO Look into your inventory, look for the type and add
                currentItem.grow(getItem().getAmount());
            }
            user.updateHotbar();
        }
    }

    public void spawn(User spawner, Queue<User> target) {
        Vector3d pos = spawner.getEntityInformation().getLocation().getPosition();
        pos.add(new Vector3d(0, 4, 0));
        for (User player : target) {
            WrapperPlayServerSpawnEntity spawnEntity = new WrapperPlayServerSpawnEntity(entityId,
                    Optional.of(UUID.randomUUID()),
                    EntityTypes.ITEM,
                    pos,
                    0, 0,
                    1,
                    Optional.of(new Vector3d(0, -4, 0)));
            List<EntityData> data = EntityDataProvider.builderEntity()
                    .customName(Component.text("nice item").color(NamedTextColor.GOLD).asComponent())
                    .customNameVisible(true).build().encode();
            data.add(new EntityData(8, EntityDataTypes.ITEMSTACK, item));
            WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(entityId, data);
            //TODO Change holding
            WrapperPlayServerAttachEntity attachEntity = new WrapperPlayServerAttachEntity(spawner.getEntityId(), entityId, false);
            player.sendPacket(spawnEntity);
            player.sendPacket(metadata);
            player.sendPacket(attachEntity);
        }
        Main.ITEM_ENTITIES.add(this);
        lastSpawnTime = System.currentTimeMillis();
    }
}
