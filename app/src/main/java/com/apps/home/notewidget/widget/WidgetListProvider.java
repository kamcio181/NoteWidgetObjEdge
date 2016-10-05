package com.apps.home.notewidget.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.objects.Widget;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.DatabaseHelper;
import com.apps.home.notewidget.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;

public class WidgetListProvider implements RemoteViewsService.RemoteViewsFactory {
    private static final String TAG = "ListProvider";
    private Context context = null;
    private final int appWidgetId;
    private Widget widget;
    private Note note;
	
    public WidgetListProvider(Context context, Intent intent) {
        this.context = context;
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        getObjects();
        Log.v(TAG, "constructor");
    }

    private void getObjects(){
        final DatabaseHelper helper = new DatabaseHelper(context);
        Log.v(TAG, "getObjects");
        widget = helper.getWidgetOnDemand(appWidgetId);

        if(widget != null){
            Log.v(TAG, "widget not null");
            note = helper.getNoteOnDemand(false, widget.getNoteId());
        }
    }

    @Override
    public RemoteViews getViewAt(int position) {
        Log.v(TAG, "getViewAt");
        int textSize = widget.getTextSize();
        int widgetTheme = widget.getTheme();
        String noteText = note.getNote();

        int item = widgetTheme == Constants.WIDGET_THEME_LIGHT? R.layout.note_text_light : R.layout.note_text_dark;
        Log.v(TAG, "currentThemeMode " + widgetTheme);
        final RemoteViews remoteView = new RemoteViews(
                context.getPackageName(),item);

        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(Constants.ID_COL, widget.getNoteId());
        remoteView.setOnClickFillInIntent(R.id.noteTextView, fillInIntent);

        if(noteText.trim().equals("") || (note.getType() == Constants.TYPE_LIST && noteText.trim().startsWith("0"))){
            Log.v(TAG, "note is empty");
            remoteView.setTextViewText(R.id.noteTextView, context.getString(R.string.note_is_empty_click_here_to_edit));
        } else {
            if(note.getType() == Constants.TYPE_NOTE) {
                boolean skipTabs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).getBoolean(Constants.IGNORE_TABS_IN_WIDGETS_KEY, false);
                remoteView.setTextViewText(R.id.noteTextView, Utils.getHtmlFormattedText(skipTabs ? noteText.replace("\u0009", "") : noteText));
            } else {
                StringBuilder builder = new StringBuilder();

                int activeItemsCount = Integer.parseInt(noteText.substring(0, noteText.indexOf("<br/>")));
                ArrayList<String> items = new ArrayList<>();
                items.addAll(Arrays.asList(noteText.split("<br/>")));
                items.remove(0);

                for (int i = 0; i<activeItemsCount; i++){
                    builder.append(items.get(i)).append("\n");
                    remoteView.setTextViewText(R.id.noteTextView, builder.toString().trim());
                }
            }
        }
        //Set text size
        remoteView.setFloat(R.id.noteTextView, "setTextSize", textSize);
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
        getObjects();
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
