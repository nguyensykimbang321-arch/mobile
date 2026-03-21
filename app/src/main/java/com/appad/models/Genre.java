package com.appad.models;

import java.io.Serializable;

public class Genre implements Serializable {
    @com.google.gson.annotations.SerializedName(value = "genre_id", alternate = "genreId")
    private Integer genreId;
    private String name;
    private String description;
    @com.google.gson.annotations.SerializedName(value = "cover_url", alternate = "coverUrl")
    private String coverUrl;

    @com.google.gson.annotations.SerializedName(value = "song_count", alternate = "songCount")
    private Integer songCount;

    public Genre() {
    }

    public Integer getGenreId() {
        return genreId;
    }

    public void setGenreId(Integer genreId) {
        this.genreId = genreId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Integer getSongCount() {
        return songCount;
    }

    public void setSongCount(Integer songCount) {
        this.songCount = songCount;
    }
}
