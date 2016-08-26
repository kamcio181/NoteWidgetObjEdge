package com.apps.home.notewidget;


import android.content.Context;
import android.content.SharedPreferences;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.apps.home.notewidget.customviews.RobotoEditText;
import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.objects.ShoppingListItem;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.DatabaseHelper;
import com.apps.home.notewidget.utils.DividerItemDecoration;
import com.apps.home.notewidget.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;

public class ListFragment extends Fragment{
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
        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
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

            recyclerView.setAdapter(new ListRecyclerAdapter(itemList, 0, new ListRecyclerAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position, boolean longClick) {
                    //TODO
                }
            }));
        } else {
            String content = note.getNote();
            int toBuyNumberOfItems = Integer.parseInt(content.substring(0, content.indexOf("<br/>")));
            ArrayList<String> items = new ArrayList<>();
            items.addAll(Arrays.asList(content.split("<br/>")));
            items.remove(0);

            ArrayList<ShoppingListItem> itemList = new ArrayList<>(items.size()+3);

            itemList.add(new ShoppingListItem("Items to buy", Constants.HEADER_VIEW));
            for (int i = 0; i<toBuyNumberOfItems; i++){
                itemList.add(new ShoppingListItem(items.get(i), Constants.ENABLED_ITEM_VIEW));
            }
            itemList.add(new ShoppingListItem(null, Constants.NEW_ITEM_VIEW));
            itemList.add(new ShoppingListItem("Items bought", Constants.HEADER_VIEW));

            for (int i = toBuyNumberOfItems; i<items.size(); i++){
                itemList.add(new ShoppingListItem(items.get(i), Constants.DISABLED_ITEM_VIEW));
            }

            recyclerView.setAdapter(new ListRecyclerAdapter(itemList, toBuyNumberOfItems,
                    new ListRecyclerAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position, boolean longClick) {

                }
            })); //TODO
        }


        setTitleAndSubtitle();
    }

//    public void updateNoteTextSize(){ //TODO tile size
//        noteEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP,
//                context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).getInt(Constants.NOTE_TEXT_SIZE_KEY, 14));
//    }

//    public void setNote(Note note){
//        this.note = note;
//        noteEditText.setText(Html.fromHtml(note.getNote()));
//        actionBar.setTitle(note.getTitle());
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

//    @Override
//    public void onStop() {
//        super.onStop();
//        Log.e(TAG, "Stop");
//        if(!skipSaving){
//            saveNote(false);
//        }
//    }

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



//    public void saveNote(final boolean quit){
//        Utils.showToast(context.getApplicationContext(), getString(R.string.saving));
//        if(isNewNote) {
//            note.setNote(noteEditText.getText().toString());
//            helper.createNote(note, new DatabaseHelper.OnItemInsertListener() {
//                @Override
//                public void onItemInserted(long id) {
//                    note.setId(id);
//                    isNewNote = false;
//                    Utils.incrementFolderCount(((MainActivity) context).getNavigationViewMenu(), (int) note.getFolderId(), 1);// TODO
////                    Utils.updateConnectedWidgets(context, note.getId());
////                    Utils.updateAllEdgePanels(context);
//                    if(quit)
//                        ((AppCompatActivity)context).onBackPressed();
//                }
//            });
//        } else{
//            note.setNote(noteEditText.getText().toString());
//            helper.updateNote(note, new DatabaseHelper.OnItemUpdateListener() {
//                @Override
//                public void onItemUpdated(int numberOfRows) {
//                    Utils.updateConnectedWidgets(context, note.getId());
//                    Utils.updateAllEdgePanels(context);
//                    if(quit)
//                        ((AppCompatActivity)context).onBackPressed();
//                }
//            });
//        }
//    }

//    public void setFolderId(int folderId) {
//        note.setFolderId(folderId);
//    }
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
//    public void titleChanged(String title) {
//        note.setTitle(title);
//    }
//
//    public String getNoteText(){
//        if(noteEditText.getText().length() == 0) {
//            Utils.showToast(context, context.getString(R.string.note_is_empty_or_was_not_loaded_yet));
//        }
//        return noteEditText.getText().toString();
//    }
}

class ListRecyclerAdapter extends RecyclerView.Adapter<ListRecyclerAdapter.SingleLineWithHandleViewHolder> {
    private static OnItemClickListener listener;
    private ArrayList<ShoppingListItem> items;
    private int activeItemsCount;


    public interface OnItemClickListener{
        void onItemClick(View view, int position, boolean longClick);
    }

    public ListRecyclerAdapter(ArrayList<ShoppingListItem> items, int activeItemsCount,
                               OnItemClickListener listener) {
        this.items = items;
        this.activeItemsCount = activeItemsCount;
        this.listener = listener;
        setHasStableIds(true);
    }

//    public void setNotes(ArrayList<Note> notes){
//        this.notes = notes;
//        notifyDataSetChanged();
//    }

    @Override
    public SingleLineWithHandleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = new View(parent.getContext());
        switch (viewType){
            case Constants.DISABLED_ITEM_VIEW:
            case Constants.ENABLED_ITEM_VIEW:
                Log.e("ListFragment", "Normal item");
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
            case Constants.ENABLED_ITEM_VIEW:
                onBindItemViewHolder(holder, position);
                break;
            case Constants.HEADER_VIEW:
                onBindHeaderViewHolder(holder, position);
                break;
            case Constants.NEW_ITEM_VIEW:
                onBindNewItemViewHolder(holder, position);
                break;
        }
    }

    private void onBindHeaderViewHolder(SingleLineWithHandleViewHolder holder, int position){
        holder.header.setText(items.get(position).getContent());
    }

    private void onBindNewItemViewHolder(final SingleLineWithHandleViewHolder holder, int position){
        holder.confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(holder.newItemEditText.getText().toString().length() >0){
                    items.add(activeItemsCount + 1, new ShoppingListItem(holder.newItemEditText.getText().toString(), Constants.ENABLED_ITEM_VIEW)); //+1 because header is 1st item
                    holder.newItemEditText.setText("");
                    notifyItemInserted(activeItemsCount);
                    activeItemsCount++;
                }
            }
        });
    }

    private void onBindItemViewHolder(final SingleLineWithHandleViewHolder holder, final int position){
        holder.titleTextView.setText(items.get(position).getContent());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(holder.getItemViewType() == Constants.ENABLED_ITEM_VIEW){
                    items.get(position).setViewType(Constants.DISABLED_ITEM_VIEW);
                    items.add(items.remove(position)); //TODO
                    activeItemsCount--;
                    notifyItemMoved(position, items.size()-1);
                } else {
                    items.get(position).setViewType(Constants.ENABLED_ITEM_VIEW);
                    items.add(activeItemsCount, items.remove(position));
                    activeItemsCount++;
                    notifyItemMoved(position, activeItemsCount-1);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getViewType();
    }

    static class SingleLineWithHandleViewHolder extends RecyclerView.ViewHolder{
        public RobotoTextView titleTextView, header;
        public ImageView handle, confirm;
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
        }
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getId();
    }

    public int getActiveItemsCount() {
        return activeItemsCount;
    }
}

