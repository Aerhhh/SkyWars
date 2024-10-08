package net.aerh.skywars;

import net.aerh.skywars.command.EndGameCommand;
import net.aerh.skywars.command.GamesCommand;
import net.aerh.skywars.command.SkipEventCommand;
import net.aerh.skywars.command.StartGameCommand;
import net.aerh.skywars.game.GameManager;
import net.aerh.skywars.game.state.ServerState;
import net.aerh.skywars.listener.GameListener;
import net.aerh.skywars.listener.PlayerSessionListener;
import net.aerh.skywars.util.Utils;
import net.aerh.skywars.util.menu.MenuListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;

public final class SkyWarsPlugin extends JavaPlugin {

    public static final int DESIRED_GAME_COUNT = 5;

    private static SkyWarsPlugin instance;
    private ServerState serverState;
    private GameManager gameManager;

    public static SkyWarsPlugin getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        File[] files = Bukkit.getWorldContainer().listFiles((dir, name) -> name.startsWith("game-"));

        if (files != null) {
            Arrays.stream(files).forEach(file -> Utils.deleteFolder(file.toPath()));
        }
    }

    @Override
    public void onEnable() {
        gameManager = new GameManager();

        Bukkit.getScheduler().runTask(this, () -> {
            gameManager.createGames(DESIRED_GAME_COUNT);
            gameManager.registerCleanupTask();
        });

        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(), this);
        getServer().getPluginManager().registerEvents(new GameListener(), this);

        getCommand("start").setExecutor(new StartGameCommand());
        getCommand("end").setExecutor(new EndGameCommand());
        getCommand("games").setExecutor(new GamesCommand());
        getCommand("skipevent").setExecutor(new SkipEventCommand());
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public ServerState getServerState() {
        return serverState;
    }

    public void setServerState(ServerState serverState) {
        this.serverState = serverState;
    }
}
