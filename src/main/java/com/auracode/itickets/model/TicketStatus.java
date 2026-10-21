package com.auracode.itickets.model;

public enum TicketStatus {
    OPEN("Открыт", "Открытые"),
    IN_PROGRESS("В работе", "В работе"),
    CLOSED("Закрыт", "Закрытые"),
    DELETED("Удален", "Удаленные");

    private final String displayName;
    private final String pluralName;

    TicketStatus(String displayName, String pluralName) {
        this.displayName = displayName;
        this.pluralName = pluralName;
    }

    public String displayName() {
        return displayName;
    }

    public String pluralName() {
        return pluralName;
    }

    public boolean canReceiveComments() {
        return this == OPEN || this == IN_PROGRESS;
    }

    public boolean canBeClosed() {
        return this == OPEN || this == IN_PROGRESS;
    }

    public boolean canBeReopened() {
        return this == CLOSED;
    }

    public boolean canBeAssigned() {
        return this == OPEN;
    }

    public boolean canBeSoftDeleted() {
        return this != DELETED;
    }

    public boolean canBePurged() {
        return this == DELETED;
    }

    public boolean isActive() {
        return this == OPEN || this == IN_PROGRESS;
    }

    public static TicketStatus fromString(String value) {
        if (value == null) {
            return OPEN;
        }
        for (TicketStatus status : TicketStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        return OPEN;
    }
}
