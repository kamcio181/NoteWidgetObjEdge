package com.apps.home.notewidget;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;

import com.apps.home.notewidget.customviews.RobotoEditText;
import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.objects.ShoppingListItem;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.DatabaseHelper;
import com.apps.home.notewidget.utils.DividerItemDecoration;
import com.apps.home.notewidget.utils.FolderChangeListener;
import com.apps.home.notewidget.utils.ItemTouchHelperAdapter;
import com.apps.home.notewidget.utils.ItemTouchHelperViewHolder;
import com.apps.home.notewidget.utils.NoteUpdateListener;
import com.apps.home.notewidget.utils.OnStartDragListener;
import com.apps.home.notewidget.utils.SimpleItemTouchHelperCallback;
import com.apps.home.notewidget.utils.TitleChangeListener;
import com.apps.home.notewidget.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;

public class ListFragment extends Fragment implements TitleChangeListener, NoteUpdateListener, FolderChangeListener{
    private static final String TAG = "ListFragment";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private RecyclerView recyclerView;
    private boolean skipSaving = false;
    private boolean isNewNote;
    private Context context;
    private boolean skipTextCheck = false;
    private int editTextSelection;
    private String newLine;
    private Note note;
    private DatabaseHelper helper;
    private ActionBar actionBar;
    private ItemTouchHelper itemTouchHelper;


    public ListFragment() {
        // Required empty public constructor
    }

    public static ListFragment newInstance(boolean isNewNote, Note note) {
        ListFragment fragment = new ListFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PARAM1, isNewNote);
        args.putSerializable(ARG_PARAM2, note);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isNewNote = getArguments().getBoolean(ARG_PARAM1);
            note = (Note) getArguments().getSerializable(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();
        helper = new DatabaseHelper(context);
        ((AppCompatActivity)context).invalidateOptionsMenu(); //TODO
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_note_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        if(!context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).
//                getBoolean(Constants.SKIP_MULTILEVEL_NOTE_MANUAL_DIALOG_KEY, false))
//            Utils.getMultilevelNoteManualDialog(context).show();

        //TODO tile size

        recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
//        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
        recyclerView.setHasFixedSize(true);



        newLine = System.getProperty("line.separator");
        //TODO init recycler view

        Log.e(TAG, "skip start " + skipTextCheck);
        if(isNewNote) {
            Log.e(TAG, "skip new " + skipTextCheck);
            note.setTitle(getString(R.string.untitled));
            note.setCreatedAt(Calendar.getInstance().getTimeInMillis());
            note.setDeletedState(Constants.FALSE);

            ArrayList<ShoppingListItem> itemList = new ArrayList<>(3);

            itemList.add(new ShoppingListItem("Items to buy", Constants.HEADER_VIEW));
            itemList.add(new ShoppingListItem(null, Constants.NEW_ITEM_VIEW));
            itemList.add(new ShoppingListItem("Items bought", Constants.HEADER_VIEW));

            recyclerView.setAdapter(new ListRecyclerAdapter(itemList, 0, new OnStartDragListener() {
                @Override
                public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                    itemTouchHelper.startDrag(viewHolder);
                }
            }));
        } else {
            setRecyclerViewItems();
        }

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback((ItemTouchHelperAdapter) recyclerView.getAdapter());
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        setTitleAndSubtitle();
    }

    private void setRecyclerViewItems(){
        String content = note.getNote();
        int activeItemsCount = Integer.parseInt(content.substring(0, content.indexOf("<br/>")));
        ArrayList<String> items = new ArrayList<>();
        items.addAll(Arrays.asList(content.split("<br/>")));
        items.remove(0);

        ArrayList<ShoppingListItem> itemList = new ArrayList<>(items.size()+3);

        itemList.add(new ShoppingListItem("Items to buy", Constants.HEADER_VIEW));
        for (int i = 0; i<activeItemsCount; i++){
            itemList.add(new ShoppingListItem(items.get(i), Constants.ENABLED_ITEM_VIEW));
        }
        itemList.add(new ShoppingListItem(null, Constants.NEW_ITEM_VIEW));
        itemList.add(new ShoppingListItem("Items bought", Constants.HEADER_VIEW));

        for (int i = activeItemsCount; i<items.size(); i++){
            itemList.add(new ShoppingListItem(items.get(i), Constants.DISABLED_ITEM_VIEW));
        }

        if(recyclerView.getAdapter() == null)
            recyclerView.setAdapter(new ListRecyclerAdapter(itemList, activeItemsCount,
                    new OnStartDragListener() {
                        @Override
                        public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                            itemTouchHelper.startDrag(viewHolder);
                        }
                    }));
        else
            ((ListRecyclerAdapter)recyclerView.getAdapter()).setItems(itemList, activeItemsCount);
    }

