package com.vasciie.gpstrackeronline;

import android.Manifest;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
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
                        .title("Your are here").draggable(false));

                if(prevNull)
                    moveMapCamera();
            }
        };
    }

    private void moveMapCamera(){
        if(currentMarker != null) {
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
            // ...
            System.out.println("Success");
            //System.out.println(locationSettingsResponse.getLocationSettingsStates());
            requestingLocationUpdates = true;
            startLocationUpdates();
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(MainActivity.this,
                            0);

                    e.printStackTrace();
                } catch (IntentSender.SendIntentException sendEx) {
                    // Ignore the error.
                }
            }
        });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            System.exit(0);
            return;
        }
        fusedLocationClient.requestLocationUpdates(locRequest, locCallback, Looper.getMainLooper());
        //requestingLocationUpdates = true;
    }

    private void stopLocationUpdates(){
        fusedLocationClient.removeLocationUpdates(locCallback);
        //requestingLocationUpdates = false;
    }

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