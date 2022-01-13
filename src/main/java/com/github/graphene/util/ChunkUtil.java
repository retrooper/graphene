package com.github.graphene.util;

import com.github.graphene.Main;
import com.github.graphene.user.User;
import com.github.graphene.wrapper.play.server.WrapperPlayServerChunkData_1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.DataPalette;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.MathUtil;
import com.github.retrooper.packetevents.util.Vector3i;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class ChunkUtil {
    @Nullable
    public static Column getColumnByPosition(Vector3i blockPosition) {
        int chunkX = MathUtil.floor(blockPosition.getX() / 16.0);
        int chunkZ = MathUtil.floor(blockPosition.getZ() / 16.0);
        Vector2i chunkCoord = new Vector2i(chunkX, chunkZ);
        for (Vector2i c : Main.CHUNKS.keySet()) {
            if (c.equals(chunkCoord)) {
                return Main.CHUNKS.get(c);
            }
        }
        return null;
    }

    @Nullable
    public static BaseChunk getChunkByPosition(Vector3i blockPosition) {
        Column column = getColumnByPosition(blockPosition);
        if (column != null) {
            int chunkIndex = MathUtil.floor(blockPosition.getY() / 16.0);
            return column.getChunks()[chunkIndex];
        }
        return null;
    }

    @Nullable
    public static WrappedBlockState getBlockStateByPosition(Vector3i blockPosition) {
        Column column = getColumnByPosition(blockPosition);
        if (column != null) {
            int chunkIndex = MathUtil.floor(blockPosition.getY() / 16.0);
            BaseChunk chunk = column.getChunks()[chunkIndex];
            int y = blockPosition.getY();
            for (int i = 0; i < chunkIndex; i++) {
                y -= 16;
            }
            //TODO Deal with far x and z
            return chunk.get(blockPosition.getX(), y, blockPosition.getZ());
        }
        return null;
    }

    public static void setBlockStateByPosition(Vector3i blockPosition, WrappedBlockState blockState) {
        //TODO Sometimes placing block on top of grass just makes it dissapear, its probably the y + 1 thing thatwe removed
        Column column = getColumnByPosition(blockPosition);
        if (column != null) {
            int chunkIndex = MathUtil.floor(blockPosition.getY() / 16.0);
            BaseChunk chunk = column.getChunks()[chunkIndex];
            int y = blockPosition.getY();
            for (int i = 0; i < chunkIndex; i++) {
                y -= 16;
            }
            //TODO Deal with far x and z
            chunk.set(blockPosition.getX(), y, blockPosition.getZ(), blockState.getGlobalId());
        }
    }

    public static void sendChunkColumns(User user) {
        for (Vector2i chunkCoords : Main.CHUNKS.keySet()) {
            Column column = Main.CHUNKS.get(chunkCoords);
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
            DataPalette biomePalette = DataPalette.createForBiome();
            biomePalette.set(0, 0, 0, 0);
            DataPalette chunkPalette = DataPalette.createForChunk();
            int count = 0;
            WrappedBlockState blockState = WrappedBlockState.getByString("minecraft:grass_block[snowy=false]");
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        //We only want blocks on the lowest chunk
                        if (y == 0 && i == 0) {
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
        Column column = new Column(chunkX, chunkZ, true, chunks, new TileEntity[0]);
        if (store) {
            Main.CHUNKS.put(new Vector2i(chunkX, chunkZ), column);
        }
        return column;
    }

    private static void sendChunks(User user, int chunkX, int chunkZ) {
        //These chunks go upwards
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
                        //We only want blocks on the lowest chunk
                        if (y == 0 && i == 0) {
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
        Column column = new Column(chunkX, chunkZ, true, chunks, new TileEntity[0]);
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
