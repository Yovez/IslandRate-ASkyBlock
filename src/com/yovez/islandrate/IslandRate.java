package com.yovez.islandrate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.wasteofplastic.askyblock.ASkyBlockAPI;
import com.wasteofplastic.askyblock.Island;
import com.yovez.islandrate.api.IslandRateAPI;
import com.yovez.islandrate.command.RateCommand;
import com.yovez.islandrate.listener.SignListener;
import com.yovez.islandrate.menu.IslandMenu;
import com.yovez.islandrate.menu.RateMenu;
import com.yovez.islandrate.menu.TopMenu;
import com.yovez.islandrate.misc.CustomConfig;
import com.yovez.islandrate.misc.InventoryCheck;
import com.yovez.islandrate.misc.Metrics;
import com.yovez.islandrate.misc.MySQL;
import com.yovez.islandrate.misc.Placeholders;
import com.yovez.islandrate.util.DbUtils;
import com.yovez.islandrate.util.Parser;

public class IslandRate extends JavaPlugin {

	private MySQL mysql;
	private ASkyBlockAPI askyblock;
	private IslandRateAPI api;
	private Map<UUID, Long> cooldown;
	// private Cache cache;
	private CustomConfig messages, optOut, storage;

	private static IslandRate plugin;

	@Override
	public void onEnable() {
		plugin = this;
		saveDefaultConfig();
		messages = new CustomConfig(this, "messages");
		messages.saveDefaultConfig();
		optOut = new CustomConfig(this, "opt-out");
		optOut.saveDefaultConfig();
		storage = new CustomConfig(this, "storage");
		storage.saveDefaultConfig();
		askyblock = ASkyBlockAPI.getInstance();
		api = IslandRateAPI.getInstance();
		mysql = MySQL.getInstance();
		getCommand("rate").setExecutor(new RateCommand(this));
		if (Bukkit.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			new Placeholders(this);
		}
		Bukkit.getServer().getPluginManager().registerEvents(new RateMenu(this), this);
		Bukkit.getServer().getPluginManager().registerEvents(new TopMenu(this), this);
		Bukkit.getServer().getPluginManager().registerEvents(new IslandMenu(this), this);
		Bukkit.getServer().getPluginManager().registerEvents(new SignListener(this), this);
		/*
		 * if (plugin.getConfig().getBoolean("use-cache-system", false) == true) { cache
		 * = new Cache(this);
		 * Bukkit.getServer().getScheduler().runTaskAsynchronously(this, cache);
		 * Bukkit.getServer().getPluginManager().registerEvents(new
		 * PlayerListener(this), this); }
		 */
		// Keep cache to dev builds only...
		if (plugin.getConfig().getInt("cooldown", 60) > 0)
			cooldown = new HashMap<UUID, Long>();
		if (getConfig().getBoolean("inv_check.enabled", false) == true)
			Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new InventoryCheck(this), 0L,
					getConfig().getLong("inv_check.timer") * 1000);
		new Metrics(this);
		new Parser();
	}

	public static IslandRate getInstance() {
		return plugin;
	}

	public String getMessage(String msg, Player p, OfflinePlayer t, int rating, int topPlace) {
		return Parser.getMessage(msg, p, t, rating, topPlace);
	}

	@Override
	public void onDisable() {
		messages.saveConfig();
		optOut.saveConfig();
		try {
			if (mysql != null)
				if (mysql.getConnection() != null && !mysql.getConnection().isClosed())
					DbUtils.close(mysql.getConnection());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	public ItemStack getConfigItem(String path, OfflinePlayer op) {
		if (!getConfig().contains(path))
			return null;
		ItemStack item = null;
		if (getConfig().isString(path + ".material"))
			item = new ItemStack(Material.matchMaterial(getConfig().getString(path + ".material").toUpperCase()),
					getConfig().getInt(path + ".amount", 1), (short) getConfig().getInt(path + ".data", 0));
		else
			item = new ItemStack(Material.getMaterial(getConfig().getInt(path + ".material")),
					getConfig().getInt(path + ".amount", 1), (short) getConfig().getInt(path + ".data", 0));
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(getMessage(path + ".display_name", null, op, 0, 0));
		meta.setLore(getConvertedLore(path, op));
		item.setItemMeta(meta);
		return item;
	}

	public List<String> getConvertedLore(String path, OfflinePlayer op) {
		List<String> oldList = getConfig().getStringList(path + ".lore");
		List<String> newList = new ArrayList<String>();
		for (String a : oldList) {
			newList.add(getMessage(a, null, op, 0, 0));
		}
		return newList;
	}

	public void rateIsland(Player p, OfflinePlayer op, int rating) {
		if (p == null) {
			Bukkit.getConsoleSender().sendMessage(
					"�2[IslandRate] �4WARNING: �cAn error occured! Please tell the developer about this error! (P391)");
			return;
		}
		if (op == null) {
			p.sendMessage(getMessage("no-island", p, null, 0, 0));
			p.playSound(p.getLocation(),
					Sound.valueOf(
							Bukkit.getVersion().contains("1.7") || Bukkit.getVersion().contains("1.8") ? "ANVIL_BREAK"
									: "BLOCK_ANVIL_BREAK"),
					100, 100);
			return;
		}
		if (rating < 1) {
			Bukkit.getConsoleSender().sendMessage(
					"�2[IslandRate] �4WARNING: �cAn error occured! Please tell the developer about this error! (R393)");
			return;
		}
		Island island = getAskyblock().getIslandAt(p.getLocation());
		if (island == null) {
			p.sendMessage(getMessage("no-island", p, null, 0, 0));
			p.playSound(p.getLocation(),
					Sound.valueOf(
							Bukkit.getVersion().contains("1.7") || Bukkit.getVersion().contains("1.8") ? "ANVIL_BREAK"
									: "BLOCK_ANVIL_BREAK"),
					100, 100);
			return;
		}
		if (optOut.getConfig().getBoolean(op.getUniqueId().toString(), false) == true) {
			p.sendMessage(getMessage("opted-out", p, op, 0, 0));
			return;
		}
		if (getConfig().getInt("min-island-level", 0) > 0)
			if (askyblock.getLongIslandLevel(p.getUniqueId()) < getConfig().getLong("min-island-level", 0)) {
				p.sendMessage(getMessage("incorrect-level", p, op, 0, 0));
				return;
			}
		if (getConfig().getBoolean("change-rating", true) == false) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				conn = getMySQL().getConnection();
				ps = conn.prepareStatement(
						"SELECT rater_uuid, player_uuid FROM island_ratings WHERE (rater_uuid = ? AND player_uuid = ?);");
				ps.setString(1, p.getUniqueId().toString());
				ps.setString(2, op.getUniqueId().toString());
				rs = ps.executeQuery();
				if (rs.next()) {
					p.sendMessage(getMessage("already-rated-island", p,
							Bukkit.getServer().getOfflinePlayer(island.getOwner()), rating, 0));
					p.playSound(p.getLocation(),
							Sound.valueOf(Bukkit.getVersion().contains("1.7") || Bukkit.getVersion().contains("1.8")
									? "ANVIL_BREAK"
									: "BLOCK_ANVIL_BREAK"),
							100, 100);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				try {
					DbUtils.close(rs);
					DbUtils.close(ps);
					DbUtils.close(conn);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			return;
		}
		if (cooldown != null)
			if (cooldown.containsKey(p.getUniqueId())) {
				long timeLeft = cooldown.get(p.getUniqueId());
				if (timeLeft >= (System.currentTimeMillis() / 1000) - getConfig().getInt("cooldown")) {
					p.sendMessage(getMessage("cooldown-time", p, null, rating, 0));
					return;
				} else {
					cooldown.remove(p.getUniqueId());
				}
			}
		Connection conn = null;
		PreparedStatement psDuplicateCheck = null, ps = null, psr = null;
		ResultSet rs = null;
		int previousRating = 0;
		int totalRatings = getAPI().getTotalRatings(op);
		try {
			conn = getMySQL().getConnection();
			psDuplicateCheck = conn
					.prepareStatement("SELECT * FROM island_ratings WHERE (rater_uuid = ? AND player_uuid = ?);");
			psDuplicateCheck.setString(1, p.getUniqueId().toString());
			psDuplicateCheck.setString(2, op.getUniqueId().toString());
			rs = psDuplicateCheck.executeQuery();
			if (rs.next()) {
				previousRating = rs.getInt("rating");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				DbUtils.close(rs);
				DbUtils.close(psDuplicateCheck);
				DbUtils.close(conn);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		try {
			conn = getMySQL().getConnection();
			ps = conn.prepareStatement("REPLACE INTO island_owners(player_uuid, total_ratings) VALUES (?,?);");
			ps.setString(1, op.getUniqueId().toString());
			ps.setInt(2, (totalRatings + rating) - previousRating);
			ps.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				DbUtils.close(ps);
				DbUtils.close(conn);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		try {
			conn = getMySQL().getConnection();
			// psr = conn
			// .prepareStatement("UPDATE island_ratings SET rating = ? WHERE rater_uuid = ?
			// AND player_uuid = ?");
			PreparedStatement del = conn
					.prepareStatement("DELETE FROM island_ratings WHERE rater_uuid = ? AND player_uuid = ?;");
			del.setString(1, p.getUniqueId().toString());
			del.setString(2, op.getUniqueId().toString());
			del.execute();
			DbUtils.close(del);
			psr = conn.prepareStatement("REPLACE INTO island_ratings(rater_uuid, player_uuid, rating) VALUES (?,?,?);");
			psr.setString(1, p.getUniqueId().toString());
			psr.setString(2, op.getUniqueId().toString());
			psr.setInt(3, rating);
			psr.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				DbUtils.close(psr);
				DbUtils.close(conn);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		p.playSound(p.getLocation(),
				Sound.valueOf(Bukkit.getVersion().contains("1.7") || Bukkit.getVersion().contains("1.8") ? "LEVEL_UP"
						: "ENTITY_PLAYER_LEVELUP"),
				100, 100);
		if (rating != 1)
			p.sendMessage(getMessage("successfull-rate-plural", p,
					Bukkit.getServer().getOfflinePlayer(island.getOwner()), rating, 0));
		else
			p.sendMessage(getMessage("successfull-rate", p, Bukkit.getServer().getOfflinePlayer(island.getOwner()),
					rating, 0));
		if (Bukkit.getOfflinePlayer(island.getOwner()).isOnline()
				&& getConfig().getBoolean("send-owner-message", false) == true)
			Bukkit.getPlayer(island.getOwner()).sendMessage(getMessage("owner-message", p, p, rating, 0));
		if (cooldown != null)
			cooldown.put(p.getUniqueId(), TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
		if (getConfig().getBoolean("logging.enabled", false) == true)
			if (getConfig().getBoolean("logging.rate-island", false) == true)
				Bukkit.getConsoleSender().sendMessage("�2[IslandRate] �eLOG�f: " + p.getName() + " rated "
						+ op.getName() + "'s Island " + rating + ".");
	}

	public ASkyBlockAPI getAskyblock() {
		return askyblock;
	}

	public MySQL getMySQL() {
		return mysql;
	}

	public IslandRateAPI getAPI() {
		return api;
	}

	public CustomConfig getMessages() {
		return messages;
	}

	public CustomConfig getOptOut() {
		return optOut;
	}

	public CustomConfig getStorage() {
		return storage;
	}

	public Map<UUID, Long> getCooldowns() {
		return cooldown;
	}

	/*
	 * public Cache getCache() { return cache; }
	 */
}
