package com.apps.home.notewidget.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViewsService;

public class WidgetService extends RemoteViewsService {
    private static final String TAG = "WidgetService";
    private int id;
    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate " + id);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStart " + id);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy " + id);
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e(TAG, "onTaskRemoved " + id);
        super.onTaskRemoved(rootIntent);
    }
    /**
* So pretty simple just defining the Adapter of the listview
* here Adapter is ListProvider
* */

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        Log.e(TAG, "getView " + id);
        return (new WidgetListProvider(this.getApplicationContext(), intent));
    }
}
