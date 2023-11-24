package net.aerh.skywars.game.island;

import net.aerh.skywars.player.SkyWarsPlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class Island {

    private final Location spawnLocation;
    private SkyWarsPlayer assignedPlayer;

    /**
     * Represents an island in the game.
     *
     * @param spawnLocation the {@link Location} where the player will spawn
     */
    public Island(@NotNull Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    /**
     * Assigns a player to this island. Only one player can be assigned to an island.
     *
     * @param player the {@link SkyWarsPlayer} to assign to this island
     */
    public void assignPlayer(SkyWarsPlayer player) {
        this.assignedPlayer = player;
    }

    /**
     * Unassigns the {@link SkyWarsPlayer} from this island.
     */
    public void removePlayer() {
        this.assignedPlayer = null;
    }

    /**
     * Gets the spawn location of this island.
     *
     * @return the spawn location
     */
    @NotNull
    public Location getSpawnLocation() {
        return spawnLocation;
    }

    /**
     * Spawns a glass cage around the island spawn location.
     */
    public void spawnCage() {
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 3; y++) {
                for (int z = -2; z <= 2; z++) {
                    Material type;

                    if (x == -2 || x == 2 || y == -1 || y == 3 || z == -2 || z == 2) {
                        type = Material.GLASS;
                    } else {
                        type = Material.AIR;
                    }

                    spawnLocation.clone().add(x, y, z).getBlock().setType(type);
                }
            }
        }
    }

    /**
     * Removes the glass cage around the island spawn location.
     */
    public void removeCage() {
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 3; y++) {
                for (int z = -2; z <= 2; z++) {
                    getSpawnLocation().clone().add(x, y, z).getBlock().setType(Material.AIR);
                }
            }
        }
    }

    /**
     * Gets the player assigned to this island. Can be null.
     *
     * @return the {@link SkyWarsPlayer player} assigned to this island
     */
    public Optional<SkyWarsPlayer> getAssignedPlayer() {
        return Optional.ofNullable(assignedPlayer);
    }
}
