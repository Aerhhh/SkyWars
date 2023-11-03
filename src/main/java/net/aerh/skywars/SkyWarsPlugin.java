package net.aerh.skywars;

import net.aerh.skywars.command.*;
import net.aerh.skywars.game.GameManager;
import net.aerh.skywars.listener.GameListener;
import net.aerh.skywars.listener.PlayerSessionListener;
import net.aerh.skywars.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

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
                Utils.deleteFolder(skyWarsGame.getWorld().getWorldFolder().toPath());
            });
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
