package net.aerh.skywars.game.loottable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class LootTable<T> {

    public static final int GUARANTEED_WEIGHT = -1;

    private final List<Item> table;

    /**
     * Represents a loot table.
     */
    public LootTable() {
        table = new ArrayList<>();
    }

    /**
     * Adds an item to the {@link LootTable}.
     *
     * @param item   the item
     * @param weight the weight of the item
     */
    public void addItemWithWeight(T item, int weight) {
        table.add(new Item(item, weight));
    }

    /**
     * Gets a random item from the {@link LootTable}.
     *
     * @return a random item from the {@link LootTable}
     */
    public Item getRandomItem() {
        int totalWeight = table.stream().mapToInt(Item::getWeight).sum();

        if (totalWeight <= 0) {
            return null;
        }

        Item guaranteedItem = table.stream()
            .filter(item -> item.getWeight() == GUARANTEED_WEIGHT)
            .findAny()
            .orElse(null);

        if (guaranteedItem != null) {
            return guaranteedItem;
        }

        AtomicLong randomWeight = new AtomicLong(ThreadLocalRandom.current().nextInt(totalWeight));
        Item selectedItem = table.stream()
            .filter(item -> item.getWeight() != GUARANTEED_WEIGHT)
            .reduce((accumulator, currentItem) -> {
                if (randomWeight.get() >= 0) {
                    randomWeight.addAndGet(-currentItem.getWeight());
                    return currentItem;
                } else {
                    return accumulator;
                }
            })
            .orElse(null);

        return selectedItem;
    }


    /**
     * Checks if the {@link LootTable} is empty.
     *
     * @return {@code true} if the {@link LootTable} is empty, otherwise {@code false}
     */
    public boolean isEmpty() {
        return table.isEmpty();
    }

    public class Item {
        private final T object;
        private final int weight;

        /**
         * Represents an item in a {@link LootTable}.
         *
         * @param object the object
         * @param weight the weight
         */
        public Item(T object, int weight) {
            this.object = object;
            this.weight = weight;
        }

        /**
         * Gets the object of this {@link Item}.
         *
         * @return the object
         */
        public T getObject() {
            return object;
        }

        /**
         * Gets the weight of this {@link Item}.
         *
         * @return the weight
         */
        public int getWeight() {
            return weight;
        }
    }
}

