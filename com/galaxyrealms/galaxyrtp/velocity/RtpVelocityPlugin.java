/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.inject.Inject
 *  com.velocitypowered.api.event.Subscribe
 *  com.velocitypowered.api.event.connection.PluginMessageEvent
 *  com.velocitypowered.api.event.connection.PluginMessageEvent$ForwardResult
 *  com.velocitypowered.api.event.proxy.ProxyInitializeEvent
 *  com.velocitypowered.api.plugin.Plugin
 *  com.velocitypowered.api.plugin.annotation.DataDirectory
 *  com.velocitypowered.api.proxy.Player
 *  com.velocitypowered.api.proxy.ProxyServer
 *  com.velocitypowered.api.proxy.ServerConnection
 *  com.velocitypowered.api.proxy.messages.ChannelIdentifier
 *  com.velocitypowered.api.proxy.messages.ChannelMessageSource
 *  com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
 *  com.velocitypowered.api.proxy.server.RegisteredServer
 *  org.slf4j.Logger
 */
package com.galaxyrealms.galaxyrtp.velocity;

import com.galaxyrealms.galaxyrtp.common.MessageProtocol;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.slf4j.Logger;

@Plugin(id="galaxyrtp", name="GalaxyRTP", version="1.0.0", description="Cross-platform RTP and spawn for Velocity + Paper (hub/eu/usa).", authors={"GalaxyRealms"})
public class RtpVelocityPlugin {
    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from((String)"galaxyrtp:main");
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public RtpVelocityPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.proxy.getChannelRegistrar().register(new ChannelIdentifier[]{CHANNEL});
        this.logger.info("GalaxyRTP Velocity: channel registered");
    }

    @Subscribe
    public void onPluginMessageFromBackend(PluginMessageEvent event) {
        String worldName;
        String serverName;
        if (!CHANNEL.equals((Object)event.getIdentifier())) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        ChannelMessageSource channelMessageSource = event.getSource();
        if (!(channelMessageSource instanceof ServerConnection)) {
            return;
        }
        ServerConnection backend = (ServerConnection)channelMessageSource;
        byte[] data = event.getData();
        try {
            String[] decoded = MessageProtocol.decodeConnectRequest(data);
            serverName = decoded[0];
            worldName = decoded[1];
        }
        catch (IOException e) {
            this.logger.warn("GalaxyRTP: invalid plugin message from backend: {}", (Object)e.getMessage());
            return;
        }
        Player player = backend.getPlayer();
        Optional targetOpt = this.proxy.getServer(serverName);
        if (targetOpt.isEmpty()) {
            this.logger.warn("GalaxyRTP: unknown server name {}", (Object)serverName);
            return;
        }
        RegisteredServer target = (RegisteredServer)targetOpt.get();
        try {
            byte[] rtpPayload = MessageProtocol.encodeRtpPending(player.getUniqueId(), worldName);
            if (!target.sendPluginMessage((ChannelIdentifier)CHANNEL, rtpPayload)) {
                this.logger.warn("GalaxyRTP: could not send RTP pending to {}", (Object)serverName);
            }
        }
        catch (IOException e) {
            this.logger.warn("GalaxyRTP: failed to encode RTP pending: {}", (Object)e.getMessage());
            return;
        }
        player.createConnectionRequest(target).fireAndForget();
    }
}

