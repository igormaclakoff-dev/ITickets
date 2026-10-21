package com.auracode.itickets.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class MenuHolder implements InventoryHolder {
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();
    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void action(int slot, Consumer<InventoryClickEvent> action) {
        actions.put(slot, action);
    }

    public void handle(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> action = actions.get(event.getRawSlot());
        if (action != null) {
            action.accept(event);
        }
    }
}
