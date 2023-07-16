package net.aerh.skywars.listener;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.GameState;
import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.player.SkyWarsPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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

        if (game == null) {
            return;
        }

        SkyWarsPlayer skyWarsPlayer = game.getPlayer(player);
        plugin.getLogger().info("Player " + player.getName() + " joined game " + game.getWorld().getName());
        player.setGameMode(GameMode.ADVENTURE);

        if (game.getState() == GameState.PRE_GAME || game.getState() == GameState.STARTING) {
            game.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " joined! " + ChatColor.GRAY + "(" + game.getBukkitPlayers().size() + "/" + SkyWarsGame.MAX_PLAYER_COUNT + ")");
        }

        if (game.getState() == GameState.STARTING) {
            player.teleport(game.getIsland(player).getSpawnLocation());
        } else {
            player.teleport(game.getPregameSpawn());
        }

        skyWarsPlayer.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        // todo fix issue with hiding and showing players in other games
        Bukkit.getOnlinePlayers().forEach(p -> p.hidePlayer(plugin, player));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getGames().stream()
                .filter(g -> g.equals(game))
                .forEach(g -> {
                    g.getBukkitPlayers().forEach(p -> {
                        p.showPlayer(plugin, player);
                        player.showPlayer(plugin, p);
                    });
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

        game.removePlayerFromPlayersOrSpectators(player);
        plugin.getLogger().info("Player " + player.getName() + " left game " + game.getWorld().getName());

        if (game.getState() == GameState.PRE_GAME || game.getState() == GameState.STARTING) {
            game.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " left! " + ChatColor.GRAY + "(" + game.getBukkitPlayers().size() + "/" + SkyWarsGame.MAX_PLAYER_COUNT + ")");
        }
    }
}
