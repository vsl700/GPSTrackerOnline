package com.vasciie.gpstrackeronline.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class FeedReaderDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Logins.db";


    public FeedReaderDbHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

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
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
