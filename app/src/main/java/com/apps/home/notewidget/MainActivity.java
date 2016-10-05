package com.apps.home.notewidget;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.apps.home.notewidget.objects.Folder;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.settings.SettingsActivity;
import com.apps.home.notewidget.utils.AdvancedNoteFragment;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.DatabaseHelper;
import com.apps.home.notewidget.utils.ParametersUpdateListener;
import com.apps.home.notewidget.utils.TitleChangeListener;
import com.apps.home.notewidget.utils.Utils;
import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;

import io.fabric.sdk.android.Fabric;


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
    private final Handler handler = new Handler();
    private Runnable exitRunnable;
    private DatabaseHelper helper;
    private ArrayList<Folder> folders;
    private Note note;
    private ActionBar actionBar;
    private final int fragmentContainerId = R.id.container;
    private static UpdateReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);
        context = this;
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setContentInsetsAbsolute(0,0);
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
        Utils.setTitleMarquee(toolbar);

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
                case Constants.FRAGMENT_TRASH_LIST:
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
                    getMenuInflater().inflate(R.menu.menu_list, menu);
                    break;
                case Constants.FRAGMENT_TRASH_NOTE:
                case Constants.FRAGMENT_TRASH_LIST:
                    getMenuInflater().inflate(R.menu.menu_note_trash, menu);
                    break;
            }
        }
        return true;
//        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case R.id.action_search:
                attachFragment(Constants.FRAGMENT_SEARCH);
                return true;
        }

        return super.onOptionsItemSelected(item);
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
    protected void onStart() {
        super.onStart();

        IntentFilter intentFilter = new IntentFilter(Constants.ACTION_RELOAD_MAIN_ACTIVITY);
        intentFilter.addAction(Constants.ACTION_UPDATE_NOTE);
        intentFilter.addAction(Constants.ACTION_UPDATE_NOTE_PARAMETERS);
        if(receiver == null)
            receiver = new UpdateReceiver();
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        exit = false;
        handler.removeCallbacks(exitRunnable);
        Log.e(TAG, "Stop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e){
            Log.e(TAG, "Receiver already unregistered");
        }
    }

    public void openFolderWithNotes(long id){
        folderId = (int) id;
        attachFragment(Constants.FRAGMENT_FOLDER);
        navigationView.setCheckedItem(folderId);
    }

    public void setNavigationItemChecked(int folderId){
        navigationView.setCheckedItem(folderId);
        this.folderId = folderId;
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
                        fragmentManager.findFragmentByTag(Constants.FRAGMENT_FOLDER) != null ?
                                getString(R.string.set_folder_name) : getString(R.string.set_note_title),
                        32, fragmentManager.findFragmentByTag(Constants.FRAGMENT_FOLDER) != null?
                                getString(R.string.folder_name) : getString(R.string.note_name),
                        new Utils.OnNameSet() {
                            @Override
                            public void onNameSet(String name) {
                                setNoteTitleOrFolderName(name);
                            }
                        }).show();
            }
        };
    }

    public void addFolderToNavView(Folder folder){

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
        @SuppressLint("InflateParams") AppCompatTextView countTextView = (AppCompatTextView) inflater.inflate(R.layout.nav_folder_item, null);
        countTextView.setText(String.valueOf(count));
        newItem.setActionView(countTextView);
        newItem.setCheckable(true);
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
        return builder.setItems(getResources().getStringArray(R.array.note_types), new DialogInterface.OnClickListener() {
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
                fragmentToAttach = FolderFragment.newInstance(folderId);

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
                    fragmentToAttach = NoteFragment.newInstance(note);
                } else
                    fragmentToAttach = NoteFragment.newInstance(note.getId());
                break;
            case Constants.FRAGMENT_LIST:
                Log.e(TAG, "LIST FRAGMENT");
                setOnTitleClickListener(true);
                if(isNew){
                    note = new Note();
                    note.setType(Constants.TYPE_LIST);
                    note.setFolderId(folderId);
                    fragmentToAttach = ListFragment.newInstance(note);
                } else
                    fragmentToAttach = ListFragment.newInstance(note.getId());
                break;
            case Constants.FRAGMENT_TRASH_NOTE:
                setOnTitleClickListener(false);
                fragmentToAttach = TrashNoteFragment.newInstance(note.getId());
                break;
            case Constants.FRAGMENT_TRASH_LIST:
                setOnTitleClickListener(false);
                fragmentToAttach = TrashListFragment.newInstance(note.getId());
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
    public void onNoteClicked(Note note) {
        this.note = note;
        if (folderId == trashNavId) {
            if(note.getType() == Constants.TYPE_NOTE)
                attachFragment(Constants.FRAGMENT_TRASH_NOTE);
            else if (note.getType() == Constants.TYPE_LIST)
                attachFragment(Constants.FRAGMENT_TRASH_LIST);
        }
        else if (note.getType() == Constants.TYPE_NOTE)
            attachFragment(Constants.FRAGMENT_NOTE);
        else if (note.getType() == Constants.TYPE_LIST)
            attachFragment(Constants.FRAGMENT_LIST);
    }


    @Override
    public void onItemClicked(Note note, String textToFind) {
        this.textToFind = textToFind;
        this.note = note;
        if(note.getDeletedState() == Constants.TRUE) {
            if(note.getType() == Constants.TYPE_NOTE)
                attachFragment(Constants.FRAGMENT_TRASH_NOTE);
            else if (note.getType() == Constants.TYPE_LIST)
                attachFragment(Constants.FRAGMENT_TRASH_LIST);
        }
        else if (note.getType() == Constants.TYPE_NOTE)
            attachFragment(Constants.FRAGMENT_NOTE);
        else if (note.getType() == Constants.TYPE_LIST)
            attachFragment(Constants.FRAGMENT_LIST);
    }

    public Menu getNavigationViewMenu() {
        return navigationView.getMenu();
    }

    class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent arg1) {
            if(arg1 != null){
                final Fragment fragment = fragmentManager.findFragmentById(fragmentContainerId);
                String fragmentTag = fragment.getTag();
                switch (arg1.getAction()){
                    case Constants.ACTION_RELOAD_MAIN_ACTIVITY:
                        reloadMainActivityAfterRestore();
                        break;
                    case Constants.ACTION_UPDATE_NOTE:


                        if(fragmentTag.equals(Constants.FRAGMENT_NOTE) || fragmentTag.equals(Constants.FRAGMENT_LIST)){
                            ((AdvancedNoteFragment)fragment).onNoteUpdate();
                        }
                        else if (fragmentTag.equals(Constants.FRAGMENT_FOLDER))
                            ((FolderFragment)fragment).reloadList();
                        break;
                    case Constants.ACTION_UPDATE_NOTE_PARAMETERS:
                        if (fragmentTag.equals(Constants.FRAGMENT_NOTE) || fragmentTag.equals(Constants.FRAGMENT_LIST))
                            ((ParametersUpdateListener) fragment).onParametersUpdated();
                        break;
                }
            }
        }
    }
}
