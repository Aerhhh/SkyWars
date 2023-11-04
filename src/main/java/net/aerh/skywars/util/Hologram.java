package net.aerh.skywars.util;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

public class Hologram {

    private ArmorStand armorStand;
    private Location location;
    private String text;

    public Hologram(Location location, String text) {
        this.location = location;
        this.text = text;
    }

    public void spawn() {
        armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        armorStand.setGravity(false);
        armorStand.setVisible(false);
        armorStand.setCustomName(text);
        armorStand.setCustomNameVisible(true);
        armorStand.setSmall(true);
        armorStand.setInvulnerable(true);
        armorStand.setMarker(true);
    }

    public void updateText(String newText) {
        text = newText;
        armorStand.setCustomName(text);
    }

    public void teleport(Location newLocation) {
        armorStand.teleport(newLocation);
    }

    public void remove() {
        armorStand.remove();
    }
}