package com.heavenlyauction.managers;

import com.heavenlyauction.HeavenlyAuction;
import com.heavenlyauction.database.model.BountyItem;
import com.heavenlyauction.utils.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class BountyManager {
    private final HeavenlyAuction plugin;
    private final Map<Long, BountyItem> activeBounties = new ConcurrentHashMap<>();
    private final ReentrantLock bountyLock = new ReentrantLock();
    private BukkitTask expireTask;

    public BountyManager(HeavenlyAuction plugin) {
        this.plugin = plugin;
    }

    public void load() {
        activeBounties.clear();
        plugin.getDatabaseManager().executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM bounties ORDER BY created_at DESC;");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    UUID creatorUuid = UUID.fromString(rs.getString("creator_uuid"));
                    String creatorName = rs.getString("creator_name");
                    String protoData = rs.getString("prototype_data");
                    int reqAmount = rs.getInt("required_amount");
                    double reward = rs.getDouble("reward_price");
                    String currency = rs.getString("reward_currency");
                    long createdAt = rs.getLong("created_at");
                    long expireAt = rs.getLong("expire_at");

                    ItemStack proto = ItemSerializer.fromBase64(protoData);
                    if (proto != null) {
                        BountyItem bi = new BountyItem(id, creatorUuid, creatorName, proto, reqAmount, reward, currency, createdAt, expireAt);
                        activeBounties.put(id, bi);
                    }
                }
                plugin.getLogger().info("Đã nạp " + activeBounties.size() + " lệnh Huyền Thưởng!");
            } catch (Exception e) {
                plugin.getLogger().severe("Lỗi nạp dữ liệu bounties: " + e.getMessage());
                plugin.getDatabaseManager().migrateSchema();
            }
        });

        if (expireTask != null) expireTask.cancel();
        expireTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkExpirations, 20L * 30L, 20L * 60L);
    }

    public void stop() {
        if (expireTask != null) {
            expireTask.cancel();
            expireTask = null;
        }
    }

    public List<BountyItem> getActiveBounties() {
        List<BountyItem> list = new ArrayList<>(activeBounties.values());
        list.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        return list;
    }

    public List<BountyItem> getPlayerBounties(UUID playerUuid) {
        List<BountyItem> list = new ArrayList<>();
        for (BountyItem bi : activeBounties.values()) {
            if (bi.getCreatorUuid().equals(playerUuid)) {
                list.add(bi);
            }
        }
        return list;
    }

    public void createBounty(Player creator, ItemStack prototype, int requiredAmount, double rewardPrice, String currency, Consumer<Boolean> callback) {
        if (prototype == null || requiredAmount <= 0 || rewardPrice <= 0) {
            callback.accept(false);
            return;
        }

        int maxBounties = plugin.getConfigManager().getBountyMaxPerPlayer();
        int currentCount = getPlayerBounties(creator.getUniqueId()).size();
        if (currentCount >= maxBounties) {
            creator.sendMessage(plugin.getLanguageManager().getMessage("messages.bounty.treo-limit-reached", "%limit%", String.valueOf(maxBounties)));
            callback.accept(false);
            return;
        }

        // Withdraw reward upfront
        if (!plugin.getEconomyManager().withdraw(creator.getUniqueId(), rewardPrice, currency)) {
            creator.sendMessage(plugin.getLanguageManager().getMessage("messages.not-enough-money",
                    "%price%", plugin.getEconomyManager().formatAmount(rewardPrice),
                    "%currency%", plugin.getEconomyManager().getCurrencyName(currency)));
            callback.accept(false);
            return;
        }

        String b64 = ItemSerializer.toBase64(prototype);
        long now = System.currentTimeMillis();
        long expire = now + (plugin.getConfigManager().getBountyExpireSeconds() * 1000L);
        UUID creatorUuid = creator.getUniqueId();
        String creatorName = creator.getName();

        plugin.getDatabaseManager().executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO bounties (creator_uuid, creator_name, prototype_data, required_amount, reward_price, reward_currency, created_at, expire_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?);")) {
                ps.setString(1, creatorUuid.toString());
                ps.setString(2, creatorName);
                ps.setString(3, b64);
                ps.setInt(4, requiredAmount);
                ps.setDouble(5, rewardPrice);
                ps.setString(6, currency.toLowerCase());
                ps.setLong(7, now);
                ps.setLong(8, expire);
                ps.executeUpdate();

                long id;
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid();")) {
                    if (rs.next()) {
                        id = rs.getLong(1);
                    } else {
                        id = System.currentTimeMillis();
                    }
                }

                BountyItem bi = new BountyItem(id, creatorUuid, creatorName, prototype.clone(), requiredAmount, rewardPrice, currency.toLowerCase(), now, expire);
                activeBounties.put(id, bi);

                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(true));
            } catch (Exception e) {
                // Refund if failed
                plugin.getEconomyManager().deposit(creatorUuid, rewardPrice, currency);
                e.printStackTrace();
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }

    public void fulfillBounty(Player fulfiller, long bountyId) {
        bountyLock.lock();
        try {
            BountyItem bounty = activeBounties.get(bountyId);
            if (bounty == null) {
                fulfiller.sendMessage(plugin.getLanguageManager().getMessage("messages.target-not-found"));
                return;
            }

            if (bounty.getCreatorUuid().equals(fulfiller.getUniqueId())) {
                fulfiller.sendMessage(plugin.getLanguageManager().getMessage("messages.bounty.cannot-fulfill-own"));
                return;
            }

            ItemStack prototype = bounty.getPrototypeItem();
            int required = bounty.getRequiredAmount();

            // Count matching items in inventory
            int totalFound = 0;
            ItemStack[] contents = fulfiller.getInventory().getContents();
            for (ItemStack slot : contents) {
                if (slot != null && ItemSerializer.isMatching(prototype, slot)) {
                    totalFound += slot.getAmount();
                }
            }

            String itemName = prototype.getItemMeta() != null && prototype.getItemMeta().hasDisplayName() ? prototype.getItemMeta().getDisplayName() : prototype.getType().name();

            if (totalFound < required) {
                fulfiller.sendMessage(plugin.getLanguageManager().getMessage("messages.bounty.not-enough-items",
                        "%required%", String.valueOf(required),
                        "%item%", itemName));
                return;
            }

            // Remove items from fulfiller's inventory
            int toRemove = required;
            for (int i = 0; i < contents.length; i++) {
                ItemStack slot = contents[i];
                if (slot != null && ItemSerializer.isMatching(prototype, slot)) {
                    if (slot.getAmount() <= toRemove) {
                        toRemove -= slot.getAmount();
                        fulfiller.getInventory().setItem(i, null);
                    } else {
                        slot.setAmount(slot.getAmount() - toRemove);
                        toRemove = 0;
                    }
                    if (toRemove <= 0) break;
                }
            }
            fulfiller.updateInventory();

            // Remove bounty from active map
            activeBounties.remove(bountyId);

            // Deposit reward to fulfiller
            plugin.getEconomyManager().deposit(fulfiller.getUniqueId(), bounty.getRewardPrice(), bounty.getCurrency());
            fulfiller.sendMessage(plugin.getLanguageManager().getMessage("messages.bounty.fulfill-success",
                    "%reward%", plugin.getEconomyManager().formatAmount(bounty.getRewardPrice()),
                    "%currency%", plugin.getEconomyManager().getCurrencyName(bounty.getCurrency())));

            // Deliver gathered items to creator
            ItemStack rewardStack = prototype.clone();
            rewardStack.setAmount(required);
            Player creatorOnline = Bukkit.getPlayer(bounty.getCreatorUuid());
            if (creatorOnline != null && creatorOnline.isOnline() && creatorOnline.getInventory().firstEmpty() != -1) {
                creatorOnline.getInventory().addItem(rewardStack);
                creatorOnline.sendMessage(plugin.getLanguageManager().getMessage("messages.bounty.fulfill-notify-creator",
                        "%fulfiller%", fulfiller.getName(),
                        "%amount%", String.valueOf(required),
                        "%item%", itemName));
            } else {
                plugin.getMailboxManager().addItem(bounty.getCreatorUuid(), rewardStack,
                        "Huyền Thưởng hoàn tất bởi tu sĩ " + fulfiller.getName());
                if (creatorOnline != null && creatorOnline.isOnline()) {
                    creatorOnline.sendMessage(plugin.getLanguageManager().getMessage("messages.bounty.fulfill-notify-creator",
                            "%fulfiller%", fulfiller.getName(),
                            "%amount%", String.valueOf(required),
                            "%item%", itemName));
                }
            }

            // Remove from database
            plugin.getDatabaseManager().executeAsync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM bounties WHERE id = ?;")) {
                    ps.setLong(1, bountyId);
                    ps.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } finally {
            bountyLock.unlock();
        }
    }

    public void cancelBounty(Player player, long bountyId) {
        bountyLock.lock();
        try {
            BountyItem bounty = activeBounties.get(bountyId);
            if (bounty == null) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.target-not-found"));
                return;
            }

            if (!bounty.getCreatorUuid().equals(player.getUniqueId()) && !player.hasPermission("vbl.admin")) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.no-permission"));
                return;
            }

            activeBounties.remove(bountyId);

            // Refund bounty reward to player's mailbox
            plugin.getMailboxManager().addMoney(bounty.getCreatorUuid(), bounty.getRewardPrice(), bounty.getCurrency(),
                    "Hủy lệnh Huyền Thưởng (Hoàn tiền)");

            player.sendMessage(plugin.getLanguageManager().getMessage("messages.bounty.cancel-success",
                    "%reward%", plugin.getEconomyManager().formatAmount(bounty.getRewardPrice()),
                    "%currency%", plugin.getEconomyManager().getCurrencyName(bounty.getCurrency())));

            // Remove from database
            plugin.getDatabaseManager().executeAsync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM bounties WHERE id = ?;")) {
                    ps.setLong(1, bountyId);
                    ps.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } finally {
            bountyLock.unlock();
        }
    }

    private void checkExpirations() {
        long now = System.currentTimeMillis();
        List<BountyItem> expired = new ArrayList<>();
        for (BountyItem item : activeBounties.values()) {
            if (now >= item.getExpireAt()) {
                expired.add(item);
            }
        }

        for (BountyItem item : expired) {
            activeBounties.remove(item.getId());
            // Refund money to creator's mailbox
            plugin.getMailboxManager().addMoney(item.getCreatorUuid(), item.getRewardPrice(), item.getCurrency(),
                    "Lệnh Huyền Thưởng quá hạn niêm yết (Hoàn tiền)");

            plugin.getDatabaseManager().executeAsync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM bounties WHERE id = ?;")) {
                    ps.setLong(1, item.getId());
                    ps.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
