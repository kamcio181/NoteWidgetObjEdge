package com.apps.home.notewidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.SimpleCursorAdapter;

/**
 * Created by k.kaszubski on 1/20/16.
 */
public class WidgetProvider extends AppWidgetProvider {
    public static final String INCREASE_TEXT_SIZE = "android.appwidget.action.INCREASE_TEXT_SIZE";
    public static final String DECREASE_TEXT_SIZE = "android.appwidget.action.DECREASE_TEXT_SIZE";
    public static final String CHANGE_WIDGET_MODE = "android.appwidget.action.CHANGE_WIDGET_MODE";
    public static String ACTION_WIDGET_CONFIGURE = "ConfigureWidget";

    private SQLiteDatabase db;
    private Cursor configCursor;
    private Cursor noteCursor;

    private SharedPreferences preferences;
    private boolean isConfugured = false;

    private int currentSize = 18;
    private int currentMode = Constants.WIDGET_TITLE_MODE;

    @Override
    public void onReceive(Context context, Intent intent) {

        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        switch(intent.getAction()){
            case INCREASE_TEXT_SIZE:
                getCursors(context, appWidgetId);

                currentSize = configCursor.getInt(configCursor.getColumnIndexOrThrow(Constants.CURRENT_TEXT_SIZE))+1;
                putInConfigTable(context, Constants.CURRENT_TEXT_SIZE, currentSize, appWidgetId);

                Utils.showToast(context, "Text size: " + currentSize);

                updateNote(context, appWidgetId);

                break;
            case DECREASE_TEXT_SIZE:
                getCursors(context, appWidgetId);

                currentSize = configCursor.getInt(configCursor.getColumnIndexOrThrow(Constants.CURRENT_TEXT_SIZE))-1;
                putInConfigTable(context, Constants.CURRENT_TEXT_SIZE, currentSize, appWidgetId);

                Utils.showToast(context, "Text size: " + currentSize);

                updateNote(context, appWidgetId);

                break;
            case CHANGE_WIDGET_MODE:
                getCursors(context, appWidgetId);

                currentMode = configCursor.getInt(configCursor.getColumnIndexOrThrow(Constants.CURRENT_MODE));

                currentMode = currentMode == Constants.WIDGET_TITLE_MODE? Constants.WIDGET_CONFIG_MODE : Constants.WIDGET_TITLE_MODE;
                putInConfigTable(context, Constants.CURRENT_MODE, currentMode, appWidgetId);

                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

                RemoteViews views = updateWidgetListView(context, appWidgetId);
                appWidgetManager.updateAppWidget(appWidgetId, views);
                break;
        }
        super.onReceive(context, intent);
    }

