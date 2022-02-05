package com.vasciie.gpstrackeronline.activities;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;
import com.vasciie.gpstrackeronline.R;
import com.vasciie.gpstrackeronline.database.FeedReaderContract;
import com.vasciie.gpstrackeronline.database.FeedReaderDbHelper;
import com.vasciie.gpstrackeronline.fragments.ButtonsFragment;
import com.vasciie.gpstrackeronline.fragments.LocationsListFragment;
import com.vasciie.gpstrackeronline.services.APIConnector;
import com.vasciie.gpstrackeronline.services.TrackerService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static class FirstOperationsTask extends AsyncTask<MainActivity, Void, Void>{

        public FirstOperationsTask(){super();}

        @Override
        protected Void doInBackground(MainActivity... mainActivities) {
            int oldCode = LoginWayActivity.getLoggedTargetCode();
            int newCode = APIConnector.GetTargetNewCode(oldCode);
            if(oldCode != newCode && newCode != -1){
                saveCodeToDB(newCode);
            }

            while(!TrackerService.alive){
                System.out.println("Waiting for the service...");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            TrackerService.createHubConnection();
            TrackerService.startHubConnection();

            if(TrackerService.prevLoc != null)
                APIConnector.SendCurrentLocation(TrackerService.prevLoc.getLatitude(), TrackerService.prevLoc.getLongitude());
            APIConnector.SendLocationsList(newCode, latitudes, longitudes, images, capTimes);

            return null;
        }

        private void saveCodeToDB(int code){
            while(dbHelper == null){
                System.out.println("Waiting for db...");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete(FeedReaderContract.FeedLoggedTarget.TABLE_NAME, null, null);

            ContentValues values = new ContentValues();
            values.put(FeedReaderContract.FeedLoggedTarget.COLUMN_NAME_CODE, code);

            db.insert(FeedReaderContract.FeedLoggedTarget.TABLE_NAME, null, values);
        }
    }
    private static class NetworkCallback extends ConnectivityManager.NetworkCallback{
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);

            if(!this.equals(networkCallback))
                return;

            System.out.println("on-internet-available");
            currentMainActivity.syncWithInternet();

            if(currentMainActivity.outerNetworkCallback != null)
                currentMainActivity.outerNetworkCallback.onConnected();
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);

            if(currentMainActivity.outerNetworkCallback != null)
                currentMainActivity.outerNetworkCallback.onDisconnected();
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
        }
    }
    public interface OuterNetworkCallback{
        void onConnected();
        void onDisconnected();
    }


    protected GoogleMap gMap;

    protected Marker currentMarker, lookupMarker;
    protected Marker[] lookupMarkers;

    public static Intent locService;

    protected static ConnectivityManager cm;
    protected NetworkRequest networkRequest;
    protected static ConnectivityManager.NetworkCallback networkCallback;

    public OuterNetworkCallback outerNetworkCallback;

    protected static final String capTimePattern = "yyyy-MM-dd 'at' HH:mm:ss";
    public final static SimpleDateFormat formatter = new SimpleDateFormat(capTimePattern, Locale.US);

    // Using LinkedList to improve performance while tracking
    public static LinkedList<String> capTimes = new LinkedList<>();
    public static LinkedList<Double> latitudes = new LinkedList<>(), longitudes = new LinkedList<>();
    public static LinkedList<Integer> images = new LinkedList<>();

    protected static FeedReaderDbHelper dbHelper;

    // For indexing the app's database image numbers with the real app picture IDs (held in class R)
    public static HashMap<Integer, Integer> imageIds;

    protected static final Random r = new Random();

    public static MainActivity currentMainActivity;

    public static final String systemName = "Phone Tracker-Online";
    public static final String smsServiceRequest = "request";
    public static final String smsServiceResponse = "response";
    public static final String returnOnlineReq = "return-online";
    public static final String returnSmsReq = "return-sms";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!(this instanceof MainActivityCaller))
            setContentView(R.layout.activity_main_target);

        initializeDB();

        if(currentMainActivity == null) { // If that's the first time we start the activity
            createImageIds();

            capTimes = new LinkedList<>();
            latitudes = new LinkedList<>();
            longitudes = new LinkedList<>();
            images = new LinkedList<>();
        }

        if(!(this instanceof MainActivityCaller) && latitudes.size() == 0)
            readLocationsFromDB();

        currentMainActivity = this;
        if(!TrackerService.alive) {
            locService = new Intent(this, TrackerService.class);
        }
        startServices();

        if(!(this instanceof MainActivityCaller)) {
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

        requestConnectionCallback();
    }

    public void requestConnectionCallback(){
        networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        boolean cmNull = cm == null;
        cm = getSystemService(ConnectivityManager.class);

        if(!(this instanceof MainActivityCaller)){
            if(!cmNull)
                unregisterConnectionCallback();

            networkCallback = new NetworkCallback();
        }
        cm.requestNetwork(networkRequest, networkCallback);
        System.out.println("Registered connection callback");
    }

    public void unregisterConnectionCallback(){
        cm.unregisterNetworkCallback(networkCallback);
        System.out.println("Unregistered connection callback");
    }

    public void syncWithInternet(){
        new FirstOperationsTask().execute(this);
    }

    protected static void initializeDB(){
        if(LoginWayActivity.dbHelper == null) {
            LoginWayActivity.dbHelper = new FeedReaderDbHelper(MainActivity.currentMainActivity);
        }
        dbHelper = LoginWayActivity.dbHelper;
    }

    public static void readLocationsFromDB(){
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

    private boolean isDistanceLegalForSaving(double lat, double lng, Location loc){
        LatLng oldCoords = new LatLng(lat, lng);
        LatLng newCoords = new LatLng(loc.getLatitude(), loc.getLongitude());
        double distMeters = SphericalUtil.computeDistanceBetween(oldCoords, newCoords);

        return distMeters > 40;
    }

    public void saveNewLocationToDB(Location loc){
        if(latitudes.size() > 0){
            if(!isDistanceLegalForSaving(latitudes.getLast(), longitudes.getLast(), loc))
                return; // Don't save locations that are too close to each other
        }

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
        if(dbHelper == null)
            initializeDB();

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedLocations.COLUMN_NAME_LAT, loc.getLatitude());
        values.put(FeedReaderContract.FeedLocations.COLUMN_NAME_LONG, loc.getLongitude());
        values.put(FeedReaderContract.FeedLocations.COLUMN_NAME_MARKER_COLOR, imageIndex);
        values.put(FeedReaderContract.FeedLocations.COLUMN_NAME_TIME_TAKEN, dateStr);

        // Insert the new row (the method below returns the primary key value of the new row)
        db.insert(FeedReaderContract.FeedLocations.TABLE_NAME, null, values);

        ExecutorService threadPool = Executors.newCachedThreadPool();
        threadPool.submit(() -> APIConnector.SendLocationsList(LoginWayActivity.getLoggedTargetCode(), latitudes, longitudes, images, capTimes));
    }

    public static void saveAllLocations(){
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // A very efficient way to go through multiple linked lists at a time
        Iterator<Double> iterator1 = latitudes.listIterator();
        Iterator<Double> iterator2 = longitudes.listIterator();
        Iterator<Integer> iterator3 = images.listIterator();
        Iterator<String> iterator4 = capTimes.listIterator();
        while (iterator1.hasNext()){
            ContentValues values = new ContentValues();
            values.put(FeedReaderContract.FeedLocations.COLUMN_NAME_LAT, iterator1.next());
            values.put(FeedReaderContract.FeedLocations.COLUMN_NAME_LONG, iterator2.next());
            values.put(FeedReaderContract.FeedLocations.COLUMN_NAME_MARKER_COLOR, iterator3.next());
            values.put(FeedReaderContract.FeedLocations.COLUMN_NAME_TIME_TAKEN, iterator4.next());

            db.insert(FeedReaderContract.FeedLocations.TABLE_NAME, null, values);
        }
    }

    public void locationUpdated(Location loc){
        LatLng current = new LatLng(loc.getLatitude(), loc.getLongitude());
        locationUpdated(current);
    }

    public void locationUpdated(LatLng current){
        boolean prevNull = currentMarker == null;
        if(!prevNull)
            currentMarker.remove();

        currentMarker = gMap.addMarker(new MarkerOptions().position(current)
                .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .title("You are here").draggable(false));


        if(lookupMarkers != null && LoginWayActivity.loggedInTarget)
            setupLookupMarkers();

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

    private int prevIndex = -1;
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
                .title("A selected previous location").draggable(false));

        lookupMarkers[index].setVisible(false);
        if(prevIndex >= 0) {
            lookupMarkers[prevIndex].setVisible(true);
        }

        prevIndex = index;

        moveMapCamera(false, lookupMarker);
    }

    public void setupLookupMarkers(){
        removeLookupMarkers();
        lookupMarkers = new Marker[latitudes.size()];
        Iterator<Double> latIter = latitudes.listIterator();
        Iterator<Double> longIter = longitudes.listIterator();
        Iterator<String> capIter = capTimes.listIterator();
        synchronized (latitudes) { // To clear out a bug with using these lists at the same time
            synchronized (longitudes) {
                synchronized (capTimes) {
                    for (int i = 0; i < lookupMarkers.length; i++) {
                        LatLng lookUp = new LatLng(latIter.next(), longIter.next());
                        lookupMarkers[i] = gMap.addMarker(new MarkerOptions().position(lookUp)
                                .icon(BitmapDescriptorFactory
                                        .defaultMarker(BitmapDescriptorFactory.HUE_RED))
                                .title("A previous location (" + capIter.next() + ")").draggable(false));
                    }
                }
            }
        }
    }

    public void removeLookupMarkers(){
        if(lookupMarkers != null) {
            for (Marker marker : lookupMarkers) {
                marker.remove();
            }
        }

        lookupMarkers = null;
    }

    public void moveMapCamera(boolean zoom){
        moveMapCamera(zoom, currentMarker);
    }

    protected void moveMapCamera(boolean zoom, Marker marker){
        if(marker != null) {
            if(zoom)
                gMap.moveCamera(CameraUpdateFactory.zoomTo(13));

            gMap.moveCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
        }
    }

    private boolean startServicesInvoked = false;
    private void startServices(){
        if(!startServicesInvoked)
            startService(locService);


        startServicesInvoked = true;
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause(){
        super.onPause();

        /*if(!TrackerService.isCallerTracking)
            stopService(locService);*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //unregisterConnectionCallback();
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

    protected void logout(){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(FeedReaderContract.FeedLoggedUser.TABLE_NAME, null, null);
        db.delete(FeedReaderContract.FeedLoggedTarget.TABLE_NAME, null, null);
        db.delete(FeedReaderContract.FeedLocations.TABLE_NAME, null, null);

        capTimes.clear();
        images.clear();
        latitudes.clear();
        longitudes.clear();

        stopService(locService);

        cm = null;

        Intent intent = new Intent(this, LoginWayActivity.class);
        startActivity(intent);
        finish();
    }

    private void quitApplication(){
        unregisterConnectionCallback();
        cm = null;
        networkCallback = null;

        stopService(locService);
        dbHelper.close();
        LoginWayActivity.dbHelper = dbHelper = null;

        capTimes.clear();
        images.clear();
        latitudes.clear();
        longitudes.clear();

        finish();
    }

    // create an action bar button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu, menu);
        menu.findItem(R.id.menu_sms_btn).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }


    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_quit_btn) {
            quitApplication();
        }
        else if(id == R.id.menu_logout_btn){
            logout();
        }

        return super.onOptionsItemSelected(item);
    }

    public void showLocationsList(){
        if(getLifecycle().getCurrentState().equals(Lifecycle.State.RESUMED))
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(LoginWayActivity.loggedInCaller ? R.id.fragmentContainerView2 : R.id.fragmentContainerView, LocationsListFragment.class, null, "loclist")
                .commit();
    }

}