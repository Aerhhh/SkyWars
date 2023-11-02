package net.aerh.skywars.game;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.map.MapLoader;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class GameManager {

    private final SkyWarsPlugin plugin;
    private final List<SkyWarsGame> games;

    public GameManager(SkyWarsPlugin plugin) {
        this.plugin = plugin;
        this.games = new ArrayList<>();
    }

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

    public void registerCleanupTasks() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (games.isEmpty()) {
                plugin.getLogger().info("All games ended, shutting down server!");
                Bukkit.getServer().shutdown();
            }
        }, 0L, 20L * 5L);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            games.forEach(game -> {
                try {
                    try (Stream<Path> path = Files.walk(game.getWorld().getWorldFolder().toPath())) {
                        path.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }));
    }

    public void addGame(SkyWarsGame game) {
        games.add(game);
    }

    public void removeGame(SkyWarsGame game) {
        games.remove(game);
    }

    @Nullable
    public SkyWarsGame getGame(String worldName) {
        return games.stream()
            .filter(game -> game.getWorld().getName().equals(worldName))
            .findFirst()
            .orElse(null);
    }

    @Nullable
    public SkyWarsGame findNextFreeGame() {
        return games.stream()
            .filter(game -> game.getState() == GameState.PRE_GAME || game.getState() == GameState.STARTING)
            .filter(game -> game.getBukkitPlayers().size() < SkyWarsGame.MAX_PLAYER_COUNT)
            .findFirst()
            .orElse(null);
    }

    @Nullable
    public SkyWarsGame findGame(Player player) {
        return games.stream()
            .filter(game -> game.getBukkitSpectators().contains(player) || game.getPlayer(player) != null)
            .findFirst()
            .orElse(null);
    }

    public List<SkyWarsGame> getGames() {
        return games;
    }
}
