package com.vasciie.gpstrackeronline.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.vasciie.gpstrackeronline.activities.MainActivity;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(MainActivity.currentMainActivity.isDestroyed()){
            Intent main = new Intent(context, MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(main);
        }
    }
}