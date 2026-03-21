package com.appad.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class ArtistMembership implements Serializable {
    @SerializedName(value = "membership_id", alternate = "membershipId")
    private Long membershipId;
    @SerializedName(value = "user_id", alternate = "userId")
    private Integer userId;
    @SerializedName(value = "artist_id", alternate = "artistId")
    private Integer artistId;
    @SerializedName(value = "price_paid", alternate = "pricePaid")
    private Double pricePaid;
    @SerializedName(value = "start_date", alternate = "startDate")
    private String startDate;
    @SerializedName(value = "expiry_date", alternate = "expiryDate")
    private String expiryDate;
    private String status;
    private Artist artist;

    public Long getMembershipId() { return membershipId; }
    public void setMembershipId(Long membershipId) { this.membershipId = membershipId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getArtistId() { return artistId; }
    public void setArtistId(Integer artistId) { this.artistId = artistId; }

    public Double getPricePaid() { return pricePaid; }
    public void setPricePaid(Double pricePaid) { this.pricePaid = pricePaid; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Artist getArtist() { return artist; }
    public void setArtist(Artist artist) { this.artist = artist; }
}
