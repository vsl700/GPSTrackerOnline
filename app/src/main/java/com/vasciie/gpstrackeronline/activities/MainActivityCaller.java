package com.vasciie.gpstrackeronline.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.vasciie.gpstrackeronline.R;
import com.vasciie.gpstrackeronline.fragments.ButtonsFragment;
import com.vasciie.gpstrackeronline.fragments.NoInternetDialog;
import com.vasciie.gpstrackeronline.fragments.RecyclerViewAdapterPhones;
import com.vasciie.gpstrackeronline.fragments.SMSDialog;
import com.vasciie.gpstrackeronline.services.APIConnector;
import com.vasciie.gpstrackeronline.services.TrackerService;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivityCaller extends MainActivity {
    private static class FirstOperationsTask extends AsyncTask<MainActivityCaller, Void, Void> {

        public FirstOperationsTask(){super();}

        @Override
        protected Void doInBackground(MainActivityCaller... mainActivities) {
            mainActivities[0].codes = APIConnector.GetTargetCodes();
            if(mainActivities[0].codes == null){
                mainActivities[0].runOnUiThread(() -> Toast.makeText(mainActivities[0], "Error! Try restarting the app or check your connection!", Toast.LENGTH_LONG).show());
                return null;
            }
            if(tempCodes == null || tempCodes.length != mainActivities[0].codes.length)
                tempCodes = mainActivities[0].codes;
            mainActivities[0].targetMarkers = new Marker[tempCodes.length];
            System.out.println(mainActivities[0].codes);

            mainActivities[0].names = APIConnector.GetTargetNames();
            mainActivities[0].sendContacts();

            if(mainActivities[0].currentIndex != -1)
                APIConnector.SendLocationsList(mainActivities[0].codes[mainActivities[0].currentIndex], latitudes, longitudes, images, capTimes);

            mainActivities[0].runOnUiThread(() -> mainActivities[0].setupPhonesList());

            return null;
        }
    }
    private static class PhoneSelectedTask extends AsyncTask<MainActivityCaller, Void, Void> {

        public PhoneSelectedTask(){super();}

        @Override
        protected Void doInBackground(MainActivityCaller... mainActivities) {
            int code = mainActivities[0].codes[mainActivities[0].currentIndex];
            String[] prevLocs = APIConnector.GetPreviousLocations(code);

            latitudes.clear();
            longitudes.clear();
            images.clear();
            capTimes.clear();

            if(prevLocs != null)
            for(String loc : prevLocs){
                loc = loc.replace(",", ".");
                String[] locData = loc.split(";");

                double lat = Double.parseDouble(locData[0]);
                latitudes.add(lat);

                double lng = Double.parseDouble(locData[1]);
                longitudes.add(lng);

                int image = Integer.parseInt(locData[2]);
                images.add(image);

                String capTime = locData[3];
                capTimes.add(capTime);
            }

            if(mainActivities[0].getSupportFragmentManager().findFragmentByTag("loclist") != null && mainActivities[0].getSupportFragmentManager().findFragmentByTag("loclist").isVisible())
                mainActivities[0].showLocationsList();

            return null;
        }
    }
    private static class NetworkCallback extends ConnectivityManager.NetworkCallback{
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);

            currentMainActivity.syncWithInternet();
            ((MainActivityCaller) currentMainActivity).dismissNoInternet();

            while(currentMainActivity.outerNetworkCallback == null){
                System.out.println("Waiting for the service...");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            currentMainActivity.outerNetworkCallback.onConnected();
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);

            ((MainActivityCaller) currentMainActivity).showNoInternet(true);

            if(currentMainActivity.outerNetworkCallback != null)
                currentMainActivity.outerNetworkCallback.onDisconnected();
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            /*boolean hasCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            boolean hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);*/
        }
    }


    private RecyclerView recyclerView;
    private RecyclerViewAdapterPhones rvAdapter;

    private NoInternetDialog noInternetDialog;

    private Marker[] targetMarkers; // of current locations

    String[] names;
    private int[] codes;
    private static int[] tempCodes; // is used when sending & receiving SMS, as the SMS will contain the old code
    private int currentIndex; // Selected phone's index from the list

    private AsyncTask firstTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(cm != null)
            cm.unregisterNetworkCallback(networkCallback);
        networkCallback = new NetworkCallback();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_caller);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.mapView2, SupportMapFragment.class, null)
                    .commit();

            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragmentContainerView2, ButtonsFragment.class, null)
                    .commit();
        }

        getSupportFragmentManager().addFragmentOnAttachListener(this::onAttachFragment);
        getSupportFragmentManager().executePendingTransactions();
        System.out.println(getSupportFragmentManager());
        System.out.println(getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView2));
        System.out.println(this);


        recyclerView = findViewById(R.id.phones_list);
        currentIndex = -1;

        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        if(activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
            showNoInternet(false);
        }
    }

    private void showNoInternet(boolean check){
        if(getLifecycle().getCurrentState().equals(Lifecycle.State.RESUMED) || !check) {
            try{
                noInternetDialog = new NoInternetDialog();
                noInternetDialog.show(getSupportFragmentManager(), "no_internet");
            }catch (Exception e) {e.printStackTrace();}
        }
    }

    private void dismissNoInternet(){
        if (noInternetDialog != null && getSupportFragmentManager().findFragmentByTag("no_internet") != null) {
            try {
                noInternetDialog.dismiss();
            }catch (Exception e) {e.printStackTrace();}
        }
    }

    @Override
    public void syncWithInternet(){
        firstTask = new FirstOperationsTask().execute(this);
    }

    public void sendContacts(){
        JSONObject contacts = new JSONObject();

        try {
            Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

            while (phones.moveToNext()) {
                @SuppressLint("Range") String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                @SuppressLint("Range") String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                contacts.put(name, phoneNumber);
            }
            phones.close();

            System.out.println(contacts.toString());

            APIConnector.SendContacts(contacts);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void setupPhonesList(){
        rvAdapter = new RecyclerViewAdapterPhones(this, names, currentIndex);
        recyclerView.setAdapter(rvAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    public void onPhoneSelected(int index){ // The index of the phone in the phones list
        currentIndex = index;
        moveMapCamera(false, targetMarkers[index]);

        new PhoneSelectedTask().execute(this);
    }

    @Override
    protected void logout() {
        super.logout();

        APIConnector.CallerLogout();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(rvAdapter != null)
            rvAdapter.onDestroy();

        dismissNoInternet();

        if(TrackerService.alive)
            TrackerService.stopHubConnection();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean temp = super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.menu_sms_btn).setVisible(true);

        return temp;
    }

    private static final int smsSendRequestCode = 39843;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_sms_btn){
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.SEND_SMS}, smsSendRequestCode);
            }
            else {
                openSMSDialog();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void openSMSDialog(){
        if(currentIndex == -1){
            Toast.makeText(this, "Please select a phone to send the SMS to!", Toast.LENGTH_LONG).show();
            return;
        }
        SMSDialog smsDialog = new SMSDialog();
        smsDialog.show(getSupportFragmentManager(), "sms_dialog");
    }

    public static void takeActionOnSmsSendRequest(boolean online, boolean sms){
        MainActivityCaller main = (MainActivityCaller) currentMainActivity;
        int oldCode = APIConnector.GetTargetOldCode(main.codes[main.currentIndex]);
        String phoneNumber = APIConnector.GetTargetPhoneNumber(main.codes[main.currentIndex]);
        if(phoneNumber == null){
            main.runOnUiThread(() -> Toast.makeText(main, "Error! Try to restart the app!", Toast.LENGTH_LONG).show());
            return;
        }
        if(phoneNumber.equals("-1")){
            main.runOnUiThread(() -> Toast.makeText(main, "Phone Number is not set for this device!", Toast.LENGTH_LONG).show());
            return;
        }
        String message = String.format("%s service %s:\nCode:%s\n\n%s\n%s", systemName, smsServiceRequest, oldCode,
                online ? returnOnlineReq : "", sms ? returnSmsReq : "");

        // Change the Target's code
        int newCode = APIConnector.ChangeCodeRequest(oldCode);
        if(newCode != -1) // If there's no fail connecting the API method
            main.codes[main.currentIndex] = newCode;

        // Send the SMS
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendMultipartTextMessage(phoneNumber, null, smsManager.divideMessage(message), null, null);
    }

    private void onAttachFragment(FragmentManager fragmentManager, Fragment fragment) {
        if(fragment instanceof SupportMapFragment){
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView2))
                    .getMapAsync(this);
        }
        else if(fragment instanceof ButtonsFragment){
            if(lookupMarker != null) {
                lookupMarker.remove();
                lookupMarker = null;
            }
        }
    }

    public void targetLocationUpdated(int index, LatLng current){
        if(index == -1){
            syncWithInternet();
            return;
        }

        if(targetMarkers[index] != null)
            targetMarkers[index].remove();

        targetMarkers[index] = gMap.addMarker(new MarkerOptions().position(current)
                .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                .title(names[index] + " is here").draggable(false));
    }

    public int getIndexByOldCode(int targetCode){
        for(int i = 0; i < tempCodes.length; i++){
            if(targetCode == tempCodes[i]){
                return i;
            }
        }

        for(int i = 0; i < codes.length; i++){
            if(targetCode == codes[i]){
                // If the phone sends data through a code, contained in the array with the current
                // codes, it means it has already switched from the old one to the new one.
                // Thus, we overwrite the code in the old codes array with the new code
                tempCodes[i] = targetCode;
                return i;
            }
        }

        return -1;
    }

    public static void changeSearchedPhoneLocation(int targetCode, String currentLoc, String locsList){
        // I added this 'while' due to the fact that this method runs before the 'currentMainActivity' variable changes
        while(currentMainActivity.isDestroyed()){
            System.out.print("currentMainActivity is null-");
        }
        // The described occurrence above happens because, as the SMS receiver starts the activity
        // and after that calls this method (asynchronously), only the window opens up, while
        // the activity is still not created! This happens because while this method runs on another
        // thread, the activity creates after the Android system finishes executing the
        // receiver method (the one that starts the activity and runs this method).
        // And because of that, the 'currentMainActivity' variable still hasn't changed
        // to the new activity!

        MainActivityCaller main = (MainActivityCaller) currentMainActivity;
        try {
            // If the activity is just being created, firstTask would still be null
            System.out.println("\nfirstTask cycle started");
            while(main.firstTask == null || main.gMap == null){
                System.out.print("firstTask or gMap is null-");
            }
            main.firstTask.get();
            System.out.println("\nfirstTask finished");
        } catch (Exception e) {
            e.printStackTrace();
        }

        int index = main.getIndexByOldCode(targetCode);

        if(index == -1)
            return;

        String[] currentElements = currentLoc.split(";");
        if(currentElements.length > 1) {
            double currentLat = Double.parseDouble(currentElements[0]);
            double currentLng = Double.parseDouble(currentElements[1]);

            int finalIndex = index;
            main.runOnUiThread(() -> main.targetLocationUpdated(finalIndex, new LatLng(currentLat, currentLng)));
        }


        String[] locsListArr = locsList.split("\n");
        if(locsListArr[0].length() == 0)
            return;

        for(String loc : locsListArr){
            String[] elements = loc.split(";");
            String capTime = elements[3];
            if(capTimes.contains(capTime))
                continue;

            double lat = Double.parseDouble(elements[0]);
            double lng = Double.parseDouble(elements[1]);
            int image = Integer.parseInt(elements[2]);


            latitudes.add(lat);
            longitudes.add(lng);
            images.add(image);
            capTimes.add(capTime);
        }

        APIConnector.SendLocationsList(targetCode, latitudes, longitudes, images, capTimes);


        main.currentIndex = index;
        int finalIndex1 = index;
        main.runOnUiThread(() -> {
            main.setupPhonesList();
            main.onPhoneSelected(finalIndex1);
            main.showLocationsList();
        });

        /*boolean applicationOff = dbHelper == null;
        if(applicationOff)
            initializeDB();

        saveAllLocations();*/
    }
}