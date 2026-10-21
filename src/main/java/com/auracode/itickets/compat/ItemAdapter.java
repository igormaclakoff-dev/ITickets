package com.auracode.itickets.compat;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemAdapter {
    public static void setGlow(ItemStack item, boolean glow) {
        if (!glow || item == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    public static ItemStack create(String material, int amount) {
        Material type = Material.matchMaterial(material);
        if (type == null || !type.isItem()) {
            return new ItemStack(Material.PAPER, Math.max(1, amount));
        }
        return new ItemStack(type, Math.max(1, amount));
    }
}
