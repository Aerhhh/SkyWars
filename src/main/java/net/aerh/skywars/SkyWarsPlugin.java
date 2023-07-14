package net.aerh.skywars;

import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.listener.PlayerSessionListener;
import net.aerh.skywars.map.MapLoader;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SkyWarsPlugin extends JavaPlugin {

    private static final int DESIRED_GAME_COUNT = 5;

    private final Map<UUID, SkyWarsGame> games = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(this), this);

        Bukkit.getScheduler().runTask(this, () -> {
            MapLoader mapLoader = new MapLoader(this, getDataFolder().getAbsolutePath() + File.separator + "/map-templates");
            try {
                for (int i = 0; i < DESIRED_GAME_COUNT; i++) {
                    SkyWarsGame game = mapLoader.loadRandomMap("game-" + (i + 1));
                    games.put(UUID.randomUUID(), game);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalStateException exception) {
                exception.printStackTrace();
                Bukkit.getServer().shutdown();
            }
        });
    }

    @Override
    public void onDisable() {
        // TODO delete worlds
    }

    public Map<UUID, SkyWarsGame> getGames() {
        return games;
    }

    public SkyWarsGame findGame(Player player) {
        return games.values().stream()
                .filter(game -> game.getPlayers().contains(player))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Player is not in a game!"));
    }
}
