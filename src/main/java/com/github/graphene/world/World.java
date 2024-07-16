package com.github.graphene.world;

import com.github.graphene.entity.ItemEntity;
import com.github.graphene.player.Player;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.LightData;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.MathUtil;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.graphene.util.ChunkHelper.chunkPositionToLong;

public class World {
    private final Map<Long, Column> chunkMap = new ConcurrentHashMap<>();
    private final List<ItemEntity> items = new ArrayList<>();

    private final int minHeight;

    public World(int minHeight) {
        this.minHeight = minHeight;
    }

    public World() {
        this(0);
    }

    public List<ItemEntity> getItems() {
        return items;
    }

    @Nullable
    public Column getColumnAt(Vector3i blockPosition) {
        return chunkMap.get(chunkPositionToLong(blockPosition.getX() >> 4, blockPosition.getZ() >> 4));
    }

    @Nullable
    public BaseChunk getChunkAt(Vector3i blockPosition) {
        Column column = getColumnAt(blockPosition);
        if (column != null) {
            int y = blockPosition.getY();
            y -= minHeight;
            return column.getChunks()[y >> 4];
        }
        return null;
    }

    @Nullable
    public WrappedBlockState getBlockStateAt(Vector3i blockPosition) {
        BaseChunk chunk = getChunkAt(blockPosition);
        int secX = blockPosition.getX() & 15;
        int secY = blockPosition.getY() & 15;
        int secZ = blockPosition.getZ() & 15;
        return chunk.get(secX, secY, secZ);
    }

    public WrappedBlockState getBlockStateAt(Vector3d position) {
        return getBlockStateAt(
                new Vector3i(
                        MathUtil.floor(position.x),
                        MathUtil.floor(position.y),
                        MathUtil.floor(position.z)));
    }

    public void setBlockStateAt(Vector3i blockPosition, WrappedBlockState blockState) {
        BaseChunk chunk = getChunkAt(blockPosition);
        int secX = blockPosition.getX() & 15;
        int secY = blockPosition.getY() & 15;
        int secZ = blockPosition.getZ() & 15;
        System.out.println("Block there previously (before internal set): " + chunk.get(secX, secY, secZ));
        chunk.set(secX, secY, secZ, blockState.getGlobalId());
    }

    public void setBlockStateAt(Vector3d position, WrappedBlockState blockState) {
        setBlockStateAt(new Vector3i(
                MathUtil.floor(position.x),
                MathUtil.floor(position.y),
                MathUtil.floor(position.z)), blockState);
    }

    public void presentWorld(Player player) {
        for (Column column : chunkMap.values()) {
            LightData lightData = new LightData(true, new BitSet(), new BitSet(), new BitSet(), new BitSet(), 0, 0, new byte[0][0], new byte[0][0]);
            WrapperPlayServerChunkData chunkData = new WrapperPlayServerChunkData(column, lightData);
            player.sendPacket(chunkData);
        }
    }

    public void generateRectangularWorld(int width, int length) {
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < width; j++) {
                generateChunkColumn(j, i);
            }
        }
    }

    protected void generateChunkColumn(int chunkX, int chunkZ) {
        BaseChunk[] chunks = new BaseChunk[16];
        for (int i = 0; i < chunks.length; i++) {
            chunks[i] = BaseChunk.create();
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        //We only want blocks on the lowest chunk.
                        if (y == 0 && i == 0) {
                            //chunks[i].set(x, y, z, 1);
                            chunks[i].set(x, y, z, 1);
                        } else {
                            //chunks[i].set(x, y, z, 0);
                            chunks[i].set(x, y, z, 0);
                        }
                        ((Chunk_v1_18)chunks[i]).getBiomeData().set(0, 0, 0, 0);
                    }
                }
            }
        }
        Column column = new Column(chunkX, chunkZ, true, chunks, new TileEntity[0]);
        chunkMap.put(chunkPositionToLong(chunkX, chunkZ), column);
    }
}
