package com.appad.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.appad.R;

public class ProfileMenuItemView extends LinearLayout {

    private ImageView imgIcon;
    private TextView txtTitle, txtSubtitle;

    public ProfileMenuItemView(Context context) {
        super(context);
        init(null);
    }

    public ProfileMenuItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ProfileMenuItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        LayoutInflater.from(getContext()).inflate(R.layout.layout_profile_menu_item, this, true);
        
        imgIcon = findViewById(R.id.imgItemIcon);
        txtTitle = findViewById(R.id.txtItemTitle);
        txtSubtitle = findViewById(R.id.txtItemSubtitle);

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ProfileMenuItemView);
            String title = a.getString(R.styleable.ProfileMenuItemView_itemTitle);
            String subtitle = a.getString(R.styleable.ProfileMenuItemView_itemSubtitle);
            int iconRes = a.getResourceId(R.styleable.ProfileMenuItemView_itemIcon, 0);
            int color = a.getColor(R.styleable.ProfileMenuItemView_itemColor, 0xFFFFFFFF);

            txtTitle.setText(title);
            txtSubtitle.setText(subtitle);
            if (iconRes != 0) imgIcon.setImageResource(iconRes);
            imgIcon.setColorFilter(color);
            
            a.recycle();
        }

        setClickable(true);
        setFocusable(true);
        setBackgroundResource(android.R.drawable.list_selector_background);
    }
}
