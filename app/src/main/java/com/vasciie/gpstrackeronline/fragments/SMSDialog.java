package com.vasciie.gpstrackeronline.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.appcompat.widget.SwitchCompat;

import com.vasciie.gpstrackeronline.R;
import com.vasciie.gpstrackeronline.activities.MainActivityCaller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SMSDialog extends AppCompatDialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_dialog_sms, null);

        SwitchCompat online = view.findViewById(R.id.return_online_sw);
        SwitchCompat sms = view.findViewById(R.id.return_sms_sw);

        online.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked)
                sms.setText(R.string.return_data_through_sms_no_internet);
            else {
                sms.setText(R.string.return_data_through_sms);
                sms.setChecked(true);
            }
        });
        sms.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if(!isChecked)
                online.setChecked(true);
        }));

        builder.setView(view)
                .setTitle("Send SMS")
                .setNegativeButton("Cancel", (dialog, which) -> {})
                .setPositiveButton("Send!", (dialog, which) -> {
                    ExecutorService threadPool = Executors.newCachedThreadPool();
                    threadPool.submit(() -> MainActivityCaller.takeActionOnSmsSendRequest(online.isChecked(), sms.isChecked()));
                });


        return builder.create();
    }
}
