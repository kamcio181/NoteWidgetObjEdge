package com.apps.home.notewidget.edge;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Html;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.objects.Widget;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.DatabaseHelper;

import java.util.ArrayList;

public class EdgeListProvider implements RemoteViewsService.RemoteViewsFactory {
    private static final String TAG = "ListProvider";
    private Context context = null;
    private int cocktailId;
    private ArrayList<Note> notes;
    private float titleSize;
    private int noteSize;

    public EdgeListProvider(Context context, Intent intent) {
        this.context = context;
        cocktailId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        getObjects(context);
        Log.v(TAG, "constructor");
    }

    private void getObjects(Context context){
        final DatabaseHelper helper = new DatabaseHelper(context);
        Log.v(TAG, "getObjects");
        notes = helper.getNotesOnDemand(false);
        SharedPreferences preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        noteSize = preferences.getInt("TextSize"+cocktailId, 10);
        titleSize = 1.4f * noteSize;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        Log.v(TAG, "getViewAt");

        final RemoteViews remoteView = new RemoteViews(
                context.getPackageName(),R.layout.edge_list_item);

        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(Constants.ID_COL, notes.get(position).getId());
        remoteView.setOnClickFillInIntent(R.id.item, fillInIntent);

        remoteView.setTextViewText(R.id.textView7, notes.get(position).getTitle());
        String noteText = notes.get(position).getNote();

        if(!noteText.trim().equals("")){
            Log.v(TAG, "note is not empty");
            //Set note text
            boolean skipTabs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).getBoolean(Constants.IGNORE_TABS_IN_EDGE_PANEL_KEY, false);
            remoteView.setTextViewText(R.id.textView, Html.fromHtml(skipTabs? noteText.replace("\u0009", "") : noteText));
        } else {
            Log.v(TAG, "empty note");
            remoteView.setTextViewText(R.id.textView, context.getString(R.string.note_is_empty_click_here_to_edit));
        }

        //Set title size
        remoteView.setFloat(R.id.textView7, "setTextSize", titleSize);
        //Set note size
        remoteView.setFloat(R.id.textView, "setTextSize", noteSize);
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
        getObjects(context);
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return notes.size();
    }

    @Override
    public long getItemId(int position) {
        return notes.get(position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
