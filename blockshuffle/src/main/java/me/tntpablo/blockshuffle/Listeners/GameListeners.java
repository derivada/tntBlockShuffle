package me.tntpablo.blockshuffle.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import me.tntpablo.blockshuffle.Main;

public class GameListeners implements Listener {
    private Main plugin;

    GameListeners(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Bukkit.broadcastMessage("TEST");
        Player p = e.getPlayer();
        if (plugin.shuffleCore.isAlive(p)) {
            Bukkit.broadcastMessage("TEST 2");
            new BukkitRunnable() {
                int timeLeft = 60;
                @Override
                public void run() {
                    if (timeLeft == 60)
                        plugin.shuffleCore
                                .msgAll(p.getName() + " se ha desconectado y tiene 1 minuto para reconectarse!");

                    timeLeft--;
                    if (timeLeft == 0) {
                        plugin.shuffleCore.msgAll(p.getName() + " ha sido eliminado!");
                        plugin.shuffleCore.playerEliminate(p);
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 20);
        }
    }
}