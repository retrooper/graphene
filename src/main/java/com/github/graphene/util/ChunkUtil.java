package com.github.graphene.util;

import com.github.graphene.Main;
import com.github.graphene.player.Player;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.DataPalette;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ChunkUtil {
    private static final int MIN_HEIGHT = 0;
    private static final int MAX_HEIGHT = 255;

    public static long chunkPositionToLong(int x, int z) {
        return ((x & 0xFFFFFFFFL) << 32L) | (z & 0xFFFFFFFFL);
    }

    @Nullable
    public static Column getColumnByPosition(Vector3i blockPosition) {
        return Main.CHUNKS.get(chunkPositionToLong(blockPosition.getX() >> 4, blockPosition.getZ() >> 4));
    }

    @Nullable
    public static BaseChunk getChunkByPosition(Vector3i blockPosition) {
        int y = blockPosition.getY();
        y -= MIN_HEIGHT;
        Column column = getColumnByPosition(blockPosition);
        if (column != null) {
            return column.getChunks()[y >> 4];
        }
        return null;
    }

    @Nullable
    public static WrappedBlockState getBlockStateByPosition(Vector3i blockPosition) {
        BaseChunk chunk = getChunkByPosition(blockPosition);
        int secX = blockPosition.getX() & 15;
        int secY = blockPosition.getY() & 15;
        int secZ = blockPosition.getZ() & 15;
        return chunk.get(secX, secY, secZ);
    }

    public static void setBlockStateByPosition(Vector3i blockPosition, WrappedBlockState blockState) {
        BaseChunk chunk = getChunkByPosition(blockPosition);
        int secX = blockPosition.getX() & 15;
        int secY = blockPosition.getY() & 15;
        int secZ = blockPosition.getZ() & 15;
        chunk.set(secX, secY, secZ, blockState.getGlobalId());
    }

    public static void sendChunkColumns(Player player) {
        for (Column column : Main.CHUNKS.values()) {
            WrapperPlayServerChunkData chunkData = new WrapperPlayServerChunkData(column);
            player.sendPacket(chunkData);
        }
    }

    public static List<Column> generateChunkColumns(int width, int length, boolean store) {
        List<Column> columns = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < width; j++) {
                columns.add(generateChunkColumn(j, i, store));
            }
        }
        return columns;
    }

    public static Column generateChunkColumn(int chunkX, int chunkZ, boolean store) {
        Chunk_v1_18[] chunks = new Chunk_v1_18[16];
        for (int i = 0; i < chunks.length; i++) {
            //chunks[i] = new Chunk_v1_8(false);
            DataPalette biomePalette = DataPalette.createForBiome();
            biomePalette.set(0, 0, 0, 0);
            DataPalette chunkPalette = DataPalette.createForChunk();
            int count = 0;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        //We only want blocks on the lowest chunk
                        if (y == 0 && i == 0) {
                            //chunks[i].set(x, y, z, 1);
                            chunkPalette.set(x, y, z, 1);
                        } else {
                            //chunks[i].set(x, y, z, 0);
                            chunkPalette.set(x, y, z, 0);
                        }
                        count++;
                    }
                }
            }
            chunks[i] = new Chunk_v1_18(count, chunkPalette, biomePalette);
        }
        Column column = new Column(chunkX, chunkZ, true, chunks, new TileEntity[0]);
        if (store) {
            Main.CHUNKS.put(chunkPositionToLong(chunkX, chunkZ), column);
        }
        return column;
    }

    /*private static void sendChunks(Player player, int chunkX, int chunkZ) {
        //These chunks go upwards
        Chunk_v1_18[] chunks = new Chunk_v1_18[16];
        for (int i = 0; i < chunks.length; i++) {
            DataPalette biomePalette = DataPalette.createForBiome();
            biomePalette.set(0, 0, 0, 0);
            DataPalette chunkPalette = DataPalette.createForChunk();
            int count = 0;
            //WrappedBlockState blockState = WrappedBlockState.getByString("minecraft:grass_block[snowy=false]");
            //WrappedBlockState blockState = WrappedBlockState.getByGlobalId(1);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        //We only want blocks on the lowest chunk
                        if (y == 0 && i == 0) {
                            chunkPalette.set(x, y, z, 1);
                        } else {
                            chunkPalette.set(x, y, z, 0);
                        }
                        count++;
                    }
                }
            }
            chunks[i] = new Chunk_v1_18(count, chunkPalette, biomePalette);
        }
        Column column = new Column(chunkX, chunkZ, true, chunks, new TileEntity[0]);
        WrapperPlayServerChunkData chunkData = new WrapperPlayServerChunkData(column);
        player.sendPacket(chunkData);
    }*/
}
