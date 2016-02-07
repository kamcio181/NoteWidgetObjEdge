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
import android.text.Html;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

/**
 * If you are familiar with Adapter of ListView,this is the same as adapter
 * with few changes
 *
 */
public class WidgetListProvider implements RemoteViewsService.RemoteViewsFactory {
    private Context context = null;
    private int appWidgetId;
    private int currentSize;
    private int currentThemeMode;
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

    private void getCursors(){
        if(db==null) {
            SQLiteOpenHelper helper = new DatabaseHelper(context);
            db = helper.getWritableDatabase();
        }

        configCursor = db.query(Constants.WIDGETS_TABLE, new String[]{Constants.CURRENT_TEXT_SIZE_COL, Constants.CURRENT_THEME_MODE_COL},
                Constants.WIDGET_ID_COL + " = ?", new String[]{Integer.toString(appWidgetId)},
                null, null, null);
        configCursor.moveToFirst();

        currentSize = configCursor.getInt(configCursor.getColumnIndexOrThrow(Constants.CURRENT_TEXT_SIZE_COL));

        currentThemeMode = configCursor.getInt(configCursor.getColumnIndexOrThrow(Constants.CURRENT_THEME_MODE_COL));
    }

    /*
    *Similar to getView of Adapter where instead of View
    *we return RemoteViews
    *
    */
    @Override
    public RemoteViews getViewAt(int position) {
        getCursors();

        int item = currentThemeMode == Constants.WIDGET_THEME_LIGHT? R.layout.note_text_light : R.layout.note_text_dark;
        Log.e("list", "currentThemeMode " + currentThemeMode);
        final RemoteViews remoteView = new RemoteViews(
                context.getPackageName(),item);

        //Set note text
        remoteView.setTextViewText(R.id.noteTextView, Html.fromHtml(noteText));
        //Set text size
        remoteView.setFloat(R.id.noteTextView, "setTextSize", currentSize);
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
    }

    @Override
    public void onDataSetChanged() {

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
