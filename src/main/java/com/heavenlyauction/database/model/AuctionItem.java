package com.heavenlyauction.database.model;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class AuctionItem {
    private final long id;
    private final UUID sellerUuid; // null for server auction
    private final String sellerName;
    private final ItemStack item;
    private final double startingPrice;
    private final double minBidStep;
    private double currentBid;
    private UUID highestBidderUuid;
    private String highestBidderName;
    private final String currency;
    private final boolean serverAuction;
    private final boolean mystery;
    private final List<String> mysteryLore;
    private final long startedAt;
    private long expireAt;

    public AuctionItem(long id, UUID sellerUuid, String sellerName, ItemStack item, double startingPrice, double minBidStep,
                       double currentBid, UUID highestBidderUuid, String highestBidderName, String currency,
                       boolean serverAuction, boolean mystery, List<String> mysteryLore, long startedAt, long expireAt) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.item = item;
        this.startingPrice = startingPrice;
        this.minBidStep = minBidStep;
        this.currentBid = currentBid;
        this.highestBidderUuid = highestBidderUuid;
        this.highestBidderName = highestBidderName;
        this.currency = currency;
        this.serverAuction = serverAuction;
        this.mystery = mystery;
        this.mysteryLore = mysteryLore;
        this.startedAt = startedAt;
        this.expireAt = expireAt;
    }

    public long getId() { return id; }
    public UUID getSellerUuid() { return sellerUuid; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItem() { return item.clone(); }
    public double getStartingPrice() { return startingPrice; }
    public double getMinBidStep() { return minBidStep; }
    public double getCurrentBid() { return currentBid; }
    public void setCurrentBid(double bid) { this.currentBid = bid; }
    public UUID getHighestBidderUuid() { return highestBidderUuid; }
    public void setHighestBidderUuid(UUID uuid) { this.highestBidderUuid = uuid; }
    public String getHighestBidderName() { return highestBidderName; }
    public void setHighestBidderName(String name) { this.highestBidderName = name; }
    public String getCurrency() { return currency; }
    public boolean isServerAuction() { return serverAuction; }
    public boolean isServer() { return serverAuction; }
    public boolean isMystery() { return mystery; }
    public List<String> getMysteryLore() { return mysteryLore; }
    public long getStartedAt() { return startedAt; }
    public long getExpireAt() { return expireAt; }
    public void setExpireAt(long expireAt) { this.expireAt = expireAt; }

    public double getNextMinimumBid() {
        if (highestBidderUuid == null) {
            return startingPrice;
        }
        return currentBid + minBidStep;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expireAt;
    }
}
