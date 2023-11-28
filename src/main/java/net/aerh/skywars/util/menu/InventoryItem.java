package net.aerh.skywars.util.menu;

import org.bukkit.inventory.ItemStack;

public class InventoryItem {

    private ItemStack itemStack;
    private final ClickHandler clickHandler;

    public InventoryItem(ItemStack itemStack, ClickHandler clickHandler) {
        this.itemStack = itemStack;
        this.clickHandler = clickHandler;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public ClickHandler getClickHandler() {
        return clickHandler;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }
}
