package com.appad.components;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.appad.R;
import com.appad.activities.DepositActivity;
import com.appad.models.Song;
import com.appad.utils.RetrofitClient;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PurchaseConfirmationModal extends DialogFragment {

    private String type; // song, premium, membership
    private String title;
    private double price;
    private double currentBalance;
    private Song song;
    private OnSuccessEventListener successListener;

    public interface OnSuccessEventListener {
        void onSuccess();
    }

    public static PurchaseConfirmationModal newInstance(String type, String title, double price, double currentBalance, Song song, OnSuccessEventListener listener) {
        PurchaseConfirmationModal fragment = new PurchaseConfirmationModal();
        fragment.type = type;
        fragment.title = title;
        fragment.price = price;
        fragment.currentBalance = currentBalance;
        fragment.song = song;
        fragment.successListener = listener;
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            // Set width to 90% of screen width for better appearance
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_purchase_confirmation_modal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
    }

    private void initViews(View view) {
        TextView txtItemTitle = view.findViewById(R.id.txtItemTitle);
        TextView txtPrice = view.findViewById(R.id.txtPrice);
        TextView txtCurrentBalance = view.findViewById(R.id.txtCurrentBalance);
        TextView txtRemainingBalance = view.findViewById(R.id.txtRemainingBalance);
        TextView txtBadge = view.findViewById(R.id.txtPurchaseBadge);
        TextView txtDescription = view.findViewById(R.id.txtDescription);
        TextView txtPremiumNotice = view.findViewById(R.id.txtPremiumNotice);
        LinearLayout layoutWarning = view.findViewById(R.id.layoutWarning);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);
        ImageView imgIcon = view.findViewById(R.id.imgTypeIcon);

        txtItemTitle.setText(title);
        txtPrice.setText(formatCurrency(price));
        txtCurrentBalance.setText(formatCurrency(currentBalance));

        double remaining = currentBalance - price;
        txtRemainingBalance.setText(formatCurrency(remaining));

        if (remaining < 0) {
            txtRemainingBalance.setTextColor(Color.parseColor("#EF5350"));
            layoutWarning.setVisibility(View.VISIBLE);
            btnConfirm.setText("Nạp tiền ngay");
            btnConfirm.setTextColor(Color.parseColor("#EF5350"));
        } else {
            txtRemainingBalance.setTextColor(Color.parseColor("#4CAF50"));
            layoutWarning.setVisibility(View.GONE);
            btnConfirm.setText("Xác nhận mua");
        }

        // Style based on type
        switch (type) {
            case "premium":
                txtBadge.setText("Gói Premium");
                txtBadge.setTextColor(Color.parseColor("#8b5cf6"));
                txtDescription.setText("Nghe không giới hạn toàn bộ kho nhạc.");
                imgIcon.setImageResource(android.R.drawable.btn_star_big_on);
                break;
            case "song":
                txtBadge.setText("Mua lẻ Bài hát");
                txtBadge.setTextColor(Color.parseColor("#06b6d4"));
                txtDescription.setText("Bạn sẽ sở hữu bài hát này vĩnh viễn.");
                imgIcon.setImageResource(android.R.drawable.ic_input_add);
                break;
            case "album":
                txtBadge.setText("Sở hữu Album");
                txtBadge.setTextColor(Color.parseColor("#EAB308"));
                txtDescription.setText("Sở hữu trọn bộ bài hát trong album này.");
                imgIcon.setImageResource(android.R.drawable.ic_menu_agenda);
                break;
            case "membership":
                txtBadge.setText("Hội viên Artist");
                txtBadge.setTextColor(Color.parseColor("#EC4899"));
                txtDescription.setText("Nghe mọi bài hát Premium của nghệ sĩ này.");
                imgIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
                break;
        }

        com.appad.utils.SessionManager sm = com.appad.utils.SessionManager.getInstance(getContext());
        com.appad.models.User user = (sm != null) ? sm.getUser() : null;
        if (user != null && user.getIsPremium() != null && user.getIsPremium() == 1 && txtPremiumNotice != null) {
            String expiry = formatExpiryDate(user.getPremiumExpiry());
            switch (type) {
                case "song":
                    txtPremiumNotice.setText("Bạn đã có quyền truy cập nhạc này đến ngày " + expiry);
                    txtPremiumNotice.setVisibility(View.VISIBLE);
                    break;
                case "album":
                    txtPremiumNotice.setText("Bạn đã có quyền truy cập album này đến ngày " + expiry);
                    txtPremiumNotice.setVisibility(View.VISIBLE);
                    break;
                case "membership":
                    txtPremiumNotice.setText("Bạn đã có quyền truy cập tất cả nhạc của nghệ sĩ này đến ngày " + expiry);
                    txtPremiumNotice.setVisibility(View.VISIBLE);
                    break;
            }
        }

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
        btnConfirm.setOnClickListener(v -> {
            if (remaining < 0) {
                startActivity(new Intent(getContext(), DepositActivity.class));
                dismiss();
            } else {
                performPurchase();
            }
        });
    }

    private void performPurchase() {
        Call<Map<String, Object>> call = null;
        if (type.equals("song")) {
            Map<String, Integer> payload = new HashMap<>();
            payload.put("songId", song.getSongId());
            call = RetrofitClient.getApiService().purchaseSong(payload);
        } else if (type.equals("premium")) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("duration", 30);
            call = RetrofitClient.getApiService().subscribePremium(payload);
        } else if (type.equals("album")) {
            Map<String, Integer> payload = new HashMap<>();
            payload.put("albumId", song.getAlbumId());
            call = RetrofitClient.getApiService().purchaseAlbum(payload);
        } else if (type.equals("membership")) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", com.appad.utils.SessionManager.getInstance(getContext()).getUserId());
            payload.put("artistId", song.getArtistId());
            call = RetrofitClient.getApiService().subscribeArtist(payload);
        }

        if (call == null) return;

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Thanh toán thành công!", Toast.LENGTH_SHORT).show();
                    if (successListener != null) successListener.onSuccess();
                    dismiss();
                } else {
                    Toast.makeText(getContext(), "Thanh toán thất bại", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatExpiryDate(String expiryStr) {
        if (expiryStr == null || expiryStr.isEmpty()) return "N/A";
        try {
            if (expiryStr.contains("T")) {
                String datePart = expiryStr.split("T")[0]; // "yyyy-MM-dd"
                String[] parts = datePart.split("-");
                if (parts.length == 3) {
                    return parts[2] + "/" + parts[1] + "/" + parts[0]; // "dd/MM/yyyy"
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return expiryStr;
    }

    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }
}
