package com.vasciie.gpstrackeronline.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.vasciie.gpstrackeronline.MainActivity;
import com.vasciie.gpstrackeronline.R;

public class LocationService extends Service {
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locCallback;

    public LocationRequest locRequest;

    // The access to the activity functions and public application fields
    public static MainActivity main;

    public static boolean isCallerTracking;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private static final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            System.out.println("Service Loop");
        }
    }

    @Override
    public void onCreate() {
        main = MainActivity.currentMainActivity;

        // Due to working with the Internet we are using a separate thread for the service
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationRequest();

        locCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locResult) {
                if(!main.isDestroyed())
                    main.locationUpdated(locResult);

            }
        };
    }

    @SuppressLint("RemoteViewLayout")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!updatesOn) {
            Message msg = serviceHandler.obtainMessage();
            msg.arg1 = startId;
            serviceHandler.sendMessage(msg);

            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(this, 0, notificationIntent, 0);


            String channelId;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = getString(R.string.channel_name);
                String description = getString(R.string.channel_description);
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel("channelId", name, importance);
                channel.setDescription(description);

                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);

                channelId = channel.getId();
            }
            else{
                channelId = "notification";
            }

            RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_collapsed);

            Intent quitClickIntent = new Intent(this, MainActivity.QuitButtonNotificationReceiver.class);
            remoteViews.setOnClickPendingIntent(R.id.imageButton, PendingIntent.getBroadcast(this, 0, quitClickIntent, 0));

            Intent clickIntent = new Intent(this, MainActivity.NotificationReceiver.class);
            remoteViews.setOnClickPendingIntent(R.id.notification_layout, PendingIntent.getBroadcast(this, 0, clickIntent, 0));

            Notification notification =
                    new NotificationCompat.Builder(main, channelId)
                            .setContentTitle(getText(R.string.notification_title))
                            .setContentText(getText(R.string.notification_message))
                            .setSmallIcon(R.drawable.loc_icon_blue)
                            .setContentIntent(pendingIntent)
                            .setTicker(getText(R.string.ticker_text))
                            .setCustomContentView(remoteViews)
                            //.setCustomBigContentView(new RemoteViews(getPackageName(), R.layout.text_row_item))
                            .build();

            startForeground(12893, notification);
        }

        startLocationUpdates();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();
    }

    public void createLocationRequest() {
        locRequest = LocationRequest.create();
        locRequest.setInterval(10000);
        locRequest.setFastestInterval(5000); //TODO: CHANGE TO 5000
        locRequest.setSmallestDisplacement(2);
        locRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locRequest);

        SettingsClient client = LocationServices.getSettingsClient(main);
        Task<LocationSettingsResponse> task =
                client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(main, locationSettingsResponse -> {
            // All location settings are satisfied. The client can initialize
            // location requests here.

            System.out.println("Success");


            //requestingLocationUpdates = true;
        });
    }

    public static boolean updatesOn = false;
    public void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(main, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(main, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 14894);
            return;
        }

        fusedLocationClient.requestLocationUpdates(locRequest, locCallback, Looper.getMainLooper());
        updatesOn = true;
    }

    public void stopLocationUpdates(){
        fusedLocationClient.removeLocationUpdates(locCallback);
        updatesOn = false;
    }

}
