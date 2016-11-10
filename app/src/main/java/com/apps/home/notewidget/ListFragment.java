package com.apps.home.notewidget;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.objects.ShoppingListItem;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.ItemTouchHelperAdapter;
import com.apps.home.notewidget.utils.ItemTouchHelperViewHolder;
import com.apps.home.notewidget.utils.AdvancedNoteFragment;
import com.apps.home.notewidget.utils.NoteUpdateListener;
import com.apps.home.notewidget.utils.OnStartDragListener;
import com.apps.home.notewidget.utils.ParametersUpdateListener;
import com.apps.home.notewidget.utils.SimpleItemTouchHelperCallback;
import com.apps.home.notewidget.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

public class ListFragment extends AdvancedNoteFragment implements NoteUpdateListener, ParametersUpdateListener{
    private static final String TAG = "ListFragment";
    private RecyclerView recyclerView;
    private ItemTouchHelper itemTouchHelper;


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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_note_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setHasFixedSize(true);


        if(isNewNote) {
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
        }

        super.onViewCreated(view, savedInstanceState);
    }

//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        Log.v(TAG, "onCreateOptionsMenu");
//        super.onCreateOptionsMenu(menu, inflater);
//
//        getActivity().getMenuInflater().inflate(R.menu.menu_list, menu);
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(TAG, "onOptionsItemSelected");

        switch (item.getItemId()){
            case R.id.action_remove_disabled_items:
                removeDisabledItems();
                break;
            case R.id.action_add_from_clipboard:
                addItemsFromClipboard();
                break;
            default:
                super.onOptionsItemSelected(item);
        }
        return true;
    }


    @Override
    public void setNoteViews() {
        super.setNoteViews();
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

    @Override
    public void onParametersUpdated() {
        ((ListRecyclerAdapter)recyclerView.getAdapter()).refreshParameters();
    }

    @Override
    public void deleteNote() {
        note.setNote(((ListRecyclerAdapter)recyclerView.getAdapter()).getStringFromList());
        super.deleteNote();
    }

    @Override
    public void saveNote(final boolean quitAfterSaving) {
        note.setNote(((ListRecyclerAdapter)recyclerView.getAdapter()).getStringFromList());
        super.saveNote(quitAfterSaving);
    }

    @Override
    public String getContent() {
        return ((ListRecyclerAdapter)recyclerView.getAdapter()).getActiveItemsFromList();
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
            if(pasteData.trim().length() == 0)
                return null;
            
            return pasteData;
        } else {
            return null;
        }
    }
}

class ListRecyclerAdapter extends RecyclerView.Adapter<ListRecyclerAdapter.SingleLineWithHandleViewHolder>
        implements ItemTouchHelperAdapter{
    private static final String TAG = "ListRecyclerAdapter";
    private static final int ITEM_CHAR_LIMIT = 32;
    private final Context context;
    private RecyclerView recyclerView;
    private final OnStartDragListener listener;
    private ArrayList<ShoppingListItem> items;
    private int activeItemsCount;
    private static int selectColor;
    private static int tileSize;
    private static int textSize;
    private static int textStyle;
    private static int newlyBoughtItemBehavior;
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
        holder.newItemEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString().trim();
                if (text.length() > ITEM_CHAR_LIMIT) {
                    holder.newItemEditText.setText(text.substring(0, ITEM_CHAR_LIMIT));
                    holder.newItemEditText.setSelection(ITEM_CHAR_LIMIT);
                    Utils.showToast(context, String.format(Locale.getDefault(), context.getString(R.string.item_size_is_limited_to_s_chars), ITEM_CHAR_LIMIT));
                }
            }
        });
    }

    private void insertItem(SingleLineWithHandleViewHolder holder, int position){
        String name = holder.newItemEditText.getText().toString().trim();
        if(name.length() > 0){
            ShoppingListItem newItem = new ShoppingListItem(Utils.capitalizeFirstLetter(name), Constants.ENABLED_ITEM_VIEW);
            if(checkIfActiveItemIsPresent(newItem)){
                Utils.showToast(context,  context.getString(R.string.item_is_already_on_list));
                return;
            }

            int index = checkIfDisabledItemIsPresent(newItem);
            if(index != -1){
                items.remove(index);
                Utils.showToast(context, context.getString(R.string.restored_from_bought_items));
            } else {
                Utils.showToast(context, context.getString(R.string.added_new_item));
            }
            items.add(activeItemsCount + 1, newItem); //+1 because header is 1st item
            holder.newItemEditText.setText("");
            activeItemsCount++;
            recyclerView.scrollToPosition(position + 1);
            requestNewItem = true;
            notifyDataSetChanged();
        }
    }

    private int checkIfDisabledItemIsPresent(ShoppingListItem newItem){
        for(int i = activeItemsCount + 3; i < items.size(); i++){
            if(newItem.getContent().equalsIgnoreCase(items.get(i).getContent().trim()))
                return i;
        }
        return -1;
    }

    private boolean checkIfActiveItemIsPresent(ShoppingListItem newItem){
        for(int i = 1; i < activeItemsCount +1; i++){
            if(newItem.getContent().equalsIgnoreCase(items.get(i).getContent().trim()))
                return true;
        }
        return false;
    }

    private void onBindEnabledItemViewHolder(final SingleLineWithHandleViewHolder holder, final int position){
        holder.titleTextView.setText(items.get(position).getContent());
        holder.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        Utils.setMarquee(holder.titleTextView);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                items.get(position).setViewType(Constants.DISABLED_ITEM_VIEW);
                ShoppingListItem item = items.remove(position);
                activeItemsCount--;
                if(newlyBoughtItemBehavior == Constants.MOVE_TO_BOTTOM)
                    items.add(item);
                else
                    items.add(activeItemsCount + 3, item);

                notifyDataSetChanged();
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Utils.getNameDialog(context, items.get(position).getContent(), context.getString(R.string.edit_item),
                        ITEM_CHAR_LIMIT, context.getString(R.string.new_item), new Utils.OnNameSet() {
                            @Override
                            public void onNameSet(String name) {
                                if (name.length() > 0) {
                                    items.get(position).setContent(name);
                                    notifyItemChanged(position);
                                } else {
                                    Utils.showToast(context, context.getString(R.string.item_name_cannot_be_empty));
                                }
                            }
                        }, context.getString(R.string.delete), new Utils.OnNameSet() {
                            @Override
                            public void onNameSet(String name) {
                                items.remove(position);
                                activeItemsCount--;
                                notifyDataSetChanged();
                                Utils.showToast(context, context.getString(R.string.item_removed));
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
        Utils.setMarquee(holder.titleTextView);

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

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(context.getString(R.string.edit_item))
                        .setMessage(items.get(position).getContent())
                        .setNegativeButton(context.getText(R.string.cancel), null)
                        .setNeutralButton(context.getText(R.string.delete), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                items.remove(position);
                                notifyDataSetChanged();
                                Utils.showToast(context, context.getString(R.string.item_removed));
                            }
                        }).show();
                return false;
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
        newlyBoughtItemBehavior = preferences.getInt(Constants.NEWLY_BOUGHT_ITEM_BEHAVIOR, Constants.MOVE_TO_BOTTOM);
    }
}

