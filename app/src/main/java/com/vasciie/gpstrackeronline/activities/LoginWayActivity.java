package com.vasciie.gpstrackeronline.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.transition.Slide;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.vasciie.gpstrackeronline.R;
import com.vasciie.gpstrackeronline.database.FeedReaderContract;
import com.vasciie.gpstrackeronline.database.FeedReaderDbHelper;
import com.vasciie.gpstrackeronline.services.APIConnector;
import com.vasciie.gpstrackeronline.services.TrackerService;

public class LoginWayActivity extends AppCompatActivity {

    private static class LoginCheckTask extends AsyncTask<LoginWayActivity, Void, Void>{

        public LoginCheckTask(){super();} // To prevent a Deprecation warning

        @Override
        protected Void doInBackground(LoginWayActivity... objects) {
            if(checkLoggedIn(true)){
                objects[0].startMainActivity();
            }else {
                objects[0].runOnUiThread(() -> {
                    objects[0].entryProgressBar.setVisibility(View.INVISIBLE);
                    objects[0].loginCallerBtn.setEnabled(true);
                    objects[0].loginTargetBtn.setEnabled(true);
                });
            }

            return null;
        }
    }


    private ProgressBar entryProgressBar;
    private Button loginCallerBtn, loginTargetBtn;

    public static FeedReaderDbHelper dbHelper;
    public static LoginWayActivity currentLoginWayActivity;

    public static boolean loggedInTarget, loggedInCaller;

    private static final int smsReadRequestCode = 93021;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(TrackerService.alive) {
            startMainActivity();
            return;
        }

        loggedInTarget = loggedInCaller = false;

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS}, smsReadRequestCode);
        }

        if(dbHelper == null)
            dbHelper = new FeedReaderDbHelper(this);


        currentLoginWayActivity = this;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
            getWindow().setExitTransition(new Slide(Gravity.START));
        }

        setContentView(R.layout.activity_login); // Because of the animation feature above


        entryProgressBar = findViewById(R.id.progressBar_entry);

        loginCallerBtn = findViewById(R.id.caller_login_btn);
        loginCallerBtn.setOnClickListener(view -> {
            startOtherActivity(LoginCallerActivity.class);
        });

        loginTargetBtn = findViewById(R.id.target_login_btn);
        loginTargetBtn.setOnClickListener(view -> {
            startOtherActivity(LoginTargetActivity.class);
        });

        new LoginCheckTask().execute(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == smsReadRequestCode || requestCode == TrackerService.gpsAccessRequestCode){
            for(int grantResult : grantResults){
                if(grantResult == PackageManager.PERMISSION_DENIED) {
                    dbHelper.close();
                    System.exit(0);
                }
            }

            if(requestCode == smsReadRequestCode) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, TrackerService.gpsAccessRequestCode);
                }
            }
        }
    }

    private void startMainActivity(){
        Intent intent = new Intent(this, loggedInCaller ? MainActivityCaller.class : MainActivity.class);
        startActivity(intent);

        finish();
    }

    public static boolean checkLoggedIn(boolean startup){
        if(loggedInCaller || loggedInTarget)
            return true;

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                FeedReaderContract.FeedLoggedTarget.COLUMN_NAME_CODE
        };

        Cursor cursor = db.query(
                FeedReaderContract.FeedLoggedTarget.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // don't sort
        );

        // If there's something written in there, it means a target is already logged in
        if(cursor.moveToNext()){
            int code = cursor.getInt(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedLoggedTarget.COLUMN_NAME_CODE));
            cursor.close();

            if(startup){
                if(!APIConnector.TargetLogin(code))
                    return false;
            }

            loggedInTarget = true;
            return true;
        }

        cursor.close();


        String[] projection2 = {
                FeedReaderContract.FeedLoggedUser.COLUMN_NAME_USERNAME,
                FeedReaderContract.FeedLoggedUser.COLUMN_NAME_PASSWORD
        };

        cursor = db.query(
                FeedReaderContract.FeedLoggedUser.TABLE_NAME,   // The table to query
                projection2,             // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // don't sort
        );

        // If there's something written in there, it means a user is already logged in
        if(cursor.moveToNext()){
            String username = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedLoggedUser.COLUMN_NAME_USERNAME));
            String password = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedLoggedUser.COLUMN_NAME_PASSWORD));
            cursor.close();
            if(startup){
                if(!APIConnector.CallerLogin(username, password))
                    return false;
            }

            loggedInCaller = true;
            return true;
        }

        return false;
    }

    public static int getLoggedTargetCode(){
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                FeedReaderContract.FeedLoggedTarget.COLUMN_NAME_CODE
        };

        Cursor cursor = db.query(
                FeedReaderContract.FeedLoggedTarget.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // don't sort
        );

        // If there's something written in there, it means a target is already logged in
        if(cursor.moveToNext()){
            int code = cursor.getInt(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedLoggedTarget.COLUMN_NAME_CODE));
            cursor.close();

            loggedInTarget = true;
            return code;
        }

        cursor.close();

        return -1;
    }

    private void startOtherActivity(Class<?> cls){
        Intent intent = new Intent(this, cls);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        }else{
            startActivity(intent);
        }
    }

    /*public static boolean LoggedIn(){
        return loggedInCaller || loggedInTarget;
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();

        currentLoginWayActivity = null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finish();
    }
}
