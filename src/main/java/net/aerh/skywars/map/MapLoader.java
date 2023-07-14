package net.aerh.skywars.map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.SkyWarsGame;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class MapLoader {

    private final SkyWarsPlugin plugin;
    private final File mapsDir;
    private final Gson gson;

    public MapLoader(SkyWarsPlugin plugin, String mapsDirPath) {
        this.plugin = plugin;
        this.mapsDir = new File(mapsDirPath);
        this.gson = new Gson();
    }

    public SkyWarsGame loadRandomMap(String worldName) throws IOException {
        File[] mapDirs = mapsDir.listFiles(File::isDirectory);

        if (mapDirs == null || mapDirs.length == 0) {
            throw new IllegalStateException("No map directory found");
        }

        plugin.getLogger().info("Found " + mapDirs.length + " map directories!");

        File mapDir = mapDirs[ThreadLocalRandom.current().nextInt(mapDirs.length)];
        File configFile = new File(mapDir, "config.json");
        if (!configFile.exists()) {
            throw new IllegalStateException("No config.json found in map directory '" + mapDir.getName() + "' (" + mapDir.getAbsolutePath() + ")");
        }

        plugin.getLogger().info("Loading map " + mapDir.getName() + "... (" + mapDir.getAbsolutePath() + ")");

        File worldDir = new File(plugin.getServer().getWorldContainer(), worldName);

        plugin.getLogger().info("Copying map directory " + mapDir.getName() + " to " + worldDir.getName() + "...");
        copyDirectory(mapDir, worldDir);

        JsonObject config;
        try (FileReader reader = new FileReader(configFile)) {
            config = gson.fromJson(reader, JsonObject.class);
            plugin.getLogger().info("Loaded config.json for map " + mapDir.getName());
        }

        plugin.getLogger().info("Creating world " + worldName + "...");
        World world = Bukkit.createWorld(new WorldCreator(worldName));

        return new SkyWarsGame(plugin, world, config);
    }

    private void copyDirectory(File source, File target) throws IOException {
        try (Stream<Path> paths = Files.walk(source.toPath())) {
            paths.forEach(sourcePath -> {
                Path targetPath = target.toPath().resolve(source.toPath().relativize(sourcePath));
                try {
                    Files.copy(sourcePath, targetPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
