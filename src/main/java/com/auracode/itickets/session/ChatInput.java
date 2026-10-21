package com.auracode.itickets.session;

public record ChatInput(ChatInputType type, int ticketId, String categoryId, int priority, long startedAt) {
}
