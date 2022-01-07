package com.github.graphene.packetevents.listener;

import com.github.graphene.Graphene;
import com.github.graphene.logic.EntityHandler;
import com.github.graphene.packetevents.manager.netty.ByteBufUtil;
import com.github.graphene.user.User;
import com.github.graphene.util.ServerUtil;
import com.github.graphene.util.entity.EntityInformation;
import com.github.graphene.wrapper.play.server.WrapperPlayServerChunkData_1_18;
import com.github.graphene.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.impl.PacketSendEvent;
import com.github.retrooper.packetevents.netty.channel.ChannelAbstract;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.chat.Color;
import com.github.retrooper.packetevents.protocol.chat.component.BaseComponent;
import com.github.retrooper.packetevents.protocol.chat.component.ClickEvent;
import com.github.retrooper.packetevents.protocol.chat.component.impl.TextComponent;
import com.github.retrooper.packetevents.protocol.chat.component.impl.TranslatableComponent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.Enchantment;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.Difficulty;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.DataPalette;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;

public class JoinListener implements PacketListener {
    private static final NBTCompound DIMENSION;
    private static final NBTCompound DIMENSION_CODEC;

    static {
        byte[] dimensionBytes = new byte[0];
        try (InputStream dimensionInfo = Graphene.class.getClassLoader().getResourceAsStream("RawDimensions.bytes")) {
            dimensionBytes = new byte[dimensionInfo.available()];
            dimensionInfo.read(dimensionBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        PacketWrapper<?> dimensionBuffer = PacketWrapper.createUniversalPacketWrapper(ByteBufUtil.buffer());
        dimensionBuffer.buffer.writeBytes(dimensionBytes);
        DIMENSION = dimensionBuffer.readNBT();

        byte[] dimensionCodecBytes = new byte[0];
        try (InputStream dimensionCodecInfo = Graphene.class.getClassLoader().getResourceAsStream("RawCodec.bytes")) {
            dimensionCodecBytes = new byte[dimensionCodecInfo.available()];
            dimensionCodecInfo.read(dimensionCodecBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        PacketWrapper<?> dimensionCodecBuffer = PacketWrapper.createUniversalPacketWrapper(ByteBufUtil.buffer());
        dimensionCodecBuffer.buffer.writeBytes(dimensionCodecBytes);
        DIMENSION_CODEC = dimensionCodecBuffer.readNBT();

    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        User user = (User) event.getPlayer();
        if (event.getPacketType() == PacketType.Login.Server.LOGIN_SUCCESS) {
            //We now set the connection state to PLAY.
            user.setState(ConnectionState.PLAY);

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
            PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), joinGame);

            //Send optional plugin message packet with our server's brand
            String brandName = "Graphene";
            PacketWrapper<?> brandNameBuffer = PacketWrapper.createUniversalPacketWrapper(ByteBufUtil.buffer());
            brandNameBuffer.writeByteArray(brandName.getBytes());
            byte[] brandNameBytes = PacketEvents.getAPI().getNettyManager().asByteArray(brandNameBuffer.getBuffer());
            WrapperPlayServerPluginMessage pluginMessage = new WrapperPlayServerPluginMessage("minecraft:brand", brandNameBytes);
            PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), pluginMessage);
            //Send server difficulty packet
            WrapperPlayServerDifficulty difficulty = new WrapperPlayServerDifficulty(Difficulty.HARD, false);
            PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), difficulty);
            //Send player abilities
            WrapperPlayServerPlayerAbilities abilities = new WrapperPlayServerPlayerAbilities(true, false, true, true, 0.05f, 0.1f);
            PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), abilities);

            ServerUtil.handlePlayerJoin(user);

            //Send held item change
            WrapperPlayServerHeldItemChange heldItemChange = new WrapperPlayServerHeldItemChange(0);
            PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), heldItemChange);
            //Send entity status
            WrapperPlayServerEntityStatus entityStatus = new WrapperPlayServerEntityStatus(user.getEntityId(), 28);
            PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), entityStatus);

            //Send world chunks
            sendChunks(user);


            WrapperPlayServerBlockChange[] changes = new WrapperPlayServerBlockChange[16 * 16];
            int i = 0;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int y = 15;
                    WrappedBlockState state = WrappedBlockState.getByString("minecraft:light_gray_stained_glass");
                    changes[i++] = new WrapperPlayServerBlockChange(new Vector3i(x, y, z), state.getGlobalId());
                }
            }
            for (WrapperPlayServerBlockChange change : changes) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), change);
            }

            //Actually spawn them into the world
            WrapperPlayServerPlayerPositionAndLook positionAndLook = new WrapperPlayServerPlayerPositionAndLook(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ(), spawnLocation.getYaw(), spawnLocation.getPitch(), (byte) 0, 0, true);
            PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), positionAndLook);
            //Additionally give them a diamond sword
            ItemStack sword = ItemStack.builder().type(ItemTypes.DIAMOND_SWORD).amount(ItemTypes.DIAMOND_SWORD.getMaxAmount()).build();
            List<Enchantment> enchantments = new ArrayList<>();
            enchantments.add(Enchantment.builder().type(EnchantmentTypes.FIRE_ASPECT).level(2).build());
            sword.setEnchantments(enchantments);
            WrapperPlayServerSetSlot setSlot = new WrapperPlayServerSetSlot(0, 0, 37, sword);
            PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), setSlot);

            EntityHandler.onLogin(user);
        }
    }

    private void sendChunks(User user) {
        Chunk_v1_18[] chunks = new Chunk_v1_18[16];
        for (int i = 0; i < chunks.length; i++) {
            DataPalette biomePalette = DataPalette.createForBiome();
            biomePalette.set(0, 0, 0, 0);
            DataPalette chunkPalette = DataPalette.createForChunk();
            int count = 0;
            WrappedBlockState blockState = WrappedBlockState.getByString("minecraft:grass_block[snowy=false]");
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        if (i == 0) {
                            chunkPalette.set(x, y, z, blockState.getGlobalId());
                        } else {
                            chunkPalette.set(x, y, z, 0);
                        }
                        count++;
                    }
                }
            }
            chunks[i] = new Chunk_v1_18(count, chunkPalette, biomePalette);
        }
        Column column = new Column(0, 0, true, chunks, new TileEntity[0]);
        WrapperPlayServerChunkData_1_18 chunkData = new WrapperPlayServerChunkData_1_18(column);
        // Should be set to false when updating a chunk instead of sending a new one.
        chunkData.trustEdges = true;
        chunkData.skyLightMask = new BitSet(0);
        chunkData.emptySkyLightMask = new BitSet(0);
        chunkData.blockLightMask = new BitSet(0);

        chunkData.emptyBlockLightMask = new BitSet(0);
        chunkData.skyLightArray = new byte[0][0];//2048 for second
        chunkData.blockLightArray = new byte[0][0];//2048 for second
        user.sendPacket(chunkData);
    }

}
