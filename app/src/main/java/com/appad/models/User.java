package com.appad.models;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName(value = "user_id", alternate = "userId")
    private Integer userId;
    private String username;
    private String email;
    @SerializedName(value = "full_name", alternate = "fullName")
    private String fullName;
    @SerializedName(value = "avatar_url", alternate = "avatarUrl")
    private String avatarUrl;
    private String role;
    private String token;
    private Double balance;
    @SerializedName("is_premium")
    private Integer isPremium;
    @SerializedName("premium_expiry")
    private String premiumExpiry;

    // Getters
    public Integer getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getRole() { return role; }
    public String getToken() { return token; }
    public Double getBalance() { return balance; }
    public Integer getIsPremium() { return isPremium; }
    public String getPremiumExpiry() { return premiumExpiry; }
}
