package com.appad.components;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.appad.R;
import java.text.NumberFormat;
import java.util.Locale;

public class PurchaseDialogFragment extends DialogFragment {

    public interface OnPurchaseConfirmListener {
        void onConfirm();
    }

    private String type; // 'song', 'membership', 'premium'
    private String title;
    private double price;
    private double currentBalance;
    private OnPurchaseConfirmListener listener;

    public static PurchaseDialogFragment newInstance(String type, String title, double price, double currentBalance, OnPurchaseConfirmListener listener) {
        PurchaseDialogFragment fragment = new PurchaseDialogFragment();
        fragment.type = type;
        fragment.title = title;
        fragment.price = price;
        fragment.currentBalance = currentBalance;
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return inflater.inflate(R.layout.dialog_purchase_confirmation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView txtTitle = view.findViewById(R.id.txtItemTitle);
        TextView txtUnitPrice = view.findViewById(R.id.txtUnitPrice);
        TextView txtCurrent = view.findViewById(R.id.txtCurrentBalance);
        TextView txtRemaining = view.findViewById(R.id.txtRemainingBalance);
        TextView txtDescription = view.findViewById(R.id.txtDescription);
        TextView txtWarning = view.findViewById(R.id.txtWarning);
        View layoutWarning = view.findViewById(R.id.layoutWarning);
        Button btnConfirm = view.findViewById(R.id.btnConfirmPurchase);
        Button btnCancel = view.findViewById(R.id.btnCancelPurchase);

        txtTitle.setText(title);
        txtUnitPrice.setText(formatCurrency(price));
        txtCurrent.setText(formatCurrency(currentBalance));

        double remaining = currentBalance - price;
        txtRemaining.setText(formatCurrency(remaining));

        if (remaining < 0) {
            txtRemaining.setTextColor(Color.parseColor("#ef5350"));
            layoutWarning.setVisibility(View.VISIBLE);
            txtWarning.setText("Số dư không đủ. Vui lòng nạp thêm " + formatCurrency(Math.abs(remaining)));
            btnConfirm.setText("Nạp tiền ngay");
            btnConfirm.setBackgroundColor(Color.parseColor("#333333"));
            btnConfirm.setTextColor(Color.parseColor("#ef5350"));
        } else {
            txtRemaining.setTextColor(Color.parseColor("#4CAF50"));
            layoutWarning.setVisibility(View.GONE);
            btnConfirm.setText("Xác nhận mua");
        }

        // Custom UI based on type
        if ("membership".equals(type)) {
            txtDescription.setText("Đăng ký hội viên để nhận các bài hát mới nhất.");
        }

        btnCancel.setOnClickListener(v -> dismiss());
        btnConfirm.setOnClickListener(v -> {
            if (remaining < 0) {
                // Handle Deposit navigation (Placeholder)
                android.widget.Toast.makeText(getContext(), "Tính năng nạp tiền đang phát triển", android.widget.Toast.LENGTH_SHORT).show();
            } else if (listener != null) {
                listener.onConfirm();
                dismiss();
            }
        });
    }

    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }
}
