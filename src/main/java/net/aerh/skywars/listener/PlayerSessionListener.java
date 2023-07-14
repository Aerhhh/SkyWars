package net.aerh.skywars.listener;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.SkyWarsGame;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerSessionListener implements Listener {

    private final SkyWarsPlugin plugin;

    public PlayerSessionListener(SkyWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);

        Player player = event.getPlayer();
        SkyWarsGame game = plugin.getGames().values().iterator().next();

        // TODO move to pre login event so they never log in
        if (!game.addPlayer(player)) {
            player.kickPlayer("Game is full!");
            return;
        }

        plugin.getLogger().info("Player " + player.getName() + " joined game " + game.getWorld().getName());
        game.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " joined! " + ChatColor.GRAY + "(" + game.getPlayers().size() + "/" + SkyWarsGame.MAX_PLAYER_COUNT + ")");
        player.teleport(game.getPregameSpawn());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        event.setQuitMessage(null);

        SkyWarsGame game = plugin.findGame(player);
        game.removePlayer(player);
        plugin.getLogger().info("Player " + player.getName() + " left game " + game.getWorld().getName());
        game.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " left! " + ChatColor.GRAY + "(" + game.getPlayers().size() + "/" + SkyWarsGame.MAX_PLAYER_COUNT + ")");
    }

}
