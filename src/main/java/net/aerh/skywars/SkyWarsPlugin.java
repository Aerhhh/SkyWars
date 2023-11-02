package net.aerh.skywars;

import net.aerh.skywars.command.*;
import net.aerh.skywars.game.GameManager;
import net.aerh.skywars.listener.GameListener;
import net.aerh.skywars.listener.PlayerSessionListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public final class SkyWarsPlugin extends JavaPlugin {

    private static final int DESIRED_GAME_COUNT = 5;

    private GameManager gameManager;

    @Override
    public void onEnable() {
        gameManager = new GameManager(this);

        Bukkit.getScheduler().runTask(this, () -> {
            gameManager.createGames(DESIRED_GAME_COUNT);
            gameManager.registerCleanupTasks();
        });

        getServer().getPluginManager().registerEvents(new PlayerSessionListener(this), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);

        getCommand("start").setExecutor(new StartGameCommand(this));
        getCommand("end").setExecutor(new EndGameCommand(this));
        getCommand("games").setExecutor(new GamesCommand(this));
        getCommand("gameinfo").setExecutor(new GameInfoCommand(this));
        getCommand("skipevent").setExecutor(new SkipEventCommand(this));
    }

    @Override
    public void onDisable() {
            gameManager.getGames().forEach(skyWarsGame -> {
                Bukkit.unloadWorld(skyWarsGame.getWorld(), false);

                try (Stream<Path> path = Files.walk(skyWarsGame.getWorld().getWorldFolder().toPath())) {
                    path.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(file -> {
                            try {
                                Files.delete(file.toPath());
                                getLogger().info("Deleted file " + file.getName());
                            } catch (IOException e) {
                                getLogger().warning("Could not delete file " + file.getName() + ": " + e.getMessage());
                            }
                        });
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            });
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
