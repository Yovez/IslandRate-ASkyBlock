package com.yovez.islandrate.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import com.yovez.islandrate.IslandRate;

public class Cache {

	private Map<UUID, Integer> cache;
	private Map<UUID, Double> average;
	private Map<UUID, Integer> raters;
	private List<UUID> topTen;
	private int totalRatings;

	private final IslandRate plugin;

	public Cache(IslandRate plugin) {
		cache = new HashMap<>();
		topTen = new ArrayList<>();
		this.plugin = plugin;
	}

	public Map<UUID, Integer> get() {
		return cache;
	}

	public int getRatings(OfflinePlayer p) {
		if (cache.get(p.getUniqueId()) != null)
			return 0;
		return cache.get(p.getUniqueId());
	}

	public void addTopTen() {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				OfflinePlayer p = null;
				for (int i = 0; i <= 10; i++) {
					p = plugin.getAPI().getTopRated(i);
					topTen.add(i, p.getUniqueId());
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

	public List<UUID> getTopTen() {
		return topTen;
	}

	public Map<UUID, Double> getAverage() {
		return average;
	}

	public Map<UUID, Integer> getRaters() {
		return raters;
	}

	public int getTotalRatings() {
		return totalRatings;
	}

}
