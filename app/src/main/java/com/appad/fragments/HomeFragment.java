package com.appad.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearSmoothScroller;
import com.appad.R;
import com.appad.activities.FullPlayerActivity;
import com.appad.adapters.CardAdapter;
import com.appad.adapters.SongAdapter;
import com.appad.adapters.AlbumCardAdapter;
import com.appad.adapters.ArtistAdapter;
import com.appad.models.Song;
import com.appad.models.Album;
import com.appad.models.Artist;
import com.appad.utils.MusicPlayerManager;
import com.appad.utils.RetrofitClient;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerViewTrending, recyclerViewAlbums, recyclerViewMain, recyclerViewArtists;
    private CardAdapter trendingAdapter;
    private AlbumCardAdapter albumsAdapter;
    private ArtistAdapter artistAdapter;
    private SongAdapter mainAdapter;
    private TabLayout tabLayout;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshHome;
    private android.widget.ProgressBar pbMainList;
    private View viewNotificationBadge;
    private boolean isDataLoaded = false;
    private MusicPlayerManager.OnPlayerStatusChangeListener playerStatusChangeListener;
    private android.os.Handler autoScrollHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable trendingRunnable, albumsRunnable, artistsRunnable;
    private ItemTouchHelper mainListTouchHelper;

    private List<Song> trendingList = new ArrayList<>();
    private List<Album> albumsList = new ArrayList<>();
    private List<Artist> artistList = new ArrayList<>();
    private List<Song> mainList = new ArrayList<>();

    private int currentOffset = 0;
    private final int PAGE_SIZE = 10;
    private boolean isLoadingMore = false;
    private boolean hasMoreData = true;
    private int selectedTab = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = null;
        try {
            view = inflater.inflate(R.layout.fragment_home, container, false);
            initViews(view);
            setupAdapters();
            if (!isDataLoaded) {
                loadData();
            }
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Critical error in onCreateView", e);
            android.widget.TextView errorView = new android.widget.TextView(getContext());
            errorView.setText("Lỗi khởi tạo giao diện: " + e.getMessage());
            errorView.setTextColor(android.graphics.Color.WHITE);
            errorView.setPadding(40, 40, 40, 40);
            return errorView;
        }

        if (view != null) {
            view.findViewById(R.id.btnNotificationBell).setOnClickListener(v -> {
                startActivity(new Intent(getContext(), com.appad.activities.NotificationActivity.class));
            });
        }

        setupPlayerListener();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startAutoScroll();
        checkUnreadNotifications();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoScroll();
    }

    private void startAutoScroll() {
        trendingRunnable = createScrollRunnable(recyclerViewTrending, trendingList);
        albumsRunnable = createScrollRunnable(recyclerViewAlbums, albumsList);
        artistsRunnable = createScrollRunnable(recyclerViewArtists, artistList);
        
        autoScrollHandler.postDelayed(trendingRunnable, 4000);
        autoScrollHandler.postDelayed(albumsRunnable, 5000);
        autoScrollHandler.postDelayed(artistsRunnable, 6000);
    }

    private void stopAutoScroll() {
        if (trendingRunnable != null) autoScrollHandler.removeCallbacks(trendingRunnable);
        if (albumsRunnable != null) autoScrollHandler.removeCallbacks(albumsRunnable);
        if (artistsRunnable != null) autoScrollHandler.removeCallbacks(artistsRunnable);
    }

    private Runnable createScrollRunnable(final RecyclerView rv, final List<?> list) {
        return new Runnable() {
            @Override
            public void run() {
                if (rv == null || !rv.isAttachedToWindow() || list == null || list.isEmpty()) {
                    if (autoScrollHandler != null) autoScrollHandler.postDelayed(this, 5000);
                    return;
                }

                try {
                    if (rv.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                        LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                        if (lm != null) {
                            int firstVisible = lm.findFirstVisibleItemPosition();
                            int lastVisible = lm.findLastVisibleItemPosition();
                            
                            if (firstVisible != RecyclerView.NO_POSITION) {
                                int nextPos;
                                
                                // Check if we're at or near the end
                                if (lastVisible >= list.size() - 1) {
                                    // Scroll back to the beginning
                                    nextPos = 0;
                                } else {
                                    nextPos = firstVisible + 1;
                                }
                                
                                // Custom scroller for smoother "Premium" feel
                                RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(rv.getContext()) {
                                    @Override
                                    protected int getHorizontalSnapPreference() {
                                        return SNAP_TO_START;
                                    }
                                    @Override
                                    protected float calculateSpeedPerPixel(android.util.DisplayMetrics displayMetrics) {
                                        // Slower speed when scrolling back to start for smoother effect
                                        if (nextPos == 0) {
                                            return 80f / displayMetrics.densityDpi;
                                        }
                                        return 120f / displayMetrics.densityDpi;
                                    }
                                };
                                smoothScroller.setTargetPosition(nextPos);
                                lm.startSmoothScroll(smoothScroller);
                            }
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.e("HomeFragment", "AutoScroll error", e);
                }

                if (autoScrollHandler != null) {
                    autoScrollHandler.postDelayed(this, 5000);
                }
            }
        };
    }

    // Need to import LinearSmoothScroller if not already there
    // Adding imports via multi_replace or checking existing ones.

    private void setupPlayerListener() {
        if (playerStatusChangeListener == null) {
            playerStatusChangeListener = new MusicPlayerManager.OnPlayerStatusChangeListener() {
                @Override
                public void onSongChanged(Song song) {
                    notifyAdapters();
                }

                @Override
                public void onPlaybackStatusChanged(boolean isPlaying) {
                    notifyAdapters();
                }

                @Override
                public void onProgressUpdated(int position, int duration) {
                }
            };
        }
        MusicPlayerManager.getInstance().addStatusChangeListener(playerStatusChangeListener);
    }

    private void notifyAdapters() {
        if (mainAdapter != null) mainAdapter.notifyDataSetChanged();
        if (trendingAdapter != null) trendingAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (playerStatusChangeListener != null) {
            MusicPlayerManager.getInstance().removeStatusChangeListener(playerStatusChangeListener);
        }
        // Cleanup music player end listener
        MusicPlayerManager.getInstance().setOnPlaylistEndListener(null);
    }

    private void initViews(View view) {
        recyclerViewTrending = view.findViewById(R.id.recyclerViewTrending);
        recyclerViewAlbums = view.findViewById(R.id.recyclerViewAlbums);
        recyclerViewArtists = view.findViewById(R.id.recyclerViewArtists);
        recyclerViewMain = view.findViewById(R.id.recyclerViewMain);
        tabLayout = view.findViewById(R.id.tabLayout);
        swipeRefreshHome = view.findViewById(R.id.swipeRefreshHome);
        pbMainList = view.findViewById(R.id.pbMainList);
        viewNotificationBadge = view.findViewById(R.id.viewNotificationBadge);

        if (swipeRefreshHome != null) {
            swipeRefreshHome.setColorSchemeColors(getResources().getColor(R.color.accent));
            swipeRefreshHome.setOnRefreshListener(() -> {
                isDataLoaded = false;
                loadData();
            });
        }

        // Welcome message logic
        android.widget.TextView txtWelcome = view.findViewById(R.id.txtWelcomeUser);
        com.appad.models.User user = null;
        try {
            com.appad.utils.SessionManager sm = com.appad.utils.SessionManager.getInstance(getContext());
            if (sm != null) user = sm.getUser();
        } catch (Exception e) { e.printStackTrace(); }
        
        if (user != null) {
            String displayName = user.getFullName();
            if (displayName == null || displayName.isEmpty()) displayName = user.getUsername();
            txtWelcome.setText(displayName);
        } else {
            txtWelcome.setText("Bạn");
        }

        // Horizontal Layouts
        recyclerViewTrending.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerViewAlbums.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerViewArtists.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        
        // Vertical Main List
        recyclerViewMain.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewMain.setNestedScrollingEnabled(false);

        // Setup Drag and Drop for Main List (Session only)
        setupMainListDragAndDrop();

        // Infinite Scroll Listener for NestedScrollView
        androidx.core.widget.NestedScrollView nestedScroll = view.findViewById(R.id.homeNestedScroll);
        if (nestedScroll != null) {
            nestedScroll.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (scrollY == v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight()) {
                    if (!isLoadingMore && hasMoreData) {
                        loadMoreData();
                    }
                }
            });
        }
    }

    private void setupAdapters() {
        trendingAdapter = new CardAdapter(getContext(), trendingList, song -> startPlaying(song, trendingList));
        
        albumsAdapter = new AlbumCardAdapter(getContext(), albumsList, album -> {
            Intent intent = new Intent(getContext(), com.appad.activities.AlbumDetailActivity.class);
            intent.putExtra("albumId", album.getAlbumId());
            startActivity(intent);
        });

        artistAdapter = new ArtistAdapter(getContext(), artistList, artist -> {
            Intent intent = new Intent(getContext(), com.appad.activities.ArtistDetailActivity.class);
            intent.putExtra("artistId", artist.getArtistId());
            startActivity(intent);
        });

        mainAdapter = new SongAdapter(getContext(), mainList);

        if (tabLayout != null && tabLayout.getTabCount() == 0) {
            tabLayout.addTab(tabLayout.newTab().setText("Mới nhất"));
            tabLayout.addTab(tabLayout.newTab().setText("Gợi ý"));
            tabLayout.addTab(tabLayout.newTab().setText("Nhạc tủ"));
        }

        recyclerViewTrending.setAdapter(trendingAdapter);
        recyclerViewAlbums.setAdapter(albumsAdapter);
        recyclerViewArtists.setAdapter(artistAdapter);
        recyclerViewMain.setAdapter(mainAdapter);

        // Setup Playlist End Listener for auto-pagination
        MusicPlayerManager.getInstance().setOnPlaylistEndListener(() -> {
            if (isAdded() && hasMoreData && !isLoadingMore) {
                getActivity().runOnUiThread(this::loadMoreData);
            }
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                currentOffset = 0;
                hasMoreData = true;
                
                // Show loading state
                if (pbMainList != null) pbMainList.setVisibility(View.VISIBLE);
                if (recyclerViewMain != null) recyclerViewMain.setAlpha(0.3f);
                
                loadMainListData(selectedTab, false);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadData() {
        if (swipeRefreshHome != null) swipeRefreshHome.setRefreshing(true);
        
        // 1. Load Trending
        RetrofitClient.getApiService().getTrendingSongs(10, 0).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    trendingList.clear();
                    processGenericResponse(response.body(), trendingList, trendingAdapter, new TypeToken<List<Song>>(){}.getType());
                }
                checkAllLoaded();
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) { checkAllLoaded(); }
        });

        // 2. Load Artists (Fixed structure)
        RetrofitClient.getApiService().getAllArtists().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    artistList.clear();
                    processGenericResponse(response.body(), artistList, artistAdapter, new TypeToken<List<Artist>>(){}.getType());
                }
                checkAllLoaded();
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) { checkAllLoaded(); }
        });

        // 3. Load Albums
        RetrofitClient.getApiService().getAllAlbums(10, 0).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    albumsList.clear();
                    processGenericResponse(response.body(), albumsList, albumsAdapter, new TypeToken<List<Album>>(){}.getType());
                }
                checkAllLoaded();
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) { checkAllLoaded(); }
        });

        // 4. Load initial tab
        currentOffset = 0;
        hasMoreData = true;
        loadMainListData(selectedTab, false);
    }

    private void checkAllLoaded() {
        if (isAdded() && swipeRefreshHome != null && swipeRefreshHome.isRefreshing()) {
            // Simple check: we don't strictly need to wait for all, 
            // but for better UX we could use a counter. 
            // For now, let's just stop refreshing when the main list is loaded (handled in loadMainListData)
        }
    }

    private void loadMoreData() {
        currentOffset += PAGE_SIZE;
        loadMainListData(selectedTab, true);
    }

    private void loadMainListData(int position, boolean isAppend) {
        isLoadingMore = true;
        Call<Map<String, Object>> call;
        if (position == 0) call = RetrofitClient.getApiService().getLatestSongs(PAGE_SIZE, currentOffset);
        else if (position == 1) call = RetrofitClient.getApiService().getRecommendedSongs(PAGE_SIZE, currentOffset);
        else call = RetrofitClient.getApiService().getPersonalFavoriteSongs(PAGE_SIZE, currentOffset);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isLoadingMore = false;
                if (pbMainList != null) pbMainList.setVisibility(View.GONE);
                if (recyclerViewMain != null) recyclerViewMain.setAlpha(1.0f);
                
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    isDataLoaded = true;
                    if (swipeRefreshHome != null) swipeRefreshHome.setRefreshing(false);
                    processMainListResponse(response.body(), isAppend);
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isLoadingMore = false;
                if (pbMainList != null) pbMainList.setVisibility(View.GONE);
                if (recyclerViewMain != null) recyclerViewMain.setAlpha(1.0f);
                if (swipeRefreshHome != null) swipeRefreshHome.setRefreshing(false);
            }
        });
    }

    private void processMainListResponse(Map<String, Object> body, boolean isAppend) {
        try {
            Object data = body.get("data");
            if (data instanceof List) {
                Gson gson = new Gson();
                String json = gson.toJson(data);
                List<Song> items = gson.fromJson(json, new TypeToken<List<Song>>(){}.getType());
                if (items != null) {
                    if (items.isEmpty()) {
                        hasMoreData = false;
                    } else {
                        if (!isAppend) mainList.clear();
                        mainList.addAll(items);
                        mainAdapter.notifyDataSetChanged();
                        
                        // Nếu đang phát nhạc từ danh sách này, cập nhật cho manager biết có bài mới
                        Song current = MusicPlayerManager.getInstance().getCurrentSong();
                        if (current != null) {
                            for (Song s : mainList) {
                                if (s.getSongId().equals(current.getSongId())) {
                                    MusicPlayerManager.getInstance().updatePlaylist(new ArrayList<>(mainList));
                                    break;
                                }
                            }
                        }

                        if (items.size() < PAGE_SIZE) hasMoreData = false;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private <T> void processGenericResponse(Map<String, Object> body, List<T> list, RecyclerView.Adapter adapter, Type type) {
        try {
            Object data = body.get("data");
            if (data instanceof List) {
                Gson gson = new Gson();
                String json = gson.toJson(data);
                List<T> items = gson.fromJson(json, type);
                if (items != null) {
                    list.clear();
                    list.addAll(items);
                    adapter.notifyDataSetChanged();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startPlaying(Song song, List<Song> list) {
        int index = list.indexOf(song);
        
        Intent intent = new Intent(getContext(), FullPlayerActivity.class);
        intent.putExtra("title", song.getTitle());
        intent.putExtra("artist", song.getArtistName());
        intent.putExtra("cover", song.getCoverUrl());
        
        intent.putExtra("playlist_json", new Gson().toJson(list));
        intent.putExtra("start_index", index);
        
        startActivity(intent);
    }

    private void setupMainListDragAndDrop() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {
            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                return makeMovementFlags(dragFlags, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                if (mainAdapter != null) {
                    mainAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                    return true;
                }
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                    viewHolder.itemView.setAlpha(0.8f);
                    viewHolder.itemView.setScaleX(1.02f);
                    viewHolder.itemView.setScaleY(1.02f);
                }
                super.onSelectedChanged(viewHolder, actionState);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setAlpha(1.0f);
                viewHolder.itemView.setScaleX(1.0f);
                viewHolder.itemView.setScaleY(1.0f);

                // Khi kết thúc drag, cập nhật thứ tự cho MusicPlayerManager nếu đang phát từ list này
                Song current = MusicPlayerManager.getInstance().getCurrentSong();
                if (current != null) {
                    boolean isFromThisList = false;
                    for (Song s : mainList) {
                        if (s.getSongId().equals(current.getSongId())) {
                            isFromThisList = true;
                            break;
                        }
                    }
                    if (isFromThisList) {
                        MusicPlayerManager.getInstance().updatePlaylist(new ArrayList<>(mainList));
                    }
                }
            }
        };

        mainListTouchHelper = new ItemTouchHelper(callback);
        mainListTouchHelper.attachToRecyclerView(recyclerViewMain);
    }

    public void refreshData() {
        isDataLoaded = false;
        if (isAdded()) {
            loadData();
        }
    }

    private void checkUnreadNotifications() {
        RetrofitClient.getApiService().getNotifications().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!isAdded() || viewNotificationBadge == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Map<String, Object> dataObj = (Map<String, Object>) response.body().get("data");
                        if (dataObj != null) {
                            Object unreadObj = dataObj.get("unread_count");
                            int unreadCount = 0;
                            if (unreadObj instanceof Number) unreadCount = ((Number) unreadObj).intValue();
                            viewNotificationBadge.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // Giữ trạng thái badge hiện tại nếu lỗi mạng
            }
        });
    }
}
