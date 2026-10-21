package com.auracode.itickets.config;

import java.util.List;

public record GuiItemConfig(int slot, String material, String name, List<String> lore, int amount, boolean glow,
        Integer customModelData) {
    public GuiItemConfig(int slot, String material, String name, List<String> lore) {
        this(slot, material, name, lore, 1, false, null);
    }

    public GuiItemConfig {
        lore = lore == null ? List.of() : List.copyOf(lore);
    }
}
