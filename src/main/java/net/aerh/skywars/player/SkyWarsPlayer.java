package net.aerh.skywars.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SkyWarsPlayer {

    private final UUID uuid;
    private Scoreboard scoreboard;
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

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public void setScoreboard(Scoreboard scoreboard) {
        this.scoreboard = scoreboard;

        if (getBukkitPlayer() != null) {
            getBukkitPlayer().setScoreboard(scoreboard);
        }
    }

    @Nullable
    public Player getBukkitPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    @Override
    public String toString() {
        return "SkyWarsPlayer{" +
            "uuid=" + uuid +
            '}';
    }
}
