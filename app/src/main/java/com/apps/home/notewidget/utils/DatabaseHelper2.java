package com.apps.home.notewidget.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.apps.home.notewidget.Objects.Note;
import com.apps.home.notewidget.R;

public class DatabaseHelper2 extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;
    private Context context;

    public DatabaseHelper2(Context context) {
        super(context, Constants.DB_NAME, null, DB_VERSION);
        this.context = context;
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
                    + Constants.NOTE_TEXT_COL + " TEXT, "
                    + Constants.FOLDER_ID_COL + " INTEGER, "
                    + Constants.DELETED_COL + " INTEGER);");
//                    + Constants.ENCRYPTED_COL + " TEXT, "
//                    + Constants.SALT_COL + " INTEGER);");
            Log.e("helper", "created" + Constants.NOTES_TABLE + " table");

            db.execSQL("CREATE TABLE " + Constants.WIDGETS_TABLE + " ("
                    + Constants.ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + Constants.WIDGET_ID_COL + " INTEGER, "
                    + Constants.CONNECTED_NOTE_ID_COL + " INTEGER, "
                    + Constants.CURRENT_WIDGET_MODE_COL + " INTEGER, "
                    + Constants.CURRENT_THEME_MODE_COL + " INTEGER, "
                    + Constants.CURRENT_TEXT_SIZE_COL + " INTEGER);");
            Log.e("helper", "created" + Constants.WIDGETS_TABLE + " table");

            db.execSQL("CREATE TABLE " + Constants.FOLDER_TABLE + " ("
                    + Constants.ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + Constants.FOLDER_NAME_COL + " TEXT, "
                    + Constants.FOLDER_ICON_COL + " INTEGER);");
            Log.e("helper", "created" + Constants.FOLDER_TABLE + " table");

            int myNotesId = (int) insertItem(db, context.getString(R.string.my_notes), R.drawable.ic_nav_black_home);
            int trashId = (int) insertItem(db, context.getString(R.string.trash), R.drawable.ic_nav_black_trash);

            Log.e("helper", "my notes Id " + myNotesId + ", trash id "+ trashId);

            context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).edit().
                    putInt(Constants.MY_NOTES_ID_KEY, myNotesId).putInt(Constants.TRASH_ID_KEY, trashId).apply();

            //long folder = insertItem(db, "Folder 1", R.drawable.ic_nav_black_folder);

            /*for(int i = 0; i < 5; i++){
                ContentValues cv = new ContentValues();
                cv.put(Constants.MILLIS_COL, 165165654);
                cv.put(Constants.NOTE_TITLE_COL, "title my notes"+i);
                cv.put(Constants.NOTE_TEXT_COL, "empty");
                cv.put(Constants.FOLDER_ID_COL, myNotesId);
                cv.put(Constants.DELETED_COL, 0);
                db.insert(Constants.NOTES_TABLE, null, cv);
                Log.e("helper", "Note " + cv);
                cv.put(Constants.FOLDER_ID_COL, folder);
                cv.put(Constants.NOTE_TITLE_COL, "title folder" + i);
                if(i != 4)
                    db.insert(Constants.NOTES_TABLE, null, cv);
                Log.e("helper", "Note " + cv);

            }*/
        }
    }

    public long createNote (Note note){ //TODO asynctask
        SQLiteDatabase db;
        try {
            db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(Constants.NOTE_TITLE_COL, note.getTitle());
            values.put(Constants.NOTE_TEXT_COL, note.getNote().replace(System.getProperty("line.separator"), "<br/>"));
            values.put(Constants.MILLIS_COL, note.getCreatedAt());
            values.put(Constants.FOLDER_ID_COL, note.getFolderId());
            values.put(Constants.DELETED_COL, 0);
            long noteId = db.insert(Constants.NOTES_TABLE, null, values);

            db.close();

            return noteId;
        }catch (SQLiteException e){
            Utils.showToast(context, context.getString(R.string.database_unavailable));
            return -1;
        }
    }

    public long updateNote (Note note, long noteId){ //TODO asynctask
        SQLiteDatabase db;
        try {
            db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(Constants.NOTE_TITLE_COL, note.getTitle());
            values.put(Constants.NOTE_TEXT_COL, note.getNote().replace(System.getProperty("line.separator"), "<br/>"));
            values.put(Constants.FOLDER_ID_COL, note.getFolderId());
            values.put(Constants.DELETED_COL, 0);
            int rows = db.update(Constants.NOTES_TABLE, values, Constants.ID_COL + " = ?",
                    new String[]{Long.toString(noteId)});

            db.close();

            return rows;
        }catch (SQLiteException e){
            Utils.showToast(context, context.getString(R.string.database_unavailable));
            return -1;
        }
    }

    public Note getNote(long noteId){
        SQLiteDatabase db;
        try {
            db = this.getReadableDatabase();

            String selectQuery = "SELECT * FROM " + Constants.NOTES_TABLE + " WHERE " +
                    Constants.ID_COL + " = " + noteId;

            Cursor cursor = db.rawQuery(selectQuery, null);

            if(cursor != null){
                cursor.moveToFirst();

                Note note = new Note();
                note.setId(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.ID_COL)));
                note.setCreatedAt(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.MILLIS_COL)));
                note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TITLE_COL)));
                note.setNote(cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TEXT_COL)));
                note.setFolderId(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.FOLDER_ID_COL)));
                note.setFolderId(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.DELETED_COL)));

                cursor.close();
                db.close();

                return note;
            } else
                return null;
        }catch (SQLiteException e){
            Utils.showToast(context, context.getString(R.string.database_unavailable));
            return null;
        }
    }

    private static long insertItem(SQLiteDatabase db , String folderName, int icon) {
        Log.e("helper", "insert");
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.FOLDER_NAME_COL, folderName);
        contentValues.put(Constants.FOLDER_ICON_COL, icon);
        Log.e("helper", "item inserted " + contentValues.toString());
        return db.insert(Constants.FOLDER_TABLE, null, contentValues);

    }
}