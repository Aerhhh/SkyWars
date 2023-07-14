package net.aerh.skywars.game;

public class GameSettings {

    private boolean blockBreak = true;
    private boolean blockPlace = true;
    private boolean hunger = true;
    private boolean damage = true;
    private boolean dropItem = true;
    private boolean pickupItem = true;
    private boolean interact = true;
    private boolean chat = true;

    public boolean isBlockBreak() {
        return blockBreak;
    }

    public void setBlockBreak(boolean blockBreak) {
        this.blockBreak = blockBreak;
    }

    public boolean isBlockPlace() {
        return blockPlace;
    }

    public void setBlockPlace(boolean blockPlace) {
        this.blockPlace = blockPlace;
    }

    public boolean isHunger() {
        return hunger;
    }

    public void setHunger(boolean hunger) {
        this.hunger = hunger;
    }

    public boolean isDamage() {
        return damage;
    }

    public void setDamage(boolean damage) {
        this.damage = damage;
    }

    public boolean isDropItem() {
        return dropItem;
    }

    public void setDropItem(boolean dropItem) {
        this.dropItem = dropItem;
    }

    public boolean isPickupItem() {
        return pickupItem;
    }

    public void setPickupItem(boolean pickupItem) {
        this.pickupItem = pickupItem;
    }

    public boolean isInteract() {
        return interact;
    }

    public void setInteract(boolean interact) {
        this.interact = interact;
    }

    public boolean isChat() {
        return chat;
    }

    public void setChat(boolean chat) {
        this.chat = chat;
    }
}
