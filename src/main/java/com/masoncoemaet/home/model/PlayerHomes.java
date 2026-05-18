package com.masoncoemaet.home.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PlayerHomes {
    private final Map<Integer, HomeLocation> homes = new HashMap<Integer, HomeLocation>();

    public synchronized HomeLocation getHome(int slot) {
        return homes.get(slot);
    }

    public synchronized void setHome(int slot, HomeLocation location) {
        homes.put(slot, location);
    }

    public synchronized void deleteHome(int slot) {
        homes.remove(slot);
    }

    public synchronized boolean hasHome(int slot) {
        return homes.containsKey(slot);
    }

    public synchronized Map<Integer, HomeLocation> getHomes() {
        return Collections.unmodifiableMap(new HashMap<Integer, HomeLocation>(homes));
    }

    public synchronized PlayerHomes copy() {
        PlayerHomes copy = new PlayerHomes();
        for (Map.Entry<Integer, HomeLocation> entry : homes.entrySet()) {
            HomeLocation home = entry.getValue();
            copy.setHome(entry.getKey(), new HomeLocation(home.getWorld(), home.getX(), home.getY(), home.getZ(),
                    home.getYaw(), home.getPitch(), home.getCustomName()));
        }
        return copy;
    }
}
