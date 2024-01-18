/*
 *  Copyright (C) 2024  JNNGL
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jnngl.vanillaminimaps;

import com.jnngl.vanillaminimaps.clientside.ClientsideMinimapFactory;
import com.jnngl.vanillaminimaps.clientside.MinimapPacketSender;
import com.jnngl.vanillaminimaps.clientside.impl.NMSClientsideMinimapFactory;
import com.jnngl.vanillaminimaps.clientside.impl.NMSMinimapPacketSender;
import com.jnngl.vanillaminimaps.injection.PassengerRewriter;
import com.jnngl.vanillaminimaps.listener.MinimapBlockListener;
import com.jnngl.vanillaminimaps.listener.MinimapListener;
import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.renderer.world.VanillaWorldMinimapRenderer;
import com.jnngl.vanillaminimaps.map.renderer.world.WorldMinimapRenderer;
import com.jnngl.vanillaminimaps.map.renderer.world.cache.CacheableWorldMinimapRenderer;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.HashMap;
import java.util.Map;

public final class VanillaMinimaps extends JavaPlugin implements Listener {

  @Getter
  private final Map<Player, PassengerRewriter> passengerRewriters = new HashMap<>();

  @Getter
  @MonotonicNonNull
  private ClientsideMinimapFactory defaultClientsideMinimapFactory;

  @Getter
  @MonotonicNonNull
  private MinimapPacketSender defaultMinimapPacketSender;

  @Getter
  @MonotonicNonNull
  private WorldMinimapRenderer defaultWorldRenderer;

  @Getter
  @MonotonicNonNull
  private MinimapBlockListener minimapBlockListener;

  @Getter
  @MonotonicNonNull
  private MinimapListener minimapListener;

  @Override
  public void onEnable() {
    defaultClientsideMinimapFactory = new NMSClientsideMinimapFactory();
    defaultMinimapPacketSender = new NMSMinimapPacketSender(this);
    defaultWorldRenderer = new VanillaWorldMinimapRenderer();
    minimapBlockListener = new MinimapBlockListener(this);
    minimapListener = new MinimapListener(this);

    if (defaultWorldRenderer instanceof CacheableWorldMinimapRenderer cacheable) {
      minimapBlockListener.registerCache(cacheable.getWorldMapCache());
    }

    Bukkit.getPluginManager().registerEvents(this, this);
    Bukkit.getPluginManager().registerEvents(minimapListener, this);
    minimapBlockListener.registerListener(this);
  }

  public PassengerRewriter getPassengerRewriter(Player player) {
    return passengerRewriters.get(player);
  }

  public Minimap getPlayerMinimap(Player player) {
    return minimapListener.getPlayerMinimaps().get(player);
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerJoin(PlayerJoinEvent event) {
    PassengerRewriter rewriter = new PassengerRewriter();
    ((CraftPlayer) event.getPlayer()).getHandle().connection.connection.channel.pipeline().addBefore("packet_handler", "passenger_rewriter", rewriter);
    passengerRewriters.put(event.getPlayer(), rewriter);
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerQuit(PlayerQuitEvent event) {
    passengerRewriters.remove(event.getPlayer());
  }
}
