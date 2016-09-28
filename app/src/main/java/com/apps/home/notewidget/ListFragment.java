package com.apps.home.notewidget;


import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContentResolverCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.MimeTypeMap;
import android.widget.TextView;

import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.edge.EdgeConfigActivity;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.objects.ShoppingListItem;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.ContentGetter;
import com.apps.home.notewidget.utils.DatabaseHelper;
import com.apps.home.notewidget.utils.DeleteListener;
import com.apps.home.notewidget.utils.DiscardChangesListener;
import com.apps.home.notewidget.utils.FolderChangeListener;
import com.apps.home.notewidget.utils.ItemTouchHelperAdapter;
import com.apps.home.notewidget.utils.ItemTouchHelperViewHolder;
import com.apps.home.notewidget.utils.NoteUpdateListener;
import com.apps.home.notewidget.utils.OnStartDragListener;
import com.apps.home.notewidget.utils.ParametersUpdateListener;
import com.apps.home.notewidget.utils.SaveListener;
import com.apps.home.notewidget.utils.SimpleItemTouchHelperCallback;
import com.apps.home.notewidget.utils.TitleChangeListener;
import com.apps.home.notewidget.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

public class ListFragment extends Fragment implements TitleChangeListener, NoteUpdateListener,
        FolderChangeListener, DeleteListener, DiscardChangesListener, SaveListener, ContentGetter, ParametersUpdateListener{
    private static final String TAG = "ListFragment";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_PARAM3 = "param3";
    private RecyclerView recyclerView;
    private boolean skipSaving = false;
    private boolean isNewNote;
    private Context context;
    private Note note;
    private DatabaseHelper helper;
    private ItemTouchHelper itemTouchHelper;
    private static EdgeVisibilityReceiver receiver;


    public ListFragment() {
        // Required empty public constructor
    }

    public static ListFragment newInstance(Note note) {
        ListFragment fragment = new ListFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PARAM1, true);
        args.putSerializable(ARG_PARAM2, note);
        fragment.setArguments(args);
        return fragment;
    }

    public static ListFragment newInstance(long noteId) {
        ListFragment fragment = new ListFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PARAM1, false);
        args.putLong(ARG_PARAM3, noteId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isNewNote = getArguments().getBoolean(ARG_PARAM1);
            if(isNewNote)
                note = (Note) getArguments().getSerializable(ARG_PARAM2);
            else
                note = new Note(getArguments().getLong(ARG_PARAM3));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();
        helper = new DatabaseHelper(context);
        ((AppCompatActivity)context).invalidateOptionsMenu();
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_note_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        if(!context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).
//                getBoolean(Constants.SKIP_MULTILEVEL_NOTE_MANUAL_DIALOG_KEY, false))
//            Utils.getMultilevelNoteManualDialog(context).show();

        recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setHasFixedSize(true);


        if(isNewNote) {
            note.setTitle(getString(R.string.untitled));
            note.setCreatedAt(Calendar.getInstance().getTimeInMillis());
            note.setDeletedState(Constants.FALSE);

            ArrayList<ShoppingListItem> itemList = new ArrayList<>(3);

            itemList.add(new ShoppingListItem(context.getString(R.string.items_to_buy), Constants.HEADER_VIEW));
            itemList.add(new ShoppingListItem(null, Constants.NEW_ITEM_VIEW));
            itemList.add(new ShoppingListItem(context.getString(R.string.items_bought), Constants.HEADER_VIEW));

            recyclerView.setAdapter(new ListRecyclerAdapter(context, itemList, 0, new OnStartDragListener() {
                @Override
                public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                    itemTouchHelper.startDrag(viewHolder);
                    ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback((ItemTouchHelperAdapter) recyclerView.getAdapter());
                    itemTouchHelper = new ItemTouchHelper(callback);
                    itemTouchHelper.attachToRecyclerView(recyclerView);
                }
            }));
            setTitleAndSubtitle();
        } else {
            loadNote();
        }
    }

    private void loadNote(){
        Log.e(TAG, "load note " + note.getId());
        helper.getNote(true, note.getId(), new DatabaseHelper.OnNoteLoadListener() {
            @Override
            public void onNoteLoaded(Note note) {
                Log.e(TAG, "IS note null " + (note == null) ); //TODO check crash
                ListFragment.this.note = note;
                setRecyclerViewItems();
                setTitleAndSubtitle();
            }
        });
    }

    private void setRecyclerViewItems(){
        String content = note.getNote();
        int activeItemsCount = Integer.parseInt(content.substring(0, content.indexOf("<br/>")));
        ArrayList<String> items = new ArrayList<>();
        items.addAll(Arrays.asList(content.split("<br/>")));
        items.remove(0);

        ArrayList<ShoppingListItem> itemList = new ArrayList<>(items.size()+3);

        itemList.add(new ShoppingListItem(context.getString(R.string.items_to_buy), Constants.HEADER_VIEW));
        for (int i = 0; i<activeItemsCount; i++){
            itemList.add(new ShoppingListItem(items.get(i), Constants.ENABLED_ITEM_VIEW));
        }
        itemList.add(new ShoppingListItem(null, Constants.NEW_ITEM_VIEW));
        itemList.add(new ShoppingListItem(context.getString(R.string.items_bought), Constants.HEADER_VIEW));

        for (int i = activeItemsCount; i<items.size(); i++){
            itemList.add(new ShoppingListItem(items.get(i), Constants.DISABLED_ITEM_VIEW));
        }

        if(recyclerView.getAdapter() == null) {
            recyclerView.setAdapter(new ListRecyclerAdapter(context, itemList, activeItemsCount,
                    new OnStartDragListener() {
                        @Override
                        public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                            itemTouchHelper.startDrag(viewHolder);
                        }
                    }));
            ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback((ItemTouchHelperAdapter) recyclerView.getAdapter());
            itemTouchHelper = new ItemTouchHelper(callback);
            itemTouchHelper.attachToRecyclerView(recyclerView);
        }
        else
            ((ListRecyclerAdapter)recyclerView.getAdapter()).setItems(itemList, activeItemsCount);
    }


    private void setTitleAndSubtitle(){
        ActionBar actionBar = ((AppCompatActivity) context).getSupportActionBar();
        if(actionBar != null){
            actionBar.setTitle(note.getTitle());
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(note.getCreatedAt());
            Log.e(TAG, "millis " + note.getCreatedAt());
            actionBar.setSubtitle(String.format("%1$tb %1$te, %1$tY %1$tT", calendar));
        }
    }

    @Override
    public void onParametersUpdated() {
        ((ListRecyclerAdapter)recyclerView.getAdapter()).refreshParameters();
    }

    class EdgeVisibilityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent arg1) {
            if(arg1 != null){
                switch (arg1.getAction()){
                    case EdgeConfigActivity.SAVE_CHANGES_ACTION:
                        saveNote(false);
                        break;
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        receiver = new EdgeVisibilityReceiver();
        context.registerReceiver(receiver, new IntentFilter(EdgeConfigActivity.SAVE_CHANGES_ACTION));
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.e(TAG, "Stop, skip saving" + skipSaving);
        if(!skipSaving){
            saveNote(false);
        }

        try {
            context.unregisterReceiver(receiver);
        } catch (IllegalArgumentException e){
            Log.e(TAG, "Receiver already unregistered");
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
    public void onNoteUpdate() {
        loadNote();
    }

    @Override
    public void onFolderChanged(int newFolderId) {
        note.setFolderId(newFolderId);
    }

    @Override
    public void deleteNote() {
        skipSaving = true;
        Utils.showToast(context, context.getString(R.string.moving_to_trash));
        note.setNote(((ListRecyclerAdapter)recyclerView.getAdapter()).getStringFromList());
        note.setDeletedState(Constants.TRUE);
        helper.updateNote(note, new DatabaseHelper.OnItemUpdateListener() {
            @Override
            public void onItemUpdated(int numberOfRows) {
                if (numberOfRows > 0) {
                    Utils.updateConnectedWidgets(context, note.getId()); //TODO update and res
                    Utils.updateAllEdgePanels(context);
                    SharedPreferences preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
                    preferences.edit().putString(Constants.EDGE_VISIBLE_NOTES_KEY,preferences.getString(Constants.EDGE_VISIBLE_NOTES_KEY,"").replace(";" + note.getId() + ";", ";")).apply();

                    Menu menu = ((MainActivity) context).getNavigationViewMenu();
                    Utils.incrementFolderCount(menu, (int) Utils.getTrashNavId(context), 1);
                    Utils.decrementFolderCount(menu, (int) note.getFolderId(), 1);
                }
                ((AppCompatActivity)context).onBackPressed();
            }
        });
    }

    @Override
    public void discardChanges() {
        skipSaving = true;
        Utils.showToast(context, context.getString(R.string.closed_without_saving));
        ((AppCompatActivity)context).onBackPressed();
    }

    @Override
    public void saveNote(final boolean quitAfterSaving) {
        Utils.showToast(context.getApplicationContext(), getString(R.string.saving));
        note.setNote(((ListRecyclerAdapter)recyclerView.getAdapter()).getStringFromList());
        if(isNewNote) {
            helper.createNote(note, new DatabaseHelper.OnItemInsertListener() {
                @Override
                public void onItemInserted(long id) {
                    Log.e(TAG, "note saved " + id);
                    note.setId(id);
                    isNewNote = false;
                    Utils.incrementFolderCount(((MainActivity) context).getNavigationViewMenu(), (int) note.getFolderId(), 1);

                    if(quitAfterSaving)
                        ((AppCompatActivity)context).onBackPressed();
                }
            });
        } else{
            helper.updateNote(note, new DatabaseHelper.OnItemUpdateListener() {
                @Override
                public void onItemUpdated(int numberOfRows) {
                    Log.e(TAG, "note saved ");
                    Utils.updateConnectedWidgets(context, note.getId());
                    Utils.updateAllEdgePanels(context);
                    if(quitAfterSaving)
                        ((AppCompatActivity)context).onBackPressed();
                }
            });
        }
    }

    @Override
    public String getContent() {
        String content = ((ListRecyclerAdapter)recyclerView.getAdapter()).getActiveItemsFromList();
        if(content.length() == 0) {
            Utils.showToast(context, context.getString(R.string.note_is_empty_or_was_not_loaded_yet));
        }
        return content;
    }

    public void removeDisabledItems(){
        ((ListRecyclerAdapter)recyclerView.getAdapter()).removeDisabledItems();
    }

    public void addItemsFromClipboard(){
        String text = getTextFromClipboard();
        
        if (text != null){
            String[] items = text.split("\n");
            ((ListRecyclerAdapter)recyclerView.getAdapter()).addItems(items);
        } else {
            Utils.showToast(context, context.getString(R.string.clipboard_does_not_contain_proper_data));
        }
    }

    private String getTextFromClipboard(){
        String mimeType = "text/plain";
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if(clipboardManager.hasPrimaryClip() && clipboardManager.getPrimaryClipDescription().hasMimeType(mimeType)){
            ClipData.Item item = clipboardManager.getPrimaryClip().getItemAt(0);
            String pasteData = item.getText().toString();
            if(pasteData == null || pasteData.trim().length() == 0)
                return null;
            
            return pasteData;
//            if(pasteData == null){
//                Uri pasteUri = item.getUri();
//                if(pasteUri == null){
//                    return null;
//                } else {
//                    ContentResolver cr = context.getContentResolver();
//                    String uriMimeType = cr.getType(pasteUri);
//                    if(uriMimeType != null && uriMimeType.equals(mimeType)) {
//                        Cursor cursor = cr.query(pasteUri, null, null, null, null);
//                        if(cursor != null && cursor.moveToFirst()){
//                                
//                        }
//                        cursor.close();
//                    }
//                }
//
//            }



        } else {
            return null;
        }
    }
}

class ListRecyclerAdapter extends RecyclerView.Adapter<ListRecyclerAdapter.SingleLineWithHandleViewHolder>
        implements ItemTouchHelperAdapter{
    private static final String TAG = "ListRecyclerAdapter";
    private final Context context;
    private RecyclerView recyclerView;
    private final OnStartDragListener listener;
    private ArrayList<ShoppingListItem> items;
    private int activeItemsCount;
    private static int selectColor;
    private static int tileSize;
    private static int textSize;
    private static int textStyle;
    private boolean requestNewItem;
    private boolean firstDrawing = true;

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(items, fromPosition, toPosition);
        notifyDataSetChanged();
        return true;
    }

    public ListRecyclerAdapter(Context context, ArrayList<ShoppingListItem> items, int activeItemsCount,
                               OnStartDragListener listener) {
        this.context = context;
        this.items = items;
        this.activeItemsCount = activeItemsCount;
        this.listener = listener;
        requestNewItem = activeItemsCount == 0;
        selectColor = ContextCompat.getColor(context, R.color.colorAccent);
        getParameters();

        setHasStableIds(true);
    }

    public void setItems(ArrayList<ShoppingListItem> items, int activeItemsCount){
        this.items = items;
        this.activeItemsCount = activeItemsCount;
        notifyDataSetChanged();
    }
    
    public void addItems(String[] items){
        for (String s : items){
            this.items.add(activeItemsCount+1, new ShoppingListItem(s, Constants.ENABLED_ITEM_VIEW));
            activeItemsCount++;
        }
        notifyDataSetChanged();
    }

    @Override
    public SingleLineWithHandleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = new View(parent.getContext());
        switch (viewType){
            case Constants.DISABLED_ITEM_VIEW:
                Log.e("ListFragment", "Disabled item");
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_line_with_handle_recycle_view_item, parent, false);
                break;
            case Constants.ENABLED_ITEM_VIEW:
                Log.e("ListFragment", "Enabled item");
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_line_with_handle_recycle_view_item, parent, false);
                break;
            case Constants.HEADER_VIEW:
                Log.e("ListFragment", "Header");
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_recycler_view_item, parent, false);
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
        if(firstDrawing || holder.getItemViewType() != Constants.NEW_ITEM_VIEW) {
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
            params.height = tileSize;
            holder.itemView.setLayoutParams(params);
        }
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
                onBindNewItemViewHolder(holder, position);
                firstDrawing = false;
                break;
        }
    }

    private void onBindHeaderViewHolder(SingleLineWithHandleViewHolder holder, int position){
        holder.header.setText(items.get(position).getContent());
        holder.header.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
        params.height = Utils.convertPxToDP(context, 48);
        holder.itemView.setLayoutParams(params);
    }

    private void onBindNewItemViewHolder(final SingleLineWithHandleViewHolder holder, final int position){
        holder.newItemEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        holder.newItemEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.e(TAG, "focus " + v.getId() + " " + hasFocus);
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
                params.height = hasFocus? Utils.convertPxToDP(context, 72) : tileSize;
                holder.itemView.setLayoutParams(params);
            }
        });
        Log.e(TAG, "request focus " + requestNewItem);
        if(requestNewItem) {
            requestNewItem = false;
            holder.newItemEditText.requestFocus();
        }
        holder.confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertItem(holder, position);
            }
        });
        holder.newItemEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    insertItem(holder, position);
                    return true;
                }
                return false;
            }
        });
    }

    private void insertItem(SingleLineWithHandleViewHolder holder, int position){
        if(holder.newItemEditText.getText().toString().length() > 0) {
            items.add(activeItemsCount + 1, new ShoppingListItem(holder.newItemEditText.getText().toString(), Constants.ENABLED_ITEM_VIEW)); //+1 because header is 1st item
            holder.newItemEditText.setText("");
            activeItemsCount++;
            recyclerView.scrollToPosition(position + 1);
            requestNewItem = true;
            notifyDataSetChanged();

//
//            if (recyclerView != null) {
//                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForLayoutPosition(position + 1);
//                if (viewHolder == null) {
//                    recyclerView.smoothScrollToPosition(position + 1);
//                }
//            }
//            if (recyclerView != null) {
//                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForLayoutPosition(activeItemsCount + 1);
//                if (viewHolder == null) {
//                    recyclerView.smoothScrollToPosition(activeItemsCount + 2);
//                    ((SingleLineWithHandleViewHolder)viewHolder).newItemEditText.requestFocus();
//                }
//            }
        }
    }

    private void onBindEnabledItemViewHolder(final SingleLineWithHandleViewHolder holder, final int position){
        holder.titleTextView.setText(items.get(position).getContent());
        holder.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                items.get(position).setViewType(Constants.DISABLED_ITEM_VIEW);
                ShoppingListItem item = items.remove(position);
                items.add(item);
                activeItemsCount--;
                notifyDataSetChanged();
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Utils.getNameDialog(context, items.get(position).getContent(), context.getString(R.string.edit_item),
                        32, context.getString(R.string.new_item) ,new Utils.OnNameSet() {
                    @Override
                    public void onNameSet(String name) {
                        if(name.length()>0){
                            items.get(position).setContent(name);
                            notifyItemChanged(position);
                        } else {
                            Utils.showToast(context, context.getString(R.string.item_name_cannot_be_empty));
                        }
                    }
                }).show();

                return false;
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
        holder.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        holder.handle.setVisibility(View.GONE);

        switch (textStyle){
            case Constants.COLOR:
                holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                holder.titleTextView.setStrikeEnabled(false);
                break;
            case Constants.STRIKETHROUGH:
                holder.titleTextView.setStrikeEnabled(true);
                holder.titleTextView.setTextColor(Color.BLACK);
                break;
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
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
        public final RobotoTextView titleTextView;
        public final AppCompatTextView header;
        public final AppCompatImageView handle;
        public final AppCompatImageView confirm;
        public final AppCompatImageView divider;
        public final TextInputEditText newItemEditText;

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
            handle = (AppCompatImageView) itemView.findViewById(R.id.imageView2);
            header = (AppCompatTextView) itemView.findViewById(R.id.textView);
            newItemEditText = (TextInputEditText) itemView.findViewById(R.id.editText);
            confirm = (AppCompatImageView) itemView.findViewById(R.id.imageView);
            divider = (AppCompatImageView) itemView.findViewById(R.id.imageView3);
        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(ListRecyclerAdapter.selectColor);
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
        return builder.toString();
    }

    public String getActiveItemsFromList() {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < activeItemsCount + 1; i++)
            builder.append(items.get(i).getContent()).append("\n");
        return builder.toString().trim();
    }

    public void removeDisabledItems(){
        int fromPosition = activeItemsCount + 3;
        int toPosition = items.size();
        for (int i = fromPosition; i<toPosition; i++)
            items.remove(fromPosition);
        notifyItemRangeRemoved(fromPosition, toPosition);
    }

    public void refreshParameters() {
        getParameters();
        notifyDataSetChanged();
    }

    private void getParameters(){
        SharedPreferences preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        tileSize = Utils.convertPxToDP(context, preferences.getInt(Constants.LIST_TILE_SIZE_KEY, 56));
        textSize = preferences.getInt(Constants.LIST_TILE_TEXT_SIZE, 16);
        textStyle = preferences.getInt(Constants.BOUGHT_ITEM_STYLE_KEY, Constants.COLOR);
    }


}

