package com.vasciie.gpstrackeronline;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

import java.security.InvalidKeyException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap gMap;
    private OnMapReadyCallback thisActivity;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locCallback;
    private Marker currentMarker, lookupMarker;

    // Using LinkedList to improve performance while tracking
    public LinkedList<String> capTimes;
    public LinkedList<Double> latitudes, longitudes;
    public LinkedList<Integer> images;

    // For indexing the app's database image numbers with the real app picture IDs (held in class R)
    public HashMap<Integer, Integer> imageIds;

    private static final Random r = new Random();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createImageIds();

        capTimes = new LinkedList<>();
        latitudes = new LinkedList<>();
        longitudes = new LinkedList<>();
        images = new LinkedList<>();

        if (savedInstanceState == null) {
            ButtonsFragment.main = this;
            LocationsListFragment.main = this;

            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.mapView, SupportMapFragment.class, null)
                    .commit();

            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragmentContainerView, ButtonsFragment.class, null)
                    .commit();
        }

        thisActivity = this;
        getSupportFragmentManager().addFragmentOnAttachListener(this::onAttachFragment);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationRequest();

        locCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locResult) {
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
        };
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
            //case 1: hue = BitmapDescriptorFactory.HUE_CYAN; break;
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

    private LocationRequest locRequest;
    protected void createLocationRequest() {
        locRequest = LocationRequest.create();
        locRequest.setInterval(10000);
        locRequest.setFastestInterval(5000); //TODO: CHANGE TO 5000
        locRequest.setSmallestDisplacement(2);
        locRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task =
            client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, locationSettingsResponse -> {
            // All location settings are satisfied. The client can initialize
            // location requests here.

            System.out.println("Success");

            requestingLocationUpdates = true;
            startLocationUpdates();
        });
    }

    private void startLocationUpdates() {
        if(updatesOn)
            return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 14894);
            return;
        }

        fusedLocationClient.requestLocationUpdates(locRequest, locCallback, Looper.getMainLooper());
        updatesOn = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // I used a 'for' just in case I add more permissions
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_GRANTED)
                startLocationUpdates();
                return;
        }

        System.exit(0);
    }

    private void stopLocationUpdates(){
        fusedLocationClient.removeLocationUpdates(locCallback);
        updatesOn = false;
    }

    private boolean updatesOn = false; // On some devices onResume is called after permission is given, while on others - not! So we check whether we have already turned updates on
    private boolean requestingLocationUpdates = false;
    @Override
    public void onResume() {
        super.onResume();

        if(requestingLocationUpdates)
            startLocationUpdates();
    }

    @Override
    public void onPause(){
        super.onPause();

        stopLocationUpdates();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
    }

    private void onAttachFragment(FragmentManager fragmentManager, Fragment fragment) {
        if(fragment instanceof SupportMapFragment){
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView))
                    .getMapAsync(thisActivity);
        }
        else if(fragment instanceof ButtonsFragment){
            if(lookupMarker != null) {
                lookupMarker.remove();
                lookupMarker = null;
            }
        }
    }
}