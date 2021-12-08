package com.github.graphene.packetevents;

import com.github.graphene.Graphene;
import com.github.graphene.user.User;
import com.github.graphene.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.graphene.wrapper.play.server.WrapperStatusServerResponse;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.impl.PacketSendEvent;
import com.github.retrooper.packetevents.manager.player.attributes.TabCompleteAttribute;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.nbt.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.handshaking.client.WrapperHandshakingClientHandshake;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerLoginSuccess;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTabComplete;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import com.github.retrooper.packetevents.wrapper.status.client.WrapperStatusClientPing;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerPong;
import com.google.gson.JsonObject;
import io.netty.channel.Channel;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class InternalPacketListener implements PacketListener {
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        User user = (User) event.getPlayer();
        assert user != null;
        switch (event.getConnectionState()) {
            case HANDSHAKING:
                if (event.getPacketType() == PacketType.Handshaking.Client.HANDSHAKE) {
                    WrapperHandshakingClientHandshake handshake = new WrapperHandshakingClientHandshake(event);
                    ClientVersion clientVersion = handshake.getClientVersion();

                    //Update client version for this event call
                    event.setClientVersion(clientVersion);

                    user.setClientVersion(clientVersion);

                    //Map netty channel with the client version.
                    PacketEvents.getAPI().getPlayerManager().CLIENT_VERSIONS.put(event.getChannel(), clientVersion);

                    //Transition into the LOGIN OR STATUS connection state
                    PacketEvents.getAPI().getPlayerManager().changeConnectionState(event.getChannel(), handshake.getNextConnectionState());
                }
                break;
            case STATUS:
                if (event.getPacketType() == PacketType.Status.Client.REQUEST) {
                    JsonObject responseComponent = new JsonObject();

                    JsonObject versionComponent = new JsonObject();
                    versionComponent.addProperty("name", Graphene.SERVER_VERSION_NAME);
                    versionComponent.addProperty("protocol", Graphene.SERVER_PROTOCOL_VERSION);
                    //Add sub component
                    responseComponent.add("version", versionComponent);

                    JsonObject playersComponent = new JsonObject();
                    playersComponent.addProperty("max", Graphene.MAX_PLAYERS);
                    playersComponent.addProperty("online", Graphene.ONLINE_PLAYERS);
                    //Add sub component
                    responseComponent.add("players", playersComponent);

                    JsonObject descriptionComponent = new JsonObject();
                    descriptionComponent.addProperty("text", Graphene.SERVER_DESCRIPTION);
                    //Add sub component
                    responseComponent.add("description", descriptionComponent);

                    WrapperStatusServerResponse response = new WrapperStatusServerResponse(responseComponent);
                    PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), response);
                } else if (event.getPacketType() == PacketType.Status.Client.PING) {
                    WrapperStatusClientPing ping = new WrapperStatusClientPing(event);
                    long time = ping.getTime();

                    WrapperStatusServerPong pong = new WrapperStatusServerPong(time);
                    PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), pong);
                    //Close channel
                    //TODO Contribute some better way in packetevents to close the channel
                    ((Channel) event.getChannel().rawChannel()).close();
                }
                break;
            case LOGIN:
                if (event.getPacketType() == PacketType.Login.Client.LOGIN_START) {
                    WrapperLoginClientLoginStart start = new WrapperLoginClientLoginStart(event);
                    //Map the player usernames with their netty channels
                    PacketEvents.getAPI().getPlayerManager().CHANNELS.put(start.getUsername(), event.getChannel());
                    user.setUsername(start.getUsername());
                    UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + user.getUsername()).getBytes(StandardCharsets.UTF_8));
                    user.setUUID(uuid);

                    WrapperLoginServerLoginSuccess loginSuccess = new WrapperLoginServerLoginSuccess(user.getUUID(), user.getUsername());
                    PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), loginSuccess);
                    System.out.println("[Graphene] " + user.getUsername() + " has logged in.");
                    List<String> worldNames = new ArrayList<>();
                    worldNames.add("world");
                    worldNames.add("world2");
                    worldNames.add("world3");

                    long hashedSeed = 100L;
                    NBTCompound dimension = new NBTCompound();
                    dimension.setTag("piglin_safe", new NBTByte((byte) 1));
                    dimension.setTag("natural", new NBTByte((byte) 1));
                    dimension.setTag("ambient_light", new NBTFloat(1.0f));
                    dimension.setTag("infiniburn", new NBTString(""));
                    dimension.setTag("respawn_anchor_works", new NBTByte((byte) 1));
                    dimension.setTag("has_skylight", new NBTByte((byte) 1));
                    dimension.setTag("bed_works", new NBTByte((byte) 1));
                    dimension.setTag("effects", new NBTString("minecraft:overworld"));
                    dimension.setTag("has_raids", new NBTByte((byte) 1));
                    dimension.setTag("min_y", new NBTInt(0));
                    dimension.setTag("height", new NBTInt(400));
                    dimension.setTag("logical_height", new NBTInt(256));
                    dimension.setTag("coordinate_scale", new NBTInt(1));
                    dimension.setTag("ultrawarm", new NBTByte((byte) 1));
                    dimension.setTag("has_ceiling", new NBTByte((byte) 1));
                    NBTCompound dimensionCodec = new NBTCompound();
                    NBTCompound dimensionType = new NBTCompound();
                    NBTList<NBTCompound> types = new NBTList<>(NBTType.COMPOUND);
                    NBTCompound type = new NBTCompound();
                    type.setTag("name", new NBTString("minecraft:overworld"));
                    type.setTag("id", new NBTInt(0));
                    type.setTag("element", dimension);
                    types.addTag(type);
                    dimensionType.setTag("type", new NBTString("minecraft:dimension_type"));
                    dimensionType.setTag("value", types);
                    dimensionCodec.setTag("minecraft:dimension_type", dimensionType);

                    NBTCompound biomeRegistry = new NBTCompound();
                    biomeRegistry.setTag("type", new NBTString("minecraft:worldgen/biome"));
                    NBTList<NBTCompound> biomes = new NBTList<>(NBTType.COMPOUND);
                    NBTCompound biomeRegEntry = new NBTCompound();
                    biomeRegEntry.setTag("name", new NBTString("minecraft:ocean"));
                    biomeRegEntry.setTag("id", new NBTInt(0));
                    NBTCompound oceanElement = new NBTCompound();
                    oceanElement.setTag("precipitation", new NBTString("none"));
                    oceanElement.setTag("depth", new NBTFloat(1.6f));
                    oceanElement.setTag("temperature", new NBTFloat(1.0f));
                     oceanElement.setTag("scale", new NBTFloat(1.0f));
                     oceanElement.setTag("downfall", new NBTFloat(0.6f));
                     oceanElement.setTag("category", new NBTString("ocean"));
                     NBTCompound oceanEffects = new NBTCompound();
                     oceanEffects.setTag("sky_color", new NBTInt(8364543));
                    oceanEffects.setTag("water_fog_color", new NBTInt(8364543));
                    oceanEffects.setTag("fog_color", new NBTInt(8364543));
                     oceanEffects.setTag("water_color", new NBTInt(4159204));
                     oceanElement.setTag("effects", oceanEffects);
                    biomeRegEntry.setTag("element", oceanElement);
                    biomes.addTag(biomeRegEntry);
                    biomeRegistry.setTag("value", biomes);
                    dimensionCodec.setTag("minecraft:worldgen/biome", biomeRegistry);

                    WrapperPlayServerJoinGame joinGame = new WrapperPlayServerJoinGame(user.getEntityId(),
                            false, user.getGameMode(), user.getPreviousGameMode(),
                            worldNames, dimensionCodec, dimension, worldNames.get(0), hashedSeed, Graphene.MAX_PLAYERS, 10, 20,
                            true, true, false, true);

                    PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), joinGame);

                    WrapperPlayServerHeldItemChange heldItemChange = new WrapperPlayServerHeldItemChange(0);
                    PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), heldItemChange);

                    System.out.println("send held item change!");

                    int flags = 0x01;
                    flags |= 0x02;
                    flags |= 0x04;
                    flags |= 0x08;
                    flags |= 0x10;
                    WrapperPlayServerPlayerPositionAndLook positionAndLook = new WrapperPlayServerPlayerPositionAndLook(0, 0, 0, 0.0f, 0.0f, flags, 0);
                    PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), positionAndLook);
                    System.out.println("send position and look!");
                }
                break;
            case PLAY:
                if (event.getPacketType() == PacketType.Play.Client.TAB_COMPLETE) {
                    WrapperPlayClientTabComplete tabComplete = new WrapperPlayClientTabComplete(event);
                    String text = tabComplete.getText();
                    TabCompleteAttribute tabCompleteAttribute =
                            PacketEvents.getAPI().getPlayerManager().getAttributeOrDefault(user.getUUID(),
                                    TabCompleteAttribute.class,
                                    new TabCompleteAttribute());
                    tabCompleteAttribute.setInput(text);
                    Optional<Integer> transactionID = tabComplete.getTransactionId();
                    transactionID.ifPresent(tabComplete::setTransactionId);
                }
                else if (event.getPacketType() == PacketType.Play.Client.CLIENT_SETTINGS) {
                    WrapperPlayClientSettings settings = new WrapperPlayClientSettings(event);
                    System.out.println("got settings, hand: " + settings.getHand());
                }
                break;
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Login.Server.LOGIN_SUCCESS) {
            //Transition into the PLAY connection state
            PacketEvents.getAPI().getPlayerManager().changeConnectionState(event.getChannel(), ConnectionState.PLAY);
        }
    }
}
