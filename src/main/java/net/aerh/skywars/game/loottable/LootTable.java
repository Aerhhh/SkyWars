package net.aerh.skywars.game.loottable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class LootTable<T> {

    public static final int GUARANTEED_WEIGHT = -1;

    private final List<Item> table;

    public LootTable() {
        table = new ArrayList<>();
    }

    public void addItemWithWeight(T item, int weight) {
        table.add(new Item(item, weight));
    }

    public Item getRandomItem() {
        int totalWeight = table.stream().mapToInt(Item::getWeight).sum();

        if (totalWeight <= 0) {
            return null;
        }

        Item guaranteedItem = table.stream()
            .filter(item -> item.getWeight() == GUARANTEED_WEIGHT)
            .findFirst()
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


    public boolean isEmpty() {
        return table.isEmpty();
    }

    public class Item {
        private final T object;
        private final int weight;

        public Item(T object, int weight) {
            this.object = object;
            this.weight = weight;
        }

        public T getObject() {
            return object;
        }

        public int getWeight() {
            return weight;
        }
    }
}