    private void updateNote(Context context, int appWidgetId){
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        mgr.notifyAppWidgetViewDataChanged(appWidgetId, R.id.noteListView);
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.e("widget", "onUpdate");
        for (int appWidgetId : appWidgetIds) {
            if(isConfigured(context, appWidgetId)){

            RemoteViews views = updateWidgetListView(context, appWidgetId);

            appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);

        removeFromConfigTable(context, appWidgetIds);
    }

    private boolean isConfigured(Context context, int widgetId){
        if(preferences==null)
            preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(widgetId+Constants.CONFIGURED_KEY, false);
    }

    private void getCursors(Context context, int widgetId){ //TODO pobieranie kursorów i wykorzystanie ich do sterowania widgetamia
        if(db==null) {
            SQLiteOpenHelper helper = new DatabaseHelper(context);
            db = helper.getWritableDatabase();
        }

        configCursor = db.query(Constants.WIDGETS_TABLE, new String[]{
                        Constants.CONNECTED_NOTE_ID, Constants.CURRENT_MODE,
                        Constants.CURRENT_THEME, Constants.CURRENT_TEXT_SIZE},
                Constants.WIDGET_ID + " = ?", new String[]{Integer.toString(widgetId)},
                null, null, null);
        configCursor.moveToFirst();

        noteCursor = db.query(Constants.NOTES_TABLE, new String[]{Constants.NOTE_TITLE_COL, Constants.NOTE_TEXT_COL},
                Constants.ID_COL + " = ?", new String[]{Integer.toString(
                        configCursor.getInt(configCursor.getColumnIndexOrThrow(
                                Constants.CONNECTED_NOTE_ID)))}, null, null, null );
        noteCursor.moveToFirst();
    }

    private void putInConfigTable(Context context, String column, int value, int widgetId){
        if(db==null) {
            SQLiteOpenHelper helper = new DatabaseHelper(context);
            db = helper.getWritableDatabase();
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(column, value);
        db.update(Constants.WIDGETS_TABLE, contentValues, Constants.WIDGET_ID + " = ?",
                new String[]{Integer.toString(widgetId)});
    }

    private void removeFromConfigTable(Context context, int[] widgetIds){
        if(db==null) {
            SQLiteOpenHelper helper = new DatabaseHelper(context);
            db = helper.getWritableDatabase();
        }
        if(preferences==null)
            preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        
        String[] args = new String[widgetIds.length];
        for(int i =0; i < widgetIds.length; i++) {
            args[i] = Integer.toString(widgetIds[i]);
            preferences.edit().remove(widgetIds[i]+Constants.CONFIGURED_KEY).commit();
        }
        db.delete(Constants.WIDGETS_TABLE, Constants.WIDGET_ID + " = ?", args);


    }

    private PendingIntent getPendingIntentWithAction(Context context, Intent intent, int appWidgetId, String action){
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private RemoteViews updateWidgetListView(Context context,
                                             int appWidgetId) {
        getCursors(context, appWidgetId);

        RemoteViews views = new RemoteViews(context.getPackageName(), currentMode);

        //Set intent for change widget mode
        views.setOnClickPendingIntent(R.id.modeSwitchImageView, getPendingIntentWithAction(context,
                new Intent(context, WidgetProvider.class), appWidgetId, CHANGE_WIDGET_MODE));

        getCursors(context, appWidgetId);

        Log.e("provider", "list update");
        //which layout to show on widget
        if(currentMode == Constants.WIDGET_TITLE_MODE){
            //Set note title and intent to change note
            views.setTextViewText(R.id.titleTextView, noteCursor.getString(noteCursor.getColumnIndexOrThrow(Constants.NOTE_TITLE_COL)));

            //Reconfigure intent
            Intent configIntent = new Intent(context, WidgetConfigActivity.class);
            configIntent.setAction(WidgetProvider.ACTION_WIDGET_CONFIGURE);
            PendingIntent configPendingIntent = PendingIntent.getActivity(context, 0, configIntent, 0);
            views.setOnClickPendingIntent(R.id.titleTextView, configPendingIntent);

        }
        else {
            //Set intent for increase text size
            views.setOnClickPendingIntent(R.id.increaseTextSizeImageView, getPendingIntentWithAction(context, new
                    Intent(context, WidgetProvider.class), appWidgetId, INCREASE_TEXT_SIZE));

            //Set intent for decrease text size
            views.setOnClickPendingIntent(R.id.decreaseTextSizeImageView, getPendingIntentWithAction(context,
                    new Intent(context, WidgetProvider.class), appWidgetId, DECREASE_TEXT_SIZE));
        }

        //RemoteViews Service needed to provide adapter for ListView
        Intent svcIntent = new Intent(context, WidgetService.class);
        //passing app widget id to that RemoteViews Service
        svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        //pass note text
        svcIntent.putExtra(Constants.NOTE_TEXT_COL, noteCursor.getString(
                noteCursor.getColumnIndexOrThrow(Constants.NOTE_TEXT_COL)));
        //setting a unique Uri to the intent
        //don't know its purpose to me right now
        svcIntent.setData(Uri.parse(
                svcIntent.toUri(Intent.URI_INTENT_SCHEME)));
        //setting adapter to listview of the widget
        views.setRemoteAdapter(R.id.noteListView,
                svcIntent);
        //setting an empty view in case of no data
        views.setEmptyView(R.id.noteListView, R.id.emptyTextView);
        return views;
    }
}
