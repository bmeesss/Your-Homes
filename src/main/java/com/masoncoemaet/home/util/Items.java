package com.masoncoemaet.home.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class Items {
    private Items() {
    }

    public static ItemStack item(Material material, int amount, int data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, amount, (short) data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) {
            meta.setLore(new ArrayList<String>(lore));
        }
        item.setItemMeta(meta);
        return item;
    }
}
