package com.github.graphene.packetevents;

import com.github.graphene.packetevents.injector.ChannelInjectorImpl;
import com.github.graphene.packetevents.manager.netty.NettyManagerImpl;
import com.github.graphene.packetevents.manager.player.PlayerManagerImpl;
import com.github.graphene.packetevents.manager.server.ServerManagerImpl;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.EventManager;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.netty.NettyManager;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import com.github.retrooper.packetevents.util.PEVersion;
import com.github.retrooper.packetevents.util.updatechecker.UpdateChecker;

import java.util.logging.Logger;

public class GraphenePacketEventsBuilder {

    public static class Plugin {
        private String name;

        public Plugin(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static PacketEventsAPI<Plugin> API_INSTANCE;

    public static PacketEventsAPI<Plugin> build(Plugin plugin) {
        if (API_INSTANCE == null) {
            API_INSTANCE = buildNoCache(plugin);
        }
        return API_INSTANCE;
    }

    public static PacketEventsAPI<Plugin> build(Plugin plugin, PacketEventsSettings settings) {
        if (API_INSTANCE == null) {
            API_INSTANCE = buildNoCache(plugin, settings);
        }
        return API_INSTANCE;
    }

    public static PacketEventsAPI<Plugin> buildNoCache(Plugin plugin) {
        return buildNoCache(plugin, new PacketEventsSettings());
    }

    public static PacketEventsAPI<Plugin> buildNoCache(Plugin plugin, PacketEventsSettings inSettings) {
        return new PacketEventsAPI<Plugin>() {
            private final EventManager eventManager = new EventManager();
            private final PacketEventsSettings settings = inSettings;
            private final Logger logger = Logger.getLogger(PacketEventsAPI.class.getName());
            private final ServerManager serverManager = new ServerManagerImpl();
            private final PlayerManager playerManager = new PlayerManagerImpl();
            private final NettyManager nettyManager = new NettyManagerImpl();
            private final ChannelInjector injector = new ChannelInjectorImpl();
            private final UpdateChecker updateChecker = new UpdateChecker();
            private boolean loaded;
            private boolean initialized;
            private boolean lateBind = false;

            @Override
            public void load() {
                if (!loaded) {
                    //Resolve server version and cache
                    PacketEvents.IDENTIFIER = "pe-" + plugin.getName().toLowerCase();
                    PacketEvents.ENCODER_NAME = "pe-encoder-" + plugin.getName().toLowerCase();
                    PacketEvents.DECODER_NAME = "packet_decoder";
                    PacketEvents.CONNECTION_NAME = "pe-connection-handler-" + plugin.getName().toLowerCase();
                    PacketEvents.SERVER_CHANNEL_HANDLER_NAME = "pe-connection-initializer-" + plugin.getName().toLowerCase();

                    loaded = true;

                    //Register internal packet listener (should be the first listener)
                    getEventManager().registerListener(new InternalPacketListener(), PacketListenerPriority.LOWEST);
                }
            }

            @Override
            public boolean isLoaded() {
                return loaded;
            }

            @Override
            public void init() {
                //Load if we haven't loaded already
                load();
                if (!initialized) {
                    if (settings.shouldCheckForUpdates()) {
                        updateChecker.handleUpdateCheck();
                    }

                    if (settings.isbStatsEnabled()) {
                        //TODO bStats
                    }

                    PacketType.Play.Client.load();
                    PacketType.Play.Server.load();

                    initialized = true;
                }
            }

            @Override
            public boolean isInitialized() {
                return initialized;
            }

            @Override
            public void terminate() {
                if (initialized) {
                    //Unregister all our listeners
                    getEventManager().unregisterAllListeners();
                    initialized = false;
                }
            }

            @Override
            public Plugin getPlugin() {
                return plugin;
            }

            @Override
            public ServerManager getServerManager() {
                return serverManager;
            }

            @Override
            public PlayerManager getPlayerManager() {
                return playerManager;
            }

            @Override
            public EventManager getEventManager() {
                return eventManager;
            }

            @Override
            public PacketEventsSettings getSettings() {
                return settings;
            }

            @Override
            public PEVersion getVersion() {
                return PacketEvents.VERSION;
            }

            @Override
            public Logger getLogger() {
                return logger;
            }

            @Override
            public NettyManager getNettyManager() {
                return nettyManager;
            }

            @Override
            public ChannelInjector getInjector() {
                return injector;
            }

            @Override
            public UpdateChecker getUpdateChecker() {
                return updateChecker;
            }
        };
    }
}
