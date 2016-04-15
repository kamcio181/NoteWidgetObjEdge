package com.apps.home.notewidget.utils;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.apps.home.notewidget.MainActivity;
import com.apps.home.notewidget.R;
import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.widget.WidgetProvider;

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

    public static SQLiteDatabase getDb(Context context){
        try {
            if (db == null || !db.isOpen()) {
                SQLiteOpenHelper helper = DatabaseHelper.getInstance(context);
                db = helper.getWritableDatabase();
            }
            return db;
        }catch (SQLiteException e){
            Toast.makeText(context, "Database unavailable", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    public static void closeDb(){
        if(db != null && db.isOpen())
            db.close();
    }

    public static Dialog getConfirmationDialog(final Context context, String title, DialogInterface.OnClickListener action){
        return new AlertDialog.Builder(context).setMessage(title)
                .setPositiveButton("Confirm", action)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Utils.showToast(context, "Canceled");
                    }
                }).create();
    }

    public static Dialog getFolderListDialog(Context context, Menu menu, int folderId, int trashNavId,
                                             DialogInterface.OnClickListener action){
        int size = menu.size()-4;
        CharSequence[] nameArray = new CharSequence[size];
        idArray = new int[size];
        int j = 0;
        for(int i = 0; i<size+4; i++){
            Log.e(TAG, "loop " + i);
            int id = menu.getItem(i).getItemId();
            if(id != folderId && id != trashNavId && id != R.id.nav_settings && id != R.id.nav_about )
            {
                nameArray[j] = menu.getItem(i).getTitle().toString();
                idArray[j] = menu.getItem(i).getItemId();
                j++;
            }
        }
        Log.e(TAG, "after loop");
        if(nameArray.length > 0)
            return new AlertDialog.Builder(context).setTitle("Choose new folder")
                    .setItems(nameArray, action).create();
        else
            return null;
    }

    public static void removeAllMenuItems(Menu menu){
        Log.e(TAG, " size "+ menu.size());
        while (menu.size()>0)
            menu.removeItem(menu.getItem(0).getItemId());
    }

    public static int getFolderId(int which){
        return idArray[which];
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

    public interface FinishListener {
        void onFinished(int task , boolean result);
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
                    Utils.showToast(context, "Note was removed");
                    updateConnectedWidgets(context, noteId);
                } else {
                    Utils.showToast(context, "Notes was restored");
                    Utils.incrementFolderCount(menu, folderId, 1);
                }
            }
            if(finishListener!= null)
                finishListener.onFinished(Constants.RESTORE_OR_REMOVE_NOTE_FROM_TRASH, result);
        }
    }
}
