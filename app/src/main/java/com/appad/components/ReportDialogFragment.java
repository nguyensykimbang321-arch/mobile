package com.appad.components;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.appad.R;
import com.appad.models.Song;
import com.appad.utils.ImageUrlUtils;
import com.appad.utils.RetrofitClient;
import com.appad.utils.SessionManager;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportDialogFragment extends BottomSheetDialogFragment {

    private Integer targetId;
    private String targetType;
    private String songTitle;
    private String artistName;
    private String coverUrl;

    // Report types
    private static final String TYPE_ERROR = "error";
    private static final String TYPE_COPYRIGHT = "copyright";
    private static final String TYPE_INAPPROPRIATE = "inappropriate";
    private static final String TYPE_OTHER = "other";

    private String selectedType = TYPE_ERROR;

    // Views
    private ImageButton btnClose;
    private ImageView imgSongCover;
    private TextView txtSongTitle, txtSongArtist, txtCharCount;
    private EditText edtTitle, edtDescription;
    private Button btnCancel, btnSubmit;
    private FrameLayout loadingOverlay;
    private LinearLayout btnTypeError, btnTypeCopyright, btnTypeInappropriate, btnTypeOther;
    private ImageView iconTypeError, iconTypeCopyright, iconTypeInappropriate, iconTypeOther;
    private TextView txtTypeError, txtTypeCopyright, txtTypeInappropriate, txtTypeOther;

    public static ReportDialogFragment newInstance(Integer targetId, String targetType) {
        ReportDialogFragment fragment = new ReportDialogFragment();
        Bundle args = new Bundle();
        args.putInt("targetId", targetId);
        args.putString("targetType", targetType);
        fragment.setArguments(args);
        return fragment;
    }

    public static ReportDialogFragment newInstance(Song song) {
        ReportDialogFragment fragment = new ReportDialogFragment();
        Bundle args = new Bundle();
        if (song != null) {
            args.putInt("targetId", song.getSongId() != null ? song.getSongId() : 0);
            args.putString("targetType", "song");
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
            targetId = getArguments().getInt("targetId");
            targetType = getArguments().getString("targetType", "song");
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
        View view = inflater.inflate(R.layout.dialog_report, container, false);

        initViews(view);
        setupListeners();
        updateTypeButtons();

        return view;
    }

    private void initViews(View view) {
        btnClose = view.findViewById(R.id.btnCloseReport);
        imgSongCover = view.findViewById(R.id.imgSongCover);
        txtSongTitle = view.findViewById(R.id.txtSongTitle);
        txtSongArtist = view.findViewById(R.id.txtSongArtist);
        txtCharCount = view.findViewById(R.id.txtCharCount);
        edtTitle = view.findViewById(R.id.edtReportTitle);
        edtDescription = view.findViewById(R.id.edtReportDescription);
        btnCancel = view.findViewById(R.id.btnCancelReport);
        btnSubmit = view.findViewById(R.id.btnSubmitReport);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);

        btnTypeError = view.findViewById(R.id.btnTypeError);
        btnTypeCopyright = view.findViewById(R.id.btnTypeCopyright);
        btnTypeInappropriate = view.findViewById(R.id.btnTypeInappropriate);
        btnTypeOther = view.findViewById(R.id.btnTypeOther);

        iconTypeError = view.findViewById(R.id.iconTypeError);
        iconTypeCopyright = view.findViewById(R.id.iconTypeCopyright);
        iconTypeInappropriate = view.findViewById(R.id.iconTypeInappropriate);
        iconTypeOther = view.findViewById(R.id.iconTypeOther);

        txtTypeError = view.findViewById(R.id.txtTypeError);
        txtTypeCopyright = view.findViewById(R.id.txtTypeCopyright);
        txtTypeInappropriate = view.findViewById(R.id.txtTypeInappropriate);
        txtTypeOther = view.findViewById(R.id.txtTypeOther);

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
        btnClose.setOnClickListener(v -> dismiss());
        btnCancel.setOnClickListener(v -> dismiss());

        btnSubmit.setOnClickListener(v -> submitReport());

        // Type buttons
        btnTypeError.setOnClickListener(v -> {
            selectedType = TYPE_ERROR;
            updateTypeButtons();
        });
        btnTypeCopyright.setOnClickListener(v -> {
            selectedType = TYPE_COPYRIGHT;
            updateTypeButtons();
        });
        btnTypeInappropriate.setOnClickListener(v -> {
            selectedType = TYPE_INAPPROPRIATE;
            updateTypeButtons();
        });
        btnTypeOther.setOnClickListener(v -> {
            selectedType = TYPE_OTHER;
            updateTypeButtons();
        });

        // Character count
        edtDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                txtCharCount.setText(s.length() + "/500");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateTypeButtons() {
        int activeColor = 0xFF8b5cf6;
        int inactiveColor = 0xFF94A3B8;

        // Reset all to inactive
        btnTypeError.setBackgroundResource(R.drawable.bg_report_type_inactive);
        btnTypeCopyright.setBackgroundResource(R.drawable.bg_report_type_inactive);
        btnTypeInappropriate.setBackgroundResource(R.drawable.bg_report_type_inactive);
        btnTypeOther.setBackgroundResource(R.drawable.bg_report_type_inactive);

        iconTypeError.setColorFilter(inactiveColor);
        iconTypeCopyright.setColorFilter(inactiveColor);
        iconTypeInappropriate.setColorFilter(inactiveColor);
        iconTypeOther.setColorFilter(inactiveColor);

        txtTypeError.setTextColor(inactiveColor);
        txtTypeCopyright.setTextColor(inactiveColor);
        txtTypeInappropriate.setTextColor(inactiveColor);
        txtTypeOther.setTextColor(inactiveColor);

        // Set active
        LinearLayout activeBtn;
        ImageView activeIcon;
        TextView activeTxt;

        switch (selectedType) {
            case TYPE_COPYRIGHT:
                activeBtn = btnTypeCopyright;
                activeIcon = iconTypeCopyright;
                activeTxt = txtTypeCopyright;
                break;
            case TYPE_INAPPROPRIATE:
                activeBtn = btnTypeInappropriate;
                activeIcon = iconTypeInappropriate;
                activeTxt = txtTypeInappropriate;
                break;
            case TYPE_OTHER:
                activeBtn = btnTypeOther;
                activeIcon = iconTypeOther;
                activeTxt = txtTypeOther;
                break;
            default:
                activeBtn = btnTypeError;
                activeIcon = iconTypeError;
                activeTxt = txtTypeError;
                break;
        }

        activeBtn.setBackgroundResource(R.drawable.bg_report_type_active);
        activeIcon.setColorFilter(activeColor);
        activeTxt.setTextColor(activeColor);
    }

    private void submitReport() {
        String title = edtTitle.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();

        // Validation
        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.length() < 5) {
            Toast.makeText(getContext(), "Tiêu đề phải có ít nhất 5 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.length() < 10) {
            Toast.makeText(getContext(), "Mô tả phải có ít nhất 10 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        loadingOverlay.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        Integer userId = SessionManager.getInstance(getContext()).getUserId();
        if (userId == null) userId = 1;

        // Build description with user info
        String userFullName = SessionManager.getInstance(getContext()).getUserFullName();
        String fullDescription = description + "\n\n--- Người báo cáo ---\nTên: " + 
                (userFullName != null ? userFullName : "Unknown") + "\nID: " + userId;

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("targetId", targetId);
        payload.put("targetType", targetType);
        payload.put("reason", getReasonLabel(selectedType));
        payload.put("description", fullDescription);
        payload.put("title", title);

        RetrofitClient.getApiService().submitReport(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                loadingOverlay.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Cảm ơn bạn đã báo cáo!", Toast.LENGTH_SHORT).show();
                    dismiss();
                } else {
                    Toast.makeText(getContext(), "Không thể gửi báo cáo", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                loadingOverlay.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
                Toast.makeText(getContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getReasonLabel(String type) {
        switch (type) {
            case TYPE_COPYRIGHT:
                return "Vi phạm bản quyền";
            case TYPE_INAPPROPRIATE:
                return "Nội dung không phù hợp";
            case TYPE_OTHER:
                return "Khác";
            default:
                return "Lỗi kỹ thuật";
        }
    }
}
