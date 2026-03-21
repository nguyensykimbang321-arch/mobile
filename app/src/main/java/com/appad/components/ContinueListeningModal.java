package com.appad.components;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.appad.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ContinueListeningModal extends BottomSheetDialogFragment {

    private OnContinueListener listener;

    public interface OnContinueListener {
        void onContinue();
    }

    public static ContinueListeningModal newInstance(OnContinueListener listener) {
        ContinueListeningModal fragment = new ContinueListeningModal();
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
        return inflater.inflate(R.layout.layout_continue_listening_modal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnContinue).setOnClickListener(v -> {
            if (listener != null) {
                listener.onContinue();
            }
            dismiss();
        });
    }
}
