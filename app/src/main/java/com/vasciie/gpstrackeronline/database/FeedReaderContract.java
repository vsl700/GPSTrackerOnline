package com.vasciie.gpstrackeronline.database;

import android.provider.BaseColumns;

public final class FeedReaderContract {
    private FeedReaderContract() {}

    public static class FeedLoggedTarget implements BaseColumns {
        public static final String TABLE_NAME = "LoggedTarget";
        public static final String COLUMN_NAME_CODE = "Code";

        public static final String SQL_CREATE_ENTRIES = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_NAME_CODE + " INTEGER PRIMARY KEY);";

        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static class FeedLocations implements BaseColumns {
        public static final String TABLE_NAME = "Locations";
        public static final String COLUMN_NAME_LAT = "Latitude";
        public static final String COLUMN_NAME_LONG = "Longitude";
        public static final String COLUMN_NAME_MARKER_COLOR = "MarkerColor";
        public static final String COLUMN_NAME_TIME_TAKEN = "TimeTaken";

        public static final String SQL_CREATE_ENTRIES = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY," +
                COLUMN_NAME_LAT + " DOUBLE," +
                COLUMN_NAME_LONG + " DOUBLE," +
                COLUMN_NAME_MARKER_COLOR + " INT," +
                COLUMN_NAME_TIME_TAKEN + " TEXT);";

        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
}
