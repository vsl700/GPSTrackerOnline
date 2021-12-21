package com.vasciie.gpstrackeronline.activities;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.vasciie.gpstrackeronline.fragments.ButtonsFragment;
import com.vasciie.gpstrackeronline.R;
import com.vasciie.gpstrackeronline.database.FeedReaderContract;
import com.vasciie.gpstrackeronline.database.FeedReaderDbHelper;
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
    public static LinkedList<String> capTimes;
    public static LinkedList<Double> latitudes, longitudes;
    public static LinkedList<Integer> images;

    private static FeedReaderDbHelper dbHelper;

    // For indexing the app's database image numbers with the real app picture IDs (held in class R)
    public static HashMap<Integer, Integer> imageIds;

    private static final Random r = new Random();

    public static MainActivity currentMainActivity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if(currentMainActivity == null) { // If that's the first time we start the activity
            createImageIds();

            capTimes = new LinkedList<>();
            latitudes = new LinkedList<>();
            longitudes = new LinkedList<>();
            images = new LinkedList<>();

            dbHelper = LoginWayActivity.dbHelper;
            readLocationsFromDB();

            locService = new Intent(this, LocationService.class);
            startLocationService();
        }

        currentMainActivity = this;

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
    }

    private void readLocationsFromDB(){
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                FeedReaderContract.FeedLocations.COLUMN_NAME_LAT,
                FeedReaderContract.FeedLocations.COLUMN_NAME_LONG,
                FeedReaderContract.FeedLocations.COLUMN_NAME_MARKER_COLOR,
                FeedReaderContract.FeedLocations.COLUMN_NAME_TIME_TAKEN
        };

        Cursor cursor = db.query(
                FeedReaderContract.FeedLocations.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // don't sort
        );

        while(cursor.moveToNext()) {
            double lat = cursor.getDouble(
                    cursor.getColumnIndexOrThrow(FeedReaderContract.FeedLocations.COLUMN_NAME_LAT));
            double lng = cursor.getDouble(
                    cursor.getColumnIndexOrThrow(FeedReaderContract.FeedLocations.COLUMN_NAME_LONG));
            int color = cursor.getInt(
                    cursor.getColumnIndexOrThrow(FeedReaderContract.FeedLocations.COLUMN_NAME_MARKER_COLOR));
            String capTime = cursor.getString(
                    cursor.getColumnIndexOrThrow(FeedReaderContract.FeedLocations.COLUMN_NAME_TIME_TAKEN));

            latitudes.add(lat);
            longitudes.add(lng);
            images.add(color);
            capTimes.add(capTime);
        }
        cursor.close();
    }

    public void saveNewLocationToDB(Location loc){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        String dateStr = formatter.format(date);

        int imageIndex;
        do{
            // To make it more colorful we prevent from choosing the same color as
            // one of the previous twos
            imageIndex = r.nextInt(imageIds.size());
        }while (images.size() >= 2 && images.lastIndexOf(imageIndex) >= images.size() - 2
                || images.size() == 1 && images.getLast() == imageIndex);


        capTimes.add(dateStr);
        images.add(imageIndex);
        latitudes.add(loc.getLatitude());
        longitudes.add(loc.getLongitude());


        // Gets the data repository in write mode
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedLocations.COLUMN_NAME_LAT, loc.getLatitude());
        values.put(FeedReaderContract.FeedLocations.COLUMN_NAME_LONG, loc.getLongitude());
        values.put(FeedReaderContract.FeedLocations.COLUMN_NAME_MARKER_COLOR, imageIndex);
        values.put(FeedReaderContract.FeedLocations.COLUMN_NAME_TIME_TAKEN, dateStr);

        // Insert the new row (the method below returns the primary key value of the new row)
        db.insert(FeedReaderContract.FeedLocations.TABLE_NAME, null, values);
    }

    public static void saveAllLocations(){
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // A very efficient way to go through multiple linked lists at a time
        while (latitudes.iterator().hasNext()){
            ContentValues values = new ContentValues();
            values.put(FeedReaderContract.FeedLocations.COLUMN_NAME_LAT, latitudes.iterator().next());
            values.put(FeedReaderContract.FeedLocations.COLUMN_NAME_LONG, longitudes.iterator().next());
            values.put(FeedReaderContract.FeedLocations.COLUMN_NAME_MARKER_COLOR, images.iterator().next());
            values.put(FeedReaderContract.FeedLocations.COLUMN_NAME_TIME_TAKEN, capTimes.iterator().next());

            db.insert(FeedReaderContract.FeedLocations.TABLE_NAME, null, values);
        }
    }

    public void locationUpdated(Location loc){
        boolean prevNull = currentMarker == null;
        if(!prevNull)
            currentMarker.remove();

        LatLng current = new LatLng(loc.getLatitude(), loc.getLongitude());
        currentMarker = gMap.addMarker(new MarkerOptions().position(current)
                .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .title("You are here").draggable(false));


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
                startService(locService);
                return;
        }

        quitApplication(this);
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

    private void logout(){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(FeedReaderContract.FeedLoggedUser.TABLE_NAME, null, null);
        db.delete(FeedReaderContract.FeedLoggedTarget.TABLE_NAME, null, null);
        db.delete(FeedReaderContract.FeedLocations.TABLE_NAME, null, null);

        capTimes.clear();
        images.clear();
        latitudes.clear();
        longitudes.clear();

        stopService(locService);

        Intent intent = new Intent(this, LoginWayActivity.class);
        startActivity(intent);
        finish();
    }

    private static void quitApplication(Context context){
        context.stopService(MainActivity.locService);
        dbHelper.close();
        System.exit(0);
    }

    // create an action bar button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_quit_btn) {
            quitApplication(this);
        }
        else if(id == R.id.menu_logout_btn){
            logout();
        }

        return super.onOptionsItemSelected(item);
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
}