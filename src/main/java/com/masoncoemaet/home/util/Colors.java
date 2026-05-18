package com.masoncoemaet.home.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public final class Colors {
    private Colors() {
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<String> color(List<String> lines) {
        List<String> colored = new ArrayList<String>();
        for (String line : lines) {
            colored.add(color(line));
        }
        return colored;
    }
}
