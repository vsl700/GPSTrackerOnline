package com.vasciie.gpstrackeronline.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.vasciie.gpstrackeronline.R;
import com.vasciie.gpstrackeronline.activities.MainActivity;

public class NoInternetDialog extends AppCompatDialogFragment {
    public static NoInternetDialog currentDialog;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        currentDialog = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_dialog_nointernet, null);
        setCancelable(false);

        builder.setView(view)
                .setTitle("No Internet!");

        builder.setOnKeyListener((arg0, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                dismiss();
                MainActivity.currentMainActivity.finish();
            }
            return true;
        });

        return builder.create();
    }

}
