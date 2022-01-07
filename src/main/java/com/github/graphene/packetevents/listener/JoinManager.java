package com.github.graphene.packetevents.listener;

import com.github.graphene.Graphene;
import com.github.graphene.packetevents.manager.netty.ByteBufUtil;
import com.github.graphene.user.User;
import com.github.graphene.util.ChunkUtil;
import com.github.graphene.util.ServerUtil;
import com.github.graphene.util.entity.EntityInformation;
import com.github.graphene.wrapper.play.server.WrapperPlayServerChunkData_1_18;
import com.github.graphene.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.Enchantment;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.world.Difficulty;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.DataPalette;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class JoinManager {
    private final static NBTCompound DIMENSION;
    private final static NBTCompound DIMENSION_CODEC;

    static {
        byte[] dimensionBytes = new byte[0];
        try (InputStream dimensionInfo = JoinManager.class.getClassLoader().getResourceAsStream("RawDimensions.bytes")) {
            dimensionBytes = new byte[dimensionInfo.available()];
            dimensionInfo.read(dimensionBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        PacketWrapper<?> dimensionBuffer = PacketWrapper.createUniversalPacketWrapper(ByteBufUtil.buffer());
        dimensionBuffer.buffer.writeBytes(dimensionBytes);
        DIMENSION = dimensionBuffer.readNBT();

        byte[] dimensionCodecBytes = new byte[0];
        try (InputStream dimensionCodecInfo = JoinManager.class.getClassLoader().getResourceAsStream("RawCodec.bytes")) {
            dimensionCodecBytes = new byte[dimensionCodecInfo.available()];
            dimensionCodecInfo.read(dimensionCodecBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        PacketWrapper<?> dimensionCodecBuffer = PacketWrapper.createUniversalPacketWrapper(ByteBufUtil.buffer());
        dimensionCodecBuffer.buffer.writeBytes(dimensionCodecBytes);
        DIMENSION_CODEC = dimensionCodecBuffer.readNBT();
    }

    public static void handleJoin(User user) {
        Graphene.WORKER_THREADS.execute(() -> {
            Location spawnLocation = new Location(6, 16, 6, 0.0f, 0.0f);
            user.setEntityInformation(new EntityInformation(spawnLocation));

            List<String> worldNames = new ArrayList<>();
            worldNames.add("minecraft:overworld");
            worldNames.add("minecraft:the_nether");
            worldNames.add("minecraft:the_end");
            long hashedSeed = 0L;
            //Send join game packet
            WrapperPlayServerJoinGame joinGame = new WrapperPlayServerJoinGame(user.getEntityId(),
                    false, user.getGameMode(), user.getPreviousGameMode(),
                    worldNames, DIMENSION_CODEC, DIMENSION, worldNames.get(0), hashedSeed, Graphene.MAX_PLAYERS, 20, 20, false, true, false, true);
            user.sendPacket(joinGame);

            //Send optional plugin message packet with our server's brand
            String brandName = "Graphene";
            PacketWrapper<?> brandNameBuffer = PacketWrapper.createUniversalPacketWrapper(ByteBufUtil.buffer());
            brandNameBuffer.writeByteArray(brandName.getBytes());
            byte[] brandNameBytes = PacketEvents.getAPI().getNettyManager().asByteArray(brandNameBuffer.getBuffer());
            WrapperPlayServerPluginMessage pluginMessage = new WrapperPlayServerPluginMessage("minecraft:brand", brandNameBytes);
            user.sendPacket(pluginMessage);
            //Send server difficulty packet
            WrapperPlayServerDifficulty difficulty = new WrapperPlayServerDifficulty(Difficulty.HARD, false);
            user.sendPacket(difficulty);
            //Send player abilities
            WrapperPlayServerPlayerAbilities abilities = new WrapperPlayServerPlayerAbilities(true, false, true, true, 0.05f, 0.1f);
            user.sendPacket(abilities);

            ServerUtil.handlePlayerJoin(user);

            //Send held item change
            WrapperPlayServerHeldItemChange heldItemChange = new WrapperPlayServerHeldItemChange(1);
            user.sendPacket(heldItemChange);
            //Send entity status
            WrapperPlayServerEntityStatus entityStatus = new WrapperPlayServerEntityStatus(user.getEntityId(), 28);
            user.sendPacket(entityStatus);

            ChunkUtil.sendChunksLine(user, 2, 2);


            //Actually spawn them into the world
            WrapperPlayServerPlayerPositionAndLook positionAndLook = new WrapperPlayServerPlayerPositionAndLook(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ(), spawnLocation.getYaw(), spawnLocation.getPitch(), (byte) 0, 0, true);
            user.sendPacket(positionAndLook);
            //Additionally give them a diamond sword
            ItemStack sword = ItemStack.builder().type(ItemTypes.DIAMOND_SWORD).amount(ItemTypes.DIAMOND_SWORD.getMaxAmount()).build();
            List<Enchantment> enchantments = new ArrayList<>();
            enchantments.add(Enchantment.builder().type(EnchantmentTypes.FIRE_ASPECT).level(2).build());
            sword.setEnchantments(enchantments);
            WrapperPlayServerSetSlot setSlot = new WrapperPlayServerSetSlot(0, 0, 37, sword);
            user.sendPacket(setSlot);

            EntityHandler.onLogin(user);
        });
    }

}
