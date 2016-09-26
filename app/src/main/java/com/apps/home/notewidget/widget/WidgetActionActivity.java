package com.apps.home.notewidget.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.apps.home.notewidget.MainActivity;
import com.apps.home.notewidget.R;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.DividerItemDecoration;

public class WidgetActionActivity extends AppCompatActivity {
    private static final String TAG = "WidgetActionActivity";
    private RecyclerView recyclerView;
    private String[] items;
    private int appWidgetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_action);

        if(getIntent() != null)
            appWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

        items = getResources().getStringArray(R.array.widget_actions);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new SingleLineViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.single_line_item, parent, false));
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                ((SingleLineViewHolder)holder).titleTextView.setText(items[position]);
            }

            @Override
            public int getItemCount() {
                return items.length;
            }
        });
    }

    class SingleLineViewHolder extends RecyclerView.ViewHolder{
        public final AppCompatTextView titleTextView;

        public SingleLineViewHolder(View itemView){
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                switch (getLayoutPosition()){
                    case 0:
                        Intent intent = new Intent(WidgetActionActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
                        startActivity(intent);
                        break;
                    case 1:
                        Intent configIntent = new Intent(WidgetActionActivity.this, WidgetConfigActivity.class);
                        configIntent.setAction(WidgetProvider.ACTION_WIDGET_CONFIGURE);
                        configIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                        configIntent.putExtra(Constants.RECONFIGURE, true);
                        startActivity(configIntent);
                        break;
                }
                    finish();
                }
            });
            titleTextView = (AppCompatTextView) itemView.findViewById(R.id.textView2);
        }
    }
}
