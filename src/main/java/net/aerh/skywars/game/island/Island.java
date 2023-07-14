package net.aerh.skywars.game.island;

import net.aerh.skywars.player.SkyWarsPlayer;
import org.bukkit.Location;

import javax.annotation.Nullable;

public class Island {

    private final Location spawnLocation;
    private SkyWarsPlayer assignedPlayer;

    public Island(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public void assignPlayer(SkyWarsPlayer player) {
        this.assignedPlayer = player;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    @Nullable
    public SkyWarsPlayer getAssignedPlayer() {
        return assignedPlayer;
    }

    public void setAssignedPlayer(@Nullable SkyWarsPlayer assignedPlayer) {
        this.assignedPlayer = assignedPlayer;
    }
}
