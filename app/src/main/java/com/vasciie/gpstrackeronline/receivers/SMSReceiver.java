package com.vasciie.gpstrackeronline.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.widget.Toast;

import com.vasciie.gpstrackeronline.activities.LoginWayActivity;
import com.vasciie.gpstrackeronline.activities.MainActivity;
import com.vasciie.gpstrackeronline.activities.MainActivityCaller;
import com.vasciie.gpstrackeronline.database.FeedReaderDbHelper;
import com.vasciie.gpstrackeronline.services.TrackerService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SMSReceiver extends BroadcastReceiver {

    private static final String currentLocTag = "Current:";
    private static final String locListTag = "Full List:";
    private static final String unavailableTag = "Unavailable";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (LoginWayActivity.dbHelper == null)
            LoginWayActivity.dbHelper = new FeedReaderDbHelper(context);


        if (!LoginWayActivity.checkLoggedIn(false))
            return;

        System.out.println("SMS Received!");


        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            SmsMessage[] msgs = new SmsMessage[pdus.length];
            String from = null;
            StringBuilder dataSB = new StringBuilder();

            for (int i = 0; i < msgs.length; i++) {
                msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                if(from == null)
                    from = msgs[i].getOriginatingAddress();
                dataSB.append(msgs[i].getDisplayMessageBody());
            }

            String data = dataSB.toString();

            System.out.println(from);
            System.out.println(data);

            if (!data.contains(MainActivity.systemName))
                return;

            int indexOfCode = data.indexOf("Code:");
            String sentCode = data.substring(data.indexOf(':', indexOfCode) + 1, data.indexOf('\n', indexOfCode));
            if (data.contains(MainActivity.smsServiceRequest)) {
                String actualCode = LoginWayActivity.getLoggedTargetCode() + "";
                if (sentCode.equals(actualCode)) { // To verify that the message is not a... prank
                    LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    boolean gps_enabled = false;
                    boolean network_enabled;

                    try {
                        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
                    network_enabled = activeNetworkInfo != null && activeNetworkInfo.isConnected();

                    System.out.println("GPS: " + gps_enabled);
                    System.out.println("Internet: " + network_enabled);

                    Intent locService;
                    if (!TrackerService.alive) {
                        MainActivity.locService = locService = new Intent(context.getApplicationContext(), TrackerService.class);
                        TrackerService.prevLoc = null;
                    }
                    else{
                        locService = MainActivity.locService;
                    }

                    context.startService(locService);
                    TrackerService.isCallerTracking = true;


                    boolean returnOnline = data.contains(MainActivity.returnOnlineReq);
                    boolean returnSms = data.contains(MainActivity.returnSmsReq);
                    if (returnOnline && returnSms) {
                        if (!network_enabled) {
                            ExecutorService threadPool = Executors.newCachedThreadPool();
                            boolean finalGps_enabled = gps_enabled;
                            String finalFrom = from;
                            threadPool.submit(() -> {
                                if (finalGps_enabled) {
                                    int timeout = 60;
                                    while (TrackerService.prevLoc == null && timeout > 0) {
                                        try {
                                            Thread.sleep(1000);
                                            timeout--;
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                            return false;
                                        }
                                    }
                                }

                                // Send SMS
                                String currentLoc = TrackerService.prevLoc.getLatitude() + ";" + TrackerService.prevLoc.getLongitude();
                                String locList = getLocationsDataList();

                                String message = String.format("%s service %s:\nCode:%s\n\n%s\n%s\n\n%s\n%s\n\n%s", MainActivity.systemName, MainActivity.smsServiceResponse, sentCode, currentLocTag, currentLoc,
                                        locListTag, locList, finalGps_enabled);
                                SmsManager smsManager = SmsManager.getDefault();
                                smsManager.sendMultipartTextMessage(finalFrom, null, smsManager.divideMessage(message), null, null);

                                TrackerService.isCallerTracking = false;

                                return true;
                            });
                        }
                    } else if (returnSms) {
                        ExecutorService threadPool = Executors.newCachedThreadPool();
                        boolean finalGps_enabled = gps_enabled;
                        String finalFrom = from;
                        threadPool.submit(() -> {
                            if(finalGps_enabled || network_enabled) {
                                int timeout = 60;
                                while (TrackerService.prevLoc == null && timeout > 0) {
                                    try {
                                        Thread.sleep(1000);
                                        timeout--;
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                        return false;
                                    }
                                }
                            }

                            // Send SMS
                            String currentLoc;
                            if(TrackerService.prevLoc == null)
                                currentLoc = unavailableTag;
                            else currentLoc = TrackerService.prevLoc.getLatitude() + ";" + TrackerService.prevLoc.getLongitude();
                            String locList = getLocationsDataList();

                            String message = String.format("%s service %s:\nCode:%s\n\n%s\n%s\n\n%s\n%s\n\n%s", MainActivity.systemName, MainActivity.smsServiceResponse, sentCode, currentLocTag, currentLoc,
                                    locListTag, locList, finalGps_enabled || network_enabled);
                            SmsManager smsManager = SmsManager.getDefault();
                            smsManager.sendMultipartTextMessage(finalFrom, null, smsManager.divideMessage(message), null, null);

                            TrackerService.isCallerTracking = false;

                            return true;
                        });
                    }
                }
            } else if (data.contains(MainActivity.smsServiceResponse) && LoginWayActivity.loggedInCaller) {
                if(MainActivityCaller.currentMainActivity.isDestroyed()){
                    context.startActivity(new Intent(context, MainActivityCaller.class));
                }

                int x = data.indexOf(locListTag) + locListTag.length() + 1;
                System.out.println(x);
                int y = Math.max(data.lastIndexOf("\n\n"), x);
                System.out.println(y);

                String currentStr = data.substring(data.indexOf(currentLocTag) + currentLocTag.length() + 1, data.indexOf(locListTag) - 2);
                String locListStr = data.substring(x, y);

                // Two reasons for running this method asynchronously:
                // - there's a networking operation inside it
                // - unless the method, containing this comment and all the other stuff, has finished
                // execution the main activity will not start and the 'while' loops inside the method below
                // will turn into endless loops as they require the activity to be created
                ExecutorService threadPool = Executors.newCachedThreadPool();
                threadPool.submit(() -> {
                    try{
                        // If an exception is on another thread, it doesn't stop the application,
                        // and also doesn't print out on the console without 'printStackTrace'
                        MainActivityCaller.changeSearchedPhoneLocation(Integer.parseInt(sentCode), currentStr, locListStr);
                    }catch (Exception e){e.printStackTrace();}
                });


                if(currentStr.contains(unavailableTag))
                    Toast.makeText(context, "Current location couldn't be accessed! Services unavailable!", Toast.LENGTH_LONG).show();
                else if (data.contains("false"))
                    Toast.makeText(context, "Current location might be an old one! Location services are unavailable!", Toast.LENGTH_LONG).show();

                if(locListStr.length() == 0)
                    Toast.makeText(context, "No saved locations!", Toast.LENGTH_LONG).show();
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
