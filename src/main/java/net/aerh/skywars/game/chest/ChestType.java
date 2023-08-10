package net.aerh.skywars.game.chest;

public enum ChestType {
    ISLAND(4),
    MIDDLE(6);

    private final int maxRefillItems;

    ChestType(int maxRefillItems) {
        this.maxRefillItems = maxRefillItems;
    }

    public int getMaxRefillItems() {
        return maxRefillItems;
    }

    public static ChestType valueOfOrElse(String name, ChestType type) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException exception) {
            return type;
        }
    }
}
