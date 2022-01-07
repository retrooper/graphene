package com.github.graphene.packetevents.listener;

import com.github.graphene.user.User;
import com.github.graphene.util.entity.UpdateType;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

public class KeepAliveListener implements PacketListener {
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        User user = (User) event.getPlayer();
        if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) {
            if (user.getSendKeepAliveTime() != 0L) {
                user.setLatency(System.currentTimeMillis() - user.getSendKeepAliveTime());
                user.getEntityInformation().queueUpdate(UpdateType.LATENCY);
                user.setLastKeepAliveTime(System.currentTimeMillis());
            }
        }

    }
}
