package com.masoncoemaet.home.gui;

import com.masoncoemaet.home.HomePlugin;
import com.masoncoemaet.home.model.HomeLocation;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class HomeGuiListener implements Listener {
    private final HomePlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<UUID, Long>();
    private final Map<UUID, Integer> renaming = new HashMap<UUID, Integer>();

    public HomeGuiListener(HomePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getHomeService().loadAsync(event.getPlayer().getUniqueId(), null);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getHomeService().saveAsync(event.getPlayer().getUniqueId());
        renaming.remove(event.getPlayer().getUniqueId());
        cooldowns.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || event.getClickedInventory() == null) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        String title = event.getInventory().getTitle();

        if (plugin.getHomeMenu().mainTitle().equals(title)) {
            event.setCancelled(true);
            handleMainClick(player, event.getRawSlot());
            return;
        }

        for (int i = 1; i <= plugin.getMaxHomes(player); i++) {
            if (plugin.getHomeMenu().manageTitle(i).equals(title)) {
                event.setCancelled(true);
                handleManageClick(player, i, event.getRawSlot());
                return;
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final Integer slot = renaming.remove(player.getUniqueId());
        if (slot == null) {
            return;
        }
        event.setCancelled(true);
        final String name = event.getMessage().trim();
        String pattern = plugin.getConfig().getString("settings.home-name-pattern", "^[A-Za-z0-9_\\-]{1,16}$");
        if (!Pattern.matches(pattern, name)) {
            player.sendMessage(plugin.prefixed("invalid-name"));
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            public void run() {
                plugin.getHomeService().renameHome(player.getUniqueId(), slot, name);
                player.sendMessage(plugin.prefixed("home-renamed")
                        .replace("%old%", "Home " + slot)
                        .replace("%new%", name));
                plugin.getHomeMenu().openManage(player, slot);
            }
        });
    }

    private void handleMainClick(Player player, int rawSlot) {
        HomeMenu.HomeSlot homeSlot = plugin.getHomeMenu().homeSlotFromMainSlot(player, rawSlot);
        if (homeSlot == null) {
            return;
        }
        int homeNumber = homeSlot.getHomeNumber();
        if (homeSlot.getBedSlot() == rawSlot) {
            HomeLocation home = plugin.getHomeService().getHomes(player.getUniqueId()).getHome(homeNumber);
            if (home == null) {
                setHome(player, homeNumber);
            } else {
                teleport(player, homeNumber);
            }
            return;
        }
        if (homeSlot.getDyeSlot() == rawSlot
                && plugin.getHomeService().getHomes(player.getUniqueId()).hasHome(homeNumber)) {
            plugin.getHomeMenu().openManage(player, homeNumber);
        }
    }

    private void handleManageClick(Player player, int homeNumber, int rawSlot) {
        if (!plugin.getHomeService().getHomes(player.getUniqueId()).hasHome(homeNumber)) {
            plugin.getHomeMenu().openMain(player);
            return;
        }
        if (rawSlot == 10) {
            teleport(player, homeNumber);
        } else if (rawSlot == 12) {
            plugin.getHomeService().deleteHome(player.getUniqueId(), homeNumber);
            player.sendMessage(plugin.prefixed("home-deleted").replace("%home%", String.valueOf(homeNumber)));
            plugin.getHomeMenu().openMain(player);
        } else if (rawSlot == 14) {
            renaming.put(player.getUniqueId(), homeNumber);
            player.closeInventory();
            player.sendMessage(plugin.prefixed("rename-start"));
        } else if (rawSlot == 22) {
            plugin.getHomeMenu().openMain(player);
        }
    }

    private void setHome(Player player, int homeNumber) {
        if (homeNumber > plugin.getMaxHomes(player)) {
            player.sendMessage(plugin.prefixed("home-limit").replace("%limit%", String.valueOf(plugin.getMaxHomes(player))));
            return;
        }
        plugin.getHomeService().setHome(player, homeNumber);
        player.sendMessage(plugin.prefixed("home-set").replace("%home%", String.valueOf(homeNumber)));
        plugin.getHomeMenu().openMain(player);
    }

    private void teleport(Player player, int homeNumber) {
        if (homeNumber > plugin.getMaxHomes(player)) {
            player.sendMessage(plugin.prefixed("home-limit").replace("%limit%", String.valueOf(plugin.getMaxHomes(player))));
            return;
        }
        HomeLocation home = plugin.getHomeService().getHomes(player.getUniqueId()).getHome(homeNumber);
        if (home == null) {
            player.sendMessage(plugin.prefixed("home-not-found"));
            return;
        }
        int cooldown = plugin.getConfig().getInt("settings.teleport-cooldown-seconds", 0);
        long now = System.currentTimeMillis();
        if (cooldown > 0 && !player.hasPermission("home.bypass.cooldown")) {
            long available = cooldowns.containsKey(player.getUniqueId()) ? cooldowns.get(player.getUniqueId()) : 0L;
            if (available > now) {
                long seconds = (available - now + 999L) / 1000L;
                player.sendMessage(plugin.prefixed("teleport-cooldown").replace("%seconds%", String.valueOf(seconds)));
                return;
            }
            cooldowns.put(player.getUniqueId(), now + cooldown * 1000L);
        }
        Location location = home.toLocation();
        if (location == null) {
            player.sendMessage(plugin.prefixed("home-not-found"));
            return;
        }
        player.closeInventory();
        player.teleport(location);
        player.sendMessage(plugin.prefixed("teleport-success").replace("%home%", String.valueOf(homeNumber)));
    }
}
