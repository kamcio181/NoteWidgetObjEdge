package com.apps.home.notewidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

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
    private long folderId;
    private DatabaseHelper helper;
    private ArrayList<Note> notes;

    public FolderFragment() {
        // Required empty public constructor
    }

    public static FolderFragment newInstance(long folderId) {
        FolderFragment fragment = new FolderFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PARAM1, folderId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            folderId = getArguments().getLong(ARG_PARAM1);
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

        loadFolder();
        loadNotes();
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.v(TAG, "onCreateOptionsMenu");
        super.onCreateOptionsMenu(menu, inflater);

        if (folderId == Utils.getMyNotesNavId(context))
            getActivity().getMenuInflater().inflate(R.menu.menu_my_notes_list, menu);
        else if (folderId == Utils.getTrashNavId(context))
            getActivity().getMenuInflater().inflate(R.menu.menu_trash, menu);
        else
            getActivity().getMenuInflater().inflate(R.menu.menu_folder_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(TAG, "onOptionsItemSelected");

        switch (item.getItemId()){
            case R.id.action_sort_by_date:
                setSortByDate(true);
                break;
            case R.id.action_sort_by_title:
                setSortByDate(false);
                break;
            case R.id.action_add_nav_folder:
                handleAddFolder();
                break;
        }
        return true;
    }

    public void setSortByDate(boolean sortByDate) {
        this.sortByDate = sortByDate;
        preferences.edit().putBoolean(Constants.SORT_BY_DATE_KEY, sortByDate).apply();
        if(notes != null) {
            Collections.sort(notes, new NotesComparator(sortByDate));
            recyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    private void handleAddFolder(){
        Utils.getNameDialog(context, getString(R.string.new_folder), getString(R.string.add_folder),
                32, getString(R.string.folder_name), new Utils.OnNameSet() {
                    @Override
                    public void onNameSet(String name) {
                        if(name.equals(""))
                            name = getString(R.string.new_folder);
                        else
                            name = Utils.capitalizeFirstLetter(name);
                        final Folder folder = new Folder(name);
                        helper.createFolder(folder, new DatabaseHelper.OnItemInsertListener() {
                            @Override
                            public void onItemInserted(long id) {
                                if(id > 0){
                                    folder.setId(id);
                                    ((MainActivity)context).addFolderToNavView(folder);
                                }
                            }
                        });
                    }
                }).show();
    }

    @Override
    public void onTitleChanged(String newTitle) {
        folder.setName(newTitle);
        helper.updateFolder(folder, null);
    }

    class NotesComparator implements Comparator<Note>{
        final boolean sortByDate;

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

    private void loadFolder(){
        helper.getFolder(folderId, new DatabaseHelper.OnFolderLoadListener() {
            @Override
            public void onFolderLoaded(Folder folder) {
                FolderFragment.this.folder = folder;

                ActionBar actionBar = ((AppCompatActivity) context).getSupportActionBar();

                if(actionBar!=null){
                    actionBar.setTitle(folder.getName());
                    actionBar.setSubtitle("");
                }
            }
        });
    }

    private void loadNotes(){

        helper.getFolderNotes(folderId, sortByDate, new DatabaseHelper.OnNotesLoadListener() {
            @Override
            public void onNotesLoaded(ArrayList<Note> notes) {
                if (notes != null) {
                    FolderFragment.this.notes = notes;
                    FolderFragment.this.folder.setCount(notes.size());
                    if (recyclerView.getAdapter() == null) {
                        recyclerView.setLayoutManager(new LinearLayoutManager(context));
                        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
                        recyclerView.setAdapter(new NotesRecyclerAdapter(notes,
                                new NotesRecyclerAdapter.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(View view, int position, boolean longClick) {
                                        if (mListener != null) {
                                            Note note = new Note();
                                            note.setId(FolderFragment.this.notes.get(position).getId());
                                            note.setType(FolderFragment.this.notes.get(position).getType());
                                            mListener.onNoteClicked(note, longClick);
                                        }
                                    }
                                }));
                    } else {
                        ((NotesRecyclerAdapter) recyclerView.getAdapter()).setNotes(FolderFragment.this.notes);
                    }
                } else {
                    recyclerView.setAdapter(null);
                }
//                ProgressDialog progressDialog = ((MainActivity)context).getProgressDialog(); //TODO global progress bar
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

    static class NotesRecyclerAdapter extends RecyclerView.Adapter<NotesRecyclerAdapter.DoubleLineViewHolder> {
        private final Calendar calendar;
        private static OnItemClickListener listener;
        private ArrayList<Note> notes;

        public interface OnItemClickListener{
            void onItemClick(View view, int position, boolean longClick);
        }

        public NotesRecyclerAdapter(ArrayList<Note> notes, OnItemClickListener listener) {
            this.notes = notes;
            NotesRecyclerAdapter.listener = listener;
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
            public final AppCompatTextView titleTextView;
            public final AppCompatTextView subtitleTextView;

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

                titleTextView = (AppCompatTextView) itemView.findViewById(R.id.textView2);
                subtitleTextView = (AppCompatTextView) itemView.findViewById(R.id.textView3);
            }
        }

        @Override
        public long getItemId(int position) {
            return notes.get(position).getId();
        }
    }
}


