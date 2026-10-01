package com.heavenlyauction.economy;

import com.heavenlyauction.HeavenlyAuction;
import com.heavenlyauction.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.UUID;

public class EconomyManager {
    private final HeavenlyAuction plugin;

    // Vault
    private Object vaultEconomy = null;
    private Method vaultWithdraw = null;
    private Method vaultDeposit = null;
    private Method vaultHas = null;

    // PlayerPoints
    private Object playerPointsAPI = null;
    private Method ppTake = null;
    private Method ppGive = null;
    private Method ppLook = null;

    // CoinsEngine
    private boolean coinsEngineAvailable = false;
    private Method ceTake = null;
    private Method ceGive = null;
    private Method ceGet = null;

    private final DecimalFormat formatter = new DecimalFormat("#,###.##");

    public EconomyManager(HeavenlyAuction plugin) {
        this.plugin = plugin;
    }

    public void init() {
        initVault();
        initPlayerPoints();
        initCoinsEngine();
    }

    private void initVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            try {
                Class<?> classEconomy = Class.forName("net.milkbowl.vault.economy.Economy");
                RegisteredServiceProvider<?> rsp = Bukkit.getServer().getServicesManager().getRegistration(classEconomy);
                if (rsp != null) {
                    vaultEconomy = rsp.getProvider();
                    vaultWithdraw = classEconomy.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
                    vaultDeposit = classEconomy.getMethod("depositPlayer", OfflinePlayer.class, double.class);
                    vaultHas = classEconomy.getMethod("has", OfflinePlayer.class, double.class);
                    plugin.getLogger().info("Đã kết nối thành công với Vault (Bạc)!");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Không thể khởi tạo Vault: " + e.getMessage());
            }
        }
    }

    private void initPlayerPoints() {
        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") != null) {
            try {
                Class<?> ppAPIClass = Class.forName("org.black_ixx.playerpoints.PlayerPointsAPI");
                Object ppPlugin = Bukkit.getPluginManager().getPlugin("PlayerPoints");
                Method getAPIMethod = ppPlugin.getClass().getMethod("getAPI");
                playerPointsAPI = getAPIMethod.invoke(ppPlugin);

                ppTake = ppAPIClass.getMethod("take", UUID.class, int.class);
                ppGive = ppAPIClass.getMethod("give", UUID.class, int.class);
                ppLook = ppAPIClass.getMethod("look", UUID.class);
                plugin.getLogger().info("Đã kết nối thành công với PlayerPoints (Linh Thạch)!");
            } catch (Exception e) {
                plugin.getLogger().warning("Không thể khởi tạo PlayerPoints: " + e.getMessage());
            }
        }
    }

    private void initCoinsEngine() {
        if (Bukkit.getPluginManager().getPlugin("CoinsEngine") != null) {
            try {
                Class<?> ceAPIClass = Class.forName("su.nightexpress.coinsengine.api.CoinsEngineAPI");
                ceTake = ceAPIClass.getMethod("removeBalance", UUID.class, String.class, double.class);
                ceGive = ceAPIClass.getMethod("addBalance", UUID.class, String.class, double.class);
                ceGet = ceAPIClass.getMethod("getBalance", UUID.class, String.class);
                coinsEngineAvailable = true;
                plugin.getLogger().info("Đã kết nối thành công với CoinsEngine (Xu Hệ Thống)!");
            } catch (Exception e) {
                plugin.getLogger().warning("Không thể khởi tạo CoinsEngine qua API: " + e.getMessage());
            }
        }
    }

    public boolean has(UUID playerUuid, double amount, String currencyId) {
        String cur = currencyId.toLowerCase();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);

        if ("eco".equalsIgnoreCase(cur) && vaultEconomy != null) {
            try {
                return (boolean) vaultHas.invoke(vaultEconomy, offlinePlayer, amount);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("points".equalsIgnoreCase(cur) && playerPointsAPI != null) {
            try {
                int bal = (int) ppLook.invoke(playerPointsAPI, playerUuid);
                return bal >= amount;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("xuhethong".equalsIgnoreCase(cur)) {
            if (coinsEngineAvailable && ceGet != null) {
                try {
                    ConfigManager.CurrencyConfig cCfg = plugin.getConfigManager().getCurrency("xuhethong");
                    String cName = cCfg != null ? cCfg.getCoinsEngineName() : "xuhethong";
                    double bal = (double) ceGet.invoke(null, playerUuid, cName);
                    return bal >= amount;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public boolean withdraw(UUID playerUuid, double amount, String currencyId) {
        if (!has(playerUuid, amount, currencyId)) {
            return false;
        }

        String cur = currencyId.toLowerCase();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);

        if ("eco".equalsIgnoreCase(cur) && vaultEconomy != null) {
            try {
                Object resp = vaultWithdraw.invoke(vaultEconomy, offlinePlayer, amount);
                Method isSuccess = resp.getClass().getMethod("transactionSuccess");
                return (boolean) isSuccess.invoke(resp);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("points".equalsIgnoreCase(cur) && playerPointsAPI != null) {
            try {
                return (boolean) ppTake.invoke(playerPointsAPI, playerUuid, (int) amount);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("xuhethong".equalsIgnoreCase(cur)) {
            if (coinsEngineAvailable && ceTake != null) {
                try {
                    ConfigManager.CurrencyConfig cCfg = plugin.getConfigManager().getCurrency("xuhethong");
                    String cName = cCfg != null ? cCfg.getCoinsEngineName() : "xuhethong";
                    ceTake.invoke(null, playerUuid, cName, amount);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if (offlinePlayer.isOnline()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "coinsengine balance remove " + offlinePlayer.getName() + " xuhethong " + amount);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean deposit(UUID playerUuid, double amount, String currencyId) {
        String cur = currencyId.toLowerCase();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);

        if ("eco".equalsIgnoreCase(cur) && vaultEconomy != null) {
            try {
                Object resp = vaultDeposit.invoke(vaultEconomy, offlinePlayer, amount);
                Method isSuccess = resp.getClass().getMethod("transactionSuccess");
                return (boolean) isSuccess.invoke(resp);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("points".equalsIgnoreCase(cur) && playerPointsAPI != null) {
            try {
                return (boolean) ppGive.invoke(playerPointsAPI, playerUuid, (int) amount);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("xuhethong".equalsIgnoreCase(cur)) {
            if (coinsEngineAvailable && ceGive != null) {
                try {
                    ConfigManager.CurrencyConfig cCfg = plugin.getConfigManager().getCurrency("xuhethong");
                    String cName = cCfg != null ? cCfg.getCoinsEngineName() : "xuhethong";
                    ceGive.invoke(null, playerUuid, cName, amount);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if (offlinePlayer.isOnline()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "coinsengine balance add " + offlinePlayer.getName() + " xuhethong " + amount);
                    return true;
                }
            }
        }
        return false;
    }

    public String formatAmount(double amount) {
        if (amount == (long) amount) {
            return String.format("%,d", (long) amount);
        }
        return formatter.format(amount);
    }

    public String getCurrencyName(String currencyId) {
        ConfigManager.CurrencyConfig cfg = plugin.getConfigManager().getCurrency(currencyId);
        return cfg != null ? cfg.getDisplayName() : currencyId;
    }

    public String format(double amount, String currencyId) {
        return formatAmount(amount) + " " + getCurrencyName(currencyId);
    }
}
