package com.apps.home.notewidget.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.customviews.RobotoEditText;
import com.apps.home.notewidget.widget.WidgetProvider;

public class Utils {
    private static final String TAG = "Utils";
    private static Toast toast;
    private static int[][][] widgetLayouts;
    private static SQLiteDatabase db;
    private static int idArray[];

    public static void showToast(Context context, String message){
        if(toast!=null)
            toast.cancel();
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static int getLayoutFile(Context context, int themeMode, int widgetMode){
        if(widgetLayouts==null) {
            widgetLayouts = new int[3][2][2];
            widgetLayouts[0][0][0] = R.layout.appwidget_title_lollipop_light;
            widgetLayouts[0][0][1] = R.layout.appwidget_config_lollipop_light;
            widgetLayouts[0][1][0] = R.layout.appwidget_title_lollipop_dark;
            widgetLayouts[0][1][1] = R.layout.appwidget_config_lollipop_dark;
            widgetLayouts[1][0][0] = R.layout.appwidget_title_miui_light;
            widgetLayouts[1][0][1] = R.layout.appwidget_config_miui_light;
            widgetLayouts[1][1][0] = R.layout.appwidget_title_miui_dark;
            widgetLayouts[1][1][1] = R.layout.appwidget_config_miui_dark;
            widgetLayouts[2][0][0] = R.layout.appwidget_title_simple_light;
            widgetLayouts[2][0][1] = R.layout.appwidget_config_simple_light;
            widgetLayouts[2][1][0] = R.layout.appwidget_title_simple_dark;
            widgetLayouts[2][1][1] = R.layout.appwidget_config_simple_dark;
        }

        return widgetLayouts[context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).
                getInt(Constants.CURRENT_WIDGET_THEME_KEY, 0)][themeMode][widgetMode];
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
        return new AlertDialog.Builder(context).setTitle("Choose new folder")
                .setItems(nameArray, action).create();
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

}
