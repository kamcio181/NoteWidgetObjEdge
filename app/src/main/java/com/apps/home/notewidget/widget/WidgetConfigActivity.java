package com.apps.home.notewidget.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.objects.Widget;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.DatabaseHelper2;
import com.apps.home.notewidget.utils.DividerItemDecoration;
import com.apps.home.notewidget.utils.Utils;

import java.util.ArrayList;

public class WidgetConfigActivity extends AppCompatActivity{
    private int widgetID = 0;
    private RecyclerView notesRecyclerView;
    private DatabaseHelper2 helper;
    private ArrayList<Note> notes;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_widget_config);
        setResult(RESULT_CANCELED);

        Utils.hideShadowSinceLollipop(this);

        notesRecyclerView = (RecyclerView) findViewById(R.id.notesRecyclerView);

        Bundle extras = getIntent().getExtras();
        if(extras != null){
            widgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            Log.e("config onCreate", "widgetId "+ widgetID);
        }

        helper = new DatabaseHelper2(this);
        helper.getNotes(false, new DatabaseHelper2.OnNotesLoadListener() {
            @Override
            public void onNotesLoaded(final ArrayList<Note> notes) {
                if(notes != null){
                    WidgetConfigActivity.this.notes = notes;

                    notesRecyclerView.setLayoutManager(new LinearLayoutManager(WidgetConfigActivity.this));
                    notesRecyclerView.addItemDecoration(new DividerItemDecoration(WidgetConfigActivity.this, DividerItemDecoration.VERTICAL_LIST));
                    notesRecyclerView.setAdapter(new RecyclerView.Adapter() {
                        @Override
                        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                            return new SingleLineViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.single_line_item, parent, false));
                        }

                        @Override
                        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                            ((SingleLineViewHolder) holder).titleTextView.setText(notes.get(position).getTitle());
                        }

                        @Override
                        public int getItemCount() {
                            return notes.size();
                        }
                    });
                } else{
                    Utils.showToast(WidgetConfigActivity.this,
                            "Database unavailable or you do not have notes");
                    finish();
                }
            }
        });
    }

    class SingleLineViewHolder extends RecyclerView.ViewHolder {
        public RobotoTextView titleTextView;

        public SingleLineViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.showToast(WidgetConfigActivity.this, "Creating widget");
                    Widget widget = new Widget();
                    widget.setWidgetId(widgetID);
                    widget.setNoteId(notes.get(getLayoutPosition()).getId());
                    helper.createWidget(widget, new DatabaseHelper2.OnItemInsertListener() {
                        @Override
                        public void onItemInserted(long id) {
                            if (id >= 0) {
                                Log.e("WidgetConfig", "widget inserted");
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
            });

            titleTextView = (RobotoTextView) itemView.findViewById(R.id.textView2);
        }
    }


    @Override
    public void onBackPressed() {
        finish();
    }
}
