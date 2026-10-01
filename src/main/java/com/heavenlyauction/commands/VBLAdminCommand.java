package com.heavenlyauction.commands;

import com.heavenlyauction.HeavenlyAuction;
import com.heavenlyauction.config.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VBLAdminCommand implements CommandExecutor, TabCompleter {
    private final HeavenlyAuction plugin;

    public VBLAdminCommand(HeavenlyAuction plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();

        if (!sender.hasPermission("vbl.admin")) {
            sender.sendMessage(lang.getMessage("messages.no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(lang.getMessage("messages.admin.usage"));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            plugin.getConfigManager().load();
            plugin.getLanguageManager().load();
            plugin.getDatabaseManager().migrateSchema();
            sender.sendMessage(lang.getMessage("messages.admin.reloaded"));
            return true;
        }

        if (sub.equals("fixdb") || sub.equals("migrate")) {
            sender.sendMessage("§e[HeavenlyAuction] Đang kiểm tra và sửa đổi cấu trúc SQLite...");
            plugin.getDatabaseManager().migrateSchema();
            plugin.getMarketManager().load();
            sender.sendMessage("§a[HeavenlyAuction] Đã đồng bộ và sửa đổi cấu trúc SQLite thành công! Mọi bảng đã có đủ cột.");
            return true;
        }

        if (sub.equals("startauction")) {
            plugin.getAuctionManager().startServerScheduledAuction(false);
            sender.sendMessage(lang.getMessage("messages.admin.auction-forced"));
            return true;
        }

        if (sub.equals("secretauction") || sub.equals("mystery")) {
            plugin.getAuctionManager().startServerScheduledAuction(true);
            sender.sendMessage(lang.getMessage("messages.admin.mystery-forced"));
            return true;
        }

        if (sub.equals("purge")) {
            plugin.getDataRetentionManager().manualPurgeAndVacuum();
            sender.sendMessage(lang.getMessage("messages.admin.purge-success"));
            return true;
        }

        sender.sendMessage(lang.getMessage("messages.admin.usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("vbl.admin")) return Collections.emptyList();

        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            String[] subs = {"reload", "fixdb", "startauction", "secretauction", "purge"};
            for (String s : subs) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(s);
                }
            }
        }
        return list;
    }
}
