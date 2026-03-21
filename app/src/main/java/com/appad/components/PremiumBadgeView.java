package com.appad.components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.appad.R;

public class PremiumBadgeView extends LinearLayout {

    public PremiumBadgeView(Context context) {
        super(context);
        init(context);
    }

    public PremiumBadgeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private TextView txtBadge;

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        
        int paddingH = dpToPx(6);
        int paddingV = dpToPx(1);
        setPadding(paddingH, paddingV, paddingH, paddingV);

        // Background
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#1AFFD700")); // 10% Alpha Gold
        gd.setCornerRadius(dpToPx(10));
        gd.setStroke(dpToPx(1), Color.parseColor("#80FFD700")); // 50% Alpha Gold
        setBackground(gd);

        // Text
        txtBadge = new TextView(context);
        txtBadge.setText("PREMIUM");
        txtBadge.setTextColor(Color.parseColor("#FFD700"));
        txtBadge.setTextSize(7);
        txtBadge.setLetterSpacing(0.05f);
        txtBadge.setTypeface(null, android.graphics.Typeface.BOLD);
        LayoutParams textParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        addView(txtBadge, textParams);
    }

    public void setText(String text) {
        if (txtBadge != null) {
            txtBadge.setText(text);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getContext().getResources().getDisplayMetrics().density);
    }
}
