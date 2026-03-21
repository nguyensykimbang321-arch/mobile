package com.appad.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Album implements Serializable {
    @SerializedName(value = "album_id", alternate = "albumId")
    private Integer albumId;
    private String title;
    @SerializedName(value = "artist_id", alternate = "artistId")
    private Integer artistId;
    @SerializedName(value = "cover_url", alternate = "coverUrl")
    private String coverUrl;
    @SerializedName(value = "release_date", alternate = "releaseDate")
    private String releaseDate;
    @SerializedName(value = "is_premium", alternate = "isPremium")
    private Integer isPremium;
    private Double price;
    
    // Add extra field often returned in JOINs
    @SerializedName("artist_name")
    private String artistName;

    // Getters and Setters
    public Integer getAlbumId() { return albumId; }
    public void setAlbumId(Integer albumId) { this.albumId = albumId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getArtistId() { return artistId; }
    public void setArtistId(Integer artistId) { this.artistId = artistId; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public Integer getIsPremium() { return isPremium; }
    public void setIsPremium(Integer isPremium) { this.isPremium = isPremium; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }
    
    // For compatibility with potential snake_case calls (though better to fix caller)
    // I will fix the caller instead of adding duplicate methods.
}
