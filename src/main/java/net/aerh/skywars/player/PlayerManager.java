package net.aerh.skywars.player;

import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.game.island.Island;
import net.aerh.skywars.game.state.GameState;
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
            .filter(i -> i.getAssignedPlayer().isEmpty())
            .findFirst();

        if (island.isEmpty()) {
            return false;
        }

        if (skyWarsGame.getState() == GameState.IN_GAME || skyWarsGame.getState() == GameState.ENDING) {
            skyWarsGame.log(Level.INFO, "Player " + player.getUuid() + " tried to join but the game is already running!");
            return false;
        }

        players.add(player);
        skyWarsGame.getKills().putIfAbsent(player.getDisplayName(), 0);
        island.get().assignPlayer(player);

        skyWarsGame.log(Level.INFO, "Added player " + player.getUuid() + " to island " + Utils.parseLocationToString(island.get().getSpawnLocation()) + "!");

        if (skyWarsGame.getState() != GameState.STARTING) {
            getOnlinePlayers().stream()
                .filter(skyWarsPlayer -> skyWarsPlayer.getScoreboard() != null)
                .forEach(skyWarsPlayer -> {
                    skyWarsPlayer.getScoreboard().add(6, ChatColor.RESET + "Players: " + ChatColor.GREEN + getAlivePlayers().size());
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
        players.remove(player);
        spectators.remove(player);

        skyWarsGame.getIsland(player).ifPresent(Island::removePlayer);

        if (skyWarsGame.getState() != GameState.STARTING) {
            getOnlinePlayers().forEach(skyWarsPlayer -> {
                skyWarsPlayer.getScoreboard().add(6, ChatColor.RESET + "Players: " + ChatColor.GREEN + getAlivePlayers().size());
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
     * Gets a {@link Set} of the {@link Player players} in this game.
     *
     * @return the {@link Set} of the {@link Player players} in this game
     */
    public Set<Player> getPlayersBukkit() {
        return players.stream()
            .map(SkyWarsPlayer::getBukkitPlayer)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
    }

    /**
     * Gets a {@link Set} of spectators in this game. Can be empty.
     *
     * @return a {@link Set} of {@link SkyWarsPlayer players} who are spectating
     */
    public Set<Player> getSpectatorsBukkit() {
        return spectators.stream()
            .map(SkyWarsPlayer::getBukkitPlayer)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
    }

    /**
     * Checks if the specified {@link Player} is a member of this game.
     *
     * @param player the {@link Player} to check
     * @return true if the {@link Player} is a member of this game, otherwise false
     */
    public boolean isPlayer(Player player) {
        return players.stream()
            .map(SkyWarsPlayer::getBukkitPlayer)
            .filter(Optional::isPresent)
            .anyMatch(optional -> optional.get().equals(player));
    }

    /**
     * Checks if the specified {@link Player} is a spectator of this game.
     *
     * @param player the {@link Player} to check
     * @return true if the {@link Player} is a spectator of this game, otherwise false
     */
    public boolean isSpectator(Player player) {
        return spectators.stream()
            .map(SkyWarsPlayer::getBukkitPlayer)
            .filter(Optional::isPresent)
            .anyMatch(optional -> optional.get().equals(player));
    }

    public Set<SkyWarsPlayer> getPlayers() {
        return players;
    }

    public Set<SkyWarsPlayer> getSpectators() {
        return spectators;
    }
}
