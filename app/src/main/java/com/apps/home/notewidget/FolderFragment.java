package com.apps.home.notewidget;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
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
    private DatabaseHelper helper;
    private ArrayList<Note> notes;
    private Menu menu;

    public interface OnNoteClickListener {
        void onNoteClicked(Note note);
    }

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
            folder = new Folder(getArguments().getLong(ARG_PARAM1));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();
//        Utils.showOrHideKeyboard(((AppCompatActivity) context).getWindow(), false); //TODO not working
        preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        sortByDate = preferences.getBoolean(Constants.SORT_BY_DATE_KEY, false);
        helper = new DatabaseHelper(context);
        menu = ((MainActivity)context).getNavigationViewMenu();

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

//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        Log.v(TAG, "onCreateOptionsMenu");
//        super.onCreateOptionsMenu(menu, inflater);
//
//        if (folderId == Utils.getMyNotesNavId(context))
//            getActivity().getMenuInflater().inflate(R.menu.menu_my_notes_list, menu);
//        else if (folderId == Utils.getTrashNavId(context))
//            getActivity().getMenuInflater().inflate(R.menu.menu_trash, menu);
//        else
//            getActivity().getMenuInflater().inflate(R.menu.menu_folder_list, menu);
//    }

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
            case R.id.action_delete_all:
                handleDeleteAllAction();
                break;
            case R.id.action_restore_all:
                handleRestoreAllAction();
                break;
            case R.id.action_delete_nav_folder:
                handleDeleteFolder();
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

    private void handleDeleteAllAction(){
        Utils.getConfirmationDialog(context, getString(R.string.do_you_want_to_delete_all_notes), getRemoveAllNotesFromTrashAction()).show();
    }

    private DialogInterface.OnClickListener getRemoveAllNotesFromTrashAction(){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                helper.removeAllNotesFromTrash(new DatabaseHelper.OnItemRemoveListener() {
                    @Override
                    public void onItemRemoved(int numberOfRows) {
                        if(numberOfRows > 0){
                            Utils.setFolderCount(menu, (int) Utils.getTrashNavId(context), 0); //Set count to 0 for trash
                            Utils.showToast(context, getString(R.string.all_notes_were_removed));

                            clearRecyclerViewAdapter();
                        }
                    }
                });
            }
        };
    }

    private void handleRestoreAllAction(){
        Utils.getConfirmationDialog(context, getString(R.string.do_you_want_to_restore_all_notes), getRestoreAllNotesFromTrashAction()).show();
    }

    private DialogInterface.OnClickListener getRestoreAllNotesFromTrashAction(){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                helper.restoreAllNotesFromTrash(new DatabaseHelper.OnFoldersLoadListener() {
                    @Override
                    public void onFoldersLoaded(ArrayList<Folder> folders) {
                        if (folders != null) {
                            Utils.setFolderCount(menu, (int) Utils.getTrashNavId(context), 0); //Set count to 0 for trash
                            Utils.updateAllWidgets(context);
                            Utils.updateAllEdgePanels(context);
                            for (Folder f : folders) {
                                Utils.incrementFolderCount(menu, (int) f.getId(), f.getCount());
                            }
                            Utils.showToast(context, getString(R.string.all_notes_were_restored));

                            clearRecyclerViewAdapter();
                        }
                    }
                });
            }
        };
    }

    private void handleDeleteFolder(){
        Utils.getConfirmationDialog(context, getString(R.string.do_you_want_to_delete_this_folder_and_all_associated_notes),
                getRemoveFolderAndAllNotesAction()).show();
    }

    private DialogInterface.OnClickListener getRemoveFolderAndAllNotesAction(){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                helper.removeFolder(folder.getId(), new DatabaseHelper.OnItemRemoveListener() {
                    @Override
                    public void onItemRemoved(int numberOfRows) {
                        if (numberOfRows > 0) {
                            helper.removeAllNotesFromFolder(folder.getId(), new DatabaseHelper.OnItemRemoveListener() {
                                @Override
                                public void onItemRemoved(int numberOfRows) {
                                    Utils.showToast(context, getString(R.string.folder_and_all_associated_notes_were_removed));
                                    Utils.updateAllWidgets(context);
                                    Utils.updateAllEdgePanels(context);
                                    removeMenuItem(folder.getId());
                                    if (preferences.getInt(Constants.STARTING_FOLDER_KEY, -1) == folder.getId())
                                        preferences.edit().remove(Constants.STARTING_FOLDER_KEY).apply();
                                    ((MainActivity)context).openFolderWithNotes(Utils.getMyNotesNavId(context));
                                }
                            });
                        }
                    }
                });
            }
        };
    }

    private void removeMenuItem(long id){
        menu.removeItem((int) id);
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
        helper.getFolder(folder.getId(), new DatabaseHelper.OnFolderLoadListener() {
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

        helper.getFolderNotes(folder.getId(), sortByDate, new DatabaseHelper.OnNotesLoadListener() {
            @Override
            public void onNotesLoaded(ArrayList<Note> notes) {
                if (notes != null) {
                    FolderFragment.this.notes = notes;
                    FolderFragment.this.folder.setCount(notes.size());
                    if (recyclerView.getAdapter() == null) {
                        recyclerView.setLayoutManager(new LinearLayoutManager(context));
                        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
                        recyclerView.setAdapter(new NotesRecyclerAdapter(notes,
                                new OnNoteClickListener() {
                                    @Override
                                    public void onNoteClicked(Note note) {
                                        if (mListener != null) {
                                            mListener.onNoteClicked(note);
                                        }
                                    }
                                }));
                    } else {
                        ((NotesRecyclerAdapter) recyclerView.getAdapter()).setNotes(FolderFragment.this.notes);
                    }
                } else {
                    recyclerView.setAdapter(null);
                }
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

    private Dialog getNoteActionDialog(final Note note){
        String[] items = new String[]{getString(R.string.open), getString(R.string.share),
                getString(R.string.move_to_other_folder), getString(R.string.move_to_trash)};

        return new AlertDialog.Builder(context).setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        //open
                        mListener.onNoteClicked(note); //TODO
                        break;
                    case 1:
                        //share
                        handleShareAction(note);
                        break;
                    case 2:
                        //move to other folder
                        handleNoteMoveAction(note);
                        break;
                    case 3:
                        //move to trash
                        deleteNote(note);
                }
            }
        }).create();
    }

    private void handleShareAction(Note note){
        helper.getNote(false, note.getId(), new DatabaseHelper.OnNoteLoadListener() {
            @Override
            public void onNoteLoaded(Note note) {
                Utils.sendShareIntent(context, Html.fromHtml(note.getNote()).toString(), note.getTitle());
            }
        });
    }

    private void handleNoteMoveAction(final Note note){
        menu = ((MainActivity)context).getNavigationViewMenu();
        Dialog dialog = Utils.getFolderListDialog(context, menu,
                new int[]{(int) folder.getId(), (int) Utils.getTrashNavId(context)},
                getString(R.string.choose_new_folder), getMoveNoteToOtherFolderAction(note));
        if(dialog != null)
            dialog.show();
    }

    private DialogInterface.OnClickListener getMoveNoteToOtherFolderAction(final Note note){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final int newFolderId = Utils.getFolderIdFromArray(which);

                ContentValues contentValues = new ContentValues(1);
                contentValues.put(Constants.FOLDER_ID_COL, newFolderId);


                helper.updateNote(note.getId(), contentValues, new DatabaseHelper.OnItemUpdateListener() {
                    @Override
                    public void onItemUpdated(int numberOfRows) {
                        if (numberOfRows > 0) {
                            Utils.showToast(context, context.getString(R.string.note_has_been_moved));
                            Utils.incrementFolderCount(menu, newFolderId, 1);
                            Utils.decrementFolderCount(menu, (int) folder.getId(), 1);
                            reloadList();
                        }
                    }
                });
            }
        };
    }

    public void deleteNote(final Note note) {
        Utils.showToast(context, context.getString(R.string.moving_to_trash));

        ContentValues contentValues = new ContentValues(1);
        contentValues.put(Constants.DELETED_COL, Constants.TRUE);
        helper.updateNote(note.getId(), contentValues, new DatabaseHelper.OnItemUpdateListener() {
            @Override
            public void onItemUpdated(int numberOfRows) {
                if (numberOfRows > 0) {
                    Utils.updateConnectedWidgets(context, note.getId()); //TODO update and res
                    Utils.updateAllEdgePanels(context);
                    preferences.edit().putString(Constants.EDGE_VISIBLE_NOTES_KEY,preferences.
                            getString(Constants.EDGE_VISIBLE_NOTES_KEY,"").
                            replace(";" + note.getId() + ";", ";")).apply();

                    Utils.incrementFolderCount(menu, (int) Utils.getTrashNavId(context), 1);
                    Utils.decrementFolderCount(menu, (int) folder.getId(), 1);
                    reloadList();
                }
            }
        });
    }

    private Dialog getTrashNoteActionDialog(final Note note){
        String[] items = new String[]{getString(R.string.restore), getString(R.string.delete)};

        return new AlertDialog.Builder(context).setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        handleRestoreFromTrashAction(note);
                        break;
                    case 1:
                        handleRemoveFromTrashAction(note);
                        break;
                }
            }
        }).create();
    }

    private void handleRemoveFromTrashAction(Note note){
        Utils.getConfirmationDialog(context, getString(R.string.do_you_want_to_delete_this_note_from_trash), getRemoveNoteFromTrashAction(note)).show();
    }

    private void handleRestoreFromTrashAction(Note note){
        Utils.getConfirmationDialog(context, getString(R.string.do_you_want_to_restore_this_note_from_trash), getRestoreNoteFromTrashAction(note)).show();
    }

    private DialogInterface.OnClickListener getRemoveNoteFromTrashAction(final Note note){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                helper.removeNote(note.getId(), new DatabaseHelper.OnItemRemoveListener() {
                    @Override
                    public void onItemRemoved(int numberOfRows) {
                        if(numberOfRows > 0){
                            Utils.decrementFolderCount(menu, (int) Utils.getTrashNavId(context), 1);

                            Utils.showToast(context, context.getString(R.string.note_was_removed));

                            reloadList();
                        }
                    }
                });
            }
        };
    }

    private DialogInterface.OnClickListener getRestoreNoteFromTrashAction(final Note note){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                ContentValues contentValues = new ContentValues(1);
                contentValues.put(Constants.DELETED_COL, Constants.FALSE);
                helper.updateNote(note.getId(), contentValues, new DatabaseHelper.OnItemUpdateListener() {
                    @Override
                    public void onItemUpdated(int numberOfRows) {
                        if (numberOfRows > 0) {
                            Utils.decrementFolderCount(menu, (int) Utils.getTrashNavId(context), 1);

                            helper.getColumnValue(Constants.NOTES_TABLE, Constants.FOLDER_ID_COL, note.getId(), new DatabaseHelper.OnIntFieldLoadListener() {
                                @Override
                                public void onIntLoaded(int value) {
                                    Utils.incrementFolderCount(menu, value, 1);
                                }
                            });

                            Utils.showToast(context, context.getString(R.string.notes_was_restored));
                            Utils.updateConnectedWidgets(context, note.getId());
                            Utils.updateAllEdgePanels(context);

                            reloadList();
                        }
                    }
                });
            }
        };
    }

    class NotesRecyclerAdapter extends RecyclerView.Adapter<NotesRecyclerAdapter.DoubleLineViewHolder> {
        private final Calendar calendar;
        private OnNoteClickListener listener;
        private ArrayList<Note> notes;



        public NotesRecyclerAdapter(ArrayList<Note> notes, OnNoteClickListener listener) {
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

        class DoubleLineViewHolder extends RecyclerView.ViewHolder{
            public final AppCompatTextView titleTextView;
            public final AppCompatTextView subtitleTextView;

            public DoubleLineViewHolder(final View itemView){
                super(itemView);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(listener != null) {
                            Note note = new Note();
                            note.setId(notes.get(getLayoutPosition()).getId());
                            note.setType(notes.get(getLayoutPosition()).getType());

                            listener.onNoteClicked(note);
                        }
                    }
                });
                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        Note note = new Note();
                        note.setId(notes.get(getLayoutPosition()).getId());
                        note.setType(notes.get(getLayoutPosition()).getType());

                        if(folder.getId() == Utils.getTrashNavId(context))
                            getTrashNoteActionDialog(note).show();
                        else
                            getNoteActionDialog(note).show();
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


