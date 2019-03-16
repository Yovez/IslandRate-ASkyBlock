package com.yovez.islandrate.menu;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.yovez.islandrate.IslandRate;

import net.md_5.bungee.api.ChatColor;

public class TopMenu implements Listener {

	final IslandRate plugin;
	private Inventory inv;
	private List<ItemStack> items;

	public TopMenu(IslandRate plugin) {
		this.plugin = plugin;
		inv = Bukkit.createInventory(null, 27, getTitle());
		items = new ArrayList<ItemStack>();
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onTopMenuClick(InventoryClickEvent e) {
		TopMenu menu = new TopMenu(plugin);
		if (!e.getInventory().getTitle().equals(menu.getInv().getTitle())) {
			return;
		}
		e.setCancelled(true);
		if (plugin.getConfig().getBoolean("top_menu.teleport", false) == true) {
			if (e.getCurrentItem().getType().equals(Material.SKULL_ITEM)) {
				SkullMeta meta = (SkullMeta) e.getCurrentItem().getItemMeta();
				Location loc;
				if (!Bukkit.getVersion().contains("1.12")) {
					loc = plugin.getAskyblock().getIslandOwnedBy(Bukkit.getOfflinePlayer(meta.getOwner()).getUniqueId())
							.getSpawnPoint();
				} else
					loc = plugin.getAskyblock().getIslandOwnedBy(meta.getOwningPlayer().getUniqueId()).getSpawnPoint();
				if (loc != null) {
					e.getWhoClicked().teleport(loc);
				}
			}
		}
	}

	private String getTitle() {
		return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("top_menu.title"));
	}

	public ItemStack getSkull(OfflinePlayer player, int place) {
		if (player == null)
			return null;
		ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
		SkullMeta meta = (SkullMeta) item.getItemMeta();
		meta.setDisplayName(plugin.getMessage("top_menu.items.skull.display_name", null, player, 0, place));
		meta.setOwningPlayer(player);
		meta.setLore(plugin.getConvertedLore("top_menu.items.skull", player));
		item.setItemMeta(meta);
		if (!items.contains(item))
			items.add(item);
		return item;
	}

	public void openInv(Player p) {
		p.openInventory(createInv());
	}

	public void populateItems() {
		// try {
		// ResultSet rs = api.getTopTenSQL();
		for (int i = 1; i < 11; i++) {
			if (plugin.getAPI().getTotalRatings(plugin.getAPI().getTopRated(i)) == 0)
				break;
			getSkull(plugin.getAPI().getTopRated(i), i);
		}
		// } catch (SQLException e) {
		// e.printStackTrace();
		// }
	}

	public Inventory createInv() {
		inv = Bukkit.createInventory(null, 27, getTitle());
		int place[] = { 4, 12, 14, 19, 20, 21, 22, 23, 24, 25 };
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

	public Inventory getInv() {
		return inv;
	}

	public void setInv(Inventory inv) {
		this.inv = inv;
	}

	public List<ItemStack> getItems() {
		return items;
	}

	public void setItems(List<ItemStack> items) {
		this.items = items;
	}

}
