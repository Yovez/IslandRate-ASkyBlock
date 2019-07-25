package com.yovez.islandrate.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.wasteofplastic.askyblock.Island;
import com.yovez.islandrate.IslandRate;

public class IslandMenu implements Listener {

	final IslandRate plugin;
	private Inventory inv;
	private Player player;
	private List<ItemStack> items;

	public IslandMenu(IslandRate plugin) {
		this.plugin = plugin;
	}

	public IslandMenu(IslandRate plugin, Player player) {
		this.plugin = plugin;
		this.player = player;
		inv = Bukkit.createInventory(player, 9, getTitle());
		items = new ArrayList<ItemStack>();
	}

	@EventHandler
	public void onIslandMenuClick(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();
		IslandMenu menu = new IslandMenu(plugin, p);
		if (!e.getInventory().getTitle().equals(menu.getInv().getTitle())) {
			return;
		}
		Island island = plugin.getAskyblock().getIslandAt(p.getLocation());
		if (island == null)
			return;
		e.setCancelled(true);
		Bukkit.getServer().getScheduler().runTask(plugin, new Runnable() {

			@Override
			public void run() {
				if (plugin.getConfig().getBoolean("island_menu.custom", false) == false)
					menu.openInv();
				else
					menu.openCustomInv();
			}

		});
		ItemStack item = e.getCurrentItem();
		if (item.equals(menu.getOptOut())) {
			plugin.getOptOut().getConfig().set(p.getUniqueId().toString(),
					!plugin.getOptOut().getConfig().getBoolean(p.getUniqueId().toString(), false));
			plugin.getOptOut().saveConfig();
		}
	}

	private String getTitle() {
		return ChatColor.translateAlternateColorCodes('&', plugin.getMessage("island_menu.title", player, null, 0, 0));
	}

	@SuppressWarnings("deprecation")
	public ItemStack getSkull() {
		ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
		SkullMeta meta = (SkullMeta) item.getItemMeta();
		meta.setDisplayName("§r§f" + player.getName());
		if (Bukkit.getVersion().contains("1.7") || Bukkit.getVersion().contains("1.8"))
			meta.setOwner(player.getName());
		else
			meta.setOwningPlayer(player);
		meta.setLore(Arrays.asList("§6Total Ratings: §e" + plugin.getAPI().getTotalRatings(player)));
		item.setItemMeta(meta);
		if (!items.contains(item))
			items.add(item);
		return item;
	}

	public ItemStack getOptOut() {
		ItemStack item = new ItemStack(Material.BARRIER);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName("§bToggle Opted-Out");
		meta.setLore(Arrays.asList("§6Opted-Out: §r"
				+ (plugin.getOptOut().getConfig().getBoolean(player.getUniqueId().toString(), false) ? "§aTrue"
						: "§cFalse")));
		item.setItemMeta(meta);
		if (!items.contains(item))
			items.add(item);
		return item;
	}

	public void openInv() {
		player.openInventory(createInv(player));
	}

	public void openCustomInv() {
		player.openInventory(createCustomInv());
	}

	public void populateItems() {
		if (plugin.getConfig().getBoolean("island_menu.custom", false) == false) {
			getSkull();
			getOptOut();
		} else {
			setupItems();
		}
	}

	public void setupItems() {
		items = new ArrayList<ItemStack>();
		for (String s : plugin.getConfig().getConfigurationSection("island_menu.items").getKeys(false)) {
			s = "island_menu.items." + s;
			if (s.equalsIgnoreCase("island_menu.items.skull"))
				continue;
			if (plugin.getConfigItem(s, player) != null)
				if (!items.contains(plugin.getConfigItem(s, player)))
					items.add(plugin.getConfigItem(s, player));
		}
	}

	public Inventory createInv(Player p) {
		inv = Bukkit.createInventory(p, 9, getTitle());
		int place[] = { 0, 2 };
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

			@Override
			public void run() {
				if (items == null || items.isEmpty())
					populateItems();
				for (int i = 0; i < items.size(); i++) {
					inv.setItem(place[i], items.get(i));
				}
			}

		});
		return inv;
	}

	public Inventory createCustomInv() {
		inv = Bukkit.createInventory(player, plugin.getConfig().getInt("island_menu.size", 9), getTitle());
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

			@Override
			public void run() {
				for (String s : plugin.getConfig().getConfigurationSection("island_menu.items").getKeys(false)) {
					if (s == null)
						continue;
					s = "island_menu.items." + s;
					if (s.equalsIgnoreCase("island_menu.items.skull")) {
						ItemStack item = new ItemStack(Material.SKULL_ITEM, plugin.getConfig().getInt(s + ".amount"),
								(short) 3);
						SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
						skullMeta.setDisplayName(plugin.getMessage(s + ".display_name", null, player, 0, 0));
						skullMeta.setOwningPlayer(player);
						skullMeta.setLore(plugin.getConvertedLore(s, player));
						item.setItemMeta(skullMeta);
						inv.setItem(plugin.getConfig().getInt(s + ".slot"), item);
					} else {
						if (plugin.getConfigItem(s, player) != null)
							inv.setItem(plugin.getConfig().getInt(s + ".slot"), plugin.getConfigItem(s, player));
					}
				}
			}

		});
		return inv;

	}

	public Inventory getInv() {
		return inv;
	}

	public void setInv(Inventory inv) {
		this.inv = inv;
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

}
