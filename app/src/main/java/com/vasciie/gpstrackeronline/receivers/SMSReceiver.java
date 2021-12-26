package com.vasciie.gpstrackeronline.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import com.vasciie.gpstrackeronline.activities.LoginWayActivity;
import com.vasciie.gpstrackeronline.activities.MainActivity;
import com.vasciie.gpstrackeronline.database.FeedReaderDbHelper;
import com.vasciie.gpstrackeronline.services.LocationService;

import java.util.Date;
import java.util.Iterator;

public class SMSReceiver extends BroadcastReceiver {

    private static final String currentLocTag = "Current:";
    private static final String locListTag = "Full List:";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (LoginWayActivity.dbHelper == null)
            LoginWayActivity.dbHelper = new FeedReaderDbHelper(context);


        if (!LoginWayActivity.checkLoggedIn())
            return;

        System.out.println("SMS Received!");


        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            SmsMessage[] msgs = new SmsMessage[pdus.length];

            for (int i = 0; i < msgs.length; i++) {
                msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                String from = msgs[i].getOriginatingAddress();
                String data = msgs[i].getDisplayMessageBody();

                System.out.println(from);
                System.out.println(data);

                if (!data.contains(MainActivity.systemName))
                    continue;

                if (data.contains(MainActivity.smsServiceRequest)) {
                    String sentCode = data.substring(data.indexOf(':', data.indexOf("Code:")) + 1, data.lastIndexOf("\n\n"));
                    String actualCode = LoginWayActivity.getLoggedTargetCode() + "";
                    if (sentCode.equals(actualCode)) { // To verify that the message is not a... prank
                        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                        boolean gps_enabled = false;
                        boolean network_enabled = false;

                        try {
                            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        try {
                            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        if (!LocationService.updatesOn) {
                            Intent locService;
                            MainActivity.locService = locService = new Intent(context, LocationService.class);
                            LocationService.isCallerTracking = true;

                            if(gps_enabled)
                                LocationService.prevLoc = null;

                            context.startService(locService);
                        }

                        boolean returnOnline = data.contains(MainActivity.returnOnlineReq);
                        boolean returnSms = data.contains(MainActivity.returnSmsReq);
                        if (returnOnline && returnSms) {
                            if (!network_enabled) {
                                if(gps_enabled) {
                                    while (LocationService.prevLoc == null) {
                                        try {
                                            Thread.sleep(500);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                // Send SMS
                                String currentLoc = LocationService.prevLoc.getLatitude() + ";" + LocationService.prevLoc.getLongitude();
                                String locList = getLocationsDataList();

                                String message = String.format("%s service %s:\nCode:%s\n\n%s\n%s\n\n%s\n%s\n\n%s", MainActivity.systemName, MainActivity.smsServiceResponse, sentCode, currentLocTag, currentLoc,
                                        locListTag, locList, gps_enabled);
                                SmsManager smsManager = SmsManager.getDefault();
                                smsManager.sendTextMessage(from, null, message, null, null);
                            }
                        } else if (returnSms) {
                            if(gps_enabled) {
                                while (LocationService.prevLoc == null) {
                                    try {
                                        Thread.sleep(500); // TODO: This cycle blocks the app sometimes! Think of another way!
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            // Send SMS
                            String currentLoc = LocationService.prevLoc.getLatitude() + ";" + LocationService.prevLoc.getLongitude();
                            String locList = getLocationsDataList();

                            String message = String.format("%s service %s:\nCode:%s\n\n%s\n%s\n\n%s\n%s\n\n%s", MainActivity.systemName, MainActivity.smsServiceResponse, sentCode, currentLocTag, currentLoc,
                                    locListTag, locList, gps_enabled);
                            SmsManager smsManager = SmsManager.getDefault();
                            smsManager.sendTextMessage(from, null, message, null, null);
                        }
                    }
                } else if (data.contains(MainActivity.smsServiceResponse)) {

                }
            }
        }
    }

    private String getLocationsDataList() {
        StringBuilder strBuilder = new StringBuilder();

        if (MainActivity.latitudes.isEmpty())
            MainActivity.readLocationsFromDB();

        Iterator<Double> iterator1 = MainActivity.latitudes.listIterator();
        Iterator<Double> iterator2 = MainActivity.longitudes.listIterator();
        Iterator<Integer> iterator3 = MainActivity.images.listIterator();
        Iterator<String> iterator4 = MainActivity.capTimes.listIterator();
        while (iterator1.hasNext()) {
            strBuilder.append(iterator1.next());
            strBuilder.append(';');
            strBuilder.append(iterator2.next());
            strBuilder.append(';');
            strBuilder.append(iterator3.next());
            strBuilder.append(';');
            strBuilder.append(iterator4.next());
            strBuilder.append('\n');
        }

        return strBuilder.toString();
    }
}
