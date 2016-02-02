package com.apps.home.notewidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WidgetConfigActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{


    private int widgetID = 0;
    private ListView notesListView;
    private SQLiteDatabase db;
    private Cursor cursor;
    private Cursor noteCursor;
    private int noteId;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_widget_config);
        setResult(RESULT_CANCELED);

        notesListView = (ListView) findViewById(R.id.notesListView);
        new LoadNotes().execute();

        Bundle extras = getIntent().getExtras();
        if(extras != null){
            widgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            Log.e("config", "widgetId "+ widgetID);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        cursor.moveToPosition(i);
        noteId = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.ID_COL));
        insertOrUpdateItem();
        getNoteCursor();

        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.updateAppWidget(widgetID, updateWidgetListView());

        getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).edit().
                putBoolean(widgetID+Constants.CONFIGURED_KEY, true).commit();

        Intent intent = new Intent();
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void insertOrUpdateItem() {
        Log.e("config", "insert");
        Cursor cursor = db.query(Constants.WIDGETS_TABLE, new String[]{Constants.ID_COL},
                Constants.WIDGET_ID + " = ?", new String[]{Integer.toString(widgetID)}, null, null, null, null);

        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.CONNECTED_NOTE_ID, noteId);

        if(cursor.getCount()>0) {

            cursor.close();
            db.update(Constants.WIDGETS_TABLE, contentValues, Constants.WIDGET_ID + " = ?",
                    new String[]{Integer.toString(widgetID)});
            Log.e("config", "item updated " + contentValues.toString());
        } else {
            contentValues.put(Constants.WIDGET_ID, widgetID);
            contentValues.put(Constants.CURRENT_MODE, Constants.WIDGET_TITLE_MODE);
            contentValues.put(Constants.CURRENT_THEME, Constants.WIDGET_THEME_LIGHT);
            contentValues.put(Constants.CURRENT_TEXT_SIZE, 18);
            Log.e("config", "item inserted " + contentValues.toString());
        }
        db.insert(Constants.WIDGETS_TABLE, null, contentValues);

    }

    private void getNoteCursor(){
        noteCursor = db.query(Constants.NOTES_TABLE, new String[]{Constants.NOTE_TITLE_COL, Constants.NOTE_TEXT_COL},
                Constants.ID_COL + " = ?", new String[]{Integer.toString(
                        noteId)}, null, null, null );
        noteCursor.moveToFirst();
    }

    private RemoteViews updateWidgetListView() {

        RemoteViews views = new RemoteViews(getPackageName(), Constants.WIDGET_TITLE_MODE);

        //Set intent for change widget mode
        views.setOnClickPendingIntent(R.id.modeSwitchImageView, getPendingIntentWithAction(
                new Intent(this, WidgetProvider.class), widgetID, WidgetProvider.CHANGE_WIDGET_MODE));

        Log.e("config", "list update");
        //which layout to show on widget

        //Set note title and intent to change note
        views.setTextViewText(R.id.titleTextView, noteCursor.getString(
                noteCursor.getColumnIndexOrThrow(Constants.NOTE_TITLE_COL)));
        Log.e("config", "title " + noteCursor.getString(noteCursor.getColumnIndexOrThrow(Constants.NOTE_TITLE_COL)));
        //Reconfigure intent
        Intent configIntent = new Intent(this, WidgetConfigActivity.class);
        configIntent.setAction(WidgetProvider.ACTION_WIDGET_CONFIGURE);
        configIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent configPendingIntent = PendingIntent.getActivity(this, 0, configIntent, 0);
        views.setOnClickPendingIntent(R.id.titleTextView, configPendingIntent);

        //RemoteViews Service needed to provide adapter for ListView
        Intent svcIntent = new Intent(this, WidgetService.class);
        //passing app widget id to that RemoteViews Service
        svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
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

    private PendingIntent getPendingIntentWithAction(Intent intent, int appWidgetId, String action){
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(this, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private class LoadNotes extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            SQLiteOpenHelper helper = new DatabaseHelper(WidgetConfigActivity.this);
            try {
                db = helper.getWritableDatabase();

                cursor = db.query(Constants.NOTES_TABLE, new String[]{Constants.ID_COL, Constants.NOTE_TITLE_COL},
                        null, null, null, null, Constants.NOTE_TITLE_COL + " ASC");

                return true;

            } catch(SQLiteException e) {
                return false;

            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if(aBoolean){
                cursor.moveToFirst();
                notesListView.setAdapter(new SimpleCursorAdapter(WidgetConfigActivity.this,
                        android.R.layout.simple_expandable_list_item_1, cursor,
                        new String[]{Constants.NOTE_TITLE_COL}, new int[]{android.R.id.text1}, 0));
                notesListView.setOnItemClickListener(WidgetConfigActivity.this);
            }
            else {
                Utils.showToast(WidgetConfigActivity.this, "Database is not available");
                finish();
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cursor.close();
        db.close();
    }
}
