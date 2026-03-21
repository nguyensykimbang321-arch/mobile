package com.appad.models;

import com.google.gson.annotations.SerializedName;

public class Transaction {
    @SerializedName("transaction_id")
    private Integer transactionId;

    @SerializedName("amount")
    private Double amount;

    @SerializedName("type")
    private String type;

    @SerializedName("status")
    private String status;

    @SerializedName("user_id")
    private Integer userId;

    @SerializedName("target_id")
    private Long targetId;

    @SerializedName("created_at")
    private String createdAt;

    public Integer getTransactionId() { return transactionId; }
    public Integer getUserId() { return userId; }
    public Long getTargetId() { return targetId; }
    public Double getAmount() { return amount; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}
