package com.auracode.itickets.command;

import com.auracode.itickets.TicketService;
import com.auracode.itickets.config.MessageService;
import com.auracode.itickets.gui.GuiService;
import com.auracode.itickets.model.Ticket;
import com.auracode.itickets.model.TicketStatus;
import com.auracode.itickets.session.ChatInputService;
import com.auracode.itickets.storage.TicketStorage;
import com.auracode.itickets.util.TextSanitizer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TicketCommand implements CommandExecutor, TabCompleter {
    private final TicketStorage storage;
    private final TicketService ticketService;
    private final GuiService guiService;
    private final MessageService messages;
    private final ChatInputService chatInputs;
    private final Runnable reloadAction;
    private final TicketTabCompletion tabCompletion;

    public TicketCommand(TicketStorage storage, TicketService ticketService, GuiService guiService,
            MessageService messages, ChatInputService chatInputs, Runnable reloadAction) {
        this.storage = storage;
        this.ticketService = ticketService;
        this.guiService = guiService;
        this.messages = messages;
        this.chatInputs = chatInputs;
        this.reloadAction = reloadAction;
        tabCompletion = new TicketTabCompletion(storage, ticketService);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                guiService.showPlayerHome(player);
            } else {
                sendHelp(sender, label);
            }
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create", "new" -> openCreateTicketMenu(sender);
            case "list" -> openTicketListFromCommand(sender, args);
            case "view", "open" -> openTicketDetailsFromCommand(sender, args);
            case "comment", "reply" -> addTicketCommentFromCommand(sender, args);
            case "close" -> closeTicketFromCommand(sender, args);
            case "staff", "admin" -> openStaffDashboardFromCommand(sender);
            case "assign" -> assignTicketToOnlinePlayer(sender, args);
            case "reopen" -> reopenTicketFromCommand(sender, args);
            case "delete" -> softDeleteTicketFromCommand(sender, args);
            case "purge" -> purgeTicketFromCommand(sender, args);
            case "tp", "teleport" -> teleportToTicketLocation(sender, args);
            case "ban" -> banPlayerFromTicketCreation(sender, args, true);
            case "unban" -> banPlayerFromTicketCreation(sender, args, false);
            case "reload" -> reloadPluginConfigs(sender);
            case "reset", "wipe" -> resetDatabase(sender, args);
            case "export" -> exportTickets(sender, args);
            case "rate" -> rateTicketFromCommand(sender, args);
            case "history" -> sendAdminHistory(sender, args);
            case "stats" -> sendTicketStats(sender, args);
            case "cancel" -> cancelChatInput(sender);
            case "help", "?" -> sendHelp(sender, label);
            default -> {
                messages.send(sender, "errors.unknown-command");
                sendHelp(sender, label);
            }
        }
        return true;
    }

    private void openCreateTicketMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "errors.only-player");
            return;
        }
        guiService.showTicketCreation(player, null, 3);
    }

    private void openTicketListFromCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendTicketStats(sender, new String[0]);
            return;
        }
        TicketStatus status = args.length > 1 && args[1].equalsIgnoreCase("closed") ? TicketStatus.CLOSED
                : TicketStatus.OPEN;
        if (args.length > 1 && args[1].equalsIgnoreCase("all")) {
            guiService.showStaffTickets(player, null, null, false, 0);
            return;
        }
        guiService.showPlayerTickets(player, status, 0);
    }

    private void openTicketDetailsFromCommand(CommandSender sender, String[] args) {
        Ticket ticket = requireTicket(sender, args, 1, "/iticket view <id>");
        if (ticket == null) {
            return;
        }
        if (!ticketService.canView(sender, ticket)) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        if (sender instanceof Player player) {
            guiService.showTicketDetails(player, ticket);
        } else {
            messages.send(sender, "ticket.info", ticketService.placeholders(ticket));
        }
    }

    private void addTicketCommentFromCommand(CommandSender sender, String[] args) {
        Ticket ticket = requireTicket(sender, args, 1, "/iticket comment <id> <текст>");
        if (ticket == null) {
            return;
        }
        if (args.length < 3) {
            messages.send(sender, "errors.usage",
                    Map.of("usage", "/iticket comment <id> <текст>"));
            return;
        }
        ticketService.addComment(sender, ticket, join(args, 2));
    }

    private void closeTicketFromCommand(CommandSender sender, String[] args) {
        Ticket ticket = requireTicket(sender, args, 1, "/iticket close <id> [причина]");
        if (ticket == null) {
            return;
        }
        ticketService.close(sender, ticket, args.length >= 3 ? join(args, 2) : "Закрыто без указанной причины");
    }

    private void openStaffDashboardFromCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "errors.only-player");
            return;
        }
        guiService.showStaffDashboard(player);
    }

    private void assignTicketToOnlinePlayer(CommandSender sender, String[] args) {
        Ticket ticket = requireTicket(sender, args, 1, "/iticket assign <id> <ник>");
        if (ticket == null) {
            return;
        }
        if (args.length < 3) {
            messages.send(sender, "errors.usage", Map.of("usage", "/iticket assign <id> <ник>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            messages.send(sender, "errors.player-not-found");
            return;
        }
        ticketService.assign(sender, ticket, target);
    }

    private void reopenTicketFromCommand(CommandSender sender, String[] args) {
        Ticket ticket = requireTicket(sender, args, 1, "/iticket reopen <id>");
        if (ticket != null) {
            ticketService.reopen(sender, ticket);
        }
    }

    private void softDeleteTicketFromCommand(CommandSender sender, String[] args) {
        Ticket ticket = requireTicket(sender, args, 1, "/iticket delete <id>");
        if (ticket != null) {
            ticketService.delete(sender, ticket);
        }
    }

    private void purgeTicketFromCommand(CommandSender sender, String[] args) {
        Ticket ticket = requireTicket(sender, args, 1, "/iticket purge <id>");
        if (ticket != null) {
            ticketService.purge(sender, ticket);
        }
    }

    private void teleportToTicketLocation(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "errors.only-player");
            return;
        }
        if (!player.hasPermission("itickets.teleport")) {
            messages.send((CommandSender) player, "errors.no-permission");
            return;
        }
        Ticket ticket = requireTicket(sender, args, 1, "/iticket tp <id>");
        if (ticket == null) {
            return;
        }
        if (!ticketService.canView((CommandSender) player, ticket)) {
            messages.send((CommandSender) player, "errors.no-permission");
            return;
        }
        if (ticket.location() == null) {
            return;
        }
        Location location = ticket.location().toBukkitLocation();
        if (location == null) {
            messages.send((CommandSender) player, "errors.world-not-found");
            return;
        }
        player.teleport(location);
        messages.send((CommandSender) player, "ticket.teleported", ticketService.placeholders(ticket));
    }

    private void banPlayerFromTicketCreation(CommandSender sender, String[] args, boolean banned) {
        String permission = banned ? "itickets.ban" : "itickets.unban";
        if (!sender.hasPermission(permission)) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        if (args.length < 2) {
            messages.send(sender, "errors.usage", Map.of("usage",
                    banned ? "/iticket ban <ник>" : "/iticket unban <ник>"));
            return;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
        storage.setCreationBan(offlinePlayer.getUniqueId(), banned);
        messages.send(sender, banned ? "ticket.creation-ban-added" : "ticket.creation-ban-removed",
                Map.of("player", offlinePlayer.getName() == null ? args[1] : offlinePlayer.getName()));
    }

    private void reloadPluginConfigs(CommandSender sender) {
        if (!sender.hasPermission("itickets.reload")) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        reloadAction.run();
        messages.send(sender, "plugin.reloaded");
    }

    private void resetDatabase(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itickets.reset")) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            messages.send(sender, "reset.confirm-required");
            return;
        }
        storage.resetAll();
        messages.send(sender, "reset.done");
    }

    private void exportTickets(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itickets.export")) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        String format = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "csv";
        try {
            java.io.File file = ticketService.exportTickets(format);
            messages.send(sender, "ticket.exported", Map.of(
                    "count", String.valueOf(storage.allTickets().size()),
                    "file", file.getName()));
        } catch (Exception exception) {
            messages.send(sender, "errors.export-failed");
        }
    }

    private void sendTicketStats(CommandSender sender, String[] args) {
        if (args.length > 1) {
            if (!sender.hasPermission("itickets.stats") && !sender.hasPermission("itickets.history")) {
                messages.send(sender, "errors.no-permission");
                return;
            }
            sendAdminStats(sender, args[1]);
            return;
        }
        if (sender.hasPermission("itickets.stats")) {
            messages.send(sender, "stats.global", ticketService.globalStats());
            List<Map<String, String>> top = ticketService.topAdmins(5);
            if (top.isEmpty()) {
                messages.send(sender, "stats.top-empty");
                return;
            }
            messages.send(sender, "stats.top-header");
            int rank = 1;
            for (Map<String, String> entry : top) {
                HashMap<String, String> line = new HashMap<>(entry);
                line.put("rank", String.valueOf(rank++));
                messages.send(sender, "stats.top-entry", line);
            }
            return;
        }
        if (sender instanceof Player player) {
            messages.send(sender, "stats.personal", ticketService.personalStats(player.getUniqueId()));
            return;
        }
        messages.send(sender, "errors.no-permission");
    }

    private void sendAdminStats(CommandSender sender, String name) {
        Map<String, String> stats = ticketService.adminStats(name);
        if (Long.parseLong(stats.getOrDefault("assigned", "0")) == 0
                && Long.parseLong(stats.getOrDefault("closed", "0")) == 0) {
            messages.send(sender, "stats.admin-empty", Map.of("admin", TextSanitizer.clean(name)));
            return;
        }
        messages.send(sender, "stats.admin", stats);
    }

    private void sendAdminHistory(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itickets.history")) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        if (args.length < 2) {
            messages.send(sender, "errors.usage", Map.of("usage", "/iticket history <ник>"));
            return;
        }
        String name = TextSanitizer.clean(args[1]);
        List<Map<String, String>> history = ticketService.adminHistory(args[1], 10);
        messages.send(sender, "history.header", Map.of("admin", name,
                "count", String.valueOf(history.size())));
        for (Map<String, String> entry : history) {
            messages.send(sender, "history.entry", entry);
        }
        if (history.isEmpty()) {
            messages.send(sender, "history.empty", Map.of("admin", name));
        }
    }

    private void rateTicketFromCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "errors.only-player");
            return;
        }
        Ticket ticket = requireTicket(sender, args, 1, "/iticket rate <id> <1-5>");
        if (ticket == null) {
            return;
        }
        if (args.length < 3) {
            messages.send(sender, "errors.usage", Map.of("usage", "/iticket rate <id> <1-5>"));
            return;
        }
        int value;
        try {
            value = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            messages.send(sender, "errors.rate-invalid");
            return;
        }
        ticketService.rate(sender, ticket, value);
    }

    private void cancelChatInput(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "errors.only-player");
            return;
        }
        if (chatInputs.cancel(player)) {
            messages.send((CommandSender) player, "input.cancelled");
        } else {
            messages.send((CommandSender) player, "input.nothing-to-cancel");
        }
    }

    private Ticket requireTicket(CommandSender sender, String[] args, int index, String usage) {
        if (args.length <= index) {
            messages.send(sender, "errors.usage", Map.of("usage", usage));
            return null;
        }
        int id;
        try {
            id = Integer.parseInt(args[index]);
        } catch (NumberFormatException exception) {
            messages.send(sender, "errors.invalid-number");
            return null;
        }
        Ticket ticket = storage.byId(id);
        if (ticket == null) {
            messages.send(sender, "errors.ticket-not-found");
        }
        return ticket;
    }

    private void sendHelp(CommandSender sender, String label) {
        boolean player = sender instanceof Player;
        messages.send(sender, "help.header");
        messages.send(sender, "help.player");
        helpLine(sender, label, "", "открыть меню тикетов");
        if (player && sender.hasPermission("itickets.create")) {
            helpLine(sender, label, "create", "создать новое обращение");
        }
        helpLine(sender, label, "list [open|closed]", "список тикетов");
        helpLine(sender, label, "view <id>", "открыть тикет");
        helpLine(sender, label, "comment <id> <текст>", "ответить в тикет");
        helpLine(sender, label, "close <id> [причина]", "закрыть тикет");
        helpLine(sender, label, "rate <id> <1-5>", "оценить работу администратора");
        helpLine(sender, label, "stats [ник]", "статистика тикетов");
        helpLine(sender, label, "cancel", "отменить ввод текста");

        boolean staff = hasAny(sender, "itickets.staff", "itickets.staff.gui", "itickets.assign");
        if (staff) {
            messages.send(sender, "help.staff");
            if (player && hasAny(sender, "itickets.staff", "itickets.staff.gui")) {
                helpLine(sender, label, "staff", "меню персонала");
            }
            if (sender.hasPermission("itickets.assign")) {
                helpLine(sender, label, "assign <id> <ник>", "назначить ответственного");
            }
            if (sender.hasPermission("itickets.reopen")) {
                helpLine(sender, label, "reopen <id>", "переоткрыть закрытый тикет");
            }
            if (player && sender.hasPermission("itickets.teleport")) {
                helpLine(sender, label, "tp <id>", "телепорт к месту тикета");
            }
            if (sender.hasPermission("itickets.history")) {
                helpLine(sender, label, "history <ник>", "история тикетов администратора");
            }
        }

        boolean admin = hasAny(sender, "itickets.delete", "itickets.purge", "itickets.ban", "itickets.unban",
                "itickets.reload", "itickets.reset", "itickets.export");
        if (admin) {
            messages.send(sender, "help.admin");
            if (sender.hasPermission("itickets.delete")) {
                helpLine(sender, label, "delete <id>", "мягко удалить тикет");
            }
            if (sender.hasPermission("itickets.purge")) {
                helpLine(sender, label, "purge <id>", "удалить тикет навсегда");
            }
            if (sender.hasPermission("itickets.ban")) {
                helpLine(sender, label, "ban <ник>", "запретить игроку создавать тикеты");
            }
            if (sender.hasPermission("itickets.unban")) {
                helpLine(sender, label, "unban <ник>", "снять запрет создания тикетов");
            }
            if (sender.hasPermission("itickets.reload")) {
                helpLine(sender, label, "reload", "перезагрузить конфиги");
            }
            if (sender.hasPermission("itickets.reset")) {
                helpLine(sender, label, "reset confirm", "полностью очистить базу тикетов");
            }
            if (sender.hasPermission("itickets.export")) {
                helpLine(sender, label, "export [csv|txt]", "выгрузить базу тикетов в файл");
            }
        }
        helpLine(sender, label, "help", "показать этот список");
    }

    private void helpLine(CommandSender sender, String label, String subcommand, String description) {
        String command = "/" + label + (subcommand.isEmpty() ? "" : " " + subcommand);
        messages.send(sender, "help.line", Map.of("command", command, "description", description));
    }

    private boolean hasAny(CommandSender sender, String... permissions) {
        for (String permission : permissions) {
            if (sender.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    private String join(String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        return tabCompletion.complete(sender, args);
    }
}
