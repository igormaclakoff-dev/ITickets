package com.auracode.itickets.listener;

import com.auracode.itickets.gui.GuiService;
import com.auracode.itickets.gui.MenuHolder;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public final class GuiListener implements Listener {
    private final GuiService guiService;

    public GuiListener(GuiService guiService) {
        this.guiService = guiService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder inventoryHolder = event.getInventory().getHolder();
        if (!(inventoryHolder instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        HumanEntity humanEntity = event.getWhoClicked();
        if (humanEntity instanceof Player player) {
            guiService.playClick(player);
        }
        holder.handle(event);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }
}
