package com.apps.home.notewidget.edge;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;

public class EdgeConfigActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener{
    private static final String TAG = "EdgeConfigActivity";
    private static SharedPreferences preferences;
    private RecyclerView notesRV, edgeRV;
    private SwitchCompat ignoreTabsSwitch;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edge_config);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        preferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        ignoreTabsSwitch = (SwitchCompat) findViewById(R.id.switch1);
        notesRV = (RecyclerView) findViewById(R.id.recycler_view1);
        edgeRV = (RecyclerView) findViewById(R.id.recycler_view2);

        ignoreTabsSwitch.setOnCheckedChangeListener(this);
        ignoreTabsSwitch.setChecked(preferences.getBoolean(Constants.IGNORE_TABS_IN_WIDGETS_KEY, false));

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()){
            case R.id.switch1:
                preferences.edit().putBoolean(Constants.IGNORE_TABS_IN_EDGE_PANEL_KEY, isChecked).apply();
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        //TODO save checked items to prefs

        Utils.updateAllEdgePanels(this);
    }
}
class CheckableItemsAdapter extends RecyclerView.Adapter<CheckableItemsAdapter.ViewHolder> { //TODO saveCheckedArray onStop
    private static final String TAG = "CheckableItemsAdapter";
    private static ArrayList<Note> notes;
    private static boolean[] checkedArray;
    private static String[] contents;

    public CheckableItemsAdapter(ArrayList<Note> notes, String checked) {//checked format ";int;int;int;"
        CheckableItemsAdapter.notes = notes;

        checkedArray = new boolean[notes.size()];
        for(int i = 0; i < checkedArray.length; i++) {
            checkedArray[i] = checked.contains(";" + notes.get(i).getId() + ";");
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.double_line_check_recycle_view_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.titleTV.setText(note.getTitle());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(note.getCreatedAt());
        holder.contentTV.setText(String.format("%1$tb %1$te, %1$tY %1$tT", calendar));
        holder.checkBox.setChecked(checkedArray[position]);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        public CheckBox checkBox;
        public TextView titleTV;
        public TextView contentTV;

        public ViewHolder(final View itemView) {
            super(itemView);
            titleTV = (TextView) itemView.findViewById(R.id.textView2);
            contentTV = (TextView) itemView.findViewById(R.id.textView3);
            checkBox = (CheckBox) itemView.findViewById(R.id.checkBox2);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkedArray[getLayoutPosition()] = !checkedArray[getLayoutPosition()];
                    CheckableItemsAdapter.this.notifyItemChanged(getLayoutPosition());
                }
            });
        }
    }

    public String getChekedNotes () {
        StringBuilder builder = new StringBuilder();
        builder.append(";");
        for(int i = 0; i<checkedArray.length; i++){
            if(checkedArray[i]){
                builder.append(notes.get(i).getId()).append(";");
            }
        }
        String result = builder.toString();
        if(result.length()>2)
            return result;
        return null;
    }
}
