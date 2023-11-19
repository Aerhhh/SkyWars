package net.aerh.skywars.listener;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.GameState;
import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.player.PlayerScoreboard;
import net.aerh.skywars.player.SkyWarsPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.logging.Level;

public class PlayerSessionListener implements Listener {

    private static final String GENERIC_KICK_MESSAGE = ChatColor.RED + "An error occurred while trying to join the game!";

    private final SkyWarsPlugin plugin;

    public PlayerSessionListener(SkyWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        plugin.getGameManager().findNextFreeGame().ifPresentOrElse(skyWarsGame -> {
            SkyWarsPlayer skyWarsPlayer = new SkyWarsPlayer(event.getUniqueId(), event.getName());

            if (!skyWarsGame.addPlayer(skyWarsPlayer)) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, GENERIC_KICK_MESSAGE);
            }

            if (skyWarsGame.getIsland(skyWarsPlayer) == null) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, GENERIC_KICK_MESSAGE);
            }
        }, () -> event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatColor.RED + "No games available!"));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        event.setJoinMessage(null);
        player.getInventory().clear();
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0D);
        player.setHealth(20.0D);
        player.setSaturation(20.0F);

        plugin.getGameManager().findGame(player).ifPresentOrElse(skyWarsGame -> {
            skyWarsGame.log(Level.INFO, "Player " + player.getName() + " joined the game!");
            player.setGameMode(GameMode.ADVENTURE);

            if (skyWarsGame.getState() == GameState.PRE_GAME || skyWarsGame.getState() == GameState.STARTING) {
                skyWarsGame.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " joined! " + ChatColor.GRAY + "(" + skyWarsGame.getBukkitPlayers().size() + "/" + SkyWarsGame.MAX_PLAYER_COUNT + ")");
            }

            skyWarsGame.getPlayer(player).ifPresentOrElse(skyWarsPlayer -> {
                if (skyWarsGame.getState() == GameState.STARTING) {
                    skyWarsGame.getIsland(skyWarsPlayer).ifPresentOrElse(island -> player.teleport(island.getSpawnLocation().clone().add(0.5, 0, 0.5)), () -> skyWarsGame.setSpectator(skyWarsPlayer));
                } else {
                    player.teleport(skyWarsGame.getPregameSpawn());
                }

                skyWarsPlayer.setScoreboard(new PlayerScoreboard(ChatColor.YELLOW + ChatColor.BOLD.toString() + "SkyWars"));
            }, () -> player.kickPlayer(GENERIC_KICK_MESSAGE));


            Bukkit.getOnlinePlayers().forEach(p -> {
                p.hidePlayer(plugin, player);
                player.hidePlayer(plugin, p);
            });

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getGameManager().getGames().stream()
                    .filter(g -> g.equals(skyWarsGame))
                    .forEach(g -> {
                        g.getBukkitPlayers().forEach(p -> {
                            p.showPlayer(plugin, player);
                            player.showPlayer(plugin, p);
                        });
                    });
            }, 1L);
        }, () -> player.kickPlayer(GENERIC_KICK_MESSAGE));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        plugin.getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            skyWarsGame.getPlayer(player).ifPresent(skyWarsPlayer1 -> {
                skyWarsGame.removePlayer(skyWarsPlayer1);
                skyWarsGame.log(Level.INFO, "Player " + player.getName() + " left the game!");

                if (skyWarsGame.getState() == GameState.PRE_GAME || skyWarsGame.getState() == GameState.STARTING) {
                    skyWarsGame.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " left! " + ChatColor.GRAY + "(" + skyWarsGame.getBukkitPlayers().size() + "/" + SkyWarsGame.MAX_PLAYER_COUNT + ")");
                } else {
                    skyWarsGame.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " left!");
                }
            });
        });

        event.setQuitMessage(null);
    }
}
