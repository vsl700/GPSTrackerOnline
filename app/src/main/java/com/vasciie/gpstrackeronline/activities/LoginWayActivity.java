package com.vasciie.gpstrackeronline.activities;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.transition.Slide;
import android.view.Gravity;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.vasciie.gpstrackeronline.R;
import com.vasciie.gpstrackeronline.database.FeedReaderContract;
import com.vasciie.gpstrackeronline.database.FeedReaderDbHelper;

public class LoginWayActivity extends AppCompatActivity {

    public static FeedReaderDbHelper dbHelper;
    public static Activity currentLoginWayActivity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new FeedReaderDbHelper(this);
        if(checkLoggedIn()){
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);

            finish();
            return;
        }

        currentLoginWayActivity = this;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
            getWindow().setExitTransition(new Slide(Gravity.START));
        }

        setContentView(R.layout.activity_login);

        Button loginCaller = findViewById(R.id.caller_login_btn);
        loginCaller.setOnClickListener(view -> {
            startOtherActivity(LoginCallerActivity.class);
        });

        Button loginTarget = findViewById(R.id.target_login_btn);
        loginTarget.setOnClickListener(view -> {
            startOtherActivity(LoginTargetActivity.class);
        });
    }

    private boolean checkLoggedIn(){
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
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
            cursor.close();
            return true;
        }

        cursor.close();

        cursor = db.query(
                FeedReaderContract.FeedLoggedUser.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // don't sort
        );

        // If there's something written in there, it means a user is already logged in
        if(cursor.moveToNext()){
            cursor.close();
            return true;
        }

        return false;
    }

    private void startOtherActivity(Class<?> cls){
        Intent intent = new Intent(this, cls);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        }else{
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        currentLoginWayActivity = null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        System.exit(0);
    }
}
