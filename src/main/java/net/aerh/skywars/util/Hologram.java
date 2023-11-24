package net.aerh.skywars.util;

import net.aerh.skywars.SkyWarsPlugin;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

public class Hologram {

    private ArmorStand armorStand;
    private Location location;
    private String text;

    /**
     * Represents a hologram.
     *
     * @param location the location of the hologram
     * @param text     the text of the hologram
     */
    public Hologram(Location location, String text) {
        this.location = location;
        this.text = text;
    }

    /**
     * Spawns the hologram.
     */
    public void spawn() {
        if (location.getWorld() == null) {
            SkyWarsPlugin.getInstance().getLogger().severe("Could not spawn hologram at " + location + " because the world is null");
            return;
        }

        armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        armorStand.setGravity(false);
        armorStand.setVisible(false);
        armorStand.setCustomName(text);
        armorStand.setCustomNameVisible(true);
        armorStand.setSmall(true);
        armorStand.setInvulnerable(true);
        armorStand.setMarker(true);
        armorStand.setCollidable(false);
    }

    /**
     * Updates the text of the hologram.
     *
     * @param newText the new text of the hologram
     */
    public void updateText(String newText) {
        text = newText;
        armorStand.setCustomName(text);
    }

    /**
     * Teleports the hologram to a new location.
     *
     * @param newLocation the new location of the hologram
     */
    public void teleport(Location newLocation) {
        location = newLocation;
        armorStand.teleport(newLocation);
    }

    /**
     * Removes the hologram.
     */
    public void remove() {
        armorStand.remove();
    }
}