package com.github.graphene.util;

import com.github.graphene.Main;
import com.github.graphene.player.Player;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.chat.ChatTypes;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage_v1_16;
import com.github.retrooper.packetevents.protocol.npc.NPC;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.MojangAPIUtil;
import com.github.retrooper.packetevents.util.PacketEventsImplHelper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class ServerUtil {
    public static void broadcastMessage(Component component) {
        for (Player player : Main.PLAYERS) {
            player.sendMessage(component);
        }
    }

    public static void handlePlayerQuit(User user, Player player) {
        if (user.getConnectionState() == ConnectionState.PLAY) {
            ServerUtil.handlePlayerLeave(player);
        }
        UUID uuid = player.getUserProfile() != null ? player.getUserProfile().getUUID() : null;
        PacketEventsImplHelper.handleDisconnection(user.getChannel(), uuid);
        Main.PLAYERS.remove(player);
    }

    public static void handlePlayerLeave(Player player) {
        //TODO If it doesnt work, make sub component yellow too
        Component translatableComponent = Component.translatable("multiplayer.player.left").color(NamedTextColor.YELLOW)
                .args(Component
                        .text(player.getUsername())
                        .asComponent())
                .asComponent();
        for (Player p : Main.PLAYERS) {
            if (p.getEntityId() != player.getEntityId()) {
                WrapperPlayServerPlayerInfo.PlayerData data = new WrapperPlayServerPlayerInfo.PlayerData(null, player.getUserProfile(), null, -1);
                WrapperPlayServerPlayerInfo removePlayerInfo = new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER, data);

                WrapperPlayServerDestroyEntities destroyEntities = new WrapperPlayServerDestroyEntities(player.getEntityId());

                ChatMessage leftChatMsg = new ChatMessage_v1_16(translatableComponent, ChatTypes.CHAT, new UUID(0L, 0L));
                WrapperPlayServerChatMessage leftMessage =
                        new WrapperPlayServerChatMessage(leftChatMsg);
                //Remove this user from everyone's tab list
                p.sendPacket(removePlayerInfo);
                //Destroy this user's entity
                p.sendPacket(destroyEntities);
                //Send a message to everyone that this user has left
                p.sendPacket(leftMessage);
            }
        }
        Main.LOGGER.info(player.getUsername() + " left the server.");
        ProtocolManager.CHANNELS.remove(player.getUsername());
    }

    public static void handlePlayerJoin(User user, Player player) {
        Main.PLAYERS.add(player);
        HoverEvent<HoverEvent.ShowEntity> hoverEvent = HoverEvent.hoverEvent(HoverEvent.Action.SHOW_ENTITY,
                HoverEvent.ShowEntity.showEntity(Key.key("minecraft:player"),
                        player.getUserProfile().getUUID(),
                        Component.text(player.getUsername())));
        ClickEvent clickEvent = ClickEvent.suggestCommand("/tell " + player.getUsername() + " Welcome!");
        Component translatableComponent = Component.translatable("multiplayer.player.joined")
                .color(NamedTextColor.YELLOW)
                .arguments(Component
                        .text(player.getUsername())
                        .hoverEvent(hoverEvent)
                        .clickEvent(clickEvent).asComponent())
                .asComponent();
        for (Player p : Main.PLAYERS) {
            Component otherDisplayName = Component.text(player.getUsername()).color(NamedTextColor.DARK_GREEN).asComponent();
            WrapperPlayServerPlayerInfo.PlayerData nextData =
                    new WrapperPlayServerPlayerInfo.PlayerData(otherDisplayName, player.getUserProfile(), player.getGameMode(), 100);
            WrapperPlayServerPlayerInfo nextPlayerInfo =
                    new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.ADD_PLAYER, nextData);

            //Send every player the login message
            p.sendMessage(translatableComponent);

            //Add this joining user to everyone's tab list
            Component displayName = Component.text(p.getUsername()).color(NamedTextColor.DARK_GREEN).asComponent();
            WrapperPlayServerPlayerInfo.PlayerData data = new WrapperPlayServerPlayerInfo.PlayerData(displayName,
                    p.getUserProfile(), p.getGameMode(), 100);
            WrapperPlayServerPlayerInfo playerInfo = new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.ADD_PLAYER, data);
            player.sendPacket(playerInfo);

            //Add existing players to this user's tab list
            p.sendPacket(nextPlayerInfo);
        }

        Main.LOGGER.info(player.getUsername() + " has joined the server.");

        //Spawn MD_5 like npc
        Main.WORKER_THREADS.execute(() -> {
            UUID md5UUID = MojangAPIUtil.requestPlayerUUID("md_5");
            UserProfile profile = new UserProfile(md5UUID, "md_5", MojangAPIUtil.requestPlayerTextureProperties(md5UUID));
            NPC npc = new NPC(profile, Main.ENTITIES++, GameMode.SURVIVAL, Component.text("md_5_npc").asComponent(),
                    NamedTextColor.BLACK, Component.text("Owner: ").color(NamedTextColor.RED),
                    null);
            npc.setLocation(player.getEntityInformation().getLocation());
            npc.spawn(user.getChannel());
        });
    }

}
