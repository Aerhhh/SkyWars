package net.aerh.skywars.player;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.GameState;
import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.game.island.Island;
import net.aerh.skywars.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlayerManager {

    private final SkyWarsGame skyWarsGame;
    private final Set<SkyWarsPlayer> players;
    private final Set<SkyWarsPlayer> spectators;

    /**
     * Represents the player manager for a {@link SkyWarsGame}.
     *
     * @param skyWarsGame the {@link SkyWarsGame} to initialize the {@link PlayerManager} for
     */
    public PlayerManager(@NotNull SkyWarsGame skyWarsGame) {
        this.skyWarsGame = skyWarsGame;
        this.players = new HashSet<>();
        this.spectators = new HashSet<>();
    }

    /**
     * Adds a {@link SkyWarsPlayer} to the game. Returns false if the game is already running or if there are no islands left.
     *
     * @param player the {@link SkyWarsPlayer} to add
     * @return true if the {@link SkyWarsPlayer} was added, false otherwise
     */
    public boolean addPlayer(SkyWarsPlayer player) {
        skyWarsGame.log(Level.INFO, "Adding player " + player.getUuid());

        Optional<Island> island = skyWarsGame.getIslands().stream()
            .filter(i -> i.getAssignedPlayer() == null)
            .findFirst();

        if (island.isEmpty()) {
            return false;
        }

        if (skyWarsGame.getState() == GameState.IN_GAME || skyWarsGame.getState() == GameState.ENDING) {
            skyWarsGame.log(Level.INFO, "Player " + player.getUuid() + " tried to join but the game is already running!");
            return false;
        }

        skyWarsGame.getPlayerManager().getPlayers().add(player);
        island.get().assignPlayer(player);
        SkyWarsPlugin.getInstance().getServer().getScheduler().runTask(SkyWarsPlugin.getInstance(), () -> island.get().spawnCage());

        skyWarsGame.log(Level.INFO, "Added player " + player.getUuid() + " to island " + Utils.parseLocationToString(island.get().getSpawnLocation()) + "!");

        if (skyWarsGame.getState() != GameState.STARTING) {
            getOnlinePlayers().stream()
                .filter(skyWarsPlayer -> skyWarsPlayer.getScoreboard() != null)
                .forEach(skyWarsPlayer -> {
                    skyWarsPlayer.getScoreboard().add(6, ChatColor.RESET + "Players: " + ChatColor.GREEN + skyWarsGame.getPlayerManager().getAlivePlayers().size());
                    skyWarsPlayer.getScoreboard().update();
                });
        }

        return true;
    }

    /**
     * Removes a {@link SkyWarsPlayer} from the game.
     *
     * @param player the {@link SkyWarsPlayer} to remove
     */
    public void removePlayer(SkyWarsPlayer player) {
        skyWarsGame.getPlayerManager().getPlayers().remove(player);
        skyWarsGame.getPlayerManager().getSpectators().remove(player);

        skyWarsGame.getIsland(player).ifPresent(Island::removePlayer);

        if (skyWarsGame.getState() != GameState.STARTING) {
            getOnlinePlayers().forEach(skyWarsPlayer -> {
                skyWarsPlayer.getScoreboard().add(6, ChatColor.RESET + "Players: " + ChatColor.GREEN + skyWarsGame.getPlayerManager().getAlivePlayers().size());
                skyWarsPlayer.getScoreboard().update();
            });
        }
    }

    /**
     * Gets a {@link SkyWarsPlayer} by their {@link UUID}.
     *
     * @param uuid the {@link UUID} to get
     * @return the {@link SkyWarsPlayer} with the {@link UUID}. Can be null
     */
    public Optional<SkyWarsPlayer> getPlayer(UUID uuid) {
        return players.stream()
            .filter(p -> p.getUuid().equals(uuid))
            .findFirst();
    }

    /**
     * Gets a {@link SkyWarsPlayer} who is a spectator.
     *
     * @param uuid the {@link UUID} of the {@link SkyWarsPlayer} to get
     * @return the {@link SkyWarsPlayer} who is a spectator. Returns null if the {@link SkyWarsPlayer} is not a spectator
     */
    public Optional<SkyWarsPlayer> getSpectator(UUID uuid) {
        return spectators.stream()
            .filter(p -> p.getUuid().equals(uuid))
            .findFirst();
    }

    /**
     * Get all online players in the game.
     *
     * @return A set of all online players.
     */
    public Set<SkyWarsPlayer> getOnlinePlayers() {
        return Stream.concat(players.stream(), spectators.stream())
            .filter(skyWarsPlayer -> skyWarsPlayer.getBukkitPlayer().isPresent())
            .collect(Collectors.toSet());
    }

    /**
     * Gets a {@link List} of all alive {@link SkyWarsPlayer players} in this game.
     *
     * @return the {@link List} of the alive {@link SkyWarsPlayer players} in this game
     */
    public List<SkyWarsPlayer> getAlivePlayers() {
        return players.stream()
            .filter(skyWarsPlayer -> skyWarsPlayer.getBukkitPlayer().isPresent() && !spectators.contains(skyWarsPlayer))
            .toList();
    }

    /**
     * Gets a {@link List} of the {@link Player players} in this game.
     *
     * @return the {@link List} of the {@link Player players} in this game
     */
    public List<Player> getPlayersBukkit() {
        return players.stream()
            .map(SkyWarsPlayer::getBukkitPlayer)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }

    /**
     * Gets the spectators in this game. Can be empty.
     *
     * @return a {@link List} of {@link SkyWarsPlayer players} who are spectating
     */
    public Set<Player> getSpectatorsBukkit() {
        return spectators.stream()
            .map(SkyWarsPlayer::getBukkitPlayer)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
    }

    public Set<SkyWarsPlayer> getPlayers() {
        return players;
    }

    public Set<SkyWarsPlayer> getSpectators() {
        return spectators;
    }
}
