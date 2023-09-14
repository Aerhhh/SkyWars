package net.aerh.skywars.util;

import net.aerh.skywars.SkyWarsPlugin;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class WorldSignParser {

    private final SkyWarsPlugin plugin;
    private final World world;
    private final List<ParsedSign> signs;

    /**
     * Creates a new world sign parser.
     *
     * @param plugin               The {@link SkyWarsPlugin plugin} instance.
     * @param world                The {@link World world} to parse signs in.
     * @param removeSignsFromWorld If the signs should be removed from the world.
     */
    public WorldSignParser(@NotNull SkyWarsPlugin plugin, @NotNull World world, boolean removeSignsFromWorld) {
        Objects.requireNonNull(plugin, "plugin cannot be null!");
        Objects.requireNonNull(world, "world cannot be null!");

        this.plugin = plugin;
        this.world = world;
        this.signs = new ArrayList<>();

        parseSigns(removeSignsFromWorld);
    }

    /**
     * Parses all signs in the world. If {@code removeFromWorld} is true, the signs will be removed from the world.
     *
     * @param removeFromWorld If the signs should be removed from the world.
     */
    public void parseSigns(boolean removeFromWorld) {
        signs.clear();

        for (Chunk chunk : world.getLoadedChunks()) {
            for (BlockState tileEntity : chunk.getTileEntities()) {
                if (!(tileEntity instanceof Sign)) {
                    continue;
                }

                Sign sign = (Sign) tileEntity;
                ParsedSign parsedSign = parse(sign);

                if (parsedSign != null) {
                    signs.add(parsedSign);
                    plugin.getLogger().info("Found sign: " + parsedSign.getCommand() + " with options: " + parsedSign.getOptions() + " at " + Utils.parseLocationToString(sign.getLocation()));

                    if (removeFromWorld) {
                        world.getBlockAt(sign.getLocation()).setType(Material.AIR);
                    }
                }
            }
        }
    }

    /**
     * Parses a sign. If the sign is not a valid sign, null is returned. The command must be surrounded by square brackets.
     * The first line of the sign is the command, and the remaining lines are options for the command.
     *
     * @param sign The sign to parse.
     * @return The parsed sign. Can be null.
     */
    @Nullable
    private ParsedSign parse(Sign sign) {
        String command = null;
        List<String> options = new ArrayList<>();

        org.bukkit.block.data.type.Sign signData = (org.bukkit.block.data.type.Sign) sign.getBlockData();

        for (String line : sign.getSide(Side.FRONT).getLines()) {
            if (line.startsWith("[") && line.endsWith("]")) {
                command = line.substring(1, line.length() - 1);
            } else {
                options.add(line);
            }
        }

        if (command != null) {
            return new ParsedSign(command, options, sign.getLocation(), signData.getRotation());
        }

        return null;
    }

    /**
     * Gets all parsed signs.
     *
     * @return All parsed signs. Can be empty.
     */
    public List<ParsedSign> getParsedSigns() {
        return signs;
    }

    /**
     * Gets all parsed signs with the specified command.
     *
     * @param command The command to search for.
     * @return All parsed signs with the specified command. Can be empty.
     */
    public List<ParsedSign> getParsedSigns(String command) {
        return signs.stream().filter(parsedSign -> parsedSign.getCommand().equalsIgnoreCase(command)).collect(Collectors.toList());
    }

    /**
     * Represents a parsed sign.
     */
    public static class ParsedSign {
        private final String command;
        private final List<String> options;
        private final Location location;
        private final BlockFace rotation;

        /**
         * Creates a new parsed sign.
         *
         * @param command  The first line of the sign, representing a "command" to be executed. For example, "join" or "spawn".
         * @param options  The remaining lines of the sign, representing options for the command. For example, "world" or "123". Can be empty.
         * @param location The {@link Location location} of the sign.
         * @param rotation The {@link BlockFace rotation} of the sign.
         */
        public ParsedSign(String command, List<String> options, Location location, BlockFace rotation) {
            this.command = command;
            this.options = options;
            this.location = location;
            this.rotation = rotation;
        }

        /**
         * Gets the command of the sign.
         *
         * @return The command of the sign.
         */
        public String getCommand() {
            return command;
        }

        /**
         * Gets the options of the sign.
         *
         * @return The options of the sign as a {@link List}. Can be empty.
         */
        public List<String> getOptions() {
            return options;
        }

        /**
         * Gets the location of the sign.
         *
         * @return The location of the sign.
         */
        public Location getLocation() {
            return location;
        }

        /**
         * Gets the rotation of the sign.
         *
         * @return The rotation of the sign.
         */
        public BlockFace getRotation() {
            return rotation;
        }
    }
}
