package io.github.polskistevek.epicguard.bukkit.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import io.github.polskistevek.epicguard.bukkit.GuardBukkit;

public class TabCompletePacket extends PacketAdapter {
    public static GuardBukkit inst;

    public TabCompletePacket(final GuardBukkit plugin) {
        super(plugin, PacketType.Play.Client.TAB_COMPLETE);
        inst = plugin;
    }

    public void onPacketReceiving(final PacketEvent event) {
        if (GuardBukkit.TAB_COMPLETE_BLOCK) {
            event.setCancelled(true);
        }
    }
}