package com.apps.home.notewidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.CursorRecyclerAdapter;
import com.apps.home.notewidget.utils.DividerItemDecoration;
import com.apps.home.notewidget.utils.Utils;

import java.util.Calendar;

public class NoteListFragment extends Fragment {
    private static final String TAG = "NoteListFragment";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private RecyclerView recyclerView;
    private OnItemClickListener mListener;
    private SQLiteDatabase db;
    private Cursor cursor;
    private boolean sortByDate;
    private SharedPreferences preferences;
    private Context context;
    private int folderId;
    private String folderName;

    public NoteListFragment() {
        // Required empty public constructor
    }

    public static NoteListFragment newInstance(int folderId, String folderName) {
        NoteListFragment fragment = new NoteListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, folderId);
        args.putString(ARG_PARAM2, folderName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            folderId = getArguments().getInt(ARG_PARAM1);
            folderName = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();
        ((AppCompatActivity)context).invalidateOptionsMenu();

        preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        sortByDate = preferences.getBoolean(Constants.SORT_BY_DATE_KEY, false);

        return inflater.inflate(R.layout.fragment_note_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = (RecyclerView) view;
        new LoadNoteList().execute();

        ((AppCompatActivity)context).getSupportActionBar().setTitle(folderName);
        ((AppCompatActivity)context).getSupportActionBar().setSubtitle("");
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnItemClickListener) {
            mListener = (OnItemClickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnItemClickListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        if(cursor!=null && !cursor.isClosed())
            cursor.close();
    }

    public void setSortByDate(boolean sortByDate) {
        this.sortByDate = sortByDate;
        preferences.edit().putBoolean(Constants.SORT_BY_DATE_KEY, sortByDate).apply();
        new LoadNoteList().execute();
    }

    public void reloadList(){
        new LoadNoteList().execute();
    }

    public interface OnItemClickListener {
        void onItemClicked(int noteId);
    }

    private class LoadNoteList extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {

            if((db = Utils.getDb(context)) != null){
                String orderColumn = sortByDate ? Constants.MILLIS_COL : Constants.NOTE_TITLE_COL;
                Log.e(TAG, orderColumn);
                Log.e(TAG, "get list cursor");

                if(folderId != 2) { //is not trash
                    cursor = db.query(Constants.NOTES_TABLE, new String[]{Constants.ID_COL, Constants.MILLIS_COL,
                                    Constants.NOTE_TITLE_COL, Constants.NOTE_TEXT_COL},
                            Constants.FOLDER_ID_COL + " = ? AND " + Constants.DELETED_COL + " = ?",
                            new String[]{Long.toString(folderId), Integer.toString(0)}, null, null, "LOWER(" + orderColumn + ") ASC");
                } else {
                    cursor = db.query(Constants.NOTES_TABLE, new String[]{Constants.ID_COL, Constants.MILLIS_COL,
                                    Constants.NOTE_TITLE_COL, Constants.NOTE_TEXT_COL},
                            Constants.DELETED_COL + " = ?",
                            new String[]{Integer.toString(1)}, null, null, "LOWER(" + orderColumn + ") ASC");
                }
                return true;
            } else
                return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if(aBoolean){
                cursor.moveToFirst();

                if (recyclerView.getAdapter() == null) {
                    recyclerView.setLayoutManager(new LinearLayoutManager(context));
                    recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
                    recyclerView.setAdapter(new NotesCursorRecyclerAdapter(cursor,
                            new NotesCursorRecyclerAdapter.OnItemClickListener() {
                                @Override
                                public void onItemClick(View view, int position) {
                                    if (mListener != null){
                                        cursor.moveToPosition(position);
                                        mListener.onItemClicked(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.ID_COL)));
                                    }
                                }
                            }));
                } else {
                    ((CursorRecyclerAdapter)recyclerView.getAdapter()).changeCursor(cursor);
                }
            }
        }
    }
}

class NotesCursorRecyclerAdapter extends CursorRecyclerAdapter<NotesCursorRecyclerAdapter.DoubleLineViewHolder>{
    private Calendar calendar;
    private NotesCursorRecyclerAdapter.OnItemClickListener listener;

    public interface OnItemClickListener{
        void onItemClick(View view, int position);
    }

    public NotesCursorRecyclerAdapter (Cursor cursor, OnItemClickListener listener){
        super(cursor);
        this.listener = listener;
        calendar = Calendar.getInstance();
    }

    @Override
    public int getItemViewType(Cursor cursor) {
        return 0;
    }

    @Override
    public void onBindViewHolder(NotesCursorRecyclerAdapter.DoubleLineViewHolder holder, Cursor cursor) {
        calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.MILLIS_COL)));
        Log.e("RecycleViewAdapter", "millis " + cursor.getLong(cursor.getColumnIndexOrThrow(Constants.MILLIS_COL)));
        holder.titleTextView.setText(cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TITLE_COL)));
        holder.subtitleTextView.setText(String.format("%1$tb %1$te, %1$tY %1$tT", calendar));
    }

    @Override
    public NotesCursorRecyclerAdapter.DoubleLineViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DoubleLineViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.double_line_recycle_view_item, parent, false));
    }

    class DoubleLineViewHolder extends RecyclerView.ViewHolder{
        public RobotoTextView titleTextView, subtitleTextView;

        public DoubleLineViewHolder(final View itemView){
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener != null)
                        listener.onItemClick(itemView, getLayoutPosition());
                }
            });

            titleTextView = (RobotoTextView) itemView.findViewById(R.id.textView2);
            subtitleTextView = (RobotoTextView) itemView.findViewById(R.id.textView3);
        }
    }
}
