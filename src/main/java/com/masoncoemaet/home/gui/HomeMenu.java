package com.masoncoemaet.home.gui;

import com.masoncoemaet.home.HomePlugin;
import com.masoncoemaet.home.model.HomeLocation;
import com.masoncoemaet.home.model.PlayerHomes;
import com.masoncoemaet.home.util.Colors;
import com.masoncoemaet.home.util.Items;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class HomeMenu {
    public static final int MANAGE_SIZE = 27;

    private final HomePlugin plugin;

    public HomeMenu(HomePlugin plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {
        HomeLayout layout = createLayout(plugin.getMaxHomes(player));
        Inventory inventory = plugin.getServer().createInventory(null, layout.getInventorySize(), mainTitle());
        PlayerHomes homes = plugin.getHomeService().getHomes(player.getUniqueId());
        for (HomeSlot homeSlot : layout.getSlots()) {
            int homeNumber = homeSlot.getHomeNumber();
            HomeLocation home = homes.getHome(homeNumber);
            inventory.setItem(homeSlot.getBedSlot(), home == null ? emptyHomeItem() : filledHomeItem(homeNumber));
            inventory.setItem(homeSlot.getDyeSlot(), infoItem(home != null));
        }
        player.openInventory(inventory);
    }

    public void openManage(Player player, int homeNumber) {
        Inventory inventory = plugin.getServer().createInventory(null, MANAGE_SIZE, manageTitle(homeNumber));
        HomeLocation home = plugin.getHomeService().getHomes(player.getUniqueId()).getHome(homeNumber);
        String displayName = home != null && home.getCustomName() != null ? home.getCustomName() : "Home " + homeNumber;
        inventory.setItem(10, guiItem("manage.teleport", Material.WOOL, DyeColor.LIME.getWoolData(), homeNumber, displayName));
        inventory.setItem(12, guiItem("manage.delete", Material.INK_SACK, 8, homeNumber, displayName));
        inventory.setItem(14, guiItem("manage.rename", Material.NAME_TAG, 0, homeNumber, displayName));
        inventory.setItem(22, guiItem("manage.back", Material.BARRIER, 0, homeNumber, displayName));
        player.openInventory(inventory);
    }

    public HomeSlot homeSlotFromMainSlot(Player player, int slot) {
        HomeLayout layout = createLayout(plugin.getMaxHomes(player));
        for (HomeSlot homeSlot : layout.getSlots()) {
            if (homeSlot.getBedSlot() == slot || homeSlot.getDyeSlot() == slot) {
                return homeSlot;
            }
        }
        return null;
    }

    public String mainTitle() {
        return plugin.guiText("main.title", "&8Homes");
    }

    public String manageTitle(int homeNumber) {
        return plugin.guiText("manage.title", "&8Home %home%").replace("%home%", String.valueOf(homeNumber));
    }

    public HomeLayout createLayout(int maxHomes) {
        int homes = Math.max(1, Math.min(27, maxHomes));
        int pairRows = homes <= 5 ? 1 : (homes <= 10 ? 2 : 3);
        int inventoryRows = pairRows <= 2 ? 5 : 6;
        int startPairRow = Math.max(0, (inventoryRows - pairRows * 2) / 2);

        int basePerRow = homes / pairRows;
        int extraRows = homes % pairRows;
        int nextHome = 1;
        List<HomeSlot> slots = new ArrayList<HomeSlot>();

        for (int rowIndex = 0; rowIndex < pairRows; rowIndex++) {
            int homesInRow = basePerRow + (rowIndex < extraRows ? 1 : 0);
            int startColumn = (9 - homesInRow) / 2;
            int bedRow = startPairRow + rowIndex * 2;
            int dyeRow = bedRow + 1;
            for (int columnOffset = 0; columnOffset < homesInRow; columnOffset++) {
                int column = startColumn + columnOffset;
                slots.add(new HomeSlot(nextHome, bedRow * 9 + column, dyeRow * 9 + column));
                nextHome++;
            }
        }
        return new HomeLayout(inventoryRows * 9, slots);
    }

    private ItemStack emptyHomeItem() {
        return guiItem("main.empty", Material.BED, DyeColor.GRAY.getWoolData(), 0, "");
    }

    private ItemStack filledHomeItem(int number) {
        return guiItem("main.filled", Material.BED, DyeColor.BLUE.getWoolData(), number, "Home " + number);
    }

    private ItemStack infoItem(boolean exists) {
        if (exists) {
            return guiItem("main.manage", Material.INK_SACK, 8, 0, "");
        }
        return guiItem("main.locked", Material.INK_SACK, 8, 0, "");
    }

    private ItemStack guiItem(String path, Material material, int data, int homeNumber, String currentName) {
        String name = plugin.guiText(path + ".name", "&7Item");
        List<String> lore = Colors.color(plugin.getGuiConfig().getStringList(path + ".lore"));
        name = format(name, homeNumber, currentName);
        List<String> formattedLore = new ArrayList<String>();
        for (String line : lore) {
            formattedLore.add(format(line, homeNumber, currentName));
        }
        return Items.item(material, 1, data, name, formattedLore);
    }

    private String format(String text, int homeNumber, String currentName) {
        return text.replace("%home%", String.valueOf(homeNumber)).replace("%name%", currentName);
    }

    public static class HomeLayout {
        private final int inventorySize;
        private final List<HomeSlot> slots;

        private HomeLayout(int inventorySize, List<HomeSlot> slots) {
            this.inventorySize = inventorySize;
            this.slots = slots;
        }

        public int getInventorySize() {
            return inventorySize;
        }

        public List<HomeSlot> getSlots() {
            return slots;
        }
    }

    public static class HomeSlot {
        private final int homeNumber;
        private final int bedSlot;
        private final int dyeSlot;

        private HomeSlot(int homeNumber, int bedSlot, int dyeSlot) {
            this.homeNumber = homeNumber;
            this.bedSlot = bedSlot;
            this.dyeSlot = dyeSlot;
        }

        public int getHomeNumber() {
            return homeNumber;
        }

        public int getBedSlot() {
            return bedSlot;
        }

        public int getDyeSlot() {
            return dyeSlot;
        }
    }
}
