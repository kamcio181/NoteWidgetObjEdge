package com.apps.home.notewidget;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.CursorRecyclerAdapter;
import com.apps.home.notewidget.utils.DatabaseHelper2;
import com.apps.home.notewidget.utils.DividerItemDecoration;
import com.apps.home.notewidget.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;

public class NoteListFragment extends Fragment {
    private static final String TAG = "NoteListFragment";
    private static final String ARG_PARAM1 = "param1";
    private RecyclerView recyclerView;
    private OnItemClickListener mListener;
    private SQLiteDatabase db;
    private boolean sortByDate;
    private SharedPreferences preferences;
    private Context context;
    private int folderId;
    private String folderName;

    public NoteListFragment() {
        // Required empty public constructor
    }

    public static NoteListFragment newInstance(int folderId) {
        NoteListFragment fragment = new NoteListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, folderId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            folderId = getArguments().getInt(ARG_PARAM1);
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

        recyclerView = (RecyclerView) view; //TODO click

        new LoadNoteList().execute();

        folderName = Utils.getFolderName(context, folderId);

        ActionBar actionBar = ((AppCompatActivity) context).getSupportActionBar();

        if(actionBar!=null){
            actionBar.setTitle(folderName);
            actionBar.setSubtitle("");
        }
    }

    public void titleChanged(String title){
        this.folderName = title;
        new UpdateFolderNameInTable().execute();
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

    public void loadNotes(){
        DatabaseHelper2 helper = new DatabaseHelper2(context);
        helper.getFolderNotes(folderId, sortByDate, new DatabaseHelper2.OnNotesLoadListener() {
            @Override
            public void onNotesLoaded(ArrayList<Note> notes) {
                if (recyclerView.getAdapter() == null) {
                    recyclerView.setLayoutManager(new LinearLayoutManager(context));
                    recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
                    recyclerView.setAdapter(new NotesRecyclerAdapter(notes,
                            new NotesRecyclerAdapter.OnItemClickListener() {
                                @Override
                                public void onItemClick(View view, int position, boolean longClick) {
                                    if (mListener != null) {
                                        cursor.moveToPosition(position);
                                        mListener.onItemClicked(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.ID_COL)), longClick);
                                    }
                                }
                            }));
                } else {
                    ((CursorRecyclerAdapter)recyclerView.getAdapter()).changeCursor(cursor);
                }
            }
        });

    }

    public void reloadList(){
        new LoadNoteList().execute();
    }

    public interface OnItemClickListener {
        void onItemClicked(long noteId, boolean longClick);
    }



    private class UpdateFolderNameInTable extends AsyncTask<Void, Void, Boolean>
    {   private ContentValues contentValues;

        @Override
        protected void onPreExecute()
        {
            contentValues = new ContentValues();
            contentValues.put(Constants.FOLDER_NAME_COL, folderName);
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... p1)
        {
            if((db = Utils.getDb(context)) != null) {
                db.update(Constants.FOLDER_TABLE, contentValues, Constants.ID_COL + " = ?",
                        new String[]{Long.toString(folderId)});
                Log.e(TAG, "update " + contentValues.toString());
                return true;
            } else
                return false;

        }
    }
}

class NotesRecyclerAdapter extends RecyclerView.Adapter<NotesRecyclerAdapter.DoubleLineViewHolder>{
    private Calendar calendar;
    private static OnItemClickListener listener;
    private ArrayList<Note> notes;

    public interface OnItemClickListener{
        void onItemClick(View view, int position, boolean longClick);
    }

    public NotesRecyclerAdapter(ArrayList<Note> notes, OnItemClickListener listener) {
        this.notes = notes;
        this.listener = listener;
        calendar = Calendar.getInstance();
    }


    @Override
    public DoubleLineViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DoubleLineViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.double_line_recycle_view_item, parent, false));
    }

    @Override
    public void onBindViewHolder(DoubleLineViewHolder holder, int position) {
        Note note = notes.get(position);
        calendar.setTimeInMillis(note.getCreatedAt());
        holder.titleTextView.setText(note.getTitle());
        holder.subtitleTextView.setText(String.format("%1$tb %1$te, %1$tY %1$tT", calendar));
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class DoubleLineViewHolder extends RecyclerView.ViewHolder{
        public RobotoTextView titleTextView, subtitleTextView;

        public DoubleLineViewHolder(final View itemView){
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener != null)
                        listener.onItemClick(itemView, getLayoutPosition(), false);
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if(listener != null)
                        listener.onItemClick(itemView, getLayoutPosition(), true);
                    return true;
                }
            });

            titleTextView = (RobotoTextView) itemView.findViewById(R.id.textView2);
            subtitleTextView = (RobotoTextView) itemView.findViewById(R.id.textView3);
        }
    }
}
