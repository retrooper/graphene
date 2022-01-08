package com.github.graphene.util;

import com.github.graphene.Main;
import com.github.graphene.user.User;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.netty.channel.ChannelAbstract;
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
        for (User user : Main.USERS) {
            WrapperPlayServerChatMessage outChatMessage = new WrapperPlayServerChatMessage(component,
                    WrapperPlayServerChatMessage.ChatPosition.CHAT, new UUID(0L, 0L));
            outChatMessage.prepareForSend();
            user.sendPacket(outChatMessage);
        }
    }

    public static void handlePlayerLeave(User user) {
        ChannelAbstract ch = PacketEvents.getAPI().getNettyManager().wrapChannel(user.getChannel());
        PacketEvents.getAPI().getPlayerManager().CLIENT_VERSIONS.remove(ch);
        PacketEvents.getAPI().getPlayerManager().CONNECTION_STATES.remove(ch);
        PacketEvents.getAPI().getPlayerManager().GAME_PROFILES.remove(ch);
        PacketEvents.getAPI().getPlayerManager().CHANNELS.remove(user.getUsername());
        PacketEvents.getAPI().getPlayerManager().PLAYER_ATTRIBUTES.remove(user.getGameProfile().getId());
        Main.USERS.remove(user);
        TextComponent withComponent = TextComponent.builder().color(Color.YELLOW).text(user.getUsername()).insertion(user.getUsername()).build();
        TranslatableComponent translatableComponent = TranslatableComponent.builder().color(Color.YELLOW).translate("multiplayer.player.left")
                .appendWith(withComponent).build();
        WrapperPlayServerPlayerInfo.PlayerData data = new WrapperPlayServerPlayerInfo.PlayerData(null, user.getGameProfile(), null, -1);
        WrapperPlayServerPlayerInfo removePlayerInfo = new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER, data);
        removePlayerInfo.prepareForSend();

        WrapperPlayServerDestroyEntities destroyEntities = new WrapperPlayServerDestroyEntities(user.getEntityId());
        destroyEntities.prepareForSend();

        WrapperPlayServerChatMessage leftMessage =
                new WrapperPlayServerChatMessage(translatableComponent,
                        WrapperPlayServerChatMessage.ChatPosition.CHAT, new UUID(0L, 0L));
        leftMessage.prepareForSend();
        for (User player : Main.USERS) {
            //Remove this user from everyone's tab list
            removePlayerInfo.getBuffer().retain();
            player.sendPacket(removePlayerInfo);
            //Destroy this user's entity
            destroyEntities.getBuffer().retain();
            player.sendPacket(destroyEntities);
            //Send a message to everyone that this user has left
            leftMessage.getBuffer().retain();
            player.sendPacket(leftMessage);
        }
        Main.LOGGER.info(user.getUsername() + " left the server.");
    }

    public static void handlePlayerJoin(User user) {
        Main.USERS.add(user);
        ClickEvent clickEvent = new ClickEvent(ClickEvent.ClickType.SUGGEST_COMMAND, "/tell " + user.getUsername() + " Welcome!");
        TextComponent withComponent = TextComponent.builder().color(Color.YELLOW).text(user.getUsername()).insertion(user.getUsername()).clickEvent(clickEvent).build();
        TranslatableComponent translatableComponent = TranslatableComponent.builder().color(Color.YELLOW).translate("multiplayer.player.joined")
                .appendWith(withComponent).build();

        WrapperPlayServerChatMessage loginMessage = new WrapperPlayServerChatMessage(translatableComponent, WrapperPlayServerChatMessage.ChatPosition.CHAT, new UUID(0L, 0L));
        loginMessage.prepareForSend();

        BaseComponent otherDisplayName = TextComponent.builder().text(user.getUsername())
                .color(Color.DARK_GREEN).build();
        WrapperPlayServerPlayerInfo.PlayerData nextData =
                new WrapperPlayServerPlayerInfo.PlayerData(otherDisplayName, user.getGameProfile(), user.getGameMode(), 100);
        WrapperPlayServerPlayerInfo nextPlayerInfo =
                new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.ADD_PLAYER, nextData);
        nextPlayerInfo.prepareForSend();

        for (User player : Main.USERS) {
            //Send every player the login message
            loginMessage.getBuffer().retain();
            player.sendPacket(loginMessage);

            //Add this joining user to everyone's tab list
            BaseComponent displayName = TextComponent.builder().text(player.getUsername())
                    .color(Color.DARK_GREEN).build();
            WrapperPlayServerPlayerInfo.PlayerData data = new WrapperPlayServerPlayerInfo.PlayerData(displayName,
                    player.getGameProfile(), player.getGameMode(), 100);
            WrapperPlayServerPlayerInfo playerInfo = new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.ADD_PLAYER, data);
            user.sendPacket(playerInfo);

            //Add everyone to this user's tab list
            nextPlayerInfo.getBuffer().retain();
            player.sendPacket(nextPlayerInfo);
        }

        Main.LOGGER.info(user.getUsername() + " has joined the server.");
    }

}
