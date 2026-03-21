package com.appad.utils;

public class FormatUtils {

    public static String formatCount(Long count) {
        if (count == null) return "0";
        if (count < 1000) return String.valueOf(count);
        
        if (count < 1000000) {
            double k = count / 1000.0;
            if (count % 1000 == 0 || k >= 100) {
                return String.format("%.0fK", k);
            }
            return String.format("%.1fK", k).replace(".0", "");
        } else {
            double m = count / 1000000.0;
            if (count % 1000000 == 0 || m >= 100) {
                return String.format("%.0fM", m);
            }
            return String.format("%.1fM", m).replace(".0", "");
        }
    }

    public static String formatCount(Integer count) {
        if (count == null) return "0";
        return formatCount(count.longValue());
    }
}
