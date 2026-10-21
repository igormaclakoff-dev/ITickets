package com.auracode.itickets.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class Ticket {
    private final int id;
    private final UUID ownerUuid;
    private String ownerName;
    private TicketStatus status;
    private String categoryId;
    private int priority;
    private final long createdAt;
    private long updatedAt;
    private TicketLocation location;
    private UUID assignedToUuid;
    private String assignedToName;
    private UUID closedByUuid;
    private String closedByName;
    private long closedAt;
    private String closeReason;
    private UUID deletedByUuid;
    private String deletedByName;
    private long deletedAt;
    private int rating;
    private long ratedAt;
    private final List<TicketMessage> messages;

    public Ticket(int id, UUID ownerUuid, String ownerName, TicketStatus status, String categoryId, int priority,
            long createdAt, long updatedAt, TicketLocation location, List<TicketMessage> messages) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.status = status;
        this.categoryId = categoryId;
        this.priority = priority;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.location = location;
        this.messages = new ArrayList<>(messages);
    }

    public int id() {
        return id;
    }

    public UUID ownerUuid() {
        return ownerUuid;
    }

    public String ownerName() {
        return ownerName;
    }

    public void ownerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public TicketStatus status() {
        return status;
    }

    public void status(TicketStatus status) {
        this.status = status;
        touch();
    }

    public String categoryId() {
        return categoryId;
    }

    public void categoryId(String categoryId) {
        this.categoryId = categoryId;
        touch();
    }

    public int priority() {
        return priority;
    }

    public void priority(int priority) {
        this.priority = priority;
        touch();
    }

    public long createdAt() {
        return createdAt;
    }

    public long updatedAt() {
        return updatedAt;
    }

    public void updatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public TicketLocation location() {
        return location;
    }

    public void location(TicketLocation location) {
        this.location = location;
        touch();
    }

    public UUID assignedToUuid() {
        return assignedToUuid;
    }

    public String assignedToName() {
        return assignedToName;
    }

    public void assign(UUID uuid, String name) {
        this.assignedToUuid = uuid;
        this.assignedToName = name;
        if (uuid != null && status == TicketStatus.OPEN) {
            status = TicketStatus.IN_PROGRESS;
        }
        touch();
    }

    public UUID closedByUuid() {
        return closedByUuid;
    }

    public String closedByName() {
        return closedByName;
    }

    public long closedAt() {
        return closedAt;
    }

    public String closeReason() {
        return closeReason;
    }

    public void close(UUID uuid, String name, String reason) {
        status = TicketStatus.CLOSED;
        closedByUuid = uuid;
        closedByName = name;
        closedAt = System.currentTimeMillis();
        closeReason = reason;
        touch();
    }

    public void reopen() {
        status = TicketStatus.OPEN;
        assignedToUuid = null;
        assignedToName = null;
        closedByUuid = null;
        closedByName = null;
        closedAt = 0L;
        closeReason = null;
        rating = 0;
        ratedAt = 0L;
        touch();
    }

    public int rating() {
        return rating;
    }

    public long ratedAt() {
        return ratedAt;
    }

    public void rate(int value) {
        rating = Math.max(1, Math.min(5, value));
        ratedAt = System.currentTimeMillis();
        touch();
    }

    public void ratingMeta(int value, long timestamp) {
        rating = Math.max(0, Math.min(5, value));
        ratedAt = timestamp;
    }

    public UUID deletedByUuid() {
        return deletedByUuid;
    }

    public String deletedByName() {
        return deletedByName;
    }

    public long deletedAt() {
        return deletedAt;
    }

    public void markDeleted(UUID uuid, String name) {
        status = TicketStatus.DELETED;
        deletedByUuid = uuid;
        deletedByName = name;
        deletedAt = System.currentTimeMillis();
        touch();
    }

    public void deletedMeta(UUID uuid, String name, long timestamp) {
        deletedByUuid = uuid;
        deletedByName = name;
        deletedAt = timestamp;
    }

    public void closedMeta(UUID uuid, String name, long timestamp, String reason) {
        closedByUuid = uuid;
        closedByName = name;
        closedAt = timestamp;
        closeReason = reason;
    }

    public List<TicketMessage> messages() {
        return Collections.unmodifiableList(messages);
    }

    public TicketMessage firstMessage() {
        return messages.isEmpty() ? null : messages.get(0);
    }

    public TicketMessage lastMessage() {
        if (messages.isEmpty()) {
            return null;
        }
        for (int index = messages.size() - 1; index >= 0; index--) {
            if (!messages.get(index).internal()) {
                return messages.get(index);
            }
        }
        return messages.get(messages.size() - 1);
    }

    public boolean lastMessageFromStaff() {
        TicketMessage last = lastMessage();
        return last != null && last.staff();
    }

    public List<TicketMessage> internalNotes() {
        return messages.stream().filter(TicketMessage::internal).toList();
    }

    public void addMessage(TicketMessage message) {
        messages.add(message);
        touch();
    }

    public boolean isOwner(UUID uuid) {
        return ownerUuid.equals(uuid);
    }

    private void touch() {
        updatedAt = System.currentTimeMillis();
    }
}
