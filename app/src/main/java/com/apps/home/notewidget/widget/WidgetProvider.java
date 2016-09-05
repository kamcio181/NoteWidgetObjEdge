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

import com.apps.home.notewidget.EditNoteActivity;
import com.apps.home.notewidget.MainActivity;
import com.apps.home.notewidget.R;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.objects.Widget;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.DatabaseHelper;
import com.apps.home.notewidget.utils.Utils;

public class WidgetProvider extends AppWidgetProvider {
    public static final String INCREASE_TEXT_SIZE = "android.appwidget.action.INCREASE_TEXT_SIZE";
    public static final String DECREASE_TEXT_SIZE = "android.appwidget.action.DECREASE_TEXT_SIZE";
    public static final String CHANGE_WIDGET_MODE = "android.appwidget.action.CHANGE_WIDGET_MODE";
    public static final String CHANGE_THEME_MODE = "android.appwidget.action.CHANGE_THEME_MODE";
    public static final String ACTION_WIDGET_CONFIGURE = "ConfigureWidget";

    private static final String TAG = "WidgetProvider";
    private DatabaseHelper helper;
    private Widget widget;
    private Note note;

    private SharedPreferences preferences;

    private int currentTextSize;
    private int currentWidgetMode;
    private int currentThemeMode;

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.v(TAG, "onEnabled");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "onReceive");

        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if(isConfigured(context, appWidgetId)) {
            Log.v(TAG, "isConfigured");
            getObjects(context, appWidgetId);

            switch (intent.getAction()) {
                case INCREASE_TEXT_SIZE:
                    Log.v(TAG, "increase text size");
                    widget.setTextSize(currentTextSize + 1);
                    helper.updateWidgetOnDemand(widget, widget.getId());

                    Utils.showToast(context, context.getString(R.string.text_size) + (currentTextSize + 1));

                    updateNote(context, appWidgetId);

                    break;
                case DECREASE_TEXT_SIZE:
                    Log.v(TAG, "decrease text size");
                    if (currentTextSize > 1) {
                        widget.setTextSize(currentTextSize - 1);
                        helper.updateWidgetOnDemand(widget, widget.getId());

                        Utils.showToast(context, context.getString(R.string.text_size) + (currentTextSize - 1));

                        updateNote(context, appWidgetId);
                    } else
                        Utils.showToast(context, context.getString(R.string.text_size_cannot_be_lower_than_1));

                    break;
                case CHANGE_WIDGET_MODE:
                    Log.v(TAG, "widget mode change");
                    widget.setMode(Utils.switchWidgetMode(currentWidgetMode));
                    helper.updateWidgetOnDemand(widget, widget.getId());

                    AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, updateWidgetListView(context, appWidgetId));

                    break;

                case CHANGE_THEME_MODE:
                    Log.v(TAG, "widget theme change");
                    widget.setTheme(Utils.switchThemeMode(currentThemeMode));
                    helper.updateWidgetOnDemand(widget, widget.getId());

                    AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, updateWidgetListView(context, appWidgetId));
                    updateNote(context, appWidgetId);
                    break;
            }
        }
        super.onReceive(context, intent);
    }

    private void updateNote(Context context, int appWidgetId){
		Log.v(TAG, "updateNote " + appWidgetId);
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        mgr.notifyAppWidgetViewDataChanged(appWidgetId, R.id.noteListView);
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.v(TAG, "onUpdate");
        for (int appWidgetId : appWidgetIds) {
            if(isConfigured(context, appWidgetId)){

                appWidgetManager.updateAppWidget(appWidgetId, updateWidgetListView(context, appWidgetId));

                updateNote(context, appWidgetId);
            }
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        Log.v(TAG, "deleteNote");
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
        helper = new DatabaseHelper(context);
        Log.v(TAG, "getObjects");

        widget = helper.getWidgetOnDemand(widgetId);
        if(widget != null){
            Log.v(TAG, "widget Not null");
            currentTextSize = widget.getTextSize();
            currentThemeMode = widget.getTheme();
            currentWidgetMode = widget.getMode();

            note = helper.getNoteOnDemand(false, widget.getNoteId());
        }
    }

    private PendingIntent getPendingIntentWithAction(Context context, Intent intent, int appWidgetId, String action){
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getConfigPendingIntent(Context context, int appWidgetId){
        Intent configIntent = new Intent(context, WidgetConfigActivity.class);
        configIntent.setAction(WidgetProvider.ACTION_WIDGET_CONFIGURE);
        configIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        configIntent.putExtra(Constants.RECONFIGURE, true);
        return PendingIntent.getActivity(context, appWidgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getOpenAppPendingIntent(Context context, int appWidgetId){
        Intent configIntent = new Intent(context, MainActivity.class);
        configIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
        return PendingIntent.getActivity(context, appWidgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
	
	private PendingIntent getNoteEditPendingIntent(Context context){
		Intent startIntent = new Intent(context, EditNoteActivity.class);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

    private RemoteViews updateWidgetListView(Context context,
                                             int appWidgetId) {
        getObjects(context, appWidgetId);
        RemoteViews views;
        Log.v(TAG, "updateWidgetListView");

        if(widget != null && note != null){
            Log.v(TAG, "themeMode " + currentThemeMode + " widgetMode " + currentWidgetMode);
            views = new RemoteViews(context.getPackageName(), Utils.getLayoutFile(context, currentThemeMode, currentWidgetMode));

            //Set intent for change widget mode
            views.setOnClickPendingIntent(R.id.modeSwitchImageView, getPendingIntentWithAction(context,
                    new Intent(context, WidgetProvider.class), appWidgetId, CHANGE_WIDGET_MODE));

            //which layout to show on widget
            if(currentWidgetMode == Constants.WIDGET_MODE_TITLE){
                Log.v(TAG, "Widget Title Mode");
                //Set note title and intent to change note
                views.setTextViewText(R.id.titleTextView, note.getTitle());

                //Reconfigure intent
                //views.setOnClickPendingIntent(R.id.titleTextView, getConfigPendingIntent(context, appWidgetId));

                //Open app intent
                views.setOnClickPendingIntent(R.id.titleTextView, getOpenAppPendingIntent(context, appWidgetId));
            }
            else {
                Log.v(TAG, "Widget Config Mode");
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
            Log.v(TAG, "Widget Before List View");
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
            Log.v(TAG, "Widget After List View");

            views.setPendingIntentTemplate(R.id.noteListView, getNoteEditPendingIntent(context));
            //setting an empty view in case of no data
            views.setEmptyView(R.id.noteListView, R.id.noteTextView);
        } else {
            Log.v(TAG, "Widget Empty");
            views = new RemoteViews(context.getPackageName(),
                    currentThemeMode == Constants.WIDGET_THEME_LIGHT? R.layout.appwidget_deleted_note_light
                    : R.layout.appwidget_deleted_note_dark);
            views.setTextViewText(R.id.noteTextView, context.getString(R.string.note_was_deleted_click_here_to_pick_new_one));
            views.setOnClickPendingIntent(R.id.container, getConfigPendingIntent(context, appWidgetId));

        }
        return views;
    }
}
