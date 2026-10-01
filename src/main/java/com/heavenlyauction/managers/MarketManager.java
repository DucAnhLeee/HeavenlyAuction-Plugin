package com.heavenlyauction.managers;

import com.heavenlyauction.HeavenlyAuction;
import com.heavenlyauction.database.model.MarketItem;
import com.heavenlyauction.utils.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
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

public class MarketManager {
    private final HeavenlyAuction plugin;
    private final Map<Long, MarketItem> activeItems = new ConcurrentHashMap<>();
    private final ReentrantLock marketLock = new ReentrantLock();
    private BukkitTask expireTask;

    public MarketManager(HeavenlyAuction plugin) {
        this.plugin = plugin;
    }

    public void load() {
        activeItems.clear();
        plugin.getDatabaseManager().executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM market_items ORDER BY listed_at DESC;");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    UUID sellerUuid = UUID.fromString(rs.getString("seller_uuid"));
                    String sellerName = rs.getString("seller_name");
                    String itemData = rs.getString("item_data");
                    double price = rs.getDouble("price");
                    String currency = rs.getString("currency");
                    long listedAt = rs.getLong("listed_at");
                    long expireAt = rs.getLong("expire_at");

                    ItemStack item = ItemSerializer.fromBase64(itemData);
                    if (item != null) {
                        MarketItem mi = new MarketItem(id, sellerUuid, sellerName, item, price, currency, listedAt, expireAt);
                        activeItems.put(id, mi);
                    } else {
                        plugin.getLogger().warning("Không thể nạp vật phẩm id=" + id + " từ SQLite!");
                    }
                }
                plugin.getLogger().info("Đã nạp " + activeItems.size() + " vật phẩm trên sàn Vạn Bảo Lâu!");
            } catch (Exception e) {
                plugin.getLogger().severe("Lỗi khi nạp dữ liệu market_items: " + e.getMessage());
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

    public MarketItem getItem(long id) {
        return activeItems.get(id);
    }

    public List<MarketItem> getActiveItems() {
        List<MarketItem> list = new ArrayList<>(activeItems.values());
        list.sort((a, b) -> Long.compare(b.getListedAt(), a.getListedAt()));
        return list;
    }

    public List<MarketItem> getPlayerItems(UUID playerUuid) {
        List<MarketItem> list = new ArrayList<>();
        for (MarketItem mi : activeItems.values()) {
            if (mi.getSellerUuid().equals(playerUuid)) {
                list.add(mi);
            }
        }
        return list;
    }

    public void addItem(Player seller, ItemStack item, double price, String currency, Consumer<Boolean> callback) {
        if (item == null || price <= 0) {
            callback.accept(false);
            return;
        }

        int maxListings = plugin.getConfigManager().getMarketMaxListingsPerPlayer();
        int currentCount = getPlayerItems(seller.getUniqueId()).size();
        if (currentCount >= maxListings) {
            seller.sendMessage(plugin.getLanguageManager().getMessage("messages.market.sell-limit-reached", "%limit%", String.valueOf(maxListings)));
            callback.accept(false);
            return;
        }

        String b64 = ItemSerializer.toBase64(item);
        if (b64.isEmpty()) {
            seller.sendMessage(plugin.getLanguageManager().getMessage("messages.prefix") + "§cKhông thể mã hóa dữ liệu vật phẩm này!");
            callback.accept(false);
            return;
        }

        long now = System.currentTimeMillis();
        long expire = now + (plugin.getConfigManager().getMarketExpireSeconds() * 1000L);
        UUID sellerUuid = seller.getUniqueId();
        String sellerName = seller.getName();

        plugin.getDatabaseManager().executeAsync(conn -> {
            try {
                insertItem(conn, sellerUuid, sellerName, b64, price, currency, now, expire, item, callback);
            } catch (Exception e) {
                plugin.getLogger().severe("Lỗi khi thêm vật phẩm vào market_items: " + e.getMessage());
                if (e.getMessage() != null && e.getMessage().contains("no column named")) {
                    plugin.getLogger().warning("Phát hiện cấu trúc bảng cũ, tự động nâng cấp schema...");
                    plugin.getDatabaseManager().migrateSchema();
                    try {
                        insertItem(conn, sellerUuid, sellerName, b64, price, currency, now, expire, item, callback);
                        return;
                    } catch (Exception ex) {
                        plugin.getDatabaseManager().recreateTable("market_items");
                    }
                }
                e.printStackTrace();
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }

    private void insertItem(Connection conn, UUID sellerUuid, String sellerName, String b64, double price, String currency,
                            long now, long expire, ItemStack item, Consumer<Boolean> callback) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO market_items (seller_uuid, seller_name, item_data, price, currency, listed_at, expire_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?);")) {
            ps.setString(1, sellerUuid.toString());
            ps.setString(2, sellerName);
            ps.setString(3, b64);
            ps.setDouble(4, price);
            ps.setString(5, currency.toLowerCase());
            ps.setLong(6, now);
            ps.setLong(7, expire);
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

            MarketItem mi = new MarketItem(id, sellerUuid, sellerName, item.clone(), price, currency.toLowerCase(), now, expire);
            activeItems.put(id, mi);

            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(true));
        }
    }

    public void cancelListing(Player seller, long itemId) {
        marketLock.lock();
        try {
            MarketItem item = activeItems.get(itemId);
            if (item == null) {
                seller.sendMessage(plugin.getLanguageManager().getMessage("messages.target-not-found"));
                return;
            }

            if (!item.getSellerUuid().equals(seller.getUniqueId()) && !seller.hasPermission("vbl.admin")) {
                seller.sendMessage(plugin.getLanguageManager().getMessage("messages.no-permission"));
                return;
            }

            activeItems.remove(itemId);

            // Return item to mailbox
            plugin.getMailboxManager().addItem(item.getSellerUuid(), item.getItem(), "Hủy bán bảo vật trên Vạn Bảo Lâu");

            seller.sendMessage(plugin.getLanguageManager().getMessage("messages.market.cancel-success"));

            // Remove from DB
            plugin.getDatabaseManager().executeAsync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM market_items WHERE id = ?;")) {
                    ps.setLong(1, itemId);
                    ps.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } finally {
            marketLock.unlock();
        }
    }

    public void buyItem(Player buyer, long itemId) {
        marketLock.lock();
        try {
            MarketItem item = activeItems.get(itemId);
            if (item == null) {
                buyer.sendMessage(plugin.getLanguageManager().getMessage("messages.target-not-found"));
                return;
            }

            if (item.getSellerUuid().equals(buyer.getUniqueId())) {
                buyer.sendMessage(plugin.getLanguageManager().getMessage("messages.market.cannot-buy-own"));
                return;
            }

            double price = item.getPrice();
            String currency = item.getCurrency();

            if (!plugin.getEconomyManager().has(buyer.getUniqueId(), price, currency)) {
                buyer.sendMessage(plugin.getLanguageManager().getMessage("messages.not-enough-money",
                        "%price%", plugin.getEconomyManager().formatAmount(price),
                        "%currency%", plugin.getEconomyManager().getCurrencyName(currency)));
                return;
            }

            // Withdraw from buyer
            if (!plugin.getEconomyManager().withdraw(buyer.getUniqueId(), price, currency)) {
                buyer.sendMessage(plugin.getLanguageManager().getMessage("messages.not-enough-money",
                        "%price%", plugin.getEconomyManager().formatAmount(price),
                        "%currency%", plugin.getEconomyManager().getCurrencyName(currency)));
                return;
            }

            // Remove from active map immediately
            activeItems.remove(itemId);

            // Calculate tax
            double taxPercent = plugin.getConfigManager().getCurrencyTaxPercent(currency);
            double taxAmount = price * (taxPercent / 100.0);
            double netSellerAmount = price - taxAmount;

            // Deliver money to seller mailbox
            plugin.getMailboxManager().addMoney(item.getSellerUuid(), netSellerAmount, currency,
                    "Tiền bán bảo vật (Thuế sàn: " + taxPercent + "%)");

            // Deliver item to buyer (either inventory or mailbox)
            if (buyer.getInventory().firstEmpty() != -1) {
                buyer.getInventory().addItem(item.getItem());
            } else {
                plugin.getMailboxManager().addItem(buyer.getUniqueId(), item.getItem(), "Bảo vật mua từ Vạn Bảo Lâu (Túi đồ đầy)");
            }

            String itemName = item.getItem().getItemMeta() != null && item.getItem().getItemMeta().hasDisplayName() ?
                    item.getItem().getItemMeta().getDisplayName() : item.getItem().getType().name();

            buyer.sendMessage(plugin.getLanguageManager().getMessage("messages.market.buy-success",
                    "%item%", itemName,
                    "%price%", plugin.getEconomyManager().formatAmount(price),
                    "%currency%", plugin.getEconomyManager().getCurrencyName(currency)));

            // Notify seller if online
            Player sellerOnline = Bukkit.getPlayer(item.getSellerUuid());
            if (sellerOnline != null && sellerOnline.isOnline()) {
                sellerOnline.sendMessage(plugin.getLanguageManager().getMessage("messages.market.sold-notify",
                        "%buyer%", buyer.getName(),
                        "%item%", itemName,
                        "%price%", plugin.getEconomyManager().formatAmount(netSellerAmount),
                        "%currency%", plugin.getEconomyManager().getCurrencyName(currency)));
            }

            // Remove from DB
            plugin.getDatabaseManager().executeAsync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM market_items WHERE id = ?;")) {
                    ps.setLong(1, itemId);
                    ps.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } finally {
            marketLock.unlock();
        }
    }

    private void checkExpirations() {
        long now = System.currentTimeMillis();
        List<MarketItem> expired = new ArrayList<>();
        for (MarketItem item : activeItems.values()) {
            if (now >= item.getExpireAt()) {
                expired.add(item);
            }
        }

        for (MarketItem item : expired) {
            activeItems.remove(item.getId());
            // Move item to seller's mailbox
            plugin.getMailboxManager().addItem(item.getSellerUuid(), item.getItem(), "Bảo vật hết hạn niêm yết trên Vạn Bảo Lâu");

            // Delete from database
            plugin.getDatabaseManager().executeAsync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM market_items WHERE id = ?;")) {
                    ps.setLong(1, item.getId());
                    ps.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
