package com.github.graphene.listener;

import com.github.graphene.Main;
import com.github.graphene.player.Player;
import com.github.graphene.util.ChunkHelper;
import com.github.graphene.util.ServerUtil;
import com.github.graphene.util.entity.EntityInformation;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.netty.buffer.UnpooledByteBufAllocationHelper;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.Enchantment;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTInt;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.Difficulty;
import com.github.retrooper.packetevents.protocol.world.Dimension;
import com.github.retrooper.packetevents.protocol.world.DimensionType;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class JoinManager {
    private final static NBTCompound DIMENSION_NBT;
    private final static NBTCompound DIMENSION_CODEC_NBT;

    static {
        byte[] dimensionBytes = new byte[0];
        try (InputStream dimensionInfo = JoinManager.class.getClassLoader().getResourceAsStream("RawDimensions.bytes")) {
            dimensionBytes = new byte[dimensionInfo.available()];
            dimensionInfo.read(dimensionBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        PacketWrapper<?> dimensionBuffer = PacketWrapper.createUniversalPacketWrapper(UnpooledByteBufAllocationHelper.buffer());
        ByteBufHelper.writeBytes(dimensionBuffer.buffer, dimensionBytes);
        DIMENSION_NBT = dimensionBuffer.readNBT();

        byte[] dimensionCodecBytes = new byte[0];
        try (InputStream dimensionCodecInfo = JoinManager.class.getClassLoader().getResourceAsStream("RawCodec.bytes")) {
            dimensionCodecBytes = new byte[dimensionCodecInfo.available()];
            dimensionCodecInfo.read(dimensionCodecBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        PacketWrapper<?> dimensionCodecBuffer = PacketWrapper.createUniversalPacketWrapper(UnpooledByteBufAllocationHelper.buffer());
        ByteBufHelper.writeBytes(dimensionCodecBuffer.buffer, dimensionCodecBytes);
        DIMENSION_CODEC_NBT = dimensionCodecBuffer.readNBT();
    }

    public static void handleJoin(User user, Player player) {
        Location spawnLocation = new Location(6, 1, 6, 0.0f, 0.0f);
        player.setEntityInformation(new EntityInformation(spawnLocation));
        List<String> worldNames = new ArrayList<>();
        worldNames.add("minecraft:overworld");
        worldNames.add("minecraft:the_nether");
        worldNames.add("minecraft:the_end");
        long hashedSeed = 0L;
        //Send join game packet
        Dimension dimension = new Dimension(DimensionType.OVERWORLD, DIMENSION_NBT);
        NBTCompound attributes = dimension.getAttributes().get();
        attributes.setTag("min_y", new NBTInt(0));
        attributes.setTag("height", new NBTInt(256));
        WrapperPlayServerJoinGame joinGame = new WrapperPlayServerJoinGame(player.getEntityId(),
                false, player.getGameMode(), player.getPreviousGameMode(),
                worldNames, DIMENSION_CODEC_NBT, dimension, Difficulty.NORMAL, worldNames.get(0), hashedSeed, Main.MAX_PLAYERS,
                20, 20, false, true, false, true);
        user.sendPacket(joinGame);

        //Send optional plugin message packet with our server's brand
        String brandName = "Graphene";
        PacketWrapper<?> brandNameBuffer = PacketWrapper.createUniversalPacketWrapper(UnpooledByteBufAllocationHelper.buffer());
        brandNameBuffer.writeByteArray(brandName.getBytes());
        byte[] brandNameBytes = ByteBufHelper.copyBytes(brandNameBuffer.getBuffer());
        WrapperPlayServerPluginMessage pluginMessage = new WrapperPlayServerPluginMessage("minecraft:brand", brandNameBytes);
        player.sendPacket(pluginMessage);
        //Send player abilities
        WrapperPlayServerPlayerAbilities abilities = new WrapperPlayServerPlayerAbilities(false, false, false, false, 0.05f, 0.1f);
        player.sendPacket(abilities);

        ServerUtil.handlePlayerJoin(user, player);

        //Send held item change
        player.setHotbarIndex(0, ItemStack.builder().type(ItemTypes.DIAMOND_SWORD).amount(1).build());
        player.setHotbarIndex(1, ItemStack.builder().type(ItemTypes.DIAMOND_PICKAXE).amount(1)
                .addEnchantment(Enchantment.builder().type(EnchantmentTypes.BLOCK_EFFICIENCY).level(3).build())
                .build());
        player.setHotbarIndex(2, ItemStack.builder().type(ItemTypes.COBBLESTONE).amount(64).build());
        player.updateHotbar();
        WrapperPlayServerHeldItemChange heldItemChange = new WrapperPlayServerHeldItemChange(0);
        player.sendPacket(heldItemChange);
        //Send entity status
        WrapperPlayServerEntityStatus entityStatus = new WrapperPlayServerEntityStatus(player.getEntityId(), 28);
        player.sendPacket(entityStatus);

        Main.MAIN_WORLD.createWorldForUser(player);

        //Actually spawn them into the world
        WrapperPlayServerPlayerPositionAndLook positionAndLook =
                new WrapperPlayServerPlayerPositionAndLook(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ(), spawnLocation.getYaw(), spawnLocation.getPitch(), (byte) 0, 0, true);
        player.sendPacket(positionAndLook);

        EntityHandler.onLogin(player);
    }

}
