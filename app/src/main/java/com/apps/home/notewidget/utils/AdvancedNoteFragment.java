package com.apps.home.notewidget.utils;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.apps.home.notewidget.MainActivity;
import com.apps.home.notewidget.R;
import com.apps.home.notewidget.edge.EdgeConfigActivity;
import com.apps.home.notewidget.objects.Note;

public abstract class AdvancedNoteFragment extends BasicNoteFragment implements TitleChangeListener{
    private static final String TAG = "NoteFragment";
    public static final String ARG_PARAM1 = "param1";
    public static final String ARG_PARAM2 = "param2";
    public static final String ARG_PARAM3 = "param3";

    private boolean skipSaving;
    private EdgeVisibilityReceiver receiver;

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

    public void discardChanges() {
        skipSaving = true;
        Utils.showToast(context, context.getString(R.string.closed_without_saving));
        ((AppCompatActivity)context).onBackPressed();
    }

    public void deleteNote() {
        skipSaving = true;
        Utils.showToast(getActivity(), getActivity().getString(R.string.moving_to_trash));

        note.setDeletedState(Constants.TRUE);
        helper.updateNote(note, new DatabaseHelper.OnItemUpdateListener() {
            @Override
            public void onItemUpdated(int numberOfRows) {
                if (numberOfRows > 0) {
                    Utils.updateConnectedWidgets(context, note.getId());
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

    public void saveNote(final boolean quitAfterSaving) {
        Utils.showToast(context.getApplicationContext(), getString(R.string.saving));
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

    public void onTitleChanged(String newTitle) {
        note.setTitle(newTitle);
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
            case R.id.action_save:
                saveNote(true);
                break;
            case R.id.action_share:
                Utils.sendShareIntent(context, getContent(),
                        ((AppCompatActivity)context).getTitle().toString());
                break;
        }
        return true;
    }

    public String getContent() {
        return null;
    }

    public void onNoteUpdate() {
        loadNote();
    }

    private void handleNoteMoveAction(){
        getActivityMenu();
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
                            getActivityMenu();
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
}
