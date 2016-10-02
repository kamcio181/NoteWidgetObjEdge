package com.apps.home.notewidget;


import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.apps.home.notewidget.edge.EdgeConfigActivity;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.ContentGetter;
import com.apps.home.notewidget.utils.DatabaseHelper;
import com.apps.home.notewidget.utils.FolderChangeListener;
import com.apps.home.notewidget.utils.NoteUpdateListener;
import com.apps.home.notewidget.utils.ParametersUpdateListener;
import com.apps.home.notewidget.utils.SaveListener;
import com.apps.home.notewidget.utils.TitleChangeListener;
import com.apps.home.notewidget.utils.Utils;

import java.util.Calendar;
import java.util.regex.Pattern;

public class NoteFragment extends Fragment implements TitleChangeListener, NoteUpdateListener,
        FolderChangeListener, SaveListener, ContentGetter, ParametersUpdateListener{
    private static final String TAG = "NoteFragment";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_PARAM3 = "param3";
    private AppCompatEditText noteEditText;
    private boolean skipSaving = false;
    private boolean isNewNote;
    private Context context;
    private TextWatcher textWatcher;
    private boolean skipTextCheck = false;
    private int editTextSelection;
    private String newLine;
    private Note note;
    private DatabaseHelper helper;
    private static EdgeVisibilityReceiver receiver;
    private Menu menu;


    public NoteFragment() {
        // Required empty public constructor
    }

//    public static NoteFragment newInstance(boolean isNewNote, Note note) {
//        NoteFragment fragment = new NoteFragment();
//        Bundle args = new Bundle();
//        args.putBoolean(ARG_PARAM1, isNewNote);
//        args.putSerializable(ARG_PARAM2, note);
//        fragment.setArguments(args);
//        return fragment;
//    }

    public static NoteFragment newInstance(Note note) {
        NoteFragment fragment = new NoteFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PARAM1, true);
        args.putSerializable(ARG_PARAM2, note);
        fragment.setArguments(args);
        return fragment;
    }

    public static NoteFragment newInstance(long noteId) {
        NoteFragment fragment = new NoteFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PARAM1, false);
        args.putLong(ARG_PARAM3, noteId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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
        setTextWatcher();
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_note, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(!context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).
                getBoolean(Constants.SKIP_MULTILEVEL_NOTE_MANUAL_DIALOG_KEY, false))
            Utils.getMultilevelNoteManualDialog(context).show();

        noteEditText = (AppCompatEditText) view.findViewById(R.id.noteEditText);
        noteEditText.addTextChangedListener(textWatcher);
        noteEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).getInt(Constants.NOTE_TEXT_SIZE_KEY, 14));
        newLine = System.getProperty("line.separator");
        Log.e(TAG, "skip start " + skipTextCheck);
        if(!isNewNote) {
            Log.e(TAG, "skip old " + skipTextCheck);
            loadNote();

        } else {
            Log.e(TAG, "skip new " + skipTextCheck);
            note.setTitle(getString(R.string.untitled));
            note.setCreatedAt(Calendar.getInstance().getTimeInMillis());
            note.setDeletedState(Constants.FALSE);
            setTitleAndSubtitle();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.v(TAG, "onCreateOptionsMenu");
        super.onCreateOptionsMenu(menu, inflater);

        getActivity().getMenuInflater().inflate(R.menu.menu_note, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(TAG, "onOptionsItemSelected");

        switch (item.getItemId()){
            case R.id.action_move_to_other_folder:
                handleNoteMoveAction();
                break;
            case R.id.action_delete:
                deleteNote();
                break;
            case R.id.action_discard_changes:
                discardChanges();
                break;
        }
        return true;
    }

    private void handleNoteMoveAction(){
        menu = ((MainActivity)context).getNavigationViewMenu();
        Dialog dialog = Utils.getFolderListDialog(context, menu,
                new int[]{(int) note.getFolderId(), (int) Utils.getTrashNavId(context)},
                getString(R.string.choose_new_folder), getMoveNoteToOtherFolderAction());
        if(dialog != null)
            dialog.show();
    }

    private DialogInterface.OnClickListener getMoveNoteToOtherFolderAction(){
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
                            Utils.decrementFolderCount(menu, (int) note.getFolderId(), 1);

//                            if (actionBarMenuItemClicked) {
                            //Update current folderId for folder fragment displayed onBackPressed
                            ((MainActivity)context).setNavigationItemChecked(newFolderId);
                            note.setFolderId(newFolderId);


//                            } else {
//                                if (fragmentManager.findFragmentByTag(Constants.FRAGMENT_FOLDER) != null)
//                                    ((FolderFragment) fragmentManager.findFragmentByTag(Constants.FRAGMENT_FOLDER)).reloadList(); //TODO handle it in main Activity
//                            }
                        }
                    }
                });
            }
        };
    }

    private void setTitleAndSubtitle(){
        ActionBar actionBar = ((AppCompatActivity) context).getSupportActionBar();
        if(actionBar != null){
            actionBar.setTitle(note.getTitle());
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(note.getCreatedAt());
            Log.e(TAG, "millis " + note.getCreatedAt());
            actionBar.setSubtitle(String.format("%1$tb %1$te, %1$tY %1$tT", calendar));
//            Utils.setSubtitleMarquee(((MainActivity)context).getToolbar());
        }
    }

    private void loadNote(){
        helper.getNote(true, note.getId(), new DatabaseHelper.OnNoteLoadListener() {
            @Override
            public void onNoteLoaded(Note note) {
                NoteFragment.this.note = note;
                noteEditText.setText(Html.fromHtml(note.getNote()));
                setTitleAndSubtitle();
            }
        });
    }

    private void setTextWatcher(){
        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //Log.e(TAG, "before "+s.toString() + " start "+start+" count "+count+" aft ");
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if(!skipTextCheck && (start+count) <= s.length()){
                    boolean isFirstLine = start == 0;
                    String newText = isFirstLine ? s.subSequence(0, count).toString() : s.subSequence(start - 1, start + count).toString();
                    if((!isFirstLine && (newText.startsWith(newLine + "-") || newText.startsWith(newLine + "+")
                            || newText.startsWith(newLine + "*"))) || (isFirstLine && (newText.startsWith("-")
                            || newText.startsWith("+") || newText.startsWith("*")))) {
                        skipTextCheck = true;
                        editTextSelection = newText.contains("-") ? start+count + 2 : newText.contains("+")?
                                start+count + 3 : start+count + 4;
                        if(!isFirstLine)
                            newText = newText.substring(1);
                        newText = newText.startsWith("-") ? newText.replaceFirst("-", "\u0009- ") :  newText.startsWith("+")?
                                newText.replaceFirst(Pattern.quote("+"), "\u0009\u0009+ ") : newText.replaceFirst(Pattern.quote("*"), "\u0009\u0009\u0009* ");
                        String fullText = s.subSequence(0,start) + newText +s.subSequence(start+count, s.length());
                        noteEditText.setText(fullText);

                    }
                } else if (skipTextCheck) {
                    skipTextCheck = false;
                    noteEditText.setSelection(editTextSelection);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
    }

    @Override
    public void onParametersUpdated() {
        noteEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).getInt(Constants.NOTE_TEXT_SIZE_KEY, 14));
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
        Log.e(TAG, "Stop");
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

    public void deleteNote() {
        skipSaving = true;
        Utils.showToast(context, context.getString(R.string.moving_to_trash));
        note.setNote(noteEditText.getText().toString());
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

    public void discardChanges() {
        skipSaving = true;
        Utils.showToast(context, context.getString(R.string.closed_without_saving));
        ((AppCompatActivity)context).onBackPressed();
    }

    @Override
    public void saveNote(final boolean quitAfterSaving) {
        Utils.showToast(context.getApplicationContext(), getString(R.string.saving));
        if(isNewNote) {
            note.setNote(noteEditText.getText().toString());
            helper.createNote(note, new DatabaseHelper.OnItemInsertListener() {
                @Override
                public void onItemInserted(long id) {
                    note.setId(id);
                    isNewNote = false;
                    Utils.incrementFolderCount(((MainActivity) context).getNavigationViewMenu(), (int) note.getFolderId(), 1);// TODO
                    if(quitAfterSaving)
                        ((AppCompatActivity)context).onBackPressed();
                }
            });
        } else{
            note.setNote(noteEditText.getText().toString());
            helper.updateNote(note, new DatabaseHelper.OnItemUpdateListener() {
                @Override
                public void onItemUpdated(int numberOfRows) {
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
        if(noteEditText.getText().length() == 0) {
            Utils.showToast(context, context.getString(R.string.note_is_empty_or_was_not_loaded_yet));
        }
        return noteEditText.getText().toString();
    }
}

