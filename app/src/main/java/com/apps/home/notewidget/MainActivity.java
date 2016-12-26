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

import com.apps.home.notewidget.calendar.CalendarActivity;
import com.apps.home.notewidget.objects.Folder;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.settings.SettingsActivity;
import com.apps.home.notewidget.utils.AdvancedNoteFragment;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.DatabaseHelper;
import com.apps.home.notewidget.utils.ParametersUpdateListener;
import com.apps.home.notewidget.utils.TitleChangeListener;
import com.apps.home.notewidget.utils.Utils;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, FolderFragment.OnNoteClickListener,
        View.OnClickListener, SearchFragment.OnItemClickListener{
    private static final String TAG = "MainActivity";
    private static final int FRAGMENT_CONTAINER_ID = R.id.container;
    private final UpdateReceiver receiver = new UpdateReceiver();
    private final Handler handler = new Handler();
    private final Runnable resetExitFlag = new Runnable() {
        @Override
        public void run() {
            exit = false;
        }
    };
    private Context context;
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private FloatingActionButton fab;
    private IntentFilter intentFilter;
    private FragmentManager fragmentManager;
    private SharedPreferences preferences;
    private NavigationView navigationView;
    private Menu menu;
    private int currentFolderId;
    private int myNotesNavId;
    private int trashNavId;
    private String textToFind;
    private boolean exit = false;
    private DatabaseHelper helper;
    private Note note;
    private ActionBar actionBar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
//        Fabric.with(context, new Crashlytics());

        fragmentManager = getSupportFragmentManager();
        preferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        helper = new DatabaseHelper(context);

        setupActivityViews();
        getBaseFolderIds();
        setupIntentFilterForUpdateReceiver();

        loadNavigationViewItems();
    }


    private void getBaseFolderIds(){
        myNotesNavId = (int) Utils.getMyNotesNavId(context);
        Log.d(TAG, "My notes folder id " + myNotesNavId);
        trashNavId = (int) Utils.getTrashNavId(context);
        Log.d(TAG, "Trash folder id " + trashNavId);
        currentFolderId = preferences.getInt(Constants.STARTING_FOLDER_KEY, (int) Utils.getMyNotesNavId(context));
        Log.d(TAG, "Starting folder id " + currentFolderId);
    }

    private void setupIntentFilterForUpdateReceiver(){
        intentFilter = new IntentFilter(Constants.ACTION_RELOAD_MAIN_ACTIVITY);
        intentFilter.addAction(Constants.ACTION_UPDATE_NOTE);
        intentFilter.addAction(Constants.ACTION_UPDATE_NOTE_PARAMETERS);
    }

    private void setupActivityViews(){
        setContentView(R.layout.activity_main);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

        setupActionBar();
        setupNavigationViewViews();
        Utils.hideShadowSinceLollipop(context);
    }

    private void setupActionBar(){
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setContentInsetsAbsolute(0,0);
        actionBar = getSupportActionBar();
        Utils.setTitleMarquee(toolbar);
    }

    private void setupNavigationViewViews(){
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        menu = navigationView.getMenu();
    }

    private void loadNavigationViewItems(){
        helper.getFolders(new DatabaseHelper.OnFoldersLoadListener() {
            @Override
            public void onFoldersLoaded(ArrayList<Folder> folders) {
                if (folders != null) {
                    Log.d(TAG, "Folders loaded");
                    setupNavigationViewItems(folders);
                    attachFragment(Constants.FRAGMENT_FOLDER);
                }
            }
        });
    }

    private void setupNavigationViewItems(ArrayList<Folder> folders){
        resetNavigationViewMenu();
        addNavigationViewItems(folders);
        checkCurrentFolderInNavigationView();
   }

    private void resetNavigationViewMenu(){
        if(menu.size()!=0)
            Utils.removeAllMenuItems(menu);

        navigationView.inflateMenu(R.menu.activity_main_drawer);
        Log.d(TAG, "NavigationView menu inflated");
    }

    private void addNavigationViewItems(ArrayList<Folder> folders){
        for (Folder f : folders){
            int id = (int) f.getId();
            int order = 11;
            int icon = R.drawable.ic_nav_black_folder;

            if(id == myNotesNavId) {
                order = 10;
                icon = R.drawable.ic_nav_black_home;
            } else if (id == trashNavId) {
                order = 10000;
                icon = R.drawable.ic_nav_black_trash;
            }

            addMenuCustomItem(id, order, f.getName(), icon, f.getCount());
        }
    }

    private void addMenuCustomItem(int id, int orderId, String name, int icon, int notesCount){
        MenuItem newItem = menu.add(R.id.nav_group_notes, id, orderId, name);
        newItem.setIcon(icon);
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") AppCompatTextView countTextView = (AppCompatTextView) inflater.inflate(R.layout.nav_folder_item, null);
        countTextView.setText(String.valueOf(notesCount));
        newItem.setActionView(countTextView);
        newItem.setCheckable(true);
    }

    private void checkCurrentFolderInNavigationView(){
        if(!isCurrentFolderPresentInNavigationView())
            currentFolderId = myNotesNavId;

        navigationView.setCheckedItem(currentFolderId);
    }

    private boolean isCurrentFolderPresentInNavigationView(){
        return menu.findItem(currentFolderId) == null;
    }

    private void attachFragment(String fragment) {
        attachFragment(fragment, false);
    }

    private void attachFragment (String fragment, boolean isNewFragment){
        Log.d(TAG, fragment);
        Fragment fragmentToAttach = null;
        boolean fabVisible = false;
        switch (fragment){
            case Constants.FRAGMENT_FOLDER:
                textToFind = "";
                fragmentToAttach = FolderFragment.newInstance(currentFolderId);

                if(currentFolderId != trashNavId)  //Folder list
                    fabVisible = true;
                if(currentFolderId != trashNavId && currentFolderId != myNotesNavId)
                    setOnTitleClickListener(true);
                else
                    setOnTitleClickListener(false);

                navigationView.setCheckedItem(currentFolderId);
                break;
            case Constants.FRAGMENT_NOTE:

                setOnTitleClickListener(true);
                if(isNewFragment) {
                    note = new Note();
                    note.setType(Constants.TYPE_NOTE);
                    note.setFolderId(currentFolderId);
                    fragmentToAttach = NoteFragment.newInstance(note);
                } else
                    fragmentToAttach = NoteFragment.newInstance(note.getId());
                break;
            case Constants.FRAGMENT_LIST:
                setOnTitleClickListener(true);
                if(isNewFragment){
                    note = new Note();
                    note.setType(Constants.TYPE_LIST);
                    note.setFolderId(currentFolderId);
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
        fragmentManager.beginTransaction().replace(FRAGMENT_CONTAINER_ID, fragmentToAttach, fragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commitAllowingStateLoss();
        Log.e(TAG, "attached fragment");
        if(fabVisible)
            fab.show();
        else
            fab.hide();
    }

    private void reloadMainActivityAfterRestore(){
        currentFolderId = myNotesNavId;
        attachFragment(Constants.FRAGMENT_FOLDER);
        loadNavigationViewItems();
    }

    @Override
    public void onBackPressed() {

        String attachedFragment = fragmentManager.findFragmentById(FRAGMENT_CONTAINER_ID).getTag();
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
                        handler.postDelayed(resetExitFlag, 5000);
                        Utils.showToast(this, getString(R.string.press_back_button_again_to_exit));
                    } else {
                        //Exit flag reset and canceling exit runnable in onStop method to handle home button presses
                        super.onBackPressed();
                    }
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if(fragmentManager.findFragmentById(FRAGMENT_CONTAINER_ID) != null) {
            switch (fragmentManager.findFragmentById(FRAGMENT_CONTAINER_ID).getTag()) {
                case Constants.FRAGMENT_SEARCH:
                    getMenuInflater().inflate(R.menu.menu_empty, menu);
                    break;
                case Constants.FRAGMENT_FOLDER:
                    if (currentFolderId == myNotesNavId)
                        getMenuInflater().inflate(R.menu.menu_my_notes_list, menu);
                    else if (currentFolderId == trashNavId)
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

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        int id = item.getItemId();
        Log.d(TAG, "NavigationView item clicked: " + id + " " + item.getTitle().toString());

        switch (id){
            case R.id.nav_calendar:
                startActivity(new Intent(this, CalendarActivity.class));
                break;
            case R.id.nav_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.nav_about:
                Utils.showToast(this, getString(R.string.created_by));
                break;
            default:
                openFolderWithNotes(id);
                break;
        }

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        exit = false;
        handler.removeCallbacks(resetExitFlag);
        Log.d(TAG, "Stop");
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
        currentFolderId = (int) id;
        attachFragment(Constants.FRAGMENT_FOLDER);
    }

    public void setNavigationItemChecked(int folderId){
        navigationView.setCheckedItem(folderId);
        currentFolderId = folderId;
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
                        isFolderFragmentAttached() ? getString(R.string.set_folder_name) : getString(R.string.set_note_title),
                        32, isFolderFragmentAttached() ? getString(R.string.folder_name) : getString(R.string.note_name),
                        new Utils.OnNameSet() {
                            @Override
                            public void onNameSet(String name) {
                                setNewTitle(name);
                            }
                        }).show();
            }
        };
    }

    private boolean isFolderFragmentAttached(){
        return fragmentManager.findFragmentByTag(Constants.FRAGMENT_FOLDER) != null;
    }

    public void setupNavigationViewItems(Folder folder){

        addMenuCustomItem((int) folder.getId(), 11, folder.getName(), R.drawable.ic_nav_black_folder, 0);

        openFolderWithNotes((int) folder.getId());
    }



	private void setNewTitle(String title){
		title = setTitle(title);

        Fragment fragment = fragmentManager.findFragmentById(FRAGMENT_CONTAINER_ID);
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
                switch (fragmentManager.findFragmentById(FRAGMENT_CONTAINER_ID).getTag()){
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





    //Interface from FolderFragment
    @Override
    public void onNoteClicked(Note note) {
        this.note = note;
        if (currentFolderId == trashNavId) {
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
        return menu;
    }

    class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent arg1) {
            if(arg1 != null){
                final Fragment fragment = fragmentManager.findFragmentById(FRAGMENT_CONTAINER_ID);
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
