package com.apps.home.notewidget.edge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.DatabaseHelper;
import com.apps.home.notewidget.utils.DividerItemDecoration;
import com.apps.home.notewidget.utils.ItemTouchHelperAdapter;
import com.apps.home.notewidget.utils.ItemTouchHelperViewHolder;
import com.apps.home.notewidget.utils.SimpleItemTouchHelperCallback;
import com.apps.home.notewidget.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

public class EdgeConfigActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener{
    public static final String SAVE_CHANGES_ACTION = "com.apps.home.notewidget.SAVE_CHANGES_ACTION";
    public static final String UPDATE_NOTE_TEXT_SIZE = "com.apps.home.notewidget.UPDATE_NOTE_TEXT_SIZE";
    private static final String TAG = "EdgeConfigActivity";
    private static SharedPreferences preferences;
    private static RecyclerView notesRV, edgeRV;
    private static SwitchCompat ignoreTabsSwitch, hideContentOnLockScreenSwitch;
    private static ItemTouchHelper itemTouchHelper;
    private static EdgeVisibilityReceiver receiver;
    private static boolean settingsChanged = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edge_config);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        preferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        ignoreTabsSwitch = (SwitchCompat) findViewById(R.id.switch1);
        hideContentOnLockScreenSwitch = (SwitchCompat) findViewById(R.id.switch2);
        notesRV = (RecyclerView) findViewById(R.id.recycler_view1);
        edgeRV = (RecyclerView) findViewById(R.id.recycler_view2);

        ignoreTabsSwitch.setOnCheckedChangeListener(this);
        hideContentOnLockScreenSwitch.setOnCheckedChangeListener(this);
        final boolean ignoreTabs = preferences.getBoolean(Constants.IGNORE_TABS_IN_WIDGETS_KEY, false);
        ignoreTabsSwitch.setChecked(ignoreTabs);
        hideContentOnLockScreenSwitch.setChecked(preferences.getBoolean(Constants.EDGE_HIDE_CONTENT_KEY, false));

        DatabaseHelper helper = new DatabaseHelper(this);
        helper.getNotes(false, new DatabaseHelper.OnNotesLoadListener() {
            @Override
            public void onNotesLoaded(final ArrayList<Note> notes) {
                if(notes != null){
                    String notesVisibleOnEdge = preferences.getString(Constants.EDGE_VISIBLE_NOTES_KEY, null);

                    final ArrayList<Note> orderedList = getOrderedList(notes, notesVisibleOnEdge, preferences.getString(Constants.EDGE_NOTES_ORDER_KEY, null));

                    edgeRV.setLayoutManager(new LinearLayoutManager(EdgeConfigActivity.this));
                    edgeRV.addItemDecoration(new DividerItemDecoration(EdgeConfigActivity.this, DividerItemDecoration.VERTICAL_LIST));
                    edgeRV.setHasFixedSize(true);
                    edgeRV.setAdapter(new EdgeAdapter(orderedList, ignoreTabs, preferences.getInt(Constants.EDGE_TEXT_SIZE_KEY, 10), new EdgeAdapter.OnStartDragListener() {
                        @Override
                        public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                            itemTouchHelper.startDrag(viewHolder);
                        }
                    }));

                    ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback((EdgeAdapter) edgeRV.getAdapter());
                    itemTouchHelper = new ItemTouchHelper(callback);
                    itemTouchHelper.attachToRecyclerView(edgeRV);


                    notesRV.setLayoutManager(new LinearLayoutManager(EdgeConfigActivity.this));
                    notesRV.addItemDecoration(new DividerItemDecoration(EdgeConfigActivity.this, DividerItemDecoration.VERTICAL_LIST));
                    notesRV.setAdapter(new CheckableItemsAdapter(notes, notesVisibleOnEdge, new CheckableItemsAdapter.OnItemCheckListener() {
                        @Override
                        public void onItemChecked(Note note, boolean checked) {
                            settingsChanged = true;
                            if(checked){
                                orderedList.add(note);
                                edgeRV.getAdapter().notifyItemInserted(orderedList.size()-1);
                            } else {
                                int position = EdgeAdapter.getItemPosition(note.getId());
                                if(position >=0) {
                                    orderedList.remove(position);
                                    edgeRV.getAdapter().notifyItemRemoved(position);
                                } else {
                                    Utils.showToast(EdgeConfigActivity.this, "invalid ID");
                                }
                            }
                        }
                    }));


                } else{
                    Utils.showToast(EdgeConfigActivity.this,
                            getString(R.string.database_unavailable_or_you_do_not_have_notes));
                    finish();
                }
            }
        });

        IntentFilter intentFilter = new IntentFilter(SAVE_CHANGES_ACTION);
        intentFilter.addAction(UPDATE_NOTE_TEXT_SIZE);
        receiver = new EdgeVisibilityReceiver();
        registerReceiver(receiver, intentFilter);
    }

    private ArrayList<Note> getOrderedList(ArrayList<Note> notes, String notesToBeDisplayed, String orderString){
        if(notesToBeDisplayed != null && notesToBeDisplayed.length()>2){
            ArrayList<Note> visibleNotes = new ArrayList<>();
            for(Note n : notes){
                if(notesToBeDisplayed.contains(";" + n.getId() + ";")){
                    visibleNotes.add(n);
                    notesToBeDisplayed = notesToBeDisplayed.replace(";" + n.getId() + ";", ";");
                }
            }

            if(orderString == null)
                return visibleNotes;

            String[] orderArray = orderString.trim().split(";");

            ArrayList<Note> orderedNotes = new ArrayList<>();
            if(orderArray.length>0) {
                for (String idString : orderArray) {
                    long id = Long.parseLong(idString);
                    for (int j = 0; j < visibleNotes.size(); j++) {
                        if (id == visibleNotes.get(j).getId()) {
                            orderedNotes.add(visibleNotes.get(j));
                            visibleNotes.remove(j);
                            break;
                        }
                    }
                }
                orderedNotes.addAll(visibleNotes);
                return orderedNotes;
            } else {
                return visibleNotes;
            }
        }
        return new ArrayList<>();
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        settingsChanged = true;
        switch (buttonView.getId()){
            case R.id.switch1:
                EdgeAdapter.setIgnoreTabs(isChecked);
                edgeRV.getAdapter().notifyDataSetChanged();
                break;
            case R.id.switch2:
                //Dummy case for switch 2
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(receiver);
        saveSettings();
    }

    private void saveSettings(){
        if(settingsChanged) {
            if (notesRV.getAdapter() != null) {
                Log.e(TAG, "adapter is present");
                preferences.edit().putString(Constants.EDGE_VISIBLE_NOTES_KEY, ((CheckableItemsAdapter) notesRV.getAdapter()).getCheckedNotes())
                        .putString(Constants.EDGE_NOTES_ORDER_KEY, ((EdgeAdapter) edgeRV.getAdapter()).getNotesOrder())
                        .putBoolean(Constants.EDGE_IGNORE_TABS_KEY, ignoreTabsSwitch.isChecked())
                        .putBoolean(Constants.EDGE_HIDE_CONTENT_KEY, hideContentOnLockScreenSwitch.isChecked()).apply();

            }
            Utils.updateAllEdgePanels(this);
        }
        settingsChanged = false;
    }

    class EdgeVisibilityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent arg1) {
            if(arg1 != null){
                switch (arg1.getAction()){
                    case EdgeConfigActivity.SAVE_CHANGES_ACTION:
                        saveSettings();
                        break;
                    case EdgeConfigActivity.UPDATE_NOTE_TEXT_SIZE:
                            EdgeAdapter.reloadTextSize();
                            edgeRV.getAdapter().notifyDataSetChanged();
                        break;
                }
            }
        }
    }

    static class EdgeAdapter extends RecyclerView.Adapter<EdgeAdapter.ViewHolder> implements ItemTouchHelperAdapter{
        private static final String TAG = "EdgeAdapter";
        private static ArrayList<Note> notes;
        private static boolean ignoreTabs;
        private static OnStartDragListener listener;
        private static float noteSize;
        private static float titleSize;

        public interface OnStartDragListener{
            void onStartDrag(RecyclerView.ViewHolder viewHolder);
        }


        public EdgeAdapter(ArrayList<Note> notes, boolean ignoreTabs, float noteSize, OnStartDragListener listener) {//checked format ";int;int;int;"
            EdgeAdapter.notes = notes;
            EdgeAdapter.ignoreTabs = ignoreTabs;
            setHasStableIds(true);
            EdgeAdapter.listener = listener;
            EdgeAdapter.noteSize = noteSize;
            titleSize = 1.4f * noteSize;

            Log.i(TAG, "Constructor ");
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.edge_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            Note note = notes.get(position);
            holder.titleTV.setText(note.getTitle());
            holder.contentTV.setText(Html.fromHtml(ignoreTabs? note.getNote().replace("\u0009", "") : note.getNote()));

            holder.titleTV.setTextSize(titleSize);
            holder.contentTV.setTextSize(noteSize);
        }

        public boolean onItemMove(int fromPosition, int toPosition) {
            Log.i(TAG, "onItemMove");
            settingsChanged = true;
            Collections.swap(notes, fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        @Override
        public int getItemCount() {
            Log.i(TAG, "count " + notes.size());
            return notes.size();
        }

        @Override
        public long getItemId(int position) {
            return notes.get(position).getId();
        }

        public static int getItemPosition(long id){
            for (int i = 0; i < notes.size(); i++){
                if(notes.get(i).getId() == id) {
                    Log.i(TAG, "ID found");
                    return i;
                }
            }
            return -1;
        }

        public static void setIgnoreTabs(boolean ignoreTabs) {
            EdgeAdapter.ignoreTabs = ignoreTabs;
        }

        public static void reloadTextSize(){
            EdgeAdapter.noteSize = preferences.getInt(Constants.EDGE_TEXT_SIZE_KEY, 10);
            titleSize = 1.4f * noteSize;
        }

        class ViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder{
            public View tile;
            public TextView titleTV;
            public TextView contentTV;

            public ViewHolder(final View itemView) {
                super(itemView);
                this.tile = itemView;
                titleTV = (TextView) itemView.findViewById(R.id.textView7);
                contentTV = (TextView) itemView.findViewById(R.id.textView);

                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        Log.i(TAG, "LongClick");
                        if(listener!=null)
                            listener.onStartDrag(ViewHolder.this);
                        return true;
                    }
                });
            }

            public void onItemSelected(){
                tile.setBackgroundColor(Color.CYAN);
            }

            public void onItemClear(){
                tile.setBackgroundColor(0);
            }
        }
        public String getNotesOrder() {
            StringBuilder builder = new StringBuilder();
            for(int i = 0; i<notes.size(); i++){
                builder.append(notes.get(i).getId()).append(";");
            }
            Log.i(TAG, "GetNotesOrder length: " + builder.toString().length());
            return builder.toString().length() == 0? null : builder.toString().substring(0, builder.length()-1);
        }
    }

    static class CheckableItemsAdapter extends RecyclerView.Adapter<CheckableItemsAdapter.ViewHolder> {
        private static final String TAG = "CheckableItemsAdapter";
        private static ArrayList<Note> notes;
        private static boolean[] checkedArray;
        private static OnItemCheckListener listener;

        public interface OnItemCheckListener{
            void onItemChecked(Note note, boolean checked);
        }

        public CheckableItemsAdapter(ArrayList<Note> notes, String checked, OnItemCheckListener listener) {//checked format ";int;int;int;"
            CheckableItemsAdapter.notes = notes;
            CheckableItemsAdapter.listener = listener;

            Log.e(TAG, "checked " + checked);
            checkedArray = new boolean[notes.size()];
            if(checked == null) {
                for (int i = 0; i < checkedArray.length; i++)
                    checkedArray[i] = false;
            } else {
                for (int i = 0; i < checkedArray.length; i++) {
                    long id = notes.get(i).getId();
                    checkedArray[i] = checked.contains(";" + id + ";");
                    checked = checked.replace(";" + id + ";", ";");
                    Log.e(TAG, "i " + checkedArray[i]);
                }
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
                        boolean checked = !checkedArray[getLayoutPosition()];
                        checkedArray[getLayoutPosition()] = checked;
                        CheckableItemsAdapter.this.notifyItemChanged(getLayoutPosition());
                        if(listener != null)
                            listener.onItemChecked(notes.get(getLayoutPosition()), checked);
                    }
                });
            }
        }

        public String getCheckedNotes() {
            StringBuilder builder = new StringBuilder();
            builder.append(";");
            Log.e(TAG, checkedArray.toString());
            for(int i = 0; i<checkedArray.length; i++){
                Log.e(TAG, "" +checkedArray[i]);
                if(checkedArray[i]){
                    builder.append(notes.get(i).getId()).append(";");
                }
            }
            return builder.toString().length() == 1? null : builder.toString();
        }
    }
}




