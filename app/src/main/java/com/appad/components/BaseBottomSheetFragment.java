package com.appad.components;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.appad.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class BaseBottomSheetFragment extends BottomSheetDialogFragment {

    private String title;
    
    public static BaseBottomSheetFragment newInstance(String title) {
        BaseBottomSheetFragment fragment = new BaseBottomSheetFragment();
        fragment.title = title;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        TextView txtTitle = view.findViewById(R.id.txtBottomSheetTitle);
        txtTitle.setText(title);
        
        // can add more logic for RecyclerView options here
    }
    
    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialogTheme;
    }
}
