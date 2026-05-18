package com.masoncoemaet.home;

import com.masoncoemaet.home.command.HomeCommand;
import com.masoncoemaet.home.gui.HomeGuiListener;
import com.masoncoemaet.home.gui.HomeMenu;
import com.masoncoemaet.home.storage.HomeStorage;
import com.masoncoemaet.home.util.Colors;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HomePlugin extends JavaPlugin {
    private HomeService homeService;
    private HomeMenu homeMenu;
    private FileConfiguration messages;
    private FileConfiguration gui;

    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("gui.yml");
        reloadMessages();
        reloadGui();

        try {
            HomeStorage storage = createStorage();
            storage.init();
            homeService = new HomeService(this, storage);
        } catch (Exception ex) {
            getLogger().severe("Failed to initialize storage: " + ex.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        homeMenu = new HomeMenu(this);
        HomeCommand homeCommand = new HomeCommand(this);
        getCommand("home").setExecutor(homeCommand);
        getCommand("home").setTabCompleter(homeCommand);
        Bukkit.getPluginManager().registerEvents(new HomeGuiListener(this), this);
    }

    public void onDisable() {
        if (homeService != null) {
            homeService.saveAllNow();
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        reloadMessages();
        reloadGui();
    }

    public HomeService getHomeService() {
        return homeService;
    }

    public HomeMenu getHomeMenu() {
        return homeMenu;
    }

    public String message(String key) {
        return Colors.color(messages.getString(key, "&cMissing message: " + key));
    }

    public String prefixed(String key) {
        return Colors.color(messages.getString("prefix", "")) + message(key);
    }

    public FileConfiguration getGuiConfig() {
        return gui;
    }

    public String guiText(String path, String fallback) {
        return Colors.color(gui.getString(path, fallback));
    }

    public int getMaxHomes(org.bukkit.entity.Player player) {
        int permissionMax = 0;
        Pattern pattern = Pattern.compile("^home\\.homes\\.(\\d+)$", Pattern.CASE_INSENSITIVE);
        for (PermissionAttachmentInfo permission : player.getEffectivePermissions()) {
            if (!permission.getValue()) {
                continue;
            }
            Matcher matcher = pattern.matcher(permission.getPermission());
            if (matcher.matches()) {
                permissionMax = Math.max(permissionMax, Integer.parseInt(matcher.group(1)));
            }
        }
        if (permissionMax > 0) {
            return clampMaxHomes(permissionMax);
        }
        return clampMaxHomes(getConfig().getInt("max-homes", 5));
    }

    private int clampMaxHomes(int amount) {
        return Math.max(1, Math.min(27, amount));
    }

    private HomeStorage createStorage() {
        String type = getConfig().getString("storage.type", "sqlite");
        if ("mysql".equalsIgnoreCase(type)) {
            String host = getConfig().getString("storage.mysql.host", "localhost");
            int port = getConfig().getInt("storage.mysql.port", 3306);
            String database = getConfig().getString("storage.mysql.database", "minecraft");
            String user = getConfig().getString("storage.mysql.username", "root");
            String password = getConfig().getString("storage.mysql.password", "");
            boolean ssl = getConfig().getBoolean("storage.mysql.useSSL", false);
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + ssl;
            return new HomeStorage(url, user, password, true);
        }
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        return new HomeStorage("jdbc:sqlite:" + new File(getDataFolder(), "homes.db").getAbsolutePath(), null, null, false);
    }

    private void reloadMessages() {
        messages = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
    }

    private void reloadGui() {
        gui = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "gui.yml"));
    }

    private void saveResourceIfMissing(String name) {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) {
            saveResource(name, false);
        }
    }
}
