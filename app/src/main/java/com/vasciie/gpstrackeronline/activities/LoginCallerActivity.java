package com.vasciie.gpstrackeronline.activities;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
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

public class LoginCallerActivity extends AppCompatActivity {

    private static class LoginCheckTask extends AsyncTask<LoginCallerActivity, Void, Void> {

        public LoginCheckTask(){super();} // To prevent a Deprecation warning

        @Override
        protected Void doInBackground(LoginCallerActivity... objects) {
            if(!APIConnector.CallerLogin(objects[0].username.getText().toString(), objects[0].password.getText().toString())){
                objects[0].runOnUiThread(() -> {
                    objects[0].progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(objects[0], "Login failed!", Toast.LENGTH_SHORT).show();
                });

            }
            else
                objects[0].runOnUiThread(() -> objects[0].loginSuccess());

            objects[0].loginCheckTask = null;

            return null;
        }
    }


    private TextView username, password;
    private ProgressBar progressBar;

    private AsyncTask loginCheckTask;

    public static Activity currentLoginCallerActivity;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_caller);

        currentLoginCallerActivity = this;

        username = findViewById(R.id.textInput_Username);
        username.setOnEditorActionListener((textView, i, keyEvent) -> password.requestFocus());

        password = findViewById(R.id.textInput_Password);
        password.setOnEditorActionListener((textView, i, keyEvent) -> login());

        progressBar = findViewById(R.id.progressBar_caller);

        Button login = findViewById(R.id.login_caller_btn);
        login.setOnClickListener(view -> login());
    }

    private boolean login(){
        progressBar.setVisibility(View.VISIBLE);

        if(username.getText().toString().equals("") || password.getText().toString().equals("")) {
            progressBar.setVisibility(View.INVISIBLE);
            return false;
        }

        loginCheckTask = new LoginCheckTask().execute(this);

        return true;
    }

    private void loginSuccess(){
        // Gets the data repository in write mode
        SQLiteDatabase db = LoginWayActivity.dbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedLoggedUser.COLUMN_NAME_USERNAME, username.getText().toString());
        values.put(FeedReaderContract.FeedLoggedUser.COLUMN_NAME_PASSWORD, password.getText().toString());

        // Insert the new row (the method below returns the primary key value of the new row)
        db.insert(FeedReaderContract.FeedLoggedUser.TABLE_NAME, null, values);

        LoginWayActivity.loggedInCaller = true;

        Intent intent = new Intent(this, MainActivityCaller.class);
        startActivity(intent);

        LoginWayActivity.currentLoginWayActivity.finish();
        if(LoginTargetActivity.currentLoginTargetActivity != null)
            LoginTargetActivity.currentLoginTargetActivity.finish();

        finish();

        currentLoginCallerActivity = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if(loginCheckTask == null)
            currentLoginCallerActivity = null;
    }
}
