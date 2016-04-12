package com.apps.home.notewidget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.apps.home.notewidget.customviews.RobotoEditText;
import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.settings.SettingsActivity;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.Utils;
import com.apps.home.notewidget.widget.WidgetProvider;

import java.lang.reflect.Field;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, NoteListFragment.OnItemClickListener,
        View.OnClickListener, SearchFragment.OnItemClickListener, NoteFragment.OnNoteAddListener{
    private static final String TAG = "MainActivity";
    private Context context;
    private long noteId = -1;
    private Toolbar toolbar;
    private FloatingActionButton fab;
    private FragmentManager fragmentManager;
    private SharedPreferences preferences;
    private NavigationView navigationView;
    private String folder = "My Notes";
    private int folderId = 1;
    private SQLiteDatabase db;
    private int myNotesNavId;
    private int trashNavId;
    private String textToFind;
    private boolean exit = false;
    private Handler handler = new Handler();
    private Runnable exitRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fragmentManager = getSupportFragmentManager();
        preferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        setResetExitFlagRunnable();

        myNotesNavId = preferences.getInt(Constants.MY_NOTES_ID_KEY, 1);
        Log.e(TAG, "my notes id " + myNotesNavId);
        trashNavId = preferences.getInt(Constants.TRASH_ID_KEY, 2);
        Log.e(TAG, "trash id " + trashNavId);
        folderId = myNotesNavId;
        preferences.edit().putBoolean(Constants.NOTE_UPDATED_FROM_WIDGET, false).apply();//reset flag

        Utils.hideShadowSinceLollipop(this);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        attachFragment(Constants.FRAGMENT_LIST);

        new LoadNavViewItems().execute();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        String attachedFragment = fragmentManager.findFragmentById(R.id.container).getTag();
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            switch (attachedFragment) {
                case Constants.FRAGMENT_NOTE:
                case Constants.FRAGMENT_TRASH_NOTE:
                    if(textToFind.length()==0)
                        attachFragment(Constants.FRAGMENT_LIST);
                    else
                        attachFragment(Constants.FRAGMENT_SEARCH);
                    break;
                case Constants.FRAGMENT_SEARCH:
                    attachFragment(Constants.FRAGMENT_LIST);
                    break;
                case Constants.FRAGMENT_LIST:
                    if(!exit){
                        exit = true;
                        handler.postDelayed(exitRunnable, 5000);
                        Utils.showToast(this, "Press back button again to exit");
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
        switch (fragmentManager.findFragmentById(R.id.container).getTag()){
            case Constants.FRAGMENT_SEARCH:
                getMenuInflater().inflate(R.menu.menu_empty, menu);
                break;
            case Constants.FRAGMENT_LIST:
                if(folderId == myNotesNavId)
                    getMenuInflater().inflate(R.menu.menu_my_notes_list, menu);
                else if (folderId == trashNavId)
                    getMenuInflater().inflate(R.menu.menu_trash, menu);
                else
                    getMenuInflater().inflate(R.menu.menu_folder_list, menu);
                break;
            case Constants.FRAGMENT_NOTE:
                getMenuInflater().inflate(R.menu.menu_note, menu);
                break;
            case Constants.FRAGMENT_TRASH_NOTE:
                getMenuInflater().inflate(R.menu.menu_note_trash, menu);
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        String confirmationTitle;

        //noinspection SimplifiableIfStatement
        switch (id){
            case R.id.action_sort_by_date:
                setOrderType(true);
                break;
            case R.id.action_sort_by_title:
                setOrderType(false);
                break;
            case R.id.action_delete:
            case R.id.action_discard_changes:
                if(id == R.id.action_delete)
                    ((NoteFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_NOTE)).deleteNote();
                else
                    ((NoteFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_NOTE)).discardChanges();
                if(textToFind.length()==0)
                    attachFragment(Constants.FRAGMENT_LIST);
                else
                    attachFragment(Constants.FRAGMENT_SEARCH);
                break;
            case R.id.action_delete_all:
            case R.id.action_restore_all:
                confirmationTitle = id == R.id.action_delete_all ? "Do you want to delete all notes?" :
                        "Do you want to restore all notes?";
                Utils.getConfirmationDialog(this, confirmationTitle, getRestoreOrRemoveAllNotesFromTrashAction(id)).show();
                break;
            case R.id.action_delete_from_trash:
            case R.id.action_restore_from_trash:
                confirmationTitle = id == R.id.action_delete_from_trash ? "Do you want to delete this note from trash?" :
                        "Do you want to restore this note from trash?";
                Utils.getConfirmationDialog(this, confirmationTitle, getRestoreOrRemoveNoteFromTrashAction(id)).show();
                break;
            case R.id.action_add_nav_folder:
                addFolderDialog().show();
                break;
            case R.id.action_delete_nav_folder:
                Utils.getConfirmationDialog(this, "Do you want to delete this folder and all associated notes?",
                        getRemoveFolderAndAllNotesAction()).show();
                break;
            case R.id.action_move_to_other_folder:
                Utils.getFolderListDialog(this, navigationView.getMenu(), folderId, trashNavId, getMoveNoteToOtherFolderAction()).show();
                break;
            case R.id.action_search:
                attachFragment(Constants.FRAGMENT_SEARCH);
                break;
            case R.id.action_share:
                Utils.sendShareIntent(this, ((NoteFragment) fragmentManager.
                        findFragmentByTag(Constants.FRAGMENT_NOTE)).getNoteText(),
                        getSupportActionBar().getTitle().toString());
                break;
            case R.id.action_save:
                onBackPressed();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setOrderType(Boolean orderByDate){
        ((NoteListFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_LIST)).setSortByDate(orderByDate);
    }



    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.

        int id = item.getItemId();
        Log.e(TAG, "nav clicked " + id + " " + item.getTitle().toString());

        if(id == R.id.nav_settings){
            //TODO open settings activity
            Log.e(TAG, "NAV clicked - Settings");
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_about) {
            //TODO open about activity
            Log.e(TAG, "NAV clicked - About Activity");
        } else {
            Log.e(TAG, "NAV clicked - Other");
            openFolderWithNotes(id);
        }
        
        Log.e(TAG, "FOLDER after click = "+folder);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        exit = false;
        handler.removeCallbacks(exitRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Utils.closeDb();
    }

    private void openFolderWithNotes(int id){
        folder = navigationView.getMenu().findItem(id).getTitle().toString();
        folderId = id;
        attachFragment(Constants.FRAGMENT_LIST);
        setTitle(folder);
        navigationView.setCheckedItem(folderId);
    }

    private DialogInterface.OnClickListener getRestoreOrRemoveAllNotesFromTrashAction(final int action){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new RestoreOrRemoveAllNotesFromTrash().execute(action);
            }
        };
    }

    private DialogInterface.OnClickListener getRestoreOrRemoveNoteFromTrashAction(final int action){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((TrashNoteFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_TRASH_NOTE)).removeOrRestoreFromTrash(action);
            }
        };
    }

    private DialogInterface.OnClickListener getRemoveFolderAndAllNotesAction(){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new RemoveFolderAndAllNotes().execute();
            }
        };
    }

    private DialogInterface.OnClickListener getMoveNoteToOtherFolderAction(){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new MoveNoteToOtherFolder().execute(Utils.getFolderId(which));
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
                setNoteTitleOrFolderNameDialog().show();
            }
        };
    }

    private Dialog setNoteTitleOrFolderNameDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_roboto_edit_text, null);
        final RobotoEditText titleEditText = (RobotoEditText) layout.findViewById(R.id.titleEditText);
        titleEditText.setText(getSupportActionBar().getTitle().toString());
        titleEditText.setSelection(0, titleEditText.length());
        final boolean isNoteTitleEdition = fragmentManager.findFragmentByTag(Constants.FRAGMENT_NOTE)!=null;
        AlertDialog dialog = builder.setTitle(isNoteTitleEdition? "Set note title" : "Set folder name").setView(layout)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setNoteTitleOrFolderName(titleEditText.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Utils.showToast(context, "Canceled");
                    }
                }).create();
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		return dialog;
    }

    private Dialog addFolderDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_roboto_edit_text, null);
        final RobotoEditText titleEditText = (RobotoEditText) layout.findViewById(R.id.titleEditText);
        titleEditText.setText("New folder");
        titleEditText.setSelection(0, titleEditText.length());

        AlertDialog dialog = builder.setTitle("Add folder").setView(layout)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addFolderToDb(titleEditText.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Utils.showToast(context, "Canceled");
                    }
                }).create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        return dialog;
    }

    private void addFolderToNavView(int id, String name, int icon){

        Menu menu = navigationView.getMenu();
        addMenuCustomItem(menu, id, 11, name, icon, 0);

        openFolderWithNotes(id);
    }

    private void addFolderToNavView(Cursor cursor){
        Menu menu = navigationView.getMenu();

        if(menu.size()!=0)
            Utils.removeAllMenuItems(menu);

        navigationView.inflateMenu(R.menu.activity_main_drawer);

        for (int i =0; i<cursor.getCount(); i++){
            Log.e(TAG, " " + cursor.getString(cursor.getColumnIndexOrThrow(Constants.FOLDER_NAME_COL))
                    + ",  " + cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTES_COUNT_COL)));

            int id = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.ID_COL));
            int order = 11;
            if(id == myNotesNavId)
                order = 10;
            else if (id == trashNavId)
                order = 10000;

            addMenuCustomItem(menu, id, order,
                    cursor.getString(cursor.getColumnIndexOrThrow(Constants.FOLDER_NAME_COL)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(Constants.FOLDER_ICON_COL)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(Constants.NOTES_COUNT_COL)));
            cursor.moveToNext();
        }

        navigationView.setCheckedItem(folderId);
    }

    /*private void addMenuItem(Menu m, int id, int order, String name, int icon){
        MenuItem newItem = m.add(R.id.nav_group_notes, id, order, name);
        newItem.setIcon(icon);
        newItem.setCheckable(true);
    }*/

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

    private void addFolderToDb(String name){
        new PutFolderInTable().execute(name);
    }
	
	private void setNoteTitleOrFolderName(String title){
		title = setTitle(title);

        if(fragmentManager.findFragmentByTag(Constants.FRAGMENT_NOTE) != null) // Note fragment is displayed
            ((NoteFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_NOTE)).titleChanged(title);
        else if(fragmentManager.findFragmentByTag(Constants.FRAGMENT_LIST) != null){
            ((NoteListFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_LIST)).titleChanged(title);
            navigationView.getMenu().findItem(folderId).setTitle(title);
        }
	}

    private String setTitle(String title){
        if(title.equals(""))
            title = "Untitled";
        else
            title = Utils.capitalizeFirstLetter(title);
        getSupportActionBar().setTitle(title);
        Log.e(TAG, "setToolbarTitle "+title);

        return title;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.fab:
                switch (fragmentManager.findFragmentById(R.id.container).getTag()){
                    case Constants.FRAGMENT_LIST:
                        attachFragment(Constants.FRAGMENT_NOTE, true, true);
                        noteId = -1;
                        break;
                }
                break;
        }
    }

    private void attachFragment (String fragment){
        attachFragment(fragment, false, false);
    }

    private void attachFragment (String fragment, boolean isNew){
        attachFragment(fragment, isNew, false);
    }

    private void attachFragment (String fragment, boolean isNew, boolean moveToEnd){
        Fragment fragmentToAttach = null;
        boolean fabVisible = false;
        switch (fragment){
            case Constants.FRAGMENT_LIST:
                textToFind = "";
                noteId = -1;
                fragmentToAttach = NoteListFragment.newInstance(folderId, folder);

                Log.e(TAG, "FOLDER = "+folder);
                if(folderId != 2)  //Folder list
                    fabVisible = true;
                if(folderId != 2 && folderId != 1)
                    setOnTitleClickListener(true);
                else
                    setOnTitleClickListener(false);
                break;
            case Constants.FRAGMENT_NOTE:
                Log.e(TAG, "NOTE FRAGMENT");
                setOnTitleClickListener(true);
                fragmentToAttach = NoteFragment.newInstance(isNew, noteId, moveToEnd, folderId);
                break;
            case Constants.FRAGMENT_TRASH_NOTE:
                setOnTitleClickListener(false);
                fragmentToAttach = TrashNoteFragment.newInstance(noteId);
                break;
            case Constants.FRAGMENT_SEARCH:
                setOnTitleClickListener(false);
                fragmentToAttach = SearchFragment.newInstance(textToFind);
                getSupportActionBar().setTitle("Search");
                break;
        }
        fragmentManager.beginTransaction().replace(R.id.container, fragmentToAttach, fragment).commit();
        if(fabVisible)
            fab.show();
        else
            fab.hide();
    }

    //Interface from NoteListFragment
    @Override
    public void onItemClicked(int noteId) {
        this.noteId = noteId;
        if(folderId != trashNavId)
            attachFragment(Constants.FRAGMENT_NOTE, false);
        else
            attachFragment(Constants.FRAGMENT_TRASH_NOTE);
    }

    @Override
    public void onItemClicked(int noteId, boolean deleted, String textToFind) {
        this.textToFind = textToFind;
        this.noteId = noteId;
        if(deleted)
            attachFragment(Constants.FRAGMENT_TRASH_NOTE, false);
        else
            attachFragment(Constants.FRAGMENT_NOTE);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if(preferences.getBoolean(Constants.NOTE_UPDATED_FROM_WIDGET, false)) {
            if (fragmentManager.findFragmentByTag(Constants.FRAGMENT_NOTE) != null)
                ((NoteFragment) fragmentManager.findFragmentByTag(Constants.FRAGMENT_NOTE)).reloadNote();
            else if (fragmentManager.findFragmentByTag(Constants.FRAGMENT_LIST) != null)
                ((NoteListFragment) fragmentManager.findFragmentByTag(Constants.FRAGMENT_LIST)).reloadList();
            else if (fragmentManager.findFragmentByTag(Constants.FRAGMENT_TRASH_NOTE) != null)
                ((TrashNoteFragment) fragmentManager.findFragmentByTag(Constants.FRAGMENT_TRASH_NOTE)).reloadNote();
            preferences.edit().putBoolean(Constants.NOTE_UPDATED_FROM_WIDGET, false).apply();
        }
        if(preferences.getBoolean(Constants.NOTE_TEXT_SIZE_UPDATED, false)){
            if(fragmentManager.findFragmentByTag(Constants.FRAGMENT_NOTE) != null)
                ((NoteFragment) fragmentManager.findFragmentByTag(Constants.FRAGMENT_NOTE)).updateNoteTextSize();
            preferences.edit().putBoolean(Constants.NOTE_TEXT_SIZE_UPDATED, false).apply();
        }
    }

    @Override
    public void onNoteAdded(int folderId) { //TODO test and finish this, delete note
        MenuItem item = navigationView.getMenu().getItem(folderId);
        RobotoTextView count = (RobotoTextView) item.getActionView();
        count.setText(String.valueOf(Integer.parseInt(count.getText().toString())+1));//
        item.setActionView(count);
    }

    private class LoadNavViewItems extends AsyncTask<Void, Integer, Boolean>
    {
        Cursor cursor;

        @Override
        protected Boolean doInBackground(Void... params) {
            if((db = Utils.getDb(context)) != null) {

                String query ="SELECT f." + Constants.ID_COL + ", f." + Constants.FOLDER_NAME_COL
                        + ", f." + Constants.FOLDER_ICON_COL
                        + ", COUNT(n." + Constants.ID_COL + ") AS " + Constants.NOTES_COUNT_COL
                        + " FROM " + Constants.FOLDER_TABLE + " f LEFT JOIN "
                        + Constants.NOTES_TABLE + " n ON f." + Constants.ID_COL + " = n."
                        + Constants.FOLDER_ID_COL + " GROUP BY f." + Constants.ID_COL;

                cursor = db.rawQuery(query, null);
                return (cursor.getCount()>0);
            } else
                return false;

        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if(aBoolean){
                cursor.moveToFirst();
                Log.e(TAG, " count " + cursor.getCount());
                addFolderToNavView(cursor);
            }
        }
    }

    private class PutFolderInTable extends AsyncTask<String, Void, Boolean>
    {   private ContentValues contentValues;
        private int id;
        private String name;

        @Override
        protected void onPreExecute()
        {
            contentValues = new ContentValues();
            contentValues.put(Constants.FOLDER_ICON_COL, R.drawable.ic_nav_black_folder);
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... p1)
        {
            if((db = Utils.getDb(context)) != null) {
                name = p1[0];
                if(name.equals(""))
                    name = "New folder";
                else
                    name = Utils.capitalizeFirstLetter(name);
                contentValues.put(Constants.FOLDER_NAME_COL, name);
                id = (int) db.insert(Constants.FOLDER_TABLE, null, contentValues);
                return true;

            } else
                return false;

        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            if(result){
                addFolderToNavView(id, name, R.drawable.ic_nav_black_folder);
                Utils.showToast(context, "Folder was added");
            }
            super.onPostExecute(result);
        }
    }

    private class RestoreOrRemoveAllNotesFromTrash extends AsyncTask<Integer,Void,Boolean>
    {
        int action;
        @Override
        protected Boolean doInBackground(Integer[] p1)
        {
            SQLiteDatabase db;
            action = p1[0];

            if((db = Utils.getDb(context)) != null) {
                if(action == R.id.action_delete_all){ //remove all
                    db.delete(Constants.NOTES_TABLE, Constants.DELETED_COL + " = ?", new String[]{Integer.toString(1)});
                    Log.e(TAG, "delete all");
                    return true;
                } else { //restore all
                    Log.e(TAG, "restore all");
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(Constants.DELETED_COL, 0);
                    db.update(Constants.NOTES_TABLE, contentValues, Constants.DELETED_COL + " = ?", new String[]{Integer.toString(1)});
                    return true;
                }
            } else
                return false;
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            super.onPostExecute(result);
            if(result){
                if(action == R.id.action_delete_all) {
                    Utils.showToast(context, "All notes were removed");
                    Utils.updateAllWidgets(context);
                } else {
                    Utils.showToast(context, "All notes were restored");
                }
                ((NoteListFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_LIST)).reloadList();
            }
        }
    }

    private class RemoveFolderAndAllNotes extends AsyncTask<Void,Void,Boolean>
    {
        @Override
        protected Boolean doInBackground(Void... voids)
        {
            SQLiteDatabase db;

            if((db = Utils.getDb(context)) != null) {

                //Delete folder
                Log.e(TAG, "deleted folders " + db.delete(Constants.FOLDER_TABLE, Constants.ID_COL + " = ?", new String[]{Integer.toString(folderId)}));
                //Delete all associated notes which are not in trash
                Log.e(TAG, "deleted  notes " + db.delete(Constants.NOTES_TABLE, Constants.FOLDER_ID_COL + " = ? AND " +
                        Constants.DELETED_COL + " = ?", new String[]{Integer.toString(folderId), Integer.toString(0)}));
                //Change associated folder to My Notes for all notes associated with deleted folder
                //which are currently in trash and can be restored
                ContentValues contentValues = new ContentValues();
                contentValues.put(Constants.FOLDER_ID_COL, myNotesNavId);
                Log.e(TAG, "updated trash notes " + db.update(Constants.NOTES_TABLE, contentValues, Constants.FOLDER_ID_COL + " = ? AND " +
                        Constants.DELETED_COL + " = ?", new String[]{Integer.toString(folderId), Integer.toString(1)}));
                return true;

            } else
                return false;
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            super.onPostExecute(result);
            if(result){
                Utils.showToast(context, "Folder and all associated notes were removed");
                Utils.updateAllWidgets(context);
                removeMenuItem(navigationView.getMenu(), folderId);
                openFolderWithNotes(myNotesNavId);
            }
        }
    }

    private class MoveNoteToOtherFolder extends AsyncTask<Integer,Void,Boolean>
    {
        private int newFolderId;
        @Override
        protected Boolean doInBackground(Integer... integers)
        {
            SQLiteDatabase db;
            newFolderId = integers[0];

            if((db = Utils.getDb(context)) != null) {

                //Update folder id for current note
                ContentValues contentValues = new ContentValues();
                contentValues.put(Constants.FOLDER_ID_COL, newFolderId);
                db.update(Constants.NOTES_TABLE, contentValues, Constants.ID_COL + " = ?",
                        new String[]{Long.toString(noteId)});
                return true;

            } else
                return false;
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            super.onPostExecute(result);
            if(result){
                Utils.showToast(context, "Note has been moved");

                //Change folder id for note which is currently visible
                Fragment fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_NOTE);
                if(fragment != null)
                    ((NoteFragment)fragment).setFolderId(newFolderId);

                //Update current folderId for folder fragment displayed onBackPressed
                folderId = newFolderId;
                folder = navigationView.getMenu().findItem(folderId).getTitle().toString();

                //Load NavView items again to refresh count number
                new LoadNavViewItems().execute();
            }
        }
    }
}
