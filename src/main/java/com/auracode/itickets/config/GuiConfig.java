package com.auracode.itickets.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class GuiConfig {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    public GuiConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "gui.yml");
        if (!file.exists()) {
            plugin.saveResource("gui.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public String title(String menu, String fallback) {
        return config.getString("menus." + menu + ".title", fallback);
    }

    public int size(String menu, int fallback) {
        int size = config.getInt("menus." + menu + ".size", fallback);
        if (size < 9 || size > 54 || size % 9 != 0) {
            return fallback;
        }
        return size;
    }

    public GuiItemConfig item(String path, int slot, String material, String name, List<String> lore) {
        String base = "items." + path + ".";
        Integer customModelData = config.isSet(base + "custom-model-data")
                ? config.getInt(base + "custom-model-data")
                : null;
        return new GuiItemConfig(config.getInt(base + "slot", slot), config.getString(base + "material", material),
                config.getString(base + "name", name),
                config.getStringList(base + "lore").isEmpty() ? lore : config.getStringList(base + "lore"),
                config.getInt(base + "amount", 1), config.getBoolean(base + "glow", false), customModelData);
    }

    public List<CloseReasonConfig> closeReasons(List<CloseReasonConfig> fallback) {
        ConfigurationSection section = config.getConfigurationSection("items.close-reasons");
        if (section == null) {
            return fallback;
        }
        Map<String, CloseReasonConfig> fallbackByKey = fallback.stream()
                .collect(Collectors.toMap(CloseReasonConfig::key, Function.identity()));
        ArrayList<CloseReasonConfig> result = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            CloseReasonConfig defaults = fallbackByKey.get(key);
            GuiItemConfig defaultItem = defaults == null
                    ? new GuiItemConfig(10, "PAPER", "<white>" + key + "</white>", List.of())
                    : defaults.item();
            GuiItemConfig item = item("close-reasons." + key, defaultItem.slot(), defaultItem.material(),
                    defaultItem.name(), defaultItem.lore());
            String base = "items.close-reasons." + key + ".";
            boolean custom = config.getBoolean(base + "custom", defaults != null && defaults.custom());
            String reason = config.getString(base + "reason", defaults == null ? "" : defaults.reason());
            if (!custom && (reason == null || reason.isBlank())) {
                plugin.getLogger().warning("Причина закрытия '" + key + "' пропущена: не указан reason.");
                continue;
            }
            result.add(new CloseReasonConfig(key, item, reason == null ? "" : reason, custom));
        }
        if (result.isEmpty()) {
            plugin.getLogger().warning("В gui.yml не найдено рабочих причин закрытия. Используются встроенные значения.");
            return fallback;
        }
        return result.stream().sorted(Comparator.comparingInt(reason -> reason.item().slot())).toList();
    }

    public List<Integer> ticketSlots() {
        List<Integer> slots = config.getIntegerList("ticket-list-slots");
        if (!slots.isEmpty()) {
            return slots;
        }
        return List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38,
                39, 40, 41, 42, 43);
    }
}
