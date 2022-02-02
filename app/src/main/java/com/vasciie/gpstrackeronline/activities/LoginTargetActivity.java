package com.vasciie.gpstrackeronline.activities;

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

public class LoginTargetActivity extends AppCompatActivity {

    private static class LoginCheckTask extends AsyncTask<LoginTargetActivity, Void, Void> {

        public LoginCheckTask(){super();} // To prevent a Deprecation warning

        @Override
        protected Void doInBackground(LoginTargetActivity... objects) {
            String codeStr = objects[0].code.getText().toString();
            int code = Integer.parseInt(codeStr);
            if(!APIConnector.TargetLogin(code)){
                objects[0].runOnUiThread(() -> {
                    objects[0].progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(objects[0], "Login failed!", Toast.LENGTH_SHORT).show();
                });
            }else {
                code = APIConnector.ChangeCodeRequest(code);
                int finalCode = code;
                objects[0].runOnUiThread(() -> objects[0].loginSuccess(finalCode));
            }

            return null;
        }
    }


    private TextView code;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_target);

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

    private synchronized void loginSuccess(int code){
        // This method is sometimes being invoked twice when logging in with the 'Enter' key
        // (on keyDown and on keyUp events from the OnEditorAction)
        if(LoginWayActivity.loggedInTarget)
            return;

        // Gets the data repository in write mode
        SQLiteDatabase db = LoginWayActivity.dbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedLoggedTarget.COLUMN_NAME_CODE, code);

        // Insert the new row (the method below returns the primary key value of the new row)
        db.insert(FeedReaderContract.FeedLoggedTarget.TABLE_NAME, null, values);

        LoginWayActivity.loggedInTarget = true;

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

        LoginWayActivity.currentLoginWayActivity.finish();

        finish();
    }
}
