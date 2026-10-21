package com.auracode.itickets.gui;

import com.auracode.itickets.config.PluginSettings;
import com.auracode.itickets.model.Ticket;
import com.auracode.itickets.model.TicketMessage;
import com.auracode.itickets.util.TimeFormatter;
import com.auracode.itickets.config.MessageService;

import java.util.ArrayList;
import java.util.List;

final class GuiTextFormatter {
    static final int LORE_WRAP_WIDTH = 38;
    private final String dateFormat;
    private final String timeZone;

    GuiTextFormatter(PluginSettings settings) {
        this(settings.dateFormat(), settings.timeZone());
    }

    GuiTextFormatter(String dateFormat, String timeZone) {
        this.dateFormat = dateFormat;
        this.timeZone = timeZone;
    }

    String priorityMaterial(int priority) {
        return switch (priority) {
            case 1 -> "LIGHT_GRAY_WOOL";
            case 2 -> "LIME_WOOL";
            case 3 -> "YELLOW_WOOL";
            case 4 -> "ORANGE_WOOL";
            case 5 -> "RED_WOOL";
            default -> "WHITE_WOOL";
        };
    }

    String priorityName(int priority) {
        return switch (priority) {
            case 1 -> "низкая";
            case 2 -> "обычная";
            case 3 -> "средняя";
            case 4 -> "высокая";
            case 5 -> "срочная";
            default -> "неизвестная";
        };
    }

    String priorityColor(int priority) {
        return switch (priority) {
            case 1 -> "<gray>";
            case 2 -> "<green>";
            case 3 -> "<yellow>";
            case 4 -> "<gold>";
            case 5 -> "<red>";
            default -> "<white>";
        };
    }

    List<String> messageLore(Ticket ticket, MessageService messages) {
        if (ticket.messages().isEmpty()) {
            return List.of("<dark_gray>Сообщений пока нет.</dark_gray>");
        }
        ArrayList<String> lore = new ArrayList<>();
        TimeFormatter formatter = new TimeFormatter(dateFormat, timeZone);
        List<TicketMessage> ticketMessages = ticket.messages();
        int visible = 0;
        for (TicketMessage message : ticketMessages) {
            if (message.internal()) {
                continue;
            }
            if (visible > 0) {
                lore.add(" ");
            }
            lore.add("<white>" + plain(message.authorName()) + " <dark_gray>("
                    + formatter.format(message.timestamp()) + ")</dark_gray>");
            lore.addAll(wrapLore(plain(message.text()), "<gray>", LORE_WRAP_WIDTH));
            visible++;
        }
        if (visible == 0) {
            return List.of("<dark_gray>Сообщений пока нет.</dark_gray>");
        }
        return lore;
    }

    List<String> noteLore(Ticket ticket, MessageService messages) {
        List<TicketMessage> notes = ticket.internalNotes();
        if (notes.isEmpty()) {
            return List.of("<dark_gray>Заметок пока нет.</dark_gray>");
        }
        ArrayList<String> lore = new ArrayList<>();
        TimeFormatter formatter = new TimeFormatter(dateFormat, timeZone);
        for (int index = 0; index < notes.size(); index++) {
            TicketMessage note = notes.get(index);
            if (index > 0) {
                lore.add(" ");
            }
            lore.add("<aqua>" + plain(note.authorName()) + " <dark_gray>("
                    + formatter.format(note.timestamp()) + ")</dark_gray>");
            lore.addAll(wrapLore(plain(note.text()), "<white>", LORE_WRAP_WIDTH));
        }
        return lore;
    }

    String wrapPlaceholder(String text, String color, int width, MessageService messages) {
        List<String> wrapped = wrapLore(plain(text), color, width);
        return String.join("\n", wrapped);
    }

    List<String> wrapLore(String text, String color, int width) {
        return wrapPlain(text, width).stream().map(line -> color + line).toList();
    }

    List<String> wrapPlain(String text, int width) {
        String clean = text == null ? "" : text.strip();
        if (clean.isEmpty()) {
            return List.of("");
        }
        ArrayList<String> result = new ArrayList<>();
        for (String paragraph : clean.split("\\R")) {
            wrapParagraph(paragraph, width, result);
        }
        return result;
    }

    private void wrapParagraph(String paragraph, int width, List<String> result) {
        StringBuilder line = new StringBuilder();
        for (String word : paragraph.split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (line.isEmpty()) {
                appendWordOrSplit(word, width, result, line);
                continue;
            }
            if (line.length() + 1 + word.length() <= width) {
                line.append(' ').append(word);
                continue;
            }
            result.add(line.toString());
            line.setLength(0);
            appendWordOrSplit(word, width, result, line);
        }
        if (!line.isEmpty()) {
            result.add(line.toString());
        }
    }

    private void appendWordOrSplit(String word, int width, List<String> result, StringBuilder line) {
        if (word.length() <= width) {
            line.append(word);
            return;
        }
        int start = 0;
        while (start < word.length()) {
            int end = Math.min(start + width, word.length());
            String part = word.substring(start, end);
            if (end < word.length()) {
                result.add(part);
            } else {
                line.append(part);
            }
            start = end;
        }
    }

    private String plain(String input) {
        return com.auracode.itickets.util.TextSanitizer.clean(input);
    }

    static String clean(String input) {
        return com.auracode.itickets.util.TextSanitizer.clean(input);
    }

    static String plainOf(String input) {
        return com.auracode.itickets.util.TextSanitizer.clean(input);
    }
}
