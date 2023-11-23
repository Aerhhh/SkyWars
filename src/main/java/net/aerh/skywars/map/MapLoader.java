package net.aerh.skywars.map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class MapLoader {

    private static final Gson GSON = new Gson();

    /**
     * Loads a random map from the given file path and creates a world with the given name.
     *
     * @param filePath  the path to the map directory
     * @param worldName the name of the world to create
     * @return the {@link SkyWarsGame game} instance
     */
    public static SkyWarsGame loadRandomMap(String filePath, String worldName) {
        File[] mapDirs = new File(filePath).listFiles(File::isDirectory);

        if (mapDirs == null || mapDirs.length == 0) {
            throw new IllegalStateException("No map directory found");
        }

        SkyWarsPlugin.getInstance().getLogger().info("Found " + mapDirs.length + " map directories!");

        File mapDir = mapDirs[ThreadLocalRandom.current().nextInt(mapDirs.length)];
        File configFile = new File(mapDir, "config.json");

        if (!configFile.exists()) {
            throw new IllegalStateException("No config.json found in map directory '" + mapDir.getName() + "' (" + mapDir.getAbsolutePath() + ")");
        }

        JsonObject config;
        try (FileReader reader = new FileReader(configFile)) {
            SkyWarsPlugin.getInstance().getLogger().info("Loading map " + mapDir.getName() + "... (" + mapDir.getAbsolutePath() + ")");

            File worldDir = new File(SkyWarsPlugin.getInstance().getServer().getWorldContainer(), worldName);

            SkyWarsPlugin.getInstance().getLogger().info("Copying map directory " + mapDir.getName() + " to " + worldDir.getName() + "...");
            Utils.copyDirectory(mapDir, worldDir);

            config = GSON.fromJson(reader, JsonObject.class);
            SkyWarsPlugin.getInstance().getLogger().info("Loaded config.json for map " + config.get("name").getAsString());
        } catch (IOException | JsonSyntaxException e) {
            throw new IllegalStateException("Could not read config.json", e);
        }

        SkyWarsPlugin.getInstance().getLogger().info("Creating world " + worldName + "...");
        World world = Bukkit.createWorld(new WorldCreator(worldName));

        if (world == null) {
            throw new IllegalStateException("Could not create world " + worldName);
        }

        world.setAutoSave(false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);

        return new SkyWarsGame(world, config);
    }
}
