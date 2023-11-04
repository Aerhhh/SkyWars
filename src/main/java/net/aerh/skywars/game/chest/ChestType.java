package net.aerh.skywars.game.chest;

public enum ChestType {
    ISLAND(4),
    MIDDLE(6);

    private final int maxRefillItems;

    ChestType(int maxRefillItems) {
        this.maxRefillItems = maxRefillItems;
    }

    /**
     * Gets the maximum amount of items that can be refilled in the chest.
     *
     * @return the maximum amount of items that can be refilled in the chest
     */
    public int getMaxRefillItems() {
        return maxRefillItems;
    }
}
