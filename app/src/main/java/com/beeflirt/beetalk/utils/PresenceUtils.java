package com.beeflirt.beetalk.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class PresenceUtils {

    public static String getStatusText(int isOnline, String lastSeen) {
        if (isOnline == 1) {
            return "Active now";
        }
        if (lastSeen == null || lastSeen.isEmpty()) {
            return "Offline";
        }
        return "Active " + getTimeAgo(lastSeen);
    }

    public static boolean isOnline(int isOnline) {
        return isOnline == 1;
    }

    private static String getTimeAgo(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(dateString);
            if (date == null) return "";

            long diffMs = System.currentTimeMillis() - date.getTime();
            long diffSec = diffMs / 1000;
            long diffMin = diffSec / 60;
            long diffHour = diffMin / 60;
            long diffDay = diffHour / 24;

            if (diffMin < 1) return "just now";
            if (diffMin < 60) return diffMin + "m ago";
            if (diffHour < 24) return diffHour + "h ago";
            if (diffDay < 7) return diffDay + "d ago";
            return diffDay / 7 + "w ago";
        } catch (ParseException e) {
            return "";
        }
    }
}
