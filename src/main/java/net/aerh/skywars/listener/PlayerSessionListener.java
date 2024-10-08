package net.aerh.skywars.listener;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.state.GameState;
import net.aerh.skywars.game.state.ServerState;
import net.aerh.skywars.player.PlayerScoreboard;
import net.aerh.skywars.player.SkyWarsPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;

public class PlayerSessionListener implements Listener {

    private static final String GENERIC_KICK_MESSAGE = ChatColor.RED + "An error occurred while trying to join the game!";

    @EventHandler
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        if (SkyWarsPlugin.getInstance().getServerState() != ServerState.ACCEPTING_PLAYERS) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatColor.RED + "You cannot join yet!");
            return;
        }

        SkyWarsPlugin.getInstance().getGameManager().findNextFreeGame().ifPresentOrElse(skyWarsGame -> {
            SkyWarsPlayer skyWarsPlayer = new SkyWarsPlayer(event.getUniqueId(), event.getName());

            if (!skyWarsGame.getPlayerManager().addPlayer(skyWarsPlayer)) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, GENERIC_KICK_MESSAGE);
                SkyWarsPlugin.getInstance().getLogger().severe("Could not add player " + event.getName() + " to game " + skyWarsGame.getWorld().getName() + "!");
                return;
            }

            if (skyWarsGame.getIsland(skyWarsPlayer).isEmpty()) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, GENERIC_KICK_MESSAGE);
                SkyWarsPlugin.getInstance().getLogger().severe("Could not find island for player " + event.getName() + "!");
            }
        }, () -> {
            SkyWarsPlugin.getInstance().setServerState(ServerState.NO_GAMES_AVAILABLE);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatColor.RED + "No games available!");
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        event.setJoinMessage(null);

        player.removeMetadata(GameListener.FALL_DAMAGE_IMMUNITY_METADATA_KEY, SkyWarsPlugin.getInstance());
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0D);
        player.getInventory().clear();
        player.setHealth(20.0D);
        player.setSaturation(20.0F);
        player.setWalkSpeed(0.2F);
        player.setFlySpeed(0.1F);
        Arrays.stream(PotionEffectType.values()).forEach(player::removePotionEffect);

        SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresentOrElse(skyWarsGame -> {
            skyWarsGame.log(Level.INFO, "Player " + player.getName() + " joined the game!");
            player.setGameMode(GameMode.ADVENTURE);

            if (skyWarsGame.getState() == GameState.PRE_GAME || skyWarsGame.getState() == GameState.STARTING) {
                skyWarsGame.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " joined! "
                    + ChatColor.GRAY + "(" + skyWarsGame.getPlayerManager().getPlayersBukkit().size() + "/" + skyWarsGame.getMaxPlayers() + ")");
                skyWarsGame.checkPlayerCountForCountdown();
            }

            skyWarsGame.getPlayerManager().getPlayer(player.getUniqueId()).ifPresentOrElse(skyWarsPlayer -> {
                skyWarsGame.getIsland(skyWarsPlayer).ifPresentOrElse(island -> player.teleport(skyWarsGame.getPregameSpawn()),
                    () -> skyWarsGame.setSpectator(skyWarsPlayer));

                skyWarsPlayer.setScoreboard(new PlayerScoreboard(ChatColor.YELLOW + ChatColor.BOLD.toString() + "SkyWars"));
            }, () -> player.kickPlayer(GENERIC_KICK_MESSAGE));


            Bukkit.getOnlinePlayers().forEach(p -> {
                p.hidePlayer(SkyWarsPlugin.getInstance(), player);
                player.hidePlayer(SkyWarsPlugin.getInstance(), p);
            });

            SkyWarsPlugin.getInstance().getServer().getScheduler().runTaskLater(SkyWarsPlugin.getInstance(), () -> {
                skyWarsGame.getPlayerManager().getOnlinePlayers().stream()
                    .map(SkyWarsPlayer::getBukkitPlayer)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(p -> {
                        p.showPlayer(SkyWarsPlugin.getInstance(), player);
                        player.showPlayer(SkyWarsPlugin.getInstance(), p);
                    });
            }, 1L);
        }, () -> player.kickPlayer(GENERIC_KICK_MESSAGE));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        SkyWarsPlugin.getInstance().getServer().getScheduler().runTaskLater(SkyWarsPlugin.getInstance(), () -> {
            SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresent(skyWarsGame -> {
                skyWarsGame.getPlayerManager().getPlayer(player.getUniqueId()).ifPresent(skywarsPlayer -> {
                    skyWarsGame.getPlayerManager().removePlayer(skywarsPlayer);
                    skyWarsGame.log(Level.INFO, "Player " + player.getName() + " left the game!");

                    if (skyWarsGame.getState() == GameState.PRE_GAME || skyWarsGame.getState() == GameState.STARTING) {
                        skyWarsGame.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " left! "
                            + ChatColor.GRAY + "(" + skyWarsGame.getPlayerManager().getPlayersBukkit().size() + "/" + skyWarsGame.getMaxPlayers() + ")");
                        skyWarsGame.checkPlayerCountForCountdown();
                    } else {
                        skyWarsGame.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " left!");
                    }
                });
            });
        }, 1L);

        event.setQuitMessage(null);
    }
}
