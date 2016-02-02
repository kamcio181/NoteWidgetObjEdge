package com.apps.home.notewidget;

/**
 * Created by Kamil on 2016-01-23.
 */

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;

/**
 * If you are familiar with Adapter of ListView,this is the same as adapter
 * with few changes
 *
 */
public class WidgetListProvider implements RemoteViewsService.RemoteViewsFactory {
    private Context context = null;
    private int appWidgetId;
    private int currentSize;
    private String noteText;
    private SQLiteDatabase db;
    private Cursor configCursor;

    public WidgetListProvider(Context context, Intent intent) {
        this.context = context;
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        noteText = intent.getStringExtra(Constants.NOTE_TEXT_COL);
        Log.e("list", "constructor");
    }

    private void setTextSize(){
        if(db==null) {
            SQLiteOpenHelper helper = new DatabaseHelper(context);
            db = helper.getWritableDatabase();
        }

        configCursor = db.query(Constants.WIDGETS_TABLE, new String[]{Constants.CURRENT_TEXT_SIZE},
                Constants.WIDGET_ID + " = ?", new String[]{Integer.toString(appWidgetId)},
                null, null, null);
        configCursor.moveToFirst();

        currentSize = configCursor.getInt(configCursor.getColumnIndexOrThrow(Constants.CURRENT_TEXT_SIZE));
    }

    /*
    *Similar to getView of Adapter where instead of View
    *we return RemoteViews
    *
    */
    @Override
    public RemoteViews getViewAt(int position) {
        final RemoteViews remoteView = new RemoteViews(
                context.getPackageName(), R.layout.note_text);

        //Set note text
        remoteView.setTextViewText(R.id.noteTextView, noteText);
        //Set text size
        remoteView.setFloat(R.id.noteTextView, "setTextSize", currentSize);
        Log.e("list", "getViewAt "+currentSize);
        return remoteView;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public void onCreate() {
        Log.e("list", "create");
    }

    @Override
    public void onDataSetChanged() {
        setTextSize();
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return noteText.length()>0? 1:0;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
