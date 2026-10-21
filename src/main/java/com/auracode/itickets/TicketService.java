package com.auracode.itickets;

import com.auracode.itickets.config.CategoryRegistry;
import com.auracode.itickets.config.MessageService;
import com.auracode.itickets.config.PluginSettings;
import com.auracode.itickets.model.Ticket;
import com.auracode.itickets.model.TicketCategory;
import com.auracode.itickets.model.TicketLocation;
import com.auracode.itickets.model.TicketMessage;
import com.auracode.itickets.model.TicketStatus;
import com.auracode.itickets.storage.TicketStorage;
import com.auracode.itickets.util.TextSanitizer;
import com.auracode.itickets.util.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class TicketService {
    private final TicketStorage storage;
    private final PluginSettings settings;
    private final MessageService messages;
    private final CategoryRegistry categories;
    private final Map<UUID, Long> lastCreation = new HashMap<>();
    private TimeFormatter timeFormatter;

    public TicketService(TicketStorage storage, PluginSettings settings, MessageService messages,
            CategoryRegistry categories) {
        this.storage = storage;
        this.settings = settings;
        this.messages = messages;
        this.categories = categories;
        timeFormatter = new TimeFormatter(settings.dateFormat(), settings.timeZone());
    }

    public void reloadFormatter() {
        timeFormatter = new TimeFormatter(settings.dateFormat(), settings.timeZone());
    }

    public Ticket createFromInput(Player player, String categoryId, int priority, String text) {
        TicketCategory category = categories.byId(categoryId);
        if (!player.hasPermission("itickets.create")) {
            messages.send(player, "errors.no-permission");
            return null;
        }
        if (storage.isCreationBanned(player.getUniqueId())) {
            messages.send(player, "errors.creation-banned");
            return null;
        }
        if (!categories.canCreate(player, category)) {
            messages.send(player, "errors.category-no-permission");
            return null;
        }
        String cleanText = text == null ? "" : text.trim();
        if (cleanText.isBlank()) {
            messages.send(player, "errors.empty-message");
            return null;
        }
        if (cleanText.length() > settings.maxMessageLength()) {
            messages.send(player, "errors.message-too-long",
                    Map.of("max", String.valueOf(settings.maxMessageLength())));
            return null;
        }
        if (!player.hasPermission("itickets.bypass.limit")
                && storage.countOpen(player.getUniqueId()) >= settings.maxOpenTickets()) {
            messages.send(player, "errors.open-limit",
                    Map.of("limit", String.valueOf(settings.maxOpenTickets())));
            return null;
        }
        long now = System.currentTimeMillis();
        long last = lastCreation.getOrDefault(player.getUniqueId(), 0L);
        long wait = settings.cooldownSeconds() - (now - last) / 1000L;
        if (!player.hasPermission("itickets.bypass.cooldown") && wait > 0L) {
            messages.send(player, "errors.cooldown", Map.of("seconds", String.valueOf(wait)));
            return null;
        }
        Ticket ticket = storage.createTicket(player.getUniqueId(), player.getName(), player.getLocation(),
                category.id(), priority, cleanText);
        lastCreation.put(player.getUniqueId(), now);
        messages.send(player, "ticket.created", placeholders(ticket));
        notifyStaff("notify.new-ticket", placeholders(ticket), "itickets.notify.create");
        return ticket;
    }

    public boolean canView(CommandSender sender, Ticket ticket) {
        if (ticket == null) {
            return false;
        }
        if (!(sender instanceof Player player)) {
            return true;
        }
        if (ticket.status() == TicketStatus.DELETED && !player.hasPermission("itickets.view.deleted")) {
            return false;
        }
        if (ticket.isOwner(player.getUniqueId()) && player.hasPermission("itickets.view.own")) {
            return true;
        }
        if (ticket.status() == TicketStatus.CLOSED && !player.hasPermission("itickets.view.closed")) {
            return false;
        }
        return player.hasPermission("itickets.view.all") || player.hasPermission("itickets.staff");
    }

    public boolean canComment(CommandSender sender, Ticket ticket) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        if (!ticket.status().canReceiveComments()) {
            return false;
        }
        if (ticket.isOwner(player.getUniqueId())) {
            return player.hasPermission("itickets.comment.own");
        }
        return player.hasPermission("itickets.comment.staff") || player.hasPermission("itickets.staff");
    }

    public void addComment(CommandSender sender, Ticket ticket, String text) {
        if (ticket == null) {
            messages.send(sender, "errors.ticket-not-found");
            return;
        }
        if (ticket.status() == TicketStatus.DELETED) {
            messages.send(sender, "errors.ticket-deleted");
            return;
        }
        if (!ticket.status().canReceiveComments()) {
            messages.send(sender, "errors.ticket-not-open");
            return;
        }
        if (!canComment(sender, ticket)) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        String cleanText = text == null ? "" : text.trim();
        if (cleanText.isBlank()) {
            messages.send(sender, "errors.empty-message");
            return;
        }
        if (cleanText.length() > settings.maxMessageLength()) {
            messages.send(sender, "errors.message-too-long",
                    Map.of("max", String.valueOf(settings.maxMessageLength())));
            return;
        }
        boolean staff;
        UUID uuid;
        String name;
        if (sender instanceof Player player) {
            staff = !ticket.isOwner(player.getUniqueId());
            uuid = player.getUniqueId();
            name = player.getName();
        } else {
            staff = true;
            uuid = null;
            name = "Консоль";
        }
        ticket.addMessage(new TicketMessage(uuid, name, staff, System.currentTimeMillis(), cleanText));
        storage.saveTicket(ticket);
        messages.send(sender, "ticket.comment-added", placeholders(ticket));
        if (staff) {
            notifyOwner(ticket, "notify.owner-comment", "offline.owner-comment");
        } else {
            notifyStaff("notify.player-comment", placeholders(ticket), "itickets.notify.comment");
        }
    }

    public void close(CommandSender sender, Ticket ticket, String reason) {
        if (ticket == null) {
            messages.send(sender, "errors.ticket-not-found");
            return;
        }
        if (!canClose(sender, ticket)) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        if (ticket.status() == TicketStatus.DELETED) {
            messages.send(sender, "errors.ticket-deleted");
            return;
        }
        if (!ticket.status().canBeClosed()) {
            messages.send(sender, "errors.ticket-not-open");
            return;
        }
        UUID uuid;
        String name;
        if (sender instanceof Player player) {
            uuid = player.getUniqueId();
            name = player.getName();
        } else {
            uuid = null;
            name = "Консоль";
        }
        ticket.close(uuid, name, reason == null || reason.isBlank() ? "Причина не указана" : reason.trim());
        storage.saveTicket(ticket);
        messages.send(sender, "ticket.closed", placeholders(ticket));
        notifyOwner(ticket, "notify.owner-closed", "offline.owner-closed");
        notifyStaff("notify.staff-closed", placeholders(ticket), "itickets.notify.close");
        requestRating(ticket);
    }
    public void reopen(CommandSender sender, Ticket ticket) {
        if (ticket == null) {
            messages.send(sender, "errors.ticket-not-found");
            return;
        }
        if (!sender.hasPermission("itickets.reopen")) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        if (ticket.status() == TicketStatus.DELETED) {
            messages.send(sender, "errors.ticket-deleted");
            return;
        }
        if (!ticket.status().canBeReopened()) {
            messages.send(sender, "errors.ticket-not-closed");
            return;
        }
        ticket.reopen();
        storage.saveTicket(ticket);
        messages.send(sender, "ticket.reopened", placeholders(ticket));
        notifyOwner(ticket, "notify.owner-reopened", "offline.owner-reopened");
    }

    public void delete(CommandSender sender, Ticket ticket) {
        if (ticket == null) {
            messages.send(sender, "errors.ticket-not-found");
            return;
        }
        if (!sender.hasPermission("itickets.delete")) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        if (!ticket.status().canBeSoftDeleted()) {
            messages.send(sender, "errors.ticket-deleted");
            return;
        }
        UUID uuid;
        String name;
        if (sender instanceof Player player) {
            uuid = player.getUniqueId();
            name = player.getName();
        } else {
            uuid = null;
            name = "Консоль";
        }
        ticket.markDeleted(uuid, name);
        storage.saveTicket(ticket);
        messages.send(sender, "ticket.deleted", placeholders(ticket));
    }

    public void purge(CommandSender sender, Ticket ticket) {
        if (ticket == null) {
            messages.send(sender, "errors.ticket-not-found");
            return;
        }
        if (!sender.hasPermission("itickets.purge")) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        if (!ticket.status().canBePurged()) {
            messages.send(sender, "errors.ticket-not-deleted");
            return;
        }
        int id = ticket.id();
        storage.purge(ticket);
        messages.send(sender, "ticket.purged", Map.of("id", String.valueOf(id)));
    }

    public void assign(CommandSender sender, Ticket ticket, Player target) {
        if (ticket == null) {
            messages.send(sender, "errors.ticket-not-found");
            return;
        }
        if (!sender.hasPermission("itickets.assign")) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        if (ticket.status() == TicketStatus.DELETED) {
            messages.send(sender, "errors.ticket-deleted");
            return;
        }
        if (!ticket.status().isActive()) {
            messages.send(sender, "errors.ticket-not-open");
            return;
        }
        ticket.assign(target.getUniqueId(), target.getName());
        storage.saveTicket(ticket);
        messages.send(sender, "ticket.assigned", placeholders(ticket));
        notifyStaff("notify.staff-taken", placeholders(ticket), "itickets.notify.create");
        notifyOwner(ticket, "notify.owner-taken", "offline.owner-taken");
    }

    public void takeTicket(CommandSender sender, Ticket ticket) {
        if (ticket == null) {
            messages.send(sender, "errors.ticket-not-found");
            return;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, "errors.only-player");
            return;
        }
        if (!player.hasPermission("itickets.assign")) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        if (ticket.status() == TicketStatus.DELETED) {
            messages.send(sender, "errors.ticket-deleted");
            return;
        }
        if (!ticket.status().canBeAssigned() && ticket.status() != TicketStatus.IN_PROGRESS) {
            messages.send(sender, "errors.ticket-not-open");
            return;
        }
        if (player.getUniqueId().equals(ticket.assignedToUuid())) {
            messages.send(sender, "ticket.already-taken", placeholders(ticket));
            return;
        }
        ticket.assign(player.getUniqueId(), player.getName());
        storage.saveTicket(ticket);
        messages.send(sender, "ticket.assigned-self", placeholders(ticket));
        notifyStaff("notify.staff-taken", placeholders(ticket), "itickets.notify.create");
        notifyOwner(ticket, "notify.owner-taken", "offline.owner-taken");
    }

    public void updatePriority(CommandSender sender, Ticket ticket, int priority) {
        if (ticket == null) {
            messages.send(sender, "errors.ticket-not-found");
            return;
        }
        if (!sender.hasPermission("itickets.manage")) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        if (ticket.status() == TicketStatus.DELETED) {
            messages.send(sender, "errors.ticket-deleted");
            return;
        }
        if (priority < 1 || priority > 5) {
            messages.send(sender, "errors.rate-invalid");
            return;
        }
        ticket.priority(priority);
        storage.saveTicket(ticket);
        messages.send(sender, "ticket.priority-updated", placeholders(ticket));
    }

    public void updateCategory(CommandSender sender, Ticket ticket, String categoryId) {
        if (ticket == null) {
            messages.send(sender, "errors.ticket-not-found");
            return;
        }
        if (!sender.hasPermission("itickets.manage")) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        if (ticket.status() == TicketStatus.DELETED) {
            messages.send(sender, "errors.ticket-deleted");
            return;
        }
        TicketCategory category = categories.byId(categoryId);
        if (category == null) {
            messages.send(sender, "errors.category-not-found");
            return;
        }
        ticket.categoryId(category.id());
        storage.saveTicket(ticket);
        messages.send(sender, "ticket.category-updated", placeholders(ticket));
    }

    public void addNote(CommandSender sender, Ticket ticket, String text) {
        if (ticket == null) {
            messages.send(sender, "errors.ticket-not-found");
            return;
        }
        if (ticket.status() == TicketStatus.DELETED) {
            messages.send(sender, "errors.ticket-deleted");
            return;
        }
        if (!isStaff(sender, ticket)) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        String cleanText = text == null ? "" : text.trim();
        if (cleanText.isBlank()) {
            messages.send(sender, "errors.empty-message");
            return;
        }
        if (cleanText.length() > settings.maxMessageLength()) {
            messages.send(sender, "errors.message-too-long",
                    Map.of("max", String.valueOf(settings.maxMessageLength())));
            return;
        }
        UUID uuid;
        String name;
        if (sender instanceof Player player) {
            uuid = player.getUniqueId();
            name = player.getName();
        } else {
            uuid = null;
            name = "Консоль";
        }
        ticket.addMessage(new TicketMessage(uuid, name, true, System.currentTimeMillis(), cleanText, true));
        storage.saveTicket(ticket);
        messages.send(sender, "ticket.note-added", placeholders(ticket));
    }

    public void autoClose(Ticket ticket) {
        if (ticket == null || !ticket.status().isActive()) {
            return;
        }
        ticket.close(null, "Автоматика", settings.autoCloseReason());
        storage.saveTicket(ticket);
        notifyOwner(ticket, "notify.owner-autoclosed", "offline.owner-autoclosed");
    }

    public void purgeInactive(int days) {
        long threshold = System.currentTimeMillis() - days * 86_400_000L;
        for (Ticket ticket : storage.allTickets()) {
            if (ticket.status().isActive() && ticket.updatedAt() < threshold) {
                ticket.markDeleted(null, "Автоматика");
                storage.saveTicket(ticket);
            }
        }
    }

    public void remindStaff() {
        List<Ticket> waiting = storage.expectingReply();
        if (waiting.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        long afterMillis = settings.reminderAfterMinutes() * 60_000L;
        List<Ticket> overdue = waiting.stream().filter(ticket -> now - ticket.updatedAt() >= afterMillis).toList();
        if (overdue.isEmpty()) {
            return;
        }
        Map<String, String> extra = Map.of("count", String.valueOf(overdue.size()),
                "oldest", String.valueOf(overdue.get(0).id()),
                "oldest_player", safe(overdue.get(0).ownerName()));
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("itickets.notify.join") && !online.hasPermission("itickets.staff")) {
                continue;
            }
            messages.send(online, "notify.reminder", extra);
        }
    }

    public boolean isStaff(CommandSender sender, Ticket ticket) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        if (ticket.isOwner(player.getUniqueId())) {
            return false;
        }
        return player.hasPermission("itickets.comment.staff") || player.hasPermission("itickets.staff");
    }

    public void requestRating(Ticket ticket) {
        String admin = ratingTargetName(ticket);
        if (admin == null) {
            return;
        }
        Player owner = Bukkit.getPlayer(ticket.ownerUuid());
        Map<String, String> map = placeholders(ticket);
        map.put("admin", safe(admin));
        if (owner != null && owner.isOnline()) {
            messages.send(owner, "rating.request", map);
            messages.sendRaw(owner, ratingStarsLine(ticket.id()), Map.of());
        } else {
            storage.addPendingNotification(ticket.ownerUuid(), messages.text("rating.request", map));
            storage.addPendingNotification(ticket.ownerUuid(), ratingStarsLine(ticket.id()));
        }
    }

    public void rate(CommandSender sender, Ticket ticket, int value) {
        if (ticket == null) {
            messages.send(sender, "errors.ticket-not-found");
            return;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, "errors.only-player");
            return;
        }
        if (!ticket.isOwner(player.getUniqueId())) {
            messages.send(sender, "errors.rate-not-owner");
            return;
        }
        if (ticket.status() != TicketStatus.CLOSED) {
            messages.send(sender, "errors.rate-not-closed");
            return;
        }
        if (ticket.rating() > 0) {
            messages.send(sender, "errors.rate-already");
            return;
        }
        if (value < 1 || value > 5) {
            messages.send(sender, "errors.rate-invalid");
            return;
        }
        ticket.rate(value);
        storage.saveTicket(ticket);
        messages.send(sender, "rating.done", placeholders(ticket));
        notifyStaff("notify.rating", placeholders(ticket), "itickets.notify.rating");
    }

    public String ratingTargetName(Ticket ticket) {
        if (ticket.assignedToName() != null && !ticket.assignedToName().isBlank()) {
            return ticket.assignedToName();
        }
        if (ticket.closedByUuid() != null && !ticket.closedByUuid().equals(ticket.ownerUuid())) {
            return ticket.closedByName();
        }
        return null;
    }

    private String ratingStarsLine(int id) {
        StringBuilder line = new StringBuilder("◆ <gray>Оценить: </gray>");
        for (int stars = 1; stars <= 5; stars++) {
            if (stars > 1) {
                line.append(" <dark_gray>|</dark_gray> ");
            }
            StringBuilder group = new StringBuilder();
            for (int index = 1; index <= 5; index++) {
                if (index <= stars) {
                    group.append("<yellow>★</yellow>");
                } else {
                    group.append("<dark_gray>☆</dark_gray>");
                }
            }
            line.append("<click:run_command:'/iticket rate ").append(id).append(' ').append(stars)
                    .append("'><hover:show_text:'<gray>Оценка: ").append(stars).append(" из 5</gray>'>")
                    .append(group)
                    .append("</hover></click>");
        }
        return line.toString();
    }

    public boolean canClose(CommandSender sender, Ticket ticket) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        if (ticket.isOwner(player.getUniqueId())) {
            return player.hasPermission("itickets.close.own");
        }
        return player.hasPermission("itickets.close") || player.hasPermission("itickets.staff");
    }

    public String applyTemplate(Ticket ticket, CommandSender sender, String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        Map<String, String> map = new HashMap<>(placeholders(ticket));
        String adminName = sender instanceof Player player ? player.getName()
                : (sender == null ? "" : sender.getName());
        map.put("admin", safe(adminName));
        String result = text;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }

    public Map<String, String> placeholders(Ticket ticket) {
        TicketCategory category = categories.byId(ticket.categoryId());
        TicketMessage first = ticket.firstMessage();
        TicketMessage last = ticket.lastMessage();
        TicketLocation location = ticket.location();
        HashMap<String, String> map = new HashMap<>();
        map.put("id", String.valueOf(ticket.id()));
        map.put("player", safe(ticket.ownerName()));
        map.put("status", displayStatus(ticket));
        map.put("status_raw", ticket.status().name());
        map.put("category", category == null ? ticket.categoryId()
                : (category.color() == null ? "" : category.color()) + category.displayName());
        map.put("category_plain", category == null ? ticket.categoryId() : category.displayName());
        map.put("category_id", ticket.categoryId());
        map.put("priority", String.valueOf(ticket.priority()));
        map.put("priority_value", String.valueOf(ticket.priority()));
        map.put("priority_name", priorityName(ticket.priority()));
        map.put("priority_colored", priorityColored(ticket.priority()));
        map.put("created", timeFormatter.format(ticket.createdAt()));
        map.put("updated", timeFormatter.format(ticket.updatedAt()));
        map.put("closed", timeFormatter.format(ticket.closedAt()));
        map.put("closed_by", safe(ticket.closedByName()));
        map.put("close_reason", safe(ticket.closeReason()));
        map.put("assigned",
                ticket.assignedToName() == null ? "Не назначен" : safe(ticket.assignedToName()));
        map.put("rating", ratingDisplay(ticket.rating()));
        map.put("rating_value", String.valueOf(ticket.rating()));
        map.put("admin", safe(ratingTargetName(ticket)));
        map.put("status_colored", statusColored(ticket));
        map.put("location", location == null ? "Неизвестно" : safe(location.compact()));
        map.put("first_message", first == null ? "" : TextSanitizer.clean(first.text()));
        map.put("last_message", last == null ? "" : TextSanitizer.clean(last.text()));
        map.put("messages_count", String.valueOf(ticket.messages().size()));
        map.put("notes_count", String.valueOf(ticket.internalNotes().size()));
        return map;
    }

    public String ratingDisplay(int rating) {
        if (rating <= 0) {
            return "<dark_gray>Не оценен</dark_gray>";
        }
        StringBuilder result = new StringBuilder();
        for (int index = 1; index <= 5; index++) {
            result.append(index <= rating ? "<yellow>★</yellow>" : "<dark_gray>☆</dark_gray>");
        }
        return result.toString();
    }

    public Map<String, String> adminStats(String name) {
        UUID uuid = resolveUuid(name);
        int assigned = 0;
        int closed = 0;
        int rated = 0;
        double ratingSum = 0.0;
        for (Ticket ticket : storage.allTickets()) {
            boolean isAssigned = matchesAdmin(ticket.assignedToUuid(), ticket.assignedToName(), uuid, name);
            boolean closedByAdmin = ticket.closedByUuid() != null
                    && matchesAdmin(ticket.closedByUuid(), ticket.closedByName(), uuid, name)
                    && !ticket.closedByUuid().equals(ticket.ownerUuid());
            if (isAssigned) {
                assigned++;
            }
            if (closedByAdmin) {
                closed++;
            }
            if (ticket.rating() > 0 && (isAssigned || closedByAdmin)) {
                rated++;
                ratingSum += ticket.rating();
            }
        }
        HashMap<String, String> map = new HashMap<>();
        map.put("admin", safe(name));
        map.put("assigned", String.valueOf(assigned));
        map.put("closed", String.valueOf(closed));
        map.put("rated", String.valueOf(rated));
        map.put("avg", rated == 0 ? "—" : String.format(java.util.Locale.ROOT, "%.1f", ratingSum / rated));
        return map;
    }

    public List<Map<String, String>> adminHistory(String name, int limit) {
        UUID uuid = resolveUuid(name);
        List<Ticket> handled = storage.allTickets().stream()
                .filter(ticket -> matchesAdmin(ticket.assignedToUuid(), ticket.assignedToName(), uuid, name)
                        || matchesAdmin(ticket.closedByUuid(), ticket.closedByName(), uuid, name))
                .sorted((left, right) -> Long.compare(right.updatedAt(), left.updatedAt()))
                .limit(limit)
                .toList();
        List<Map<String, String>> result = new java.util.ArrayList<>();
        for (Ticket ticket : handled) {
            HashMap<String, String> map = new HashMap<>(placeholders(ticket));
            map.put("handled_by", safe(name));
            result.add(map);
        }
        return result;
    }

    public List<Map<String, String>> topAdmins(int limit) {
        HashMap<String, int[]> counts = new HashMap<>();
        HashMap<String, double[]> sums = new HashMap<>();
        HashMap<String, String> names = new HashMap<>();
        for (Ticket ticket : storage.allTickets()) {
            if (ticket.rating() <= 0) {
                continue;
            }
            String admin = ratingTargetName(ticket);
            if (admin == null) {
                continue;
            }
            String key = admin.toLowerCase(Locale.ROOT);
            names.putIfAbsent(key, admin);
            counts.computeIfAbsent(key, ignored -> new int[1])[0]++;
            double[] sum = sums.computeIfAbsent(key, ignored -> new double[2]);
            sum[0] += ticket.rating();
            sum[1] = counts.get(key)[0];
        }
        List<Map<String, String>> result = new java.util.ArrayList<>();
        sums.entrySet().stream()
                .sorted((left, right) -> Double.compare(right.getValue()[0] / right.getValue()[1],
                        left.getValue()[0] / left.getValue()[1]))
                .limit(limit)
                .forEach(entry -> {
                    double[] sum = entry.getValue();
                    HashMap<String, String> map = new HashMap<>();
                    map.put("admin", safe(names.getOrDefault(entry.getKey(), entry.getKey())));
                    map.put("rated", String.valueOf((int) sum[1]));
                    map.put("avg", String.format(java.util.Locale.ROOT, "%.1f", sum[0] / sum[1]));
                    map.put("rating", ratingDisplay((int) Math.round(sum[0] / sum[1])));
                    result.add(map);
                });
        return result;
    }

    public java.io.File exportTickets(String format) throws java.io.IOException {
        java.io.File directory = storage.exportsDirectory();
        String stamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());
        boolean csv = !"txt".equalsIgnoreCase(format);
        java.io.File file = new java.io.File(directory, "tickets-" + stamp + (csv ? ".csv" : ".txt"));
        try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
                new java.io.OutputStreamWriter(new java.io.FileOutputStream(file),
                        java.nio.charset.StandardCharsets.UTF_8))) {
            if (csv) {
                writer.write("id,owner,category,priority,status,assigned,rating,created,updated,closed_by,close_reason,text");
                writer.newLine();
            }
            for (Ticket ticket : storage.allTickets()) {
                if (csv) {
                    writer.write(String.join(",",
                            String.valueOf(ticket.id()),
                            csvCell(ticket.ownerName()),
                            csvCell(ticket.categoryId()),
                            String.valueOf(ticket.priority()),
                            ticket.status().name(),
                            csvCell(ticket.assignedToName()),
                            String.valueOf(ticket.rating()),
                            String.valueOf(ticket.createdAt()),
                            String.valueOf(ticket.updatedAt()),
                            csvCell(ticket.closedByName()),
                            csvCell(ticket.closeReason()),
                            csvCell(ticket.firstMessage() == null ? "" : ticket.firstMessage().text())));
                    writer.newLine();
                } else {
                    writer.write("#" + ticket.id() + " | " + ticket.ownerName() + " | " + ticket.categoryId()
                            + " | priority " + ticket.priority() + " | " + ticket.status().name()
                            + " | rating " + ticket.rating());
                    writer.newLine();
                    for (TicketMessage message : ticket.messages()) {
                        writer.write("   " + (message.internal() ? "[note] " : "")
                                + message.authorName() + ": " + message.text());
                        writer.newLine();
                    }
                    writer.newLine();
                }
            }
        }
        return file;
    }

    private String csvCell(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replace("\"", "\"\"");
        return "\"" + cleaned + "\"";
    }

    public Map<String, String> globalStats() {
        long rated = 0;
        double ratingSum = 0.0;
        for (Ticket ticket : storage.allTickets()) {
            if (ticket.rating() > 0) {
                rated++;
                ratingSum += ticket.rating();
            }
        }
        HashMap<String, String> map = new HashMap<>();
        map.put("open", String.valueOf(storage.countByStatus(TicketStatus.OPEN)));
        map.put("in_progress", String.valueOf(storage.countByStatus(TicketStatus.IN_PROGRESS)));
        map.put("active", String.valueOf(storage.countByStatus(TicketStatus.OPEN)
                + storage.countByStatus(TicketStatus.IN_PROGRESS)));
        map.put("closed", String.valueOf(storage.countByStatus(TicketStatus.CLOSED)));
        map.put("deleted", String.valueOf(storage.countByStatus(TicketStatus.DELETED)));
        map.put("rated", String.valueOf(rated));
        map.put("avg", rated == 0 ? "—" : String.format(java.util.Locale.ROOT, "%.1f", ratingSum / rated));
        return map;
    }

    public Map<String, String> personalStats(UUID ownerUuid) {
        List<Ticket> own = storage.ownTickets(ownerUuid, true, true);
        long open = own.stream().filter(ticket -> ticket.status() == TicketStatus.OPEN).count();
        long inProgress = own.stream().filter(ticket -> ticket.status() == TicketStatus.IN_PROGRESS).count();
        long closed = own.stream().filter(ticket -> ticket.status() == TicketStatus.CLOSED).count();
        long deleted = own.stream().filter(ticket -> ticket.status() == TicketStatus.DELETED).count();
        long rated = own.stream().filter(ticket -> ticket.rating() > 0).count();
        HashMap<String, String> map = new HashMap<>();
        map.put("open", String.valueOf(open));
        map.put("in_progress", String.valueOf(inProgress));
        map.put("closed", String.valueOf(closed));
        map.put("deleted", String.valueOf(deleted));
        map.put("rated", String.valueOf(rated));
        return map;
    }

    private String displayStatus(Ticket ticket) {
        return ticket.status().displayName();
    }

    private String statusColored(Ticket ticket) {
        return switch (ticket.status()) {
            case OPEN -> "<green>Открыт</green>";
            case IN_PROGRESS -> "<aqua>В работе</aqua>";
            case CLOSED -> "<gray>Закрыт</gray>";
            case DELETED -> "<red>Удален</red>";
        };
    }

    private String priorityName(int priority) {
        return switch (priority) {
            case 1 -> "низкая";
            case 2 -> "обычная";
            case 3 -> "средняя";
            case 4 -> "высокая";
            case 5 -> "срочная";
            default -> "неизвестная";
        };
    }

    private String priorityColored(int priority) {
        return switch (priority) {
            case 1 -> "<gray>" + priorityName(1) + " (1)</gray>";
            case 2 -> "<green>" + priorityName(2) + " (2)</green>";
            case 3 -> "<yellow>" + priorityName(3) + " (3)</yellow>";
            case 4 -> "<gold>" + priorityName(4) + " (4)</gold>";
            case 5 -> "<red>" + priorityName(5) + " (5)</red>";
            default -> "<white>" + priorityName(priority) + "</white>";
        };
    }

    private boolean matchesAdmin(UUID adminUuid, String adminName, UUID uuid, String name) {
        if (adminUuid != null && uuid != null && adminUuid.equals(uuid)) {
            return true;
        }
        return adminName != null && adminName.equalsIgnoreCase(name);
    }

    private UUID resolveUuid(String name) {
        try {
            return Bukkit.getOfflinePlayer(name).getUniqueId();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void notifyStaff(String messagePath, Map<String, String> placeholders, String permission) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission(permission) && !online.hasPermission("itickets.staff")) {
                continue;
            }
            messages.send(online, messagePath, placeholders);
        }
    }

    private void notifyOwner(Ticket ticket, String onlinePath, String offlinePath) {
        Player owner = Bukkit.getPlayer(ticket.ownerUuid());
        if (owner != null && owner.isOnline()) {
            messages.send(owner, onlinePath, placeholders(ticket));
            return;
        }
        storage.addPendingNotification(ticket.ownerUuid(), messages.text(offlinePath, placeholders(ticket)));
    }

    private String safe(String input) {
        return TextSanitizer.clean(input);
    }
}
