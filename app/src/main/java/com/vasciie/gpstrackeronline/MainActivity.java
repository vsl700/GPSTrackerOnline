package com.vasciie.gpstrackeronline;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap gMap;
    private OnMapReadyCallback thisActivity;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locCallback;
    private Marker currentMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ConstraintLayout const1 = findViewById(R.id.const1);


        Button locationsListBtn = findViewById(R.id.locsList);
        locationsListBtn.setOnClickListener(view -> {});

        Button currentLocBtn = findViewById(R.id.currentLoc);
        currentLocBtn.setOnClickListener(view -> moveMapCamera(false));

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.mapView, SupportMapFragment.class, null)
                    .commit();
        }

        thisActivity = this;
        getSupportFragmentManager().addFragmentOnAttachListener((fragmentManager, fragment) ->
                ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView))
                        .getMapAsync(thisActivity));

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
                        .title("You are here").draggable(false));

                if(prevNull)
                    moveMapCamera(true);
            }
        };
    }

    private void moveMapCamera(boolean zoom){
        if(currentMarker != null) {
            if(zoom)
                gMap.moveCamera(CameraUpdateFactory.zoomTo(13));

            gMap.moveCamera(CameraUpdateFactory.newLatLng(currentMarker.getPosition()));
        }
    }

    private LocationRequest locRequest;
    protected void createLocationRequest() {
        locRequest = LocationRequest.create();
        locRequest.setInterval(10000);
        locRequest.setFastestInterval(1000); //CHANGE TO 5000
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
}