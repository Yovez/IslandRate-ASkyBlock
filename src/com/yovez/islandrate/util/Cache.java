package com.yovez.islandrate.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import com.yovez.islandrate.IslandRate;

public class Cache {

	private Map<UUID, Integer> cache;
	private final IslandRate plugin;

	public Cache(IslandRate plugin) {
		cache = new HashMap<>();
		this.plugin = plugin;
	}

	public Map<UUID, Integer> get() {
		return cache;
	}

	public int getRatings(OfflinePlayer p) {
		return cache.get(p.getUniqueId());
	}

	public void addTopTen() {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				OfflinePlayer p = null;
				for (int i = 0; i <= 10; i++) {
					p = plugin.getAPI().getTopRated(i);
					add(p);
				}
			}
		});
	}

	public void add(OfflinePlayer p) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				if (p != null) {
					cache.put(p.getUniqueId(), plugin.getAPI().getTotalRatings(p));
				}
			}
		});
	}

	public void remove(OfflinePlayer p) {
		if (cache.containsKey(p.getUniqueId()))
			cache.remove(p.getUniqueId());
	}

}
