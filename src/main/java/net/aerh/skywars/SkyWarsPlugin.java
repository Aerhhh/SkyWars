package net.aerh.skywars;

import net.aerh.skywars.command.StartGameCommand;
import net.aerh.skywars.game.GameState;
import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.listener.GameListener;
import net.aerh.skywars.listener.PlayerSessionListener;
import net.aerh.skywars.map.MapLoader;
import net.aerh.skywars.player.SkyWarsPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public final class SkyWarsPlugin extends JavaPlugin {

    private static final int DESIRED_GAME_COUNT = 5;

    private final Map<UUID, SkyWarsGame> games = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(this), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);

        getCommand("start").setExecutor(new StartGameCommand(this));

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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            games.forEach((uuid, game) -> {
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
        // TODO delete worlds
    }

    public Map<UUID, SkyWarsGame> getGames() {
        return games;
    }

    @Nullable
    public SkyWarsGame findNextFreeGame() {
        return games.values().stream()
            .filter(game -> game.getState() == GameState.PRE_GAME)
            .filter(game -> game.getPlayers().size() < SkyWarsGame.MAX_PLAYER_COUNT)
            .findFirst()
            .orElse(null);
    }

    @Nullable
    public SkyWarsGame findGame(Player player) {
        return games.values().stream()
            .filter(game -> game.getPlayers().stream().map(SkyWarsPlayer::getUuid).anyMatch(uuid -> uuid.equals(player.getUniqueId())))
            .findFirst()
            .orElse(null);
    }
}
