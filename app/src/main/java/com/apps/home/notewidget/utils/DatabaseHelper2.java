package com.apps.home.notewidget.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import com.apps.home.notewidget.objects.Folder;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.R;
import com.apps.home.notewidget.objects.Widget;

import java.util.ArrayList;

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

            final SharedPreferences.Editor editor = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).edit();

            Folder folder = new Folder(context.getString(R.string.my_notes), R.drawable.ic_nav_black_home);
            createFolder(folder, new OnItemInsertListener() {
                @Override
                public void onItemInserted(long id) {
                    editor.putLong(Constants.MY_NOTES_ID_KEY, id).apply();
                }
            });
            folder = new Folder(context.getString(R.string.trash), R.drawable.ic_nav_black_trash);
            createFolder(folder, new OnItemInsertListener() {
                @Override
                public void onItemInserted(long id) {
                    editor.putLong(Constants.TRASH_ID_KEY, id).apply();
                }
            });
        }
    }

    interface OnItemInsertListener{
        void onItemInserted(long id);
    }

    interface OnItemUpdateListener{
        void onItemUpdated(int numberOfRows);
    }

    interface OnItemRemoveListener{
        void onItemRemoved(int numberOfRows);
    }

    interface OnNoteLoadListener {
        void onNoteLoaded(Note note);
    }

    interface OnNotesLoadListener {
        void onNotesLoaded(ArrayList<Note> notes);
    }

    interface OnFolderLoadListener {
        void onFolderLoaded(Folder folder);
    }

    interface OnWidgetLoadListener {
        void onWidgetLoaded(Widget widget);
    }

    public void createNote (Note note, OnItemInsertListener listener){
        new CreateNote(note, listener).execute();
    }

    public void updateNote (Note note, long noteId, OnItemUpdateListener listener){
        new UpdateNote(note, noteId, listener).execute();
    }

    public void getNote(long noteId, OnNoteLoadListener listener){
        new GetNote(noteId, listener).execute();
    }

    public void getNotes(boolean includeDeleted, OnNotesLoadListener listener){

    }

    public void removeNote(long noteId, OnItemRemoveListener listener){
        new RemoveNote(noteId, listener).execute();
    }

    public void createFolder(Folder folder, OnItemInsertListener listener){
        new CreateFolder(folder, listener).execute();
    }

    public void updateFolder (Folder folder, long folderId, OnItemUpdateListener listener){
        new UpdateFolder(folder, folderId, listener).execute();
    }

    public void getFolder(long folderId, OnFolderLoadListener listener){
        new GetFolder(folderId, listener).execute();
    }

    public void removeFolder(long folderId, OnItemRemoveListener listener){
        new RemoveFolder(folderId, listener).execute();
    }

    public void createWidget(Widget widget, OnItemInsertListener listener){
        new CreateWidget(widget, listener).execute();
    }

    public void updateWidget (Widget widget, long widgetId, OnItemUpdateListener listener){
        new UpdateWidget(widget, widgetId, listener).execute();
    }

    public void getWidget(long widgetId, OnWidgetLoadListener listener){
        new GetWidget(widgetId, listener).execute();
    }

    public void removeWidget(long widgetId, OnItemRemoveListener listener){
        new RemoveWidget(widgetId, listener).execute();
    }

    private class CreateNote extends AsyncTask<Void, Void, Long>{
        private Note note;
        private OnItemInsertListener listener;

        public CreateNote(Note note, OnItemInsertListener listener) {
            this.note = note;
            this.listener = listener;
        }

        @Override
        protected Long doInBackground(Void... params) {
            SQLiteDatabase db;
            try {
                db = DatabaseHelper2.this.getWritableDatabase();

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
                return (long) -1;
            }
        }

        @Override
        protected void onPostExecute(Long aLong) {
            super.onPostExecute(aLong);

            if(listener != null)
                listener.onItemInserted(aLong);
        }
    }

    private class UpdateNote extends AsyncTask<Void, Void, Integer> {
        private Note note;
        private long noteId;
        private OnItemUpdateListener listener;

        public UpdateNote(Note note, long noteId, OnItemUpdateListener listener) {
            this.note = note;
            this.noteId = noteId;
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            SQLiteDatabase db;
            try {
                db = DatabaseHelper2.this.getWritableDatabase();

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

        @Override
        protected void onPostExecute(Integer aInt) {
            super.onPostExecute(aInt);

            if(listener != null)
                listener.onItemUpdated(aInt);
        }
    }

    private class GetNote extends AsyncTask<Void, Void, Note> {
        private long noteId;
        private OnNoteLoadListener listener;

        public GetNote(long noteId, OnNoteLoadListener listener) {
            this.noteId = noteId;
            this.listener = listener;
        }

        @Override
        protected Note doInBackground(Void... params) {
            SQLiteDatabase db;
            try {
                db = DatabaseHelper2.this.getReadableDatabase();

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

        @Override
        protected void onPostExecute(Note aNote) {
            super.onPostExecute(aNote);

            if(listener != null)
                listener.onNoteLoaded(aNote);
        }
    }

    private class GetNotes extends AsyncTask<Void, Void, ArrayList<Note>> {
        private boolean includeDeleted;
        private OnNotesLoadListener listener;

        public GetNotes(boolean includeDeleted, OnNotesLoadListener listener) {
            this.includeDeleted = includeDeleted;
            this.listener = listener;
        }

        @Override
        protected ArrayList<Note> doInBackground(Void... params) {
            SQLiteDatabase db;
            try {
                db = DatabaseHelper2.this.getReadableDatabase();

                String selectQuery;

                if(includeDeleted)
                    selectQuery = "SELECT * FROM " + Constants.NOTES_TABLE;
                else
                    selectQuery = "SELECT * FROM " + Constants.NOTES_TABLE + " WHERE " +
                        Constants.DELETED_COL + " = " + 0;

                Cursor cursor = db.rawQuery(selectQuery, null);

                if(cursor != null){
                    cursor.moveToFirst();
                    ArrayList<Note> notes = new ArrayList<>(cursor.getCount());
                    do{
                        Note note = new Note();
                        note.setId(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.ID_COL)));
                        note.setCreatedAt(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.MILLIS_COL)));
                        note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TITLE_COL)));
                        note.setNote(cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TEXT_COL)));
                        note.setFolderId(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.FOLDER_ID_COL)));
                        note.setFolderId(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.DELETED_COL)));

                        notes.add(note);

                    } while (cursor.moveToNext());

                    cursor.close();
                    db.close();

                    return notes;
                } else
                    return null;
            }catch (SQLiteException e){
                Utils.showToast(context, context.getString(R.string.database_unavailable));
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Note> aNotes) {
            super.onPostExecute(aNotes);

            if(listener != null)
                listener.onNotesLoaded(aNotes);
        }
    }

    private class RemoveNote extends AsyncTask<Void, Void, Integer> {
        private long noteId;
        private OnItemRemoveListener listener;

        public RemoveNote(long noteId, OnItemRemoveListener listener) {
            this.noteId = noteId;
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            SQLiteDatabase db;
            try {
                db = DatabaseHelper2.this.getWritableDatabase();

                int rows = db.delete(Constants.NOTES_TABLE, Constants.ID_COL + " = ?",
                        new String[]{Long.toString(noteId)});

                db.close();

                return rows;
            }catch (SQLiteException e){
                Utils.showToast(context, context.getString(R.string.database_unavailable));
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Integer aInt) {
            super.onPostExecute(aInt);

            if(listener != null)
                listener.onItemRemoved(aInt);
        }
    }

    private class CreateFolder extends AsyncTask<Void, Void, Long>{
        private Folder folder;
        private OnItemInsertListener listener;

        public CreateFolder(Folder folder, OnItemInsertListener listener) {
            this.folder = folder;
            this.listener = listener;
        }

        @Override
        protected Long doInBackground(Void... params) {
            SQLiteDatabase db;
            try {
                db = DatabaseHelper2.this.getWritableDatabase();

                ContentValues values = new ContentValues();
                values.put(Constants.FOLDER_NAME_COL, folder.getName());
                values.put(Constants.FOLDER_ICON_COL, folder.getIcon());

                long folderId = db.insert(Constants.FOLDER_TABLE, null, values);

                db.close();

                return folderId;
            }catch (SQLiteException e){
                Utils.showToast(context, context.getString(R.string.database_unavailable));
                return (long) -1;
            }
        }

        @Override
        protected void onPostExecute(Long aLong) {
            super.onPostExecute(aLong);

            if(listener != null)
                listener.onItemInserted(aLong);
        }
    }

    private class UpdateFolder extends AsyncTask<Void, Void, Integer> {
        private Folder folder;
        private long folderId;
        private OnItemUpdateListener listener;

        public UpdateFolder(Folder folder, long folderId, OnItemUpdateListener listener) {
            this.folder = folder;
            this.folderId = folderId;
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            SQLiteDatabase db;
            try {
                db = DatabaseHelper2.this.getWritableDatabase();

                ContentValues values = new ContentValues();
                values.put(Constants.FOLDER_NAME_COL, folder.getName());

                int rows = db.update(Constants.FOLDER_TABLE, values, Constants.ID_COL + " = ?",
                        new String[]{Long.toString(folderId)});

                db.close();

                return rows;
            }catch (SQLiteException e){
                Utils.showToast(context, context.getString(R.string.database_unavailable));
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Integer aInt) {
            super.onPostExecute(aInt);

            if(listener != null)
                listener.onItemUpdated(aInt);
        }
    }

    private class GetFolder extends AsyncTask<Void, Void, Folder> {
        private long folderId;
        private OnFolderLoadListener listener;

        public GetFolder(long folderId, OnFolderLoadListener listener) {
            this.folderId = folderId;
            this.listener = listener;
        }

        @Override
        protected Folder doInBackground(Void... params) {
            SQLiteDatabase db;
            try {
                db = DatabaseHelper2.this.getReadableDatabase();

                /*String selectQuery = "SELECT * FROM " + Constants.FOLDER_TABLE + " WHERE " +
                        Constants.ID_COL + " = " + folderId;*/

                String selectQuery ="SELECT f." + Constants.ID_COL + ", f." + Constants.FOLDER_NAME_COL
                        + ", f." + Constants.FOLDER_ICON_COL
                        + ", COUNT(n." + Constants.DELETED_COL + ") AS " + Constants.NOTES_COUNT_COL
                        + " FROM " + Constants.FOLDER_TABLE + " f LEFT JOIN "
                        + Constants.NOTES_TABLE + " n ON f." + Constants.ID_COL + " = n."
                        + Constants.FOLDER_ID_COL + " AND n." + Constants.DELETED_COL + " = 0"
                        + " AND n." + Constants.FOLDER_ID_COL + " = " + folderId;

                Cursor cursor = db.rawQuery(selectQuery, null);

                if(cursor != null){
                    cursor.moveToFirst();

                    Folder folder = new Folder();
                    folder.setId(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.ID_COL)));
                    folder.setName(cursor.getString(cursor.getColumnIndexOrThrow(Constants.FOLDER_NAME_COL)));
                    folder.setIcon(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.FOLDER_ICON_COL)));
                    folder.setCount(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.NOTES_COUNT_COL)));

                    cursor.close();
                    db.close();

                    return folder;
                } else
                    return null;
            }catch (SQLiteException e){
                Utils.showToast(context, context.getString(R.string.database_unavailable));
                return null;
            }
        }

        @Override
        protected void onPostExecute(Folder aFolder) {
            super.onPostExecute(aFolder);

            if(listener != null)
                listener.onFolderLoaded(aFolder);
        }
    }

    private class RemoveFolder extends AsyncTask<Void, Void, Integer> {
        private long folderId;
        private OnItemRemoveListener listener;

        public RemoveFolder(long folderId, OnItemRemoveListener listener) {
            this.folderId = folderId;
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            SQLiteDatabase db;
            try {
                db = DatabaseHelper2.this.getWritableDatabase();

                int rows = db.delete(Constants.FOLDER_TABLE, Constants.ID_COL + " = ?",
                        new String[]{Long.toString(folderId)});

                db.close();

                return rows;
            }catch (SQLiteException e){
                Utils.showToast(context, context.getString(R.string.database_unavailable));
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Integer aInt) {
            super.onPostExecute(aInt);

            if(listener != null)
                listener.onItemRemoved(aInt);
        }
    }

    private class CreateWidget extends AsyncTask<Void, Void, Long>{
        private Widget widget;
        private OnItemInsertListener listener;

        public CreateWidget(Widget widget, OnItemInsertListener listener) {
            this.widget = widget;
            this.listener = listener;
        }

        @Override
        protected Long doInBackground(Void... params) {
            SQLiteDatabase db;
            try {
                db = DatabaseHelper2.this.getWritableDatabase();

                ContentValues values = new ContentValues();
                values.put(Constants.WIDGET_ID_COL, widget.getWidgetId());
                values.put(Constants.CONNECTED_NOTE_ID_COL, widget.getNoteId());
                values.put(Constants.CURRENT_WIDGET_MODE_COL, Constants.WIDGET_MODE_TITLE);
                values.put(Constants.CURRENT_THEME_MODE_COL, Constants.WIDGET_THEME_LIGHT);
                values.put(Constants.CURRENT_TEXT_SIZE_COL, 13);

                long widgetId = db.insert(Constants.WIDGETS_TABLE, null, values);

                db.close();

                return widgetId;
            }catch (SQLiteException e){
                Utils.showToast(context, context.getString(R.string.database_unavailable));
                return (long) -1;
            }
        }

        @Override
        protected void onPostExecute(Long aLong) {
            super.onPostExecute(aLong);

            if(listener != null)
                listener.onItemInserted(aLong);
        }
    }

    private class UpdateWidget extends AsyncTask<Void, Void, Integer> {
        private Widget widget;
        private long widgetId;
        private OnItemUpdateListener listener;

        public UpdateWidget(Widget widget, long widgetId, OnItemUpdateListener listener) {
            this.widget = widget;
            this.widgetId = widgetId;
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            SQLiteDatabase db;
            try {
                db = DatabaseHelper2.this.getWritableDatabase();

                ContentValues values = new ContentValues();
                values.put(Constants.CURRENT_WIDGET_MODE_COL, widget.getMode());
                values.put(Constants.CURRENT_THEME_MODE_COL, widget.getTheme());
                values.put(Constants.CURRENT_TEXT_SIZE_COL, widget.getTextSize());

                int rows = db.update(Constants.WIDGETS_TABLE, values, Constants.ID_COL + " = ?",
                        new String[]{Long.toString(widgetId)});

                db.close();

                return rows;
            }catch (SQLiteException e){
                Utils.showToast(context, context.getString(R.string.database_unavailable));
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Integer aInt) {
            super.onPostExecute(aInt);

            if(listener != null)
                listener.onItemUpdated(aInt);
        }
    }

    private class GetWidget extends AsyncTask<Void, Void, Widget> {
        private long widgetId;
        private OnWidgetLoadListener listener;

        public GetWidget(long widgetId, OnWidgetLoadListener listener) {
            this.widgetId = widgetId;
            this.listener = listener;
        }

        @Override
        protected Widget doInBackground(Void... params) {
            SQLiteDatabase db;
            try {
                db = DatabaseHelper2.this.getReadableDatabase();

                String selectQuery = "SELECT * FROM " + Constants.WIDGETS_TABLE + " WHERE " +
                        Constants.ID_COL + " = " + widgetId;

                Cursor cursor = db.rawQuery(selectQuery, null);

                if(cursor != null){
                    cursor.moveToFirst();

                    Widget widget = new Widget();
                    widget.setId(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.ID_COL)));
                    widget.setWidgetId(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.WIDGET_ID_COL)));
                    widget.setNoteId(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.CONNECTED_NOTE_ID_COL)));
                    widget.setMode(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.CURRENT_WIDGET_MODE_COL)));
                    widget.setTheme(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.CURRENT_THEME_MODE_COL)));
                    widget.setTextSize(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.CURRENT_TEXT_SIZE_COL)));

                    cursor.close();
                    db.close();

                    return widget;
                } else
                    return null;
            }catch (SQLiteException e){
                Utils.showToast(context, context.getString(R.string.database_unavailable));
                return null;
            }
        }

        @Override
        protected void onPostExecute(Widget aWidget) {
            super.onPostExecute(aWidget);

            if(listener != null)
                listener.onWidgetLoaded(aWidget);
        }
    }

    private class RemoveWidget extends AsyncTask<Void, Void, Integer> {
        private long widgetId;
        private OnItemRemoveListener listener;

        public RemoveWidget(long widgetId, OnItemRemoveListener listener) {
            this.widgetId = widgetId;
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            SQLiteDatabase db;
            try {
                db = DatabaseHelper2.this.getWritableDatabase();

                int rows = db.delete(Constants.WIDGETS_TABLE, Constants.ID_COL + " = ?",
                        new String[]{Long.toString(widgetId)});

                db.close();

                return rows;
            }catch (SQLiteException e){
                Utils.showToast(context, context.getString(R.string.database_unavailable));
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Integer aInt) {
            super.onPostExecute(aInt);

            if(listener != null)
                listener.onItemRemoved(aInt);
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