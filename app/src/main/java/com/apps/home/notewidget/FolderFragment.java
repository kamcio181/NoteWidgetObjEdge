package com.apps.home.notewidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.objects.Folder;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.DatabaseHelper;
import com.apps.home.notewidget.utils.DividerItemDecoration;
import com.apps.home.notewidget.utils.TitleChangeListener;
import com.apps.home.notewidget.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

public class FolderFragment extends Fragment implements TitleChangeListener{
    private static final String TAG = "FolderFragment";
    private static final String ARG_PARAM1 = "param1";
    private RecyclerView recyclerView;
    private OnNoteClickListener mListener;
    private boolean sortByDate;
    private SharedPreferences preferences;
    private Context context;
    private Folder folder;
    private DatabaseHelper helper;
    private ArrayList<Note> notes;

    public FolderFragment() {
        // Required empty public constructor
    }

    public static FolderFragment newInstance(Folder folder) {
        FolderFragment fragment = new FolderFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM1, folder);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            folder = (Folder) getArguments().getSerializable(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();
        ((AppCompatActivity)context).invalidateOptionsMenu();
        Utils.showOrHideKeyboard(((AppCompatActivity) context).getWindow(), false); //TODO not working
        preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        sortByDate = preferences.getBoolean(Constants.SORT_BY_DATE_KEY, false);
        helper = new DatabaseHelper(context);

        return inflater.inflate(R.layout.fragment_note_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = (RecyclerView) view;

        loadNotes();

        ActionBar actionBar = ((AppCompatActivity) context).getSupportActionBar();

        if(actionBar!=null){
            actionBar.setTitle(folder.getName());
            actionBar.setSubtitle("");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnNoteClickListener) {
            mListener = (OnNoteClickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnNoteClickListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void setSortByDate(boolean sortByDate) {
        this.sortByDate = sortByDate;
        preferences.edit().putBoolean(Constants.SORT_BY_DATE_KEY, sortByDate).apply();
        if(notes != null) {
            Collections.sort(notes, new NotesComparator(sortByDate));
            recyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onTitleChanged(String newTitle) {
        folder.setName(newTitle);
        helper.updateFolder(folder, null);
    }

    class NotesComparator implements Comparator<Note>{
        boolean sortByDate;

        public NotesComparator(boolean sortByDate) {
            this.sortByDate = sortByDate;
        }

        @Override
        public int compare(Note lhs, Note rhs) {
            if(sortByDate){
                if(lhs.getCreatedAt() < rhs.getCreatedAt())
                    return 1;
                else if (lhs.getCreatedAt() > rhs.getCreatedAt())
                    return -1;
                else
                    return 0;
            } else {
                return lhs.getTitle().compareTo(rhs.getTitle());
            }
        }
    }

    public void loadNotes(){

        helper.getFolderNotes((int) folder.getId(), sortByDate, new DatabaseHelper.OnNotesLoadListener() {
            @Override
            public void onNotesLoaded(ArrayList<Note> notes) {
                if (notes != null) {
                    FolderFragment.this.notes = notes;
                    if (recyclerView.getAdapter() == null) {
                        recyclerView.setLayoutManager(new LinearLayoutManager(context));
                        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
                        recyclerView.setAdapter(new NotesRecyclerAdapter(notes,
                                new NotesRecyclerAdapter.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(View view, int position, boolean longClick) {
                                        if (mListener != null) {
                                            mListener.onNoteClicked(FolderFragment.this.notes.get(position), longClick);
                                        }
                                    }
                                }));
                    } else {
                        ((NotesRecyclerAdapter) recyclerView.getAdapter()).setNotes(FolderFragment.this.notes);
                    }
                } else {
                    recyclerView.setAdapter(null);
                }
//                ProgressDialog progressDialog = ((MainActivity)context).getProgressDialog();
//                if(progressDialog.isShowing())
//                    progressDialog.dismiss();
            }
        });

    }

    public void reloadList(){
        loadNotes();
    }

    public void clearRecyclerViewAdapter(){
        if(recyclerView != null)
            recyclerView.setAdapter(null);
    }

    public interface OnNoteClickListener {
        void onNoteClicked(Note note, boolean longClick);
    }
}

class NotesRecyclerAdapter extends RecyclerView.Adapter<NotesRecyclerAdapter.DoubleLineViewHolder> {
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
        setHasStableIds(true);
    }

    public void setNotes(ArrayList<Note> notes){
        this.notes = notes;
        notifyDataSetChanged();
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

    @Override
    public long getItemId(int position) {
        return notes.get(position).getId();
    }
}
