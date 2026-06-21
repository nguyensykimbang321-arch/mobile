package com.appad.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.appad.models.User;
import com.google.gson.Gson;
import java.util.Map;

public class SessionManager {
    private static final String PREF_NAME = "AppadSession";
    private static final String KEY_USER = "user_json";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_TOKEN = "jwt_token";
    
    private static SessionManager instance;
    private SharedPreferences sharedPreferences;
    private Context context;

    private SessionManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            if (context == null) {
                // Try to use application instance if available
                context = com.appad.MusicApplication.getInstance();
            }
            if (context != null) {
                instance = new SessionManager(context.getApplicationContext());
            } else {
                return null; // Must handle null in caller
            }
        }
        return instance;
    }

    public void saveUser(Map<String, Object> userData) {
        if (userData == null) return;
        
        // Clean map values: convert Double numbers representing integers back to Integer
        // to prevent Gson serialization of Double (e.g. 1.0) causing NumberFormatException later.
        Map<String, Object> cleanData = new java.util.HashMap<>();
        for (Map.Entry<String, Object> entry : userData.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof Number) {
                double d = ((Number) val).doubleValue();
                if (d == (int) d) {
                    cleanData.put(entry.getKey(), (int) d);
                } else {
                    cleanData.put(entry.getKey(), d);
                }
            } else {
                cleanData.put(entry.getKey(), val);
            }
        }

        Gson gson = new Gson();
        String json = gson.toJson(cleanData);
        sharedPreferences.edit().putString(KEY_USER, json).apply();
        
        // Save ID separately for easy access
        if (cleanData.containsKey("userId")) {
            saveId(cleanData.get("userId"));
        } else if (cleanData.containsKey("user_id")) {
            saveId(cleanData.get("user_id"));
        }

        // Save Token
        if (cleanData.containsKey("token")) {
            String token = String.valueOf(cleanData.get("token"));
            sharedPreferences.edit().putString(KEY_TOKEN, token).apply();
        }
    }
    
    private void saveId(Object idObj) {
        if (idObj == null) return;
        try {
            int id;
            if (idObj instanceof Number) {
                id = ((Number) idObj).intValue();
            } else {
                id = Integer.parseInt(String.valueOf(idObj));
            }
            sharedPreferences.edit().putInt(KEY_USER_ID, id).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Integer getUserId() {
        try {
            // Safe read even if type changed
            Map<String, ?> all = sharedPreferences.getAll();
            Object idVal = all.get(KEY_USER_ID);
            if (idVal instanceof Number) {
                return ((Number) idVal).intValue();
            }
            if (idVal instanceof String) {
                return Integer.parseInt((String) idVal);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    public User getUser() {
        String json = sharedPreferences.getString(KEY_USER, null);
        if (json == null) return null;
        Gson gson = new Gson();
        try {
            // Try standard deserialization
            return gson.fromJson(json, com.appad.models.User.class);
        } catch (Exception e) {
            android.util.Log.e("SessionManager", "Error parsing user JSON, attempting number normalization recovery", e);
            try {
                // Recovery: parse as generic Map, normalize numbers, serialize and parse to User
                Map<String, Object> map = gson.fromJson(json, Map.class);
                Map<String, Object> cleanMap = new java.util.HashMap<>();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    Object val = entry.getValue();
                    if (val instanceof Number) {
                        double d = ((Number) val).doubleValue();
                        if (d == (int) d) {
                            cleanMap.put(entry.getKey(), (int) d);
                        } else {
                            cleanMap.put(entry.getKey(), d);
                        }
                    } else {
                        cleanMap.put(entry.getKey(), val);
                    }
                }
                String cleanJson = gson.toJson(cleanMap);
                return gson.fromJson(cleanJson, com.appad.models.User.class);
            } catch (Exception ex) {
                android.util.Log.e("SessionManager", "Failed normalization recovery", ex);
                return null;
            }
        }
    }

    public String getUserFullName() {
        User user = getUser();
        if (user != null && user.getFullName() != null) {
            return user.getFullName();
        }
        return null;
    }

    public String getUsername() {
        User user = getUser();
        if (user != null) {
            return user.getUsername();
        }
        return null;
    }

    public String getRole() {
        User user = getUser();
        if (user != null) {
            return user.getRole();
        }
        return null;
    }

    public boolean isLoggedIn() {
        return sharedPreferences.contains(KEY_USER_ID);
    }

    public void logout() {
        // Stop music and record history
        try {
            MusicPlayerManager.getInstance().stopMusic();
        } catch (Exception e) {
            android.util.Log.e("SessionManager", "Error stopping music on logout", e);
        }
        
        sharedPreferences.edit().clear().apply();
    }
}
