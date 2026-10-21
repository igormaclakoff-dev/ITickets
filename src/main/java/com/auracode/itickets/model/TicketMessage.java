package com.auracode.itickets.model;

import java.util.UUID;

public record TicketMessage(UUID authorUuid, String authorName, boolean staff, long timestamp, String text,
        boolean internal) {

    public TicketMessage(UUID authorUuid, String authorName, boolean staff, long timestamp, String text) {
        this(authorUuid, authorName, staff, timestamp, text, false);
    }
}
