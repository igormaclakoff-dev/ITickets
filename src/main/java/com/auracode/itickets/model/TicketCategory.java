package com.auracode.itickets.model;

import java.util.List;

public record TicketCategory(String id, String displayName, String material, int slot, String color,
        String permission, List<String> description, int amount, boolean glow, Integer customModelData) {
    public TicketCategory(String id, String displayName, String material, int slot, String color, String permission,
            List<String> description) {
        this(id, displayName, material, slot, color, permission, description, 1, false, null);
    }

    public TicketCategory {
        description = description == null ? List.of() : List.copyOf(description);
    }
}
