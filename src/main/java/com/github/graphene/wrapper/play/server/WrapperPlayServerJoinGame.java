package com.github.graphene.wrapper.play.server;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.impl.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WrapperPlayServerJoinGame extends PacketWrapper<WrapperPlayServerJoinGame> {
    private int entityID;
    private boolean hardcore;
    private GameMode gameMode;

    @Nullable
    private GameMode previousGameMode;

    private List<String> worldNames;
    private NBTCompound dimensionCodec;
    private NBTCompound dimension;
    private String worldName;
    private long hashedSeed;
    private int maxPlayers;
    private int viewDistance;
    private int simulationDistance;
    private boolean reducedDebugInfo;
    private boolean enableRespawnScreen;
    private boolean isDebug;
    private boolean isFlat;

    public WrapperPlayServerJoinGame(PacketSendEvent event) {
        super(event);
    }

    //TODO Constructor
    public WrapperPlayServerJoinGame(int entityID, boolean isHardcore, GameMode gamemode,
                                     GameMode previousGameMode,
                                     List<String> worldNames, NBTCompound dimensionCodec, NBTCompound dimension,
                                     String worldName, long hashedSeed, int maxPlayers,
                                     int viewDistance, int simulationDistance,
                                     boolean reducedDebugInfo, boolean enableRespawnScreen,
                                     boolean isDebug, boolean isFlat) {
        super(PacketType.Play.Server.JOIN_GAME);
        this.entityID = entityID;
        this.hardcore = isHardcore;
        this.gameMode = gamemode;
        this.previousGameMode = previousGameMode;
        this.worldNames = worldNames;
        this.dimensionCodec = dimensionCodec;
        this.dimension = dimension;
        this.worldName = worldName;
        this.hashedSeed = hashedSeed;
        this.maxPlayers = maxPlayers;
        this.viewDistance = viewDistance;
        this.simulationDistance = simulationDistance;
        this.reducedDebugInfo = reducedDebugInfo;
        this.enableRespawnScreen = enableRespawnScreen;
        this.isDebug = isDebug;
        this.isFlat = isFlat;
    }

    @Override
    public void readData() {
        entityID = readInt();
        hardcore = readBoolean();
        gameMode = GameMode.values()[readByte()];
        int previousGameModeId = readByte();
        if (previousGameModeId != -1) {
            previousGameMode = GameMode.values()[previousGameModeId];
        }
        else {
            previousGameMode = null;
        }
        int worldCount = readVarInt();
        worldNames = new ArrayList<>(worldCount);
        for (int i = 0; i < worldCount; i++) {
            worldNames.add(readString());
        }
        dimensionCodec = readNBT();
        dimension = readNBT();
        worldName = readString();
        hashedSeed = readLong();
        maxPlayers = readVarInt();
        viewDistance = readVarInt();
        simulationDistance = readVarInt();
        reducedDebugInfo = readBoolean();
        enableRespawnScreen = readBoolean();
        isDebug = readBoolean();
        isFlat = readBoolean();
    }

    @Override
    public void readData(WrapperPlayServerJoinGame wrapper) {
        entityID = wrapper.entityID;
        hardcore = wrapper.hardcore;
        gameMode = wrapper.gameMode;
        previousGameMode = wrapper.previousGameMode;
        worldNames = wrapper.worldNames;
        dimensionCodec = wrapper.dimensionCodec;
        dimension = wrapper.dimension;
        worldName = wrapper.worldName;
        hashedSeed = wrapper.hashedSeed;
        maxPlayers = wrapper.maxPlayers;
        viewDistance = wrapper.viewDistance;
        simulationDistance = wrapper.simulationDistance;
        reducedDebugInfo = wrapper.reducedDebugInfo;
        enableRespawnScreen = wrapper.enableRespawnScreen;
        isDebug = wrapper.isDebug;
        isFlat = wrapper.isFlat;
    }

    @Override
    public void writeData() {
        writeInt(entityID);
        writeBoolean(hardcore);
        writeByte(gameMode.ordinal());
        if (previousGameMode == null) {
            writeByte(-1);
        }
        else {
            writeByte(previousGameMode.ordinal());
        }
        writeVarInt(worldNames.size());
        for (String worldName : worldNames) {
            writeString(worldName);
        }
        writeNBT(dimensionCodec);
        writeNBT(dimension);
        writeString(worldName);
        writeLong(hashedSeed);
        writeVarInt(maxPlayers);
        writeVarInt(viewDistance);
        writeVarInt(simulationDistance);
        writeBoolean(reducedDebugInfo);
        writeBoolean(enableRespawnScreen);
        writeBoolean(isDebug);
        writeBoolean(isFlat);
    }

    public int getEntityId() {
        return entityID;
    }

    public void setEntityId(int entityID) {
        this.entityID = entityID;
    }

    public boolean isHardcore() {
        return hardcore;
    }

    public void setHardcore(boolean hardcore) {
        this.hardcore = hardcore;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    @Nullable
    public GameMode getPreviousGameMode() {
        return previousGameMode;
    }

    public void setPreviousGameMode(@Nullable GameMode previousGameMode) {
        this.previousGameMode = previousGameMode;
    }

    public List<String> getWorldNames() {
        return worldNames;
    }

    public void setWorldNames(List<String> worldNames) {
        this.worldNames = worldNames;
    }

    public NBTCompound getDimensionCodec() {
        return dimensionCodec;
    }

    public void setDimensionCodec(NBTCompound dimensionCodec) {
        this.dimensionCodec = dimensionCodec;
    }

    public NBTCompound getDimension() {
        return dimension;
    }

    public void setDimension(NBTCompound dimension) {
        this.dimension = dimension;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public long getHashedSeed() {
        return hashedSeed;
    }

    public void setHashedSeed(long hashedSeed) {
        this.hashedSeed = hashedSeed;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(int viewDistance) {
        this.viewDistance = viewDistance;
    }

    public int getSimulationDistance() {
        return simulationDistance;
    }

    public void setSimulationDistance(int simulationDistance) {
        this.simulationDistance = simulationDistance;
    }

    public boolean isReducedDebugInfo() {
        return reducedDebugInfo;
    }


    public void setReducedDebugInfo(boolean reducedDebugInfo) {
        this.reducedDebugInfo = reducedDebugInfo;
    }

    public boolean isRespawnScreenEnabled() {
        return enableRespawnScreen;
    }

    public void setRespawnScreenEnabled(boolean enableRespawnScreen) {
        this.enableRespawnScreen = enableRespawnScreen;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public void setDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }

    public boolean isFlat() {
        return isFlat;
    }

    public void setFlat(boolean isFlat) {
        this.isFlat = isFlat;
    }
}
