package com.appad.components;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.appad.R;

public class CustomAlertDialogFragment extends DialogFragment {

    public enum AlertType {
        SUCCESS, ERROR, WARNING, INFO
    }

    private AlertType type = AlertType.INFO;
    private String title;
    private String message;
    private String primaryButtonText = "Đồng ý";
    private String secondaryButtonText;
    private Runnable onPrimaryAction;
    private Runnable onSecondaryAction;

    public static CustomAlertDialogFragment newInstance(AlertType type, String title, String message) {
        CustomAlertDialogFragment fragment = new CustomAlertDialogFragment();
        fragment.type = type;
        fragment.title = title;
        fragment.message = message;
        return fragment;
    }

    public CustomAlertDialogFragment setPrimaryButton(String text, Runnable action) {
        this.primaryButtonText = text;
        this.onPrimaryAction = action;
        return this;
    }

    public CustomAlertDialogFragment setSecondaryButton(String text, Runnable action) {
        this.secondaryButtonText = text;
        this.onSecondaryAction = action;
        return this;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return inflater.inflate(R.layout.dialog_custom_alert, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FrameLayout iconContainer = view.findViewById(R.id.alertIconContainer);
        ImageView imgIcon = view.findViewById(R.id.imgAlertIcon);
        TextView txtTitle = view.findViewById(R.id.txtAlertTitle);
        TextView txtMessage = view.findViewById(R.id.txtAlertMessage);
        Button btnPrimary = view.findViewById(R.id.btnAlertPrimary);
        Button btnSecondary = view.findViewById(R.id.btnAlertSecondary);

        txtTitle.setText(title);
        txtMessage.setText(message);
        btnPrimary.setText(primaryButtonText);

        if (secondaryButtonText != null) {
            btnSecondary.setVisibility(View.VISIBLE);
            btnSecondary.setText(secondaryButtonText);
        }

        setupType(iconContainer, imgIcon, btnPrimary);

        btnPrimary.setOnClickListener(v -> {
            if (onPrimaryAction != null) onPrimaryAction.run();
            dismiss();
        });

        btnSecondary.setOnClickListener(v -> {
            if (onSecondaryAction != null) onSecondaryAction.run();
            dismiss();
        });
    }

    private void setupType(FrameLayout container, ImageView icon, Button primaryBtn) {
        int color;
        int iconRes;

        switch (type) {
            case SUCCESS:
                color = Color.parseColor("#4CAF50");
                iconRes = android.R.drawable.checkbox_on_background;
                break;
            case ERROR:
                color = Color.parseColor("#EF5350");
                iconRes = android.R.drawable.ic_delete;
                break;
            case WARNING:
                color = Color.parseColor("#FFA726");
                iconRes = android.R.drawable.stat_sys_warning;
                break;
            case INFO:
            default:
                color = Color.parseColor("#2196F3");
                iconRes = android.R.drawable.ic_dialog_info;
                break;
        }

        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color & 0x1AFFFFFF | 0x1A000000); // 10% Alpha
        gd.setStroke(dpToPx(2), color);
        container.setBackground(gd);

        icon.setImageResource(iconRes);
        icon.setColorFilter(color);
        primaryBtn.setBackgroundColor(color);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
