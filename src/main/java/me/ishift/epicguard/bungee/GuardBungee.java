package me.ishift.epicguard.bungee;

import me.ishift.epicguard.bungee.command.GuardCommand;
import me.ishift.epicguard.bungee.listener.ProxyPingListener;
import me.ishift.epicguard.bungee.listener.ProxyPreLoginListener;
import me.ishift.epicguard.bungee.task.AttackClearTask;
import me.ishift.epicguard.bungee.task.CloudTask;
import me.ishift.epicguard.bungee.task.DisplayTask;
import me.ishift.epicguard.bungee.util.MessagesBungee;
import me.ishift.epicguard.bungee.util.Metrics;
import me.ishift.epicguard.universal.Config;
import me.ishift.epicguard.universal.StorageManager;
import me.ishift.epicguard.universal.types.Platform;
import me.ishift.epicguard.universal.util.GeoAPI;
import me.ishift.epicguard.universal.util.Logger;
import me.ishift.epicguard.universal.util.LogoPrinter;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

public class GuardBungee extends Plugin {
    public static boolean log = false;
    public static boolean status = false;
    private static GuardBungee instance;

    public static GuardBungee getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        try {
            this.getDataFolder().mkdir();

            final File dir1 = new File(this.getDataFolder() + "/logs");
            dir1.mkdir();

            final File dir3 = new File(this.getDataFolder() + "/data");
            dir3.mkdir();

            instance = this;
            Logger.create(Platform.BUNGEE);
            LogoPrinter.print();

            this.loadConfig();
            StorageManager.load();
            MessagesBungee.load();

            GeoAPI.create(Platform.BUNGEE);
            new Metrics(this, 5956);

            this.getProxy().getPluginManager().registerListener(this, new ProxyPreLoginListener());
            this.getProxy().getPluginManager().registerListener(this, new ProxyPingListener());

            this.getProxy().getScheduler().schedule(this, new AttackClearTask(), 1L, 30L, TimeUnit.SECONDS);
            this.getProxy().getScheduler().schedule(this, new DisplayTask(), 1L, 300L, TimeUnit.MILLISECONDS);
            this.getProxy().getScheduler().schedule(this, new CloudTask(), 1L, Config.cloudTime, TimeUnit.SECONDS);

            this.getProxy().getPluginManager().registerCommand(this, new GuardCommand("guard"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        final File file = new File(getDataFolder(), "config_bungee.yml");
        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config_bungee.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Config.loadBungee();
    }
}
