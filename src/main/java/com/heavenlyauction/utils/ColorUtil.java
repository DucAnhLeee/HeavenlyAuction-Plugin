package com.heavenlyauction.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {
    private static final Pattern HEX_PATTERN = Pattern.compile("(&#[a-fA-F0-9]{6}|<#[a-fA-F0-9]{6}>|#[a-fA-F0-9]{6})");

    private ColorUtil() {}

    public static String colorize(String message) {
        if (message == null || message.isEmpty()) return "";

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group(1);
            String hexCode = match.replace("&", "").replace("<", "").replace(">", "");
            try {
                ChatColor color = ChatColor.of(hexCode);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(color.toString()));
            } catch (Exception e) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(match));
            }
        }
        matcher.appendTail(sb);

        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public static String stripColor(String message) {
        if (message == null) return "";
        return ChatColor.stripColor(colorize(message));
    }
}
