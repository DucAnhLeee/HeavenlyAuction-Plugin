package com.heavenlyauction.managers;

import com.heavenlyauction.HeavenlyAuction;
import com.heavenlyauction.config.ConfigManager;
import com.heavenlyauction.database.model.AuctionItem;
import com.heavenlyauction.utils.ColorUtil;
import com.heavenlyauction.utils.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class AuctionManager {
    private final HeavenlyAuction plugin;
    private final Map<Long, AuctionItem> activeAuctions = new ConcurrentHashMap<>();
    private final ReentrantLock auctionLock = new ReentrantLock();
    private BukkitTask auctionTask;
    private final Random random = new Random();
    private long lastServerAuctionTime = 0;

    public AuctionManager(HeavenlyAuction plugin) {
        this.plugin = plugin;
    }

    public void load() {
        activeAuctions.clear();
        plugin.getDatabaseManager().executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM auctions ORDER BY started_at DESC;");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String sUuid = rs.getString("seller_uuid");
                    UUID sellerUuid = (sUuid != null && !sUuid.isEmpty()) ? UUID.fromString(sUuid) : null;
                    String sellerName = rs.getString("seller_name");
                    String itemData = rs.getString("item_data");
                    double startingPrice = rs.getDouble("starting_price");
                    double minBidStep = rs.getDouble("min_bid_step");
                    double currentBid = rs.getDouble("current_bid");
                    String hUuid = rs.getString("highest_bidder_uuid");
                    UUID highestBidderUuid = (hUuid != null && !hUuid.isEmpty()) ? UUID.fromString(hUuid) : null;
                    String highestBidderName = rs.getString("highest_bidder_name");
                    String currency = rs.getString("currency");
                    boolean isServer = rs.getInt("is_server") == 1;
                    boolean isMystery = rs.getInt("is_mystery") == 1;
                    String mysteryLoreRaw = rs.getString("mystery_lore");
                    List<String> mysteryLore = mysteryLoreRaw != null ? Arrays.asList(mysteryLoreRaw.split(";;")) : new ArrayList<>();
                    long startedAt = rs.getLong("started_at");
                    long expireAt = rs.getLong("expire_at");

                    ItemStack item = ItemSerializer.fromBase64(itemData);
                    if (item != null) {
                        AuctionItem ai = new AuctionItem(id, sellerUuid, sellerName, item, startingPrice, minBidStep,
                                currentBid, highestBidderUuid, highestBidderName, currency, isServer, isMystery, mysteryLore, startedAt, expireAt);
                        activeAuctions.put(id, ai);
                    }
                }
                plugin.getLogger().info("Đã nạp " + activeAuctions.size() + " phiên Thiên Bảo Đấu Giá!");
            } catch (Exception e) {
                plugin.getLogger().severe("Lỗi nạp dữ liệu auctions: " + e.getMessage());
                plugin.getDatabaseManager().migrateSchema();
            }
        });

        if (auctionTask != null) auctionTask.cancel();
        auctionTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAuctions, 20L * 5L, 20L * 10L);
    }

    public void stop() {
        if (auctionTask != null) {
            auctionTask.cancel();
            auctionTask = null;
        }
    }

    public List<AuctionItem> getActiveAuctions() {
        List<AuctionItem> list = new ArrayList<>(activeAuctions.values());
        list.sort((a, b) -> Long.compare(a.getExpireAt(), b.getExpireAt()));
        return list;
    }

    public void createPlayerAuction(Player seller, ItemStack item, double startingPrice, double minBidStep, long durationSeconds, String currency, Consumer<Boolean> callback) {
        if (item == null || startingPrice <= 0 || minBidStep <= 0) {
            callback.accept(false);
            return;
        }

        String b64 = ItemSerializer.toBase64(item);
        long now = System.currentTimeMillis();
        long expire = now + (durationSeconds * 1000L);
        UUID sellerUuid = seller.getUniqueId();
        String sellerName = seller.getName();

        plugin.getDatabaseManager().executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO auctions (seller_uuid, seller_name, item_data, starting_price, min_bid_step, current_bid, currency, is_server, is_mystery, started_at, expire_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, ?, ?);")) {
                ps.setString(1, sellerUuid.toString());
                ps.setString(2, sellerName);
                ps.setString(3, b64);
                ps.setDouble(4, startingPrice);
                ps.setDouble(5, minBidStep);
                ps.setDouble(6, startingPrice);
                ps.setString(7, currency.toLowerCase());
                ps.setLong(8, now);
                ps.setLong(9, expire);
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

                AuctionItem ai = new AuctionItem(id, sellerUuid, sellerName, item.clone(), startingPrice, minBidStep,
                        startingPrice, null, null, currency.toLowerCase(), false, false, new ArrayList<>(), now, expire);
                activeAuctions.put(id, ai);

                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(true));
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }

    public void startServerScheduledAuction(boolean forceMystery) {
        List<ConfigManager.ServerAuctionPoolItem> pool = plugin.getConfigManager().getServerAuctionPool();
        if (pool.isEmpty()) return;

        ConfigManager.ServerAuctionPoolItem chosen = pool.get(random.nextInt(pool.size()));
        boolean isMystery = forceMystery || chosen.isMystery();

        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        if (currentTime.equals(plugin.getConfigManager().getServerAuctionMysteryTime())) {
            isMystery = true;
        }

        ItemStack item = chosen.getItem();
        double startingPrice = chosen.getStartingPrice();
        double minBidStep = chosen.getMinBidStep();
        String currency = chosen.getCurrency();
        long now = System.currentTimeMillis();
        long expire = now + (plugin.getConfigManager().getServerAuctionDurationSeconds() * 1000L);
        List<String> mysteryLore = plugin.getConfigManager().getMysteryItemLore();
        String mysteryLoreSerialized = String.join(";;", mysteryLore);

        String b64 = ItemSerializer.toBase64(item);
        final boolean finalMystery = isMystery;

        plugin.getDatabaseManager().executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO auctions (seller_uuid, seller_name, item_data, starting_price, min_bid_step, current_bid, currency, is_server, is_mystery, mystery_lore, started_at, expire_at) " +
                            "VALUES (NULL, 'Hệ Thống', ?, ?, ?, ?, ?, 1, ?, ?, ?, ?);")) {
                ps.setString(1, b64);
                ps.setDouble(2, startingPrice);
                ps.setDouble(3, minBidStep);
                ps.setDouble(4, startingPrice);
                ps.setString(5, currency);
                ps.setInt(6, finalMystery ? 1 : 0);
                ps.setString(7, mysteryLoreSerialized);
                ps.setLong(8, now);
                ps.setLong(9, expire);
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

                AuctionItem ai = new AuctionItem(id, null, "Hệ Thống", item, startingPrice, minBidStep,
                        startingPrice, null, null, currency, true, finalMystery, mysteryLore, now, expire);
                activeAuctions.put(id, ai);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (finalMystery) {
                        Bukkit.broadcastMessage(plugin.getLanguageManager().getMessage("messages.auction.server-mystery-start"));
                    } else {
                        String iName = item.getItemMeta() != null && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
                        Bukkit.broadcastMessage(plugin.getLanguageManager().getMessage("messages.auction.server-start",
                                "%item%", iName,
                                "%price%", plugin.getEconomyManager().formatAmount(startingPrice),
                                "%currency%", plugin.getEconomyManager().getCurrencyName(currency)));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void placeBid(Player bidder, long auctionId, double bidAmount) {
        auctionLock.lock();
        try {
            AuctionItem auction = activeAuctions.get(auctionId);
            if (auction == null) {
                bidder.sendMessage(plugin.getLanguageManager().getMessage("messages.target-not-found"));
                return;
            }

            if (auction.isExpired()) {
                bidder.sendMessage(plugin.getLanguageManager().getMessage("messages.target-not-found"));
                return;
            }

            if (auction.getSellerUuid() != null && auction.getSellerUuid().equals(bidder.getUniqueId())) {
                bidder.sendMessage(ColorUtil.colorize("&cĐạo hữu không thể tự đấu giá bảo vật của chính mình!"));
                return;
            }

            double minNextBid = auction.getNextMinimumBid();
            if (bidAmount < minNextBid) {
                bidder.sendMessage(plugin.getLanguageManager().getMessage("messages.auction.bid-too-low",
                        "%min%", plugin.getEconomyManager().formatAmount(minNextBid),
                        "%currency%", plugin.getEconomyManager().getCurrencyName(auction.getCurrency())));
                return;
            }

            if (!plugin.getEconomyManager().has(bidder.getUniqueId(), bidAmount, auction.getCurrency())) {
                bidder.sendMessage(plugin.getLanguageManager().getMessage("messages.not-enough-money",
                        "%price%", plugin.getEconomyManager().formatAmount(bidAmount),
                        "%currency%", plugin.getEconomyManager().getCurrencyName(auction.getCurrency())));
                return;
            }

            // Withdraw from new bidder
            if (!plugin.getEconomyManager().withdraw(bidder.getUniqueId(), bidAmount, auction.getCurrency())) {
                bidder.sendMessage(plugin.getLanguageManager().getMessage("messages.not-enough-money",
                        "%price%", plugin.getEconomyManager().formatAmount(bidAmount),
                        "%currency%", plugin.getEconomyManager().getCurrencyName(auction.getCurrency())));
                return;
            }

            // Refund previous highest bidder immediately!
            UUID prevBidderUuid = auction.getHighestBidderUuid();
            double prevBid = auction.getCurrentBid();
            if (prevBidderUuid != null) {
                plugin.getEconomyManager().deposit(prevBidderUuid, prevBid, auction.getCurrency());
                Player prevOnline = Bukkit.getPlayer(prevBidderUuid);
                if (prevOnline != null && prevOnline.isOnline()) {
                    prevOnline.sendMessage(plugin.getLanguageManager().getMessage("messages.auction.outbid-notify",
                            "%bid%", plugin.getEconomyManager().formatAmount(bidAmount),
                            "%currency%", plugin.getEconomyManager().getCurrencyName(auction.getCurrency())));
                }
            }

            // Update auction
            auction.setCurrentBid(bidAmount);
            auction.setHighestBidderUuid(bidder.getUniqueId());
            auction.setHighestBidderName(bidder.getName());

            // Anti-sniping: extend if remaining time < 30s
            long timeLeft = auction.getExpireAt() - System.currentTimeMillis();
            if (timeLeft < 30000L) {
                auction.setExpireAt(System.currentTimeMillis() + 30000L);
            }

            bidder.sendMessage(plugin.getLanguageManager().getMessage("messages.auction.bid-success",
                    "%bid%", plugin.getEconomyManager().formatAmount(bidAmount),
                    "%currency%", plugin.getEconomyManager().getCurrencyName(auction.getCurrency())));

            // Save to DB
            long currentExpire = auction.getExpireAt();
            plugin.getDatabaseManager().executeAsync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE auctions SET current_bid = ?, highest_bidder_uuid = ?, highest_bidder_name = ?, expire_at = ? WHERE id = ?;")) {
                    ps.setDouble(1, bidAmount);
                    ps.setString(2, bidder.getUniqueId().toString());
                    ps.setString(3, bidder.getName());
                    ps.setLong(4, currentExpire);
                    ps.setLong(5, auctionId);
                    ps.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } finally {
            auctionLock.unlock();
        }
    }

    private void tickAuctions() {
        long now = System.currentTimeMillis();

        if (plugin.getConfigManager().isServerAuctionEnabled()) {
            long intervalMillis = plugin.getConfigManager().getServerAuctionIntervalMinutes() * 60000L;
            if (now - lastServerAuctionTime >= intervalMillis) {
                lastServerAuctionTime = now;
                startServerScheduledAuction(false);
            }
        }

        List<AuctionItem> expired = new ArrayList<>();
        for (AuctionItem ai : activeAuctions.values()) {
            if (now >= ai.getExpireAt()) {
                expired.add(ai);
            }
        }

        for (AuctionItem ai : expired) {
            endAuction(ai);
        }
    }

    private void endAuction(AuctionItem ai) {
        auctionLock.lock();
        try {
            activeAuctions.remove(ai.getId());

            String itemName = ai.getItem().getItemMeta() != null && ai.getItem().getItemMeta().hasDisplayName() ?
                    ai.getItem().getItemMeta().getDisplayName() : ai.getItem().getType().name();

            if (ai.getHighestBidderUuid() != null) {
                UUID winnerUuid = ai.getHighestBidderUuid();
                double winningBid = ai.getCurrentBid();

                plugin.getMailboxManager().addItem(winnerUuid, ai.getItem(), "Thắng Thiên Bảo Đấu Giá: " + itemName);

                Player winnerOnline = Bukkit.getPlayer(winnerUuid);
                if (winnerOnline != null && winnerOnline.isOnline()) {
                    winnerOnline.sendMessage(plugin.getLanguageManager().getMessage("messages.auction.end-winner",
                            "%item%", itemName,
                            "%price%", plugin.getEconomyManager().formatAmount(winningBid),
                            "%currency%", plugin.getEconomyManager().getCurrencyName(ai.getCurrency())));
                }

                if (!ai.isServer() && ai.getSellerUuid() != null) {
                    double taxPercent = plugin.getConfigManager().getCurrencyTaxPercent(ai.getCurrency());
                    double taxAmount = winningBid * (taxPercent / 100.0);
                    double netAmount = winningBid - taxAmount;

                    plugin.getMailboxManager().addMoney(ai.getSellerUuid(), netAmount, ai.getCurrency(),
                            "Tiền thắng đấu giá bảo vật (Thuế sàn: " + taxPercent + "%)");

                    Player sellerOnline = Bukkit.getPlayer(ai.getSellerUuid());
                    if (sellerOnline != null && sellerOnline.isOnline()) {
                        sellerOnline.sendMessage(plugin.getLanguageManager().getMessage("messages.auction.end-seller",
                                "%winner%", ai.getHighestBidderName(),
                                "%item%", itemName,
                                "%price%", plugin.getEconomyManager().formatAmount(netAmount),
                                "%currency%", plugin.getEconomyManager().getCurrencyName(ai.getCurrency())));
                    }
                }
            } else {
                if (!ai.isServer() && ai.getSellerUuid() != null) {
                    plugin.getMailboxManager().addItem(ai.getSellerUuid(), ai.getItem(), "Bảo vật đấu giá không có người ra giá");
                    Player sellerOnline = Bukkit.getPlayer(ai.getSellerUuid());
                    if (sellerOnline != null && sellerOnline.isOnline()) {
                        sellerOnline.sendMessage(plugin.getLanguageManager().getMessage("messages.auction.end-no-bids"));
                    }
                }
            }

            plugin.getDatabaseManager().executeAsync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM auctions WHERE id = ?;")) {
                    ps.setLong(1, ai.getId());
                    ps.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } finally {
            auctionLock.unlock();
        }
    }
}
