package net.aerh.skywars.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Utils {

    public static final String SEPARATOR = ChatColor.GREEN + ChatColor.STRIKETHROUGH.toString() + repeat(' ', 72);
    public static final DecimalFormat TWO_DECIMAL_PLACES_FORMAT = new DecimalFormat("#.##");

    private Utils() {
        throw new UnsupportedOperationException("Cannot instantiate utility class.");
    }

    /**
     * Parse a {@link Location} and return a string.
     *
     * @param location the {@link Location} to parse
     * @return the parsed {@link Location} string
     */
    public static String parseLocationToString(Location location) {
        String output = "";

        if (location.getWorld() != null) {
            output += location.getWorld().getName() + ", ";
        }

        output += TWO_DECIMAL_PLACES_FORMAT.format(location.getX()) + ", " + TWO_DECIMAL_PLACES_FORMAT.format(location.getY()) + ", " + TWO_DECIMAL_PLACES_FORMAT.format(location.getZ());

        return output;
    }

    /**
     * Check if two locations match.
     *
     * @param location1 the first location
     * @param location2 the second location
     * @return {@code true} if the locations match, otherwise {@code false}
     */
    public static boolean locationsMatch(@NotNull Location location1, @NotNull Location location2) {
        Objects.requireNonNull(location1, "location1 cannot be null!");
        Objects.requireNonNull(location2, "location2 cannot be null!");

        if (!location1.isWorldLoaded() || !location2.isWorldLoaded()) {
            throw new IllegalArgumentException("Both locations must be in a loaded world!");
        }

        return location1.equals(location2);
    }

    /**
     * Repeat a character a given amount of times.
     *
     * @param character the character to repeat
     * @param count     the amount of times to repeat the character
     * @return the result string
     */
    private static String repeat(char character, int count) {
        return IntStream.range(0, count).mapToObj(i -> String.valueOf(character)).collect(Collectors.joining());
    }

    /**
     * Delete a given folder and all its contents.
     *
     * @param directoryPath the {@link Path} to the directory
     */
    public static void deleteFolder(Path directoryPath) {
        try (Stream<Path> walk = Files.walk(directoryPath)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        Bukkit.getLogger().warning("Could not delete " + path + ": " + e.getMessage());
                    }
                });
        } catch (IOException exception) {
            Bukkit.getLogger().warning("Could not delete directory " + directoryPath + ": " + exception.getMessage());
        }
    }

    /**
     * Parses a {@link Location} from a {@link JsonObject}.
     *
     * @param config the {@link JsonObject} to parse from
     * @param field  the field to parse
     * @return the parsed {@link Location}
     */
    public static Location parseConfigLocationObject(JsonObject config, World world, String field) {
        JsonObject locationsObject = config.getAsJsonObject("locations");
        JsonObject desiredLocation = locationsObject.getAsJsonObject(field);

        if (!desiredLocation.has("x") || !desiredLocation.has("y") || !desiredLocation.has("z")) {
            throw new IllegalStateException("Location is missing coordinates! " + desiredLocation);
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

    public static List<JsonObject> parseConfigLocationArray(JsonObject config, String field) {
        JsonObject locationsObject = config.getAsJsonObject("locations");
        List<JsonObject> locations = new ArrayList<>();

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
