package com.yovez.islandrate.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.yovez.islandrate.IslandRate;

public class PlayerListener implements Listener {

	private final IslandRate plugin;

	public PlayerListener(IslandRate plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		plugin.getCache().add(e.getPlayer());
	}

}
