package com.appad.components;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.appad.models.Song;
import com.appad.utils.ImageUrlUtils;
import com.appad.utils.RetrofitClient;
import com.appad.utils.SessionManager;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddToPlaylistFragment extends BottomSheetDialogFragment {

    private Integer songId;
    private String songTitle;
    private String artistName;
    private String coverUrl;

    private RecyclerView rvPlaylists;
    private TextView txtNoPlaylists;
    private LinearLayout btnCreateNew;
    private LinearLayout createNewContainer;
    private LinearLayout emptyContainer;
    private LinearLayout songInfoContainer;
    private EditText edtNewPlaylistName;
    private Button btnCancelCreate;
    private Button btnConfirmCreate;
    private ProgressBar pbLoading;
    private ImageButton btnCloseSheet;
    private ImageView imgSongCover;
    private TextView txtSongTitle;
    private TextView txtSongArtist;
    private List<Map<String, Object>> playlists = new ArrayList<>();

    public static AddToPlaylistFragment newInstance(Integer songId) {
        AddToPlaylistFragment fragment = new AddToPlaylistFragment();
        Bundle args = new Bundle();
        args.putInt("songId", songId);
        fragment.setArguments(args);
        return fragment;
    }

    public static AddToPlaylistFragment newInstance(Song song) {
        AddToPlaylistFragment fragment = new AddToPlaylistFragment();
        Bundle args = new Bundle();
        if (song != null) {
            args.putInt("songId", song.getSongId() != null ? song.getSongId() : 0);
            args.putString("songTitle", song.getTitle());
            args.putString("artistName", song.getArtistName());
            args.putString("coverUrl", song.getCoverUrl());
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            songId = getArguments().getInt("songId");
            songTitle = getArguments().getString("songTitle", "");
            artistName = getArguments().getString("artistName", "");
            coverUrl = getArguments().getString("coverUrl", "");
        }
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_add_to_playlist_sheet, container, false);

        initViews(view);
        setupListeners();
        loadPlaylists();

        return view;
    }

    private void initViews(View view) {
        rvPlaylists = view.findViewById(R.id.rvPlaylists);
        txtNoPlaylists = view.findViewById(R.id.txtNoPlaylists);
        btnCreateNew = view.findViewById(R.id.btnCreateNewPlaylist);
        createNewContainer = view.findViewById(R.id.createNewContainer);
        emptyContainer = view.findViewById(R.id.emptyContainer);
        edtNewPlaylistName = view.findViewById(R.id.edtNewPlaylistName);
        btnCancelCreate = view.findViewById(R.id.btnCancelCreate);
        btnConfirmCreate = view.findViewById(R.id.btnConfirmCreate);
        pbLoading = view.findViewById(R.id.pbLoading);
        btnCloseSheet = view.findViewById(R.id.btnCloseSheet);
        songInfoContainer = view.findViewById(R.id.songInfoContainer);
        imgSongCover = view.findViewById(R.id.imgSongCover);
        txtSongTitle = view.findViewById(R.id.txtSongTitle);
        txtSongArtist = view.findViewById(R.id.txtSongArtist);

        rvPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));

        // Setup song info
        if (songTitle != null && !songTitle.isEmpty()) {
            txtSongTitle.setText(songTitle);
        }
        if (artistName != null && !artistName.isEmpty()) {
            txtSongArtist.setText(artistName);
        }
        if (coverUrl != null && !coverUrl.isEmpty() && getContext() != null) {
            Glide.with(getContext())
                    .load(ImageUrlUtils.fixUrl(coverUrl))
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(imgSongCover);
        }
    }

    private void setupListeners() {
        btnCloseSheet.setOnClickListener(v -> dismiss());

        btnCreateNew.setOnClickListener(v -> {
            btnCreateNew.setVisibility(View.GONE);
            createNewContainer.setVisibility(View.VISIBLE);
            edtNewPlaylistName.requestFocus();
        });

        btnCancelCreate.setOnClickListener(v -> {
            createNewContainer.setVisibility(View.GONE);
            btnCreateNew.setVisibility(View.VISIBLE);
            edtNewPlaylistName.setText("");
        });

        btnConfirmCreate.setOnClickListener(v -> {
            String name = edtNewPlaylistName.getText().toString().trim();
            if (!name.isEmpty()) {
                createPlaylistAndAddSong(name);
            } else {
                Toast.makeText(getContext(), "Vui lòng nhập tên playlist", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPlaylists() {
        Integer userId = SessionManager.getInstance(getContext()).getUserId();
        if (userId == null) return;

        pbLoading.setVisibility(View.VISIBLE);
        rvPlaylists.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);

        RetrofitClient.getApiService().getUserPlaylists().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        playlists.clear();
                        playlists.addAll((List<Map<String, Object>>) data);

                            if (playlists.isEmpty()) {
                                emptyContainer.setVisibility(View.VISIBLE);
                                rvPlaylists.setVisibility(View.GONE);
                            } else {
                                emptyContainer.setVisibility(View.GONE);
                                rvPlaylists.setVisibility(View.VISIBLE);
                                rvPlaylists.setAdapter(new PlaylistSelectAdapter());
                                checkSongInPlaylists();
                            }
                        }
                    }
                }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                if (getContext() != null)
                    Toast.makeText(getContext(), "Lỗi tải playlist", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkSongInPlaylists() {
        if (songId == null) return;
        
        for (Map<String, Object> playlist : playlists) {
            Number pIdNum = (Number) playlist.get("playlist_id");
            if (pIdNum == null) continue;
            
            int pId = pIdNum.intValue();
            RetrofitClient.getApiService().getPlaylistSongs(pId).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null) {
                        Object data = response.body().get("data");
                        if (data instanceof List) {
                            List<Map<String, Object>> songMaps = (List<Map<String, Object>>) data;
                            boolean exists = false;
                            for (Map<String, Object> sm : songMaps) {
                                Number sid = (Number) sm.get("song_id");
                                if (sid != null && sid.intValue() == songId) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (exists) {
                                playlist.put("is_already_added", true);
                                if (rvPlaylists.getAdapter() != null) {
                                    rvPlaylists.getAdapter().notifyDataSetChanged();
                                }
                            }
                        }
                    }
                }
                @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
            });
        }
    }

    private void createPlaylistAndAddSong(String name) {
        Integer userId = SessionManager.getInstance(getContext()).getUserId();
        if (userId == null) return;

        btnConfirmCreate.setEnabled(false);
        btnConfirmCreate.setText("Đang tạo...");

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("userId", userId);
        payload.put("name", name);

        RetrofitClient.getApiService().createPlaylist(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof Map) {
                        Number playlistIdNum = (Number) ((Map<?, ?>) data).get("playlist_id");
                        if (playlistIdNum != null) {
                            addSongToPlaylist(playlistIdNum.intValue(), name);
                            return;
                        }
                    }
                    Toast.makeText(getContext(), "Đã tạo playlist: " + name, Toast.LENGTH_SHORT).show();
                    loadPlaylists();
                    resetCreateForm();
                }
                btnConfirmCreate.setEnabled(true);
                btnConfirmCreate.setText("Tạo");
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnConfirmCreate.setEnabled(true);
                btnConfirmCreate.setText("Tạo");
                Toast.makeText(getContext(), "Lỗi tạo playlist", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetCreateForm() {
        createNewContainer.setVisibility(View.GONE);
        btnCreateNew.setVisibility(View.VISIBLE);
        edtNewPlaylistName.setText("");
    }

    private void addSongToPlaylist(Integer playlistId) {
        addSongToPlaylist(playlistId, null);
    }

    private void addSongToPlaylist(Integer playlistId, String playlistName) {
        java.util.Map<String, Long> payload = new java.util.HashMap<>();
        payload.put("song_id", songId.longValue());

        RetrofitClient.getApiService().addSongToPlaylist(playlistId, payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    showSuccessAndDismiss("Đã thêm vào playlist" + (playlistName != null ? ": " + playlistName : ""));
                } else {
                    String message = "Lỗi thêm bài hát";
                    try {
                        if (response.errorBody() != null) {
                            Map<String, Object> errorMap = new Gson().fromJson(response.errorBody().string(), new TypeToken<Map<String, Object>>(){}.getType());
                            if (errorMap != null && errorMap.get("message") != null) {
                                message = errorMap.get("message").toString();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    if (response.code() == 400 || response.code() == 409) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi thêm bài hát", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSuccessAndDismiss(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        dismiss();
    }

    private class PlaylistSelectAdapter extends RecyclerView.Adapter<PlaylistSelectAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_playlist_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Map<String, Object> playlist = playlists.get(position);
            String name = (String) playlist.get("name");
            Object countObj = playlist.get("song_count");
            int count = countObj != null ? ((Number) countObj).intValue() : 0;

            holder.txtName.setText(name);
            holder.txtCount.setText(count + " bài hát");

            boolean isAlreadyIn = Boolean.TRUE.equals(playlist.get("is_already_added"));
            if (isAlreadyIn) {
                holder.badgeAdded.setVisibility(View.VISIBLE);
                holder.iconAdd.setVisibility(View.GONE);
                holder.layoutMain.setAlpha(0.7f);
            } else {
                holder.badgeAdded.setVisibility(View.GONE);
                holder.iconAdd.setVisibility(View.VISIBLE);
                holder.layoutMain.setAlpha(1.0f);
            }

            holder.layoutMain.setOnClickListener(v -> {
                if (isAlreadyIn) {
                   Toast.makeText(getContext(), "Bài hát này đã có trong playlist rồi", Toast.LENGTH_SHORT).show();
                   return;
                }
                Integer playlistId = ((Number) playlist.get("playlist_id")).intValue();
                addSongToPlaylist(playlistId, name);
            });

            // Expand functionality
            holder.btnExpand.setOnClickListener(v -> {
                boolean isExpanded = holder.layoutExpanded.getVisibility() == View.VISIBLE;
                if (!isExpanded) {
                    holder.layoutExpanded.setVisibility(View.VISIBLE);
                    holder.iconExpand.setRotation(180);
                    Integer playlistId = ((Number) playlist.get("playlist_id")).intValue();
                    loadSongsForPlaylist(playlistId, holder);
                } else {
                    holder.layoutExpanded.setVisibility(View.GONE);
                    holder.iconExpand.setRotation(0);
                }
            });
        }

        @Override
        public int getItemCount() {
            return playlists.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView txtName, txtCount, txtEmpty;
            LinearLayout layoutMain, btnExpand, layoutExpanded, layoutSongsList;
            ImageView iconExpand, iconAdd;
            LinearLayout badgeAdded;
            ProgressBar pbLoadingSongs;

            VH(View itemView) {
                super(itemView);
                txtName = itemView.findViewById(R.id.txtPlaylistName);
                txtCount = itemView.findViewById(R.id.txtPlaylistCount);
                layoutMain = itemView.findViewById(R.id.layoutPlaylistMain);
                btnExpand = itemView.findViewById(R.id.btnExpand);
                layoutExpanded = itemView.findViewById(R.id.layoutExpandedContent);
                layoutSongsList = itemView.findViewById(R.id.layoutSongsList);
                txtEmpty = itemView.findViewById(R.id.txtEmptyPlaylist);
                pbLoadingSongs = itemView.findViewById(R.id.pbLoadingSongs);
                iconExpand = itemView.findViewById(R.id.iconExpand);
                iconAdd = itemView.findViewById(R.id.iconAdd);
                badgeAdded = itemView.findViewById(R.id.badgeAdded);
            }
        }

        private void loadSongsForPlaylist(int playlistId, VH holder) {
            holder.pbLoadingSongs.setVisibility(View.VISIBLE);
            holder.layoutSongsList.removeAllViews();
            holder.txtEmpty.setVisibility(View.GONE);

            RetrofitClient.getApiService().getPlaylistSongs(playlistId).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    holder.pbLoadingSongs.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        Object data = response.body().get("data");
                        if (data instanceof List) {
                            List<Map<String, Object>> songMaps = (List<Map<String, Object>>) data;
                            if (songMaps.isEmpty()) {
                                holder.txtEmpty.setVisibility(View.VISIBLE);
                            } else {
                                for (Map<String, Object> songMap : songMaps) {
                                    addSongView(holder.layoutSongsList, songMap);
                                }
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    holder.pbLoadingSongs.setVisibility(View.GONE);
                }
            });
        }

        private void addSongView(LinearLayout container, Map<String, Object> songMap) {
            View songView = LayoutInflater.from(container.getContext()).inflate(R.layout.item_playlist_song_inline, container, false);
            TextView title = songView.findViewById(R.id.txtSongTitleInline);
            ImageView img = songView.findViewById(R.id.imgSongInline);

            String songTitle = (String) songMap.get("title");
            String songCover = (String) songMap.get("cover_url");

            title.setText(songTitle);
            if (songCover != null && !songCover.isEmpty()) {
                Glide.with(container.getContext())
                        .load(ImageUrlUtils.fixUrl(songCover))
                        .placeholder(R.drawable.ic_launcher_background)
                        .centerCrop()
                        .into(img);
            }
            container.addView(songView);
        }
    }
}
