package net.aerh.skywars.game;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class Island {

    private final Location spawnLocation;
    private Player assignedPlayer;

    public Island(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public void assignPlayer(Player player) {
        this.assignedPlayer = player;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    @Nullable
    public Player getAssignedPlayer() {
        return assignedPlayer;
    }

    public void setAssignedPlayer(@Nullable Player assignedPlayer) {
        this.assignedPlayer = assignedPlayer;
    }
}
