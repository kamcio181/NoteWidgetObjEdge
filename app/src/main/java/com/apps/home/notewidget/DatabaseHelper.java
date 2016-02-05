package com.apps.home.notewidget;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by k.kaszubski on 1/20/16.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "database";
    private static final int DB_VERSION = 1;


    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.e("helper", "On create");
        updateDatabase(db, 0, DB_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        updateDatabase(db, oldVersion, newVersion);
    }

    private void updateDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 1) {
            db.execSQL("CREATE TABLE " + Constants.NOTES_TABLE + " ("
                    + Constants.ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + Constants.MILLIS_COL + " INTEGER, "
                    + Constants.NOTE_TITLE_COL + " TEXT, "
                    + Constants.NOTE_TEXT_COL + " TEXT);");
            Log.e("helper", "created" + Constants.NOTES_TABLE + " table");
            db.execSQL("CREATE TABLE " + Constants.WIDGETS_TABLE + " ("
                    + Constants.ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + Constants.WIDGET_ID + " INTEGER, "
                    + Constants.CONNECTED_NOTE_ID + " INTEGER, "
                    + Constants.CURRENT_WIDGET_MODE + " INTEGER, "
                    + Constants.CURRENT_THEME_MODE + " INTEGER, "
                    + Constants.CURRENT_TEXT_SIZE + " INTEGER);");

            Log.e("helper", "created" + Constants.WIDGETS_TABLE + " table");
            insertItem(db, 14122015, "tytuł 1", "przykładowy text notatki 1");
            insertItem(db, 14122015, "tytuł 2", "przykładowy text notatki 2");
            insertItem(db, 14122015, "tytuł 3", "przykładowy text notatki 3");
            insertItem(db, 14122015, "tytuł 4", "przykładowy text notatki 4");
            insertItem(db, 14122015, "tytuł 5", "przykładowy text notatki 5");
        }
    }


    private static void insertItem(SQLiteDatabase db , int millis, String title, String text) {
        Log.e("helper", "insert");
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.MILLIS_COL, millis);
        contentValues.put(Constants.NOTE_TITLE_COL, title);
        contentValues.put(Constants.NOTE_TEXT_COL, text);
        db.insert(Constants.NOTES_TABLE, null, contentValues);
        Log.e("helper", "item inserted " + contentValues.toString());
    }
}