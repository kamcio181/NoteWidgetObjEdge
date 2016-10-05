package com.apps.home.notewidget.utils;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.apps.home.notewidget.R;

public abstract class BasicTrashFragment extends BasicNoteFragment{

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_delete_from_trash:
                handleRemoveFromTrashAction();
            case R.id.action_restore_from_trash:
                handleRestoreFromTrashAction();
                break;
        }
        return true;
    }

    private void handleRemoveFromTrashAction(){
        Utils.getConfirmationDialog(context, getString(R.string.do_you_want_to_delete_this_note_from_trash), getRemoveNoteFromTrashAction()).show();
    }

    private void handleRestoreFromTrashAction(){
        Utils.getConfirmationDialog(context, getString(R.string.do_you_want_to_restore_this_note_from_trash), getRestoreNoteFromTrashAction()).show();
    }

    private DialogInterface.OnClickListener getRemoveNoteFromTrashAction(){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                    helper.removeNote(note.getId(), new DatabaseHelper.OnItemRemoveListener() {
                        @Override
                        public void onItemRemoved(int numberOfRows) {
                            if(numberOfRows > 0){
                                getActivityMenu();
                                Utils.decrementFolderCount(menu, (int) Utils.getTrashNavId(context), 1);

                                Utils.showToast(context, context.getString(R.string.note_was_removed));

                                ((AppCompatActivity)context).onBackPressed();
                            }
                        }
                    });
            }
        };
    }

    private DialogInterface.OnClickListener getRestoreNoteFromTrashAction(){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                    ContentValues contentValues = new ContentValues(1);
                    contentValues.put(Constants.DELETED_COL, Constants.FALSE);
                    helper.updateNote(note.getId(), contentValues, new DatabaseHelper.OnItemUpdateListener() {
                        @Override
                        public void onItemUpdated(int numberOfRows) {
                            if (numberOfRows > 0) {
                                getActivityMenu();
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

                                ((AppCompatActivity)context).onBackPressed();
                            }
                        }
                    });
            }
        };
    }
}
