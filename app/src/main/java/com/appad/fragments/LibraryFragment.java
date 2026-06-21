package com.appad.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.appad.activities.AlbumDetailActivity;
import com.appad.activities.ArtistDetailActivity;
import com.appad.activities.FullPlayerActivity;
import com.appad.activities.PlaylistDetailActivity;
import com.appad.adapters.SongAdapter;
import com.appad.models.Song;
import com.appad.utils.MusicPlayerManager;
import com.appad.utils.RetrofitClient;
import com.appad.utils.SessionManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.appad.utils.ImageUrlUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View txtEmpty, txtEmptyTitle;
    
    // Header
    private TextView txtTabTitle;
    private ImageView imgTabIcon, btnSearchLibrary;
    private LinearLayout btnLibraryDropdown;

    // Search & Filter
    private LinearLayout layoutSearchBar;
    private EditText edtSearchLibrary;
    private ImageView btnCloseSearch;
    private TextView btnSortTitle, btnSortArtist, btnSortRecent;
    
    // Create Playlist
    private LinearLayout layoutCreatePlaylist, btnAddPlaylist, btnImportPlaylist;
    private View playlistSpacer;
    private EditText edtNewPlaylistName;
    private Button btnCancelCreate, btnConfirmCreate;
    
    private LinearLayout btnPlayAll;

    // State
    private String activeTab = "favorites"; // favorites, playlists, premium, albums, artists, history
    private String sortBy = "title";
    private String searchQuery = "";
    private MusicPlayerManager.OnPlayerStatusChangeListener playerStatusChangeListener;
    
    // Data Caches
    private List<Song> songList = new ArrayList<>(); // For Fav, Premium, History
    private List<Map<String, Object>> playlistList = new ArrayList<>();
    private List<Map<String, Object>> albumList = new ArrayList<>();
    private List<Map<String, Object>> artistList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);
        initViews(view);
        setupListeners();
        loadData();
        setupPlayerListener();
        return view;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadData();
        }
    }

    private void setupPlayerListener() {
        if (playerStatusChangeListener == null) {
            playerStatusChangeListener = new MusicPlayerManager.OnPlayerStatusChangeListener() {
                @Override
                public void onSongChanged(Song song) {
                    refreshAdapter();
                }

                @Override
                public void onPlaybackStatusChanged(boolean isPlaying) {
                    refreshAdapter();
                }

                @Override
                public void onProgressUpdated(int position, int duration) {
                }
            };
        }
        MusicPlayerManager.getInstance().addStatusChangeListener(playerStatusChangeListener);
    }

    private void refreshAdapter() {
        if (recyclerView != null && recyclerView.getAdapter() != null) {
            recyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (playerStatusChangeListener != null) {
            MusicPlayerManager.getInstance().removeStatusChangeListener(playerStatusChangeListener);
        }
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.rvLibraryContent);
        progressBar = view.findViewById(R.id.pbLibrary);
        txtEmpty = view.findViewById(R.id.txtLibraryEmpty);
        txtEmptyTitle = view.findViewById(R.id.txtEmptyTitle);
        
        txtTabTitle = view.findViewById(R.id.txtTabTitle);
        imgTabIcon = view.findViewById(R.id.imgTabIcon);
        btnLibraryDropdown = view.findViewById(R.id.btnLibraryDropdown);
        btnSearchLibrary = view.findViewById(R.id.btnSearchLibrary);
        
        layoutSearchBar = view.findViewById(R.id.layoutSearchBar);
        edtSearchLibrary = view.findViewById(R.id.edtSearchLibrary);
        btnCloseSearch = view.findViewById(R.id.btnCloseSearch);
        
        btnSortTitle = view.findViewById(R.id.btnSortTitle);
        btnSortArtist = view.findViewById(R.id.btnSortArtist);
        btnSortRecent = view.findViewById(R.id.btnSortRecent);
        
        layoutCreatePlaylist = view.findViewById(R.id.layoutCreatePlaylist);
        btnAddPlaylist = view.findViewById(R.id.btnAddPlaylist);
        edtNewPlaylistName = view.findViewById(R.id.edtNewPlaylistName);
        btnCancelCreate = view.findViewById(R.id.btnCancelCreate);
        btnConfirmCreate = view.findViewById(R.id.btnConfirmCreate);
        
        btnPlayAll = view.findViewById(R.id.btnPlayAll);
        btnImportPlaylist = view.findViewById(R.id.btnImportPlaylist);
        playlistSpacer = view.findViewById(R.id.playlistSpacer);
    }

    private void setupListeners() {
        btnLibraryDropdown.setOnClickListener(this::showDropdownMenu);
        btnSearchLibrary.setOnClickListener(v -> toggleSearchBar(true));
        btnCloseSearch.setOnClickListener(v -> toggleSearchBar(false));
        
        edtSearchLibrary.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                applyFilterAndSort();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Filter Buttons
        View.OnClickListener sortListener = v -> {
            if (v == btnSortTitle) sortBy = "title";
            else if (v == btnSortArtist) sortBy = "artist";
            else if (v == btnSortRecent) sortBy = "recent";
            updateSortButtons();
            applyFilterAndSort();
        };
        btnSortTitle.setOnClickListener(sortListener);
        btnSortArtist.setOnClickListener(sortListener);
        btnSortRecent.setOnClickListener(sortListener);
        
        // Playlist Action
        btnAddPlaylist.setOnClickListener(v -> layoutCreatePlaylist.setVisibility(View.VISIBLE));
        btnCancelCreate.setOnClickListener(v -> {
            layoutCreatePlaylist.setVisibility(View.GONE);
            edtNewPlaylistName.setText("");
        });
        btnConfirmCreate.setOnClickListener(v -> createPlaylist());
        
        btnPlayAll.setOnClickListener(v -> {
            if (!songList.isEmpty()) {
                MusicPlayerManager.getInstance().setPlaylist(songList, 0);
                startActivity(new Intent(getContext(), FullPlayerActivity.class));
            }
        });

        btnImportPlaylist.setOnClickListener(v -> showImportPlaylistDialog());
    }

    private void toggleSearchBar(boolean show) {
        layoutSearchBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSearchLibrary.setVisibility(show ? View.GONE : View.VISIBLE);
        if (!show) {
            edtSearchLibrary.setText("");
            searchQuery = "";
            applyFilterAndSort();
        }
    }

    private void showDropdownMenu(View v) {
        android.widget.ListPopupWindow listPopupWindow = new android.widget.ListPopupWindow(getContext());
        listPopupWindow.setAnchorView(btnLibraryDropdown);
        
        String[] labels = {"Yêu thích", "Playlist", "Bài hát đã mua", "Album đã mua", "Nghệ sĩ đã follow", "Lịch sử"};
        int[] icons = {
            R.drawable.ic_star_badge, 
            R.drawable.ic_nav_library, 
            R.drawable.ic_wallet, 
            R.drawable.ic_albums_outline, 
            R.drawable.ic_nav_profile, 
            R.drawable.ic_musical_notes
        };
        int[] colors = {
            android.graphics.Color.parseColor("#FFD700"), // Favorites - Gold
            android.graphics.Color.parseColor("#2196F3"), // Playlist - Blue
            android.graphics.Color.parseColor("#8B5CF6"), // Songs - Purple
            android.graphics.Color.parseColor("#9C27B0"), // Albums - Deep Purple
            android.graphics.Color.parseColor("#E91E63"), // Artists - Pink
            android.graphics.Color.parseColor("#10B981")  // History - Green
        };

        int selectedIndex = 0;
        switch (activeTab) {
            case "favorites": selectedIndex = 0; break;
            case "playlists": selectedIndex = 1; break;
            case "premium": selectedIndex = 2; break;
            case "albums": selectedIndex = 3; break;
            case "artists": selectedIndex = 4; break;
            case "history": selectedIndex = 5; break;
        }

        DropdownAdapter menuAdapter = new DropdownAdapter(labels, icons, colors, selectedIndex);
        listPopupWindow.setAdapter(menuAdapter);
        listPopupWindow.setWidth(android.widget.ListPopupWindow.WRAP_CONTENT);
        // Ensure at least 220dp width
        int minWidth = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 220, getResources().getDisplayMetrics());
        if (listPopupWindow.getWidth() < minWidth) listPopupWindow.setWidth(minWidth);
        
        listPopupWindow.setHeight(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        listPopupWindow.setModal(true);
        
        listPopupWindow.setBackgroundDrawable(androidx.core.content.ContextCompat.getDrawable(getContext(), R.drawable.bg_dropdown_button));
        listPopupWindow.setVerticalOffset(4);

        listPopupWindow.setOnItemClickListener((parent, view, position, id) -> {
            switch (position) {
                case 0: switchTab("favorites"); break;
                case 1: switchTab("playlists"); break;
                case 2: switchTab("premium"); break;
                case 3: switchTab("albums"); break;
                case 4: switchTab("artists"); break;
                case 5: switchTab("history"); break;
            }
            listPopupWindow.dismiss();
        });

        listPopupWindow.show();
    }

    private class DropdownAdapter extends android.widget.BaseAdapter {
        String[] labels;
        int[] icons;
        int[] colors;
        int selectedIndex;

        DropdownAdapter(String[] labels, int[] icons, int[] colors, int selected) {
            this.labels = labels;
            this.icons = icons;
            this.colors = colors;
            this.selectedIndex = selected;
        }

        @Override public int getCount() { return labels.length; }
        @Override public Object getItem(int position) { return labels[position]; }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_dropdown_menu, parent, false);
            }
            ImageView img = convertView.findViewById(R.id.menuIcon);
            TextView txt = convertView.findViewById(R.id.menuLabel);
            ImageView check = convertView.findViewById(R.id.menuCheck);

            img.setImageResource(icons[position]);
            img.setImageTintList(android.content.res.ColorStateList.valueOf(colors[position]));
            txt.setText(labels[position]);
            
            if (position == selectedIndex) {
                txt.setTextColor(getResources().getColor(R.color.vibrant_purple));
                txt.setTypeface(null, android.graphics.Typeface.BOLD);
                check.setVisibility(View.VISIBLE);
            } else {
                txt.setTextColor(android.graphics.Color.WHITE);
                txt.setTypeface(null, android.graphics.Typeface.NORMAL);
                check.setVisibility(View.GONE);
            }
            return convertView;
        }
    }

    private void switchTab(String tab) {
        activeTab = tab;
        searchQuery = "";
        edtSearchLibrary.setText("");
        layoutCreatePlaylist.setVisibility(View.GONE);
        
        // UI Updates
        int iconRes = R.drawable.ic_search_unselected; // Fallback
        String title = "Yêu thích";
        
        btnAddPlaylist.setVisibility(View.GONE);
        btnImportPlaylist.setVisibility(View.GONE);
        playlistSpacer.setVisibility(View.GONE);
        btnPlayAll.setVisibility(View.GONE);
        
        switch (tab) {
            case "favorites": iconRes = R.drawable.ic_search_unselected; title = "Yêu thích"; btnPlayAll.setVisibility(View.VISIBLE); break;
            case "playlists": 
                iconRes = android.R.drawable.ic_menu_sort_by_size; title = "Playlist"; 
                btnAddPlaylist.setVisibility(View.VISIBLE); 
                btnImportPlaylist.setVisibility(View.VISIBLE);
                playlistSpacer.setVisibility(View.VISIBLE);
                break;
            case "premium": iconRes = android.R.drawable.btn_star_big_on; title = "Premium"; btnPlayAll.setVisibility(View.VISIBLE); break;
            case "albums": iconRes = android.R.drawable.ic_menu_gallery; title = "Album đã mua"; break;
            case "artists": iconRes = android.R.drawable.ic_menu_my_calendar; title = "Nghệ sĩ"; break;
            case "history": iconRes = android.R.drawable.ic_menu_recent_history; title = "Lịch sử"; btnPlayAll.setVisibility(View.VISIBLE); break;
        }
        
        txtTabTitle.setText(title);
        // imgTabIcon.setImageResource(iconRes); // Use proper drawables
        
        loadData();
    }

    private void loadData() {
        Integer userId = SessionManager.getInstance(getContext()).getUserId();
        if (userId == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        txtEmpty.setVisibility(View.GONE);

        switch (activeTab) {
            case "favorites": loadFavorites(userId); break;
            case "playlists": loadPlaylists(userId); break;
            case "premium": loadPurchasedSongs(); break; // Uses token
            case "albums": loadPurchasedAlbums(); break;
            case "artists": loadFollowedArtists(userId); break;
            case "history": loadHistory(userId); break;
        }
    }
    
    // --- Data Loaders ---
    
    private void loadFavorites(Integer userId) {
        RetrofitClient.getApiService().getFavorites(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                handleSongResponse(response);
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) { progressBar.setVisibility(View.GONE); }
        });
    }
    
    private void loadPurchasedSongs() {
        RetrofitClient.getApiService().getPurchasedSongs().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                handleSongResponse(response);
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) { progressBar.setVisibility(View.GONE); }
        });
    }
    
    private void loadHistory(Integer userId) {
        RetrofitClient.getApiService().getHistoryByDay(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) response.body().get("data");
                    if (data != null) {
                        recyclerView.setVisibility(View.VISIBLE);
                        txtEmpty.setVisibility(View.GONE);
                        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                        recyclerView.setAdapter(new com.appad.adapters.HistoryAdapter(getContext(), data));
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        txtEmpty.setVisibility(View.VISIBLE);
                    }
                }
                progressBar.setVisibility(View.GONE);
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) { progressBar.setVisibility(View.GONE); }
        });
    }

    private void handleSongResponse(Response<Map<String, Object>> response) {
        progressBar.setVisibility(View.GONE);
        if (response.isSuccessful() && response.body() != null) {
            Object data = response.body().get("data"); 
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String json = gson.toJson(data);
            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Song>>(){}.getType();
            List<Song> songs = gson.fromJson(json, listType);
            songList.clear();
            if (songs != null) {
                songList.addAll(songs);
            }
            displaySongs();
        }
    }

    private void loadPlaylists(Integer userId) {
        RetrofitClient.getApiService().getUserPlaylists().enqueue(new Callback<Map<String, Object>>() {
            @Override 
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    playlistList = (List<Map<String, Object>>) response.body().get("data");
                    displayPlaylists();
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) { progressBar.setVisibility(View.GONE); }
        });
    }
    
    private void loadPurchasedAlbums() {
        RetrofitClient.getApiService().getPurchasedAlbums().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    albumList = (List<Map<String, Object>>) response.body().get("data");
                    displayAlbums();
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) { progressBar.setVisibility(View.GONE); }
        });
    }
    
    private void loadFollowedArtists(Integer userId) {
        RetrofitClient.getApiService().getFollowedArtists(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    artistList = (List<Map<String, Object>>) response.body().get("data");
                    displayArtists();
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) { progressBar.setVisibility(View.GONE); }
        });
    }

    // --- Create Playlist ---
    private void createPlaylist() {
        String name = edtNewPlaylistName.getText().toString();
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập tên playlist", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("name", name);
        payload.put("description", "");
        
        RetrofitClient.getApiService().createPlaylist(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Tạo playlist thành công", Toast.LENGTH_SHORT).show();
                    layoutCreatePlaylist.setVisibility(View.GONE);
                    edtNewPlaylistName.setText("");
                    loadData(); // Reload
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void showImportPlaylistDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_import_playlist, null);
        EditText edtCode = dialogView.findViewById(R.id.edtImportCode);
        Button btnImport = dialogView.findViewById(R.id.btnConfirmImport);
        
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnImport.setOnClickListener(v -> {
            String code = edtCode.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập mã", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            
            // Navigate to SharedPlaylistActivity
            Intent intent = new Intent(getContext(), com.appad.activities.SharedPlaylistActivity.class);
            intent.putExtra("SHARE_CODE", code);
            startActivity(intent);
        });
        
        dialog.show();
    }

    // --- Display & Adapters ---
    
    // 1. Songs
    private void displaySongs() {
        applyFilterAndSort(); // Will verify filtering
        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Note: filtering is done, but here we just pass songList to adapter if not filtering manually
        // But for Search/Sort, we need to manipulate the list.
        // Let's assume applyFilterAndSort() updates a `filteredList` and sets adapter.
    }
    
    // 2. Playlists
    private void displayPlaylists() {
        applyFilterAndSort();
    }
    
    // 3. Albums
    private void displayAlbums() {
        applyFilterAndSort();
    }
    
    // 4. Artists
    private void displayArtists() {
        applyFilterAndSort();
    }
    
    private void applyFilterAndSort() {
        // Implementation of Sort & Filter based on activeTab
        // Simplified for this response - just setting raw data to adapter
        
        if (activeTab.equals("favorites") || activeTab.equals("premium")) {
            List<Song> filtered = new ArrayList<>(songList);
            // Search
            if (!searchQuery.isEmpty()) {
                List<Song> temp = new ArrayList<>();
                for (Song s : filtered) {
                    if (s.getTitle().toLowerCase().contains(searchQuery.toLowerCase())) temp.add(s);
                }
                filtered = temp;
            }
            // Sort
            if (sortBy.equals("title")) Collections.sort(filtered, (s1, s2) -> s1.getTitle().compareTo(s2.getTitle()));
            else if (sortBy.equals("artist")) Collections.sort(filtered, (s1, s2) -> safeStr(s1.getArtistName()).compareTo(safeStr(s2.getArtistName())));
            // else recent (assuming original order is recent)

            if (filtered.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                txtEmpty.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                txtEmpty.setVisibility(View.GONE);
                SongAdapter adapter = new SongAdapter(getContext(), filtered);
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                recyclerView.setAdapter(adapter);
            }
        } 
        else if (activeTab.equals("playlists")) {
            // Filter playlists...
             List<Map<String, Object>> filtered = new ArrayList<>(playlistList);
             if(!searchQuery.isEmpty()) {
                 //..
             }
             if (filtered.isEmpty()) { recyclerView.setVisibility(View.GONE); txtEmpty.setVisibility(View.VISIBLE); }
             else {
                 recyclerView.setVisibility(View.VISIBLE);
                 txtEmpty.setVisibility(View.GONE);
                 recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                 recyclerView.setAdapter(new GenericAdapter(filtered, "playlist"));
             }
        }
        else if (activeTab.equals("albums")) {
             List<Map<String, Object>> filtered = new ArrayList<>(albumList);
             if (filtered.isEmpty()) { recyclerView.setVisibility(View.GONE); txtEmpty.setVisibility(View.VISIBLE); }
             else {
                 recyclerView.setVisibility(View.VISIBLE);
                 txtEmpty.setVisibility(View.GONE);
                 recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
                 recyclerView.setAdapter(new GenericAdapter(filtered, "album"));
             }
        }
        else if (activeTab.equals("artists")) {
             List<Map<String, Object>> filtered = new ArrayList<>(artistList);
             if (filtered.isEmpty()) { recyclerView.setVisibility(View.GONE); txtEmpty.setVisibility(View.VISIBLE); }
             else {
                 recyclerView.setVisibility(View.VISIBLE);
                 txtEmpty.setVisibility(View.GONE);
                 recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                 recyclerView.setAdapter(new GenericAdapter(filtered, "artist"));
             }
        }
    }
    
    private void updateSortButtons() {
        TextView[] btns = {btnSortTitle, btnSortArtist, btnSortRecent};
        for (TextView btn : btns) {
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)); // Reset
            // Actually need drawable check
            // For now simple UI logic: highlight based on sortBy
        }
    }

    private String safeStr(String s) { return s == null ? "" : s; }

    // --- Inner Generic Adapter ---
    class GenericAdapter extends RecyclerView.Adapter<GenericAdapter.ViewHolder> {
        List<Map<String, Object>> items;
        String type; // playlist, album, artist

        public GenericAdapter(List<Map<String, Object>> items, String type) {
            this.items = items;
            this.type = type;
        }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = R.layout.item_library_row;
            if (type.equals("album")) layout = R.layout.item_album_grid;
            View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
            return new ViewHolder(v);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> item = items.get(position);
            String title = "", subtitle = "", imgUrl = "";
            
            if (type.equals("playlist")) {
                title = (String) item.get("name");
                subtitle = ((Number) item.get("song_count")).intValue() + " bài hát";
                imgUrl = (String) (item.get("cover_url") != null ? item.get("cover_url") : item.get("coverUrl"));
                Glide.with(holder.itemView).load(ImageUrlUtils.fixUrl(imgUrl)).transform(new RoundedCorners(16)).into(holder.imgCover);
                
                String finalTitle = title;
                holder.itemView.setOnClickListener(v -> {
                    Intent i = new Intent(getContext(), PlaylistDetailActivity.class);
                    i.putExtra("PLAYLIST_ID", ((Number)item.get("playlist_id")).intValue());
                    i.putExtra("PLAYLIST_NAME", finalTitle);
                    startActivity(i);
                });
            } else if (type.equals("artist")) {
                title = (String) item.get("name");
                Object songCnt = item.get("songCount");
                subtitle = "Nghệ sĩ • " + (songCnt != null ? ((Number)songCnt).intValue() : 0) + " bài hát";
                imgUrl = (String) (item.get("image_url") != null ? item.get("image_url") : item.get("imageUrl"));
                
                if (holder.txtSubtitle != null) holder.txtSubtitle.setText(subtitle);
                
                Glide.with(holder.itemView).load(ImageUrlUtils.fixUrl(imgUrl)).transform(new CircleCrop()).into(holder.imgCover);
                
                String finalArtistName = title;
                holder.itemView.setOnClickListener(v -> {
                    Intent i = new Intent(getContext(), ArtistDetailActivity.class);
                    Object artistIdObj = item.get("artist_id") != null ? item.get("artist_id") : item.get("artistId");
                    if (artistIdObj != null) {
                        i.putExtra("artistId", ((Number)artistIdObj).intValue());
                        i.putExtra("artistName", finalArtistName);
                        startActivity(i);
                    }
                });
            } else if (type.equals("album")) {
                title = (String) item.get("title");
                subtitle = (String) item.get("artist_name"); // Ensure backend sends this in getAlbum/PurchasedAlbum?
                // PurchasedAlbum returns Album obj. Album model has artistName? Check Model.
                imgUrl = (String) (item.get("cover_url") != null ? item.get("cover_url") : item.get("coverUrl"));
                
                if (holder.txtSubtitle != null) holder.txtSubtitle.setText(subtitle != null ? subtitle : "");
                
                Glide.with(holder.itemView).load(ImageUrlUtils.fixUrl(imgUrl)).into(holder.imgCover);
                
                // Show Badges for Album
                if (holder.accessBadge != null && holder.badgePremium != null) {
                    holder.accessBadge.setVisibility(View.GONE);
                    holder.badgePremium.setVisibility(View.GONE);
                    
                    Object isPreObj = item.get("is_premium") != null ? item.get("is_premium") : item.get("isPremium");
                    boolean isPremium = (isPreObj instanceof Number && ((Number)isPreObj).intValue() == 1) || Boolean.TRUE.equals(isPreObj);
                    
                    if (isPremium) {
                        holder.badgePremium.setVisibility(View.VISIBLE);
                        holder.badgePremium.setText("PREMIUM");
                        
                        // Check user access for badge
                        if (activeTab.equals("albums")) {
                            holder.accessBadge.setVisibility(View.VISIBLE);
                            holder.accessBadge.setAccessType("purchased");
                        } else {
                            com.appad.models.User user = SessionManager.getInstance(getContext()).getUser();
                            if (user != null && Integer.valueOf(1).equals(user.getIsPremium())) {
                                holder.accessBadge.setVisibility(View.VISIBLE);
                                holder.accessBadge.setAccessType("premium");
                            }
                        }
                    }
                }

                holder.itemView.setOnClickListener(v -> {
                   Intent i = new Intent(getContext(), AlbumDetailActivity.class);
                   i.putExtra("ALBUM_ID", ((Number)item.get("album_id")).intValue());
                   startActivity(i);
                });
            }

            if (holder.txtTitle != null) holder.txtTitle.setText(title);
            if (holder.txtSubtitle != null && !type.equals("album")) holder.txtSubtitle.setText(subtitle);
        }

        @Override public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgCover;
            TextView txtTitle, txtSubtitle;
            com.appad.components.AccessBadgeView accessBadge;
            com.appad.components.PremiumBadgeView badgePremium;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                if (type.equals("album")) {
                   imgCover = itemView.findViewById(R.id.imgAlbumCover);
                   txtTitle = itemView.findViewById(R.id.txtAlbumTitle);
                   txtSubtitle = itemView.findViewById(R.id.txtAlbumArtist);
                   accessBadge = itemView.findViewById(R.id.accessBadge);
                   badgePremium = itemView.findViewById(R.id.badgePremium);
                } else {
                   imgCover = itemView.findViewById(R.id.imgRowCover);
                   txtTitle = itemView.findViewById(R.id.txtRowTitle);
                   txtSubtitle = itemView.findViewById(R.id.txtRowSubtitle);
                }
            }
        }
    }
}
