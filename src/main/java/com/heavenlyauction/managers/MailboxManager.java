package com.heavenlyauction.managers;

import com.heavenlyauction.HeavenlyAuction;
import com.heavenlyauction.database.model.MailboxItem;
import com.heavenlyauction.utils.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class MailboxManager {
    private final HeavenlyAuction plugin;

    public MailboxManager(HeavenlyAuction plugin) {
        this.plugin = plugin;
    }

    public void addItem(UUID ownerUuid, ItemStack item, String reason) {
        if (item == null) return;
        String b64 = ItemSerializer.toBase64(item);
        long now = System.currentTimeMillis();

        plugin.getDatabaseManager().executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO mailbox (owner_uuid, type, item_data, amount, currency, reason, created_at, claimed) " +
                            "VALUES (?, 'ITEM', ?, 0, NULL, ?, ?, 0);")) {
                ps.setString(1, ownerUuid.toString());
                ps.setString(2, b64);
                ps.setString(3, reason);
                ps.setLong(4, now);
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void addMoney(UUID ownerUuid, double amount, String currency, String reason) {
        if (amount <= 0) return;
        long now = System.currentTimeMillis();

        plugin.getDatabaseManager().executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO mailbox (owner_uuid, type, item_data, amount, currency, reason, created_at, claimed) " +
                            "VALUES (?, 'MONEY', NULL, ?, ?, ?, ?, 0);")) {
                ps.setString(1, ownerUuid.toString());
                ps.setDouble(2, amount);
                ps.setString(3, currency.toLowerCase());
                ps.setString(4, reason);
                ps.setLong(5, now);
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void getUnclaimedItems(UUID ownerUuid, Consumer<List<MailboxItem>> callback) {
        plugin.getDatabaseManager().executeAsync(conn -> {
            List<MailboxItem> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM mailbox WHERE owner_uuid = ? AND claimed = 0 ORDER BY created_at DESC;")) {
                ps.setString(1, ownerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long id = rs.getLong("id");
                        String type = rs.getString("type");
                        String itemData = rs.getString("item_data");
                        double amount = rs.getDouble("amount");
                        String currency = rs.getString("currency");
                        String reason = rs.getString("reason");
                        long createdAt = rs.getLong("created_at");

                        ItemStack item = null;
                        if ("ITEM".equalsIgnoreCase(type) && itemData != null && !itemData.isEmpty()) {
                            try {
                                item = ItemSerializer.fromBase64(itemData);
                            } catch (Exception ignored) {}
                        }

                        list.add(new MailboxItem(id, ownerUuid, type, item, amount, currency, reason, createdAt));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(list));
        });
    }

    public void claimItem(Player player, long entryId, Consumer<Boolean> callback) {
        getUnclaimedItems(player.getUniqueId(), items -> {
            for (MailboxItem item : items) {
                if (item.getId() == entryId) {
                    claimEntry(player, item, callback);
                    return;
                }
            }
            callback.accept(false);
        });
    }

    public void claimEntry(Player player, MailboxItem entry, Consumer<Boolean> callback) {
        plugin.getDatabaseManager().executeAsync(conn -> {
            boolean success = false;
            try {
                conn.setAutoCommit(false);

                // Check still unclaimed
                boolean exists = false;
                try (PreparedStatement psCheck = conn.prepareStatement("SELECT id FROM mailbox WHERE id = ? AND claimed = 0;")) {
                    psCheck.setLong(1, entry.getId());
                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (rs.next()) exists = true;
                    }
                }

                if (exists) {
                    if (entry.isItem()) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            ItemStack item = entry.getItem();
                            if (item != null) {
                                if (player.getInventory().firstEmpty() == -1) {
                                    player.sendMessage(plugin.getLanguageManager().getMessage("messages.inventory-full"));
                                    callback.accept(false);
                                    return;
                                }
                                player.getInventory().addItem(item);
                                player.sendMessage(plugin.getLanguageManager().getMessage("messages.mailbox.claimed-item",
                                        "%item%", item.getItemMeta() != null && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name()));

                                plugin.getDatabaseManager().executeAsync(c -> {
                                    try (PreparedStatement psMark = c.prepareStatement("UPDATE mailbox SET claimed = 1 WHERE id = ?;")) {
                                        psMark.setLong(1, entry.getId());
                                        psMark.executeUpdate();
                                    } catch (Exception e) { e.printStackTrace(); }
                                });
                                callback.accept(true);
                            }
                        });
                        conn.setAutoCommit(true);
                        return;
                    } else if (entry.isMoney()) {
                        // Deposit money
                        plugin.getEconomyManager().deposit(player.getUniqueId(), entry.getAmount(), entry.getCurrency());
                        try (PreparedStatement psMark = conn.prepareStatement("UPDATE mailbox SET claimed = 1 WHERE id = ?;")) {
                            psMark.setLong(1, entry.getId());
                            psMark.executeUpdate();
                        }
                        conn.commit();
                        success = true;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(plugin.getLanguageManager().getMessage("messages.mailbox.claimed-money",
                                    "%amount%", plugin.getEconomyManager().formatAmount(entry.getAmount()),
                                    "%currency%", plugin.getEconomyManager().getCurrencyName(entry.getCurrency())));
                        });
                    }
                }
                conn.setAutoCommit(true);
            } catch (Exception e) {
                try { conn.rollback(); conn.setAutoCommit(true); } catch (Exception ignored) {}
                e.printStackTrace();
            }
            final boolean finalSuccess = success;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalSuccess));
        });
    }
}
