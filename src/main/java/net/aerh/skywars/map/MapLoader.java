package net.aerh.skywars.map;

import com.google.gson.*;
import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.game.chest.ChestType;
import net.aerh.skywars.game.chest.RefillableChest;
import net.aerh.skywars.game.island.Island;
import net.aerh.skywars.util.Utils;
import org.bukkit.*;
import org.bukkit.block.BlockFace;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class MapLoader {

    private static final Gson GSON = new Gson();
    private static final BlockFace[] VALID_CHEST_ROTATIONS = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    private MapLoader() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    /**
     * Loads a random map from the given file path and creates a world with the given name.
     *
     * @param filePath  the path to the map directory
     * @param worldName the name of the world to create
     * @return the {@link SkyWarsGame game} instance
     * @throws IllegalStateException if no map directory is found, if the config.json is missing, or if the world could not be created
     */
    public static SkyWarsGame loadRandomMap(String filePath, String worldName) {
        File[] mapDirs = new File(filePath).listFiles(file -> file.isDirectory() && new File(file, "config.json").exists());

        if (mapDirs == null || mapDirs.length == 0) {
            throw new IllegalStateException("No map directory found");
        }

        SkyWarsPlugin.getInstance().getLogger().info("Found " + mapDirs.length + " valid map directories!");

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
        world.setTime(6_000L);

        SkyWarsGame skyWarsGame = new SkyWarsGame(world, config);
        parseIslands(skyWarsGame, config);
        parseChests(skyWarsGame, config);
        skyWarsGame.setPregameSpawn(MapLoader.parseConfigLocationObject(config, skyWarsGame.getWorld(), "pregame"));

        return skyWarsGame;
    }

    /**
     * Parses the islands from a {@link JsonArray}.
     *
     * @param config the {@link JsonObject} to parse from
     */
    private static void parseIslands(SkyWarsGame skyWarsGame, JsonObject config) {
        parseConfigLocationArray(config, "islands").forEach(island -> {
            try {
                double x = island.get("x").getAsDouble();
                double y = island.get("y").getAsDouble();
                double z = island.get("z").getAsDouble();
                Location location = new Location(skyWarsGame.getWorld(), x, y, z);
                skyWarsGame.getIslands().add(new Island(location));
                skyWarsGame.log(Level.INFO, "Registered island: " + island);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Failed to parse island: " + island);
            }
        });
    }

    /**
     * Parses the chests from a {@link JsonObject}.
     *
     * @param config the {@link JsonObject} to parse from
     */
    private static void parseChests(SkyWarsGame skyWarsGame, JsonObject config) {
        skyWarsGame.log(Level.INFO, "Parsing chest locations from map config...");

        JsonElement locations = config.get("locations");

        if (locations == null || !locations.isJsonObject() || !locations.getAsJsonObject().has("chests")
            || !locations.getAsJsonObject().get("chests").isJsonArray()) {
            skyWarsGame.log(Level.WARNING, "No chest locations found in map config!");
            return;
        }

        parseConfigLocationArray(config, "chests").forEach(chest -> {
            try {
                double x = chest.get("x").getAsDouble();
                double y = chest.get("y").getAsDouble();
                double z = chest.get("z").getAsDouble();
                Optional<BlockFace> rotation = Utils.parseEnum(BlockFace.class, chest.get("rotation").getAsString());
                Optional<ChestType> chestType = Utils.parseEnum(ChestType.class, chest.get("type").getAsString());

                if (rotation.isEmpty() || chestType.isEmpty()) {
                    return;
                }

                if (Arrays.stream(VALID_CHEST_ROTATIONS).noneMatch(face -> face.equals(rotation.get()))) {
                    throw new IllegalArgumentException("Invalid chest rotation: " + rotation.get() + " for chest: " + chest);
                }

                RefillableChest refillableChest = new RefillableChest(new Location(skyWarsGame.getWorld(), x, y, z), chestType.get());
                skyWarsGame.getRefillableChests().add(refillableChest);
                refillableChest.spawn(true, rotation.get());
                skyWarsGame.log(Level.INFO, "Registered refillable chest: " + chest);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Failed to parse chest: " + chest);
            }
        });
    }

    /**
     * Parses a {@link Location} from a {@link JsonObject}.
     *
     * @param config the {@link JsonObject} to parse from
     * @param field  the field to parse
     * @return the parsed {@link Location}
     */
    private static Location parseConfigLocationObject(JsonObject config, World world, String field) {
        JsonObject locationsObject = config.getAsJsonObject("locations");

        if (locationsObject == null) {
            throw new IllegalStateException("'locations' JSON object is missing in config.json!");
        }

        JsonObject desiredLocation = locationsObject.getAsJsonObject(field);

        if (desiredLocation == null || !desiredLocation.has("x") || !desiredLocation.has("y") || !desiredLocation.has("z")) {
            throw new IllegalStateException("Location is missing coordinates! " + (desiredLocation != null ? desiredLocation : ""));
        }

        Location location = new Location(world, desiredLocation.get("x").getAsDouble(), desiredLocation.get("y").getAsDouble(), desiredLocation.get("z").getAsDouble());

        if (desiredLocation.has("yaw")) {
            location.setYaw(desiredLocation.get("yaw").getAsFloat());
        }

        if (desiredLocation.has("pitch")) {
            location.setPitch(desiredLocation.get("pitch").getAsFloat());
        }

        return location;
    }

    private static List<JsonObject> parseConfigLocationArray(JsonObject config, String field) {
        JsonObject locationsObject = config.getAsJsonObject("locations");
        List<JsonObject> locations = new ArrayList<>();

        if (locationsObject == null) {
            throw new IllegalStateException("'locations' JSON object is missing in config.json!");
        }

        if (locationsObject.getAsJsonArray(field) == null) {
            throw new IllegalStateException("Array of '" + field + "' is missing in config.json!");
        }

        for (JsonElement locationElement : locationsObject.getAsJsonArray(field)) {
            JsonObject locationObject = locationElement.getAsJsonObject();

            if (!locationObject.has("x") || !locationObject.has("y") || !locationObject.has("z")) {
                throw new IllegalStateException("Location is missing coordinates! " + locationObject);
            }

            locations.add(locationObject);
        }

        return locations;
    }
}
