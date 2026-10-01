package com.heavenlyauction.config;

import com.heavenlyauction.HeavenlyAuction;
import com.heavenlyauction.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private final HeavenlyAuction plugin;

    // Market
    private long marketExpireSeconds;
    private int marketMaxListingsPerPlayer;

    // Bounties
    private long bountyExpireSeconds;
    private int bountyMaxPerPlayer;

    // Auctions
    private long auctionDefaultDurationSeconds;
    private double auctionMinBidStep;
    private double auctionTaxPercent;

    // Mailbox
    private int mailboxMaxItemsPerPlayer;

    // Data Retention
    private int dataRetentionDays;
    private int dataCleanupIntervalMinutes;

    // Currencies
    private final Map<String, CurrencyConfig> currencies = new HashMap<>();

    // Server Scheduled Auctions
    private boolean serverAuctionEnabled;
    private int serverAuctionIntervalMinutes;
    private int serverAuctionDurationSeconds;
    private String serverAuctionMysteryTime;
    private String mysteryItemName;
    private Material mysteryItemMaterial;
    private List<String> mysteryItemLore;
    private final List<ServerAuctionPoolItem> serverAuctionPool = new ArrayList<>();

    public static class CurrencyConfig {
        private final String id;
        private final String displayName;
        private final double taxPercent;
        private final String coinsEngineName;

        public CurrencyConfig(String id, String displayName, double taxPercent, String coinsEngineName) {
            this.id = id;
            this.displayName = displayName;
            this.taxPercent = taxPercent;
            this.coinsEngineName = coinsEngineName;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public double getTaxPercent() { return taxPercent; }
        public String getCoinsEngineName() { return coinsEngineName; }
    }

    public static class ServerAuctionPoolItem {
        private final String id;
        private final ItemStack item;
        private final double startingPrice;
        private final double minBidStep;
        private final String currency;
        private final boolean isMystery;

        public ServerAuctionPoolItem(String id, ItemStack item, double startingPrice, double minBidStep, String currency, boolean isMystery) {
            this.id = id;
            this.item = item;
            this.startingPrice = startingPrice;
            this.minBidStep = minBidStep;
            this.currency = currency;
            this.isMystery = isMystery;
        }

        public String getId() { return id; }
        public ItemStack getItem() { return item.clone(); }
        public double getStartingPrice() { return startingPrice; }
        public double getMinBidStep() { return minBidStep; }
        public String getCurrency() { return currency; }
        public boolean isMystery() { return isMystery; }
    }

    public ConfigManager(HeavenlyAuction plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // Market
        this.marketExpireSeconds = config.getLong("market.expire-time-seconds", 604800L); // 7 days
        this.marketMaxListingsPerPlayer = config.getInt("market.max-items-per-player", 10);

        // Bounties
        this.bountyExpireSeconds = config.getLong("bounties.expire-time-seconds", 604800L); // 7 days
        this.bountyMaxPerPlayer = config.getInt("bounties.max-bounties-per-player", 5);

        // Auctions
        this.auctionDefaultDurationSeconds = config.getLong("auctions.default-duration-seconds", 300L);
        this.auctionMinBidStep = config.getDouble("auctions.min-bid-step", 10.0);
        this.auctionTaxPercent = config.getDouble("auctions.tax-percent", 5.0);

        // Mailbox & Anti-Overflow
        this.mailboxMaxItemsPerPlayer = config.getInt("mailbox.max-items-per-player", 50);
        this.dataRetentionDays = config.getInt("data-retention.retention-days", 30);
        this.dataCleanupIntervalMinutes = config.getInt("data-retention.cleanup-interval-minutes", 60);

        // Currencies
        currencies.clear();
        ConfigurationSection curSec = config.getConfigurationSection("currencies");
        if (curSec != null) {
            for (String key : curSec.getKeys(false)) {
                String dName = ColorUtil.colorize(curSec.getString(key + ".display-name", key));
                double tax = curSec.getDouble(key + ".tax-percent", 0.0);
                String ceName = curSec.getString(key + ".coinsengine-currency", key);
                currencies.put(key.toLowerCase(), new CurrencyConfig(key.toLowerCase(), dName, tax, ceName));
            }
        }
        if (!currencies.containsKey("eco")) {
            currencies.put("eco", new CurrencyConfig("eco", "§aBạc", 10.0, "vault"));
        }
        if (!currencies.containsKey("points")) {
            currencies.put("points", new CurrencyConfig("points", "§bLinh Thạch", 5.0, "playerpoints"));
        }
        if (!currencies.containsKey("xuhethong")) {
            currencies.put("xuhethong", new CurrencyConfig("xuhethong", "§6Xu Hệ Thống", 8.0, "xuhethong"));
        }

        // Server Auctions
        this.serverAuctionEnabled = config.getBoolean("server-auctions.enabled", true);
        this.serverAuctionIntervalMinutes = config.getInt("server-auctions.interval-minutes", 180); // 3 hours
        this.serverAuctionDurationSeconds = config.getInt("server-auctions.duration-seconds", 300);
        this.serverAuctionMysteryTime = config.getString("server-auctions.mystery-time", "20:00");

        this.mysteryItemName = ColorUtil.colorize(config.getString("server-auctions.mystery-item.name", "&5&l??? Cổ Vật Thượng Cổ Chưa Giám Định"));
        String matName = config.getString("server-auctions.mystery-item.material", "ECHO_SHARD");
        Material mat = Material.matchMaterial(matName);
        this.mysteryItemMaterial = mat != null ? mat : Material.ECHO_SHARD;

        this.mysteryItemLore = new ArrayList<>();
        for (String l : config.getStringList("server-auctions.mystery-item.lore")) {
            this.mysteryItemLore.add(ColorUtil.colorize(l));
        }

        // Server Auction Items Pool
        serverAuctionPool.clear();
        ConfigurationSection poolSec = config.getConfigurationSection("server-auctions.items");
        if (poolSec != null) {
            for (String key : poolSec.getKeys(false)) {
                String iMatName = poolSec.getString(key + ".material", "DIAMOND");
                Material iMat = Material.matchMaterial(iMatName);
                if (iMat == null) iMat = Material.DIAMOND;

                int amount = poolSec.getInt(key + ".amount", 1);
                ItemStack stack = new ItemStack(iMat, amount);
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    if (poolSec.contains(key + ".name")) {
                        meta.setDisplayName(ColorUtil.colorize(poolSec.getString(key + ".name")));
                    }
                    if (poolSec.contains(key + ".lore")) {
                        List<String> lore = new ArrayList<>();
                        for (String l : poolSec.getStringList(key + ".lore")) {
                            lore.add(ColorUtil.colorize(l));
                        }
                        meta.setLore(lore);
                    }
                    if (poolSec.contains(key + ".custom-model-data")) {
                        meta.setCustomModelData(poolSec.getInt(key + ".custom-model-data"));
                    }
                    stack.setItemMeta(meta);
                }

                double sPrice = poolSec.getDouble(key + ".starting-price", 1000.0);
                double minStep = poolSec.getDouble(key + ".min-bid-step", 100.0);
                String cur = poolSec.getString(key + ".currency", "eco").toLowerCase();
                boolean isMys = poolSec.getBoolean(key + ".is-mystery", false);

                serverAuctionPool.add(new ServerAuctionPoolItem(key, stack, sPrice, minStep, cur, isMys));
            }
        }
    }

    public double getCurrencyTaxPercent(String id) {
        CurrencyConfig cfg = currencies.get(id.toLowerCase());
        return cfg != null ? cfg.getTaxPercent() : 0.0;
    }

    public long getMarketExpireSeconds() { return marketExpireSeconds; }
    public int getMarketMaxListingsPerPlayer() { return marketMaxListingsPerPlayer; }
    public long getBountyExpireSeconds() { return bountyExpireSeconds; }
    public int getBountyMaxPerPlayer() { return bountyMaxPerPlayer; }
    public long getAuctionDefaultDurationSeconds() { return auctionDefaultDurationSeconds; }
    public double getAuctionMinBidStep() { return auctionMinBidStep; }
    public double getAuctionTaxPercent() { return auctionTaxPercent; }
    public int getMailboxMaxItemsPerPlayer() { return mailboxMaxItemsPerPlayer; }
    public int getDataRetentionDays() { return dataRetentionDays; }
    public int getDataCleanupIntervalMinutes() { return dataCleanupIntervalMinutes; }
    public Map<String, CurrencyConfig> getCurrencies() { return currencies; }
    public CurrencyConfig getCurrency(String id) { return currencies.get(id.toLowerCase()); }
    public boolean isServerAuctionEnabled() { return serverAuctionEnabled; }
    public int getServerAuctionIntervalMinutes() { return serverAuctionIntervalMinutes; }
    public int getServerAuctionDurationSeconds() { return serverAuctionDurationSeconds; }
    public String getServerAuctionMysteryTime() { return serverAuctionMysteryTime; }
    public String getMysteryItemName() { return mysteryItemName; }
    public Material getMysteryItemMaterial() { return mysteryItemMaterial; }
    public List<String> getMysteryItemLore() { return mysteryItemLore; }
    public List<ServerAuctionPoolItem> getServerAuctionPool() { return serverAuctionPool; }
}
