package com.github.graphene.packetevents.listener;

import com.github.graphene.Main;
import com.github.graphene.user.User;
import com.github.graphene.wrapper.play.server.WrapperStatusServerResponse;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.status.client.WrapperStatusClientPing;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerPong;
import com.google.gson.JsonObject;

public class ServerListPingListener implements PacketListener {
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        User user = (User) event.getPlayer();
        if (event.getPacketType() == PacketType.Status.Client.REQUEST) {
            //This is invoked when the client opens up or refreshes the multiplayer server list menu.
            JsonObject responseComponent = new JsonObject();

            JsonObject versionComponent = new JsonObject();
            versionComponent.addProperty("name", Main.SERVER_VERSION_NAME);
            versionComponent.addProperty("protocol", Main.SERVER_PROTOCOL_VERSION);
            //Add sub component
            responseComponent.add("version", versionComponent);

            JsonObject playersComponent = new JsonObject();
            playersComponent.addProperty("max", Main.MAX_PLAYERS);
            playersComponent.addProperty("online", Main.USERS.size());
            //Add sub component
            responseComponent.add("players", playersComponent);

            JsonObject descriptionComponent = new JsonObject();
            descriptionComponent.addProperty("text", Main.SERVER_DESCRIPTION);
            //Add sub component
            responseComponent.add("description", descriptionComponent);
            //We respond by sending them information about the server.
            WrapperStatusServerResponse response = new WrapperStatusServerResponse(responseComponent);
            user.sendPacket(response);
        } else if (event.getPacketType() == PacketType.Status.Client.PING) {
            //The client sends us this to inform us they successfully received our response with data about the server.
            //We just respond by sending them the same packet.
            WrapperStatusClientPing ping = new WrapperStatusClientPing(event);
            long time = ping.getTime();
            WrapperStatusServerPong pong = new WrapperStatusServerPong(time);
            PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), pong);
            user.forceDisconnect();
        }
    }
}