package net.aerh.skywars;

import net.aerh.skywars.command.EndGameCommand;
import net.aerh.skywars.command.GamesCommand;
import net.aerh.skywars.command.SkipEventCommand;
import net.aerh.skywars.command.StartGameCommand;
import net.aerh.skywars.game.GameManager;
import net.aerh.skywars.listener.GameListener;
import net.aerh.skywars.listener.PlayerSessionListener;
import net.aerh.skywars.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public final class SkyWarsPlugin extends JavaPlugin {

    private static final int DESIRED_GAME_COUNT = 5;

    private GameManager gameManager;

    @Override
    public void onEnable() {
        Arrays.stream(Bukkit.getWorldContainer().listFiles((dir, name) -> name.startsWith("game-"))).forEach(file -> Utils.deleteFolder(file.toPath()));

        gameManager = new GameManager(this);

        Bukkit.getScheduler().runTask(this, () -> {
            gameManager.createGames(DESIRED_GAME_COUNT);
            gameManager.registerCleanupTask();
        });

        getServer().getPluginManager().registerEvents(new PlayerSessionListener(this), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);

        getCommand("start").setExecutor(new StartGameCommand(this));
        getCommand("end").setExecutor(new EndGameCommand(this));
        getCommand("games").setExecutor(new GamesCommand(this));
        getCommand("skipevent").setExecutor(new SkipEventCommand(this));
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
