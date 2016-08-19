package com.apps.home.notewidget.edge;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
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
import com.apps.home.notewidget.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

public class EdgeConfigActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener{
    private static final String TAG = "EdgeConfigActivity";
    private static SharedPreferences preferences;
    private RecyclerView notesRV, edgeRV;
    private SwitchCompat ignoreTabsSwitch;
    private ItemTouchHelper itemTouchHelper;
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
        final boolean ignoreTabs = preferences.getBoolean(Constants.IGNORE_TABS_IN_WIDGETS_KEY, false);
        ignoreTabsSwitch.setChecked(ignoreTabs);

        DatabaseHelper helper = new DatabaseHelper(this);
        helper.getNotes(false, new DatabaseHelper.OnNotesLoadListener() {
            @Override
            public void onNotesLoaded(final ArrayList<Note> notes) {
                if(notes != null){
                    String notesVisibleOnEdge = preferences.getString(Constants.EDGE_VISIBLE_NOTES, "");
                    String order = preferences.getString(Constants.EDGE_NOTES_ORDER, null);
                    String[] orderArray = new String[0];
                    if(order != null)
                        orderArray = order.trim().split(";");


                    edgeRV.setLayoutManager(new LinearLayoutManager(EdgeConfigActivity.this));
                    edgeRV.addItemDecoration(new DividerItemDecoration(EdgeConfigActivity.this, DividerItemDecoration.VERTICAL_LIST));
                    edgeRV.setHasFixedSize(true);

                    String dummy = notesVisibleOnEdge;
                    final ArrayList<Note> visibleNotes = new ArrayList<>();
                    ArrayList<Note> orderedNotes = new ArrayList<>();
                    if(dummy.length()>2){
                        for(Note n : notes){
                            if(dummy.contains(";" + n.getId() + ";")){
                                visibleNotes.add(n);
                                dummy = dummy.replace(";" + n.getId() + ";", ";");
                            }
                        }
                        if(orderArray.length>0) {
                            for (String idString : orderArray) {
                                int id = Integer.parseInt(idString);
                                for (int j = 0; j < visibleNotes.size(); j++) {
                                    if (id == visibleNotes.get(j).getId()) {
                                        orderedNotes.add(visibleNotes.get(j));
                                        visibleNotes.remove(j);
                                        break;
                                    }
                                }
                            }
                            orderedNotes.addAll(visibleNotes);
                        } else {
                            orderedNotes = visibleNotes;
                        }
                    }

                    edgeRV.setAdapter(new EdgeAdapter(orderedNotes, ignoreTabs, new EdgeAdapter.OnStartDragListener() {
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
                            if(checked){
                                visibleNotes.add(note);
                                edgeRV.getAdapter().notifyItemInserted(visibleNotes.size()-1);
                            } else {
                                int position = EdgeAdapter.getItemPosition(note.getId());
                                if(position >=0) {
                                    visibleNotes.remove(position);
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

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.config_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_apply:
                finish();
                break;
        }
        return true;
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()){
            case R.id.switch1:
                preferences.edit().putBoolean(Constants.IGNORE_TABS_IN_EDGE_PANEL_KEY, isChecked).apply();
                EdgeAdapter.setIgnoreTabs(isChecked);
                edgeRV.getAdapter().notifyDataSetChanged();
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(notesRV.getAdapter()!= null) {
            Log.e(TAG, "adapter is present");
            preferences.edit().putString(Constants.EDGE_VISIBLE_NOTES, ((CheckableItemsAdapter) notesRV.getAdapter()).getCheckedNotes()).putString(Constants.EDGE_NOTES_ORDER, ((EdgeAdapter)edgeRV.getAdapter()).getNotesOrder()).apply();

        }
        Utils.updateAllEdgePanels(this);
    }
}
class CheckableItemsAdapter extends RecyclerView.Adapter<CheckableItemsAdapter.ViewHolder> {
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
        for(int i = 0; i < checkedArray.length; i++) {
            long id = notes.get(i).getId();
            checkedArray[i] = checked.contains(";" + id + ";");
            checked = checked.replace(";" + id + ";", ";");
            Log.e(TAG, "i " + checkedArray[i]);
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
        return builder.toString();
    }
}

class EdgeAdapter extends RecyclerView.Adapter<EdgeAdapter.ViewHolder> {
    private static final String TAG = "EdgeAdapter";
    private static ArrayList<Note> notes;
    private static boolean ignoreTabs;
    private final OnStartDragListener listener;

    public interface OnStartDragListener{
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }


    public EdgeAdapter(ArrayList<Note> notes, boolean ignoreTabs, OnStartDragListener listener) {//checked format ";int;int;int;"
        EdgeAdapter.notes = notes;
        EdgeAdapter.ignoreTabs = ignoreTabs;
        setHasStableIds(true);
        this.listener = listener;
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

        holder.tile.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN){
                    listener.onStartDrag(holder);
                }
                return false;
            }
        });
    }

    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(notes, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    @Override
    public long getItemId(int position) {
        return notes.get(position).getId();
    }

    public static int getItemPosition(long id){
        for (int i = 0; i < notes.size(); i++){
            if(notes.get(i).getId() == id) {
                Log.e(TAG, "ID found");
                return i;
            }
        }
        return -1;
    }

    public static void setIgnoreTabs(boolean ignoreTabs) {
        EdgeAdapter.ignoreTabs = ignoreTabs;
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        public View tile;
        public TextView titleTV;
        public TextView contentTV;

        public ViewHolder(final View itemView) {
            super(itemView);
            this.tile = itemView;
            titleTV = (TextView) itemView.findViewById(R.id.textView7);
            contentTV = (TextView) itemView.findViewById(R.id.textView);

//            itemView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    checkedArray[getLayoutPosition()] = !checkedArray[getLayoutPosition()];
//                    CheckableItemsAdapter.this.notifyItemChanged(getLayoutPosition());
//                }
//            });
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

        Log.e(TAG, builder.toString().substring(0, builder.length()-1));
        return builder.toString().substring(0, builder.length()-1);
    }
}

class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {

    public static final float ALPHA_FULL = 1.0f;

    private final EdgeAdapter mAdapter;

    public SimpleItemTouchHelperCallback(EdgeAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        // Set movement flags based on the layout manager
        if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
            final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            final int swipeFlags = 0;
            return makeMovementFlags(dragFlags, swipeFlags);
        } else {
            final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            final int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            return makeMovementFlags(dragFlags, swipeFlags);
        }
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
        if (source.getItemViewType() != target.getItemViewType()) {
            return false;
        }

        // Notify the adapter of the move
        mAdapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
        // Notify the adapter of the dismissal
//        mAdapter.onItemDismiss(viewHolder.getAdapterPosition());
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            // Fade out the view as it is swiped out of the parent's bounds
            final float alpha = ALPHA_FULL - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
            viewHolder.itemView.setAlpha(alpha);
            viewHolder.itemView.setTranslationX(dX);
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        // We only want the active item to change
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder instanceof EdgeAdapter.ViewHolder) {
                // Let the view holder know that this item is being moved or dragged
                EdgeAdapter.ViewHolder itemViewHolder = (EdgeAdapter.ViewHolder) viewHolder;
                itemViewHolder.onItemSelected();
            }
        }

        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        viewHolder.itemView.setAlpha(ALPHA_FULL);

        if (viewHolder instanceof EdgeAdapter.ViewHolder) {
            // Tell the view holder it's time to restore the idle state
            EdgeAdapter.ViewHolder itemViewHolder = (EdgeAdapter.ViewHolder) viewHolder;
            itemViewHolder.onItemClear();
        }
    }
}
