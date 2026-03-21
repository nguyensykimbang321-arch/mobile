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

public class SuccessDialogFragment extends DialogFragment {

    private String title;
    private String message;
    private Runnable onDone;

    public static SuccessDialogFragment newInstance(String title, String message, Runnable onDone) {
        SuccessDialogFragment fragment = new SuccessDialogFragment();
        fragment.title = title;
        fragment.message = message;
        fragment.onDone = onDone;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return inflater.inflate(R.layout.dialog_success, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView txtTitle = view.findViewById(R.id.txtSuccessTitle);
        TextView txtMessage = view.findViewById(R.id.txtSuccessMessage);
        Button btnDone = view.findViewById(R.id.btnSuccessDone);

        if (title != null) txtTitle.setText(title);
        if (message != null) txtMessage.setText(message);

        btnDone.setOnClickListener(v -> {
            if (onDone != null) onDone.run();
            dismiss();
        });
    }
}
