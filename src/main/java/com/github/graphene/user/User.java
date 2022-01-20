package com.github.graphene.user;

import com.github.graphene.Main;
import com.github.graphene.util.entity.ClientSettings;
import com.github.graphene.util.entity.EntityInformation;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.netty.channel.ChannelAbstract;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.GameProfile;
import com.github.retrooper.packetevents.protocol.player.HumanoidArm;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerDisconnect;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import io.netty.channel.Channel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.UUID;

public class User {
    private final Channel channel;
    private ConnectionState state;
    private ClientVersion clientVersion;
    private final int entityID = Main.ENTITIES++;
    private GameMode gameMode = GameMode.SURVIVAL;
    private GameMode previousGameMode = null;
    private String serverID = "";
    private byte[] verifyToken;
    private String serverAddress;
    private GameProfile gameProfile;
    private long lastKeepAliveTime = Long.MAX_VALUE;
    private long keepAliveTimer = System.currentTimeMillis();
    private long latency = 0L;
    private long sendKeepAliveTime = 0L;
    private EntityInformation entityInformation;
    private ClientSettings clientSettings;
    public final ItemStack[] inventory = new ItemStack[45];
    public int currentSlot;

    public User(Channel channel, ConnectionState state) {
        this.channel = channel;
        this.state = state;
        this.clientSettings = new ClientSettings("", 0, new HashSet<>(), WrapperPlayClientSettings.ChatVisibility.FULL, HumanoidArm.RIGHT);
    }

    @Nullable
    public ItemStack getCurrentItem() {
        return getHotbarIndex(currentSlot);
    }

    public void setCurrentItem(@Nullable ItemStack item) {
        setHotbarIndex(currentSlot, item);
    }

    @Nullable
    public ItemStack getHotbarIndex(int slot) {
        return inventory[slot + 36];
    }

    public void setHotbarIndex(int slot, @Nullable ItemStack itemStack) {
        inventory[slot + 36] = itemStack;
    }

    public GameProfile getGameProfile() {
        return gameProfile;
    }

    public void setGameProfile(GameProfile gameProfile) {
        this.gameProfile = gameProfile;
    }

    public String getUsername() {
        return gameProfile.getName();
    }

    public Channel getChannel() {
        return channel;
    }

    public ConnectionState getState() {
        return state;
    }

    public void setState(ConnectionState state) {
        this.state = state;
    }

    public ClientVersion getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(ClientVersion clientVersion) {
        this.clientVersion = clientVersion;
    }

    public int getEntityId() {
        return entityID;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public GameMode getPreviousGameMode() {
        return previousGameMode;
    }

    public void setPreviousGameMode(GameMode previousGameMode) {
        this.previousGameMode = previousGameMode;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public String getServerId() {
        return serverID;
    }

    public void setServerId(String serverID) {
        this.serverID = serverID;
    }

    public byte[] getVerifyToken() {
        return verifyToken;
    }

    public void setVerifyToken(byte[] verifyToken) {
        this.verifyToken = verifyToken;
    }

    public InetSocketAddress getAddress() {
        return (InetSocketAddress) channel.remoteAddress();
    }

    public void sendPacket(PacketWrapper<?> wrapper) {
        ChannelAbstract ch = PacketEvents.getAPI().getNettyManager().wrapChannel(channel);
        PacketEvents.getAPI().getPlayerManager().sendPacket(ch, wrapper);
    }

    public void sendMessage(Component component) {
        WrapperPlayServerChatMessage chatMessage = new WrapperPlayServerChatMessage(component, WrapperPlayServerChatMessage.ChatPosition.CHAT,
                new UUID(0L, 0L));
        sendPacket(chatMessage);
    }

    public void sendMessage(String message) {
        //TODO Some improvements
        sendMessage(Component.text(message).color(NamedTextColor.WHITE).asComponent());
    }

    public void forceDisconnect() {
        channel.close();
    }

    private void kickLogin(Component component) {
        WrapperLoginServerDisconnect disconnect = new WrapperLoginServerDisconnect(component.toString());
        PacketEvents.getAPI().getPlayerManager().sendPacket(this, disconnect);
        forceDisconnect();
    }

    private void kickPlay(Component component) {
        WrapperPlayServerDisconnect disconnect = new WrapperPlayServerDisconnect(component);
        PacketEvents.getAPI().getPlayerManager().sendPacket(this, disconnect);
        forceDisconnect();
    }

    public void kick(Component component) {
        ConnectionState state = PacketEvents.getAPI().getPlayerManager().getConnectionState(this);
        switch (state) {
            case HANDSHAKING:
            case STATUS:
                forceDisconnect();
                break;
            case LOGIN:
                kickLogin(component);
                break;
            case PLAY:
                kickPlay(component);
                break;

        }
    }

    public void kick(String reason) {
        Component component = Component.text(reason).color(NamedTextColor.DARK_RED).asComponent();
        kick(component);
    }

    public long getLastKeepAliveTime() {
        return lastKeepAliveTime;
    }

    public void setLastKeepAliveTime(long lastKeepAliveTime) {
        this.lastKeepAliveTime = lastKeepAliveTime;
    }

    public long getKeepAliveTimer() {
        return keepAliveTimer;
    }

    public void setKeepAliveTimer(long keepAliveTimer) {
        this.keepAliveTimer = keepAliveTimer;
    }

    public long getLatency() {
        return latency;
    }

    public void setLatency(long latency) {
        this.latency = latency;
    }

    public long getSendKeepAliveTime() {
        return sendKeepAliveTime;
    }

    public void setSendKeepAliveTime(long sendKeepAliveTime) {
        this.sendKeepAliveTime = sendKeepAliveTime;
    }

    public EntityInformation getEntityInformation() {
        return entityInformation;
    }

    public void setEntityInformation(EntityInformation entityInformation) {
        this.entityInformation = entityInformation;
    }

    public ClientSettings getClientSettings() {
        return clientSettings;
    }

    public void setClientSettings(ClientSettings clientSettings) {
        this.clientSettings = clientSettings;
    }

    public void updateHotbar() {
        for (int i = 0; i < inventory.length; i++) {
            ItemStack item = inventory[i];
            if (item == null) {
                item = ItemStack.builder().type(ItemTypes.AIR).amount(64).build();
            }
            WrapperPlayServerSetSlot setSlot = new WrapperPlayServerSetSlot(0, (int) (Math.random() * 1000), i, item);
            sendPacket(setSlot);
        }
    }
}
