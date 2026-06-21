package com.appad.fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.appad.models.Song;
import com.appad.models.Album;
import com.appad.models.Artist;
import com.appad.models.Genre;
import com.appad.utils.MusicPlayerManager;
import com.appad.utils.RetrofitClient;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private EditText editSearch;
    private ImageButton btnCloseSearch;
    private View btnSearchTrigger;
    private View layoutSearchBar;
    private View textSearchTitle;
    private View dropdownButton;
    private View filterDropdownButton;
    private TextView textFilterLabel;
    private ImageView imgTabIcon;
    private TextView textTabLabel;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View textNoResults;
    private UniversalSearchAdapter adapter;
    private List<Object> searchResults = new ArrayList<>();
    private MusicPlayerManager.OnPlayerStatusChangeListener playerStatusChangeListener;
    
    // UI for filters
    private View filterScroll;
    private com.google.android.material.button.MaterialButton btnSortTime, btnSortListens, btnSortRating, btnSortName;
    
    private String currentFilter = "all"; // all, premium, free
    private String currentSort = "newest"; // newest, oldest, name_asc, name_desc, listens, rating
    
    // Cache for initial data
    private List<Song> allSongs = new ArrayList<>();
    private List<Artist> allArtists = new ArrayList<>();
    private List<Album> allAlbums = new ArrayList<>();
    private List<Genre> allGenres = new ArrayList<>();
    
    private int currentTab = 0; // 0: Songs, 1: Artists, 2: Genres, 3: Albums
    private String currentQuery = "";
    
    // Pagination
    private boolean isLoading = false;
    private boolean hasMore = true;
    private int offset = 0;
    private static final int LIMIT = 10;

    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        editSearch = view.findViewById(R.id.editSearch);
        btnCloseSearch = view.findViewById(R.id.btnCloseSearch);
        btnSearchTrigger = view.findViewById(R.id.btnSearchTrigger);
        layoutSearchBar = view.findViewById(R.id.layoutSearchBar);
        textSearchTitle = view.findViewById(R.id.textSearchTitle);
        dropdownButton = view.findViewById(R.id.dropdownButton);
        filterDropdownButton = view.findViewById(R.id.filterDropdownButton);
        textFilterLabel = view.findViewById(R.id.textFilterLabel);
        imgTabIcon = view.findViewById(R.id.imgTabIcon);
        textTabLabel = view.findViewById(R.id.textTabLabel);
        recyclerView = view.findViewById(R.id.recyclerViewSearch);
        progressBar = view.findViewById(R.id.progressBarSearch);
        textNoResults = view.findViewById(R.id.textNoResults);
        swipeRefreshSearch = view.findViewById(R.id.swipeRefreshSearch);
        
        filterScroll = view.findViewById(R.id.filterScroll);
        filterDropdownButton = view.findViewById(R.id.filterDropdownButton);
        textFilterLabel = view.findViewById(R.id.textFilterLabel);
        btnSortTime = view.findViewById(R.id.sortTime);
        btnSortListens = view.findViewById(R.id.sortListens);
        btnSortRating = view.findViewById(R.id.sortRating);
        btnSortName = view.findViewById(R.id.sortName);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UniversalSearchAdapter(searchResults);
        recyclerView.setAdapter(adapter);

        setupListeners();
        setupFilters();
        setupScrollListener();
        loadInitialData();
        setupPlayerListener();
        
        swipeRefreshSearch.setOnRefreshListener(() -> {
            loadInitialData();
            swipeRefreshSearch.setRefreshing(false);
        });

        // Hide initial Browse Genres layout from XML since we manage it in adapter now
        View layoutBrowseGenres = view.findViewById(R.id.layoutBrowseGenres);
        if (layoutBrowseGenres != null) layoutBrowseGenres.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        return view;
    }

    private void setupListeners() {
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().trim();
                
                if (currentQuery.length() >= 2) {
                    performSearch(currentQuery);
                } else if (currentQuery.isEmpty()) {
                    showDefaultDataForTab();
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Toggle Search Logic
        if (btnSearchTrigger != null) {
            btnSearchTrigger.setOnClickListener(v -> {
                btnSearchTrigger.setVisibility(View.GONE);
                if (textSearchTitle != null) textSearchTitle.setVisibility(View.GONE);
                if (layoutSearchBar != null) layoutSearchBar.setVisibility(View.VISIBLE);
                editSearch.requestFocus();
                showKeyboard(editSearch);
            });
        }

        if (btnCloseSearch != null) {
            btnCloseSearch.setOnClickListener(v -> {
                editSearch.setText("");
                if (layoutSearchBar != null) layoutSearchBar.setVisibility(View.GONE);
                btnSearchTrigger.setVisibility(View.VISIBLE);
                if (textSearchTitle != null) textSearchTitle.setVisibility(View.VISIBLE);
                hideKeyboard(editSearch);
                showDefaultDataForTab();
            });
        }

        dropdownButton.setOnClickListener(v -> showDropdownMenu());
        filterDropdownButton.setOnClickListener(v -> showFilterDropdownMenu());
    }

    private void showKeyboard(View view) {
        if (view.requestFocus()) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard(View view) {
        if (getActivity() == null) return;
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        view.clearFocus();
    }

    private void showDropdownMenu() {
        android.widget.ListPopupWindow listPopupWindow = new android.widget.ListPopupWindow(getContext());
        listPopupWindow.setAnchorView(dropdownButton);
        
        String[] labels = {"Bài hát", "Nghệ sĩ", "Thể loại", "Album"};
        int[] icons = {R.drawable.ic_music_note, R.drawable.ic_nav_profile, R.drawable.ic_musical_notes, R.drawable.ic_albums_outline};
        int[] colors = {Color.parseColor("#2196F3"), Color.parseColor("#E91E63"), Color.parseColor("#FF9800"), Color.parseColor("#9C27B0")};

        DropdownAdapter menuAdapter = new DropdownAdapter(labels, icons, colors, currentTab);
        listPopupWindow.setAdapter(menuAdapter);
        listPopupWindow.setWidth(dropdownButton.getWidth());
        listPopupWindow.setHeight(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        listPopupWindow.setModal(true);
        
        listPopupWindow.setBackgroundDrawable(androidx.core.content.ContextCompat.getDrawable(getContext(), R.drawable.bg_dropdown_menu_modern));
        listPopupWindow.setVerticalOffset(4);

        listPopupWindow.setOnItemClickListener((parent, view, position, id) -> {
            setTab(position);
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
            img.setImageTintList(ColorStateList.valueOf(colors[position]));
            txt.setText(labels[position]);
            
            if (position == selectedIndex) {
                txt.setTextColor(getResources().getColor(R.color.vibrant_purple));
                txt.setTypeface(null, android.graphics.Typeface.BOLD);
                check.setVisibility(View.VISIBLE);
            } else {
                txt.setTextColor(Color.WHITE);
                txt.setTypeface(null, android.graphics.Typeface.NORMAL);
                check.setVisibility(View.GONE);
            }
            return convertView;
        }
    }

    private void showFilterDropdownMenu() {
        android.widget.ListPopupWindow listPopupWindow = new android.widget.ListPopupWindow(getContext());
        listPopupWindow.setAnchorView(filterDropdownButton);
        
        String[] labels = {"Tất cả", "Premium", "Miễn phí"};
        String[] values = {"all", "premium", "free"};
        int[] icons = {R.drawable.ic_albums_outline, R.drawable.ic_star, R.drawable.ic_music_note};
        int[] colors = {Color.GRAY, Color.parseColor("#FFD700"), Color.parseColor("#4CAF50")};

        int selectedIndex = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(currentFilter)) {
                selectedIndex = i;
                break;
            }
        }

        DropdownAdapter menuAdapter = new DropdownAdapter(labels, icons, colors, selectedIndex);
        listPopupWindow.setAdapter(menuAdapter);
        listPopupWindow.setWidth(filterDropdownButton.getWidth() + 100);
        listPopupWindow.setHeight(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        listPopupWindow.setModal(true);
        listPopupWindow.setBackgroundDrawable(androidx.core.content.ContextCompat.getDrawable(getContext(), R.drawable.bg_dropdown_menu_modern));

        listPopupWindow.setOnItemClickListener((parent, view, position, id) -> {
            setFilter(values[position]);
            listPopupWindow.dismiss();
        });

        listPopupWindow.show();
    }

    private void setTab(int tabIndex) {
        currentTab = tabIndex;
        updateTabUI();
        
        // Update Filter Visibility
        filterDropdownButton.setVisibility(View.GONE);
        btnSortTime.setVisibility(View.GONE);
        btnSortListens.setVisibility(View.GONE);
        btnSortRating.setVisibility(View.GONE);
        btnSortName.setVisibility(View.GONE);

        if (currentTab != 2) { // Not for Genres
            filterDropdownButton.setVisibility(View.VISIBLE);
            btnSortTime.setVisibility(View.VISIBLE);
            btnSortName.setVisibility(View.VISIBLE);
            
            if (currentTab == 0 || currentTab == 1) { // Songs/Artists
                btnSortListens.setVisibility(View.VISIBLE);
            }
            if (currentTab == 0) { // Only Songs for Rating for now
                btnSortRating.setVisibility(View.VISIBLE);
            }
        }

        if (currentQuery.length() >= 2) performSearch(currentQuery);
        else showDefaultDataForTab();
    }

    private void updateTabUI() {
        String label = "Bài hát";
        int iconRes = R.drawable.ic_music_note;
        int iconColor = Color.parseColor("#2196F3");

        switch (currentTab) {
            case 1:
                label = "Nghệ sĩ";
                iconRes = R.drawable.ic_nav_profile;
                iconColor = Color.parseColor("#E91E63");
                break;
            case 2:
                label = "Thể loại";
                iconRes = R.drawable.ic_musical_notes;
                iconColor = Color.parseColor("#FF9800");
                break;
            case 3:
                label = "Album";
                iconRes = R.drawable.ic_albums_outline;
                iconColor = Color.parseColor("#9C27B0");
                break;
        }

        textTabLabel.setText(label);
        imgTabIcon.setImageResource(iconRes);
        imgTabIcon.setImageTintList(ColorStateList.valueOf(iconColor));
    }

    private void setupFilters() {
        // Buttons are already initialized in onCreateView, no need to find them again here via getView() which causes NPE during creation
        
        btnSortTime.setOnClickListener(v -> {
            if (currentSort.equals("newest")) setSort("oldest");
            else setSort("newest");
        });
        btnSortName.setOnClickListener(v -> {
            if (currentSort.equals("name_asc")) setSort("name_desc");
            else setSort("name_asc");
        });
        btnSortListens.setOnClickListener(v -> {
            if (currentSort.equals("listens_desc")) setSort("listens_asc");
            else setSort("listens_desc");
        });
        btnSortRating.setOnClickListener(v -> {
            if (currentSort.equals("rating_desc")) setSort("rating_asc");
            else setSort("rating_desc");
        });
        
        updateFilterUI();
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateFilterUI();
        if (currentQuery.length() >= 2) performSearch(currentQuery);
        else showDefaultDataForTab();
    }

    private void setSort(String sort) {
        currentSort = sort;
        updateFilterUI();
        
        // Reset pagination and reload from server for accurate sorting
        offset = 0;
        hasMore = true;
        searchResults.clear();
        adapter.notifyDataSetChanged();
        
        if (currentQuery.length() >= 2) {
            performSearch(currentQuery);
        } else {
            // Re-fetch default data with sorting
            loadInitialData();
        }
    }

    private void updateFilterUI() {
        int colorActive = getResources().getColor(R.color.vibrant_purple);
        int colorInactive = Color.parseColor("#12FFFFFF");
        int textActive = Color.WHITE;
        int textInactive = Color.parseColor("#B3B3B3");
        int strokeInactive = Color.parseColor("#1AFFFFFF");

        String label = "Tất cả";
        if (currentFilter.equals("premium")) label = "Premium";
        else if (currentFilter.equals("free")) label = "Miễn phí";
        textFilterLabel.setText(label);

        // Sort Time
        boolean isTime = currentSort.equals("newest") || currentSort.equals("oldest");
        updateChipState(btnSortTime, isTime, colorActive, colorInactive, textActive, textInactive, strokeInactive);
        btnSortTime.setText(currentSort.equals("oldest") ? "Cũ nhất" : "Mới nhất");
        btnSortTime.setIconResource(currentSort.equals("oldest") ? R.drawable.ic_chevron_down : R.drawable.ic_chevron_up);
        btnSortTime.setIconTint(ColorStateList.valueOf(isTime ? textActive : textInactive));

        // Sort Name
        boolean isName = currentSort.equals("name_asc") || currentSort.equals("name_desc");
        updateChipState(btnSortName, isName, colorActive, colorInactive, textActive, textInactive, strokeInactive);
        btnSortName.setText(currentSort.equals("name_desc") ? "Tên Z-A" : "Tên A-Z");

        // Sort Listens
        boolean isListens = currentSort.startsWith("listens");
        updateChipState(btnSortListens, isListens, colorActive, colorInactive, textActive, textInactive, strokeInactive);
        btnSortListens.setIconResource(currentSort.equals("listens_asc") ? R.drawable.ic_chevron_up : R.drawable.ic_chevron_down);
        btnSortListens.setIconTint(ColorStateList.valueOf(isListens ? textActive : textInactive));

        // Sort Rating
        boolean isRating = currentSort.startsWith("rating");
        updateChipState(btnSortRating, isRating, colorActive, colorInactive, textActive, textInactive, strokeInactive);
        btnSortRating.setIconResource(currentSort.equals("rating_asc") ? R.drawable.ic_chevron_up : R.drawable.ic_chevron_down);
        btnSortRating.setIconTint(ColorStateList.valueOf(isRating ? textActive : textInactive));
    }

    private void updateChipState(com.google.android.material.button.MaterialButton btn, boolean isActive, 
                             int colorActive, int colorInactive, int textActive, int textInactive, int strokeInactive) {
        btn.setBackgroundTintList(ColorStateList.valueOf(isActive ? colorActive : colorInactive));
        btn.setTextColor(isActive ? textActive : textInactive);
        btn.setStrokeColor(ColorStateList.valueOf(isActive ? Color.TRANSPARENT : strokeInactive));
        btn.setStrokeWidth(isActive ? 0 : 3); // 3px for unselected
    }

    private void loadInitialData() {
        if (!isAdded()) return;
        progressBar.setVisibility(View.VISIBLE);
        offset = 0;
        hasMore = true;
        isLoading = true;
        
        // Load songs with current sorting
        RetrofitClient.getApiService().searchSongs("", LIMIT, 0, currentSort).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        Gson gson = new Gson();
                        allSongs = gson.fromJson(gson.toJson(data), new TypeToken<List<Song>>(){}.getType());
                        if (currentQuery.isEmpty() && currentTab == 0) showDefaultDataForTab();
                    }
                }
                isLoading = false;
                if (isAdded()) progressBar.setVisibility(View.GONE);
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isLoading = false;
                if (isAdded()) progressBar.setVisibility(View.GONE);
            }
        });

        RetrofitClient.getApiService().getAllArtists().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        Gson gson = new Gson();
                        allArtists = gson.fromJson(gson.toJson(data), new TypeToken<List<Artist>>(){}.getType());
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });

        RetrofitClient.getApiService().getAllAlbums(LIMIT, 0).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        Gson gson = new Gson();
                        allAlbums = gson.fromJson(gson.toJson(data), new TypeToken<List<Album>>(){}.getType());
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });

        RetrofitClient.getApiService().getGenres().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        Gson gson = new Gson();
                        allGenres = gson.fromJson(gson.toJson(data), new TypeToken<List<Genre>>(){}.getType());
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void showDefaultDataForTab() {
        searchResults.clear();
        if (currentTab == 0) {
            List<Song> filtered = new ArrayList<>();
            for (Song s : allSongs) if (applyFilter(s)) filtered.add(s);
            sortItems(filtered);
            searchResults.addAll(filtered);
        } else if (currentTab == 1) {
            List<Artist> sorted = new ArrayList<>(allArtists);
            sortItems(sorted);
            searchResults.addAll(sorted);
        } else if (currentTab == 2) {
            searchResults.addAll(allGenres);
        } else {
            List<Album> filtered = new ArrayList<>();
            for (Album a : allAlbums) if (applyFilter(a)) filtered.add(a);
            sortItems(filtered);
            searchResults.addAll(filtered);
        }
        
        updateUI();
    }

    private <T> void sortItems(List<T> list) {
        if (list == null || list.isEmpty()) return;
        Collections.sort(list, (a, b) -> {
            if (currentSort.startsWith("listens")) {
                int res = 0;
                if (a instanceof Song && b instanceof Song) {
                    res = Long.compare(((Song) a).getListenCount(), ((Song) b).getListenCount());
                } else if (a instanceof Artist && b instanceof Artist) {
                    res = Integer.compare(((Artist) a).getSongCount(), ((Artist) b).getSongCount());
                }
                return currentSort.endsWith("asc") ? res : -res;
            } else if (currentSort.startsWith("rating")) {
                int res = 0;
                if (a instanceof Song && b instanceof Song) {
                    double r1 = ((Song) a).getStars() != null ? ((Song) a).getStars() : 0.0;
                    double r2 = ((Song) b).getStars() != null ? ((Song) b).getStars() : 0.0;
                    res = Double.compare(r1, r2);
                }
                return currentSort.endsWith("asc") ? res : -res;
            } else if ("newest".equals(currentSort)) {
                if (a instanceof Song) return safeStr(((Song) b).getReleaseDate()).compareTo(safeStr(((Song) a).getReleaseDate()));
                if (a instanceof Album) return safeStr(((Album) b).getReleaseDate()).compareTo(safeStr(((Album) a).getReleaseDate()));
                if (a instanceof Artist) return ((Artist) b).getArtistId() - ((Artist) a).getArtistId();
            } else if ("oldest".equals(currentSort)) {
                if (a instanceof Song) return safeStr(((Song) a).getReleaseDate()).compareTo(safeStr(((Song) b).getReleaseDate()));
                if (a instanceof Album) return safeStr(((Album) a).getReleaseDate()).compareTo(safeStr(((Album) b).getReleaseDate()));
                if (a instanceof Artist) return ((Artist) a).getArtistId() - ((Artist) b).getArtistId();
            } else if (currentSort.startsWith("name")) {
                String n1 = "", n2 = "";
                if (a instanceof Song) { n1 = ((Song) a).getTitle(); n2 = ((Song) b).getTitle(); }
                else if (a instanceof Album) { n1 = ((Album) a).getTitle(); n2 = ((Album) b).getTitle(); }
                else if (a instanceof Artist) { n1 = ((Artist) a).getName(); n2 = ((Artist) b).getName(); }
                int res = n1.compareToIgnoreCase(n2);
                return currentSort.endsWith("desc") ? -res : res;
            }
            return 0;
        });
    }

    private String safeStr(String s) { return s == null ? "" : s; }

    private boolean applyFilter(Object item) {
        if (currentFilter.equals("all")) return true;
        boolean isPremium = false;
        if (item instanceof Song) {
            Song s = (Song) item;
            isPremium = Integer.valueOf(1).equals(s.getIsPremium()) || Integer.valueOf(1).equals(s.getIsAlbumPremium());
        } else if (item instanceof Album) {
            Album a = (Album) item;
            isPremium = Integer.valueOf(1).equals(a.getIsPremium());
        }
        return currentFilter.equals("premium") ? isPremium : !isPremium;
    }

    private void performSearch(String query) {
        if (!isAdded()) return;
        progressBar.setVisibility(View.VISIBLE);
        textNoResults.setVisibility(View.GONE);
        isLoading = true;
        offset = 0;
        hasMore = true;

        // If tab is Songs, we use the specific searchSongs endpoint for sorting
        if (currentTab == 0) {
            RetrofitClient.getApiService().searchSongs(query, LIMIT, 0, currentSort).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (!isAdded()) return;
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        Object data = response.body().get("data");
                        searchResults.clear();
                        if (data instanceof List) {
                            Gson gson = new Gson();
                            List<Song> songs = gson.fromJson(gson.toJson(data), new TypeToken<List<Song>>(){}.getType());
                            for (Song s : songs) if (applyFilter(s)) searchResults.add(s);
                        }
                        updateUI();
                    }
                }
                @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    if (!isAdded()) return;
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                }
            });
            return;
        }

        RetrofitClient.getApiService().searchAll(query).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> dataArr = (Map<String, Object>) response.body().get("data");
                    searchResults.clear();
                    Gson gson = new Gson();

                    if (currentTab == 0) {
                        Object songsObj = dataArr.get("songs");
                        if (songsObj != null) {
                            List<Song> songs = gson.fromJson(gson.toJson(songsObj), new TypeToken<List<Song>>(){}.getType());
                            for (Song s : songs) if (applyFilter(s)) searchResults.add(s);
                        }
                    } else if (currentTab == 1) {
                        Object artistsObj = dataArr.get("artists");
                        if (artistsObj != null) {
                            List<Artist> artists = gson.fromJson(gson.toJson(artistsObj), new TypeToken<List<Artist>>(){}.getType());
                            searchResults.addAll(artists);
                        }
                    } else if (currentTab == 2) {
                        // Backend might not return genres in universal search, we filter local allGenres
                        for (Genre g : allGenres) {
                            if (g.getName().toLowerCase().contains(query.toLowerCase())) searchResults.add(g);
                        }
                    } else {
                        Object albumsObj = dataArr.get("albums");
                        if (albumsObj != null) {
                            List<Album> albums = gson.fromJson(gson.toJson(albumsObj), new TypeToken<List<Album>>(){}.getType());
                            for (Album a : albums) if (applyFilter(a)) searchResults.add(a);
                        }
                    }
                    
                    sortItems(searchResults);
                    updateUI();
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void updateUI() {
        if (searchResults.isEmpty()) {
            textNoResults.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            textNoResults.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private void setupScrollListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && currentQuery.isEmpty() && currentTab == 0) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        int visibleItemCount = layoutManager.getChildCount();
                        int totalItemCount = layoutManager.getItemCount();
                        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                        if (!isLoading && hasMore) {
                            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount) {
                                loadMoreSongs();
                            }
                        }
                    }
                }
            }
        });
    }

    private void loadMoreSongs() {
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        int nextOffset = offset + LIMIT;

        // Use searchSongs for paginated, sorted results even when not searching
        RetrofitClient.getApiService().searchSongs(currentQuery, LIMIT, nextOffset, currentSort).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isLoading = false;
                if (isAdded()) progressBar.setVisibility(View.GONE);
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        Gson gson = new Gson();
                        List<Song> newSongs = gson.fromJson(gson.toJson(data), new TypeToken<List<Song>>(){}.getType());
                        if (newSongs != null && !newSongs.isEmpty()) {
                            offset = nextOffset; // Update offset only on success
                            allSongs.addAll(newSongs);
                            if (currentTab == 0) {
                                for (Song s : newSongs) if (applyFilter(s)) searchResults.add(s);
                                adapter.notifyDataSetChanged();
                            }
                        } else {
                            hasMore = false;
                        }
                    } else hasMore = false;
                } else hasMore = false;
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isLoading = false;
                if (isAdded()) progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void setupPlayerListener() {
        playerStatusChangeListener = new MusicPlayerManager.OnPlayerStatusChangeListener() {
            @Override public void onSongChanged(Song song) { adapter.notifyDataSetChanged(); }
            @Override public void onPlaybackStatusChanged(boolean isPlaying) { adapter.notifyDataSetChanged(); }
            @Override public void onProgressUpdated(int position, int duration) {}
        };
        MusicPlayerManager.getInstance().addStatusChangeListener(playerStatusChangeListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (playerStatusChangeListener != null) MusicPlayerManager.getInstance().removeStatusChangeListener(playerStatusChangeListener);
    }

    private class UniversalSearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<Object> items;
        private static final int TYPE_SONG = 1;
        private static final int TYPE_ARTIST = 2;
        private static final int TYPE_ALBUM = 3;
        private static final int TYPE_GENRE = 4;

        UniversalSearchAdapter(List<Object> items) { this.items = items; }

        @Override
        public int getItemViewType(int position) {
            Object item = items.get(position);
            if (item instanceof Song) return TYPE_SONG;
            if (item instanceof Artist) return TYPE_ARTIST;
            if (item instanceof Album) return TYPE_ALBUM;
            return TYPE_GENRE;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_ARTIST) return new ArtistVH(inf.inflate(R.layout.item_search_artist, parent, false));
            if (viewType == TYPE_ALBUM) return new AlbumVH(inf.inflate(R.layout.item_search_album, parent, false));
            if (viewType == TYPE_GENRE) return new GenreVH(inf.inflate(R.layout.item_genre, parent, false));
            return new SongVH(inf.inflate(R.layout.item_song, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object item = items.get(position);

            if (item instanceof Song) {
                SongVH vh = (SongVH) holder;
                Song s = (Song) item;
                vh.bind(s);
            } else if (item instanceof Artist) {
                ArtistVH vh = (ArtistVH) holder;
                vh.bind((Artist) item);
            } else if (item instanceof Album) {
                AlbumVH vh = (AlbumVH) holder;
                vh.bind((Album) item);
            } else if (item instanceof Genre) {
                GenreVH vh = (GenreVH) holder;
                vh.bind((Genre) item);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        class SongVH extends RecyclerView.ViewHolder {
            TextView title, subtitle, listens, stars, duration;
            ImageView img;
            com.appad.components.PremiumBadgeView badgePremium;
            com.appad.components.AccessBadgeView accessBadge;
            ImageButton btnPlayPause, btnMore;
            View body;

            SongVH(View v) {
                super(v);
                title = v.findViewById(R.id.textSongTitle);
                subtitle = v.findViewById(R.id.textArtistGenre);
                img = v.findViewById(R.id.imgSongCover);
                badgePremium = v.findViewById(R.id.badgePremium);
                accessBadge = v.findViewById(R.id.accessBadge);
                listens = v.findViewById(R.id.textListens);
                stars = v.findViewById(R.id.textStars);
                duration = v.findViewById(R.id.textDuration);
                btnPlayPause = v.findViewById(R.id.btnPlayFast);
                btnMore = v.findViewById(R.id.btnMore);
                body = v.findViewById(R.id.llSongBody);

                // Hover animation (lift and float)
                body.setOnHoverListener((view, event) -> {
                    switch (event.getAction()) {
                        case android.view.MotionEvent.ACTION_HOVER_ENTER:
                            android.view.animation.Animation floatAnim = android.view.animation.AnimationUtils.loadAnimation(getContext(), R.anim.hover_float);
                            view.startAnimation(floatAnim);
                            view.animate().scaleX(1.02f).scaleY(1.02f).setDuration(200).start();
                            break;
                        case android.view.MotionEvent.ACTION_HOVER_EXIT:
                            view.clearAnimation();
                            view.animate().scaleX(1.0f).scaleY(1.0f).translationY(0).setDuration(200).start();
                            break;
                    }
                    return false;
                });
            }


            void bind(Song s) {
                title.setText(s.getTitle());
                subtitle.setText(s.getArtistName() + (s.getAlbumTitle() != null ? " • " + s.getAlbumTitle() : ""));
                listens.setText(com.appad.utils.FormatUtils.formatCount(s.getListenCount()));
                stars.setText(String.format("%.1f", s.getStars() != null ? s.getStars() : 0.0));
                duration.setText(formatTime(s.getDuration()));

                Glide.with(getContext()).load(com.appad.utils.ImageUrlUtils.fixUrl(s.getCoverUrl())).placeholder(R.drawable.placeholder_song).into(img);

                badgePremium.setVisibility(View.GONE);
                if (Integer.valueOf(1).equals(s.getIsPremium())) {
                    badgePremium.setVisibility(View.VISIBLE);
                    badgePremium.setText("PREMIUM");
                } else if (Integer.valueOf(1).equals(s.getIsAlbumPremium())) {
                    badgePremium.setVisibility(View.VISIBLE);
                    badgePremium.setText("ALBUM PRE");
                }

                accessBadge.setVisibility(View.GONE);
                String accessType = getAccessType(s);
                if (accessType != null) {
                    accessBadge.setVisibility(View.VISIBLE);
                    accessBadge.setAccessType(accessType);
                }

                Song current = MusicPlayerManager.getInstance().getCurrentSong();
                boolean isCurrent = current != null && current.getSongId().equals(s.getSongId());
                boolean isPlaying = MusicPlayerManager.getInstance().isPlaying();

                body.setBackgroundResource(isCurrent ? R.drawable.bg_song_card_active : R.drawable.bg_song_card);
                btnPlayPause.setImageResource(isCurrent && isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);

                btnPlayPause.setOnClickListener(v -> {
                    if (isCurrent) MusicPlayerManager.getInstance().togglePlayPause();
                    else playThis(s);
                });
                body.setOnClickListener(v -> {
                    if (MusicPlayerManager.getInstance().hasAccess(s)) {
                        // User requested auto-play on entry
                        openFullPlayer(s, true);
                    } else {
                        com.appad.components.PremiumAccessModal.newInstance(s, () -> adapter.notifyDataSetChanged())
                            .show(getParentFragmentManager(), "PremiumAccessModal");
                    }
                });
                btnMore.setOnClickListener(v -> showMore(s));
            }
        }

        class ArtistVH extends RecyclerView.ViewHolder {
            TextView name, stats, bio;
            ImageView img;
            com.google.android.material.button.MaterialButton btnFollow;

            ArtistVH(View v) {
                super(v);
                name = v.findViewById(R.id.textArtistName);
                stats = v.findViewById(R.id.textArtistStats);
                bio = v.findViewById(R.id.textArtistBio);
                img = v.findViewById(R.id.imgArtistAvatar);
                btnFollow = v.findViewById(R.id.btnFollow);
            }

            void bind(Artist a) {
                name.setText(a.getName());
                stats.setText("Nghệ sĩ");
                bio.setText(a.getBio() != null ? a.getBio() : "Chưa có tiểu sử.");
                Glide.with(getContext()).load(com.appad.utils.ImageUrlUtils.fixUrl(a.getImageUrl())).circleCrop().placeholder(R.drawable.ic_nav_profile).into(img);
                checkFollowStatus(a.getArtistId(), btnFollow);
                btnFollow.setOnClickListener(v -> toggleFollow(a.getArtistId(), btnFollow));
                itemView.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(getContext(), com.appad.activities.ArtistDetailActivity.class);
                    intent.putExtra("artistId", a.getArtistId());
                    startActivity(intent);
                });
            }
        }

        class AlbumVH extends RecyclerView.ViewHolder {
            TextView title, artist, price;
            ImageView img;
            com.appad.components.AccessBadgeView accessBadge;

            AlbumVH(View v) {
                super(v);
                title = v.findViewById(R.id.textAlbumTitle);
                artist = v.findViewById(R.id.textAlbumArtist);
                price = v.findViewById(R.id.textAlbumPrice);
                img = v.findViewById(R.id.imgAlbumCover);
                accessBadge = v.findViewById(R.id.accessBadge);
            }

            void bind(Album a) {
                title.setText(a.getTitle());
                artist.setText(a.getArtistName());
                if (a.getPrice() != null && a.getPrice() > 0) {
                    price.setText(String.format("%,.0fđ", a.getPrice()));
                    price.setTextColor(Color.parseColor("#8B5CF6"));
                } else {
                    price.setText("Miễn phí");
                    price.setTextColor(Color.parseColor("#10B981"));
                }
                Glide.with(getContext()).load(com.appad.utils.ImageUrlUtils.fixUrl(a.getCoverUrl())).placeholder(R.drawable.placeholder_song).into(img);
                
                accessBadge.setVisibility(View.GONE);
                if (Integer.valueOf(1).equals(a.getIsPremium())) {
                    com.appad.models.User user = com.appad.utils.SessionManager.getInstance(getContext()).getUser();
                    if (user != null && Integer.valueOf(1).equals(user.getIsPremium())) {
                        accessBadge.setVisibility(View.VISIBLE);
                        accessBadge.setAccessType("premium");
                    }
                }

                itemView.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(getContext(), com.appad.activities.AlbumDetailActivity.class);
                    intent.putExtra("albumId", a.getAlbumId());
                    startActivity(intent);
                });
            }
        }

        class GenreVH extends RecyclerView.ViewHolder {
            TextView name, count, countLabel, description;
            ImageView img;

            GenreVH(View v) {
                super(v);
                name = v.findViewById(R.id.textGenreName);
                count = v.findViewById(R.id.textGenreCount);
                countLabel = v.findViewById(R.id.textGenreCountLabel);
                description = v.findViewById(R.id.textGenreDescription);
                img = v.findViewById(R.id.imgGenreAvatar);
            }

            void bind(Genre g) {
                name.setText(g.getName());
                count.setText(String.valueOf(g.getSongCount() != null ? g.getSongCount() : 0));
                description.setText(g.getDescription());
                
                Glide.with(getContext())
                    .load(com.appad.utils.ImageUrlUtils.fixUrl(g.getCoverUrl()))
                    .placeholder(R.drawable.ic_musical_notes)
                    .circleCrop()
                    .into(img);

                itemView.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(getContext(), com.appad.activities.GenreDetailActivity.class);
                    intent.putExtra("genreId", g.getGenreId());
                    intent.putExtra("genreName", g.getName());
                    startActivity(intent);
                });
            }
        }

        private String getAccessType(Song s) {
            com.appad.models.User user = com.appad.utils.SessionManager.getInstance(getContext()).getUser();
            boolean userIsPremium = user != null && Integer.valueOf(1).equals(user.getIsPremium());
            if (Boolean.TRUE.equals(s.getBought())) return "purchased";
            if (Boolean.TRUE.equals(s.getAlbumBought())) return "album_purchased";
            if (Boolean.TRUE.equals(s.getIsArtistOwner())) return "artist_owner";
            if (Boolean.TRUE.equals(s.getArtistMember())) return "artist_membership";
            if (userIsPremium && (Integer.valueOf(1).equals(s.getIsPremium()) || Integer.valueOf(1).equals(s.getIsAlbumPremium()))) return "premium";
            return null;
        }

        private void playThis(Song s) {
            com.appad.utils.AccessHelper.checkAccess(getContext(), s, true, () -> {
                List<Song> playlist = new ArrayList<>();
                for (Object o : searchResults) if (o instanceof Song) playlist.add((Song) o);
                int idx = playlist.indexOf(s);
                MusicPlayerManager.getInstance().setPlaylist(playlist, idx);
                notifyDataSetChanged();
            });
        }

        private void openFullPlayer(Song s, boolean autoPlay) {
            android.content.Intent intent = new android.content.Intent(getContext(), com.appad.activities.FullPlayerActivity.class);
            intent.putExtra("title", s.getTitle());
            intent.putExtra("artist", s.getArtistName());
            intent.putExtra("cover", s.getCoverUrl());
            intent.putExtra("genre", s.getGenreName());
            
            // Pass playlist info if needed
            Song current = MusicPlayerManager.getInstance().getCurrentSong();
            // If checking access for body click (not play), we might be opening a song that IS NOT currently playing logic-wise unless we force set logic.
            // But requirement is: open full player, NOT auto play.
            // If we just open full player without setting playlist, FullPlayer might show EMPTY or Old state.
            // So we should passed the data to be READY to play.
            
            // Let's create playlist from search results
            List<Song> songList = new ArrayList<>();
             for(Object item : searchResults) {
                 if (item instanceof Song) songList.add((Song) item);
             }
             int startIndex = -1;
             for(int i=0; i<songList.size(); i++) {
                 if (songList.get(i).getSongId().equals(s.getSongId())) {
                     startIndex = i;
                     break;
                 }
             }
             
             if (startIndex != -1) {
                 intent.putExtra("playlist_json", new Gson().toJson(songList));
                 intent.putExtra("start_index", startIndex);
             }

            intent.putExtra("auto_play", autoPlay);
            startActivity(intent);
        }

        private void openFullPlayer(Song s) {
            openFullPlayer(s, true);
        }

        private void showMore(Song s) {
            com.appad.components.AddToPlaylistFragment fragment = com.appad.components.AddToPlaylistFragment.newInstance(s);
            fragment.show(getActivity().getSupportFragmentManager(), "add_to_playlist");
        }

        private String formatTime(Integer sec) {
            if (sec == null) return "0:00";
            return String.format("%d:%02d", sec / 60, sec % 60);
        }

        private String formatCount(Long count) {
            return com.appad.utils.FormatUtils.formatCount(count);
        }

        private void checkFollowStatus(Integer artistId, com.google.android.material.button.MaterialButton btn) {
            Integer userId = com.appad.utils.SessionManager.getInstance(getContext()).getUserId();
            if (userId == null) return;
            RetrofitClient.getApiService().checkFollow(userId, artistId).enqueue(new Callback<Map<String, Object>>() {
                @Override public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) updateFollowBtn(btn, Boolean.TRUE.equals(response.body().get("following")));
                }
                @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
            });
        }

        private void toggleFollow(Integer artistId, com.google.android.material.button.MaterialButton btn) {
            Integer userId = com.appad.utils.SessionManager.getInstance(getContext()).getUserId();
            if (userId == null) return;
            Map<String, Integer> payload = new java.util.HashMap<>();
            payload.put("userId", userId);
            payload.put("artistId", artistId);
            RetrofitClient.getApiService().toggleFollow(payload).enqueue(new Callback<Map<String, Object>>() {
                @Override public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) updateFollowBtn(btn, Boolean.TRUE.equals(response.body().get("following")));
                }
                @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
            });
        }

        private void updateFollowBtn(com.google.android.material.button.MaterialButton btn, boolean follow) {
            btn.setText(follow ? "Đang theo dõi" : "Theo dõi");
            btn.setBackgroundTintList(ColorStateList.valueOf(follow ? Color.DKGRAY : Color.TRANSPARENT));
            btn.setTextColor(follow ? Color.WHITE : Color.parseColor("#8B5CF6"));
            btn.setStrokeColor(ColorStateList.valueOf(follow ? Color.TRANSPARENT : Color.parseColor("#8B5CF6")));
        }
    }

    public void refreshData() {
        if (isAdded()) {
            loadInitialData();
            if (currentQuery.length() >= 2) {
                performSearch(currentQuery);
            } else {
                showDefaultDataForTab();
            }
        }
    }
}
