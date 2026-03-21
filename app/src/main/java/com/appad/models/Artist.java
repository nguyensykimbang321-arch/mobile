package com.appad.models;

import com.google.gson.annotations.SerializedName;

public class Artist {
    @SerializedName(value = "artist_id", alternate = "artistId")
    private Integer artistId;
    private String name;
    private String bio;
    @SerializedName(value = "image_url", alternate = "imageUrl")
    private String imageUrl;
    private String country;
    @SerializedName(value = "membership_price", alternate = "membershipPrice")
    private Double membershipPrice;
    @SerializedName(value = "membership_duration_days", alternate = "membershipDurationDays")
    private Integer membershipDurationDays;
    
    @SerializedName(value = "song_count", alternate = {"songCount", "songs_count", "songsCount"})
    private Integer songCount;

    @SerializedName(value = "album_count", alternate = {"albumCount", "albums_count", "albumsCount"})
    private Integer albumCount;

    // Getters and Setters
    public Integer getArtistId() { return artistId; }
    public void setArtistId(Integer artistId) { this.artistId = artistId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public Double getMembershipPrice() { return membershipPrice; }
    public void setMembershipPrice(Double membershipPrice) { this.membershipPrice = membershipPrice; }
    public Integer getMembershipDurationDays() { return membershipDurationDays != null ? membershipDurationDays : 30; }
    public void setMembershipDurationDays(Integer membershipDurationDays) { this.membershipDurationDays = membershipDurationDays; }
    public Integer getSongCount() { return songCount != null ? songCount : 0; }
    public void setSongCount(Integer songCount) { this.songCount = songCount; }
    public Integer getAlbumCount() { return albumCount != null ? albumCount : 0; }
    public void setAlbumCount(Integer albumCount) { this.albumCount = albumCount; }
}
