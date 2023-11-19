package net.aerh.skywars.game;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.map.MapLoader;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GameManager {

    private final SkyWarsPlugin plugin;
    private final List<SkyWarsGame> games;

    /**
     * Creates a new game manager.
     *
     * @param plugin the plugin
     */
    public GameManager(SkyWarsPlugin plugin) {
        this.plugin = plugin;
        this.games = new ArrayList<>();
    }

    /**
     * Creates the specified amount of games.
     *
     * @param amount the amount of games to create
     */
    public void createGames(int amount) {
        try {
            for (int i = 0; i < amount; i++) {
                games.add(MapLoader.loadRandomMap(plugin, plugin.getDataFolder().getAbsolutePath() + File.separator + "map-templates", "game-" + (i + 1)));
            }
        } catch (IOException | IllegalStateException exception) {
            exception.printStackTrace();
            Bukkit.getServer().shutdown();
        }
    }

    /**
     * Registers the cleanup tasks.
     */
    public void registerCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (games.isEmpty()) {
                plugin.getLogger().info("All games ended, shutting down server!");
                Bukkit.getServer().shutdown();
            }
        }, 0L, 20L * 5L);
    }

    /**
     * Adds a game to the game manager.
     *
     * @param game the {@link SkyWarsGame} to add
     */
    public void addGame(SkyWarsGame game) {
        games.add(game);
    }

    /**
     * Removes a game from the game manager.
     *
     * @param game the {@link SkyWarsGame} to remove
     */
    public void removeGame(SkyWarsGame game) {
        games.remove(game);
    }

    /**
     * Gets a game by its world name.
     *
     * @param worldName the world name
     * @return the {@link SkyWarsGame} or null if not found
     */
    public Optional<SkyWarsGame> getGame(String worldName) {
        return games.stream()
            .filter(game -> game.getWorld().getName().equals(worldName))
            .findFirst();
    }

    /**
     * Finds the next free {@link SkyWarsGame} in the list.
     *
     * @return the next free {@link SkyWarsGame} or null if not found
     */
    public Optional<SkyWarsGame> findNextFreeGame() {
        return games.stream()
            .filter(game -> game.getState() == GameState.PRE_GAME || game.getState() == GameState.STARTING)
            .filter(game -> game.getBukkitPlayers().size() < SkyWarsGame.MAX_PLAYER_COUNT)
            .findFirst();
    }

    /**
     * Finds a game by a {@link Player} object.
     *
     * @param player the {@link Player} to find the game of
     * @return the {@link SkyWarsGame} or null if not found
     */
    public Optional<SkyWarsGame> findGame(Player player) {
        return games.stream()
            .filter(game -> game.getBukkitPlayers().contains(player))
            .findFirst();
    }

    public Optional<SkyWarsGame> findGame(World world) {
        return games.stream()
            .filter(game -> game.getWorld().equals(world))
            .findFirst();
    }

    /**
     * Gets all games.
     *
     * @return a {@link List} of all games
     */
    public List<SkyWarsGame> getGames() {
        return games;
    }
}
