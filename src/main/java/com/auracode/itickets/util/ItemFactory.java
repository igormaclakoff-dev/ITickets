package com.auracode.itickets.util;

import com.auracode.itickets.config.GuiItemConfig;
import com.auracode.itickets.config.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class ItemFactory {
    private final MessageService messages;
    private final Logger logger;

    public ItemFactory(MessageService messages) {
        this(messages, Logger.getLogger(ItemFactory.class.getName()));
    }

    public ItemFactory(MessageService messages, Logger logger) {
        this.messages = messages;
        this.logger = logger;
    }

    public ItemStack build(GuiItemConfig item, Map<String, String> placeholders) {
        return build(item.material(), item.name(), item.lore(), placeholders, item.amount(), item.glow(),
                item.customModelData());
    }

    public ItemStack build(String materialName, String name, List<String> lore, Map<String, String> placeholders) {
        return build(materialName, name, lore, placeholders, 1, false, null);
    }

    public ItemStack build(String materialName, String name, List<String> lore, Map<String, String> placeholders,
            int amount, boolean glow, Integer customModelData) {
        Material material = parseMaterial(materialName, Material.PAPER);
        ItemStack item = new ItemStack(material);
        item.setAmount(clampAmount(amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            logger.warning("Материал " + material + " не поддерживает ItemMeta. Используется PAPER.");
            item = new ItemStack(Material.PAPER, clampAmount(amount));
            meta = item.getItemMeta();
        }
        meta.displayName(render(name, placeholders));
        ArrayList<Component> renderedLore = new ArrayList<>();
        for (String rawLine : lore) {
            for (String line : splitRenderedLine(rawLine)) {
                renderedLore.add(render(line, placeholders));
            }
        }
        meta.lore(renderedLore);
        if (glow) {
            applyGlow(meta);
        }
        applyCustomModelData(meta, customModelData, logger);
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    public Material parseMaterial(String materialName, Material fallback) {
        if (materialName == null || materialName.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(materialName.trim());
        if (material == null || material.isAir()) {
            logger.warning("Неверный GUI material '" + materialName + "'. Используется " + fallback + ".");
            return fallback;
        }
        return material;
    }

    private Component render(String raw, Map<String, String> placeholders) {
        return disableItalicDeep(
                MiniMessage.miniMessage().deserialize(applyPlaceholders(raw, placeholders)));
    }

    private Component disableItalicDeep(Component component) {
        Component result = component.decoration(TextDecoration.ITALIC, false);
        List<Component> children = new ArrayList<>();
        for (Component child : result.children()) {
            children.add(disableItalicDeep(child));
        }
        return result.children(children);
    }

    private String applyPlaceholders(String raw, Map<String, String> placeholders) {
        String result = raw == null ? "" : raw;
        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String value = entry.getValue() == null ? "" : entry.getValue();
                result = result.replace("%" + entry.getKey() + "%", value);
            }
        }
        return result;
    }

    private List<String> splitRenderedLine(String line) {
        return Arrays.asList((line == null ? "" : line).split("\\R", -1));
    }

    private int clampAmount(int amount) {
        if (amount < 1 || amount > 64) {
            logger.warning("GUI amount должен быть от 1 до 64. Получено: " + amount + ".");
            return Math.max(1, Math.min(64, amount));
        }
        return amount;
    }

    private void applyGlow(ItemMeta meta) {
        Enchantment glowEnchant = glowEnchantment();
        if (glowEnchant == null) {
            return;
        }
        meta.addEnchant(glowEnchant, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    private void applyCustomModelData(ItemMeta meta, Integer customModelData, Logger logger) {
        if (customModelData == null) {
            return;
        }
        if (customModelData < 0) {
            logger.warning("custom-model-data не может быть меньше 0: " + customModelData);
            return;
        }
        meta.setCustomModelData(customModelData);
    }

    private Enchantment glowEnchantment() {
        for (String name : new String[] { "DURABILITY", "UNBREAKING" }) {
            Enchantment enchantment = Enchantment.getByName(name);
            if (enchantment != null) {
                return enchantment;
            }
            try {
                Object value = Enchantment.class.getField(name).get(null);
                if (value instanceof Enchantment reflected) {
                    return reflected;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        Enchantment[] values = Enchantment.values();
        return values.length == 0 ? null : values[0];
    }
}
