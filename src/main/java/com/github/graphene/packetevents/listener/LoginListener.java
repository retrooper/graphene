package com.github.graphene.packetevents.listener;

import com.github.graphene.Graphene;
import com.github.graphene.handler.encryption.PacketDecryptionHandler;
import com.github.graphene.handler.encryption.PacketEncryptionHandler;
import com.github.graphene.user.User;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.chat.component.serializer.ComponentSerializer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameProfile;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.util.MinecraftEncryptionUtil;
import com.github.retrooper.packetevents.util.UUIDUtil;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientEncryptionResponse;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerEncryptionRequest;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerLoginSuccess;
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
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

public class LoginListener implements PacketListener {
    private final boolean onlineMode;
    public LoginListener(boolean onlineMode) {
        this.onlineMode = onlineMode;
    }

    public boolean isOnlineMode() {
        return onlineMode;
    }


    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        User user = (User) event.getPlayer();
        if (event.getPacketType() == PacketType.Login.Client.LOGIN_START) {
            //The client is attempting to log in.
            WrapperLoginClientLoginStart start = new WrapperLoginClientLoginStart(event);
            String username = start.getUsername();
            //Map the player usernames with their netty channels
            PacketEvents.getAPI().getPlayerManager().CHANNELS.put(username, event.getChannel());
            //If online mode is set to false, we just generate a UUID based on their username.
            UUID uuid = isOnlineMode() ? null : UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
            user.setGameProfile(new GameProfile(uuid, username));
            //If online mode is enabled, we begin the encryption and authentication process.
            if (isOnlineMode()) {
                //Server ID may be empty
                String serverID = "";
                //Public key of the server's key pair
                PublicKey key = Graphene.KEY_PAIR.getPublic();
                //Generate a random verify token, it has to be 4 bytes long
                byte[] verifyToken = new byte[4];
                new Random().nextBytes(verifyToken);

                user.setVerifyToken(verifyToken);
                user.setServerId(serverID);
                //Send our encryption request
                WrapperLoginServerEncryptionRequest encryptionRequest = new WrapperLoginServerEncryptionRequest(serverID, key, verifyToken);
                user.sendPacket(encryptionRequest);
            }
            else {
                boolean alreadyLoggedIn = false;
                for (User lUser : Graphene.USERS) {
                    if (lUser.getUsername().equals(username)) {
                        alreadyLoggedIn = true;
                    }
                }

                if (!alreadyLoggedIn) {
                    //Since we're not in online mode, we just inform the client that they have successfully logged in.
                    WrapperLoginServerLoginSuccess loginSuccess = new WrapperLoginServerLoginSuccess(user.getGameProfile());
                    user.sendPacket(loginSuccess);
                    user.setState(ConnectionState.PLAY);
                    JoinManager.handleJoin(user);
                }
                else {
                    user.kickLogin("A user with the username " + username + " is already logged in.");
                }
            }
        }
        //They responded with an encryption response
        else if (event.getPacketType() == PacketType.Login.Client.ENCRYPTION_RESPONSE) {
            WrapperLoginClientEncryptionResponse encryptionResponse = new WrapperLoginClientEncryptionResponse(event);
            // Authenticate and handle player connection on our worker threads
            Graphene.WORKER_THREADS.execute(() -> {
                //Decrypt the verify token
                byte[] verifyToken = MinecraftEncryptionUtil.decryptRSA(Graphene.KEY_PAIR.getPrivate(), encryptionResponse.getEncryptedVerifyToken());
                //Private key from the server's key pair
                PrivateKey privateKey = Graphene.KEY_PAIR.getPrivate();
                //Decrypt the shared secret
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
                //We generate a server id hash that will be used in our web request to mojang's session server.
                String serverIdHash = new BigInteger(digest.digest()).toString(16);
                //Make sure the decrypted verify token from the client is the same one we sent out earlier.
                if (Arrays.equals(user.getVerifyToken(), verifyToken)) {
                    //GET web request using our server id hash.
                    try {
                        URL url = new URL("https://sessionserver.mojang.com/session/minecraft/hasJoined?username=" + user.getUsername() + "&serverId=" + serverIdHash);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestProperty("Authorization", null);
                        connection.setRequestMethod("GET");
                        if (connection.getResponseCode() == 204) {
                            Graphene.LOGGER.info("Failed to authenticate " + user.getUsername() + "!");
                            user.kickLogin("Failed to authenticate your connection.");
                            return;
                        }
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(connection.getInputStream()));
                        String inputLine;
                        StringBuilder sb = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            sb.append(inputLine);
                        }
                        in.close();
                        //Parse the json response we got from the web request.
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
                        //Update our game profile, feed it with our real UUID, we've been authenticated.
                        GameProfile profile = user.getGameProfile();
                        profile.setId(uuid);
                        profile.setName(username);
                        for (JsonElement element : textureProperties) {
                            JsonObject property = element.getAsJsonObject();

                            String name = property.get("name").getAsString();
                            String value = property.get("value").getAsString();
                            String signature = property.get("signature").getAsString();

                            profile.getTextureProperties().add(new TextureProperty(name, value, signature));
                        }
                        //From now on, all packets will be decrypted and encrypted.
                        ChannelPipeline pipeline = user.getChannel().pipeline();
                        SecretKey sharedSecretKey = new SecretKeySpec(sharedSecret, "AES");
                        Cipher decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
                        decryptCipher.init(Cipher.DECRYPT_MODE, sharedSecretKey, new IvParameterSpec(sharedSecret));
                        //Add the decryption handler
                        pipeline.addBefore("packet_splitter", "decryption_handler", new PacketDecryptionHandler(decryptCipher));
                        Cipher encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
                        encryptCipher.init(Cipher.ENCRYPT_MODE, sharedSecretKey, new IvParameterSpec(sharedSecret));
                        //Add the encryption handler
                        pipeline.addBefore("packet_prepender", "encryption_handler", new PacketEncryptionHandler(encryptCipher));
                        //We now inform the client that they have successfully logged in.
                        //Note: The login success packet will be encrypted here.
                        WrapperLoginServerLoginSuccess loginSuccess = new WrapperLoginServerLoginSuccess(user.getGameProfile());
                        user.sendPacket(loginSuccess);
                        user.setState(ConnectionState.PLAY);
                        JoinManager.handleJoin(user);
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
    }
}
