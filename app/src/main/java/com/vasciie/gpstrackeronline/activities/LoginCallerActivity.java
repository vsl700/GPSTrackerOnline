package com.vasciie.gpstrackeronline.activities;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.vasciie.gpstrackeronline.R;
import com.vasciie.gpstrackeronline.database.FeedReaderContract;

public class LoginCallerActivity extends AppCompatActivity {

    public static Activity currentLoginCallerActivity;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_caller);

        currentLoginCallerActivity = this;

        TextView username = findViewById(R.id.textInput_Username);

        Button login = findViewById(R.id.login_caller_btn);
        login.setOnClickListener(view -> {
            // Gets the data repository in write mode
            SQLiteDatabase db = LoginWayActivity.dbHelper.getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(FeedReaderContract.FeedLoggedUser.COLUMN_NAME_USERNAME, username.getText().toString());

            // Insert the new row (the method below returns the primary key value of the new row)
            db.insert(FeedReaderContract.FeedLoggedUser.TABLE_NAME, null, values);

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);

            LoginWayActivity.currentLoginWayActivity.finish();
            if(LoginTargetActivity.currentLoginTargetActivity != null)
                LoginTargetActivity.currentLoginTargetActivity.finish();

            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        currentLoginCallerActivity = null;
    }
}
