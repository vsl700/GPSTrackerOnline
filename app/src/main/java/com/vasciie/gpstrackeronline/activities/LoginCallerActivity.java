package com.vasciie.gpstrackeronline.activities;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
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

    private TextView username, password;
    private ProgressBar progressBar;

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

        if(!APIConnector.CallerLogin(username.getText().toString(), password.getText().toString())){
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(this, "Login failed!", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Gets the data repository in write mode
        SQLiteDatabase db = LoginWayActivity.dbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedLoggedUser.COLUMN_NAME_USERNAME, username.getText().toString());

        // Insert the new row (the method below returns the primary key value of the new row)
        db.insert(FeedReaderContract.FeedLoggedUser.TABLE_NAME, null, values);

        LoginWayActivity.loggedInCaller = true;

        Intent intent = new Intent(this, MainActivityCaller.class);
        startActivity(intent);

        LoginWayActivity.currentLoginWayActivity.finish();
        if(LoginTargetActivity.currentLoginTargetActivity != null)
            LoginTargetActivity.currentLoginTargetActivity.finish();

        finish();

        progressBar.setVisibility(View.INVISIBLE);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        currentLoginCallerActivity = null;
    }
}
