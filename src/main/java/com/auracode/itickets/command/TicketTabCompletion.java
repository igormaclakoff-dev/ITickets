package com.auracode.itickets.command;

import com.auracode.itickets.TicketService;
import com.auracode.itickets.model.Ticket;
import com.auracode.itickets.storage.TicketStorage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class TicketTabCompletion {
    private static final List<String> SUBCOMMANDS = List.of("create", "list", "view", "comment", "close", "staff",
            "assign", "reopen", "delete", "purge", "tp", "ban", "unban", "reload", "rate", "history", "stats",
            "cancel", "reset", "export", "help");
    private final TicketStorage storage;
    private final TicketService ticketService;

    TicketTabCompletion(TicketStorage storage, TicketService ticketService) {
        this.storage = storage;
        this.ticketService = ticketService;
    }

    List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(visibleSubcommands(sender), args[0]);
        }
        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && completesTicketId(subcommand)) {
            return filter(visibleTicketIds(sender, subcommand), args[1]);
        }
        if (args.length == 3 && subcommand.equals("assign") && sender.hasPermission("itickets.assign")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getName).toList(), args[2]);
        }
        if (args.length == 3 && subcommand.equals("rate")) {
            return filter(List.of("1", "2", "3", "4", "5"), args[2]);
        }
        if (args.length == 2 && subcommand.equals("history") && sender.hasPermission("itickets.history")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getName).toList(), args[1]);
        }
        if (args.length == 2 && subcommand.equals("stats")
                && (sender.hasPermission("itickets.history") || sender.hasPermission("itickets.stats"))) {
            return filter(Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getName).toList(), args[1]);
        }
        if (args.length == 2 && subcommand.equals("ban") && sender.hasPermission("itickets.ban")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getName).toList(), args[1]);
        }
        if (args.length == 2 && subcommand.equals("unban") && sender.hasPermission("itickets.unban")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getName).toList(), args[1]);
        }
        if (args.length == 2 && subcommand.equals("list")) {
            return filter(visibleListFilters(sender), args[1]);
        }
        if (args.length == 2 && subcommand.equals("reset") && sender.hasPermission("itickets.reset")) {
            return filter(List.of("confirm"), args[1]);
        }
        if (args.length == 2 && subcommand.equals("export") && sender.hasPermission("itickets.export")) {
            return filter(List.of("csv", "txt"), args[1]);
        }
        return List.of();
    }

    List<String> visibleSubcommands(CommandSender sender) {
        return SUBCOMMANDS.stream().filter(subcommand -> canUseSubcommand(sender, subcommand)).toList();
    }

    List<String> visibleTicketIds(CommandSender sender, String rawSubcommand) {
        if (storage == null || ticketService == null) {
            return List.of();
        }
        String subcommand = normalizeTicketSubcommand(rawSubcommand);
        return storage.allTickets().stream().filter(ticket -> canCompleteTicket(sender, subcommand, ticket))
                .map(ticket -> String.valueOf(ticket.id())).toList();
    }

    private boolean canUseSubcommand(CommandSender sender, String subcommand) {
        boolean console = !(sender instanceof Player);
        return switch (subcommand) {
            case "create" -> sender instanceof Player player && player.hasPermission("itickets.create");
            case "list", "view", "help" -> true;
            case "comment" -> console || hasAny(sender, "itickets.comment.own", "itickets.comment.staff",
                    "itickets.staff");
            case "close" -> console || hasAny(sender, "itickets.close.own", "itickets.close", "itickets.staff");
            case "staff" -> sender instanceof Player
                    && hasAny(sender, "itickets.staff", "itickets.staff.gui");
            case "assign" -> sender.hasPermission("itickets.assign");
            case "reopen" -> sender.hasPermission("itickets.reopen");
            case "delete" -> sender.hasPermission("itickets.delete");
            case "purge" -> sender.hasPermission("itickets.purge");
            case "tp" -> sender instanceof Player && sender.hasPermission("itickets.teleport");
            case "ban" -> sender.hasPermission("itickets.ban");
            case "unban" -> sender.hasPermission("itickets.unban");
            case "reload" -> sender.hasPermission("itickets.reload");
            case "reset" -> sender.hasPermission("itickets.reset");
            case "export" -> sender.hasPermission("itickets.export");
            case "rate" -> sender instanceof Player;
            case "history" -> sender.hasPermission("itickets.history");
            case "stats" -> true;
            case "cancel" -> sender instanceof Player;
            default -> false;
        };
    }

    private List<String> visibleListFilters(CommandSender sender) {
        ArrayList<String> filters = new ArrayList<>(List.of("open", "closed"));
        if (hasAny(sender, "itickets.view.all", "itickets.staff")) {
            filters.add("all");
        }
        return filters;
    }

    private boolean completesTicketId(String subcommand) {
        return List.of("view", "open", "comment", "reply", "close", "assign", "reopen", "delete", "purge", "tp",
                "teleport", "rate").contains(subcommand);
    }

    private String normalizeTicketSubcommand(String subcommand) {
        return switch (subcommand.toLowerCase(Locale.ROOT)) {
            case "open" -> "view";
            case "reply" -> "comment";
            case "teleport" -> "tp";
            default -> subcommand.toLowerCase(Locale.ROOT);
        };
    }

    private boolean canCompleteTicket(CommandSender sender, String subcommand, Ticket ticket) {
        return switch (subcommand) {
            case "view" -> ticketService.canView(sender, ticket);
            case "comment" -> ticketService.canComment(sender, ticket);
            case "close" -> ticket.status().canBeClosed() && ticketService.canView(sender, ticket)
                    && ticketService.canClose(sender, ticket);
            case "assign" -> sender.hasPermission("itickets.assign") && ticket.status().canBeAssigned()
                    && ticketService.canView(sender, ticket);
            case "reopen" -> sender.hasPermission("itickets.reopen") && ticket.status().canBeReopened()
                    && ticketService.canView(sender, ticket);
            case "delete" -> sender.hasPermission("itickets.delete") && ticket.status().canBeSoftDeleted()
                    && ticketService.canView(sender, ticket);
            case "purge" -> sender.hasPermission("itickets.purge") && ticket.status().canBePurged()
                    && ticketService.canView(sender, ticket);
            case "tp" -> sender instanceof Player && sender.hasPermission("itickets.teleport")
                    && ticket.location() != null && ticketService.canView(sender, ticket);
            case "rate" -> sender instanceof Player player && ticket.isOwner(player.getUniqueId())
                    && ticket.status() == com.auracode.itickets.model.TicketStatus.CLOSED && ticket.rating() == 0;
            default -> false;
        };
    }

    private boolean hasAny(CommandSender sender, String... permissions) {
        for (String permission : permissions) {
            if (sender.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    private List<String> filter(List<String> values, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        ArrayList<String> result = new ArrayList<>();
        for (String value : values) {
            if (!value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                continue;
            }
            result.add(value);
        }
        return result;
    }
}
