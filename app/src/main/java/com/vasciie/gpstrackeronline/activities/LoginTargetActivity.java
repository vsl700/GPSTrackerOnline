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

public class LoginTargetActivity extends AppCompatActivity {

    private TextView code;

    public static Activity currentLoginTargetActivity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_target);

        currentLoginTargetActivity = this;

        code = findViewById(R.id.textInput_Code);
        code.setOnEditorActionListener((textView, i, keyEvent) -> login());

        Button login = findViewById(R.id.login_target_btn);
        login.setOnClickListener(view -> login());
    }

    private boolean login(){
        if(code.getText().equals(""))
            return false;

        // Gets the data repository in write mode
        SQLiteDatabase db = LoginWayActivity.dbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedLoggedTarget.COLUMN_NAME_CODE, code.getText().toString());

        // Insert the new row (the method below returns the primary key value of the new row)
        db.insert(FeedReaderContract.FeedLoggedTarget.TABLE_NAME, null, values);

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

        LoginWayActivity.currentLoginWayActivity.finish();
        if(LoginCallerActivity.currentLoginCallerActivity != null)
            LoginCallerActivity.currentLoginCallerActivity.finish();

        finish();

        return true; // TODO: Use these for online validation
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        currentLoginTargetActivity = null;
    }
}
