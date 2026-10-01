package com.heavenlyauction.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class TimeUtil {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private TimeUtil() {}

    public static String formatDuration(long seconds) {
        if (seconds <= 0) {
            return "Hết hạn";
        }

        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append(" ngày ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append(" giờ ");
        }
        if (minutes > 0 || (hours > 0 && days == 0)) {
            sb.append(minutes).append(" phút ");
        }
        if (days == 0 && hours == 0) {
            sb.append(secs).append(" giây");
        }

        return sb.toString().trim();
    }

    public static String formatTimestamp(long millis) {
        return DATE_FORMAT.format(new Date(millis));
    }
}
