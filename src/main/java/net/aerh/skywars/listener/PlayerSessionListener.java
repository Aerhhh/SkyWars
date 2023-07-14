package net.aerh.skywars.listener;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.player.SkyWarsPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerSessionListener implements Listener {

    private final SkyWarsPlugin plugin;

    public PlayerSessionListener(SkyWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        SkyWarsGame game = plugin.findNextFreeGame();

        if (plugin.getGames().isEmpty() || game == null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatColor.RED + "No games available!");
            return;
        }

        SkyWarsPlayer skyWarsPlayer = new SkyWarsPlayer(event.getUniqueId());

        if (!game.addPlayer(skyWarsPlayer)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatColor.RED + "Couldn't add you to the game!");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);

        Player player = event.getPlayer();
        SkyWarsGame game = plugin.findGame(player);
        SkyWarsPlayer skyWarsPlayer = game.getPlayer(player);

        plugin.getLogger().info("Player " + player.getName() + " joined game " + game.getWorld().getName());
        game.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " joined! " + ChatColor.GRAY + "(" + game.getPlayers().size() + "/" + SkyWarsGame.MAX_PLAYER_COUNT + ")");

        // TODO teleport players to their assigned island if it's counting down
        player.teleport(game.getPregameSpawn());

        skyWarsPlayer.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        Bukkit.getOnlinePlayers().forEach(p -> p.hidePlayer(plugin, player));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getGames().values()
                .stream()
                .filter(g -> g.equals(game))
                .forEach(g -> {
                    g.getPlayers().forEach(p -> p.getBukkitPlayer().showPlayer(plugin, player));
                    g.getPlayers().forEach(p -> player.showPlayer(plugin, p.getBukkitPlayer()));
                });
        }, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);

        Player player = event.getPlayer();
        SkyWarsGame game = plugin.findGame(player);

        if (game == null) {
            return;
        }

        game.removePlayer(player);
        plugin.getLogger().info("Player " + player.getName() + " left game " + game.getWorld().getName());
        game.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " left! " + ChatColor.GRAY + "(" + game.getPlayers().size() + "/" + SkyWarsGame.MAX_PLAYER_COUNT + ")");
    }
}
