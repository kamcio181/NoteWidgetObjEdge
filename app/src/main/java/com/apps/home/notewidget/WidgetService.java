package com.apps.home.notewidget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Created by Kamil on 2016-01-23.
 */
public class WidgetService extends RemoteViewsService {
/**
* So pretty simple just defining the Adapter of the listview
* here Adapter is ListProvider
* */

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {

        return (new WidgetListProvider(this.getApplicationContext(), intent));
    }
}
