package com.apps.home.notewidget.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.Html;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.Utils;

public class WidgetListProvider implements RemoteViewsService.RemoteViewsFactory {
    private Context context = null;
    private int appWidgetId;
    private int currentSize;
    private int currentThemeMode;
    private SQLiteDatabase db;
    private Cursor configCursor;
    private Cursor noteCursor;
	private long noteId;
    private String noteText;
	
    public WidgetListProvider(Context context, Intent intent) {
        this.context = context;
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
				
		noteId = intent.getIntExtra(Constants.ID_COL,0);
        Log.e("list", "constructor");
    }

    private void getCursors(){
        if((db = Utils.getDb(context)) != null) {

            configCursor = db.query(Constants.WIDGETS_TABLE, new String[]{Constants.CURRENT_TEXT_SIZE_COL, Constants.CURRENT_THEME_MODE_COL},
                    Constants.WIDGET_ID_COL + " = ?", new String[]{Integer.toString(appWidgetId)},
                    null, null, null);
            configCursor.moveToFirst();

            currentSize = configCursor.getInt(configCursor.getColumnIndexOrThrow(Constants.CURRENT_TEXT_SIZE_COL));

            currentThemeMode = configCursor.getInt(configCursor.getColumnIndexOrThrow(Constants.CURRENT_THEME_MODE_COL));

            noteCursor = db.query(Constants.NOTES_TABLE, new String[]{Constants.NOTE_TEXT_COL},
                    Constants.ID_COL + " = ?", new String[]{Long.toString(noteId)}, null, null, null);
            noteCursor.moveToFirst();

            noteText = noteCursor.getString(noteCursor.getColumnIndexOrThrow(Constants.NOTE_TEXT_COL));
        }
    }

    @Override
    public RemoteViews getViewAt(int position) {
        getCursors();

        int item = currentThemeMode == Constants.WIDGET_THEME_LIGHT? R.layout.note_text_light : R.layout.note_text_dark;
        Log.e("list", "currentThemeMode " + currentThemeMode);
        final RemoteViews remoteView = new RemoteViews(
                context.getPackageName(),item);

        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(Constants.ID_COL, noteId);
        remoteView.setOnClickFillInIntent(R.id.noteTextView, fillInIntent);

        if(!noteText.trim().equals("")){
            //Set note text
            boolean skipTabs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).getBoolean(Constants.IGNORE_TABS_IN_WIDGETS_KEY, false);
            remoteView.setTextViewText(R.id.noteTextView, Html.fromHtml(skipTabs? noteText.replace("\u0009", "") : noteText));
        } else {
            remoteView.setTextViewText(R.id.noteTextView, "Note is empty, click here to edit");
        }
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
        return 1;
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
