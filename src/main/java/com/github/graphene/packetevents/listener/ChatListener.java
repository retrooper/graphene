package com.github.graphene.packetevents.listener;

import com.github.graphene.Main;
import com.github.graphene.user.User;
import com.github.graphene.util.ServerUtil;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.chat.Color;
import com.github.retrooper.packetevents.protocol.chat.component.impl.TextComponent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;

public class ChatListener implements PacketListener {
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        User user = (User) event.getPlayer();
        if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
            WrapperPlayClientChatMessage chatMessage = new WrapperPlayClientChatMessage(event);
            String msg = chatMessage.getMessage();
            Main.LOGGER.info(user.getUsername() + ": " + msg);
            //Prefix the display message with the player's name in green, and then a colon and their message in white
            TextComponent displayComponent = TextComponent.builder()
                    .text(user.getUsername()).color(Color.BRIGHT_GREEN).build();
            TextComponent childComponent = TextComponent.builder().text(": " + msg).color(Color.WHITE).build();
            displayComponent.getChildren().add(childComponent);
            //Send it to everyone(including the sender)
            ServerUtil.broadcastMessage(displayComponent);
        }
    }
}
