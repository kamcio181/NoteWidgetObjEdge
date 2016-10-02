package com.apps.home.notewidget;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.objects.ShoppingListItem;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.DatabaseHelper;
import com.apps.home.notewidget.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class TrashListFragment extends Fragment {
    private static final String TAG = "TrashNoteFragment";
    private static final String ARG_PARAM1 = "param1";
    private RecyclerView recyclerView;
    private long noteId;
    private Note note;
    private Context context;


    public TrashListFragment() {
        // Required empty public constructor
    }

    public static TrashListFragment newInstance(long noteId) {
        TrashListFragment fragment = new TrashListFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM1, noteId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            noteId = getArguments().getLong(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();
        ((AppCompatActivity)context).invalidateOptionsMenu();
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_note_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setHasFixedSize(true);

        DatabaseHelper helper = new DatabaseHelper(context);
        helper.getNote(true, noteId, new DatabaseHelper.OnNoteLoadListener() {
            @Override
            public void onNoteLoaded(Note note) {
                TrashListFragment.this.note = note;
                setRecyclerViewItems();
                setTitleAndSubtitle();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.v(TAG, "onCreateOptionsMenu");
        super.onCreateOptionsMenu(menu, inflater);

        getActivity().getMenuInflater().inflate(R.menu.menu_note_trash, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(TAG, "onOptionsItemSelected");
        return super.onOptionsItemSelected(item);
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
        itemList.add(new ShoppingListItem("Items bought", Constants.HEADER_VIEW));

        for (int i = activeItemsCount; i<items.size(); i++){
            itemList.add(new ShoppingListItem(items.get(i), Constants.DISABLED_ITEM_VIEW));
        }

        if(recyclerView.getAdapter() == null)
            recyclerView.setAdapter(new ListRecyclerAdapter(context, itemList));
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

    static class ListRecyclerAdapter extends RecyclerView.Adapter<ListRecyclerAdapter.SingleLineWithHandleViewHolder> {
        private static final String TAG = "ListRecyclerAdapter";
        private final ArrayList<ShoppingListItem> items;
        private static int tileSize;
        private static int textSize;
        private static int textStyle;
        private final Context context;

        public ListRecyclerAdapter(Context context, ArrayList<ShoppingListItem> items) {
            this.items = items;
            this.context = context;
            getParameters();

            setHasStableIds(true);
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
            }

            return new SingleLineWithHandleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SingleLineWithHandleViewHolder holder, int position) {
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
            params.height = tileSize;
            holder.itemView.setLayoutParams(params);
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
            }
        }

        private void onBindHeaderViewHolder(SingleLineWithHandleViewHolder holder, int position){
            holder.header.setText(items.get(position).getContent());
            holder.header.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
            params.height = Utils.convertPxToDP(context, 48);
            holder.itemView.setLayoutParams(params);
        }

        private void onBindEnabledItemViewHolder(final SingleLineWithHandleViewHolder holder, final int position){
            holder.titleTextView.setText(items.get(position).getContent());
            holder.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            holder.handle.setVisibility(View.GONE);
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

        static class SingleLineWithHandleViewHolder extends RecyclerView.ViewHolder {
            public final RobotoTextView titleTextView;
            public final AppCompatTextView header;
            public final ImageView handle;
            public final ImageView divider;

            public SingleLineWithHandleViewHolder(final View itemView){
                super(itemView);

                titleTextView = (RobotoTextView) itemView.findViewById(R.id.textView2);
                header = (AppCompatTextView) itemView.findViewById(R.id.textView);
                divider = (ImageView) itemView.findViewById(R.id.imageView3);
                handle = (ImageView) itemView.findViewById(R.id.imageView2);
            }
        }

        @Override
        public long getItemId(int position) {
            return items.get(position).getId();
        }

        private void getParameters(){
            SharedPreferences preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
            tileSize = Utils.convertPxToDP(context, preferences.getInt(Constants.LIST_TILE_SIZE_KEY, 56));
            textSize = preferences.getInt(Constants.LIST_TILE_TEXT_SIZE, 16);
            textStyle = preferences.getInt(Constants.BOUGHT_ITEM_STYLE_KEY, Constants.COLOR);
        }
    }
}

