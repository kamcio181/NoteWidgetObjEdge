package com.apps.home.notewidget.widget;

import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.objects.Widget;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.DatabaseHelper2;
import com.apps.home.notewidget.utils.Utils;

import java.util.ArrayList;

public class WidgetConfigActivity extends AppCompatActivity implements
        AdapterView.OnItemClickListener{
    private int widgetID = 0;
    private ListView notesListView;
    private DatabaseHelper2 helper;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_widget_config);
        setResult(RESULT_CANCELED);

        Utils.hideShadowSinceLollipop(this);

        notesListView = (ListView) findViewById(R.id.notesListView);

        Bundle extras = getIntent().getExtras();
        if(extras != null){
            widgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            Log.e("config onCreate", "widgetId "+ widgetID);
        }

        helper = new DatabaseHelper2(this);
        helper.getNotes(false, new DatabaseHelper2.OnNotesLoadListener() {
            @Override
            public void onNotesLoaded(ArrayList<Note> notes) {
                if(notes != null){
                    notesListView.setAdapter(new ListViewAdapter(notes));
                    notesListView.setOnItemClickListener(WidgetConfigActivity.this);
                } else{
                    Utils.showToast(WidgetConfigActivity.this,
                            "Database unavailable or you do not have notes");
                    finish();
                }
            }
        });
    }

    class ListViewAdapter extends BaseAdapter{
        private ArrayList<Note> items;

        public ListViewAdapter(ArrayList<Note> items) {
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return items.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row;
            if(convertView == null)
                row = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_line_item,
                        parent, false);
            else
                row = convertView;

            ((RobotoTextView)row.findViewById(R.id.textView2)).setText(items.get(position).
                    getTitle());

            return row;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        Widget widget = new Widget();
        widget.setWidgetId(widgetID);
        widget.setNoteId(l);
        helper.createWidget(widget, new DatabaseHelper2.OnItemInsertListener() {
            @Override
            public void onItemInserted(long id) {
                if(id >= 0){
                    getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).edit().
                            putBoolean(widgetID + Constants.CONFIGURED_KEY, true).commit();

                    WidgetProvider widgetProvider = new WidgetProvider();
                    widgetProvider.onUpdate(WidgetConfigActivity.this,
                            AppWidgetManager.getInstance(WidgetConfigActivity.this),
                            new int[]{widgetID});

                    Intent intent = new Intent();
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
                    setResult(RESULT_OK, intent);
                }
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
