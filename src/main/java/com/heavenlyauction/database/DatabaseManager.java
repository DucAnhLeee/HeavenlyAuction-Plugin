package com.heavenlyauction.database;

import com.heavenlyauction.HeavenlyAuction;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class DatabaseManager {
    private final HeavenlyAuction plugin;
    private Connection connection;
    private final ExecutorService dbExecutor;

    public DatabaseManager(HeavenlyAuction plugin) {
        this.plugin = plugin;
        this.dbExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "HeavenlyAuction-DB-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized void init() {
        try {
            Class.forName("org.sqlite.JDBC");

            File dataFolder = new File(plugin.getDataFolder(), "data");
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "database.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode = WAL;");
                stmt.execute("PRAGMA synchronous = NORMAL;");
                stmt.execute("PRAGMA busy_timeout = 5000;");
                stmt.execute("PRAGMA foreign_keys = ON;");
            }

            createTables();
            migrateSchema();
            plugin.getLogger().info("Cơ sở dữ liệu SQLite đã khởi tạo và đồng bộ thành công tại data/database.db!");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("Không tìm thấy SQLite JDBC driver: " + e.getMessage());
        } catch (SQLException e) {
            plugin.getLogger().severe("Lỗi kết nối tới cơ sở dữ liệu SQLite: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Market Items
            stmt.execute("CREATE TABLE IF NOT EXISTS market_items (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "seller_uuid VARCHAR(36) NOT NULL," +
                    "seller_name VARCHAR(32) NOT NULL DEFAULT ''," +
                    "item_data TEXT NOT NULL," +
                    "price DOUBLE NOT NULL," +
                    "currency VARCHAR(32) NOT NULL DEFAULT 'eco'," +
                    "listed_at BIGINT NOT NULL DEFAULT 0," +
                    "expire_at BIGINT NOT NULL DEFAULT 0" +
                    ");");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_market_seller ON market_items(seller_uuid);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_market_expire ON market_items(expire_at);");

            // Bounties
            stmt.execute("CREATE TABLE IF NOT EXISTS bounties (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "creator_uuid VARCHAR(36) NOT NULL," +
                    "creator_name VARCHAR(32) NOT NULL DEFAULT ''," +
                    "prototype_data TEXT NOT NULL," +
                    "required_amount INTEGER NOT NULL," +
                    "reward_price DOUBLE NOT NULL," +
                    "reward_currency VARCHAR(32) NOT NULL DEFAULT 'eco'," +
                    "created_at BIGINT NOT NULL DEFAULT 0," +
                    "expire_at BIGINT NOT NULL DEFAULT 0" +
                    ");");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bounty_creator ON bounties(creator_uuid);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bounty_expire ON bounties(expire_at);");

            // Auctions
            stmt.execute("CREATE TABLE IF NOT EXISTS auctions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "seller_uuid VARCHAR(36) NULL," +
                    "seller_name VARCHAR(32) NOT NULL DEFAULT ''," +
                    "item_data TEXT NOT NULL," +
                    "starting_price DOUBLE NOT NULL," +
                    "min_bid_step DOUBLE NOT NULL," +
                    "current_bid DOUBLE NOT NULL," +
                    "highest_bidder_uuid VARCHAR(36) NULL," +
                    "highest_bidder_name VARCHAR(32) NULL," +
                    "currency VARCHAR(32) NOT NULL DEFAULT 'eco'," +
                    "is_server INTEGER NOT NULL DEFAULT 0," +
                    "is_mystery INTEGER NOT NULL DEFAULT 0," +
                    "mystery_lore TEXT NULL," +
                    "started_at BIGINT NOT NULL DEFAULT 0," +
                    "expire_at BIGINT NOT NULL DEFAULT 0" +
                    ");");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_auction_expire ON auctions(expire_at);");

            // Mailbox
            stmt.execute("CREATE TABLE IF NOT EXISTS mailbox (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "owner_uuid VARCHAR(36) NOT NULL," +
                    "type VARCHAR(16) NOT NULL," +
                    "item_data TEXT NULL," +
                    "amount DOUBLE NOT NULL DEFAULT 0," +
                    "currency VARCHAR(32) NULL," +
                    "reason VARCHAR(255) NOT NULL DEFAULT ''," +
                    "created_at BIGINT NOT NULL DEFAULT 0," +
                    "claimed INTEGER NOT NULL DEFAULT 0" +
                    ");");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_mailbox_owner ON mailbox(owner_uuid, claimed);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_mailbox_created ON mailbox(created_at);");

            // Transaction logs
            stmt.execute("CREATE TABLE IF NOT EXISTS transaction_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "type VARCHAR(32) NOT NULL," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "details TEXT NOT NULL," +
                    "timestamp BIGINT NOT NULL" +
                    ");");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_log_time ON transaction_logs(timestamp);");
        }
    }

    public synchronized void migrateSchema() {
        if (connection == null) return;
        try {
            ensureTableColumns("market_items", new String[]{"seller_name", "currency", "listed_at", "expire_at"},
                    new String[]{"VARCHAR(32) DEFAULT ''", "VARCHAR(32) DEFAULT 'eco'", "BIGINT DEFAULT 0", "BIGINT DEFAULT 0"});

            ensureTableColumns("bounties", new String[]{"creator_name", "reward_currency", "created_at", "expire_at"},
                    new String[]{"VARCHAR(32) DEFAULT ''", "VARCHAR(32) DEFAULT 'eco'", "BIGINT DEFAULT 0", "BIGINT DEFAULT 0"});

            ensureTableColumns("auctions", new String[]{"seller_name", "highest_bidder_name", "currency", "is_server", "is_mystery", "mystery_lore", "started_at", "expire_at"},
                    new String[]{"VARCHAR(32) DEFAULT ''", "VARCHAR(32) DEFAULT NULL", "VARCHAR(32) DEFAULT 'eco'", "INTEGER DEFAULT 0", "INTEGER DEFAULT 0", "TEXT DEFAULT NULL", "BIGINT DEFAULT 0", "BIGINT DEFAULT 0"});

            ensureTableColumns("mailbox", new String[]{"currency", "amount", "claimed"},
                    new String[]{"VARCHAR(32) DEFAULT NULL", "DOUBLE DEFAULT 0", "INTEGER DEFAULT 0"});

        } catch (Exception e) {
            plugin.getLogger().warning("Cảnh báo khi kiểm tra cấu trúc Database: " + e.getMessage());
        }
    }

    private void ensureTableColumns(String table, String[] columns, String[] defs) {
        Set<String> existingColumns = new HashSet<>();
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery("PRAGMA table_info(" + table + ");")) {
            while (rs.next()) {
                existingColumns.add(rs.getString("name").toLowerCase());
            }
        } catch (SQLException e) {
            return;
        }

        for (int i = 0; i < columns.length; i++) {
            String col = columns[i];
            String def = defs[i];
            if (!existingColumns.contains(col.toLowerCase())) {
                try (Statement alter = connection.createStatement()) {
                    alter.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + col + " " + def + ";");
                    plugin.getLogger().info("Đã tự động thêm cột " + col + " vào bảng " + table + "!");
                } catch (SQLException e) {
                    plugin.getLogger().warning("Không thể thêm cột " + col + " vào bảng " + table + " (" + e.getMessage() + "). Tiến hành tái tạo bảng...");
                    recreateTable(table);
                    break;
                }
            }
        }
    }

    public synchronized void recreateTable(String table) {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DROP TABLE IF EXISTS " + table + ";");
            createTables();
            plugin.getLogger().info("Đã tái tạo mới thành công bảng: " + table);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                init();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public void executeAsync(Consumer<Connection> action) {
        dbExecutor.submit(() -> {
            try {
                Connection conn = getConnection();
                if (conn != null) {
                    action.accept(conn);
                } else {
                    plugin.getLogger().severe("Không thể thực thi Database Action vì connection là null!");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Lỗi trong Database Executor: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public synchronized void close() {
        dbExecutor.shutdown();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
