package net.aerh.skywars.game.island;

import net.aerh.skywars.player.SkyWarsPlayer;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * Gets the spawn location of this island.
     *
     * @return the spawn location
     */
    @NotNull
    public Location getSpawnLocation() {
        return spawnLocation;
    }

    /**
     * Gets the player assigned to this island. Can be null.
     *
     * @return the {@link SkyWarsPlayer player} assigned to this island
     */
    @Nullable
    public SkyWarsPlayer getAssignedPlayer() {
        return assignedPlayer;
    }

    /**
     * Sets the player assigned to this island.
     *
     * @param assignedPlayer the {@link SkyWarsPlayer player} to assign to this island
     */
    public void setAssignedPlayer(@Nullable SkyWarsPlayer assignedPlayer) {
        this.assignedPlayer = assignedPlayer;
    }
}
