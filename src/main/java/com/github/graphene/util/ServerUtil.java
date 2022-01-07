package com.github.graphene.util;

import com.github.graphene.Graphene;
import com.github.graphene.user.User;
import com.github.retrooper.packetevents.protocol.chat.Color;
import com.github.retrooper.packetevents.protocol.chat.component.BaseComponent;
import com.github.retrooper.packetevents.protocol.chat.component.ClickEvent;
import com.github.retrooper.packetevents.protocol.chat.component.impl.TextComponent;
import com.github.retrooper.packetevents.protocol.chat.component.impl.TranslatableComponent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;

import java.util.UUID;

public class ServerUtil {
    public static void broadcastMessage(BaseComponent component) {
        WrapperPlayServerChatMessage outChatMessage = new WrapperPlayServerChatMessage(component,
                WrapperPlayServerChatMessage.ChatPosition.CHAT, new UUID(0L, 0L));
        outChatMessage.prepareForSend();
        //Retaining it allows us
        outChatMessage.getBuffer().retain();
        for (User user : Graphene.USERS) {
            user.sendPacket(outChatMessage);
        }
        outChatMessage.getBuffer().release();
    }

    public static void handlePlayerLeave(User user) {
        Graphene.USERS.remove(user);
        TextComponent withComponent = TextComponent.builder().color(Color.YELLOW).text(user.getUsername()).insertion(user.getUsername()).build();
        TranslatableComponent translatableComponent = TranslatableComponent.builder().color(Color.YELLOW).translate("multiplayer.player.left")
                .appendWith(withComponent).build();
        WrapperPlayServerPlayerInfo.PlayerData data = new WrapperPlayServerPlayerInfo.PlayerData(null, user.getGameProfile(), null, -1);
        WrapperPlayServerPlayerInfo removePlayerInfo = new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER, data);
        removePlayerInfo.prepareForSend();
        removePlayerInfo.getBuffer().retain();

        WrapperPlayServerDestroyEntities destroyEntities = new WrapperPlayServerDestroyEntities(user.getEntityId());
        destroyEntities.prepareForSend();
        destroyEntities.getBuffer().retain();

        WrapperPlayServerChatMessage leftMessage =
                new WrapperPlayServerChatMessage(translatableComponent,
                        WrapperPlayServerChatMessage.ChatPosition.CHAT, new UUID(0L, 0L));
        leftMessage.prepareForSend();
        leftMessage.getBuffer().retain();
        for (User player : Graphene.USERS) {
            //Remove this user from everyone's tab list
            player.sendPacket(removePlayerInfo);
            //Destroy this user's entity
            player.sendPacket(destroyEntities);
            //Send a message to everyone that this user has left
            player.sendPacket(leftMessage);
        }
        removePlayerInfo.getBuffer().release();
        destroyEntities.getBuffer().release();
        leftMessage.getBuffer().release();
        Graphene.LOGGER.info(user.getUsername() + " left the server.");
    }

    public static void handlePlayerJoin(User user) {
        Graphene.USERS.add(user);
        ClickEvent clickEvent = new ClickEvent(ClickEvent.ClickType.SUGGEST_COMMAND, "/tell " + user.getUsername() + " Welcome!");
        TextComponent withComponent = TextComponent.builder().color(Color.YELLOW).text(user.getUsername()).insertion(user.getUsername()).clickEvent(clickEvent).build();
        TranslatableComponent translatableComponent = TranslatableComponent.builder().color(Color.YELLOW).translate("multiplayer.player.joined")
                .appendWith(withComponent).build();

        WrapperPlayServerChatMessage loginMessage = new WrapperPlayServerChatMessage(translatableComponent, WrapperPlayServerChatMessage.ChatPosition.CHAT, new UUID(0L, 0L));
        loginMessage.prepareForSend();
        loginMessage.getBuffer().retain();

        BaseComponent otherDisplayName = TextComponent.builder().text(user.getUsername())
                .color(Color.BRIGHT_GREEN).build();
        WrapperPlayServerPlayerInfo.PlayerData nextData =
                new WrapperPlayServerPlayerInfo.PlayerData(otherDisplayName, user.getGameProfile(), user.getGameMode(), 100);
        WrapperPlayServerPlayerInfo nextPlayerInfo =
                new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.ADD_PLAYER, nextData);
        nextPlayerInfo.prepareForSend();
        nextPlayerInfo.getBuffer().retain();

        for (User player : Graphene.USERS) {
            //Send every player the login message
            player.sendPacket(loginMessage);

            //Add this joining user to everyone's tab list
            BaseComponent displayName = TextComponent.builder().text(player.getUsername())
                    .color(Color.DARK_GREEN).build();
            WrapperPlayServerPlayerInfo.PlayerData data = new WrapperPlayServerPlayerInfo.PlayerData(displayName,
                    player.getGameProfile(), player.getGameMode(), 100);
            WrapperPlayServerPlayerInfo playerInfo = new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.ADD_PLAYER, data);
            user.sendPacket(playerInfo);

            //Add everyone to this user's tab list
            player.sendPacket(nextPlayerInfo);
        }

        loginMessage.getBuffer().release();
        nextPlayerInfo.getBuffer().release();
        Graphene.LOGGER.info(user.getUsername() + " has joined the server.");
    }
}
