package net.aerh.skywars.game;

public class GameSettings {

    private boolean blockBreak = true;
    private boolean blockPlace = true;
    private boolean hunger = true;
    private boolean damage = false;
    private boolean dropItem = true;
    private boolean pickupItem = true;
    private boolean interact = true;
    private boolean chat = true;

    public boolean canBreakBlocks() {
        return blockBreak;
    }

    public void allowBlockBreaking(boolean blockBreak) {
        this.blockBreak = blockBreak;
    }

    public boolean canPlaceBlocks() {
        return blockPlace;
    }

    public void allowBlockPlacing(boolean blockPlace) {
        this.blockPlace = blockPlace;
    }

    public boolean isHungerEnabled() {
        return hunger;
    }

    public void setHunger(boolean hunger) {
        this.hunger = hunger;
    }

    public boolean isDamageEnabled() {
        return damage;
    }

    public void allowDamage(boolean damage) {
        this.damage = damage;
    }

    public boolean canDropItems() {
        return dropItem;
    }

    public void allowItemDrops(boolean dropItem) {
        this.dropItem = dropItem;
    }

    public boolean canPickupItems() {
        return pickupItem;
    }

    public void setPickupItem(boolean pickupItem) {
        this.pickupItem = pickupItem;
    }

    public boolean isInteractingEnabled() {
        return interact;
    }

    public void setInteractable(boolean interact) {
        this.interact = interact;
    }

    public boolean isChatEnabled() {
        return chat;
    }

    public void setChatEnabled(boolean chat) {
        this.chat = chat;
    }
}
