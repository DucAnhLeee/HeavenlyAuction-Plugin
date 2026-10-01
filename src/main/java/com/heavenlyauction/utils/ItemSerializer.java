package com.heavenlyauction.utils;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public final class ItemSerializer {
    private static Method serializeAsBytesMethod = null;
    private static Method deserializeBytesMethod = null;

    static {
        try {
            serializeAsBytesMethod = ItemStack.class.getMethod("serializeAsBytes");
            deserializeBytesMethod = ItemStack.class.getMethod("deserializeBytes", byte[].class);
        } catch (Throwable ignored) {
            // Paper component serialization not present, standard Bukkit serialization will be used
        }
    }

    private ItemSerializer() {}

    /**
     * Serializes an ItemStack to a resilient Base64 string, preserving 100% MMOItems, AceFormation,
     * AceAlchemy, PDC, and custom tags.
     */
    public static String toBase64(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "";
        }

        // Method 1: Paper Native Byte Components
        if (serializeAsBytesMethod != null) {
            try {
                byte[] bytes = (byte[]) serializeAsBytesMethod.invoke(item);
                if (bytes != null && bytes.length > 0) {
                    return "P2:" + Base64.getEncoder().encodeToString(bytes);
                }
            } catch (Throwable ignored) {}
        }

        // Method 2: Standard BukkitObjectOutputStream (Used by AcePet, AceRealm, Vault)
        try (ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
             BukkitObjectOutputStream objectOutput = new BukkitObjectOutputStream(byteOutput)) {
            objectOutput.writeObject(item.clone());
            objectOutput.flush();
            return Base64.getEncoder().encodeToString(byteOutput.toByteArray());
        } catch (Throwable t) {
            // Method 3: YamlConfiguration fallback
            try {
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.set("i", item.clone());
                return "Y1:" + Base64.getEncoder().encodeToString(yaml.saveToString().getBytes(StandardCharsets.UTF_8));
            } catch (Throwable ex) {
                ex.printStackTrace();
                return "";
            }
        }
    }

    /**
     * Deserializes an ItemStack from Base64 string. Gracefully handles P2:, Y1:, v1:, v2:, or plain Base64.
     */
    public static ItemStack fromBase64(String data) {
        if (data == null || data.trim().isEmpty()) {
            return null;
        }

        String trimmed = data.trim();

        // 1. Paper Native Bytes
        if (trimmed.startsWith("P2:")) {
            try {
                byte[] bytes = Base64.getDecoder().decode(trimmed.substring(3));
                if (deserializeBytesMethod != null) {
                    Object obj = deserializeBytesMethod.invoke(null, (Object) bytes);
                    if (obj instanceof ItemStack is) return is;
                }
            } catch (Throwable ignored) {}
        }

        // 2. Yaml format
        if (trimmed.startsWith("Y1:")) {
            try {
                String yamlStr = new String(Base64.getDecoder().decode(trimmed.substring(3)), StandardCharsets.UTF_8);
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.loadFromString(yamlStr);
                return yaml.getItemStack("i");
            } catch (Throwable ignored) {}
        }

        // 3. Strip legacy prefixes if any
        String raw = trimmed;
        if (raw.startsWith("v1:") || raw.startsWith("v2:")) {
            raw = raw.substring(3);
        }

        // 4. BukkitObjectInputStream
        try (ByteArrayInputStream byteInput = new ByteArrayInputStream(Base64.getDecoder().decode(raw));
             BukkitObjectInputStream objectInput = new BukkitObjectInputStream(byteInput)) {
            Object obj = objectInput.readObject();
            if (obj instanceof ItemStack is) {
                return is;
            }
        } catch (Throwable ignored) {}

        return null;
    }

    /**
     * Compares prototype item with a target item in player inventory.
     * Accurately checks Material, CustomModelData, PDC, and Meta.
     */
    public static boolean isMatching(ItemStack prototype, ItemStack target) {
        if (prototype == null || target == null) return false;
        if (prototype.getType() != target.getType()) return false;

        ItemMeta pMeta = prototype.getItemMeta();
        ItemMeta tMeta = target.getItemMeta();

        if (pMeta == null && tMeta == null) return true;
        if (pMeta == null || tMeta == null) return false;

        // Custom model data
        if (pMeta.hasCustomModelData() != tMeta.hasCustomModelData()) return false;
        if (pMeta.hasCustomModelData() && pMeta.getCustomModelData() != tMeta.getCustomModelData()) return false;

        // Display name
        if (pMeta.hasDisplayName() != tMeta.hasDisplayName()) return false;
        if (pMeta.hasDisplayName() && !Objects.equals(pMeta.getDisplayName(), tMeta.getDisplayName())) return false;

        // Persistent Data Container (MMOItems, AceFormation, AceAlchemy all use PDC)
        PersistentDataContainer pPdc = pMeta.getPersistentDataContainer();
        PersistentDataContainer tPdc = tMeta.getPersistentDataContainer();
        if (!pPdc.getKeys().isEmpty()) {
            if (!tPdc.getKeys().containsAll(pPdc.getKeys())) {
                return false;
            }
        }

        // General Spigot similarity
        return prototype.isSimilar(target);
    }
}
