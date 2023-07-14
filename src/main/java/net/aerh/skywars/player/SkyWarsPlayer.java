package net.aerh.skywars.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.UUID;

public class SkyWarsPlayer {

    private final UUID uuid;
    private int kills;

    public SkyWarsPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public void addKill() {
        kills++;
    }

    public void removeKill() {
        kills--;
    }

    public void resetKills() {
        kills = 0;
    }

    @Nullable
    public Player getBukkitPlayer() {
        return Bukkit.getPlayer(uuid);
    }
}
