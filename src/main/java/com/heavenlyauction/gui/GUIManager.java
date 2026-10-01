package com.heavenlyauction.gui;

import com.heavenlyauction.HeavenlyAuction;
import com.heavenlyauction.database.model.AuctionItem;
import com.heavenlyauction.database.model.BountyItem;
import com.heavenlyauction.database.model.MailboxItem;
import com.heavenlyauction.database.model.MarketItem;
import com.heavenlyauction.utils.ColorUtil;
import com.heavenlyauction.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GUIManager implements Listener {
    private final HeavenlyAuction plugin;
    private final Map<UUID, QuickSellSession> quickSellSessions = new ConcurrentHashMap<>();

    public static class QuickSellSession {
        private final ItemStack item;
        private double price;
        private String currency;

        public QuickSellSession(ItemStack item, double price, String currency) {
            this.item = item;
            this.price = price;
            this.currency = currency;
        }

        public ItemStack getItem() { return item; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }

    public GUIManager(HeavenlyAuction plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // 1. MAIN MENU (TIÊN CÁC TRUNG TÂM)
    // ==========================================
    public void openMainMenu(Player player) {
        HeavenlyHolder holder = new HeavenlyHolder(GUIType.MAIN_MENU, 0);
        Inventory inv = Bukkit.createInventory(holder, 27, ColorUtil.colorize("&6&lVẠN BẢO LÂU &8- &7Tiên Các"));
        holder.setInventory(inv);

        ItemStack border = createBorder();
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }

        // Slot 10: Chợ Trời (Market)
        ItemStack marketItem = new ItemStack(Material.CHEST);
        ItemMeta mMeta = marketItem.getItemMeta();
        if (mMeta != null) {
            mMeta.setDisplayName(ColorUtil.colorize("&6&lCHỢ TRỜ TU TIÊN"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.colorize("&7Khu giao thương tự do giữa các tu sĩ."));
            lore.add(ColorUtil.colorize("&7Hỗ trợ: &aBạc&7, &bLinh Thạch&7, &6Xu Hệ Thống&7."));
            lore.add("");
            lore.add(ColorUtil.colorize("&a&l[!] &eNhấp để vào Chợ Trời!"));
            mMeta.setLore(lore);
            marketItem.setItemMeta(mMeta);
        }
        inv.setItem(10, marketItem);

        // Slot 12: Huyền Thưởng Lệnh (Bounties)
        ItemStack bountyItem = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta bMeta = bountyItem.getItemMeta();
        if (bMeta != null) {
            bMeta.setDisplayName(ColorUtil.colorize("&e&lHUYỀN THƯỞNG LỆNH"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.colorize("&7Treo thưởng tìm kiếm nguyên liệu, bảo vật."));
            lore.add(ColorUtil.colorize("&7Giao nộp đúng vật phẩm để nhận thưởng lớn!"));
            lore.add("");
            lore.add(ColorUtil.colorize("&e&l[!] &eNhấp để xem bảng Huyền Thưởng!"));
            bMeta.setLore(lore);
            bountyItem.setItemMeta(bMeta);
        }
        inv.setItem(12, bountyItem);

        // Slot 14: Thiên Bảo Đấu Giá (Auctions)
        ItemStack auctionItem = new ItemStack(Material.BEACON);
        ItemMeta aMeta = auctionItem.getItemMeta();
        if (aMeta != null) {
            aMeta.setDisplayName(ColorUtil.colorize("&d&lTHIÊN BẢO ĐẤU GIÁ"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.colorize("&7Đấu giá thần binh, pháp bảo quý hiếm."));
            lore.add(ColorUtil.colorize("&eĐấu giá hệ thống mỗi 3 giờ!"));
            lore.add(ColorUtil.colorize("&5Đặc biệt: Cổ Vật Ẩn Vị lúc 20:00 hàng ngày!"));
            lore.add("");
            lore.add(ColorUtil.colorize("&d&l[!] &eNhấp để tham gia Đấu Giá!"));
            aMeta.setLore(lore);
            auctionItem.setItemMeta(aMeta);
        }
        inv.setItem(14, auctionItem);

        // Slot 16: Càn Khôn Đãi (Hòm Thư Tín)
        ItemStack mailboxItem = new ItemStack(Material.ENDER_CHEST);
        ItemMeta mbMeta = mailboxItem.getItemMeta();
        if (mbMeta != null) {
            mbMeta.setDisplayName(ColorUtil.colorize("&b&lCÀN KHÔN ĐÃI &7(Hòm Thư Tín)"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.colorize("&7Nơi lưu giữ bảo vật đã mua, tiền bán đồ,"));
            lore.add(ColorUtil.colorize("&7phần thưởng Huyền Thưởng và đồ quá hạn."));
            lore.add("");
            lore.add(ColorUtil.colorize("&b&l[!] &eNhấp để nhận bảo vật & tiền tệ!"));
            mbMeta.setLore(lore);
            mailboxItem.setItemMeta(mbMeta);
        }
        inv.setItem(16, mailboxItem);

        player.openInventory(inv);
    }

    // ==========================================
    // 2. MARKET GUI (VẠN BẢO LÂU - CHỢ TRỜ)
    // ==========================================
    public void openMarket(Player player, int page, boolean myItemsOnly) {
        HeavenlyHolder holder = new HeavenlyHolder(myItemsOnly ? GUIType.MY_MARKET : GUIType.MARKET, page);
        String title = myItemsOnly ? "&6Bảo Vật Của Bạn" : "&6Vạn Bảo Lâu &7(Trang " + (page + 1) + ")";
        Inventory inv = Bukkit.createInventory(holder, 54, ColorUtil.colorize(title));
        holder.setInventory(inv);

        List<MarketItem> allItems = myItemsOnly ?
                plugin.getMarketManager().getPlayerItems(player.getUniqueId()) :
                plugin.getMarketManager().getActiveItems();

        int maxItemsPerPage = 45;
        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, allItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            MarketItem mItem = allItems.get(i);
            int slot = i - startIndex;

            ItemStack display = mItem.getItem();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(ColorUtil.colorize("&8&m-----------------------------"));
                lore.add(ColorUtil.colorize("&7Người bán: &f" + mItem.getSellerName()));
                lore.add(ColorUtil.colorize("&7Giá bán: &e" + plugin.getEconomyManager().format(mItem.getPrice(), mItem.getCurrency())));
                long timeLeft = (mItem.getExpireAt() - System.currentTimeMillis()) / 1000L;
                lore.add(ColorUtil.colorize("&7Còn lại: &f" + TimeUtil.formatDuration(timeLeft)));
                lore.add("");
                if (mItem.getSellerUuid().equals(player.getUniqueId())) {
                    lore.add(ColorUtil.colorize("&c&l[!] &cNhấp để GỠ VẬT PHẨM & thu hồi"));
                } else {
                    lore.add(ColorUtil.colorize("&a&l[!] &eNhấp để MUA NGAY"));
                }
                lore.add(ColorUtil.colorize("&8&m-----------------------------"));
                meta.setLore(lore);
                display.setItemMeta(meta);
            }

            inv.setItem(slot, display);
            holder.setActionId(slot, mItem.getId());
        }

        // Toolbar
        ItemStack border = createBorder();
        for (int s = 45; s < 54; s++) {
            inv.setItem(s, border);
        }

        // Slot 45: Trang trước
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pMeta = prev.getItemMeta();
            if (pMeta != null) {
                pMeta.setDisplayName(ColorUtil.colorize("&a« Trang Trước"));
                prev.setItemMeta(pMeta);
            }
            inv.setItem(45, prev);
        }

        // Slot 47: Nút Đăng Bán Nhanh
        ItemStack quickSell = new ItemStack(Material.GOLD_INGOT);
        ItemMeta qsMeta = quickSell.getItemMeta();
        if (qsMeta != null) {
            qsMeta.setDisplayName(ColorUtil.colorize("&a&l[+] ĐĂNG BÁN NHANH"));
            List<String> qsLore = new ArrayList<>();
            qsLore.add(ColorUtil.colorize("&7Mở cửa sổ chọn giá nhanh"));
            qsLore.add(ColorUtil.colorize("&7cho vật phẩm đang cầm trên tay chính."));
            qsLore.add("");
            qsLore.add(ColorUtil.colorize("&a&l[!] &eNhấp để đăng bán ngay!"));
            qsMeta.setLore(qsLore);
            quickSell.setItemMeta(qsMeta);
        }
        inv.setItem(47, quickSell);

        // Slot 48: Chuyển đổi xem đồ của tôi / toàn bộ
        ItemStack filter = new ItemStack(myItemsOnly ? Material.CHEST : Material.PLAYER_HEAD);
        ItemMeta fMeta = filter.getItemMeta();
        if (fMeta != null) {
            fMeta.setDisplayName(ColorUtil.colorize(myItemsOnly ? "&e« Xem Toàn Bộ Chợ" : "&eXem Bảo Vật Của Tôi"));
            filter.setItemMeta(fMeta);
        }
        inv.setItem(48, filter);

        // Slot 49: Trở về Menu chính
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta bMeta = back.getItemMeta();
        if (bMeta != null) {
            bMeta.setDisplayName(ColorUtil.colorize("&c« Quay Lại Tiên Các"));
            back.setItemMeta(bMeta);
        }
        inv.setItem(49, back);

        // Slot 53: Trang sau
        if (endIndex < allItems.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nMeta = next.getItemMeta();
            if (nMeta != null) {
                nMeta.setDisplayName(ColorUtil.colorize("&aTrang Sau »"));
                next.setItemMeta(nMeta);
            }
            inv.setItem(53, next);
        }

        player.openInventory(inv);
    }

    // ==========================================
    // 2.1. QUICK SELL WINDOW (CỬA SỔ CHỌN GIÁ NHANH)
    // ==========================================
    public void openQuickSell(Player player, double initialPrice, String initialCurrency) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.market.sell-no-item"));
            return;
        }

        QuickSellSession session = new QuickSellSession(hand.clone(), Math.max(10, initialPrice), initialCurrency);
        quickSellSessions.put(player.getUniqueId(), session);
        renderQuickSell(player, session);
    }

    private void renderQuickSell(Player player, QuickSellSession session) {
        HeavenlyHolder holder = new HeavenlyHolder(GUIType.QUICK_SELL, 0);
        Inventory inv = Bukkit.createInventory(holder, 54, ColorUtil.colorize("&6&lĐĂNG BÁN NHANH &8- &7Chọn Giá"));
        holder.setInventory(inv);

        ItemStack border = createBorder();
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, border);
        }

        double price = session.getPrice();
        String cur = session.getCurrency();
        double tax = plugin.getConfigManager().getCurrencyTaxPercent(cur);
        double taxAmount = price * (tax / 100.0);
        double net = price - taxAmount;

        // Slot 13: Vật phẩm xem trước
        ItemStack preview = session.getItem().clone();
        ItemMeta pMeta = preview.getItemMeta();
        if (pMeta != null) {
            List<String> lore = pMeta.hasLore() ? new ArrayList<>(pMeta.getLore()) : new ArrayList<>();
            lore.add(ColorUtil.colorize("&8&m-----------------------------"));
            lore.add(ColorUtil.colorize("&7Số lượng bán: &f" + preview.getAmount()));
            lore.add(ColorUtil.colorize("&7Giá niêm yết: &e" + plugin.getEconomyManager().format(price, cur)));
            lore.add(ColorUtil.colorize("&7Thuế sàn (" + tax + "%): &c-" + plugin.getEconomyManager().format(taxAmount, cur)));
            lore.add(ColorUtil.colorize("&7Thực nhận vào hòm: &a" + plugin.getEconomyManager().format(net, cur)));
            lore.add(ColorUtil.colorize("&8&m-----------------------------"));
            pMeta.setLore(lore);
            preview.setItemMeta(pMeta);
        }
        inv.setItem(13, preview);

        // Fast increment adjustments
        inv.setItem(20, createButton(Material.EMERALD, "&a&l+100", Arrays.asList("&7Tăng thêm &a100 &7vào giá bán.")));
        inv.setItem(21, createButton(Material.EMERALD, "&a&l+1,000", Arrays.asList("&7Tăng thêm &a1,000 &7vào giá bán.")));
        inv.setItem(22, createButton(Material.EMERALD_BLOCK, "&a&l+10,000", Arrays.asList("&7Tăng thêm &a10,000 &7vào giá bán.")));
        inv.setItem(23, createButton(Material.DIAMOND, "&a&l+50,000", Arrays.asList("&7Tăng thêm &a50,000 &7vào giá bán.")));
        inv.setItem(24, createButton(Material.DIAMOND_BLOCK, "&a&l+100,000", Arrays.asList("&7Tăng thêm &a100,000 &7vào giá bán.")));

        // Fast decrement adjustments
        inv.setItem(28, createButton(Material.IRON_INGOT, "&b&lĐặt lại 1,000", Arrays.asList("&7Khôi phục giá về mức cơ bản &b1,000&7.")));
        inv.setItem(29, createButton(Material.REDSTONE, "&c&l-100", Arrays.asList("&7Giảm bớt &c100 &7khỏi giá bán.")));
        inv.setItem(30, createButton(Material.REDSTONE, "&c&l-1,000", Arrays.asList("&7Giảm bớt &c1,000 &7khỏi giá bán.")));
        inv.setItem(31, createButton(Material.REDSTONE_BLOCK, "&c&l-10,000", Arrays.asList("&7Giảm bớt &c10,000 &7khỏi giá bán.")));
        inv.setItem(32, createButton(Material.GOLD_INGOT, "&e&lNhân Đôi (x2)", Arrays.asList("&7Gấp đôi mức giá hiện tại.")));
        inv.setItem(33, createButton(Material.GOLD_NUGGET, "&e&lChia Đôi (/2)", Arrays.asList("&7Giảm một nửa mức giá hiện tại.")));

        // Slot 38: Đổi Loại Tiền Tệ
        inv.setItem(38, createButton(Material.NETHER_STAR, "&f&lLoại Tiền: " + plugin.getEconomyManager().getCurrencyName(cur),
                Arrays.asList("&7Loại tiền hiện tại: &e" + plugin.getEconomyManager().getCurrencyName(cur),
                        "&7Thuế giao dịch: &c" + tax + "%",
                        "",
                        "&e&l[!] &fNhấp để đổi sang loại tiền khác!")));

        // Slot 40: Xác nhận bán
        inv.setItem(40, createButton(Material.LIME_CONCRETE, "&a&l✔ XÁC NHẬN ĐĂNG BÁN",
                Arrays.asList("&7Bán với giá: &e" + plugin.getEconomyManager().format(price, cur),
                        "&7Thuế sàn: &c" + tax + "%",
                        "",
                        "&a&l[!] &fNhấp để đăng bảo vật lên sàn ngay!")));

        // Slot 42: Hủy bỏ
        inv.setItem(42, createButton(Material.RED_CONCRETE, "&c&l✖ HỦY BỎ",
                Arrays.asList("&7Hủy đăng bán và quay lại Chợ Trời.")));

        player.openInventory(inv);
    }

    private ItemStack createButton(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
            List<String> l = new ArrayList<>();
            for (String str : lore) l.add(ColorUtil.colorize(str));
            meta.setLore(l);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ==========================================
    // 3. BOUNTY GUI (HUYỀN THƯỞNG LỆNH)
    // ==========================================
    public void openBounty(Player player, int page, boolean myBountiesOnly) {
        HeavenlyHolder holder = new HeavenlyHolder(myBountiesOnly ? GUIType.MY_BOUNTY : GUIType.BOUNTY, page);
        String title = myBountiesOnly ? "&eHuyền Thưởng Của Bạn" : "&eHuyền Thưởng Lệnh &7(Trang " + (page + 1) + ")";
        Inventory inv = Bukkit.createInventory(holder, 54, ColorUtil.colorize(title));
        holder.setInventory(inv);

        List<BountyItem> allBounties = myBountiesOnly ?
                plugin.getBountyManager().getPlayerBounties(player.getUniqueId()) :
                plugin.getBountyManager().getActiveBounties();

        int maxItemsPerPage = 45;
        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, allBounties.size());

        for (int i = startIndex; i < endIndex; i++) {
            BountyItem bItem = allBounties.get(i);
            int slot = i - startIndex;

            ItemStack display = bItem.getPrototypeItem();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(ColorUtil.colorize("&8&m-----------------------------"));
                lore.add(ColorUtil.colorize("&7Người phát lệnh: &f" + bItem.getCreatorName()));
                lore.add(ColorUtil.colorize("&7Số lượng cần: &b" + bItem.getRequiredAmount() + "x"));
                lore.add(ColorUtil.colorize("&7Tiền thưởng: &a" + plugin.getEconomyManager().format(bItem.getRewardPrice(), bItem.getCurrency())));
                long timeLeft = (bItem.getExpireAt() - System.currentTimeMillis()) / 1000L;
                lore.add(ColorUtil.colorize("&7Hạn chót: &f" + TimeUtil.formatDuration(timeLeft)));
                lore.add("");
                if (bItem.getCreatorUuid().equals(player.getUniqueId())) {
                    lore.add(ColorUtil.colorize("&c&l[!] &cNhấp để HỦY LỆNH và nhận lại tiền"));
                } else {
                    lore.add(ColorUtil.colorize("&a&l[!] &eNhấp để GIAO NỘP và nhận thưởng"));
                }
                lore.add(ColorUtil.colorize("&8&m-----------------------------"));
                meta.setLore(lore);
                display.setItemMeta(meta);
            }

            inv.setItem(slot, display);
            holder.setActionId(slot, bItem.getId());
        }

        // Toolbar
        ItemStack border = createBorder();
        for (int s = 45; s < 54; s++) {
            inv.setItem(s, border);
        }

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pMeta = prev.getItemMeta();
            if (pMeta != null) {
                pMeta.setDisplayName(ColorUtil.colorize("&a« Trang Trước"));
                prev.setItemMeta(pMeta);
            }
            inv.setItem(45, prev);
        }

        ItemStack filter = new ItemStack(myBountiesOnly ? Material.BOOK : Material.PLAYER_HEAD);
        ItemMeta fMeta = filter.getItemMeta();
        if (fMeta != null) {
            fMeta.setDisplayName(ColorUtil.colorize(myBountiesOnly ? "&e« Xem Tất Cả Huyền Thưởng" : "&eHuyền Thưởng Của Tôi"));
            filter.setItemMeta(fMeta);
        }
        inv.setItem(48, filter);

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta bMeta = back.getItemMeta();
        if (bMeta != null) {
            bMeta.setDisplayName(ColorUtil.colorize("&c« Quay Lại Tiên Các"));
            back.setItemMeta(bMeta);
        }
        inv.setItem(49, back);

        if (endIndex < allBounties.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nMeta = next.getItemMeta();
            if (nMeta != null) {
                nMeta.setDisplayName(ColorUtil.colorize("&aTrang Sau »"));
                next.setItemMeta(nMeta);
            }
            inv.setItem(53, next);
        }

        player.openInventory(inv);
    }

    // ==========================================
    // 4. AUCTION GUI (THIÊN BẢO ĐẤU GIÁ)
    // ==========================================
    public void openAuction(Player player, int page) {
        HeavenlyHolder holder = new HeavenlyHolder(GUIType.AUCTION, page);
        Inventory inv = Bukkit.createInventory(holder, 54, ColorUtil.colorize("&dThiên Bảo Đấu Giá &7(Trang " + (page + 1) + ")"));
        holder.setInventory(inv);

        List<AuctionItem> allAuctions = plugin.getAuctionManager().getActiveAuctions();

        int maxItemsPerPage = 45;
        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, allAuctions.size());

        for (int i = startIndex; i < endIndex; i++) {
            AuctionItem ai = allAuctions.get(i);
            int slot = i - startIndex;

            ItemStack display;
            if (ai.isMystery()) {
                display = new ItemStack(plugin.getConfigManager().getMysteryItemMaterial());
                ItemMeta meta = display.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(plugin.getConfigManager().getMysteryItemName());
                    List<String> lore = new ArrayList<>(ai.getMysteryLore());
                    lore.add(ColorUtil.colorize("&8&m-----------------------------"));
                    lore.add(ColorUtil.colorize("&7Giá hiện tại: &e" + plugin.getEconomyManager().format(ai.getCurrentBid(), ai.getCurrency())));
                    lore.add(ColorUtil.colorize("&7Người dẫn đầu: &f" + (ai.getHighestBidderName() != null ? ai.getHighestBidderName() : "Chưa có")));
                    lore.add(ColorUtil.colorize("&7Bước giá kế tiếp: &a" + plugin.getEconomyManager().format(ai.getNextMinimumBid(), ai.getCurrency())));
                    long timeLeft = (ai.getExpireAt() - System.currentTimeMillis()) / 1000L;
                    lore.add(ColorUtil.colorize("&7Thời gian còn lại: &d" + TimeUtil.formatDuration(timeLeft)));
                    lore.add("");
                    lore.add(ColorUtil.colorize("&e&l[!] &eNhấp để RA GIÁ TỐI THIỂU: &a" + plugin.getEconomyManager().format(ai.getNextMinimumBid(), ai.getCurrency())));
                    lore.add(ColorUtil.colorize("&8&m-----------------------------"));
                    meta.setLore(lore);
                    display.setItemMeta(meta);
                }
            } else {
                display = ai.getItem();
                ItemMeta meta = display.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    lore.add(ColorUtil.colorize("&8&m-----------------------------"));
                    lore.add(ColorUtil.colorize("&7Người mở bán: &f" + ai.getSellerName()));
                    lore.add(ColorUtil.colorize("&7Giá hiện tại: &e" + plugin.getEconomyManager().format(ai.getCurrentBid(), ai.getCurrency())));
                    lore.add(ColorUtil.colorize("&7Người dẫn đầu: &f" + (ai.getHighestBidderName() != null ? ai.getHighestBidderName() : "Chưa có")));
                    lore.add(ColorUtil.colorize("&7Bước giá kế tiếp: &a" + plugin.getEconomyManager().format(ai.getNextMinimumBid(), ai.getCurrency())));
                    long timeLeft = (ai.getExpireAt() - System.currentTimeMillis()) / 1000L;
                    lore.add(ColorUtil.colorize("&7Thời gian còn lại: &f" + TimeUtil.formatDuration(timeLeft)));
                    lore.add("");
                    lore.add(ColorUtil.colorize("&e&l[!] &eNhấp để RA GIÁ TỐI THIỂU: &a" + plugin.getEconomyManager().format(ai.getNextMinimumBid(), ai.getCurrency())));
                    lore.add(ColorUtil.colorize("&8&m-----------------------------"));
                    meta.setLore(lore);
                    display.setItemMeta(meta);
                }
            }

            inv.setItem(slot, display);
            holder.setActionId(slot, ai.getId());
        }

        // Toolbar
        ItemStack border = createBorder();
        for (int s = 45; s < 54; s++) {
            inv.setItem(s, border);
        }

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pMeta = prev.getItemMeta();
            if (pMeta != null) {
                pMeta.setDisplayName(ColorUtil.colorize("&a« Trang Trước"));
                prev.setItemMeta(pMeta);
            }
            inv.setItem(45, prev);
        }

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta bMeta = back.getItemMeta();
        if (bMeta != null) {
            bMeta.setDisplayName(ColorUtil.colorize("&c« Quay Lại Tiên Các"));
            back.setItemMeta(bMeta);
        }
        inv.setItem(49, back);

        if (endIndex < allAuctions.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nMeta = next.getItemMeta();
            if (nMeta != null) {
                nMeta.setDisplayName(ColorUtil.colorize("&aTrang Sau »"));
                next.setItemMeta(nMeta);
            }
            inv.setItem(53, next);
        }

        player.openInventory(inv);
    }

    // ==========================================
    // 5. MAILBOX GUI (CÀN KHÔN ĐÃI)
    // ==========================================
    public void openMailbox(Player player, int page) {
        plugin.getMailboxManager().getUnclaimedItems(player.getUniqueId(), items -> {
            HeavenlyHolder holder = new HeavenlyHolder(GUIType.MAILBOX, page);
            Inventory inv = Bukkit.createInventory(holder, 54, ColorUtil.colorize("&bCàn Khôn Đãi &7(Trang " + (page + 1) + ")"));
            holder.setInventory(inv);

            int maxItemsPerPage = 45;
            int startIndex = page * maxItemsPerPage;
            int endIndex = Math.min(startIndex + maxItemsPerPage, items.size());

            for (int i = startIndex; i < endIndex; i++) {
                MailboxItem mItem = items.get(i);
                int slot = i - startIndex;

                ItemStack display;
                if ("MONEY".equalsIgnoreCase(mItem.getType())) {
                    display = new ItemStack(Material.SUNFLOWER);
                    ItemMeta meta = display.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(ColorUtil.colorize("&e&lTiền Tệ: &a" + plugin.getEconomyManager().format(mItem.getAmount(), mItem.getCurrency())));
                        List<String> lore = new ArrayList<>();
                        lore.add(ColorUtil.colorize("&7Lý do: &f" + mItem.getReason()));
                        lore.add(ColorUtil.colorize("&7Thời gian: &f" + TimeUtil.formatTimestamp(mItem.getCreatedAt())));
                        lore.add("");
                        lore.add(ColorUtil.colorize("&a&l[!] &eNhấp để rút tiền vào tài khoản!"));
                        meta.setLore(lore);
                        display.setItemMeta(meta);
                    }
                } else {
                    display = mItem.getItem() != null ? mItem.getItem().clone() : new ItemStack(Material.PAPER);
                    ItemMeta meta = display.getItemMeta();
                    if (meta != null) {
                        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                        lore.add(ColorUtil.colorize("&8&m-----------------------------"));
                        lore.add(ColorUtil.colorize("&7Lý do: &f" + mItem.getReason()));
                        lore.add(ColorUtil.colorize("&7Thời gian: &f" + TimeUtil.formatTimestamp(mItem.getCreatedAt())));
                        lore.add("");
                        lore.add(ColorUtil.colorize("&a&l[!] &eNhấp để lấy bảo vật vào túi đồ!"));
                        lore.add(ColorUtil.colorize("&8&m-----------------------------"));
                        meta.setLore(lore);
                        display.setItemMeta(meta);
                    }
                }

                inv.setItem(slot, display);
                holder.setActionId(slot, mItem.getId());
            }

            // Toolbar
            ItemStack border = createBorder();
            for (int s = 45; s < 54; s++) {
                inv.setItem(s, border);
            }

            if (page > 0) {
                ItemStack prev = new ItemStack(Material.ARROW);
                ItemMeta pMeta = prev.getItemMeta();
                if (pMeta != null) {
                    pMeta.setDisplayName(ColorUtil.colorize("&a« Trang Trước"));
                    prev.setItemMeta(pMeta);
                }
                inv.setItem(45, prev);
            }

            if (endIndex < items.size()) {
                ItemStack next = new ItemStack(Material.ARROW);
                ItemMeta nMeta = next.getItemMeta();
                if (nMeta != null) {
                    nMeta.setDisplayName(ColorUtil.colorize("&aTrang Sau »"));
                    next.setItemMeta(nMeta);
                }
                inv.setItem(53, next);
            }

            ItemStack back = new ItemStack(Material.BARRIER);
            ItemMeta bMeta = back.getItemMeta();
            if (bMeta != null) {
                bMeta.setDisplayName(ColorUtil.colorize("&c« Quay Lại Tiên Các"));
                back.setItemMeta(bMeta);
            }
            inv.setItem(49, back);

            player.openInventory(inv);
        });
    }

    // ==========================================
    // INVENTORY EVENTS
    // ==========================================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getInventory().getHolder() instanceof HeavenlyHolder) {
            event.setCancelled(true);

            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
                return;
            }

            HeavenlyHolder holder = (HeavenlyHolder) event.getInventory().getHolder();
            int slot = event.getSlot();

            switch (holder.getType()) {
                case MAIN_MENU:
                    handleMainMenuClick(player, slot);
                    break;
                case MARKET:
                case MY_MARKET:
                    handleMarketClick(player, holder, slot);
                    break;
                case QUICK_SELL:
                    handleQuickSellClick(player, slot);
                    break;
                case BOUNTY:
                case MY_BOUNTY:
                    handleBountyClick(player, holder, slot);
                    break;
                case AUCTION:
                    handleAuctionClick(player, holder, slot);
                    break;
                case MAILBOX:
                    handleMailboxClick(player, holder, slot);
                    break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof HeavenlyHolder) {
            event.setCancelled(true);
        }
    }

    private void handleMainMenuClick(Player player, int slot) {
        if (slot == 10) {
            openMarket(player, 0, false);
        } else if (slot == 12) {
            openBounty(player, 0, false);
        } else if (slot == 14) {
            openAuction(player, 0);
        } else if (slot == 16) {
            openMailbox(player, 0);
        }
    }

    private void handleMarketClick(Player player, HeavenlyHolder holder, int slot) {
        if (slot == 45 && holder.getPage() > 0) {
            openMarket(player, holder.getPage() - 1, holder.getType() == GUIType.MY_MARKET);
        } else if (slot == 53) {
            openMarket(player, holder.getPage() + 1, holder.getType() == GUIType.MY_MARKET);
        } else if (slot == 47) {
            // Mở cửa sổ Đăng Bán Nhanh
            openQuickSell(player, 1000, "eco");
        } else if (slot == 48) {
            openMarket(player, 0, holder.getType() != GUIType.MY_MARKET);
        } else if (slot == 49) {
            openMainMenu(player);
        } else if (slot < 45) {
            Long actionId = holder.getActionId(slot);
            if (actionId != null) {
                MarketItem mi = plugin.getMarketManager().getItem(actionId);
                // Cho phép gỡ vật phẩm của bản thân ngay cả khi xem toàn bộ chợ!
                if (holder.getType() == GUIType.MY_MARKET || (mi != null && mi.getSellerUuid().equals(player.getUniqueId()))) {
                    plugin.getMarketManager().cancelListing(player, actionId);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> openMarket(player, holder.getPage(), holder.getType() == GUIType.MY_MARKET), 5L);
                } else {
                    plugin.getMarketManager().buyItem(player, actionId);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> openMarket(player, holder.getPage(), false), 5L);
                }
            }
        }
    }

    private void handleQuickSellClick(Player player, int slot) {
        QuickSellSession session = quickSellSessions.get(player.getUniqueId());
        if (session == null) {
            openMarket(player, 0, false);
            return;
        }

        double curPrice = session.getPrice();
        boolean changed = false;

        if (slot == 20) { session.setPrice(curPrice + 100); changed = true; }
        else if (slot == 21) { session.setPrice(curPrice + 1000); changed = true; }
        else if (slot == 22) { session.setPrice(curPrice + 10000); changed = true; }
        else if (slot == 23) { session.setPrice(curPrice + 50000); changed = true; }
        else if (slot == 24) { session.setPrice(curPrice + 100000); changed = true; }
        else if (slot == 28) { session.setPrice(1000); changed = true; }
        else if (slot == 29) { session.setPrice(Math.max(10, curPrice - 100)); changed = true; }
        else if (slot == 30) { session.setPrice(Math.max(10, curPrice - 1000)); changed = true; }
        else if (slot == 31) { session.setPrice(Math.max(10, curPrice - 10000)); changed = true; }
        else if (slot == 32) { session.setPrice(curPrice * 2); changed = true; }
        else if (slot == 33) { session.setPrice(Math.max(10, Math.floor(curPrice / 2))); changed = true; }
        else if (slot == 38) {
            // Cycle currency: eco -> points -> xuhethong -> eco
            String c = session.getCurrency();
            if ("eco".equalsIgnoreCase(c)) session.setCurrency("points");
            else if ("points".equalsIgnoreCase(c)) session.setCurrency("xuhethong");
            else session.setCurrency("eco");
            changed = true;
        } else if (slot == 42) {
            quickSellSessions.remove(player.getUniqueId());
            openMarket(player, 0, false);
            return;
        } else if (slot == 40) {
            // XÁC NHẬN ĐĂNG BÁN
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.market.sell-no-item"));
                player.closeInventory();
                quickSellSessions.remove(player.getUniqueId());
                return;
            }

            ItemStack toSell = hand.clone();
            player.getInventory().setItemInMainHand(null);
            double finalPrice = session.getPrice();
            String finalCur = session.getCurrency();

            quickSellSessions.remove(player.getUniqueId());
            player.closeInventory();

            plugin.getMarketManager().addItem(player, toSell, finalPrice, finalCur, success -> {
                if (success) {
                    player.sendMessage(plugin.getLanguageManager().getMessage("messages.market.sell-success",
                            "%price%", plugin.getEconomyManager().formatAmount(finalPrice),
                            "%currency%", plugin.getEconomyManager().getCurrencyName(finalCur)));
                } else {
                    player.getInventory().addItem(toSell);
                    player.sendMessage(plugin.getLanguageManager().getMessage("messages.prefix") + "§cĐăng bán bảo vật thất bại! Đã hoàn trả lại vật phẩm vào túi đồ.");
                }
            });
            return;
        }

        if (changed) {
            try {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            } catch (Throwable ignored) {}
            renderQuickSell(player, session);
        }
    }

    private void handleBountyClick(Player player, HeavenlyHolder holder, int slot) {
        if (slot == 45 && holder.getPage() > 0) {
            openBounty(player, holder.getPage() - 1, holder.getType() == GUIType.MY_BOUNTY);
        } else if (slot == 53) {
            openBounty(player, holder.getPage() + 1, holder.getType() == GUIType.MY_BOUNTY);
        } else if (slot == 48) {
            openBounty(player, 0, holder.getType() != GUIType.MY_BOUNTY);
        } else if (slot == 49) {
            openMainMenu(player);
        } else if (slot < 45) {
            Long actionId = holder.getActionId(slot);
            if (actionId != null) {
                if (holder.getType() == GUIType.MY_BOUNTY) {
                    plugin.getBountyManager().cancelBounty(player, actionId);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> openBounty(player, holder.getPage(), true), 5L);
                } else {
                    plugin.getBountyManager().fulfillBounty(player, actionId);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> openBounty(player, holder.getPage(), false), 5L);
                }
            }
        }
    }

    private void handleAuctionClick(Player player, HeavenlyHolder holder, int slot) {
        if (slot == 45 && holder.getPage() > 0) {
            openAuction(player, holder.getPage() - 1);
        } else if (slot == 53) {
            openAuction(player, holder.getPage() + 1);
        } else if (slot == 49) {
            openMainMenu(player);
        } else if (slot < 45) {
            Long actionId = holder.getActionId(slot);
            if (actionId != null) {
                for (AuctionItem ai : plugin.getAuctionManager().getActiveAuctions()) {
                    if (ai.getId() == actionId) {
                        plugin.getAuctionManager().placeBid(player, actionId, ai.getNextMinimumBid());
                        Bukkit.getScheduler().runTaskLater(plugin, () -> openAuction(player, holder.getPage()), 5L);
                        break;
                    }
                }
            }
        }
    }

    private void handleMailboxClick(Player player, HeavenlyHolder holder, int slot) {
        if (slot == 45 && holder.getPage() > 0) {
            openMailbox(player, holder.getPage() - 1);
        } else if (slot == 53) {
            openMailbox(player, holder.getPage() + 1);
        } else if (slot == 49) {
            openMainMenu(player);
        } else if (slot < 45) {
            Long actionId = holder.getActionId(slot);
            if (actionId != null) {
                plugin.getMailboxManager().claimItem(player, actionId, success -> {
                    if (success) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> openMailbox(player, holder.getPage()), 5L);
                    }
                });
            }
        }
    }

    private ItemStack createBorder() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize("&7 "));
            item.setItemMeta(meta);
        }
        return item;
    }
}
