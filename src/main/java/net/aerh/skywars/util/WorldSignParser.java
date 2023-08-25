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

    public WorldSignParser(@NotNull SkyWarsPlugin plugin, @NotNull World world, boolean removeSignsFromWorld) {
        Objects.requireNonNull(plugin, "plugin cannot be null!");
        Objects.requireNonNull(world, "world cannot be null!");

        this.plugin = plugin;
        this.world = world;
        this.signs = new ArrayList<>();

        parseSigns(removeSignsFromWorld);
    }

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

    public List<ParsedSign> getParsedSigns() {
        return signs;
    }

    public List<ParsedSign> getParsedSigns(String command) {
        return signs.stream().filter(parsedSign -> parsedSign.getCommand().equalsIgnoreCase(command)).collect(Collectors.toList());
    }

    public static class ParsedSign {
        private final String command;
        private final List<String> options;
        private final Location location;
        private final BlockFace rotation;

        public ParsedSign(String command, List<String> options, Location location, BlockFace rotation) {
            this.command = command;
            this.options = options;
            this.location = location;
            this.rotation = rotation;
        }

        public String getCommand() {
            return command;
        }

        public List<String> getOptions() {
            return options;
        }

        public Location getLocation() {
            return location;
        }

        public BlockFace getRotation() {
            return rotation;
        }
    }
}
