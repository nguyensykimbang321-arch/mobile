package com.appad.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.appad.R;
import com.appad.fragments.HomeFragment;
import com.appad.fragments.LibraryFragment;
import com.appad.fragments.SearchFragment;
import com.appad.models.Song;
import com.appad.utils.MusicPlayerManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;
import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity {
    
    private Fragment homeFragment;
    private Fragment libraryFragment;
    private Fragment searchFragment;
    private Fragment profileFragment;
    private Fragment activeFragment;

    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
            
            // Initialize fragments
            homeFragment = getSupportFragmentManager().findFragmentByTag("HOME");
            libraryFragment = getSupportFragmentManager().findFragmentByTag("LIBRARY");
            searchFragment = getSupportFragmentManager().findFragmentByTag("SEARCH");
            profileFragment = getSupportFragmentManager().findFragmentByTag("PROFILE");

            if (homeFragment == null) homeFragment = new HomeFragment();
            if (libraryFragment == null) libraryFragment = new LibraryFragment();
            if (searchFragment == null) searchFragment = new SearchFragment();
            if (profileFragment == null) profileFragment = new com.appad.fragments.ProfileFragment();

            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, homeFragment, "HOME")
                        .commit();
                activeFragment = homeFragment;
            } else {
                activeFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (activeFragment == null && homeFragment != null && homeFragment.isVisible()) {
                    activeFragment = homeFragment;
                }
            }

            bottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    switchFragment(homeFragment, "HOME");
                    return true;
                } else if (id == R.id.nav_library) {
                    switchFragment(libraryFragment, "LIBRARY");
                    return true;
                } else if (id == R.id.nav_search) {
                    switchFragment(searchFragment, "SEARCH");
                    return true;
                } else if (id == R.id.nav_profile) {
                    switchFragment(profileFragment, "PROFILE");
                    return true;
                }
                return false;
            });

            // Request Notification Permission for Android 13+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
                }
            }

            // Setup Mini Player will be called in onResume
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khởi động: " + e.getMessage(), Toast.LENGTH_LONG).show();
            android.util.Log.e("Appad", "MainActivity Crash", e);
        }
    }

    private void setupMiniPlayerSync() {
        if (miniPlayerHelper == null) {
            miniPlayerHelper = new com.appad.utils.MiniPlayerHelper(this);
        }
        miniPlayerHelper.setupMiniPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupMiniPlayerSync();
    }

    private String formatTime(int ms) {
        int minutes = (ms / 1000) / 60;
        int seconds = (ms / 1000) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (miniPlayerHelper != null) {
            miniPlayerHelper.detach();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // removeStatusChangeListener is already handled in onPause
    }

    private void switchFragment(Fragment fragment, String tag) {
        if (fragment == activeFragment) return;

        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (activeFragment != null) {
            transaction.hide(activeFragment);
        }

        if (!fragment.isAdded()) {
            transaction.add(R.id.fragment_container, fragment, tag);
        } else {
            transaction.show(fragment);
        }

        activeFragment = fragment;
        transaction.commit();
    }

    public void backButton(View view) {
        Toast.makeText(this, "Back Clicked", Toast.LENGTH_SHORT).show();
    }
    public void fwdButton(View view) {
        Toast.makeText(this, "Forward Clicked", Toast.LENGTH_SHORT).show();
    }
    public void reloadButton(View view) {
        Toast.makeText(this, "Reload Clicked", Toast.LENGTH_SHORT).show();
    }
    public void goBack(View view) {
        Toast.makeText(this, "Go Clicked", Toast.LENGTH_SHORT).show();
    }
}
