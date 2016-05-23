package com.apps.home.notewidget.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.objects.Folder;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.objects.Widget;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final int DB_VERSION = 1;
    private Context context;
    private SearchNotes searchNotes;

    public DatabaseHelper(Context context) {
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

            //db = DatabaseHelper.this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(Constants.FOLDER_NAME_COL, context.getString(R.string.my_notes));
            values.put(Constants.FOLDER_ICON_COL, R.drawable.ic_nav_black_home);

            editor.putLong(Constants.MY_NOTES_ID_KEY, db.insert(Constants.FOLDER_TABLE, null, values)).apply();
            Log.e("Helper", "myNotes ");

            values.put(Constants.FOLDER_NAME_COL, context.getString(R.string.trash));
            values.put(Constants.FOLDER_ICON_COL, R.drawable.ic_nav_black_trash);

            editor.putLong(Constants.TRASH_ID_KEY, db.insert(Constants.FOLDER_TABLE, null, values)).apply();
            Log.e("Helper", "trash ");

            //db.close();

            /*Folder folder = new Folder(context.getString(R.string.my_notes), R.drawable.ic_nav_black_home);
            createFolder(folder, new OnItemInsertListener() {
                @Override
                public void onItemInserted(long id) {
                    editor.putLong(Constants.MY_NOTES_ID_KEY, id).apply();
                    Log.e("Helper", "myNotes "+id);
                }
            });
            folder = new Folder(context.getString(R.string.trash), R.drawable.ic_nav_black_trash);
            createFolder(folder, new OnItemInsertListener() {
                @Override
                public void onItemInserted(long id) {
                    editor.putLong(Constants.TRASH_ID_KEY, id).apply();
                    Log.e("Helper", "trash " + id);
                }
            });*/
            Log.e("Helper", "created");
        }
    }

    public interface OnItemInsertListener{
        void onItemInserted(long id);
    }

    public interface OnItemUpdateListener{
        void onItemUpdated(int numberOfRows);
    }

    public interface OnItemRemoveListener{
        void onItemRemoved(int numberOfRows);
    }

    public interface OnNoteLoadListener {
        void onNoteLoaded(Note note);
    }

    public interface OnNotesLoadListener {
        void onNotesLoaded(ArrayList<Note> notes);
    }

    public interface OnFolderLoadListener {
        void onFolderLoaded(Folder folder);
    }

    public interface OnFoldersLoadListener {
        void onFoldersLoaded(ArrayList<Folder> folders);
    }

    public interface OnWidgetLoadListener {
        void onWidgetLoaded(Widget widget);
    }

    public interface OnWidgetsLoadListener {
        void onWidgetsLoaded(ArrayList<Widget> widgets);
    }

    public interface OnFinishListener {
        void onFinished(boolean result);
    }

    public void createNote (Note note, OnItemInsertListener listener){
        new CreateNote(note, listener).execute();
    }

    public void updateNote (Note note, OnItemUpdateListener listener){
        new UpdateNote(note, listener).execute();
    }

    public void getNote(boolean includeDeleted, long noteId, OnNoteLoadListener listener){
        new GetNote(includeDeleted, noteId, listener).execute();
    }

    public Note getNoteOnDemand(boolean includeDeleted, long noteId){
        try {
            SQLiteDatabase db = getReadableDatabase();

            String selectQuery;

            if(includeDeleted)
                selectQuery = "SELECT * FROM " + Constants.NOTES_TABLE + " WHERE " +
                        Constants.ID_COL + " = " + noteId;
            else
                selectQuery = "SELECT * FROM " + Constants.NOTES_TABLE + " WHERE " +
                        Constants.ID_COL + " = " + noteId + " AND " +Constants.DELETED_COL +
                        " = " + Constants.FALSE;

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

    public void getNotes(boolean includeDeleted, OnNotesLoadListener listener){
        new GetNotes(includeDeleted, listener).execute();
    }

    public void getFolderNotes(int folderId, boolean sortByDate, OnNotesLoadListener listener){
        new GetFolderNotes(folderId, sortByDate, listener).execute();
    }

    public void searchNotes(boolean searchInTitle, boolean searchInContent, String textToFind, OnNotesLoadListener listener){
        if(searchNotes != null)
            searchNotes.cancel(true);
        searchNotes = new SearchNotes(searchInTitle, searchInContent, textToFind, listener);
        searchNotes.execute();
    }

    public void removeNote(long noteId, OnItemRemoveListener listener){
        new RemoveNote(noteId, listener).execute();
    }

    public void removeAllNotesFromTrash(OnItemRemoveListener listener){
        new RemoveAllNotesFromTrash(listener).execute();
    }

    public void restoreAllNotesFromTrash(OnFoldersLoadListener listener){
        new RestoreAllNotesFromTrash(listener).execute();
    }

    public void removeAllNotesFromFolder(long folderId, OnItemRemoveListener listener){
        new RemoveAllNotesFromFolder(folderId, listener).execute();
    }

    public void createFolder(Folder folder, OnItemInsertListener listener){
        new CreateFolder(folder, listener).execute();
    }

    public void updateFolder (Folder folder, OnItemUpdateListener listener){
        new UpdateFolder(folder, listener).execute();
    }

    public void getFolder(long folderId, OnFolderLoadListener listener){
        new GetFolder(folderId, listener).execute();
    }

    public void getFolders(OnFoldersLoadListener listener){
        new GetFolders(listener).execute();
    }

    public ArrayList<Folder> getFoldersOnDemand(){
        try {
            SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();
            Log.e("Helper", "get Readable - Folders");
            String selectQuery ="SELECT f." + Constants.ID_COL + ", f." + Constants.FOLDER_NAME_COL
                    + ", f." + Constants.FOLDER_ICON_COL
                    + ", COUNT(n." + Constants.DELETED_COL + ") AS " + Constants.NOTES_COUNT_COL
                    + " FROM " + Constants.FOLDER_TABLE + " f LEFT JOIN "
                    + Constants.NOTES_TABLE + " n ON f." + Constants.ID_COL + " = n."
                    + Constants.FOLDER_ID_COL + " AND n." + Constants.DELETED_COL + " = " + Constants.FALSE
                    +  " GROUP BY f." + Constants.ID_COL;

            Cursor cursor = db.rawQuery(selectQuery, null);

            int deletedCount = (int) DatabaseUtils.queryNumEntries(db, Constants.NOTES_TABLE, Constants.DELETED_COL + " = ?", new String[]{Integer.toString(Constants.TRUE)});
            long deletedId = Utils.getTrashNavId(context);

            if(cursor != null && cursor.moveToFirst()){

                ArrayList<Folder> folders = new ArrayList<>(cursor.getCount());
                do{
                    Folder folder = new Folder();
                    folder.setId(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.ID_COL)));
                    folder.setName(cursor.getString(cursor.getColumnIndexOrThrow(Constants.FOLDER_NAME_COL)));
                    folder.setIcon(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.FOLDER_ICON_COL)));
                    folder.setCount(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.NOTES_COUNT_COL)));
                    if(folder.getId() == deletedId)
                        folder.setCount(deletedCount);

                    folders.add(folder);

                } while (cursor.moveToNext());

                cursor.close();
                db.close();

                return folders;
            } else
                return null;
        }catch (SQLiteException e){
            Log.e(TAG, "" + e);
            return null;
        }
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

    public int updateWidgetOnDemand (Widget widget, long widgetId){
        try {
            SQLiteDatabase db = DatabaseHelper.this.getWritableDatabase();

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

    public void getWidget(long itemId, OnWidgetLoadListener listener){
        new GetWidget(itemId, listener).execute();
    }

    public void getWidget(int widgetId, OnWidgetLoadListener listener){
        new GetWidget(widgetId, listener).execute();
    }

    public void getWidgetsWithNote(long noteId, OnWidgetsLoadListener listener){
        new GetWidgetsWithNote(noteId, listener).execute();
    }

    public Widget getWidgetOnDemand(int widgetId){
        try {
            SQLiteDatabase db = getReadableDatabase();
            String selectQuery = "SELECT * FROM " + Constants.WIDGETS_TABLE + " WHERE " +
                    Constants.WIDGET_ID_COL + " = " + widgetId;

            Cursor cursor = db.rawQuery(selectQuery, null);

            if(cursor != null){
                cursor.moveToFirst();

                Widget widget = new Widget();
                widget.setId(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.ID_COL)));
                widget.setWidgetId(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.WIDGET_ID_COL)));
                widget.setNoteId(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.CONNECTED_NOTE_ID_COL)));
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

    public void removeWidget(int widgetId, OnItemRemoveListener listener){
        new RemoveWidget(widgetId, listener).execute();
    }

    public void clearWidgetsTable(OnFinishListener listener){
        new ClearWidgetsTable(listener).execute();
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
            try {
                SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();

                ContentValues values = new ContentValues();
                values.put(Constants.NOTE_TITLE_COL, note.getTitle());
                values.put(Constants.NOTE_TEXT_COL, note.getNote().replace(System.getProperty("line.separator"), "<br/>"));
                values.put(Constants.MILLIS_COL, note.getCreatedAt());
                values.put(Constants.FOLDER_ID_COL, note.getFolderId());
                values.put(Constants.DELETED_COL, Constants.FALSE);

                long noteId = db.insert(Constants.NOTES_TABLE, null, values);

                db.close();

                return noteId;
            }catch (SQLiteException e){
                Log.e(TAG, ""+e);
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
        private OnItemUpdateListener listener;

        public UpdateNote(Note note, OnItemUpdateListener listener) {
            this.note = note;
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();

                ContentValues values = new ContentValues();
                values.put(Constants.NOTE_TITLE_COL, note.getTitle());
                values.put(Constants.NOTE_TEXT_COL, note.getNote().replace(System.getProperty("line.separator"), "<br/>"));
                values.put(Constants.FOLDER_ID_COL, note.getFolderId());
                values.put(Constants.DELETED_COL, note.getDeletedState());

                int rows = db.update(Constants.NOTES_TABLE, values, Constants.ID_COL + " = ?",
                        new String[]{Long.toString(note.getId())});

                db.close();

                return rows;
            }catch (SQLiteException e){
                Log.e(TAG, ""+e);
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
        private boolean includeDeleted;
        private long noteId;
        private OnNoteLoadListener listener;

        public GetNote(boolean includeDeleted, long noteId, OnNoteLoadListener listener) {
            this.includeDeleted = includeDeleted;
            this.noteId = noteId;
            this.listener = listener;
        }

        @Override
        protected Note doInBackground(Void... params) {
            try {
                SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();

                String selectQuery;

                if(includeDeleted)
                    selectQuery = "SELECT * FROM " + Constants.NOTES_TABLE + " WHERE " +
                            Constants.ID_COL + " = " + noteId;
                else
                    selectQuery = "SELECT * FROM " + Constants.NOTES_TABLE + " WHERE " +
                            Constants.ID_COL + " = " + noteId + " AND " +Constants.DELETED_COL +
                            " = " + Constants.FALSE;

                Cursor cursor = db.rawQuery(selectQuery, null);

                if(cursor != null){
                    cursor.moveToFirst();

                    Note note = new Note();
                    note.setId(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.ID_COL)));
                    note.setCreatedAt(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.MILLIS_COL)));
                    note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TITLE_COL)));
                    note.setNote(cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TEXT_COL)));
                    note.setFolderId(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.FOLDER_ID_COL)));
                    note.setDeletedState(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.DELETED_COL)));

                    cursor.close();
                    db.close();

                    return note;
                } else
                    return null;
            }catch (SQLiteException e){
                Log.e(TAG, ""+e);
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
            try {
                SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();

                String selectQuery;

                if(includeDeleted)
                    selectQuery = "SELECT * FROM " + Constants.NOTES_TABLE;
                else
                    selectQuery = "SELECT * FROM " + Constants.NOTES_TABLE + " WHERE " +
                        Constants.DELETED_COL + " = " + Constants.FALSE;

                Cursor cursor = db.rawQuery(selectQuery, null);

                if(cursor != null && cursor.moveToFirst()){

                    ArrayList<Note> notes = new ArrayList<>(cursor.getCount());
                    do{
                        Note note = new Note();
                        note.setId(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.ID_COL)));
                        note.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.MILLIS_COL)));
                        note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TITLE_COL)));
                        note.setNote(cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TEXT_COL)));
                        note.setFolderId(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.FOLDER_ID_COL)));
                        note.setDeletedState(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.DELETED_COL)));

                        notes.add(note);

                    } while (cursor.moveToNext());

                    cursor.close();
                    db.close();

                    return notes;
                } else
                    return null;
            }catch (SQLiteException e){
                Log.e(TAG, "" + e);
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

    private class GetFolderNotes extends AsyncTask<Void, Void, ArrayList<Note>> {
        private int folderId;
        private boolean sortByDate;
        private OnNotesLoadListener listener;

        public GetFolderNotes(int folderId, boolean sortByDate, OnNotesLoadListener listener) {
            this.folderId = folderId;
            this.sortByDate = sortByDate;
            this.listener = listener;
        }

        @Override
        protected ArrayList<Note> doInBackground(Void... params) {
            try {
                SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();

                String orderColumn = sortByDate ? Constants.MILLIS_COL : Constants.NOTE_TITLE_COL;

                String selectQuery;

                if(folderId != Utils.getTrashNavId(context)) //is not trash
                    selectQuery = "SELECT * FROM " + Constants.NOTES_TABLE + " WHERE " +//TODO check query
                                Constants.FOLDER_ID_COL + " = " + folderId + " AND " +
                                Constants.DELETED_COL + " = " + Constants.FALSE + " ORDER BY LOWER(" + orderColumn +") ASC";
                else
                    selectQuery = "SELECT * FROM " + Constants.NOTES_TABLE + " WHERE " +
                            Constants.DELETED_COL + " = " + Constants.TRUE + " ORDER BY LOWER(" + orderColumn +") ASC";

                Cursor cursor = db.rawQuery(selectQuery, null);

                if(cursor != null && cursor.moveToFirst()){

                    ArrayList<Note> notes = new ArrayList<>(cursor.getCount());
                    do{
                        Note note = new Note();
                        note.setId(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.ID_COL)));
                        note.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.MILLIS_COL)));
                        note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TITLE_COL)));
                        note.setNote(cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TEXT_COL)));
                        note.setFolderId(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.FOLDER_ID_COL)));
                        note.setDeletedState(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.DELETED_COL)));

                        notes.add(note);

                    } while (cursor.moveToNext());

                    cursor.close();
                    db.close();

                    return notes;
                } else
                    return null;
            }catch (SQLiteException e){
                Log.e(TAG, "" + e);
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

    private class SearchNotes extends AsyncTask<Void, Void, ArrayList<Note>>{
        private boolean searchInTitle;
        private boolean searchInContent;
        private String textToFind;
        private OnNotesLoadListener listener;

        public SearchNotes(boolean searchInTitle, boolean searchInContent, String textToFind, OnNotesLoadListener listener) {
            this.searchInTitle = searchInTitle;
            this.searchInContent = searchInContent;
            this.textToFind = textToFind;
            this.listener = listener;
        }

        @Override
        protected ArrayList<Note> doInBackground(Void... params) {
            String where;

            if(textToFind.length() == 0 || (!searchInTitle && !searchInContent))
                return null;
            String textToFindLowerCase = textToFind.toLowerCase();
            String arg = "'%" + textToFindLowerCase + "%'";

            if(searchInTitle && !searchInContent){
                where = "LOWER(" + Constants.NOTE_TITLE_COL + ") LIKE " + arg;
            } else if (!searchInTitle){
                where = "LOWER(" + Constants.NOTE_TEXT_COL + ") LIKE " + arg;
            } else {
                where = "LOWER(" + Constants.NOTE_TITLE_COL + ") LIKE " + arg +
                        " OR LOWER(" +Constants.NOTE_TEXT_COL + ") LIKE " + arg;
            }

            try {
                SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();

                String query = "SELECT * FROM " + Constants.NOTES_TABLE + " WHERE " + where;

                Cursor cursor = db.rawQuery(query, null);

                if(cursor != null && cursor.moveToFirst()){

                    ArrayList<Note> notes = new ArrayList<>(cursor.getCount());
                    do{
                        Note note = new Note();
                        note.setId(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.ID_COL)));
                        note.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.MILLIS_COL)));
                        note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TITLE_COL)));
                        note.setNote(cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TEXT_COL)));
                        note.setFolderId(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.FOLDER_ID_COL)));
                        note.setDeletedState(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.DELETED_COL)));

                        notes.add(note);

                    } while (cursor.moveToNext());

                    cursor.close();
                    db.close();

                    return notes;
                } else
                    return null;
            }catch (SQLiteException e){
                Log.e(TAG, "" + e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Note> notes) {
            super.onPostExecute(notes);
            
            if(listener != null)
                listener.onNotesLoaded(notes);
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
            try {
                SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();

                int rows = db.delete(Constants.NOTES_TABLE, Constants.ID_COL + " = ?",
                        new String[]{Long.toString(noteId)});

                db.close();

                return rows;
            }catch (SQLiteException e){
                Log.e(TAG, "" + e);
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

    private class RemoveAllNotesFromTrash extends AsyncTask<Void, Void, Integer> {
        private OnItemRemoveListener listener;

        public RemoveAllNotesFromTrash(OnItemRemoveListener listener) {
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                SQLiteDatabase db = DatabaseHelper.this.getWritableDatabase();

                int rows = db.delete(Constants.NOTES_TABLE, Constants.DELETED_COL + " = ?", new String[]{Integer.toString(Constants.TRUE)});

                db.close();

                return rows;
            }catch (SQLiteException e){
                Log.e(TAG, "" + e);
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

    private class RestoreAllNotesFromTrash extends AsyncTask<Void, Void, ArrayList<Folder>> {
        private OnFoldersLoadListener listener;

        public RestoreAllNotesFromTrash(OnFoldersLoadListener listener) {
            this.listener = listener;
        }

        @Override
        protected ArrayList<Folder> doInBackground(Void... params) {
            try {
                SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();

                String selectQuery ="SELECT f." + Constants.ID_COL + ", f." + Constants.FOLDER_NAME_COL
                        + ", f." + Constants.FOLDER_ICON_COL
                        + ", COUNT(n." + Constants.DELETED_COL + ") AS " + Constants.NOTES_COUNT_COL
                        + " FROM " + Constants.FOLDER_TABLE + " f LEFT JOIN "
                        + Constants.NOTES_TABLE + " n ON f." + Constants.ID_COL + " = n."
                        + Constants.FOLDER_ID_COL + " AND n." + Constants.DELETED_COL + " = " + Constants.TRUE
                        +  " GROUP BY f." + Constants.ID_COL;

                Cursor cursor = db.rawQuery(selectQuery, null);

                if(cursor != null && cursor.moveToFirst()){

                    ArrayList<Folder> folders = new ArrayList<>(cursor.getCount());
                    do{
                        Folder folder = new Folder();
                        folder.setId(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.ID_COL)));
                        folder.setName(cursor.getString(cursor.getColumnIndexOrThrow(Constants.FOLDER_NAME_COL)));
                        folder.setIcon(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.FOLDER_ICON_COL)));
                        folder.setCount(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.NOTES_COUNT_COL)));

                        folders.add(folder);

                    } while (cursor.moveToNext());

                    cursor.close();

                    ContentValues contentValues = new ContentValues();
                    contentValues.put(Constants.DELETED_COL, Constants.FALSE);
                    int rows = db.update(Constants.NOTES_TABLE, contentValues, Constants.DELETED_COL + " = ?", new String[]{Integer.toString(Constants.TRUE)});

                    db.close();

                    return rows > 0 ? folders : null;
                } else
                    return null;
            }catch (SQLiteException e){
                Log.e(TAG, "" + e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Folder> aFolders) {
            super.onPostExecute(aFolders);

            if(listener != null)
                listener.onFoldersLoaded(aFolders);
        }
    }

    private class RemoveAllNotesFromFolder extends AsyncTask<Void, Void, Integer> {
        private long folderId;
        private OnItemRemoveListener listener;

        public RemoveAllNotesFromFolder(long folderId, OnItemRemoveListener listener) {
            this.folderId = folderId;
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                SQLiteDatabase db = DatabaseHelper.this.getWritableDatabase();

                int rows = db.delete(Constants.NOTES_TABLE, Constants.FOLDER_ID_COL + " = ? AND " +
                        Constants.DELETED_COL + " = ?", new String[]{Long.toString(folderId), Integer.toString(Constants.FALSE)});

                ContentValues contentValues = new ContentValues();
                contentValues.put(Constants.FOLDER_ID_COL, Utils.getMyNotesNavId(context));
                db.update(Constants.NOTES_TABLE, contentValues, Constants.FOLDER_ID_COL + " = ? AND " +
                        Constants.DELETED_COL + " = ?", new String[]{Long.toString(folderId), Integer.toString(Constants.TRUE)});

                db.close();

                return rows;
            }catch (SQLiteException e){
                Log.e(TAG, "" + e);
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
            try {
                SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();

                ContentValues values = new ContentValues();
                values.put(Constants.FOLDER_NAME_COL, folder.getName());
                values.put(Constants.FOLDER_ICON_COL, folder.getIcon());

                long folderId = db.insert(Constants.FOLDER_TABLE, null, values);

                db.close();

                return folderId;
            }catch (SQLiteException e){
                Log.e(TAG, "" + e);
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
        private OnItemUpdateListener listener;

        public UpdateFolder(Folder folder, OnItemUpdateListener listener) {
            this.folder = folder;
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();

                ContentValues values = new ContentValues();
                values.put(Constants.FOLDER_NAME_COL, folder.getName());

                int rows = db.update(Constants.FOLDER_TABLE, values, Constants.ID_COL + " = ?",
                        new String[]{Long.toString(folder.getId())});

                db.close();

                return rows;
            }catch (SQLiteException e){
                Log.e(TAG, "" + e);
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
            try {
                SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();

                /*String selectQuery = "SELECT * FROM " + Constants.FOLDER_TABLE + " WHERE " +
                        Constants.ID_COL + " = " + folderId;*/

                String selectQuery ="SELECT f." + Constants.ID_COL + ", f." + Constants.FOLDER_NAME_COL
                        + ", f." + Constants.FOLDER_ICON_COL
                        + ", COUNT(n." + Constants.DELETED_COL + ") AS " + Constants.NOTES_COUNT_COL
                        + " FROM " + Constants.FOLDER_TABLE + " f LEFT JOIN "
                        + Constants.NOTES_TABLE + " n ON f." + Constants.ID_COL + " = n."
                        + Constants.FOLDER_ID_COL + " AND n." + Constants.DELETED_COL + " = " + Constants.FALSE
                        + " AND n." + Constants.FOLDER_ID_COL + " = " + folderId;

                Cursor cursor = db.rawQuery(selectQuery, null);

                if(cursor != null){
                    cursor.moveToFirst();

                    Folder folder = new Folder();
                    folder.setId(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.ID_COL)));
                    folder.setName(cursor.getString(cursor.getColumnIndexOrThrow(Constants.FOLDER_NAME_COL)));
                    folder.setIcon(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.FOLDER_ICON_COL)));
                    folder.setCount(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.NOTES_COUNT_COL)));

                    cursor.close();
                    db.close();

                    return folder;
                } else
                    return null;
            }catch (SQLiteException e){
                Log.e(TAG, "" + e);
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

    private class GetFolders extends AsyncTask<Void, Void, ArrayList<Folder>> {
        private OnFoldersLoadListener listener;

        public GetFolders(OnFoldersLoadListener listener) {
            this.listener = listener;
        }

        @Override
        protected ArrayList<Folder> doInBackground(Void... params) {
            try {
                SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();
                Log.e("Helper", "get Readable - Folders");
                String selectQuery ="SELECT f." + Constants.ID_COL + ", f." + Constants.FOLDER_NAME_COL
                        + ", f." + Constants.FOLDER_ICON_COL
                        + ", COUNT(n." + Constants.DELETED_COL + ") AS " + Constants.NOTES_COUNT_COL
                        + " FROM " + Constants.FOLDER_TABLE + " f LEFT JOIN "
                        + Constants.NOTES_TABLE + " n ON f." + Constants.ID_COL + " = n."
                        + Constants.FOLDER_ID_COL + " AND n." + Constants.DELETED_COL + " = " + Constants.FALSE
                        +  " GROUP BY f." + Constants.ID_COL;

                Cursor cursor = db.rawQuery(selectQuery, null);

                int deletedCount = (int) DatabaseUtils.queryNumEntries(db, Constants.NOTES_TABLE, Constants.DELETED_COL + " = ?", new String[]{Integer.toString(Constants.TRUE)});
                long deletedId = Utils.getTrashNavId(context);

                if(cursor != null && cursor.moveToFirst()){

                    ArrayList<Folder> folders = new ArrayList<>(cursor.getCount());
                    do{
                        Folder folder = new Folder();
                        folder.setId(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.ID_COL)));
                        folder.setName(cursor.getString(cursor.getColumnIndexOrThrow(Constants.FOLDER_NAME_COL)));
                        folder.setIcon(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.FOLDER_ICON_COL)));
                        if(folder.getId() == deletedId)
                            folder.setCount(deletedCount);
                        else
                            folder.setCount(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.NOTES_COUNT_COL)));
                        folders.add(folder);

                    } while (cursor.moveToNext());

                    cursor.close();
                    db.close();

                    return folders;
                } else
                    return null;
            }catch (SQLiteException e){
                Log.e(TAG, "" + e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Folder> aFolders) {
            super.onPostExecute(aFolders);

            if(listener != null)
                listener.onFoldersLoaded(aFolders);
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
            try {
                SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();

                int rows = db.delete(Constants.FOLDER_TABLE, Constants.ID_COL + " = ?",
                        new String[]{Long.toString(folderId)});

                db.close();

                return rows;
            }catch (SQLiteException e){
                Log.e(TAG, "" + e);
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
            try {
                SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();

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
                Log.e(TAG, "" + e);
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
            try {
                SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();

                ContentValues values = new ContentValues();
                values.put(Constants.CURRENT_WIDGET_MODE_COL, widget.getMode());
                values.put(Constants.CURRENT_THEME_MODE_COL, widget.getTheme());
                values.put(Constants.CURRENT_TEXT_SIZE_COL, widget.getTextSize());

                int rows = db.update(Constants.WIDGETS_TABLE, values, Constants.ID_COL + " = ?",
                        new String[]{Long.toString(widgetId)});

                db.close();

                return rows;
            }catch (SQLiteException e){
                Log.e(TAG, "" + e);
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
        private long itemId;
        private int widgetId;
        private OnWidgetLoadListener listener;
        private boolean lookAtItemId;

        public GetWidget(long itemId, OnWidgetLoadListener listener) {
            this.itemId = itemId;
            this.listener = listener;
            this.lookAtItemId = true;
        }

        public GetWidget(int widgetId, OnWidgetLoadListener listener) {
            this.widgetId = widgetId;
            this.listener = listener;
            this.lookAtItemId = false;
        }

        @Override
        protected Widget doInBackground(Void... params) {
            try {
                SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();
                String selectQuery;

                if(lookAtItemId)
                    selectQuery = "SELECT * FROM " + Constants.WIDGETS_TABLE + " WHERE " +
                        Constants.ID_COL + " = " + itemId;
                else
                    selectQuery = "SELECT * FROM " + Constants.WIDGETS_TABLE + " WHERE " +
                            Constants.WIDGET_ID_COL + " = " + widgetId;

                Cursor cursor = db.rawQuery(selectQuery, null);

                if(cursor != null){
                    cursor.moveToFirst();

                    Widget widget = new Widget();
                    widget.setId(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.ID_COL)));
                    widget.setWidgetId(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.WIDGET_ID_COL)));
                    widget.setNoteId(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.CONNECTED_NOTE_ID_COL)));
                    widget.setMode(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.CURRENT_WIDGET_MODE_COL)));
                    widget.setTheme(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.CURRENT_THEME_MODE_COL)));
                    widget.setTextSize(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.CURRENT_TEXT_SIZE_COL)));

                    cursor.close();
                    db.close();

                    return widget;
                } else
                    return null;
            }catch (SQLiteException e){
                Log.e(TAG, "" + e);
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

    private class GetWidgetsWithNote extends AsyncTask<Void, Void, ArrayList<Widget>> {
        private long noteId;
        private OnWidgetsLoadListener listener;

        public GetWidgetsWithNote(long noteId, OnWidgetsLoadListener listener) {
            this.noteId = noteId;
            this.listener = listener;
        }

        @Override
        protected ArrayList<Widget> doInBackground(Void... params) {
            try {
                SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();

                String selectQuery = "SELECT * FROM " + Constants.WIDGETS_TABLE + " WHERE " +
                            Constants.CONNECTED_NOTE_ID_COL + " = " + noteId;

                Cursor cursor = db.rawQuery(selectQuery, null);

                if(cursor != null && cursor.moveToFirst()){

                    ArrayList<Widget> widgets = new ArrayList<>(cursor.getCount());
                    do{
                        Widget widget = new Widget();
                        widget.setId(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.ID_COL)));
                        widget.setWidgetId(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.WIDGET_ID_COL)));
                        widget.setNoteId(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.CONNECTED_NOTE_ID_COL)));
                        widget.setMode(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.CURRENT_WIDGET_MODE_COL)));
                        widget.setTheme(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.CURRENT_THEME_MODE_COL)));
                        widget.setTextSize(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.CURRENT_TEXT_SIZE_COL)));

                        widgets.add(widget);

                    } while (cursor.moveToNext());

                    cursor.close();
                    db.close();

                    return widgets;
                } else
                    return null;
            }catch (SQLiteException e){
                Log.e(TAG, "" + e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Widget> widgets) {
            super.onPostExecute(widgets);

            if(listener != null)
                listener.onWidgetsLoaded(widgets);
        }
    }

    private class RemoveWidget extends AsyncTask<Void, Void, Integer> {
        private int widgetId;
        private OnItemRemoveListener listener;

        public RemoveWidget(int widgetId, OnItemRemoveListener listener) {
            this.widgetId = widgetId;
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();

                int rows = db.delete(Constants.WIDGETS_TABLE, Constants.WIDGET_ID_COL + " = ?",
                        new String[]{Long.toString(widgetId)});

                db.close();

                return rows;
            }catch (SQLiteException e){
                Log.e(TAG, "" + e);
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

    private class ClearWidgetsTable extends AsyncTask<Void,Void,Boolean>
    {
        private final OnFinishListener listener;

        private ClearWidgetsTable(OnFinishListener listener) {
            this.listener = listener;
        }
        @Override
        protected Boolean doInBackground(Void[] p1)
        {
            try {
                SQLiteDatabase db = DatabaseHelper.this.getReadableDatabase();
                db.execSQL("delete from " + Constants.WIDGETS_TABLE);

                db.close();

                return true;
            }catch (SQLiteException e){
                Log.e(TAG, "" + e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            super.onPostExecute(result);

            if(listener!= null)
                listener.onFinished(result);
        }
    }
}