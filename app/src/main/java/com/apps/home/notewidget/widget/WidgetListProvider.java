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
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.objects.Widget;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.DatabaseHelper2;
import com.apps.home.notewidget.utils.Utils;

public class WidgetListProvider implements RemoteViewsService.RemoteViewsFactory {
    private Context context = null;
    private int appWidgetId;
    private Widget widget;
    private Note note;
	
    public WidgetListProvider(Context context, Intent intent) {
        this.context = context;
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        Log.e("list", "constructor");
    }

    private void getObjects(){
        final DatabaseHelper2 helper = new DatabaseHelper2(context);

        helper.getWidget(appWidgetId, new DatabaseHelper2.OnWidgetLoadListener() {
            @Override
            public void onWidgetLoaded(Widget widget) {
                if(widget != null){
                    WidgetListProvider.this.widget = widget;

                    helper.getNote(false, widget.getNoteId(), new DatabaseHelper2.OnNoteLoadListener() {
                        @Override
                        public void onNoteLoaded(Note note) {
                            if(note != null){
                                WidgetListProvider.this.note = note;
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public RemoteViews getViewAt(int position) {
        getObjects();

        int textSize = widget.getTextSize();
        int widgetTheme = widget.getTheme();
        String noteText = note.getNote();

        int item = widgetTheme == Constants.WIDGET_THEME_LIGHT? R.layout.note_text_light : R.layout.note_text_dark;
        Log.e("list", "currentThemeMode " + widgetTheme);
        final RemoteViews remoteView = new RemoteViews(
                context.getPackageName(),item);

        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(Constants.ID_COL, widget.getNoteId());
        remoteView.setOnClickFillInIntent(R.id.noteTextView, fillInIntent);

        if(!noteText.trim().equals("")){
            //Set note text
            boolean skipTabs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).getBoolean(Constants.IGNORE_TABS_IN_WIDGETS_KEY, false);
            remoteView.setTextViewText(R.id.noteTextView, Html.fromHtml(skipTabs? noteText.replace("\u0009", "") : noteText));
        } else {
            remoteView.setTextViewText(R.id.noteTextView, context.getString(R.string.note_is_empty_click_here_to_edit));
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
