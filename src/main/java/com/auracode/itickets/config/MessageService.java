package com.auracode.itickets.config;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public final class MessageService {
    private static final String MISSING_PREFIX = "<red>Сообщение не найдено: </red>";
    private final JavaPlugin plugin;
    private FileConfiguration config;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Collections.emptyMap());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(render(apply(raw(path), placeholders)));
    }

    public void sendRaw(CommandSender sender, String raw, Map<String, String> placeholders) {
        sender.sendMessage(render(apply(raw, placeholders)));
    }

    private net.kyori.adventure.text.Component render(String input) {
        return disableItalicDeep(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize(input == null ? "" : input));
    }

    private net.kyori.adventure.text.Component disableItalicDeep(
            net.kyori.adventure.text.Component component) {
        net.kyori.adventure.text.Component result = component
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
        java.util.List<net.kyori.adventure.text.Component> children = new java.util.ArrayList<>();
        for (net.kyori.adventure.text.Component child : result.children()) {
            children.add(disableItalicDeep(child));
        }
        return result.children(children);
    }

    public String text(String path, Map<String, String> placeholders) {
        return apply(raw(path), placeholders);
    }

    public void close() {
    }

    private String raw(String path) {
        return config == null ? MISSING_PREFIX + path : config.getString(path, MISSING_PREFIX + path);
    }

    private String apply(String raw, Map<String, String> placeholders) {
        String result = raw == null ? "" : raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }
}
