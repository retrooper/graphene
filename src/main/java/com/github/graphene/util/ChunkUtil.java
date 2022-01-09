package com.github.graphene.util;

import com.github.graphene.user.User;
import com.github.graphene.wrapper.play.server.WrapperPlayServerChunkData_1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.DataPalette;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;

import java.util.BitSet;

public class ChunkUtil {
    public static void sendChunksLine(User user, int chunkWidth, int chunkLength) {
        for (int i = 0; i < chunkLength; i++) {
            for (int j = 0; j < chunkWidth; j++) {
                final int x = j;
                final int z = i;
                sendChunks(user, x, z);
            }
        }
    }

    public static void sendChunks(User user, int chunkX, int chunkZ) {
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
