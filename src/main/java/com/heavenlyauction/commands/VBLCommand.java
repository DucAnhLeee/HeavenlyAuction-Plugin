package com.heavenlyauction.commands;

import com.heavenlyauction.HeavenlyAuction;
import com.heavenlyauction.config.LanguageManager;
import com.heavenlyauction.database.model.AuctionItem;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VBLCommand implements CommandExecutor, TabCompleter {
    private final HeavenlyAuction plugin;

    public VBLCommand(HeavenlyAuction plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();

        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("messages.only-player"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("menu") || args[0].equalsIgnoreCase("gui")) {
            player.sendMessage(lang.getMessage("messages.market.opening"));
            plugin.getGuiManager().openMainMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        // 1. CHỢ TRỜ / MARKET
        if (sub.equals("market") || sub.equals("cho")) {
            plugin.getGuiManager().openMarket(player, 0, false);
            return true;
        }

        // 2. SELL:
        // /vbl sell -> Mở cửa sổ chọn giá nhanh!
        // /vbl sell <giá> [loại_tiền]
        // /vbl sell <giá> [số_lượng] [loại_tiền]
        if (sub.equals("sell") || sub.equals("ban")) {
            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (handItem == null || handItem.getType() == Material.AIR) {
                player.sendMessage(lang.getMessage("messages.market.sell-no-item"));
                return true;
            }

            // Nếu chỉ gõ /vbl sell hoặc /ah sell mà không truyền giá -> Mở ngay Cửa Sổ Chọn Giá Nhanh!
            if (args.length == 1) {
                plugin.getGuiManager().openQuickSell(player, 1000, "eco");
                return true;
            }

            try {
                double price = Double.parseDouble(args[1]);
                if (price <= 0) {
                    player.sendMessage(lang.getMessage("messages.market.sell-invalid-price"));
                    return true;
                }

                int sellAmount = handItem.getAmount();
                String currency = "eco"; // default currency

                if (args.length == 3) {
                    // Could be /vbl sell <price> <currency> OR /vbl sell <price> <amount>
                    if (plugin.getConfigManager().getCurrencies().containsKey(args[2].toLowerCase())) {
                        currency = args[2].toLowerCase();
                    } else {
                        try {
                            int parsedAmount = Integer.parseInt(args[2]);
                            if (parsedAmount > 0 && parsedAmount <= handItem.getAmount()) {
                                sellAmount = parsedAmount;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                } else if (args.length >= 4) {
                    // /vbl sell <price> <amount> <currency>
                    try {
                        int parsedAmount = Integer.parseInt(args[2]);
                        if (parsedAmount > 0 && parsedAmount <= handItem.getAmount()) {
                            sellAmount = parsedAmount;
                        }
                    } catch (NumberFormatException ignored) {}

                    if (plugin.getConfigManager().getCurrencies().containsKey(args[3].toLowerCase())) {
                        currency = args[3].toLowerCase();
                    }
                }

                // Prepare item to sell
                ItemStack toSell = handItem.clone();
                toSell.setAmount(sellAmount);

                // Deduct from hand
                if (handItem.getAmount() <= sellAmount) {
                    player.getInventory().setItemInMainHand(null);
                } else {
                    handItem.setAmount(handItem.getAmount() - sellAmount);
                    player.getInventory().setItemInMainHand(handItem);
                }

                final String finalCurrency = currency;
                final ItemStack finalToSell = toSell;

                plugin.getMarketManager().addItem(player, toSell, price, finalCurrency, success -> {
                    if (success) {
                        player.sendMessage(lang.getMessage("messages.market.sell-success",
                                "%price%", plugin.getEconomyManager().formatAmount(price),
                                "%currency%", plugin.getEconomyManager().getCurrencyName(finalCurrency)));
                    } else {
                        // Return item to player inventory
                        player.getInventory().addItem(finalToSell);
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.prefix") + "§cĐăng bán bảo vật thất bại! Đã hoàn trả lại vật phẩm vào túi đồ.");
                    }
                });

            } catch (NumberFormatException e) {
                player.sendMessage(lang.getMessage("messages.market.sell-invalid-price"));
            }
            return true;
        }

        // 3. HUYỀN THƯỞNG: /vbl treo <số_lượng> <tiền_thưởng> [tiền_tệ]
        if (sub.equals("treo") || sub.equals("bounty")) {
            if (args.length < 3) {
                player.sendMessage(lang.getMessage("messages.bounty.treo-usage"));
                return true;
            }

            try {
                int amount = Integer.parseInt(args[1]);
                double reward = Double.parseDouble(args[2]);
                String currency = args.length >= 4 ? args[3].toLowerCase() : "eco";

                if (amount <= 0) {
                    player.sendMessage(lang.getMessage("messages.bounty.treo-invalid-amount"));
                    return true;
                }
                if (reward <= 0) {
                    player.sendMessage(lang.getMessage("messages.bounty.treo-invalid-reward"));
                    return true;
                }
                if (!plugin.getConfigManager().getCurrencies().containsKey(currency)) {
                    currency = "eco";
                }

                ItemStack handItem = player.getInventory().getItemInMainHand();
                if (handItem == null || handItem.getType() == Material.AIR) {
                    player.sendMessage(lang.getMessage("messages.bounty.treo-no-item"));
                    return true;
                }

                ItemStack prototype = handItem.clone();
                prototype.setAmount(1);

                final String finalCur = currency;
                plugin.getBountyManager().createBounty(player, prototype, amount, reward, currency, success -> {
                    if (success) {
                        String iName = prototype.getItemMeta() != null && prototype.getItemMeta().hasDisplayName() ?
                                prototype.getItemMeta().getDisplayName() : prototype.getType().name();
                        player.sendMessage(lang.getMessage("messages.bounty.treo-success",
                                "%amount%", String.valueOf(amount),
                                "%item%", iName,
                                "%reward%", plugin.getEconomyManager().formatAmount(reward),
                                "%currency%", plugin.getEconomyManager().getCurrencyName(finalCur)));
                    } else {
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.prefix") + "§cTạo lệnh Huyền Thưởng thất bại!");
                    }
                });

            } catch (NumberFormatException e) {
                player.sendMessage(lang.getMessage("messages.prefix") + "§cSố lượng hoặc tiền thưởng không hợp lệ!");
            }
            return true;
        }

        // 4. ĐẤU GIÁ: /vbl daugia <giá_khởi_điểm> <bước_giá> [thời_gian_giây] [tiền_tệ]
        if (sub.equals("daugia") || sub.equals("auction")) {
            if (args.length == 1) {
                plugin.getGuiManager().openAuction(player, 0);
                return true;
            }

            if (args.length < 3) {
                player.sendMessage(lang.getMessage("messages.auction.create-usage"));
                return true;
            }

            try {
                double startingPrice = Double.parseDouble(args[1]);
                double minBidStep = Double.parseDouble(args[2]);
                long duration = args.length >= 4 ? Long.parseLong(args[3]) : plugin.getConfigManager().getAuctionDefaultDurationSeconds();
                String currency = args.length >= 5 ? args[4].toLowerCase() : "eco";

                if (startingPrice <= 0 || minBidStep <= 0 || duration < 30) {
                    player.sendMessage(lang.getMessage("messages.prefix") + "§cThông số không hợp lệ! Thời gian tối thiểu 30 giây.");
                    return true;
                }
                if (!plugin.getConfigManager().getCurrencies().containsKey(currency)) {
                    currency = "eco";
                }

                ItemStack handItem = player.getInventory().getItemInMainHand();
                if (handItem == null || handItem.getType() == Material.AIR) {
                    player.sendMessage(lang.getMessage("messages.market.sell-no-item"));
                    return true;
                }

                ItemStack toAuction = handItem.clone();
                player.getInventory().setItemInMainHand(null);

                final String finalCur = currency;
                plugin.getAuctionManager().createPlayerAuction(player, toAuction, startingPrice, minBidStep, duration, currency, success -> {
                    if (success) {
                        player.sendMessage(lang.getMessage("messages.auction.create-success",
                                "%price%", plugin.getEconomyManager().formatAmount(startingPrice),
                                "%currency%", plugin.getEconomyManager().getCurrencyName(finalCur)));
                    } else {
                        player.getInventory().addItem(toAuction);
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.prefix") + "§cMở phiên đấu giá thất bại! Đã hoàn trả lại vật phẩm.");
                    }
                });

            } catch (NumberFormatException e) {
                player.sendMessage(lang.getMessage("messages.prefix") + "§cGiá trị số không hợp lệ!");
            }
            return true;
        }

        // 5. BID: /vbl bid <id> <số_tiền>
        if (sub.equals("bid") || sub.equals("ragia")) {
            if (args.length < 3) {
                player.sendMessage(lang.getMessage("messages.prefix") + "§cDùng: /vbl bid <id_phiên> <số_tiền>");
                return true;
            }
            try {
                long auctionId = Long.parseLong(args[1]);
                double amount = Double.parseDouble(args[2]);
                plugin.getAuctionManager().placeBid(player, auctionId, amount);
            } catch (NumberFormatException e) {
                player.sendMessage(lang.getMessage("messages.prefix") + "§cID phiên hoặc số tiền không hợp lệ!");
            }
            return true;
        }

        // 6. KHO ĐỒ / HÒM THƯ: /vbl kho
        if (sub.equals("kho") || sub.equals("mailbox") || sub.equals("mail")) {
            plugin.getGuiManager().openMailbox(player, 0);
            return true;
        }

        player.sendMessage(lang.getMessage("messages.prefix") + "§eDùng §a/vbl §eđể mở giao diện hoặc xem danh sách lệnh con.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            String[] subs = {"menu", "market", "sell", "treo", "daugia", "bid", "kho"};
            for (String s : subs) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(s);
                }
            }
            return list;
        }

        String sub = args[0].toLowerCase();

        // /vbl sell <giá> [số_lượng] [tiền_tệ]
        if (sub.equals("sell") || sub.equals("ban")) {
            if (args.length == 2) {
                return Arrays.asList("1000", "5000", "10000", "50000");
            }
            if (args.length == 3) {
                List<String> suggestions = new ArrayList<>(Arrays.asList("1", "8", "16", "32", "64"));
                suggestions.addAll(plugin.getConfigManager().getCurrencies().keySet());
                return suggestions;
            }
            if (args.length == 4) {
                return new ArrayList<>(plugin.getConfigManager().getCurrencies().keySet());
            }
        }

        // /vbl treo <số_lượng> <tiền_thưởng> [tiền_tệ]
        if (sub.equals("treo") || sub.equals("bounty")) {
            if (args.length == 2) {
                return Arrays.asList("1", "8", "16", "32", "64");
            }
            if (args.length == 3) {
                return Arrays.asList("500", "1000", "5000", "10000");
            }
            if (args.length == 4) {
                return new ArrayList<>(plugin.getConfigManager().getCurrencies().keySet());
            }
        }

        // /vbl daugia <giá_khởi_điểm> <bước_giá> [thời_gian_giây] [tiền_tệ]
        if (sub.equals("daugia") || sub.equals("auction")) {
            if (args.length == 2) {
                return Arrays.asList("1000", "5000", "10000");
            }
            if (args.length == 3) {
                return Arrays.asList("50", "100", "500", "1000");
            }
            if (args.length == 4) {
                return Arrays.asList("60", "120", "300", "600");
            }
            if (args.length == 5) {
                return new ArrayList<>(plugin.getConfigManager().getCurrencies().keySet());
            }
        }

        // /vbl bid <id> <số_tiền>
        if (sub.equals("bid") || sub.equals("ragia")) {
            if (args.length == 2) {
                for (AuctionItem ai : plugin.getAuctionManager().getActiveAuctions()) {
                    list.add(String.valueOf(ai.getId()));
                }
                return list;
            }
            if (args.length == 3) {
                try {
                    long id = Long.parseLong(args[1]);
                    for (AuctionItem ai : plugin.getAuctionManager().getActiveAuctions()) {
                        if (ai.getId() == id) {
                            list.add(String.valueOf(ai.getNextMinimumBid()));
                            break;
                        }
                    }
                } catch (Exception ignored) {}
                return list;
            }
        }

        return Collections.emptyList();
    }
}
