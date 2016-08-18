package com.apps.home.notewidget.edge;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.EditNoteActivity;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.Utils;
import com.apps.home.notewidget.widget.WidgetConfigActivity;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider;

public class EdgePanelProvider extends SlookCocktailProvider {
    private static final String TAG = "EdgePanelProvider";
    public static final String INCREASE_TEXT_SIZE = "android.edgepanel.action.INCREASE_TEXT_SIZE";
    public static final String DECREASE_TEXT_SIZE = "android.edgepanel.action.DECREASE_TEXT_SIZE";
    public static final String ACTION_WIDGET_CONFIGURE = "ConfigureWidget";
    public static SharedPreferences preferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "onReceive");
        if(preferences == null)
            preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);

        int cocktailId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        int currentTextSize = preferences.getInt("TextSize"+cocktailId, 10);
            switch (intent.getAction()) {
                case INCREASE_TEXT_SIZE:
                    Log.v(TAG, "increase text size");

                    preferences.edit().putInt("TextSize"+cocktailId, (currentTextSize + 1)).apply();

                    Utils.showToast(context, context.getString(R.string.text_size) + (currentTextSize + 1));

                    updateListView(context, cocktailId);

                    break;
                case DECREASE_TEXT_SIZE:
                    Log.v(TAG, "decrease text size");
                    if (currentTextSize > 1) {
                        preferences.edit().putInt("TextSize"+cocktailId, (currentTextSize - 1)).apply();

                        Utils.showToast(context, context.getString(R.string.text_size) + (currentTextSize - 1));

                        updateListView(context, cocktailId);
                    } else
                        Utils.showToast(context, context.getString(R.string.text_size_cannot_be_lower_than_1));

                    break;
            }

        super.onReceive(context, intent);
    }

    @Override
    public void onVisibilityChanged(Context context, int cocktailId, int visibility) {
        Log.i(TAG, "onVisibilityChanged");
        super.onVisibilityChanged(context, cocktailId, visibility);
    }

    @Override
    public void onDisabled(Context context) {
        Log.i(TAG, "onDisabled");
        super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
        Log.i(TAG, "onEnabled");
        super.onEnabled(context);
    }

    @Override
    public void onUpdate(Context context, SlookCocktailManager cocktailManager, int[] cocktailIds) {
        Log.i(TAG, "onUpdate");
        super.onUpdate(context, cocktailManager, cocktailIds);



        for(int id : cocktailIds) {
            updateListView(context, id);

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.edge_panel);

            //Set intent for increase text size
            remoteViews.setOnClickPendingIntent(R.id.increaseTextSizeImageView, getPendingIntentWithAction(context,
                    new Intent(context, EdgePanelProvider.class), id, INCREASE_TEXT_SIZE));

            //Set intent for decrease text size
            remoteViews.setOnClickPendingIntent(R.id.decreaseTextSizeImageView, getPendingIntentWithAction(context,
                    new Intent(context, EdgePanelProvider.class), id, DECREASE_TEXT_SIZE));

            remoteViews.setOnClickPendingIntent(R.id.openConfigurationImageView, getConfigPendingIntent(context, id));

            //RemoteViews Service needed to provide adapter for ListView
            Intent svcIntent = new Intent(context, EdgeListService.class);
            //passing app widget id to that RemoteViews Service
            svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            //setting a unique Uri to the intent
            //don't know its purpose to me right now
            svcIntent.setData(Uri.parse(
                    svcIntent.toUri(Intent.URI_INTENT_SCHEME)));
            //setting adapter to listView of the widget
            remoteViews.setRemoteAdapter(R.id.listView, svcIntent);
            Log.v(TAG, "Widget After List View");

            remoteViews.setPendingIntentTemplate(R.id.listView, getNoteEditPendingIntent(context));
            //setting an empty view in case of no data
            remoteViews.setEmptyView(R.id.listView, R.id.noteTextView);

            cocktailManager.updateCocktail(id, remoteViews);
        }
    }

    private PendingIntent getNoteEditPendingIntent(Context context){
        Intent startIntent = new Intent(context, EditNoteActivity.class);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getConfigPendingIntent(Context context, int cocktailId){
        Intent configIntent = new Intent(context, EdgeConfigActivity.class);
        configIntent.setAction(EdgePanelProvider.ACTION_WIDGET_CONFIGURE);
        configIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, cocktailId);
        return PendingIntent.getActivity(context, cocktailId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getPendingIntentWithAction(Context context, Intent intent, int cocktailId, String action){
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, cocktailId);
        return PendingIntent.getBroadcast(context, cocktailId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void updateListView(Context context, int cocktailId){
        Log.v(TAG, "updateListView " + cocktailId);
        SlookCocktailManager mgr = SlookCocktailManager.getInstance(context);
        mgr.notifyCocktailViewDataChanged(cocktailId, R.id.listView);
    }
}
