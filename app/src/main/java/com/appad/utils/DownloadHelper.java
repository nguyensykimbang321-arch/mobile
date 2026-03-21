package com.appad.utils;

import android.content.Context;
import com.appad.models.Song;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DownloadHelper {
    private static final String FILENAME = "downloads.json";
    private static DownloadHelper instance;
    private final Context context;
    private List<Song> downloadedSongs;
    private final Gson gson;

    private DownloadHelper(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        this.downloadedSongs = loadDownloads();
    }

    public static synchronized DownloadHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DownloadHelper(context);
        }
        return instance;
    }

    private List<Song> loadDownloads() {
        File file = new File(context.getFilesDir(), FILENAME);
        if (!file.exists()) return new ArrayList<>();
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<List<Song>>() {}.getType();
            List<Song> songs = gson.fromJson(reader, type);
            return songs != null ? songs : new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void saveDownload(Song song, String localPath) {
        // Prepare a copy of the song with the local path stored in fileUrl or a dedicated field
        // Since we don't have a localPath field in Song model, we'll reuse fileUrl for the local path
        // but it's better to NOT mess with the original model's fileUrl if we want to support both.
        // Actually, for playback, we just need the local path.
        
        // Remove if already exists (update)
        removeDownload(song.getSongId(), false);
        
        song.setFileUrl(localPath); // Store local path here for the "Download" tab playback
        downloadedSongs.add(song);
        persist();
    }

    public void removeDownload(Integer songId, boolean deleteFile) {
        Song found = null;
        for (Song s : downloadedSongs) {
            if (s.getSongId().equals(songId)) {
                found = s;
                break;
            }
        }
        if (found != null) {
            if (deleteFile && found.getFileUrl() != null) {
                File file = new File(found.getFileUrl());
                if (file.exists()) file.delete();
            }
            downloadedSongs.remove(found);
            persist();
        }
    }

    public List<Song> getDownloadedSongs() {
        // Filter out songs whose files were deleted manually
        List<Song> validSongs = new ArrayList<>();
        boolean changed = false;
        for (Song s : downloadedSongs) {
            if (s.getFileUrl() != null && new File(s.getFileUrl()).exists()) {
                validSongs.add(s);
            } else {
                changed = true;
            }
        }
        if (changed) {
            downloadedSongs = validSongs;
            persist();
        }
        return validSongs;
    }

    private void persist() {
        File file = new File(context.getFilesDir(), FILENAME);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(downloadedSongs, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
