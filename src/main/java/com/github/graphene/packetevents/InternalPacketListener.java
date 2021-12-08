package com.github.graphene.packetevents;

import com.github.graphene.Graphene;
import com.github.graphene.user.User;
import com.github.graphene.wrapper.play.server.WrapperStatusServerResponse;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.impl.PacketSendEvent;
import com.github.retrooper.packetevents.manager.player.attributes.TabCompleteAttribute;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.handshaking.client.WrapperHandshakingClientHandshake;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTabComplete;
import com.github.retrooper.packetevents.wrapper.status.client.WrapperStatusClientPing;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerPong;
import com.google.gson.JsonObject;
import io.netty.channel.Channel;

import java.util.Optional;

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
                }
                else if (event.getPacketType() == PacketType.Status.Client.PING) {
                    WrapperStatusClientPing ping = new WrapperStatusClientPing(event);
                    long time = ping.getTime();

                    WrapperStatusServerPong pong = new WrapperStatusServerPong(time);
                    PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), pong);
                    //Close channel
                    //TODO Contribute some better way in packetevents to close the channel
                    ((Channel)event.getChannel().rawChannel()).close();
                }
                break;
            case LOGIN:
                if (event.getPacketType() == PacketType.Login.Client.LOGIN_START) {
                    WrapperLoginClientLoginStart start = new WrapperLoginClientLoginStart(event);
                    //Map the player usernames with their netty channels
                    PacketEvents.getAPI().getPlayerManager().CHANNELS.put(start.getUsername(), event.getChannel());
                    user.setUsername(start.getUsername());
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
