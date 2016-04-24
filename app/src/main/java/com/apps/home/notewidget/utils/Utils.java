package com.apps.home.notewidget.utils;

import android.app.Activity;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.Toast;

import com.apps.home.notewidget.MainActivity;
import com.apps.home.notewidget.R;
import com.apps.home.notewidget.customviews.RobotoEditText;
import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.widget.WidgetProvider;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class Utils {
    private static final String TAG = "Utils";
    private static Toast toast;
    private static int[][][] widgetLayouts;
    private static SQLiteDatabase db;
    private static int idArray[];
    private static SharedPreferences preferences;
    private static int myNotesNavId = -1;
    private static int trashNavId = -1;

    public static void showToast(Context context, String message){
        if(toast!=null)
            toast.cancel();
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static int getLayoutFile(Context context, int themeMode, int widgetMode){
        if(widgetLayouts==null) {
            widgetLayouts = new int[3][2][2];
            widgetLayouts[0][0][0] = R.layout.appwidget_title_miui_light;
            widgetLayouts[0][0][1] = R.layout.appwidget_config_miui_light;
            widgetLayouts[0][1][0] = R.layout.appwidget_title_miui_dark;
            widgetLayouts[0][1][1] = R.layout.appwidget_config_miui_dark;
            widgetLayouts[1][0][0] = R.layout.appwidget_title_lollipop_light;
            widgetLayouts[1][0][1] = R.layout.appwidget_config_lollipop_light;
            widgetLayouts[1][1][0] = R.layout.appwidget_title_lollipop_dark;
            widgetLayouts[1][1][1] = R.layout.appwidget_config_lollipop_dark;
            widgetLayouts[2][0][0] = R.layout.appwidget_title_simple_light;
            widgetLayouts[2][0][1] = R.layout.appwidget_config_simple_light;
            widgetLayouts[2][1][0] = R.layout.appwidget_title_simple_dark;
            widgetLayouts[2][1][1] = R.layout.appwidget_config_simple_dark;
        }

        return widgetLayouts[context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).
                getInt(Constants.WIDGET_THEME_KEY, 0)][themeMode][widgetMode];
    }

    public static int switchWidgetMode(int currentMode){
        return currentMode == Constants.WIDGET_MODE_TITLE? Constants.WIDGET_MODE_CONFIG : Constants.WIDGET_MODE_TITLE;
    }

    public static int switchThemeMode(int currentMode){
        return currentMode == Constants.WIDGET_THEME_LIGHT? Constants.WIDGET_THEME_DARK : Constants.WIDGET_THEME_LIGHT;
    }

    public static void showOrHideKeyboard(Window window, boolean show){
        if(show)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        else
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public static SQLiteDatabase getDb(Context context){
        try {
            if (db == null || !db.isOpen()) {
                SQLiteOpenHelper helper = DatabaseHelper.getInstance(context);
                db = helper.getWritableDatabase();
            }
            return db;
        }catch (SQLiteException e){
            showToast(context, context.getString(R.string.database_unavailable));
            return null;
        }
    }

    public static void closeDb(){
        if(db != null && db.isOpen())
            db.close();
    }

    public static File getBackupDir(Context context){
        return new File(Environment.getExternalStorageDirectory() + "/backup/" + context.getPackageName());
    }

    public static File getPrefsFile(Context context){
        return new File(context.getApplicationInfo().dataDir +"/shared_prefs/", Constants.PREFS_NAME + ".xml");
    }

    public static File getDbFile(Context context){
        return context.getDatabasePath(Constants.DB_NAME);
    }

    public static Dialog getMultilevelNoteManualDialog(final Context context){
        LayoutInflater inflater = ((AppCompatActivity)context).getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_multilevel_note_manual, null);
        final CheckBox checkBox = (CheckBox) layout.findViewById(R.id.checkBox);
        return new AlertDialog.Builder(context).setTitle(context.getString(R.string.tip)).setView(layout).setCancelable(false).
                setPositiveButton(context.getString(R.string.i_have_got_it), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (checkBox.isChecked()) {
                            if (preferences == null)
                                preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
                            preferences.edit().putBoolean(Constants.SKIP_MULTILEVEL_NOTE_MANUAL_DIALOG_KEY, true).apply();
                        }
                    }
                }).create();
    }

    public static Dialog getConfirmationDialog(final Context context, String title, DialogInterface.OnClickListener action){
        return new AlertDialog.Builder(context).setMessage(title)
                .setPositiveButton(context.getString(R.string.confirm), action)
                .setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Utils.showToast(context, context.getString(R.string.canceled));
                    }
                }).create();
    }

    public interface OnNameSet {
        void onNameSet(String name);
    }

    public static Dialog getEdiTextDialog(final Context context, final String text, String title,
                                       final OnNameSet action, boolean hideContent){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = ((AppCompatActivity)context).getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_roboto_edit_text, null);
        final RobotoEditText titleEditText = (RobotoEditText) layout.findViewById(R.id.titleEditText);
        titleEditText.setText(text);
        if(hideContent)
            titleEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        titleEditText.setSelection(0, titleEditText.length());
        AlertDialog dialog = builder.setTitle(title).setView(layout)
                .setPositiveButton(context.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showOrHideKeyboard(((AppCompatActivity) context).getWindow(), false);
                        if(action != null)
                            action.onNameSet(titleEditText.getText().toString().trim());
                    }
                })
                .setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showToast(context, context.getString(R.string.canceled));
                        showOrHideKeyboard(((AppCompatActivity) context).getWindow(), false);
                    }
                }).create();
        showOrHideKeyboard(dialog.getWindow(), true);

        return dialog;
    }

    public static Dialog getNameDialog(final Context context, final String text, final String title,
                                       final OnNameSet action){

        return getEdiTextDialog(context, text, title, action, false);
    }

    public static Dialog getPasswordDialog(final Context context,
                                           final OnNameSet action){

        return getEdiTextDialog(context, "", context.getString(R.string.put_password), action, true);
    }

    public static Dialog getFolderListDialog(Context context, Menu menu, int[] exclusions, String title,
                                             DialogInterface.OnClickListener action){
        int menuSize = menu.size();
        int size = menuSize - (exclusions == null? 0 : exclusions.length) - 2;
        CharSequence[] nameArray = new CharSequence[size];
        idArray = new int[size];
        int j = 0;
        for(int i = 0; i<menuSize; i++){
            Log.e(TAG, "loop " + i);

            int id = menu.getItem(i).getItemId();
            boolean excluded = false;

            if(exclusions != null)
            for(int excludedId : exclusions){
                if(id == excludedId){
                    excluded = true;
                    break;
                }
            }

            if(!excluded && id != R.id.nav_settings && id != R.id.nav_about)
            {
                nameArray[j] = menu.getItem(i).getTitle().toString();
                idArray[j] = menu.getItem(i).getItemId();
                j++;
            }
        }
        Log.e(TAG, "after loop");
        if(nameArray.length > 0)
            return new AlertDialog.Builder(context).setTitle(title)
                    .setItems(nameArray, action).create();
        else{
            showToast(context, context.getString(R.string.you_have_only_one_folder));
            return null;
        }
    }



    public static Dialog getAllFolderListDialog(Context context, String title,
                                             DialogInterface.OnClickListener action){
        CharSequence[] folders = null;
        try {
            folders = new GetAllFolders(context).execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        if(folders != null && folders.length > 0)
            return new AlertDialog.Builder(context).setTitle(title)
                    .setItems(folders, action).create();
        else{
            showToast(context, "Unable to load folders list. Try again later");
            return null;
        }
    }

    public static String getFolderName(Context context, int id){
        String name = "";
        try {
            name = new GetFolderName(context, id).execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return name;
    }

    public static void removeAllMenuItems(Menu menu){
        Log.e(TAG, " size "+ menu.size());
        while (menu.size()>0)
            menu.removeItem(menu.getItem(0).getItemId());
    }

    public static int getFolderId(int which){
        return idArray[which];
    }

    public static void clearWidgetsTable(Context context, FinishListener finishListener){
        new ClearWidgetsTable(context, finishListener).execute();
    }

    public static void updateAllWidgets(Context context){
        WidgetProvider widgetProvider = new WidgetProvider();
        ComponentName componentName = new ComponentName(context, WidgetProvider.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        widgetProvider.onUpdate(context, appWidgetManager, appWidgetManager.getAppWidgetIds(componentName));
    }

    public static String capitalizeFirstLetter(String text){
        if (text.length() <= 1)
            return text.toUpperCase();
        else
            return text.substring(0, 1).toUpperCase() + text.substring(1);

    }

    public static void hideShadowSinceLollipop(Context context){
        if(Build.VERSION.SDK_INT >= 21){
            ((Activity)context).findViewById(R.id.shadowImageView).setVisibility(View.GONE);
        }
    }
    public static void sendShareIntent(Context context, String text, String title) {
        if(text.length()!=0) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            intent.putExtra(Constants.TITLE_KEY, title);
            context.startActivity(Intent.createChooser(intent, "Share via"));
        } else
            Utils.showToast(context, "Note is empty");
    }

    public static int getMyNotesNavId(Context context){
        if(myNotesNavId > 0)
            return myNotesNavId;
        else {
            if(preferences == null)
                preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
            return (myNotesNavId = preferences.getInt(Constants.MY_NOTES_ID_KEY, 1));
        }
    }

    public static int getTrashNavId(Context context){
        if(trashNavId > 0)
            return trashNavId;
        else {
            if(preferences == null)
                preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
            return (trashNavId = preferences.getInt(Constants.TRASH_ID_KEY, 2));
        }
    }

    public static void incrementFolderCount(Menu m, int folderId, int inc){
        MenuItem menuItem = m.findItem(folderId);
        RobotoTextView view = (RobotoTextView) menuItem.getActionView();
        view.setText(Integer.toString(Integer.parseInt(view.getText().toString()) + inc));
    }

    public static void decrementFolderCount(Menu m, int folderId, int dec){
        MenuItem menuItem = m.findItem(folderId);
        RobotoTextView view = (RobotoTextView) menuItem.getActionView();
        view.setText(Integer.toString(Integer.parseInt(view.getText().toString()) - dec));
    }

    public static void setFolderCount(Menu m, int folderId, int count){
        MenuItem menuItem = m.findItem(folderId);
        RobotoTextView view = (RobotoTextView) menuItem.getActionView();
        view.setText(Integer.toString(count));
    }

    public static void updateConnectedWidgets(Context context, long noteId){
        new UpdateConnectedWidgets(context, noteId).execute();
    }

    public static void restoreOrRemoveNoteFromTrash(Context context, long noteId, int action,
                                                    FinishListener finishListener){
        new RestoreOrRemoveNoteFromTrash(context, noteId, action, finishListener).execute();
    }

    public static void loadNote(Context context, long noteId, LoadListener loadListener){
        new LoadNote(context, noteId, loadListener).execute();
    }

    public static void updateFolderId(Context context, Menu menu, long noteId, int newFolderId, int folderId,
                                      FolderIdUpdateListener updateListener){
        new UpdateFolderId(context, menu, noteId, newFolderId, folderId, updateListener).execute();
    }

    public static void moveToTrash(Context context, Menu menu, long noteId, String title, String note, int folderId){
        new MoveToTrash(context, menu, noteId, title, note, folderId).execute();
    }

    public static void moveToTrash(Context context, Menu menu, long noteId, int folderId,
                                   FinishListener finishListener){
        new MoveToTrash(context, menu, noteId, folderId, finishListener).execute();
    }

    public interface FinishListener {
        void onFinished(boolean result);
    }

    public interface LoadListener {
        void onLoad(String[] note);
    }

    public interface FolderIdUpdateListener{
        void onUpdate(int newFolderId);
    }

    public static void updateNote(Context context, long noteId, String title, String note, FinishListener finishListener){
        new PutNoteInTable(context, noteId, title, note, finishListener).execute();
    }

    public static void updateNoteWithEncryption(Context context, long noteId, String title, String note, String password, FinishListener finishListener){
        new PutNoteInTable(context, noteId, title, note, password, finishListener).execute();
    }

    public static void saveNewNote(Context context, long noteId, String title, String note, int folderId,
                                  long creationTimeMillis, FinishListener finishListener){
        new PutNoteInTable(context, noteId, title, note, folderId, creationTimeMillis, finishListener).execute();
    }

    public static void saveNewNoteWithEncryption(Context context, long noteId, String title, String note, int folderId,
                                   long creationTimeMillis, String password, FinishListener finishListener){
        new PutNoteInTable(context, noteId, title, note, folderId, creationTimeMillis, password, finishListener).execute();
    }

    private static class PutNoteInTable extends AsyncTask<Void, Void, Boolean>
    {   private ContentValues contentValues;
        private Context context;
        private int folderId;
        private long noteId;
        private String title;
        private String note;
        private boolean isNewNote;
        private long creationTimeMillis;
        private boolean encrypt;
        private String password;
        private FinishListener finishListener;


        public PutNoteInTable(Context context, long noteId, String title, String note, FinishListener finishListener){
            init(context, noteId, title, note, finishListener);
            isNewNote = false;
            encrypt = false;
        }

        public PutNoteInTable(Context context, long noteId, String title, String note, String password, FinishListener finishListener){
            init(context, noteId, title, note, finishListener);
            this.password = password;
            isNewNote = false;
            encrypt = true;
        }

        public PutNoteInTable(Context context, long noteId, String title, String note, int folderId,
                              long creationTimeMillis, FinishListener finishListener){
            init(context, noteId, title, note, finishListener);
            this.folderId = folderId;
            this.creationTimeMillis = creationTimeMillis;
            isNewNote = true;
            encrypt = false;
        }

        public PutNoteInTable(Context context, long noteId, String title, String note, int folderId,
                              long creationTimeMillis, String password, FinishListener finishListener){
            init(context, noteId, title, note, finishListener);
            this.folderId = folderId;
            this.creationTimeMillis = creationTimeMillis;
            this.password = password;
            isNewNote = true;
            encrypt = true;
        }

        public void init(Context context, long noteId, String title, String note, FinishListener finishListener){
            this.context = context;
            this.noteId = noteId;
            this.title = title;
            this.note = note;
            this.finishListener = finishListener;
        }

        @Override
        protected void onPreExecute()
        {
            contentValues = new ContentValues();
            contentValues.put(Constants.NOTE_TITLE_COL, title);
            contentValues.put(Constants.NOTE_TEXT_COL, note.replace(System.getProperty("line.separator"), "<br/>"));
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... p1)
        {
            SQLiteDatabase db;
            if((db = Utils.getDb(context)) != null) {
                if (isNewNote) {
                    contentValues.put(Constants.MILLIS_COL, creationTimeMillis);
                    contentValues.put(Constants.FOLDER_ID_COL, folderId);
                    contentValues.put(Constants.DELETED_COL, 0);
                    noteId = db.insert(Constants.NOTES_TABLE, null, contentValues);
                    Log.e(TAG, "insert " + contentValues.toString());
                } else {
                    db.update(Constants.NOTES_TABLE, contentValues, Constants.ID_COL + " = ?",
                            new String[]{Long.toString(noteId)});
                    Log.e(TAG, "update " + contentValues.toString());
                }
                return true;
            } else
                return false;

        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            super.onPostExecute(result);
            if(result){
                showToast(context, context.getString(R.string.saved));
                if(isNewNote){
                    incrementFolderCount(((MainActivity) context).getNavigationViewMenu(), folderId, 1);
                } else
                    updateConnectedWidgets(context, noteId);
            }
            if(finishListener != null)
                finishListener.onFinished(result);

        }
    }

    public static class UpdateConnectedWidgets extends AsyncTask<Void, Void, Boolean>
    {
        private int[] widgetIds;
        private Context context;
        private long noteId;

        private UpdateConnectedWidgets(Context context, long noteId ) {
            this.context = context;
            this.noteId = noteId;
        }

        @Override
        protected Boolean doInBackground(Void[] p1)
        {
            if((db = Utils.getDb(context)) != null) {
                Cursor widgetCursor = db.query(Constants.WIDGETS_TABLE, new String[]{Constants.WIDGET_ID_COL},
                        Constants.CONNECTED_NOTE_ID_COL + " = ?", new String[]{Long.toString(noteId)}, null, null, null);
                widgetCursor.moveToFirst();
                widgetIds = new int[widgetCursor.getCount()];
                for (int i = 0; i < widgetCursor.getCount(); i++) {
                    widgetIds[i] = widgetCursor.getInt(0);
                    widgetCursor.moveToNext();
                }
                widgetCursor.close();
                return true;
            } else
                return false;
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            if(result){
                WidgetProvider widgetProvider = new WidgetProvider();
                widgetProvider.onUpdate(context, AppWidgetManager.getInstance(context), widgetIds);
            }
            super.onPostExecute(result);
        }
    }

    private static class RestoreOrRemoveNoteFromTrash extends AsyncTask<Void,Void,Boolean>
    {
        private final FinishListener finishListener;
        int action;
        private Context context;
        int folderId;
        long noteId;
        Menu menu;

        private RestoreOrRemoveNoteFromTrash(Context context, long noteId, int action, FinishListener finishListener) {
            this.context = context;
            this.noteId = noteId;
            this.action = action;
            this.finishListener = finishListener;
        }
        @Override
        protected Boolean doInBackground(Void[] p1)
        {
            if((db = Utils.getDb(context)) != null) {
                menu = ((MainActivity)context).getNavigationViewMenu();
                Utils.decrementFolderCount(menu, Utils.getTrashNavId(context), 1);
                if (action == R.id.action_delete_from_trash) { //remove note
                    db.delete(Constants.NOTES_TABLE, Constants.ID_COL + " = ?", new String[]{Long.toString(noteId)});
                    Log.e(TAG, "delete all");
                    return true;
                } else { //restore note
                    Log.e(TAG, "restore");
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(Constants.DELETED_COL, 0);
                    Cursor cursor = db.query(Constants.NOTES_TABLE, new String[]{Constants.FOLDER_ID_COL},
                            Constants.ID_COL + " = ?", new String[]{Long.toString(noteId)}, null, null, null);
                    cursor.moveToFirst();
                    folderId = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.FOLDER_ID_COL));
                    cursor.close();
                    db.update(Constants.NOTES_TABLE, contentValues, Constants.ID_COL + " = ?", new String[]{Long.toString(noteId)});
                    return true;
                }
            } else
                return false;
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            super.onPostExecute(result);
            if(result){
                if(action == R.id.action_delete_from_trash) {
                    showToast(context, context.getString(R.string.note_was_removed));
                } else {
                    showToast(context, context.getString(R.string.notes_was_restored));
                    updateConnectedWidgets(context, noteId);
                    incrementFolderCount(menu, folderId, 1);
                }
            }
            if(finishListener!= null)
                finishListener.onFinished(result);
        }
    }

    private static class ClearWidgetsTable extends AsyncTask<Void,Void,Boolean>
    {
        private final FinishListener finishListener;
        private Context context;

        private ClearWidgetsTable(Context context, FinishListener finishListener) {
            this.context = context;
            this.finishListener = finishListener;
        }
        @Override
        protected Boolean doInBackground(Void[] p1)
        {
            if((db = Utils.getDb(context)) != null) {
                db.execSQL("delete from " + Constants.WIDGETS_TABLE);
                return true;
            } else
                return false;
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            super.onPostExecute(result);

            if(finishListener!= null)
                finishListener.onFinished(result);
        }
    }

    private static class LoadNote extends AsyncTask<Void, Void, String[]> {
        private Context context;
        private Cursor cursor;
        private long noteId;
        private LoadListener loadListener;

        private LoadNote(Context context, long noteId, LoadListener loadListener) {
            this.context = context;
            this.noteId = noteId;
            this.loadListener = loadListener;
        }

        @Override
        protected String[] doInBackground(Void... params) {
            if((db = Utils.getDb(context)) != null) {
                cursor = db.query(Constants.NOTES_TABLE, new String[]{Constants.MILLIS_COL,
                                Constants.NOTE_TITLE_COL, Constants.NOTE_TEXT_COL},
                        Constants.ID_COL + " = ?", new String[]{Long.toString(noteId)}, null, null, null);
                if(cursor.getCount()>0) {
                    cursor.moveToFirst();
                    return new String[]{cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TITLE_COL)),
                            cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TEXT_COL)),
                            Long.toString(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.MILLIS_COL)))};
                } else
                    return null;
            } else
                return null;

        }

        @Override
        protected void onPostExecute(String[] note) {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
            loadListener.onLoad(note);
        }
    }

    private static class UpdateFolderId extends AsyncTask<Void,Void,Boolean>
    {
        private Context context;
        private Menu menu;
        private int newFolderId;
        private int folderId;
        private long noteId;
        private FolderIdUpdateListener updateListener;

        private UpdateFolderId(Context context, Menu menu, long noteId, int newFolderId, int folderId,
                               FolderIdUpdateListener updateListener) {
            this.context = context;
            this.menu = menu;
            this.noteId = noteId;
            this.folderId = folderId;
            this.newFolderId = newFolderId;
            this.updateListener = updateListener;
        }

        @Override
        protected Boolean doInBackground(Void[] voids)
        {
            SQLiteDatabase db;

            if((db = Utils.getDb(context)) != null) {
                //Update folder id for current note
                ContentValues contentValues = new ContentValues();
                contentValues.put(Constants.FOLDER_ID_COL, newFolderId);
                db.update(Constants.NOTES_TABLE, contentValues, Constants.ID_COL + " = ?",
                        new String[]{Long.toString(noteId)});
                return true;

            } else
                return false;
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            super.onPostExecute(result);
            if(result){
                Utils.showToast(context, context.getString(R.string.note_has_been_moved));

                Utils.incrementFolderCount(menu, newFolderId, 1);
                Utils.decrementFolderCount(menu, folderId, 1);
                if(updateListener != null)
                    updateListener.onUpdate(newFolderId);
            }
        }
    }

    private static class MoveToTrash extends AsyncTask<Void,Void,Boolean>
    {
        private Context context;
        private long noteId;
        private String title;
        private String note;
        private int folderId;
        private Menu menu;
        private boolean wasNoteChanged;
        private FinishListener finishListener;

        private MoveToTrash(Context context, Menu menu, long noteId, String title, String note,
                            int folderId) {
            this.context = context;
            this.noteId = noteId;
            this.title = title;
            this.note = note;
            this.folderId = folderId;
            this.menu = menu;
            this.wasNoteChanged = true;
            this.finishListener = null;
        }
        private MoveToTrash(Context context, Menu menu, long noteId, int folderId,
                            FinishListener finishListener) {
            this.context = context;
            this.noteId = noteId;
            this.folderId = folderId;
            this.menu = menu;
            this.wasNoteChanged = false;
            this.finishListener = finishListener;
        }

        @Override
        protected Boolean doInBackground(Void[] p1)
        {
            if((db = Utils.getDb(context)) != null) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(Constants.DELETED_COL, 1);
                if(wasNoteChanged) {
                    contentValues.put(Constants.NOTE_TITLE_COL, title);
                    contentValues.put(Constants.NOTE_TEXT_COL, note);
                }
                db.update(Constants.NOTES_TABLE, contentValues, Constants.ID_COL + " = ?", new String[]{Long.toString(noteId)});
                return true;
            } else
                return false;
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            if(result){
                showToast(context, context.getString(R.string.note_moved_to_trash));
                updateConnectedWidgets(context, noteId);
                incrementFolderCount(menu, Utils.getTrashNavId(context), 1);
                decrementFolderCount(menu, folderId, 1);
            }
            if(finishListener != null)
                finishListener.onFinished(result);
            super.onPostExecute(result);
        }
    }

    private static class GetAllFolders extends AsyncTask<Void, Void, CharSequence[]>{
        private Context context;

        private GetAllFolders(Context context){
            this.context = context;
        }

        @Override
        protected CharSequence[] doInBackground(Void... params) {

            if((db = Utils.getDb(context)) != null) {
                Cursor cursor = db.query(Constants.FOLDER_TABLE, new String[]{Constants.ID_COL,
                Constants.FOLDER_NAME_COL}, null, null, null, null, null);

                if(cursor.getCount()>0){
                    int cursorCount = cursor.getCount();
                    cursor.moveToFirst();
                    CharSequence[] foldersNames = new CharSequence[cursorCount];
                    idArray = new int[cursorCount];
                    for (int i = 0; i<cursorCount; i++){

                        idArray[i] = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.ID_COL));
                        foldersNames[i] = cursor.getString(cursor.getColumnIndexOrThrow(Constants.FOLDER_NAME_COL));
                        cursor.moveToNext();
                    }
                    cursor.close();
                    return foldersNames;
                }
                return null;
            } else
                return null;
        }
    }

    private static class GetFolderName extends AsyncTask<Void, Void, String>{
        private Context context;
        private int id;

        private GetFolderName(Context context, int id){
            this.context = context;
            this.id = id;
        }

        @Override
        protected String doInBackground(Void... params) {
            if((db = Utils.getDb(context)) != null) {

                Cursor cursor = db.query(Constants.FOLDER_TABLE, new String[]{Constants.FOLDER_NAME_COL},
                        Constants.ID_COL + " = ?", new String[]{Integer.toString(id)}, null, null, null);
                String name = "";
                if(cursor.getCount()>0) {
                    cursor.moveToFirst();
                    name = cursor.getString(cursor.getColumnIndexOrThrow(Constants.FOLDER_NAME_COL));
                }
                cursor.close();
                return name;
            } else
                return null;
        }
    }
}
