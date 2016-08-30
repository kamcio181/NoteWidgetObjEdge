package com.apps.home.notewidget;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.objects.Folder;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.settings.SettingsActivity;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.ContentGetter;
import com.apps.home.notewidget.utils.DatabaseHelper;
import com.apps.home.notewidget.utils.DeleteListener;
import com.apps.home.notewidget.utils.DiscardChangesListener;
import com.apps.home.notewidget.utils.FolderChangeListener;
import com.apps.home.notewidget.utils.NoteUpdateListener;
import com.apps.home.notewidget.utils.SaveListener;
import com.apps.home.notewidget.utils.TitleChangeListener;
import com.apps.home.notewidget.utils.Utils;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, FolderFragment.OnNoteClickListener,
        View.OnClickListener, SearchFragment.OnItemClickListener{
    private static final String TAG = "MainActivity";
    private Context context;
    private Toolbar toolbar;
    private FloatingActionButton fab;
    private FragmentManager fragmentManager;
    private SharedPreferences preferences;
    private NavigationView navigationView;
    private int folderId;
    private int myNotesNavId;
    private int trashNavId;
    private String textToFind;
    private boolean exit = false;
    private Handler handler = new Handler();
    private Runnable exitRunnable;
    private DatabaseHelper helper;
    private ArrayList<Folder> folders;
    private Note note;
    private ActionBar actionBar;
    private int fragmentContainerId = R.id.container;
    //private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        fragmentManager = getSupportFragmentManager();
        preferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        helper = new DatabaseHelper(this);
        setResetExitFlagRunnable();

        myNotesNavId = (int) Utils.getMyNotesNavId(this);
        Log.e(TAG, "my notes id " + myNotesNavId);
        trashNavId = (int) Utils.getTrashNavId(this);
        Log.e(TAG, "trash id " + trashNavId);
        folderId = preferences.getInt(Constants.STARTING_FOLDER_KEY, (int) Utils.getMyNotesNavId(context));
        preferences.edit().putBoolean(Constants.NOTE_UPDATED_FROM_WIDGET, false)
        .putBoolean(Constants.RELOAD_MAIN_ACTIVITY_AFTER_RESTORE_KEY, false).apply();//reset flag
        Log.e(TAG, "created");


        Utils.hideShadowSinceLollipop(this);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        Log.e(TAG, "nav created");
        loadNavViewItems();

//        helper.getNotes(true, new DatabaseHelper.OnNotesLoadListener() {
//            @Override
//            public void onNotesLoaded(ArrayList<Note> notes) {
//                if (notes != null) {
//                    for (Note n : notes)
//                        Log.e(TAG, n.toString());
//                }
//            }
//        });
    }

    private void reloadMainActivityAfterRestore(){
        folderId = myNotesNavId;
        attachFragment(Constants.FRAGMENT_FOLDER);
        loadNavViewItems();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        String attachedFragment = fragmentManager.findFragmentById(fragmentContainerId).getTag();
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            switch (attachedFragment) {
                case Constants.FRAGMENT_NOTE:
                case Constants.FRAGMENT_LIST:
                case Constants.FRAGMENT_TRASH_NOTE:
                    if(textToFind.length()==0)
                        attachFragment(Constants.FRAGMENT_FOLDER);
                    else
                        attachFragment(Constants.FRAGMENT_SEARCH);
                    break;
                case Constants.FRAGMENT_SEARCH:
                    attachFragment(Constants.FRAGMENT_FOLDER);
                    break;
                case Constants.FRAGMENT_FOLDER:
                    if(!exit){
                        exit = true;
                        handler.postDelayed(exitRunnable, 5000);
                        Utils.showToast(this, getString(R.string.press_back_button_again_to_exit));
                    } else {
                        //Exit flag reset and canceling exit runnable in onStop method to handle home button presses
                        super.onBackPressed();
                    }
                break;
            }
        }
    }

    private void setResetExitFlagRunnable(){
        exitRunnable = new Runnable() {
            @Override
            public void run() {
                exit = false;
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if(fragmentManager.findFragmentById(fragmentContainerId) != null) {
            switch (fragmentManager.findFragmentById(fragmentContainerId).getTag()) {
                case Constants.FRAGMENT_SEARCH:
                    getMenuInflater().inflate(R.menu.menu_empty, menu);
                    break;
                case Constants.FRAGMENT_FOLDER:
                    if (folderId == myNotesNavId)
                        getMenuInflater().inflate(R.menu.menu_my_notes_list, menu);
                    else if (folderId == trashNavId)
                        getMenuInflater().inflate(R.menu.menu_trash, menu);
                    else
                        getMenuInflater().inflate(R.menu.menu_folder_list, menu);
                    break;
                case Constants.FRAGMENT_NOTE:
                    getMenuInflater().inflate(R.menu.menu_note, menu);
                    break;
                case Constants.FRAGMENT_LIST:
                    //TODO note + some actions from original app
                    break;
                case Constants.FRAGMENT_TRASH_NOTE:
                    getMenuInflater().inflate(R.menu.menu_note_trash, menu);
                    break;
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        String confirmationTitle;

        switch (id){
            case R.id.action_sort_by_date:
                setOrderType(true);
                break;
            case R.id.action_sort_by_title:
                setOrderType(false);
                break;
            case R.id.action_delete:
            case R.id.action_discard_changes:
                if(id == R.id.action_delete){
                    ((DeleteListener)fragmentManager.findFragmentById(fragmentContainerId)).deleteNote();
                }
                else
                    ((DiscardChangesListener)fragmentManager.findFragmentById(fragmentContainerId)).discardChanges();
                break;
            case R.id.action_delete_all:
            case R.id.action_restore_all:
                confirmationTitle = id == R.id.action_delete_all ? getString(R.string.do_you_want_to_delete_all_notes) :
                        getString(R.string.do_you_want_to_restore_all_notes);
                Utils.getConfirmationDialog(this, confirmationTitle, getRestoreOrRemoveAllNotesFromTrashAction(id)).show();
                break;
            case R.id.action_delete_from_trash:
            case R.id.action_restore_from_trash:
                handleRestoreOrRemoveFromTrashAction(id, true);
                break;
            case R.id.action_add_nav_folder:
                handleAddFolder();
                //startActivity(new Intent(this, WidgetManualActivity.class));
                break;
            case R.id.action_delete_nav_folder:
                Utils.getConfirmationDialog(this, getString(R.string.do_you_want_to_delete_this_folder_and_all_associated_notes),
                        getRemoveFolderAndAllNotesAction()).show();
                break;
            case R.id.action_move_to_other_folder:
                handleNoteMoveAction(true);
                break;
            case R.id.action_search:
                attachFragment(Constants.FRAGMENT_SEARCH);
                break;
            case R.id.action_share:
                Utils.sendShareIntent(this, ((ContentGetter) fragmentManager.
                        findFragmentById(fragmentContainerId)).getContent(),
                        actionBar.getTitle().toString());
                break;
            case R.id.action_save:
                ((SaveListener)fragmentManager.findFragmentById(fragmentContainerId)).saveNote(true);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setOrderType(Boolean orderByDate){
        ((FolderFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_FOLDER)).setSortByDate(orderByDate);
    }

    public void loadNavViewItems(){
        helper.getFolders(new DatabaseHelper.OnFoldersLoadListener() {
            @Override
            public void onFoldersLoaded(ArrayList<Folder> folders) {
                if (folders != null) {
                    MainActivity.this.folders = folders;
                    Log.e(TAG, "folders got");
                    addFolderToNavView(folders);
                }
            }
        });
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.

        int id = item.getItemId();
        Log.e(TAG, "nav clicked " + id + " " + item.getTitle().toString());

        if(id == R.id.nav_settings){
            Log.e(TAG, "NAV clicked - Settings");
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_about) {
            Utils.showToast(this, getString(R.string.created_by));
            Log.e(TAG, "NAV clicked - About Activity");
        } else {
            Log.e(TAG, "NAV clicked - Other");
            openFolderWithNotes(id);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null) {
            drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        exit = false;
        handler.removeCallbacks(exitRunnable);
        Log.e(TAG, "Stop");
    }

    private void openFolderWithNotes(int id){
        folderId = id;
        attachFragment(Constants.FRAGMENT_FOLDER);
        navigationView.setCheckedItem(folderId);
    }

    private DialogInterface.OnClickListener getRestoreOrRemoveAllNotesFromTrashAction(final int action){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(action == R.id.action_restore_all)
                helper.restoreAllNotesFromTrash(new DatabaseHelper.OnFoldersLoadListener() {
                    @Override
                    public void onFoldersLoaded(ArrayList<Folder> folders) {
                        if (folders != null) {
                            Utils.setFolderCount(getNavigationViewMenu(), (int) Utils.getTrashNavId(context), 0); //Set count to 0 for trash
                            Utils.updateAllWidgets(context);
                            Utils.updateAllEdgePanels(context);
                            Menu menu = getNavigationViewMenu();
                            for (Folder f : folders) {
                                Utils.incrementFolderCount(menu, (int) f.getId(), f.getCount());
                            }
                            Utils.showToast(context, getString(R.string.all_notes_were_restored));

                            ((FolderFragment) fragmentManager.findFragmentByTag(Constants.FRAGMENT_FOLDER)).clearRecyclerViewAdapter();
                        }
                    }
                });
                else
                    helper.removeAllNotesFromTrash(new DatabaseHelper.OnItemRemoveListener() {
                        @Override
                        public void onItemRemoved(int numberOfRows) {
                            if(numberOfRows > 0){
                                Utils.setFolderCount(getNavigationViewMenu(), (int) Utils.getTrashNavId(context), 0); //Set count to 0 for trash
                                Utils.showToast(context, getString(R.string.all_notes_were_removed));

                                ((FolderFragment) fragmentManager.findFragmentByTag(Constants.FRAGMENT_FOLDER)).clearRecyclerViewAdapter();
                            }
                        }
                    });
            }
        };
    }

    private DialogInterface.OnClickListener getRestoreOrRemoveNoteFromTrashAction(final int action,
                                                                                  final boolean actionBarMenuItemClicked){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(action == R.id.action_restore_from_trash){
                    note.setDeletedState(Constants.FALSE);
                    helper.updateNote(note, new DatabaseHelper.OnItemUpdateListener() {
                        @Override
                        public void onItemUpdated(int numberOfRows) { //TODO code duplicated, toast before async task
                            if (numberOfRows > 0) {
                                Utils.decrementFolderCount(getNavigationViewMenu(), (int) Utils.getTrashNavId(context), 1);

                                Utils.showToast(context, context.getString(R.string.notes_was_restored));
                                Utils.updateConnectedWidgets(context, note.getId());
                                Utils.updateAllEdgePanels(context);
                                Utils.incrementFolderCount(getNavigationViewMenu(), (int) note.getFolderId(), 1);

                                if (!actionBarMenuItemClicked && fragmentManager.findFragmentByTag(Constants.FRAGMENT_FOLDER) != null) {
                                    ((FolderFragment) fragmentManager.findFragmentByTag(Constants.FRAGMENT_FOLDER)).reloadList();
                                } else if (actionBarMenuItemClicked) {
                                    onBackPressed();
                                }
                            }
                        }
                    });
                } else if (action == R.id.action_delete_from_trash){
                    helper.removeNote(note.getId(), new DatabaseHelper.OnItemRemoveListener() {
                        @Override
                        public void onItemRemoved(int numberOfRows) {
                            if(numberOfRows > 0){
                                Utils.decrementFolderCount(getNavigationViewMenu(), (int) Utils.getTrashNavId(context), 1);

                                Utils.showToast(context, context.getString(R.string.note_was_removed));

                                if (!actionBarMenuItemClicked && fragmentManager.findFragmentByTag(Constants.FRAGMENT_FOLDER) != null) {
                                    ((FolderFragment) fragmentManager.findFragmentByTag(Constants.FRAGMENT_FOLDER)).reloadList();
                                } else if (actionBarMenuItemClicked) {
                                    onBackPressed();
                                }
                            }
                        }
                    });
                }
            }
        };
    }

    private DialogInterface.OnClickListener getRemoveFolderAndAllNotesAction(){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                helper.removeFolder(folderId, new DatabaseHelper.OnItemRemoveListener() {
                    @Override
                    public void onItemRemoved(int numberOfRows) {
                        if (numberOfRows > 0) {
                            helper.removeAllNotesFromFolder(folderId, new DatabaseHelper.OnItemRemoveListener() {
                                @Override
                                public void onItemRemoved(int numberOfRows) {
                                    Utils.showToast(context, getString(R.string.folder_and_all_associated_notes_were_removed));
                                    Utils.updateAllWidgets(context);
                                    Utils.updateAllEdgePanels(context);
                                    removeMenuItem(navigationView.getMenu(), folderId);
                                    if (preferences.getInt(Constants.STARTING_FOLDER_KEY, -1) == folderId)
                                        preferences.edit().remove(Constants.STARTING_FOLDER_KEY).apply();
                                    openFolderWithNotes(myNotesNavId);
                                }
                            });
                        }
                    }
                });
            }
        };
    }

    private DialogInterface.OnClickListener getMoveNoteToOtherFolderAction(final boolean actionBarMenuItemClicked){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                note.setFolderId(Utils.getFolderIdFromArray(which));
                helper.updateNote(note, new DatabaseHelper.OnItemUpdateListener() {
                    @Override
                    public void onItemUpdated(int numberOfRows) {
                        if (numberOfRows > 0) {
                            Utils.showToast(context, context.getString(R.string.note_has_been_moved));
                            Menu menu = getNavigationViewMenu();
                            Utils.incrementFolderCount(menu, (int) note.getFolderId(), 1);
                            Utils.decrementFolderCount(menu, folderId, 1);

                            if (actionBarMenuItemClicked) {
                                //Update current folderId for folder fragment displayed onBackPressed
                                folderId = (int) note.getFolderId();
                                navigationView.setCheckedItem(folderId);

                                //Change folder id for note which is currently visible
                                Fragment fragment = fragmentManager.findFragmentById(fragmentContainerId);
                                String fragmentTag = fragment.getTag();
                                if (fragmentTag.equals(Constants.FRAGMENT_NOTE) || fragmentTag.equals(Constants.FRAGMENT_LIST))
                                    ((FolderChangeListener) fragment).onFolderChanged(folderId);
                            } else {
                                if (fragmentManager.findFragmentByTag(Constants.FRAGMENT_FOLDER) != null)
                                    ((FolderFragment) fragmentManager.findFragmentByTag(Constants.FRAGMENT_FOLDER)).reloadList();
                            }
                        } else
                            note.setFolderId(folderId); //TODO if this is needed?
                    }
                });
            }
        };
    }

    public void setOnTitleClickListener(boolean enable){
        if(enable)
            toolbar.setOnClickListener(noteTitleChangeOrFolderNameListener());
        else
            toolbar.setOnClickListener(null);
    }

    private View.OnClickListener noteTitleChangeOrFolderNameListener(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.getNameDialog(context, actionBar.getTitle().toString(),
                        fragmentManager.findFragmentByTag(Constants.FRAGMENT_FOLDER) != null ? getString(R.string.set_folder_name) : getString(R.string.set_note_title),
                        new Utils.OnNameSet() {
                            @Override
                            public void onNameSet(String name) {
                                setNoteTitleOrFolderName(name);
                            }
                        }).show();
            }
        };
    }

    private Dialog getNoteActionDialog(){
        final boolean trashFolder = folderId == Utils.getTrashNavId(context);
        String[] items = trashFolder? new String[]{getString(R.string.restore), getString(R.string.delete)}
                : new String[]{getString(R.string.open), getString(R.string.share), getString(R.string.move_to_other_folder), getString(R.string.move_to_trash)};

        return new AlertDialog.Builder(this).setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(trashFolder){
                    handleRestoreOrRemoveFromTrashAction(which == 0?  R.id.action_restore_from_trash : R.id.action_delete_from_trash,
                            false);
                } else {
                    switch (which){
                        case 0:
                            //open
                            onNoteClicked(note, false);
                            break;
                        case 1:
                            //share
                            Utils.sendShareIntent(context, Html.fromHtml(note.getNote()).toString(), note.getTitle()); //TODO pass only active items from list
                            break;
                        case 2:
                            //move to other folder
                            handleNoteMoveAction(false);
                            break;
                        case 3:
                            //move to trash
                            Utils.showToast(context, context.getString(R.string.moving_to_trash));
                            note.setDeletedState(Constants.TRUE);
                            helper.updateNote(note, new DatabaseHelper.OnItemUpdateListener() {
                                @Override
                                public void onItemUpdated(int numberOfRows) {
                                    if (numberOfRows > 0) {
                                        Utils.updateConnectedWidgets(context, note.getId()); //TODO update and res
                                        Utils.updateAllEdgePanels(context);
                                        preferences.edit().putString(Constants.EDGE_VISIBLE_NOTES_KEY,preferences.getString(Constants.EDGE_VISIBLE_NOTES_KEY,"").replace(";" + note.getId() + ";", ";")).apply();

                                        Menu menu = getNavigationViewMenu();
                                        Utils.incrementFolderCount(menu, (int) Utils.getTrashNavId(context), 1);
                                        Utils.decrementFolderCount(menu, (int) note.getFolderId(), 1);
                                        if(fragmentManager.findFragmentByTag(Constants.FRAGMENT_FOLDER) != null)
                                            ((FolderFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_FOLDER)).reloadList();
                                    }
                                }
                            });
                            break;
                    }
                }
            }
        }).create();
    }

    private void handleAddFolder(){
        Utils.getNameDialog(context, getString(R.string.new_folder), getString(R.string.add_folder), new Utils.OnNameSet() {
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
                            addFolderToNavView(folder);
                        }
                    }
                });
            }
        }).show();
    }

    private void handleRestoreOrRemoveFromTrashAction(int action, boolean actionBarMenuItemClicked){
        String confirmationTitle = action == R.id.action_delete_from_trash ? getString(R.string.do_you_want_to_delete_this_note_from_trash) :
                getString(R.string.do_you_want_to_restore_this_note_from_trash);
        Utils.getConfirmationDialog(this, confirmationTitle, getRestoreOrRemoveNoteFromTrashAction(action, actionBarMenuItemClicked)).show();
    }

    private void handleNoteMoveAction(boolean actionBarMenuItemClicked){
        Dialog dialog = Utils.getFolderListDialog(this, navigationView.getMenu(), new int[]{folderId, trashNavId}, getString(R.string.choose_new_folder), getMoveNoteToOtherFolderAction(actionBarMenuItemClicked));
        if(dialog != null)
            dialog.show();
    }

    private void addFolderToNavView(Folder folder){

        Menu menu = getNavigationViewMenu();
        addMenuCustomItem(menu, (int) folder.getId(), 11, folder.getName(), R.drawable.ic_nav_black_folder, 0);
        folders.add(folder);

        openFolderWithNotes((int) folder.getId());
    }

    private void addFolderToNavView(ArrayList<Folder> folders){
        Menu menu = navigationView.getMenu();

        if(menu.size()!=0)
            Utils.removeAllMenuItems(menu);

        navigationView.inflateMenu(R.menu.activity_main_drawer);
        Log.e(TAG, "inflate nav ");

        for (Folder f : folders){
            long id = f.getId();
            int order = 11;
            int icon = R.drawable.ic_nav_black_folder;


            if(id == myNotesNavId) {
                order = 10;
                icon = R.drawable.ic_nav_black_home;
            } else if (id == trashNavId) {
                order = 10000;
                icon = R.drawable.ic_nav_black_trash;
            }

            addMenuCustomItem(menu, (int) id, order, f.getName(), icon, f.getCount());
        }

        if(menu.findItem(folderId) == null)
            folderId = myNotesNavId;

        navigationView.setCheckedItem(folderId);
        Log.e(TAG, "menu created");
        attachFragment(Constants.FRAGMENT_FOLDER);
    }

    private void addMenuCustomItem(Menu m, int id, int order, String name, int icon, int count){
        MenuItem newItem = m.add(R.id.nav_group_notes, id, order, name);
        newItem.setIcon(icon);
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RobotoTextView countTextView = (RobotoTextView) inflater.inflate(R.layout.nav_folder_item, null);
        countTextView.setText(String.valueOf(count));
        newItem.setActionView(countTextView);
        newItem.setCheckable(true);
    }

    private void removeMenuItem(Menu m, int id){
        m.removeItem(id);
    }

	
	private void setNoteTitleOrFolderName(String title){
		title = setTitle(title);

        Fragment fragment = fragmentManager.findFragmentById(fragmentContainerId);
        String fragmentTag = fragment.getTag();
        if(fragmentTag.equals(Constants.FRAGMENT_FOLDER) || fragmentTag.equals(Constants.FRAGMENT_NOTE)
                || fragmentTag.equals(Constants.FRAGMENT_LIST))
            ((TitleChangeListener)fragment).onTitleChanged(title);
	}

    private String setTitle(String title){
        if(title.equals(""))
            title = getString(R.string.untitled);
        else
            title = Utils.capitalizeFirstLetter(title);
        actionBar.setTitle(title);
        Log.e(TAG, "setToolbarTitle " + title);

        return title;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.fab:
                switch (fragmentManager.findFragmentById(fragmentContainerId).getTag()){
                    case Constants.FRAGMENT_FOLDER:
                        getNoteTypeDialog().show();
                        break;
                }
                break;
        }
    }

    private Dialog getNoteTypeDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        return builder.setItems(new CharSequence[]{"Note", "List"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        attachFragment(Constants.FRAGMENT_NOTE, true);
                        break;
                    case 1:
                        attachFragment(Constants.FRAGMENT_LIST, true);
                        break;
                }
            }
        }).create();
    }

    private void attachFragment(String fragment) {
        attachFragment(fragment, false);
    }

    private void attachFragment (String fragment, boolean isNew){
        Fragment fragmentToAttach = null;
        boolean fabVisible = false;
        switch (fragment){
            case Constants.FRAGMENT_FOLDER:
                textToFind = "";
                for (Folder f : folders){
                    if(folderId == f.getId()){
                        fragmentToAttach = FolderFragment.newInstance(f);
                        break;
                    }
                }

                if(folderId != trashNavId)  //Folder list
                    fabVisible = true;
                if(folderId != trashNavId && folderId != myNotesNavId)
                    setOnTitleClickListener(true);
                else
                    setOnTitleClickListener(false);
                break;
            case Constants.FRAGMENT_NOTE:
                Log.e(TAG, "NOTE FRAGMENT");
                setOnTitleClickListener(true);
                if(isNew) {
                    note = new Note();
                    note.setType(Constants.TYPE_NOTE);
                    note.setFolderId(folderId);
                }
                fragmentToAttach = NoteFragment.newInstance(isNew, note);
                break;
            case Constants.FRAGMENT_LIST:
                Log.e(TAG, "LIST FRAGMENT");
                setOnTitleClickListener(true);
                if(isNew){
                    note = new Note();
                    note.setType(Constants.TYPE_LIST);
                    note.setFolderId(folderId);
                }
                fragmentToAttach = ListFragment.newInstance(isNew, note);
                break;
            case Constants.FRAGMENT_TRASH_NOTE:
                setOnTitleClickListener(false);
                fragmentToAttach = TrashNoteFragment.newInstance(note);
                break;
            case Constants.FRAGMENT_SEARCH:
                setOnTitleClickListener(false);
                fragmentToAttach = SearchFragment.newInstance(textToFind);
                actionBar.setTitle(R.string.search);
                break;
        }
        fragmentManager.beginTransaction().replace(fragmentContainerId, fragmentToAttach, fragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commitAllowingStateLoss();
        Log.e(TAG, "attached fragment");
        if(fabVisible)
            fab.show();
        else
            fab.hide();
    }

    //Interface from FolderFragment
    @Override
    public void onNoteClicked(Note note, boolean longClick) {
        this.note = note;
        if(!longClick) {
            if (folderId == trashNavId)
                attachFragment(Constants.FRAGMENT_TRASH_NOTE);
            else if (note.getType() == Constants.TYPE_NOTE)
                attachFragment(Constants.FRAGMENT_NOTE);
            else if (note.getType() == Constants.TYPE_LIST)
                attachFragment(Constants.FRAGMENT_LIST);//TODO
        } else {
            getNoteActionDialog().show();
        }
        Log.e(TAG, "LONG CLICK " + longClick);
    }

    @Override
    public void onItemClicked(Note note, String textToFind) {
        this.textToFind = textToFind;
        this.note = note;
        if(note.getDeletedState() == Constants.TRUE)
            attachFragment(Constants.FRAGMENT_TRASH_NOTE, false);
        else if (note.getType() == Constants.TYPE_NOTE)
            attachFragment(Constants.FRAGMENT_NOTE);
        else if (note.getType() == Constants.TYPE_LIST)
            attachFragment(Constants.FRAGMENT_LIST);//TODO
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if(preferences.getBoolean(Constants.RELOAD_MAIN_ACTIVITY_AFTER_RESTORE_KEY, false))
            reloadMainActivityAfterRestore();
        else {
            final Fragment fragment = fragmentManager.findFragmentById(fragmentContainerId);
            String fragmentTag = fragment.getTag();
            if (preferences.getBoolean(Constants.NOTE_UPDATED_FROM_WIDGET, false)) {
                if(fragmentTag.equals(Constants.FRAGMENT_NOTE) || fragmentTag.equals(Constants.FRAGMENT_LIST)){
                    helper.getNote(false, note.getId(), new DatabaseHelper.OnNoteLoadListener() { //TODO common with trash
                        @Override
                        public void onNoteLoaded(Note note) {
                            if(note != null){
                                MainActivity.this.note = note;
                                ((NoteUpdateListener)fragment).onNoteUpdate(note);
                            }
                        }
                    });
                }
                else if (fragmentTag.equals(Constants.FRAGMENT_FOLDER))
                    ((FolderFragment)fragment).reloadList();
                preferences.edit().putBoolean(Constants.NOTE_UPDATED_FROM_WIDGET, false).apply(); //TODO use receivers
            }
            if (preferences.getBoolean(Constants.NOTE_TEXT_SIZE_UPDATED, false)) {
                if (fragmentTag.equals(Constants.FRAGMENT_NOTE))
                    ((NoteFragment) fragment).updateNoteTextSize();
                preferences.edit().putBoolean(Constants.NOTE_TEXT_SIZE_UPDATED, false).apply();
            }
        }
    }

    public Menu getNavigationViewMenu() {
        return navigationView.getMenu();
    }
}
