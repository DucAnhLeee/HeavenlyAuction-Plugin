package com.heavenlyauction.config;

import com.heavenlyauction.HeavenlyAuction;
import com.heavenlyauction.utils.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LanguageManager {
    private final HeavenlyAuction plugin;
    private FileConfiguration langConfig;
    private File langFile;

    public LanguageManager(HeavenlyAuction plugin) {
        this.plugin = plugin;
    }

    public void load() {
        if (langFile == null) {
            langFile = new File(plugin.getDataFolder(), "lang.yml");
        }
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);

        try {
            InputStream defConfigStream = plugin.getResource("lang.yml");
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
                langConfig.setDefaults(defConfig);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Không thể nạp lang.yml mặc định: " + e.getMessage());
        }
    }

    public String getRaw(String path, String def) {
        return langConfig != null ? langConfig.getString(path, def) : def;
    }

    public String getMessage(String path) {
        String msg = getRaw(path, "&c[Thiếu ngôn ngữ: " + path + "]");
        String prefix = getRaw("messages.prefix", "&6&lVạn Bảo Lâu &8» &f");
        msg = msg.replace("%prefix%", prefix);
        return ColorUtil.colorize(msg);
    }

    public String getMessage(String path, Object... replacements) {
        String msg = getMessage(path);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length && replacements[i] != null) {
                msg = msg.replace(String.valueOf(replacements[i]), String.valueOf(replacements[i + 1]));
            }
        }
        return msg;
    }
}
