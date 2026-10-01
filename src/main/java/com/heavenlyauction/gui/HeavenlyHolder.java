package com.heavenlyauction.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public class HeavenlyHolder implements InventoryHolder {
    private final GUIType type;
    private int page = 0;
    private final Map<Integer, Long> slotActionIds = new HashMap<>();
    private Inventory inventory;

    public HeavenlyHolder(GUIType type) {
        this.type = type;
    }

    public HeavenlyHolder(GUIType type, int page) {
        this.type = type;
        this.page = page;
    }

    public GUIType getType() { return type; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public void setActionId(int slot, long id) {
        slotActionIds.put(slot, id);
    }

    public Long getActionId(int slot) {
        return slotActionIds.get(slot);
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
