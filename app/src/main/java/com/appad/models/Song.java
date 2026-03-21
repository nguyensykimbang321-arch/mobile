package com.appad.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Song implements Serializable {
    @SerializedName(value = "song_id", alternate = "songId")
    private Integer songId;
    private String title;
    @SerializedName(value = "artist_id", alternate = "artistId")
    private Integer artistId;
    @SerializedName("artist_name")
    private String artistName;
    @SerializedName(value = "album_id", alternate = "albumId")
    private Integer albumId;
    @SerializedName("album_title")
    private String albumTitle;
    @SerializedName(value = "cover_url", alternate = "coverUrl")
    private String coverUrl;
    @SerializedName(value = "file_url", alternate = "fileUrl")
    private String fileUrl;
    private Integer duration;
    @SerializedName(value = "is_premium", alternate = "isPremium")
    private Integer isPremium;
    @SerializedName(value = "is_album_premium", alternate = "isAlbumPremium")
    private Integer isAlbumPremium;
    @SerializedName(value = "listen_count", alternate = "listenCount")
    private Long listenCount;
    @SerializedName(value = "averageRating", alternate = {"stars", "avgRating", "average_rating"})
    private Double stars;
    @SerializedName(value = "genre_name", alternate = "genreName")
    private String genreName;
    @SerializedName(value = "genre_id", alternate = "genreId")
    private Integer genreId;
    private Double price;
    private Boolean bought;
    @SerializedName(value = "album_bought", alternate = "albumBought")
    private Boolean albumBought;
    @SerializedName(value = "artist_member", alternate = "artistMember")
    private Boolean artistMember;
    @SerializedName("album_price")
    private Double albumPrice;
    @SerializedName("album_release_date")
    private String albumReleaseDate;
    @SerializedName(value = "is_artist_owner", alternate = "isArtistOwner")
    private Boolean isArtistOwner;
    @SerializedName(value = "release_date", alternate = "releaseDate")
    private String releaseDate;
    private String lyrics;

    // Getters and Setters
    public Integer getSongId() { return songId; }
    public void setSongId(Integer songId) { this.songId = songId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getArtistId() { return artistId; }
    public void setArtistId(Integer artistId) { this.artistId = artistId; }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public Integer getAlbumId() { return albumId; }
    public void setAlbumId(Integer albumId) { this.albumId = albumId; }

    public String getAlbumTitle() { return albumTitle; }
    public void setAlbumTitle(String albumTitle) { this.albumTitle = albumTitle; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public Integer getIsPremium() { return isPremium; }
    public void setIsPremium(Integer isPremium) { this.isPremium = isPremium; }

    public boolean isPremium() { return isPremium != null && isPremium == 1; }

    public Integer getIsAlbumPremium() { return isAlbumPremium; }
    public void setIsAlbumPremium(Integer isAlbumPremium) { this.isAlbumPremium = isAlbumPremium; }
    
    public Integer getAlbumIsPremium() { return isAlbumPremium; }

    public Long getListenCount() { return listenCount != null ? listenCount : 0L; }
    public void setListenCount(Long listenCount) { this.listenCount = listenCount; }

    public Double getStars() { return stars != null ? stars : 0.0; }
    public void setStars(Double stars) { this.stars = stars; }

    public String getGenreName() { return genreName; }
    public void setGenreName(String genreName) { this.genreName = genreName; }

    public Integer getGenreId() { return genreId; }
    public void setGenreId(Integer genreId) { this.genreId = genreId; }

    public Double getPrice() { return price != null ? price : 0.0; }
    public void setPrice(Double price) { this.price = price; }

    public Boolean getBought() { return bought; }
    public void setBought(Boolean bought) { this.bought = bought; }

    public Boolean getAlbumBought() { return albumBought; }
    public void setAlbumBought(Boolean albumBought) { this.albumBought = albumBought; }

    public Boolean getArtistMember() { return artistMember; }
    public void setArtistMember(Boolean artistMember) { this.artistMember = artistMember; }

    public Double getAlbumPrice() { return albumPrice != null ? albumPrice : 0.0; }
    public void setAlbumPrice(Double albumPrice) { this.albumPrice = albumPrice; }

    public String getAlbumReleaseDate() { return albumReleaseDate; }
    public void setAlbumReleaseDate(String albumReleaseDate) { this.albumReleaseDate = albumReleaseDate; }

    public Boolean getIsArtistOwner() { return isArtistOwner != null ? isArtistOwner : false; }
    public void setIsArtistOwner(Boolean isArtistOwner) { this.isArtistOwner = isArtistOwner; }

    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }

    public String getLyrics() { return lyrics; }
    public void setLyrics(String lyrics) { this.lyrics = lyrics; }

    // Helper method for older snake_case calls if any
    public Integer getSong_id() { return getSongId(); }
    public Integer getArtist_id() { return getArtistId(); }
    public String getArtist_name() { return getArtistName(); }
    public Integer getAlbum_id() { return getAlbumId(); }
    public String getCover_url() { return getCoverUrl(); }
    public String getFile_url() { return getFileUrl(); }
    public Integer getIs_premium() { return getIsPremium(); }
    public String getRelease_date() { return getReleaseDate(); }
    public Integer getGenre_id() { return getGenreId(); }
}
