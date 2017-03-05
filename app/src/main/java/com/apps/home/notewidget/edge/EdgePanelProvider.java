package com.apps.home.notewidget.edge;

import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.apps.home.notewidget.EditNoteActivity;
import com.apps.home.notewidget.R;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.Utils;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider;

public class EdgePanelProvider extends SlookCocktailProvider {
    private static final String TAG = "EdgePanelProvider";
    private static final String INCREASE_TEXT_SIZE = "android.edgepanel.action.INCREASE_TEXT_SIZE";
    private static final String DECREASE_TEXT_SIZE = "android.edgepanel.action.DECREASE_TEXT_SIZE";
    private static final String ACTION_WIDGET_CONFIGURE = "ConfigureWidget";
    private static SharedPreferences preferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "onReceive");
        if(preferences == null)
            preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);

        int cocktailId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        int currentTextSize = preferences.getInt(Constants.EDGE_TEXT_SIZE_KEY, 10);
            switch (intent.getAction()) {
                case INCREASE_TEXT_SIZE:
                    Log.v(TAG, "increase text size");

                    preferences.edit().putInt(Constants.EDGE_TEXT_SIZE_KEY, (currentTextSize + 1)).apply();

                    Utils.showToast(context, context.getString(R.string.text_size) + (currentTextSize + 1));

                    updateListView(context, cocktailId);

                    context.sendBroadcast(new Intent(EdgeConfigActivity.UPDATE_NOTE_TEXT_SIZE));
                    break;
                case DECREASE_TEXT_SIZE:
                    Log.v(TAG, "decrease text size");
                    if (currentTextSize > 1) {
                        preferences.edit().putInt(Constants.EDGE_TEXT_SIZE_KEY, (currentTextSize - 1)).apply();

                        Utils.showToast(context, context.getString(R.string.text_size) + (currentTextSize - 1));

                        updateListView(context, cocktailId);

                        context.sendBroadcast(new Intent(EdgeConfigActivity.UPDATE_NOTE_TEXT_SIZE));
                    } else
                        Utils.showToast(context, context.getString(R.string.text_size_cannot_be_lower_than_1));

                    break;
            }

        super.onReceive(context, intent);
    }

    @Override
    public void onVisibilityChanged(Context context, int cocktailId, int visibility) {
        Log.i(TAG, "onVisibilityChanged");
        if(visibility == SlookCocktailManager.COCKTAIL_VISIBILITY_SHOW){
            context.sendBroadcast(new Intent(EdgeConfigActivity.SAVE_CHANGES_ACTION));
        }

        if(preferences == null)
            preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        boolean previousState = preferences.getBoolean(Constants.EDGE_WAS_LOCKED_KEY, false);
        KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        boolean currentState = myKM.inKeyguardRestrictedInputMode();

        if(previousState != currentState){
            preferences.edit().putBoolean(Constants.EDGE_WAS_LOCKED_KEY, currentState).apply();
            if(preferences.getBoolean(Constants.EDGE_HIDE_CONTENT_KEY, false))
                Utils.updateAllEdgePanels(context);
        }

        super.onVisibilityChanged(context, cocktailId, visibility); // check if screen is locked and if previously was locked and update if needed
    }

    @Override
    public void onDisabled(Context context) {
        Log.i(TAG, "onDisabled");
        if(preferences == null)
            preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().remove(Constants.EDGE_HIDE_CONTENT_KEY).remove(Constants.EDGE_WAS_LOCKED_KEY)
                .remove(Constants.EDGE_VISIBLE_NOTES_KEY).remove(Constants.EDGE_NOTES_ORDER_KEY)
                .remove(Constants.EDGE_TEXT_SIZE_KEY).remove(Constants.EDGE_IGNORE_TABS_KEY).apply();

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
        if(preferences == null)
            preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);

        for(int id : cocktailIds) {
            updateListView(context, id);

            RemoteViews remoteViews;

            KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            if(preferences.getBoolean(Constants.EDGE_HIDE_CONTENT_KEY, false) && myKM.inKeyguardRestrictedInputMode()) {
                remoteViews = new RemoteViews(context.getPackageName(), R.layout.edge_panel_hidden_content);
            } else {
                remoteViews = new RemoteViews(context.getPackageName(), R.layout.edge_panel);

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
            }
            cocktailManager.updateCocktail(id, remoteViews); //TODO help info
        }
    }

    private PendingIntent getNoteEditPendingIntent(Context context){
        Intent startIntent = new Intent(context, EditNoteActivity.class);
//        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
