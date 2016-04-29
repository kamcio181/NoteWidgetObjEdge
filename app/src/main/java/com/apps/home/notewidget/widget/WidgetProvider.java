package com.apps.home.notewidget.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.apps.home.notewidget.MainActivity;
import com.apps.home.notewidget.R;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.objects.Widget;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.DatabaseHelper2;
import com.apps.home.notewidget.utils.Utils;

public class WidgetProvider extends AppWidgetProvider {
    public static final String INCREASE_TEXT_SIZE = "android.appwidget.action.INCREASE_TEXT_SIZE";
    public static final String DECREASE_TEXT_SIZE = "android.appwidget.action.DECREASE_TEXT_SIZE";
    public static final String CHANGE_WIDGET_MODE = "android.appwidget.action.CHANGE_WIDGET_MODE";
    public static final String CHANGE_THEME_MODE = "android.appwidget.action.CHANGE_THEME_MODE";
    public static String ACTION_WIDGET_CONFIGURE = "ConfigureWidget";

    private DatabaseHelper2 helper;
    private Widget widget;
    private Note note;

    private SharedPreferences preferences;

    private int currentTextSize;
    private int currentWidgetMode;
    private int currentThemeMode;

    @Override
    public void onReceive(Context context, Intent intent) {

        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if(isConfigured(context, appWidgetId)) {
            getObjects(context, appWidgetId);

            switch (intent.getAction()) {
                case INCREASE_TEXT_SIZE:
                    widget.setTextSize(currentTextSize + 1);
                    helper.updateWidget(widget, widget.getId(), null);

                    Utils.showToast(context, context.getString(R.string.text_size) + (currentTextSize + 1));

                    updateNote(context, appWidgetId);

                    break;
                case DECREASE_TEXT_SIZE:
                    if (currentTextSize > 1) {
                        widget.setTextSize(currentTextSize - 1);
                        helper.updateWidget(widget, widget.getId(), null);

                        Utils.showToast(context, context.getString(R.string.text_size) + (currentTextSize - 1));

                        updateNote(context, appWidgetId);
                    } else
                        Utils.showToast(context, context.getString(R.string.text_size_cannot_be_lower_than_1));

                    break;
                case CHANGE_WIDGET_MODE:
                    widget.setMode(Utils.switchWidgetMode(currentWidgetMode));
                    helper.updateWidget(widget, widget.getId(), null);

                    AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, updateWidgetListView(context, appWidgetId));

                    break;

                case CHANGE_THEME_MODE:
                    widget.setTheme(Utils.switchThemeMode(currentThemeMode));
                    helper.updateWidget(widget, widget.getId(), null);

                    AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, updateWidgetListView(context, appWidgetId));

                    break;
            }
        }
        super.onReceive(context, intent);
    }

    private void updateNote(Context context, int appWidgetId){
		Log.e("Widget", "" + appWidgetId);
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        mgr.notifyAppWidgetViewDataChanged(appWidgetId, R.id.noteListView);
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.e("widget", "onUpdate");
        for (int appWidgetId : appWidgetIds) {
            if(isConfigured(context, appWidgetId)){

                RemoteViews views = updateWidgetListView(context, appWidgetId);

                appWidgetManager.updateAppWidget(appWidgetId, views);

                updateNote(context, appWidgetId);
            }
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);

        if (preferences == null)
            preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        for (int i : appWidgetIds) {
            helper.removeWidget(i, null);
            editor.remove(i + Constants.CONFIGURED_KEY).apply();
        }
    }

    private boolean isConfigured(Context context, int widgetId){
        if(preferences == null)
            preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(widgetId+Constants.CONFIGURED_KEY, false);
    }

    private void getObjects(final Context context, int widgetId){
        helper = new DatabaseHelper2(context);

        helper.getWidget(widgetId, new DatabaseHelper2.OnWidgetLoadListener() {
            @Override
            public void onWidgetLoaded(Widget widget) {
                if(widget != null){
                    WidgetProvider.this.widget = widget;
                    currentTextSize = widget.getTextSize();
                    currentThemeMode = widget.getTheme();
                    currentWidgetMode = widget.getMode();

                    helper.getNote(false, widget.getNoteId(), new DatabaseHelper2.OnNoteLoadListener() {
                        @Override
                        public void onNoteLoaded(Note note) {
                            if(note != null){
                                WidgetProvider.this.note = note;
                            }
                        }
                    });
                }
            }
        });
    }

    private PendingIntent getPendingIntentWithAction(Context context, Intent intent, int appWidgetId, String action){
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getConfigPendingIntent(Context context, int appWidgetId){
        Log.e("WidgetProvider", "config intent");
        Intent configIntent = new Intent(context, WidgetConfigActivity.class);
        configIntent.setAction(WidgetProvider.ACTION_WIDGET_CONFIGURE);
        configIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getActivity(context, appWidgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getOpenAppPendingIntent(Context context, int appWidgetId){
        Intent configIntent = new Intent(context, MainActivity.class);
        configIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
        return PendingIntent.getActivity(context, appWidgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
	
	private PendingIntent getNoteEditPendingIntent(Context context){
        Log.e("WidgetProvider", "edit intent");
		Intent startIntent = new Intent(context, WidgetEditNoteActivity.class);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

    private RemoteViews updateWidgetListView(Context context,
                                             int appWidgetId) {
        getObjects(context, appWidgetId);
        RemoteViews views;

        if(widget != null && note != null){
            Log.e("provider", "themeMode " + currentThemeMode + " widgetMode "+currentWidgetMode);
            views = new RemoteViews(context.getPackageName(), Utils.getLayoutFile(context, currentThemeMode, currentWidgetMode));

            //Set intent for change widget mode
            views.setOnClickPendingIntent(R.id.modeSwitchImageView, getPendingIntentWithAction(context,
                    new Intent(context, WidgetProvider.class), appWidgetId, CHANGE_WIDGET_MODE));
				
            Log.e("provider", "list update "+ currentWidgetMode);
            //which layout to show on widget
            if(currentWidgetMode == Constants.WIDGET_MODE_TITLE){
                //Set note title and intent to change note
                views.setTextViewText(R.id.titleTextView, note.getTitle());

                //Reconfigure intent
                //views.setOnClickPendingIntent(R.id.titleTextView, getConfigPendingIntent(context, appWidgetId));

                //Open app intent
                views.setOnClickPendingIntent(R.id.titleTextView, getOpenAppPendingIntent(context, appWidgetId));
            }
            else {
                //Set intent for increase text size
                views.setOnClickPendingIntent(R.id.increaseTextSizeImageView, getPendingIntentWithAction(context,
                        new Intent(context, WidgetProvider.class), appWidgetId, INCREASE_TEXT_SIZE));

                //Set intent for decrease text size
                views.setOnClickPendingIntent(R.id.decreaseTextSizeImageView, getPendingIntentWithAction(context,
                        new Intent(context, WidgetProvider.class), appWidgetId, DECREASE_TEXT_SIZE));

                //Set intent for change widget theme mode
                views.setOnClickPendingIntent(R.id.switchThemeImageView, getPendingIntentWithAction(context,
                        new Intent(context, WidgetProvider.class), appWidgetId, CHANGE_THEME_MODE));
            }

            //RemoteViews Service needed to provide adapter for ListView
            Intent svcIntent = new Intent(context, WidgetService.class);
            //passing app widget id to that RemoteViews Service
            svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            //setting a unique Uri to the intent
            //don't know its purpose to me right now
            svcIntent.setData(Uri.parse(
                    svcIntent.toUri(Intent.URI_INTENT_SCHEME)));
            //setting adapter to listView of the widget
            views.setRemoteAdapter(R.id.noteListView, svcIntent);
            views.setPendingIntentTemplate(R.id.noteListView, getNoteEditPendingIntent(context));

            //setting an empty view in case of no data
            views.setEmptyView(R.id.noteListView, R.id.noteTextView);
        } else {
            views = new RemoteViews(context.getPackageName(),
                    currentThemeMode == Constants.WIDGET_THEME_LIGHT? R.layout.appwidget_deleted_note_light
                    : R.layout.appwidget_deleted_note_dark);
            views.setTextViewText(R.id.noteTextView, context.getString(R.string.note_was_deleted_click_here_to_pick_new_one));
            views.setOnClickPendingIntent(R.id.container, getConfigPendingIntent(context, appWidgetId));

        }
        return views;
    }
}