//    public void updateNoteTextSize(){ //TODO tile size
//        noteEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP,
//                context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).getInt(Constants.NOTE_TEXT_SIZE_KEY, 14));
//    }


    private void setTitleAndSubtitle(){
        actionBar = ((AppCompatActivity) context).getSupportActionBar();
        if(actionBar != null){
            actionBar.setTitle(note.getTitle());
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(note.getCreatedAt());
            Log.e(TAG, "millis " + note.getCreatedAt());
            actionBar.setSubtitle(String.format("%1$tb %1$te, %1$tY %1$tT", calendar));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.e(TAG, "Stop");
        if(!skipSaving){
            saveList(false);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.e(TAG, "onDetach");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
    }

    @Override
    public void onTitleChanged(String newTitle) {
        note.setTitle(newTitle);
    }

    @Override
    public void onNoteUpdate(Note newNote) {
        this.note = newNote;
        setRecyclerViewItems();
        actionBar.setTitle(note.getTitle());
    }


    public void saveList(final boolean quit){
        Utils.showToast(context.getApplicationContext(), getString(R.string.saving));
        note.setNote(((ListRecyclerAdapter)recyclerView.getAdapter()).getStringFromList());
        if(isNewNote) {

            helper.createNote(note, new DatabaseHelper.OnItemInsertListener() {
                @Override
                public void onItemInserted(long id) {
                    note.setId(id);
                    isNewNote = false;
                    Utils.incrementFolderCount(((MainActivity) context).getNavigationViewMenu(), (int) note.getFolderId(), 1);// TODO

                    if(quit)
                        ((AppCompatActivity)context).onBackPressed();
                }
            });
        } else{
            helper.updateNote(note, new DatabaseHelper.OnItemUpdateListener() {
                @Override
                public void onItemUpdated(int numberOfRows) {
                    Utils.updateConnectedWidgets(context, note.getId());
                    Utils.updateAllEdgePanels(context);
                    if(quit)
                        ((AppCompatActivity)context).onBackPressed();
                }
            });
        }
    }

    @Override
    public void onFolderChanged(int newFolderId) {
        note.setFolderId(newFolderId);
    }
//
//    public void deleteNote() {
//        skipSaving = true;
//        Utils.showToast(context, context.getString(R.string.moving_to_trash));
//        note.setNote(noteEditText.getText().toString());
//        note.setDeletedState(Constants.TRUE);
//        helper.updateNote(note, new DatabaseHelper.OnItemUpdateListener() {
//            @Override
//            public void onItemUpdated(int numberOfRows) {
//                if (numberOfRows > 0) {
//                    Utils.updateConnectedWidgets(context, note.getId()); //TODO update and res
//                    Utils.updateAllEdgePanels(context);
//                    SharedPreferences preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
//                    preferences.edit().putString(Constants.EDGE_VISIBLE_NOTES_KEY,preferences.getString(Constants.EDGE_VISIBLE_NOTES_KEY,"").replace(";" + note.getId() + ";", ";")).apply();
//
//                    Menu menu = ((MainActivity) context).getNavigationViewMenu();
//                    Utils.incrementFolderCount(menu, (int) Utils.getTrashNavId(context), 1);
//                    Utils.decrementFolderCount(menu, (int) note.getFolderId(), 1);
//                }
//                ((AppCompatActivity)context).onBackPressed();
//            }
//        });
//    }

//    public void discardChanges(){
//        skipSaving = true;
//        Utils.showToast(context, context.getString(R.string.closed_without_saving));
//        ((AppCompatActivity)context).onBackPressed();
//    }
//
//
//    public String getNoteText(){
//        if(noteEditText.getText().length() == 0) {
//            Utils.showToast(context, context.getString(R.string.note_is_empty_or_was_not_loaded_yet));
//        }
//        return noteEditText.getText().toString();
//    }
}

class ListRecyclerAdapter extends RecyclerView.Adapter<ListRecyclerAdapter.SingleLineWithHandleViewHolder>
        implements ItemTouchHelperAdapter{
    private static final String TAG = "ListRecyclerAdapter";
    private OnStartDragListener listener;
    private ArrayList<ShoppingListItem> items;
    private int activeItemsCount;

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(items, fromPosition, toPosition);
        notifyDataSetChanged();
        return true;
    }

    public ListRecyclerAdapter(ArrayList<ShoppingListItem> items, int activeItemsCount,
                               OnStartDragListener listener) {
        this.items = items;
        this.activeItemsCount = activeItemsCount;
        this.listener = listener;
        setHasStableIds(true);
    }

    public void setItems(ArrayList<ShoppingListItem> items, int activeItemsCount){
        this.items = items;
        this.activeItemsCount = activeItemsCount;
        notifyDataSetChanged();
    }

    @Override
    public SingleLineWithHandleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = new View(parent.getContext());
        switch (viewType){
            case Constants.DISABLED_ITEM_VIEW:
                Log.e("ListFragment", "Disabled item");
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_line_disabled_recycler_view_item, parent, false);

                break;
            case Constants.ENABLED_ITEM_VIEW:
                Log.e("ListFragment", "Enabled item");
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_line_with_handle_recycle_view_item, parent, false);
                    break;
            case Constants.HEADER_VIEW:
                Log.e("ListFragment", "Header");
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_recycler_view_item, parent, false); //TODO header view and UI
                break;
            case Constants.NEW_ITEM_VIEW:
                Log.e("ListFragment", "New item");
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.new_item_recycle_view_item, parent, false);
                break;
        }

        return new SingleLineWithHandleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SingleLineWithHandleViewHolder holder, int position) {
        switch (holder.getItemViewType()){
            case Constants.DISABLED_ITEM_VIEW:
                onBindDisabledItemViewHolder(holder, position);
                break;
            case Constants.ENABLED_ITEM_VIEW:
                onBindEnabledItemViewHolder(holder, position);
                break;
            case Constants.HEADER_VIEW:
                onBindHeaderViewHolder(holder, position);
                break;
            case Constants.NEW_ITEM_VIEW:
                onBindNewItemViewHolder(holder);
                break;
        }
    }

    private void onBindHeaderViewHolder(SingleLineWithHandleViewHolder holder, int position){
        holder.header.setText(items.get(position).getContent());
    }

    private void onBindNewItemViewHolder(final SingleLineWithHandleViewHolder holder){
        holder.confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(holder.newItemEditText.getText().toString().length() >0){
                    insertItem(holder);
                }
            }
        });
        holder.newItemEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    if(holder.newItemEditText.getText().toString().length() > 0){
                        insertItem(holder);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private void insertItem(SingleLineWithHandleViewHolder holder){
        items.add(activeItemsCount + 1, new ShoppingListItem(holder.newItemEditText.getText().toString(), Constants.ENABLED_ITEM_VIEW)); //+1 because header is 1st item
        holder.newItemEditText.setText("");
        activeItemsCount++;
        notifyDataSetChanged();
    }

    private void onBindEnabledItemViewHolder(final SingleLineWithHandleViewHolder holder, final int position){
        holder.titleTextView.setText(items.get(position).getContent());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("adapter", "position " +position);
                items.get(position).setViewType(Constants.DISABLED_ITEM_VIEW);
                ShoppingListItem item = items.remove(position);
                items.add(item);
                activeItemsCount--;
                notifyDataSetChanged();
            }
        });

        holder.handle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEventCompat.getActionMasked(event) ==
                        MotionEvent.ACTION_DOWN) {
                    if(listener != null)
                        listener.onStartDrag(holder);
                }
                return false;
            }
        });
    }

    private void onBindDisabledItemViewHolder(final SingleLineWithHandleViewHolder holder, final int position){
        holder.titleTextView.setText(items.get(position).getContent());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("adapter", "position " +position);

                items.get(position).setViewType(Constants.ENABLED_ITEM_VIEW);
                items.add(activeItemsCount+1, items.remove(position));
                activeItemsCount++;
                notifyDataSetChanged();
            }
        });

        if(position == items.size()-1)
            holder.divider.setVisibility(View.GONE);
        else
            holder.divider.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getViewType();
    }

    static class SingleLineWithHandleViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {
        public RobotoTextView titleTextView, header;
        public ImageView handle, confirm, divider;
        public RobotoEditText newItemEditText;

        public SingleLineWithHandleViewHolder(final View itemView){
            super(itemView);


//            itemView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if(listener != null)
//                        listener.onItemClick(itemView, getLayoutPosition(), false);
//                }
//            });
//            itemView.setOnLongClickListener(new View.OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View v) {
//                    if(listener != null)
//                        listener.onItemClick(itemView, getLayoutPosition(), true);
//                    return true;
//                }
//            });

            titleTextView = (RobotoTextView) itemView.findViewById(R.id.textView2);
            handle = (ImageView) itemView.findViewById(R.id.imageView2);
            header = (RobotoTextView) itemView.findViewById(R.id.textView);
            newItemEditText = (RobotoEditText) itemView.findViewById(R.id.editText);
            confirm = (ImageView) itemView.findViewById(R.id.imageView);
            divider = (ImageView) itemView.findViewById(R.id.imageView3);
        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(Color.CYAN);
        }

        @Override
        public void onItemClear() {
            itemView.setBackgroundColor(0);
        }
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getId();
    }

    public String getStringFromList() {
        StringBuilder builder = new StringBuilder();
        builder.append(activeItemsCount).append("<br/>");
        for (int i = 1; i < activeItemsCount + 1; i++)
            builder.append(items.get(i).getContent()).append("<br/>");
        for (int i = activeItemsCount + 3; i < items.size(); i++)
            builder.append(items.get(i).getContent()).append("<br/>");
        Log.e(TAG, "items " + builder.toString());
        return builder.toString();
    }
}
