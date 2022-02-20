package com.vasciie.gpstrackeronline.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;
import com.vasciie.gpstrackeronline.R;
import com.vasciie.gpstrackeronline.activities.LoginWayActivity;
import com.vasciie.gpstrackeronline.activities.MainActivity;
import com.vasciie.gpstrackeronline.activities.MainActivityCaller;
import com.vasciie.gpstrackeronline.receivers.NotificationReceiver;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrackerService extends Service implements MainActivity.OuterNetworkCallback {
    private static class HubConnectionTask extends AsyncTask<HubConnection, Void, Void> {

        public HubConnectionTask(){super();} // To prevent a Deprecation warning

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(HubConnection... hubConnections) {
            HubConnection hubConnection = hubConnections[0];
            try {
                hubConnection.start().blockingAwait();

                if(!alive/* && MainActivity.currentMainActivity.isDestroyed()*/) {
                    if(hubConnection.getConnectionState().equals(HubConnectionState.CONNECTED))
                        hubConnection.stop();

                    hubConnection.close();
                }

                System.out.println(hubConnection.getConnectionId());
                System.out.println(hubConnection.getConnectionState());
            } catch (Exception e) {
                e.printStackTrace();
                isOnline = false;
            }

            return null;
        }
    }
    private class BatteryLevelReceiver extends BroadcastReceiver {
        private static final float minBatteryPct = 10;

        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL ||
                    status == BatteryManager.BATTERY_STATUS_NOT_CHARGING;

            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            float batteryPct = level * 100 / (float)scale;
            if(batteryPct < minBatteryPct && !isCharging)
                batteryLow = true;
            else batteryLow = false;
        }
    }


    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private HandlerThread thread;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locCallback;
    private LocationManager lm;

    public LocationRequest locRequest;
    public LocationListener locationListener;

    private static HubConnection hubConnection;
    private static final String primaryLink = APIConnector.primaryLink;

    // The access to the activity functions and public application fields
    public static MainActivity main;

    public static boolean isCallerTracking;
    private static boolean batteryLow;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private static final class ServiceHandler extends Handler {
        private final TrackerService locService;
        private final Random r;

        public ServiceHandler(TrackerService locService, Looper looper) {
            super(looper);

            this.locService = locService;
            r = new Random();
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            System.out.println("Service Loop");
            long normalMillis = 1000;

            // We don't make use of the LocationRequest's time interval properties due to the fact
            // that some phones literally stop the updates when the app is closed and only the
            // app service is working
            while (true) {
                System.out.println("Cycling...");
                if(batteryLow)
                    System.out.println("Low battery");
                if (!isLocationActivelyUsed()) {
                    if(updatesOn) {
                        locService.stopLocationUpdates();
                        locService.stopLocationUpdatesInternetOnly();
                    }

                    if(LoginWayActivity.loggedInCaller || batteryLow) {
                        try {
                            Thread.sleep(normalMillis);
                            continue;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return;
                        }
                    }

                    try {
                        int min = 300, max = 600; // next track after 5:00 to 10:00 minutes
                        int time = r.nextInt((max - min) + 1) + min;
                        int secs = 0;
                        while (secs < time) {
                            if (isLocationActivelyUsed() || batteryLow)
                                break;

                            if(updatesOn){
                                locService.stopLocationUpdates();
                                locService.stopLocationUpdatesInternetOnly();
                            }

                            System.out.println("Inner cycling...");
                            Thread.sleep(1000);
                            secs++;
                        }

                        if (isLocationActivelyUsed() || batteryLow)
                            continue;

                        locReceived = false;
                        if(!locService.isGPSEnabled() && locService.isNetworkEnabled()) {
                            locService.startLocationUpdatesInternetOnly();
                        }
                        else{
                            locService.startLocationUpdates();
                        }

                        int checks = 0;
                        do {
                            Thread.sleep(500);
                            checks++;
                            System.out.println("Location waiting...");
                        } while (!locReceived && checks < 14);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }
                else{
                    if(!locService.isGPSEnabled() && locService.isNetworkEnabled()){
                        locService.stopLocationUpdates();
                        locService.startLocationUpdatesInternetOnly();
                    }
                    else{
                        locService.stopLocationUpdatesInternetOnly();
                        locService.startLocationUpdates();
                    }
                }

                try {
                    Thread.sleep(normalMillis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }

        private boolean locReceived;
        public void locationReceived() {
            locReceived = true;
        }

        private boolean isLocationActivelyUsed() {
            return main != null && !main.isDestroyed() || isCallerTracking;
        }

    }

    public static Location prevLoc;
    @Override
    public void onCreate() {
        alive = true;

        main = MainActivity.currentMainActivity;
        if(main.isDestroyed() && LoginWayActivity.loggedInTarget){
            main.outerNetworkCallback = this;
            main.requestConnectionCallback(); // In the case where only the service starts, without the activity (when the Target receives an SMS)
        }

        thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(this, serviceLooper);

        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                prevLoc = location;
                makeUseOfNewLoc();
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationRequest();

        locCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locResult) {
                prevLoc = locResult.getLocations().get(locResult.getLocations().size() - 1);
                makeUseOfNewLoc();
            }
        };
    }

    // That method should NOT have any commands for bypassing operations, otherwise some of the app
    // functionalities will stop!
    public static void createHubConnection(){
        if(hubConnection != null){
            HubConnection tempHub = hubConnection;
            ExecutorService threadPool = Executors.newCachedThreadPool();
            threadPool.submit(() -> {
                stopHubConnection();
                tempHub.close();
            });
        }

        String value;
        if(LoginWayActivity.loggedInTarget)
            value = LoginWayActivity.getLoggedTargetCode() + "";
        else value = LoginWayActivity.getLoggedUserName();
        hubConnection = HubConnectionBuilder.create(primaryLink + "/NotificationUserHub?userId=" + value).build();

        if(LoginWayActivity.loggedInCaller)
            hubConnection.on("sendToUser", (targetCode, message) -> main.runOnUiThread(() -> {
                System.out.println("Target: " + targetCode);
                System.out.println("Message: " + message);

                try {
                    JSONArray jsonArray = new JSONArray(message.replace("\"", ""));
                    double lat = jsonArray.getDouble(0);
                    double lng = jsonArray.getDouble(1);

                    MainActivityCaller mainCaller = (MainActivityCaller) main;
                    mainCaller.targetLocationUpdated(mainCaller.getIndexByOldCode(targetCode), new LatLng(lat, lng));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }), Integer.class,String.class);
        else
            hubConnection.on("sendToTarget", (message) -> {
                System.out.println(message);

                if(message.equals("trackin"))
                    isCallerTracking = true;
                else if(message.equals("no-trackin"))
                    isCallerTracking = false;
            }, String.class);
    }

    private boolean isGPSEnabled(){
        try {
            return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return false;
    }

    private boolean isNetworkEnabled(){
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onConnected() {
        while(!alive){
            System.out.println("Waiting for the service...");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(LoginWayActivity.loggedInCaller) {
            if(hubConnection == null)
                createHubConnection();

            startHubConnection();
        }
    }

    @Override
    public void onDisconnected() {
        //stopHubConnection();
    }

    private void makeUseOfNewLoc() {
        if (!main.isDestroyed())
            main.locationUpdated(prevLoc);

        if(LoginWayActivity.loggedInTarget) {
            main.saveNewLocationToDB(prevLoc);
            serviceHandler.locationReceived();

            ExecutorService threadPool = Executors.newCachedThreadPool();
            threadPool.submit(() -> APIConnector.SendCurrentLocation(prevLoc.getLatitude(), prevLoc.getLongitude()));
        }
    }

    private BroadcastReceiver batteryReceiver;
    private static boolean isOnline = false;
    @SuppressLint("RemoteViewLayout")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        main = MainActivity.currentMainActivity;
        main.outerNetworkCallback = this;

        if (batteryReceiver == null) {
            Message msg = serviceHandler.obtainMessage();
            msg.arg1 = startId;
            serviceHandler.sendMessage(msg);

            if(LoginWayActivity.loggedInTarget) {
                IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                registerReceiver(batteryReceiver = new BatteryLevelReceiver(), filter);
            }

            Intent notificationIntent = new Intent(this, LoginWayActivity.loggedInCaller ? MainActivityCaller.class : MainActivity.class);
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
            } else {
                channelId = "notification";
            }

            RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_collapsed);

            Intent clickIntent = new Intent(this, NotificationReceiver.class);
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

        return START_STICKY;
    }

    public static boolean alive = false;
    @Override
    public void onDestroy() {
        stopLocationUpdates();
        stopLocationUpdatesInternetOnly();
        thread.interrupt();
        thread.quit();

        if(hubConnection != null) {
            stopHubConnection();

            if(!hubConnection.getConnectionState().equals(HubConnectionState.CONNECTING))
                hubConnection.close();

            hubConnection = null;
        }

        if(LoginWayActivity.loggedInTarget)
            unregisterReceiver(batteryReceiver);

        isCallerTracking = false;

        alive = false;
    }

    public void createLocationRequest() {
        locRequest = LocationRequest.create();
        locRequest.setInterval(10000);
        locRequest.setFastestInterval(3000);
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
    public static final int gpsAccessRequestCode = 14894;
    public static final int gpsBGAccessRequestCode = 14332;

    private static boolean gpsUpdatesOn = false;
    private void startLocationUpdates() {
        if(gpsUpdatesOn)
            return;

        if (ActivityCompat.checkSelfPermission(main, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(main, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, gpsAccessRequestCode);
            return;
        }

        fusedLocationClient.requestLocationUpdates(locRequest, locCallback, Looper.getMainLooper());
        updatesOn = gpsUpdatesOn = true;
    }

    private static boolean internetUpdatesOn = false;
    private void startLocationUpdatesInternetOnly(){
        if(internetUpdatesOn)
            return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        updatesOn = internetUpdatesOn = true;
    }

    public static void startHubConnection() {
        try {
            if (!isOnline || !hubConnection.getConnectionState().equals(HubConnectionState.CONNECTED)) {
                new HubConnectionTask().execute(hubConnection);
                hubConnection.onClosed(Throwable::printStackTrace);
                isOnline = true;
            }
        }catch (Exception e) {e.printStackTrace();}
    }

    private void stopLocationUpdates() {
        if(!gpsUpdatesOn)
            return;

        fusedLocationClient.removeLocationUpdates(locCallback);
        updatesOn = gpsUpdatesOn = false;
    }

    private void stopLocationUpdatesInternetOnly(){
        if(!internetUpdatesOn)
            return;

        lm.removeUpdates(locationListener);
        updatesOn = internetUpdatesOn = false;
    }

    public static void stopHubConnection() {
        //if(hubConnection.getConnectionState().equals(HubConnectionState.CONNECTED))
            hubConnection.stop();
        isOnline = false;
    }

}
