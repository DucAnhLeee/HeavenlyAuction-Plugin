package com.heavenlyauction.database.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class BountyItem {
    private final long id;
    private final UUID creatorUuid;
    private final String creatorName;
    private final ItemStack prototypeItem;
    private final int requiredAmount;
    private final double rewardPrice;
    private final String currency;
    private final long createdAt;
    private final long expireAt;

    public BountyItem(long id, UUID creatorUuid, String creatorName, ItemStack prototypeItem, int requiredAmount, double rewardPrice, String currency, long createdAt, long expireAt) {
        this.id = id;
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName;
        this.prototypeItem = prototypeItem;
        this.requiredAmount = requiredAmount;
        this.rewardPrice = rewardPrice;
        this.currency = currency;
        this.createdAt = createdAt;
        this.expireAt = expireAt;
    }

    public long getId() { return id; }
    public UUID getCreatorUuid() { return creatorUuid; }
    public String getCreatorName() { return creatorName; }
    public ItemStack getPrototypeItem() { return prototypeItem.clone(); }
    public int getRequiredAmount() { return requiredAmount; }
    public double getRewardPrice() { return rewardPrice; }
    public String getCurrency() { return currency; }
    public long getCreatedAt() { return createdAt; }
    public long getExpireAt() { return expireAt; }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expireAt;
    }
}
