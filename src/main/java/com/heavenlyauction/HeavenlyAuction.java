package com.heavenlyauction;

import com.heavenlyauction.commands.VBLAdminCommand;
import com.heavenlyauction.commands.VBLCommand;
import com.heavenlyauction.config.ConfigManager;
import com.heavenlyauction.config.LanguageManager;
import com.heavenlyauction.database.DataRetentionManager;
import com.heavenlyauction.database.DatabaseManager;
import com.heavenlyauction.economy.EconomyManager;
import com.heavenlyauction.gui.GUIManager;
import com.heavenlyauction.managers.AuctionManager;
import com.heavenlyauction.managers.BountyManager;
import com.heavenlyauction.managers.MailboxManager;
import com.heavenlyauction.managers.MarketManager;
import org.bukkit.plugin.java.JavaPlugin;

public class HeavenlyAuction extends JavaPlugin {

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private EconomyManager economyManager;
    private DatabaseManager databaseManager;
    private DataRetentionManager dataRetentionManager;
    private MailboxManager mailboxManager;
    private MarketManager marketManager;
    private BountyManager bountyManager;
    private AuctionManager auctionManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        // 1. Config & Language
        configManager = new ConfigManager(this);
        configManager.load();

        languageManager = new LanguageManager(this);
        languageManager.load();

        // 2. Economy
        economyManager = new EconomyManager(this);
        economyManager.init();

        // 3. Database
        databaseManager = new DatabaseManager(this);
        databaseManager.init();

        // 4. Data Retention (Anti-Overflow & Optimization)
        dataRetentionManager = new DataRetentionManager(this);
        dataRetentionManager.start();

        // 5. Core Business Managers
        mailboxManager = new MailboxManager(this);
        marketManager = new MarketManager(this);
        marketManager.load();

        bountyManager = new BountyManager(this);
        bountyManager.load();

        auctionManager = new AuctionManager(this);
        auctionManager.load();

        // 6. GUI System
        guiManager = new GUIManager(this);
        getServer().getPluginManager().registerEvents(guiManager, this);

        // 7. Commands & Tab Completers
        VBLCommand vblCmd = new VBLCommand(this);
        if (getCommand("vbl") != null) {
            getCommand("vbl").setExecutor(vblCmd);
            getCommand("vbl").setTabCompleter(vblCmd);
        }

        VBLAdminCommand adminCmd = new VBLAdminCommand(this);
        if (getCommand("vbladmin") != null) {
            getCommand("vbladmin").setExecutor(adminCmd);
            getCommand("vbladmin").setTabCompleter(adminCmd);
        }

        getLogger().info("=================================================");
        getLogger().info(" [Vạn Bảo Lâu - HeavenlyAuction] Khởi động thành công!");
        getLogger().info(" Tối ưu hóa dữ liệu, an toàn giao dịch & chống tràn RAM/Disk!");
        getLogger().info("=================================================");
    }

    @Override
    public void onDisable() {
        if (marketManager != null) marketManager.stop();
        if (bountyManager != null) bountyManager.stop();
        if (auctionManager != null) auctionManager.stop();
        if (dataRetentionManager != null) dataRetentionManager.stop();
        if (databaseManager != null) databaseManager.close();

        getLogger().info("[HeavenlyAuction] Đã lưu trữ toàn bộ trạng thái an toàn & tắt thành công!");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public DataRetentionManager getDataRetentionManager() { return dataRetentionManager; }
    public MailboxManager getMailboxManager() { return mailboxManager; }
    public MarketManager getMarketManager() { return marketManager; }
    public BountyManager getBountyManager() { return bountyManager; }
    public AuctionManager getAuctionManager() { return auctionManager; }
    public GUIManager getGuiManager() { return guiManager; }
}
