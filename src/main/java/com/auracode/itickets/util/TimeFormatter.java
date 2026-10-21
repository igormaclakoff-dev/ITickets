package com.auracode.itickets.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TimeFormatter {
    private static final String DEFAULT_PATTERN = "dd.MM.yyyy HH:mm";
    private static final String DEFAULT_ZONE = "Europe/Moscow";
    private final DateTimeFormatter formatter;

    public TimeFormatter(String pattern, String zoneId) {
        this(pattern, zoneId, Logger.getLogger(TimeFormatter.class.getName()));
    }

    public TimeFormatter(String pattern, String zoneId, Logger logger) {
        DateTimeFormatter dateFormatter = formatter(pattern, logger);
        formatter = dateFormatter.withZone(zone(zoneId, logger));
    }

    public String format(long timestamp) {
        if (timestamp <= 0L) {
            return "-";
        }
        return formatter.format(Instant.ofEpochMilli(timestamp));
    }

    private DateTimeFormatter formatter(String pattern, Logger logger) {
        String selected = pattern == null || pattern.isBlank() ? DEFAULT_PATTERN : pattern;
        try {
            return DateTimeFormatter.ofPattern(selected);
        } catch (IllegalArgumentException exception) {
            warn(logger, "Неверный settings.date-format '" + selected + "'. Используется dd.MM.yyyy HH:mm.",
                    exception);
            return DateTimeFormatter.ofPattern(DEFAULT_PATTERN);
        }
    }

    private ZoneId zone(String zoneId, Logger logger) {
        String selected = zoneId == null || zoneId.isBlank() ? DEFAULT_ZONE : zoneId;
        try {
            return ZoneId.of(selected);
        } catch (RuntimeException exception) {
            warn(logger, "Неверный settings.time-zone '" + selected + "'. Используется Europe/Moscow.", exception);
            return ZoneId.of(DEFAULT_ZONE);
        }
    }

    private void warn(Logger logger, String message, RuntimeException exception) {
        if (logger != null) {
            logger.log(Level.WARNING, message, exception);
        }
    }
}
