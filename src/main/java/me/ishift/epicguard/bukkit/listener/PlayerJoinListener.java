package me.ishift.epicguard.bukkit.listener;

import me.ishift.epicguard.bukkit.GuardBukkit;
import me.ishift.epicguard.bukkit.manager.*;
import me.ishift.epicguard.bukkit.manager.User;
import me.ishift.epicguard.bukkit.util.MessagesBukkit;
import me.ishift.epicguard.bukkit.util.Notificator;
import me.ishift.epicguard.bukkit.util.Updater;
import me.ishift.epicguard.bukkit.util.server.Reflection;
import me.ishift.epicguard.universal.AttackType;
import me.ishift.epicguard.universal.Config;
import me.ishift.epicguard.universal.check.GeoCheck;
import me.ishift.epicguard.universal.check.NameContainsCheck;
import me.ishift.epicguard.universal.util.ChatUtil;
import me.ishift.epicguard.universal.util.GeoAPI;
import me.ishift.epicguard.universal.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.stream.Collectors;

public class PlayerJoinListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        try {
            final Player player = event.getPlayer();
            final String address = player.getAddress().getAddress().getHostAddress();

            if (Reflection.isOldVersion()) {
                BrandPluginMessageListener.addChannel(player, "MC|BRAND");
            }

            if (Config.antibot && !BlacklistManager.isWhitelisted(address)) {
                if (NameContainsCheck.check(player.getName())) {
                    event.setJoinMessage("");
                    PlayerQuitListener.hiddenNames.add(player.getName());
                    player.kickPlayer(MessagesBukkit.MESSAGE_KICK_NAMECONTAINS.stream().map(s -> ChatUtil.fix(s) + "\n").collect(Collectors.joining()));
                    return;
                }

                if (BlacklistManager.isBlacklisted(address)) {
                    event.setJoinMessage("");
                    PlayerQuitListener.hiddenNames.add(player.getName());
                    player.kickPlayer(MessagesBukkit.MESSAGE_KICK_BLACKLIST.stream().map(s -> ChatUtil.fix(s) + "\n").collect(Collectors.joining()));
                    return;
                }

                if (GeoCheck.check(GeoAPI.getCountryCode(player.getAddress().getAddress()))) {
                    event.setJoinMessage("");
                    PlayerQuitListener.hiddenNames.add(player.getName());
                    player.kickPlayer(MessagesBukkit.MESSAGE_KICK_COUNTRY.stream().map(s -> ChatUtil.fix(s) + "\n").collect(Collectors.joining()));
                    return;
                }
            }

            UserManager.addUser(player);
            final User u = UserManager.getUser(player);
            u.setIp(address);
            Updater.notify(player);
            AttackManager.handleAttack(AttackType.JOIN);

            if (Config.autoWhitelist) {
                Bukkit.getScheduler().runTaskLater(GuardBukkit.getInstance(), () -> {
                    if (player.isOnline()) {
                        BlacklistManager.whitelist(address);
                    }
                }, Config.autoWhitelistTime);
            }

            // IP History manager
            if (Config.ipHistoryEnable) {
                final List<String> history = DataFileManager.getDataFile().getStringList("history." + player.getName());
                if (!history.contains(address)) {
                    if (!history.isEmpty()) {
                        Notificator.broadcast(MessagesBukkit.HISTORY_NEW.replace("{NICK}", player.getName()).replace("{IP}", address));
                    }
                    history.add(address);
                }
                DataFileManager.getDataFile().set("history." + player.getName(), history);
                u.setAdresses(history);
            }

            // Brand Verification
            if (Reflection.isOldVersion()) {
                Bukkit.getScheduler().runTaskLater(GuardBukkit.getInstance(), () -> {
                    if (!player.isOnline()) {
                        return;
                    }

                    if (Config.channelVerification) {
                        if (u.getBrand().equals("none")) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ChatUtil.fix(Config.channelPunish).replace("{PLAYER}", player.getName()));
                            Logger.info(player.getName() + "has been connection! If you think this is an issue, disable 'channel-verification'. Do NOT report this! This is not a bug!");
                            return;
                        }
                        return;
                    }
                    if (Config.blockedBrands) {
                        for (String string : Config.blockedBrandsValues) {
                            if (u.getBrand().equalsIgnoreCase(string)) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ChatUtil.fix(Config.blockedBrandsPunish).replace("{PLAYER}", player.getName()));
                            }
                        }
                    }
                }, Config.channelDelay);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
