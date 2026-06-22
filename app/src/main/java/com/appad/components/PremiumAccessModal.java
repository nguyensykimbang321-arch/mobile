package com.appad.components;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.appad.R;
import com.appad.models.Song;
import com.appad.utils.ImageUrlUtils;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import com.appad.utils.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PremiumAccessModal extends BottomSheetDialogFragment {

    private Song song;
    private OnPurchaseCompleteListener listener;
    private double currentBalance = 0;
    private Map<String, Object> membershipInfo;
    private boolean isDownloadMode = false;

    public interface OnPurchaseCompleteListener {
        void onPurchaseComplete();
    }

    public static PremiumAccessModal newInstance(Song song, OnPurchaseCompleteListener listener) {
        return newInstance(song, false, listener);
    }

    public static PremiumAccessModal newInstance(Song song, boolean isDownloadMode, OnPurchaseCompleteListener listener) {
        PremiumAccessModal fragment = new PremiumAccessModal();
        fragment.song = song;
        fragment.isDownloadMode = isDownloadMode;
        fragment.listener = listener;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_premium_access_modal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (song == null) {
            dismiss();
            return;
        }

        initViews(view);
        fetchData();
    }

    @Override
    public void onStart() {
        super.onStart();
        View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            com.google.android.material.bottomsheet.BottomSheetBehavior<?> behavior = 
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
            behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
    }

    private void initViews(View view) {
        ImageView imgCover = view.findViewById(R.id.imgSongCover);
        TextView txtTitle = view.findViewById(R.id.txtSongTitle);
        TextView txtArtist = view.findViewById(R.id.txtArtistName);
        TextView txtSongPriceValue = view.findViewById(R.id.txtSongPrice);
        TextView txtAlbumPriceValue = view.findViewById(R.id.txtAlbumPrice);

        txtTitle.setText(song.getTitle());
        txtArtist.setText(song.getArtistName());
        
        double sPrice = (song.getPrice() != null && song.getPrice() > 0) ? song.getPrice() : 15000.0;
        double aPrice = (song.getAlbumPrice() != null && song.getAlbumPrice() > 0) ? song.getAlbumPrice() : 150000.0;
        
        txtSongPriceValue.setText(formatCurrency(sPrice));
        txtAlbumPriceValue.setText(formatCurrency(aPrice));

        Glide.with(this)
                .load(ImageUrlUtils.fixUrl(song.getCoverUrl()))
                .placeholder(R.drawable.ic_launcher_background)
                .circleCrop()
                .into(imgCover);

        view.findViewById(R.id.btnClose).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnLater).setOnClickListener(v -> dismiss());

        // Logic Phân phối Quyền mua (1 lựa chọn hay 2 lựa chọn)
        boolean isSongPremium = (song.getIsPremium() != null && song.getIsPremium() == 1);
        boolean isAlbumPremium = (song.getIsAlbumPremium() != null && song.getIsAlbumPremium() == 1);
        boolean inAlbum = song.getAlbumId() != null;

        View btnPremium = view.findViewById(R.id.btnOptionPremium);
        View btnPurchaseSong = view.findViewById(R.id.btnOptionPurchaseSong);
        View btnPurchaseAlbum = view.findViewById(R.id.btnOptionPurchaseAlbum);
        View btnMembership = view.findViewById(R.id.btnOptionMembership);

        // Mặc định ẩn tất cả để set lại theo logic
        btnPremium.setVisibility(View.GONE);
        btnPurchaseSong.setVisibility(View.GONE);
        btnPurchaseAlbum.setVisibility(View.GONE);
        btnMembership.setVisibility(View.GONE);

        // 1. Nếu nhạc MIỄN PHÍ nhưng nằm trong Album PREMIUM -> Chỉ cho mua Album
        if (!isSongPremium && isAlbumPremium && inAlbum) {
            btnPurchaseAlbum.setVisibility(View.VISIBLE);
        }
        // 2. Nếu nhạc PREMIUM nhưng KHÔNG nằm trong Album PREMIUM -> Chỉ cho mua Lẻ
        else if (isSongPremium && (!inAlbum || !isAlbumPremium)) {
            btnPurchaseSong.setVisibility(View.VISIBLE);
        }
        // 3. Nếu nhạc PREMIUM VÀ nằm trong Album PREMIUM -> Cho cả 2 lựa chọn
        else if (isSongPremium && isAlbumPremium && inAlbum) {
            btnPurchaseSong.setVisibility(View.VISIBLE);
            btnPurchaseAlbum.setVisibility(View.VISIBLE);
        }
        
        // 4. Luôn cho phép đăng ký Premium (trừ khi đã là premium, check sẽ xử lý trong confirmation)
        // Nhưng nếu trong DownloadMode, ta chỉ hiện Premium cho bài hát Free (vì Premium chỉ cho tải nhạc Free)
        if (isDownloadMode) {
            if (!isSongPremium && !isAlbumPremium) {
                btnPremium.setVisibility(View.VISIBLE);
            } else {
                // Nhạc Premium hoặc Album Premium thì Premium status không cho phép tải -> Ẩn
                btnPremium.setVisibility(View.GONE);
            }
        } else {
            // Chế độ nghe: Luôn hiện nếu bài hát yêu cầu premium
            btnPremium.setVisibility(View.VISIBLE);
        }

        btnPremium.setOnClickListener(v -> showConfirmation("premium", "Gói Premium 30 ngày", 99000));
        btnPurchaseSong.setOnClickListener(v -> showConfirmation("song", song.getTitle(), sPrice));
        btnPurchaseAlbum.setOnClickListener(v -> showConfirmation("album", "Album " + (song.getAlbumTitle() != null ? song.getAlbumTitle() : "hiện tại"), aPrice));
    }

    private void fetchData() {
        // 1. Get Balance
        RetrofitClient.getApiService().getBalance().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentBalance = 0;
                    // Balance được wrap trong "data" object: { "success": true, "data": { "balance": ... } }
                    Object dataObj = response.body().get("data");
                    if (dataObj instanceof Map) {
                        Map<String, Object> data = (Map<String, Object>) dataObj;
                        if (data.containsKey("balance")) {
                            Object bal = data.get("balance");
                            if (bal instanceof Number) currentBalance = ((Number) bal).doubleValue();
                        }
                    } else if (response.body().containsKey("balance")) {
                        // Fallback: nếu balance nằm trực tiếp ở root
                        Object bal = response.body().get("balance");
                        if (bal instanceof Number) currentBalance = ((Number) bal).doubleValue();
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });

        // 2. Get Membership Info
        if (song.getArtistId() != null) {
            RetrofitClient.getApiService().getArtistById(song.getArtistId()).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (isAdded() && getView() != null && response.isSuccessful() && response.body() != null) {
                        Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                        if (data != null && data.containsKey("membership_price")) {
                            Object pObj = data.get("membership_price");
                            Double price = (pObj instanceof Number) ? ((Number) pObj).doubleValue() : 0.0;
                            
                            Object dObj = data.get("membership_duration_days");
                            Integer days = (dObj instanceof Number) ? ((Number) dObj).intValue() : 30;

                            if (price > 0) {
                                View btnMembership = getView().findViewById(R.id.btnOptionMembership);
                                TextView txtMTitle = getView().findViewById(R.id.txtMembershipTitle);
                                TextView txtMPrice = getView().findViewById(R.id.txtMembershipPrice);

                                if (btnMembership != null) {
                                    // Rule: Hội viên cũng không được tải nhạc. Ẩn nếu ở DownloadMode
                                    if (isDownloadMode) {
                                        btnMembership.setVisibility(View.GONE);
                                    } else {
                                        btnMembership.setVisibility(View.VISIBLE);
                                        if (txtMTitle != null) txtMTitle.setText("Hội viên " + song.getArtistName());
                                        if (txtMPrice != null) txtMPrice.setText(formatCurrency(price) + "/" + days + " ngày");
                                        btnMembership.setOnClickListener(v -> showConfirmation("membership", "Hội viên " + song.getArtistName(), price));
                                    }
                                }
                            }
                        }
                    }
                }
                @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
            });
        }
    }

    private void showConfirmation(String type, String title, double price) {
        PurchaseConfirmationModal modal = PurchaseConfirmationModal.newInstance(
                type, title, price, currentBalance, song, () -> {
                    dismiss();
                    if (listener != null) listener.onPurchaseComplete();
                }
        );
        modal.show(getParentFragmentManager(), "confirm_purchase");
    }

    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }
}
