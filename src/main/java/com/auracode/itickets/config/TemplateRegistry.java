package com.auracode.itickets.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class TemplateRegistry {
    private final JavaPlugin plugin;
    private final List<TemplateEntry> templates = new ArrayList<>();

    public TemplateRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "templates.yml");
        if (!file.exists()) {
            plugin.saveResource("templates.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        templates.clear();
        ConfigurationSection section = config.getConfigurationSection("templates");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            String base = "templates." + key + ".";
            String name = config.getString(base + "name", key);
            String text = config.getString(base + "text", "");
            String material = config.getString(base + "material", "PAPER");
            String group = config.getString(base + "group", "Прочее");
            boolean adminOnly = config.getBoolean(base + "admin-only", false);
            int slot = config.getInt(base + "slot", templates.size());
            if (text.isBlank()) {
                continue;
            }
            templates.add(new TemplateEntry(key, name, text, material, group, adminOnly, slot));
        }
    }

    public List<TemplateEntry> all() {
        return List.copyOf(templates);
    }

    public boolean isEmpty() {
        return templates.isEmpty();
    }

    public record TemplateEntry(String id, String name, String text, String material, String group,
            boolean adminOnly, int slot) {
    }
}
