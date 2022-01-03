package com.github.graphene.packetevents;

import com.github.graphene.Graphene;
import com.github.graphene.handler.encryption.PacketDecryptionHandler;
import com.github.graphene.handler.encryption.PacketEncryptionHandler;
import com.github.graphene.logic.EntityHandler;
import com.github.graphene.packetevents.manager.netty.ByteBufUtil;
import com.github.graphene.user.User;
import com.github.graphene.util.UUIDUtil;
import com.github.graphene.util.entity.EntityInformation;
import com.github.graphene.util.entity.Location;
import com.github.graphene.util.entity.UpdateType;
import com.github.graphene.wrapper.play.server.WrapperPlayServerChunkData_1_18;
import com.github.graphene.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.graphene.wrapper.play.server.WrapperStatusServerResponse;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.impl.PacketSendEvent;
import com.github.retrooper.packetevents.netty.channel.ChannelAbstract;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.chat.Color;
import com.github.retrooper.packetevents.protocol.chat.component.BaseComponent;
import com.github.retrooper.packetevents.protocol.chat.component.ClickEvent;
import com.github.retrooper.packetevents.protocol.chat.component.impl.TextComponent;
import com.github.retrooper.packetevents.protocol.chat.component.impl.TranslatableComponent;
import com.github.retrooper.packetevents.protocol.chat.component.serializer.ComponentSerializer;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.Enchantment;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameProfile;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.world.Difficulty;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.DataPalette;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.MinecraftEncryptionUtil;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientEncryptionResponse;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerEncryptionRequest;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerLoginSuccess;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import com.github.retrooper.packetevents.wrapper.status.client.WrapperStatusClientPing;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerPong;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.netty.channel.ChannelPipeline;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public class GraphenePacketListener implements PacketListener {
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        User user = (User) event.getPlayer();
        assert user != null;
        switch (event.getConnectionState()) {
            case HANDSHAKING:
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
                    playersComponent.addProperty("online", Graphene.USERS.size());
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
                    user.forceDisconnect();
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

                    //Encryption begins here
                    String serverID = "";
                    PublicKey key = Graphene.KEY_PAIR.getPublic();
                    byte[] verifyToken = new byte[4];
                    new Random().nextBytes(verifyToken);

                    user.setVerifyToken(verifyToken);
                    user.setServerId(serverID);

                    WrapperLoginServerEncryptionRequest encryptionRequest = new WrapperLoginServerEncryptionRequest(serverID, key, verifyToken);
                    PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), encryptionRequest);
                    Graphene.LOGGER.info("Sent encryption request to " + user.getUsername());
                } else if (event.getPacketType() == PacketType.Login.Client.ENCRYPTION_RESPONSE) {
                    WrapperLoginClientEncryptionResponse encryptionResponse = new WrapperLoginClientEncryptionResponse(event);
                    user.setGameProfile(new GameProfile(user.getUUID(), user.getUsername()));

                    Graphene.LOGGER.info("Calling thread-pool for authenticating user " + user.getUsername() + "!");

                    // Authenticate and handle player connection on a separate
                    // ExecutorService pool of threads.
                    Graphene.WORKER_THREADS.execute(() -> {
                        byte[] verifyToken = MinecraftEncryptionUtil.decryptRSA(Graphene.KEY_PAIR.getPrivate(), encryptionResponse.getEncryptedVerifyToken());
                        PrivateKey privateKey = Graphene.KEY_PAIR.getPrivate();
                        byte[] sharedSecret = MinecraftEncryptionUtil.decrypt(privateKey.getAlgorithm(), privateKey, encryptionResponse.getEncryptedSharedSecret());
                        MessageDigest digest;
                        try {
                            digest = MessageDigest.getInstance("SHA-1");
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                            user.forceDisconnect();
                            return; // basically asserts that digest must be not null
                        }

                        digest.update(user.getServerId().getBytes(StandardCharsets.UTF_8));
                        digest.update(sharedSecret);
                        digest.update(Graphene.KEY_PAIR.getPublic().getEncoded());
                        String serverIdHash = new BigInteger(digest.digest()).toString(16);

                        if (Arrays.equals(user.getVerifyToken(), verifyToken)) {
                            try {
//                                String ip = user.getAddress().getHostName();
                                URL url = new URL("https://sessionserver.mojang.com/session/minecraft/hasJoined?username=" + user.getUsername() + "&serverId=" + serverIdHash);
                                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                connection.setRequestProperty("Authorization", null);
                                connection.setRequestMethod("GET");
                                Graphene.LOGGER.info("Authenticating " + user.getUsername() + "...");
                                if (connection.getResponseCode() == 204) {
                                    Graphene.LOGGER.info("Failed to authenticate " + user.getUsername() + "!");
                                    user.kickLogin("Failed to authenticate your connection.");
                                    return;
                                }
                                BufferedReader in = new BufferedReader(
                                        new InputStreamReader(connection.getInputStream()));
                                String inputLine;
                                // We know the output from here MUST be a string (assuming
                                // - they don't change their API) so we can use StringBuilder not buffer.
                                StringBuilder sb = new StringBuilder();
                                while ((inputLine = in.readLine()) != null) {
                                    sb.append(inputLine);
                                }
                                in.close();
                                JsonObject jsonObject = ComponentSerializer.GSON.fromJson(sb.toString(), JsonObject.class);

                                String username = jsonObject.get("name").getAsString();
                                String rawUUID = jsonObject.get("id").getAsString();
                                UUID uuid = UUIDUtil.fromStringWithoutDashes(rawUUID);
                                JsonArray textureProperties = jsonObject.get("properties").getAsJsonArray();

                                for (User lUser : Graphene.USERS) {
                                    if (lUser.getUsername().equals(username)) {
                                        lUser.kickLogin("You logged in from another location!");
                                    }
                                }

                                GameProfile profile = user.getGameProfile();
                                for (JsonElement element : textureProperties) {
                                    JsonObject property = element.getAsJsonObject();

                                    String name = property.get("name").getAsString();
                                    String value = property.get("value").getAsString();
                                    String signature = property.get("signature").getAsString();

                                    profile.getTextureProperties().add(new TextureProperty(name, value, signature));
                                }
                                user.setGameProfile(profile);

                                user.setUUID(uuid);
                                user.setUsername(username);

                                ChannelPipeline pipeline = user.getChannel().pipeline();

                                SecretKey sharedSecretKey = new SecretKeySpec(sharedSecret, "AES");

                                Cipher decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
                                decryptCipher.init(Cipher.DECRYPT_MODE, sharedSecretKey, new IvParameterSpec(sharedSecret));

                                pipeline.addBefore("packet_splitter", "decryption_handler", new PacketDecryptionHandler(decryptCipher));

                                Cipher encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
                                encryptCipher.init(Cipher.ENCRYPT_MODE, sharedSecretKey, new IvParameterSpec(sharedSecret));
                                pipeline.addBefore("packet_prepender", "encryption_handler", new PacketEncryptionHandler(encryptCipher));
                                sendPostLoginPackets(event);
                            } catch (IOException | NoSuchPaddingException | NoSuchAlgorithmException
                                    | InvalidKeyException | InvalidAlgorithmParameterException ex) {
                                ex.printStackTrace();
                            }
                        } else {
                            Graphene.LOGGER.warning("Failed to authenticate " + user.getUsername() + ", because they replied with an invalid verify token!");
                            user.forceDisconnect();
                        }
                    });
                }

                break;
            case PLAY:
                if (event.getPacketType() == PacketType.Play.Client.CLIENT_SETTINGS) {
                    WrapperPlayClientSettings settings = new WrapperPlayClientSettings(event);
                    System.out.println("got settings, hand: " + settings.getMainHand());
                } else if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
                    WrapperPlayClientChatMessage cm = new WrapperPlayClientChatMessage(event);

                    String msg = cm.getMessage();
                    BaseComponent component = TextComponent.builder().text("[" + user.getUsername() + "] ").color(Color.GOLD)
                            .append(TextComponent.builder().text(msg).color(Color.WHITE).build()).build();
                    sendMessage(component);
                } else if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) {
                    if (user.getSendKeepAliveTime() != 0L) {
                        user.setLatency(System.currentTimeMillis() - user.getSendKeepAliveTime());
                        user.getEntityInformation().addUpdateTotal(UpdateType.LATENCY);
                        user.setLastKeepAliveTime(System.currentTimeMillis());
                    }
                }

                break;
        }

    }

    @Override
    public void onPacketSend(PacketSendEvent event) {

    }

    public static void sendMessage(User user, BaseComponent message) {
        WrapperPlayServerChatMessage outChatMessage = new WrapperPlayServerChatMessage(message, WrapperPlayServerChatMessage.ChatPosition.CHAT, new UUID(0L, 0L));
        ChannelAbstract ch = PacketEvents.getAPI().getNettyManager().wrapChannel(user.getChannel());
        PacketEvents.getAPI().getPlayerManager().sendPacket(ch, outChatMessage);
    }

    public static void sendMessage(BaseComponent component) {
        for (User user : Graphene.USERS) {
            sendMessage(user, component);
        }
    }

    public static void handleLeave(User user) {
        for (User player : Graphene.USERS) {
            WrapperPlayServerPlayerInfo.PlayerData data = new WrapperPlayServerPlayerInfo.PlayerData(null, null, null, -1);
            List<WrapperPlayServerPlayerInfo.PlayerData> dataList = new ArrayList<>();
            dataList.add(data);
            WrapperPlayServerPlayerInfo outPlayerInfo = new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER, user.getUUID(), dataList);
            ChannelAbstract ch = PacketEvents.getAPI().getNettyManager().wrapChannel(player.getChannel());
            PacketEvents.getAPI().getPlayerManager().sendPacket(ch, outPlayerInfo);
        }

        sendMessage(TextComponent.builder().text("[" + user.getUsername() + "] ").color(Color.GOLD).append(TextComponent.builder().text("has left the server!").color(Color.WHITE).build()).build());
        Graphene.USERS.remove(user);
    }

    public static void handleLogin(User user) {
        // use translations!
        ClickEvent clickEvent = new ClickEvent(ClickEvent.ClickType.SUGGEST_COMMAND, "/tell " + user.getUsername() + " Welcome!");
        TextComponent withComponent = TextComponent.builder().color(Color.YELLOW).text(user.getUsername()).insertion(user.getUsername()).clickEvent(clickEvent).build();
        TranslatableComponent translatableComponent = TranslatableComponent.builder().color(Color.YELLOW).translate("multiplayer.player.joined")
                .appendWith(withComponent).build();

        for (User online : Graphene.USERS) {
            WrapperPlayServerChatMessage loginMessage = new WrapperPlayServerChatMessage(translatableComponent, WrapperPlayServerChatMessage.ChatPosition.CHAT, new UUID(0L, 0L));
            ChannelAbstract ch = PacketEvents.getAPI().getNettyManager().wrapChannel(online.getChannel());
            PacketEvents.getAPI().getPlayerManager().sendPacket(ch, loginMessage);
        }

        for (User player : Graphene.USERS) {
            List<WrapperPlayServerPlayerInfo.PlayerData> playerDataList = new ArrayList<>();
            //TODO User#getPing and then implement a getPing in packetevents
            WrapperPlayServerPlayerInfo.PlayerData data = new WrapperPlayServerPlayerInfo.PlayerData(TextComponent.builder().text(player.getUsername()).build(), player.getGameProfile(), player.getGameMode(), 100);
            playerDataList.add(data);
            WrapperPlayServerPlayerInfo playerInfo = new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.ADD_PLAYER, player.getUUID(), playerDataList);
            ChannelAbstract channel = PacketEvents.getAPI().getNettyManager().wrapChannel(user.getChannel());
            PacketEvents.getAPI().getPlayerManager().sendPacket(channel, playerInfo);


            List<WrapperPlayServerPlayerInfo.PlayerData> nextPlayerDataList = new ArrayList<>();
            WrapperPlayServerPlayerInfo.PlayerData nextData = new WrapperPlayServerPlayerInfo.PlayerData(TextComponent.builder().text(user.getUsername()).build(), user.getGameProfile(), user.getGameMode(), 100);
            nextPlayerDataList.add(nextData);
            WrapperPlayServerPlayerInfo nextPlayerInfo = new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.ADD_PLAYER, user.getUUID(), nextPlayerDataList);
            ChannelAbstract pChannel = PacketEvents.getAPI().getNettyManager().wrapChannel(player.getChannel());
            PacketEvents.getAPI().getPlayerManager().sendPacket(pChannel, nextPlayerInfo);
        }
    }

    public static void sendPostLoginPackets(PacketReceiveEvent event) {
        User user = (User) event.getPlayer();
        WrapperLoginServerLoginSuccess loginSuccess = new WrapperLoginServerLoginSuccess(user.getUUID(), user.getUsername());
        PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), loginSuccess);
        user.setState(ConnectionState.PLAY);
        Location spawnPosition = new Location(0, 2, 0, 0, 0);
        user.setEntityInformation(new EntityInformation(spawnPosition));
        Graphene.USERS.add(user);
        Graphene.LOGGER.info(user.getUsername() + " has joined the server!");
        byte[] dimensionBytes = new byte[0];
        try (InputStream dimensionInfo = Graphene.class.getClassLoader().getResourceAsStream("RawDimensions.bytes")) {
            dimensionBytes = new byte[dimensionInfo.available()];
            dimensionInfo.read(dimensionBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] dimensionCodecBytes = new byte[0];
        try (InputStream dimensionCodecInfo = Graphene.class.getClassLoader().getResourceAsStream("RawCodec.bytes")) {
            dimensionCodecBytes = new byte[dimensionCodecInfo.available()];
            dimensionCodecInfo.read(dimensionCodecBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        PacketWrapper<?> dimensionBuffer = PacketWrapper.createUniversalPacketWrapper(ByteBufUtil.buffer());
        dimensionBuffer.buffer.writeBytes(dimensionBytes);
        NBTCompound dimension = dimensionBuffer.readNBT();

        PacketWrapper<?> dimensionCodecBuffer = PacketWrapper.createUniversalPacketWrapper(ByteBufUtil.buffer());
        dimensionCodecBuffer.buffer.writeBytes(dimensionCodecBytes);
        NBTCompound dimensionCodec = dimensionCodecBuffer.readNBT();

        List<String> worldNames = new ArrayList<>();
        worldNames.add("minecraft:overworld");
        worldNames.add("minecraft:the_nether");
        worldNames.add("minecraft:the_end");
        long hashedSeed = 0L;

        WrapperPlayServerJoinGame joinGame = new WrapperPlayServerJoinGame(user.getEntityId(),
                false, user.getGameMode(), user.getPreviousGameMode(),
                worldNames, dimensionCodec, dimension, worldNames.get(0), hashedSeed, Graphene.MAX_PLAYERS, 10, 20, 20, false, true, false, true);
        PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), joinGame);

        String brandName = "Graphene";

        PacketWrapper<?> brandNameBuffer = PacketWrapper.createUniversalPacketWrapper(ByteBufUtil.buffer());
        brandNameBuffer.writeByteArray(brandName.getBytes());
        byte[] brandNameBytes = new byte[brandNameBuffer.buffer.readableBytes()];
        brandNameBuffer.buffer.readBytes(brandNameBytes);

        WrapperPlayServerPluginMessage pluginMessage = new WrapperPlayServerPluginMessage("minecraft:brand", brandNameBytes);
        PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), pluginMessage);

        WrapperPlayServerDifficulty difficulty = new WrapperPlayServerDifficulty(Difficulty.HARD, false);
        PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), difficulty);

        WrapperPlayServerPlayerAbilities playerAbilities = new WrapperPlayServerPlayerAbilities(false, false, false, false, 0.05f, 0.1f);
        PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), playerAbilities);

        handleLogin(user);

        WrapperPlayServerHeldItemChange heldItemChange = new WrapperPlayServerHeldItemChange(0);
        PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), heldItemChange);

        WrapperPlayServerEntityStatus entityStatus = new WrapperPlayServerEntityStatus(user.getEntityId(), 28);
        PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), entityStatus);

        // send current player information

        // TODO work on sending chunks
        Chunk_v1_18[] chunks = new Chunk_v1_18[1];
        for (int i = 0; i < chunks.length; i++) {
            DataPalette chunkPalette = DataPalette.createForChunk();
            chunkPalette.set(0, 0, 0, WrappedBlockState.getByString("minecraft:dirt").getGlobalId());
            chunkPalette.set(0, 0, 1, WrappedBlockState.getByString("minecraft:dirt").getGlobalId());
            DataPalette biomePalette = DataPalette.createForBiome();
            chunks[i] = new Chunk_v1_18(2, chunkPalette, biomePalette);
        }
        Column column = new Column(0, 0, true, chunks, new TileEntity[0]);
        WrapperPlayServerChunkData_1_18 chunkData = new WrapperPlayServerChunkData_1_18(column);
        // Should be set to false when updating a chunk instead of sending a new one.
        chunkData.trustEdges = true;
        chunkData.skyLightMask = new BitSet(0);
        chunkData.emptySkyLightMask = new BitSet(0);
        chunkData.blockLightMask = new BitSet(0);

        chunkData.emptyBlockLightMask = new BitSet(0);
        chunkData.skyLightArray = new byte[0][0];//2048 for second
        chunkData.blockLightArray = new byte[0][0];//2048 for second

        PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), chunkData);


       /* WrappedBlockState grassState = WrappedBlockState.getByString("minecraft:grass_block[snowy=false]");
        WrapperPlayServerBlockChange[] changes = new WrapperPlayServerBlockChange[5];
        changes[0] = new WrapperPlayServerBlockChange(new Vector3i(0, 0, 0), WrappedBlockState.getByString("minecraft:air").getGlobalId());
        changes[1] = new WrapperPlayServerBlockChange(new Vector3i(0, 0, 0), grassState.getGlobalId());
        changes[2] = new WrapperPlayServerBlockChange(new Vector3i(1, 0, 0), grassState.getGlobalId());
        changes[3] = new WrapperPlayServerBlockChange(new Vector3i(0, 0, 1), grassState.getGlobalId());
        changes[4] = new WrapperPlayServerBlockChange(new Vector3i(1, 0, 1), grassState.getGlobalId());
        for (WrapperPlayServerBlockChange change : changes) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), change);
        }*/

        WrapperPlayServerPlayerAbilities abilities = new WrapperPlayServerPlayerAbilities(true, true, true, true, 0.05f, 0.1f);
        PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), abilities);

        WrapperPlayServerPlayerPositionAndLook positionAndLook = new WrapperPlayServerPlayerPositionAndLook(spawnPosition.getX(), spawnPosition.getY(), spawnPosition.getZ(), spawnPosition.getYaw(), spawnPosition.getPitch(), 0, 0, true);
        PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), positionAndLook);

        ItemStack sword = ItemStack.builder().type(ItemTypes.DIAMOND_SWORD).amount(ItemTypes.DIAMOND_SWORD.getMaxAmount()).build();
        List<Enchantment> enchantments = new ArrayList<>();
        enchantments.add(Enchantment.builder().type(EnchantmentTypes.FIRE_ASPECT).level(2).build());
        sword.setEnchantments(enchantments);
        WrapperPlayServerSetSlot setSlot = new WrapperPlayServerSetSlot(0, 0, 37, sword);
        PacketEvents.getAPI().getPlayerManager().sendPacket(event.getChannel(), setSlot);

        EntityHandler.onLogin(user);
    }

}
