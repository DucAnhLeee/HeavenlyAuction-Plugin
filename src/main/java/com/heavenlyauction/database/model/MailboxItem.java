package com.heavenlyauction.database.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class MailboxItem {
    private final long id;
    private final UUID ownerUuid;
    private final String type; // "ITEM" or "MONEY"
    private final ItemStack item; // null if MONEY
    private final double amount; // 0 if ITEM
    private final String currency; // null if ITEM
    private final String reason;
    private final long createdAt;

    public MailboxItem(long id, UUID ownerUuid, String type, ItemStack item, double amount, String currency, String reason, long createdAt) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.type = type;
        this.item = item;
        this.amount = amount;
        this.currency = currency;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public String getType() { return type; }
    public ItemStack getItem() { return item != null ? item.clone() : null; }
    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getReason() { return reason; }
    public long getCreatedAt() { return createdAt; }

    public boolean isItem() {
        return "ITEM".equalsIgnoreCase(type);
    }

    public boolean isMoney() {
        return "MONEY".equalsIgnoreCase(type);
    }
}
