package com.auracode.itickets.session;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatInputService {
    private final Map<UUID, ChatInput> inputs = new ConcurrentHashMap<>();

    public void awaitCreate(Player player, String categoryId, int priority) {
        inputs.put(player.getUniqueId(),
                new ChatInput(ChatInputType.CREATE, 0, categoryId, priority, System.currentTimeMillis()));
    }

    public void awaitComment(Player player, int ticketId) {
        inputs.put(player.getUniqueId(),
                new ChatInput(ChatInputType.COMMENT, ticketId, null, 0, System.currentTimeMillis()));
    }

    public void awaitCloseReason(Player player, int ticketId) {
        inputs.put(player.getUniqueId(),
                new ChatInput(ChatInputType.CLOSE_REASON, ticketId, null, 0, System.currentTimeMillis()));
    }

    public void awaitNote(Player player, int ticketId) {
        inputs.put(player.getUniqueId(),
                new ChatInput(ChatInputType.NOTE, ticketId, null, 0, System.currentTimeMillis()));
    }

    public Optional<ChatInput> consume(Player player) {
        return Optional.ofNullable(inputs.remove(player.getUniqueId()));
    }

    public boolean hasInput(Player player) {
        return inputs.containsKey(player.getUniqueId());
    }

    public boolean cancel(Player player) {
        return inputs.remove(player.getUniqueId()) != null;
    }
}
