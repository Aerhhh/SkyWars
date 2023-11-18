package net.aerh.skywars.game;

public class GameSettings {

    private boolean blockBreak = false;
    private boolean blockPlace = false;
    private boolean hunger = false;
    private boolean damage = false;
    private boolean dropItem = false;
    private boolean pickupItem = false;
    private boolean interact = true;
    private boolean chat = true;

    /**
     * Checks if players can break blocks.
     *
     * @return {@code true} if players can break blocks, otherwise {@code false}
     */
    public boolean canBreakBlocks() {
        return blockBreak;
    }

    /**
     * Sets whether players can break blocks.
     *
     * @param blockBreak {@code true} if players should be able to break blocks, otherwise {@code false}
     */
    public void allowBlockBreaking(boolean blockBreak) {
        this.blockBreak = blockBreak;
    }

    /**
     * Checks if players can place blocks.
     *
     * @return {@code true} if players can place blocks, otherwise {@code false}
     */
    public boolean canPlaceBlocks() {
        return blockPlace;
    }

    /**
     * Sets whether players can place blocks.
     *
     * @param blockPlace {@code true} if players should be able to place blocks, otherwise {@code false}
     */
    public void allowBlockPlacing(boolean blockPlace) {
        this.blockPlace = blockPlace;
    }

    /**
     * Checks if players can lose hunger.
     *
     * @return {@code true} if players can get hungry, otherwise {@code false}
     */
    public boolean isHungerEnabled() {
        return hunger;
    }

    /**
     * Sets whether players can lose hunger.
     *
     * @param hunger {@code true} if players should be able to get hungry, otherwise {@code false}
     */
    public void setHunger(boolean hunger) {
        this.hunger = hunger;
    }

    /**
     * Checks if players can take damage.
     *
     * @return {@code true} if players can take damage, otherwise {@code false}
     */
    public boolean isDamageEnabled() {
        return damage;
    }

    /**
     * Sets whether players can take damage.
     *
     * @param damage {@code true} if players should be able to take damage, otherwise {@code false}
     */
    public void allowDamage(boolean damage) {
        this.damage = damage;
    }

    /**
     * Checks if players can drop items.
     *
     * @return {@code true} if players can drop items, otherwise {@code false}
     */
    public boolean canDropItems() {
        return dropItem;
    }

    /**
     * Sets whether players can drop items.
     *
     * @param dropItem {@code true} if players should be able to drop items, otherwise {@code false}
     */
    public void allowItemDrops(boolean dropItem) {
        this.dropItem = dropItem;
    }

    /**
     * Checks if players can pick up items.
     *
     * @return {@code true} if players can pick up items, otherwise {@code false}
     */
    public boolean canPickupItems() {
        return pickupItem;
    }

    /**
     * Sets whether players can pick up items.
     *
     * @param pickupItem {@code true} if players should be able to pick up items, otherwise {@code false}
     */
    public void allowItemPickup(boolean pickupItem) {
        this.pickupItem = pickupItem;
    }

    /**
     * Checks if interacting is enabled.
     *
     * @return {@code true} if interacting is enabled, otherwise {@code false}
     */
    public boolean isInteractingEnabled() {
        return interact;
    }

    /**
     * Sets whether interacting is enabled.
     *
     * @param interact {@code true} if interacting should be enabled, otherwise {@code false}
     */
    public void setInteractable(boolean interact) {
        this.interact = interact;
    }

    /**
     * Checks if the chat is enabled.
     *
     * @return {@code true} if the chat is enabled, otherwise {@code false}
     */
    public boolean isChatEnabled() {
        return chat;
    }

    /**
     * Sets whether the chat is enabled.
     *
     * @param chat {@code true} if the chat should be enabled, otherwise {@code false}
     */
    public void setChatEnabled(boolean chat) {
        this.chat = chat;
    }
}
