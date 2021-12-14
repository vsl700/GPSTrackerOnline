package com.vasciie.gpstrackeronline;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.vasciie.gpstrackeronline.services.LocationService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap gMap;

    private Marker currentMarker, lookupMarker;

    public static Intent locService;

    // Using LinkedList to improve performance while tracking
    public LinkedList<String> capTimes;
    public LinkedList<Double> latitudes, longitudes;
    public LinkedList<Integer> images;

    // For indexing the app's database image numbers with the real app picture IDs (held in class R)
    public HashMap<Integer, Integer> imageIds;

    private static final Random r = new Random();

    public static MainActivity currentMainActivity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentMainActivity = this;

        createImageIds();

        capTimes = new LinkedList<>();
        latitudes = new LinkedList<>();
        longitudes = new LinkedList<>();
        images = new LinkedList<>();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.mapView, SupportMapFragment.class, null)
                    .commit();

            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragmentContainerView, ButtonsFragment.class, null)
                    .commit();
        }

        getSupportFragmentManager().addFragmentOnAttachListener(this::onAttachFragment);



        if(locService == null) {
            locService = new Intent(this, LocationService.class);
            startLocationService();
        }
    }

    public void locationUpdated(LocationResult locResult){
        boolean prevNull = currentMarker == null;
        if(!prevNull)
            currentMarker.remove();

        Location loc = locResult.getLocations().get(locResult.getLocations().size() - 1);
        LatLng current = new LatLng(loc.getLatitude(), loc.getLongitude());
        currentMarker = gMap.addMarker(new MarkerOptions().position(current)
                .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .title("You are here").draggable(false));

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        int imageIndex;
        do{
            // To make it more colorful we prevent from choosing the same color as the
            // previous one
            imageIndex = r.nextInt(imageIds.size());
        }while (images.size() >= 2 && images.lastIndexOf(imageIndex) >= images.size() - 2
                || images.size() == 1 && images.getLast() == imageIndex);

        capTimes.add(formatter.format(date));
        images.add(imageIndex);
        latitudes.add(loc.getLatitude());
        longitudes.add(loc.getLongitude());


        if(prevNull)
            moveMapCamera(true);
    }

    private void createImageIds(){
        imageIds = new HashMap<>();

        imageIds.put(0, R.drawable.loc_icon_blue);
        imageIds.put(1, R.drawable.loc_icon_green);
        imageIds.put(2, R.drawable.loc_icon_magenta);
        imageIds.put(3, R.drawable.loc_icon_orange);
        imageIds.put(4, R.drawable.loc_icon_red);
    }

    public void lookupLocation(int index){
        if(lookupMarker != null)
            lookupMarker.remove();

        float hue = 0;
        switch(images.get(index)){
            case 0: hue = BitmapDescriptorFactory.HUE_BLUE; break;
            case 1: hue = BitmapDescriptorFactory.HUE_GREEN; break;
            case 2: hue = BitmapDescriptorFactory.HUE_MAGENTA; break;
            case 3: hue = BitmapDescriptorFactory.HUE_ORANGE; break;
            case 4: hue = BitmapDescriptorFactory.HUE_RED; break;
        }

        LatLng lookUp = new LatLng(latitudes.get(index), longitudes.get(index));
        lookupMarker = gMap.addMarker(new MarkerOptions().position(lookUp)
                .icon(BitmapDescriptorFactory
                        .defaultMarker(hue))
                .title("A previous location").draggable(false));

        moveMapCamera(false, lookupMarker);
    }

    public void moveMapCamera(boolean zoom){
        moveMapCamera(zoom, currentMarker);
    }

    private void moveMapCamera(boolean zoom, Marker marker){
        if(marker != null) {
            if(zoom)
                gMap.moveCamera(CameraUpdateFactory.zoomTo(13));

            gMap.moveCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // I used a 'for' just in case I add more permissions
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_GRANTED)
                startLocationService();
                return;
        }

        System.exit(0);
    }

    private boolean startLocServiceInvoked = false;
    private void startLocationService(){
        if(!startLocServiceInvoked)
            startService(locService);

        startLocServiceInvoked = true;
    }


    @Override
    public void onResume() {
        super.onResume();

        startLocationService();
    }

    @Override
    public void onPause(){
        super.onPause();

        /*if(!LocationService.isCallerTracking)
            stopService(locService);*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        System.out.println("MainActivity destroyed");
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
    }

    private void onAttachFragment(FragmentManager fragmentManager, Fragment fragment) {
        if(fragment instanceof SupportMapFragment){
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView))
                    .getMapAsync(this);
        }
        else if(fragment instanceof ButtonsFragment){
            if(lookupMarker != null) {
                lookupMarker.remove();
                lookupMarker = null;
            }
        }
    }


    public static class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(MainActivity.currentMainActivity.isDestroyed()){
                Intent main = new Intent(context, MainActivity.class);
                main.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(main);
            }
        }
    }

    public static class QuitButtonNotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            context.stopService(MainActivity.locService);
            System.exit(0);
        }
    }
}