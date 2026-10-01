package com.heavenlyauction.database;

import com.heavenlyauction.HeavenlyAuction;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class DataRetentionManager {
    private final HeavenlyAuction plugin;
    private BukkitTask cleanupTask;

    public DataRetentionManager(HeavenlyAuction plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int intervalMinutes = plugin.getConfigManager().getDataCleanupIntervalMinutes();
        long intervalTicks = intervalMinutes * 60L * 20L;

        cleanupTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::performPurge, 20L * 60L, intervalTicks);
    }

    public void stop() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    public void performPurge() {
        plugin.getDatabaseManager().executeAsync(conn -> {
            try {
                long cutoff = System.currentTimeMillis() - (plugin.getConfigManager().getDataRetentionDays() * 86400000L);

                // 1. Purge claimed mailbox entries older than cutoff
                int deletedMail;
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM mailbox WHERE claimed = 1 AND created_at < ?;")) {
                    ps.setLong(1, cutoff);
                    deletedMail = ps.executeUpdate();
                }

                // 2. Purge old transaction logs older than cutoff
                int deletedLogs;
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM transaction_logs WHERE timestamp < ?;")) {
                    ps.setLong(1, cutoff);
                    deletedLogs = ps.executeUpdate();
                }

                // 3. Optimize database file size
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA optimize;");
                }

                if (deletedMail > 0 || deletedLogs > 0) {
                    plugin.getLogger().info(String.format("[DataRetention] Đã dọn dẹp an toàn %d thư cũ và %d bản ghi lịch sử, tối ưu bộ nhớ!", deletedMail, deletedLogs));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Lỗi trong quá trình dọn dẹp cơ sở dữ liệu định kỳ: " + e.getMessage());
            }
        });
    }

    public void manualPurgeAndVacuum() {
        plugin.getDatabaseManager().executeAsync(conn -> {
            try {
                long cutoff = System.currentTimeMillis() - (plugin.getConfigManager().getDataRetentionDays() * 86400000L);
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM mailbox WHERE claimed = 1 AND created_at < ?;")) {
                    ps.setLong(1, cutoff);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM transaction_logs WHERE timestamp < ?;")) {
                    ps.setLong(1, cutoff);
                    ps.executeUpdate();
                }
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("VACUUM;");
                }
                plugin.getLogger().info("[DataRetention] Đã hoàn tất cưỡng chế dọn dẹp & nén VACUUM cơ sở dữ liệu SQLite.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
