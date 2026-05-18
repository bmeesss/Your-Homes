package com.masoncoemaet.home;

import com.masoncoemaet.home.model.HomeLocation;
import com.masoncoemaet.home.model.PlayerHomes;
import com.masoncoemaet.home.storage.HomeStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HomeService {
    private final HomePlugin plugin;
    private final HomeStorage storage;
    private final Map<UUID, PlayerHomes> cache = new ConcurrentHashMap<UUID, PlayerHomes>();

    public HomeService(HomePlugin plugin, HomeStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public void loadAsync(final UUID uuid, final Runnable callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            public void run() {
                try {
                    cache.put(uuid, storage.load(uuid));
                } catch (SQLException ex) {
                    plugin.getLogger().severe("Could not load homes for " + uuid + ": " + ex.getMessage());
                    cache.put(uuid, new PlayerHomes());
                }
                if (callback != null) {
                    Bukkit.getScheduler().runTask(plugin, callback);
                }
            }
        });
    }

    public PlayerHomes getHomes(UUID uuid) {
        PlayerHomes homes = cache.get(uuid);
        if (homes == null) {
            homes = new PlayerHomes();
            cache.put(uuid, homes);
        }
        return homes;
    }

    public boolean isLoaded(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public void setHome(Player player, int slot) {
        getHomes(player.getUniqueId()).setHome(slot, HomeLocation.fromLocation(player.getLocation()));
        saveAsync(player.getUniqueId());
    }

    public void deleteHome(UUID uuid, int slot) {
        getHomes(uuid).deleteHome(slot);
        saveAsync(uuid);
    }

    public void renameHome(UUID uuid, int slot, String name) {
        HomeLocation home = getHomes(uuid).getHome(slot);
        if (home != null) {
            home.setCustomName(name);
            saveAsync(uuid);
        }
    }

    public void saveAsync(final UUID uuid) {
        final PlayerHomes snapshot = getHomes(uuid).copy();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            public void run() {
                saveNow(uuid, snapshot);
            }
        });
    }

    public void saveAllNow() {
        for (Map.Entry<UUID, PlayerHomes> entry : cache.entrySet()) {
            saveNow(entry.getKey(), entry.getValue());
        }
    }

    private void saveNow(UUID uuid, PlayerHomes homes) {
        try {
            storage.save(uuid, homes);
        } catch (SQLException ex) {
            plugin.getLogger().severe("Could not save homes for " + uuid + ": " + ex.getMessage());
        }
    }
}
