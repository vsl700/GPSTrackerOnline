package com.vasciie.gpstrackeronline.activities;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.vasciie.gpstrackeronline.R;
import com.vasciie.gpstrackeronline.database.FeedReaderContract;
import com.vasciie.gpstrackeronline.services.APIConnector;

public class LoginTargetActivity extends AppCompatActivity {

    private static class LoginCheckTask extends AsyncTask<LoginTargetActivity, Void, Void> {

        public LoginCheckTask(){super();} // To prevent a Deprecation warning

        @Override
        protected Void doInBackground(LoginTargetActivity... objects) {
            if(!APIConnector.TargetLogin(Integer.parseInt(objects[0].code.getText().toString()))){
                objects[0].runOnUiThread(() -> {
                    objects[0].progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(objects[0], "Login failed!", Toast.LENGTH_SHORT).show();
                });
            }else
                objects[0].runOnUiThread(() -> objects[0].loginSuccess());

            return null;
        }
    }


    private TextView code;
    private ProgressBar progressBar;
    private TelephonyManager tm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_target);

        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        code = findViewById(R.id.textInput_Code);
        code.setOnEditorActionListener((textView, i, keyEvent) -> login());

        progressBar = findViewById(R.id.progressBar_target);

        Button login = findViewById(R.id.login_target_btn);
        login.setOnClickListener(view -> login());
    }

    private boolean login(){
        if(code.getText().toString().equals(""))
            return false;

        progressBar.setVisibility(View.VISIBLE);
        new LoginCheckTask().execute(this);

        return true;
    }

    private void loginSuccess(){
        // Gets the data repository in write mode
        SQLiteDatabase db = LoginWayActivity.dbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedLoggedTarget.COLUMN_NAME_CODE, code.getText().toString());

        // Insert the new row (the method below returns the primary key value of the new row)
        db.insert(FeedReaderContract.FeedLoggedTarget.TABLE_NAME, null, values);

        LoginWayActivity.loggedInTarget = true;

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

        LoginWayActivity.currentLoginWayActivity.finish();

        finish();
    }
}
