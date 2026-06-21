package com.appad.utils;

public class ImageUrlUtils {
    private static final String BASE_URL = "https://backend-production-5301.up.railway.app";

    public static String fixUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // Handle localhost and 127.0.0.1
        if (url.startsWith("http://localhost:5000")) {
            return url.replace("http://localhost:5000", BASE_URL);
        }
        if (url.startsWith("http://127.0.0.1:5000")) {
            return url.replace("http://127.0.0.1:5000", BASE_URL);
        }
        
        // Handle local paths or content URIs
        if (url.startsWith("/storage") || url.startsWith("/data") || url.startsWith("content://") || url.startsWith("file://")) {
            return url;
        }

        // Handle relative paths
        if (url.startsWith("/")) {
            return BASE_URL + url;
        }
        if (!url.startsWith("http")) {
            return BASE_URL + "/" + url;
        }
        
        return url;
    }
}
