package com.masoncoemaet.home.command;

import com.masoncoemaet.home.HomePlugin;
import com.masoncoemaet.home.model.HomeLocation;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HomeCommand implements CommandExecutor, TabCompleter {
    private final HomePlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<UUID, Long>();

    public HomeCommand(HomePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.prefixed("player-only"));
            return true;
        }
        final Player player = (Player) sender;
        if (!player.hasPermission("home.use")) {
            player.sendMessage(plugin.prefixed("no-permission"));
            return true;
        }

        if (!plugin.getHomeService().isLoaded(player.getUniqueId())) {
            player.sendMessage(plugin.prefixed("loading"));
            final String[] commandArgs = args;
            plugin.getHomeService().loadAsync(player.getUniqueId(), new Runnable() {
                public void run() {
                    if (player.isOnline() && commandArgs.length == 0) {
                        plugin.getHomeMenu().openMain(player);
                    } else if (player.isOnline()) {
                        Integer slot = parseSlot(commandArgs[0]);
                        if (slot == null) {
                            player.sendMessage(plugin.prefixed("invalid-slot"));
                        } else {
                            teleport(player, slot);
                        }
                    }
                }
            });
            return true;
        }

        if (args.length == 0) {
            plugin.getHomeMenu().openMain(player);
            return true;
        }

        Integer slot = parseSlot(args[0]);
        if (slot == null) {
            player.sendMessage(plugin.prefixed("invalid-slot"));
            return true;
        }
        teleport(player, slot);
        return true;
    }

    public void teleport(Player player, int slot) {
        if (slot < 1) {
            player.sendMessage(plugin.prefixed("invalid-slot"));
            return;
        }
        if (slot > plugin.getMaxHomes(player)) {
            player.sendMessage(plugin.prefixed("home-limit").replace("%limit%", String.valueOf(plugin.getMaxHomes(player))));
            return;
        }
        HomeLocation home = plugin.getHomeService().getHomes(player.getUniqueId()).getHome(slot);
        if (home == null) {
            player.sendMessage(plugin.prefixed("home-not-found"));
            return;
        }
        int cooldown = plugin.getConfig().getInt("settings.teleport-cooldown-seconds", 0);
        if (cooldown > 0 && !player.hasPermission("home.bypass.cooldown")) {
            long now = System.currentTimeMillis();
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
        player.teleport(location);
        player.sendMessage(plugin.prefixed("teleport-success").replace("%home%", String.valueOf(slot)));
    }

    private Integer parseSlot(String input) {
        try {
            int slot = Integer.parseInt(input);
            if (slot < 1) {
                return null;
            }
            return slot;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            int max = sender instanceof Player ? plugin.getMaxHomes((Player) sender) : plugin.getConfig().getInt("max-homes", 5);
            List<String> completions = new ArrayList<String>();
            for (int i = 1; i <= max; i++) {
                completions.add(String.valueOf(i));
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
