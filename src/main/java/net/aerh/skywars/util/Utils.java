package net.aerh.skywars.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Utils {

    public static final int TICKS_PER_SECOND = 20;
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
     * Copies a directory to another directory.
     *
     * @param source the source directory
     * @param target the target directory
     * @throws IOException If the directory could not be copied
     */
    public static void copyDirectory(File source, File target) throws IOException {
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

    /**
     * Parses an {@link Enum} from a string.
     *
     * @param enumType the {@link Enum} type
     * @param string   the string to parse
     * @param <E>      the {@link Enum} type
     * @return the parsed {@link Enum}
     */
    public static <E extends Enum<E>> Optional<E> parseEnum(Class<E> enumType, String string) {
        try {
            return Optional.of(Enum.valueOf(enumType, string.toUpperCase()));
        } catch (IllegalArgumentException exception) {
            Bukkit.getLogger().severe("Failed to parse " + enumType.getSimpleName() + " " + string + "!");
            Bukkit.getLogger().severe("Valid values are: " + Arrays.toString(enumType.getEnumConstants()));
            return Optional.empty();
        }
    }

    /**
     * Formats a time in seconds to a string (mm:ss).
     *
     * @param remainingSeconds the time in seconds
     * @return the formatted time
     */
    public static String formatTime(int remainingSeconds) {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Formats the given time in milliseconds to a string in the format mm:ss.
     *
     * @param millis The time in milliseconds.
     * @return The formatted time string.
     */
    public static String formatTimeMillis(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes);

        return String.format("%02d:%02d", minutes, seconds);
    }
}
