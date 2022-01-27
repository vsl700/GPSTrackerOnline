package com.vasciie.gpstrackeronline.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.SupportMapFragment;
import com.vasciie.gpstrackeronline.R;
import com.vasciie.gpstrackeronline.fragments.ButtonsFragment;
import com.vasciie.gpstrackeronline.fragments.RecyclerViewAdapterPhones;
import com.vasciie.gpstrackeronline.fragments.SMSDialog;
import com.vasciie.gpstrackeronline.services.APIConnector;

public class MainActivityCaller extends MainActivity {
    private static class FirstOperationsTask extends AsyncTask<MainActivityCaller, Void, Void> {

        public FirstOperationsTask(){super();}

        @Override
        protected Void doInBackground(MainActivityCaller... mainActivities) {
            mainActivities[0].codes = APIConnector.GetTargetCodes();
            System.out.println(mainActivities[0].codes);

            mainActivities[0].setupPhonesList();

            return null;
        }
    }
    private static class PhoneSelectedTask extends AsyncTask<MainActivityCaller, Void, Void> {

        public PhoneSelectedTask(){super();}

        @Override
        protected Void doInBackground(MainActivityCaller... mainActivities) {


            return null;
        }
    }


    private RecyclerView recyclerView;
    private RecyclerViewAdapterPhones rvAdapter;

    private int[] codes;
    private int currentIndex = -1; // Selected phone's index from the list


    @Override
    protected void onCreate(Bundle savedInstanceState) {
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


        recyclerView = findViewById(R.id.phones_list);

        new FirstOperationsTask().execute(this);
    }

    private void setupPhonesList(){
        String[] names = APIConnector.GetTargetNames();

        rvAdapter = new RecyclerViewAdapterPhones(this, names);
        recyclerView.setAdapter(rvAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    public void onPhoneSelected(int index){ // The index of the phone in the phones list
        currentIndex = index;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        rvAdapter.onDestroy();
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
            else openSMSDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    private void openSMSDialog(){
        SMSDialog smsDialog = new SMSDialog();
        smsDialog.show(getSupportFragmentManager(), "sms_dialog");
    }

    public static void takeActionOnSmsSendRequest(boolean online, boolean sms){
        String message = String.format("%s service %s:\nCode:%s\n\n%s\n%s", systemName, smsServiceRequest, 123456,
                online ? returnOnlineReq : "", sms ? returnSmsReq : "");
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage("+16505551212", null, message, null, null);
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
}