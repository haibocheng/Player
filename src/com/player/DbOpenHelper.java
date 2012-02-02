package com.player;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbOpenHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "player";
    public static final String KEY_POSITION = "position";
    public static final String KEY_FILE = "file";
    public static final String TABLE_NAME = "tracklist";
    public static final String TABLE_CREATE = "CREATE TABLE "+TABLE_NAME+" ("+KEY_POSITION+" TEXT, "+KEY_FILE+" TEXT);";

    DbOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
         db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
         onCreate(db);
    }
}
