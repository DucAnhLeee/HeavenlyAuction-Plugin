package com.heavenlyauction.database.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class MarketItem {
    private final long id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack item;
    private final double price;
    private final String currency;
    private final long listedAt;
    private final long expireAt;

    public MarketItem(long id, UUID sellerUuid, String sellerName, ItemStack item, double price, String currency, long listedAt, long expireAt) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.currency = currency;
        this.listedAt = listedAt;
        this.expireAt = expireAt;
    }

    public long getId() { return id; }
    public UUID getSellerUuid() { return sellerUuid; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItem() { return item.clone(); }
    public double getPrice() { return price; }
    public String getCurrency() { return currency; }
    public long getListedAt() { return listedAt; }
    public long getExpireAt() { return expireAt; }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expireAt;
    }
}
