package com.vasciie.gpstrackeronline.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.vasciie.gpstrackeronline.activities.MainActivity;

public class FeedReaderDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Logins.db";


    public FeedReaderDbHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Either this or the next method might be invoked when needed (schema change, etc) only on
    // attempt to get a writable/readable database instance (getWritableDatabase()/getReadableDatabase())
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(FeedReaderContract.FeedLoggedTarget.SQL_CREATE_ENTRIES);
        db.execSQL(FeedReaderContract.FeedLoggedUser.SQL_CREATE_ENTRIES);
        db.execSQL(FeedReaderContract.FeedLocations.SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(FeedReaderContract.FeedLoggedTarget.SQL_DELETE_ENTRIES);
        db.execSQL(FeedReaderContract.FeedLoggedUser.SQL_DELETE_ENTRIES);
        db.execSQL(FeedReaderContract.FeedLocations.SQL_DELETE_ENTRIES);

        onCreate(db);

        // TODO: Invoke a method to save the current user/target account as well
        MainActivity.saveAllLocations();
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
