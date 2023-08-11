package net.aerh.skywars;

import net.aerh.skywars.command.GamesCommand;
import net.aerh.skywars.command.SkipEventCommand;
import net.aerh.skywars.command.StartGameCommand;
import net.aerh.skywars.command.TestChestCommand;
import net.aerh.skywars.game.GameState;
import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.listener.GameListener;
import net.aerh.skywars.listener.PlayerSessionListener;
import net.aerh.skywars.map.MapLoader;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public final class SkyWarsPlugin extends JavaPlugin {

    private static final int DESIRED_GAME_COUNT = 5;

    private final Set<SkyWarsGame> games = new HashSet<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(this), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);

        getCommand("start").setExecutor(new StartGameCommand(this));
        getCommand("games").setExecutor(new GamesCommand(this));
        getCommand("testchest").setExecutor(new TestChestCommand(this));
        getCommand("skipevent").setExecutor(new SkipEventCommand(this));

        Bukkit.getScheduler().runTask(this, () -> {
            MapLoader mapLoader = new MapLoader(this, getDataFolder().getAbsolutePath() + File.separator + "map-templates");
            try {
                for (int i = 0; i < DESIRED_GAME_COUNT; i++) {
                    SkyWarsGame game = mapLoader.loadRandomMap("game-" + (i + 1));
                    games.add(game);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalStateException exception) {
                exception.printStackTrace();
                Bukkit.getServer().shutdown();
            }
        });

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (games.isEmpty()) {
                getLogger().info("All games have ended, shutting down server!");
                Bukkit.getServer().shutdown();
            }
        }, 0L, 20L * 5L);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            games.forEach(game -> {
                try {
                    getLogger().info("Attempting to delete world " + game.getWorld().getName());
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

    @Override
    public void onDisable() {
    }

    public Set<SkyWarsGame> getGames() {
        return games;
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
}
