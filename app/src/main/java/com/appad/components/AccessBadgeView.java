package com.appad.components;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import com.appad.R;

public class AccessBadgeView extends AppCompatImageView {

    public AccessBadgeView(Context context) {
        super(context);
        init();
    }

    public AccessBadgeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setVisibility(View.GONE); // Hidden by default
    }

    public void setAccessType(String accessType) {
        if (accessType == null || accessType.isEmpty()) {
            setVisibility(View.GONE);
            return;
        }

        setVisibility(View.VISIBLE);
        switch (accessType.toLowerCase()) {
            case "premium":
                setImageResource(R.drawable.ic_star_badge);
                setColorFilter(Color.parseColor("#FFD700"));
                break;
            case "purchased":
                setImageResource(R.drawable.ic_check_circle_badge);
                setColorFilter(Color.parseColor("#4CAF50")); // COLORS.success
                break;
            case "album_purchased":
                setImageResource(R.drawable.ic_disc_badge);
                setColorFilter(Color.parseColor("#8B5CF6")); // COLORS.primary
                break;
            case "artist_membership":
                setImageResource(R.drawable.ic_people_badge);
                setColorFilter(Color.parseColor("#9C27B0"));
                break;
            case "artist_owner":
                setImageResource(R.drawable.ic_mic_badge);
                setColorFilter(Color.parseColor("#8B5CF6")); // COLORS.primary
                break;
            default:
                setVisibility(View.GONE);
                break;
        }
    }

    public void setBadgeSize(int sizeDp) {
        int px = (int) (sizeDp * getContext().getResources().getDisplayMetrics().density);
        getLayoutParams().width = px;
        getLayoutParams().height = px;
        requestLayout();
    }
}
