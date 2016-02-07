package com.apps.home.notewidget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, NoteListFragment.OnItemClickListener, View.OnClickListener {
    private static final String TAG = "MainActivity";
    private SQLiteDatabase db;
    private Cursor cursor;
    private long creationTimeMillis;
    private long noteId;
    private Toolbar toolbar;
    private FloatingActionButton fab;
    private FragmentManager fragmentManager;
    private SharedPreferences preferences;
    private boolean sortByDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fragmentManager = getSupportFragmentManager();
        preferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        sortByDate = preferences.getBoolean(Constants.SORT_BY_DATE_KEY, false);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);
        /*fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        fragmentManager.beginTransaction().
                add(R.id.container, new NoteListFragment(), Constants.FRAGMENT_LIST).commit();

        new LoadDatabase().execute();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        Fragment fragment;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if ((fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_NOTE)) != null) {
            fragmentManager.beginTransaction().remove(fragment).commit();
            new LoadDatabase().execute();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        switch (fragmentManager.findFragmentById(R.id.container).getTag()){
            case Constants.FRAGMENT_LIST:
                getMenuInflater().inflate(R.menu.menu_list, menu);
                break;
            case Constants.FRAGMENT_NOTE:
                getMenuInflater().inflate(R.menu.menu_note, menu);
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

        //noinspection SimplifiableIfStatement
        switch (id){
            case R.id.action_sort_by_date:
                setOrderType(true);
                break;
            case R.id.action_sort_by_title:
                setOrderType(false);
                break;
            case R.id.action_delete:
                ((NoteFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_NOTE)).deleteNote();
                onBackPressed();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setOrderType(Boolean orderByDate){
        sortByDate = orderByDate;
        new LoadDatabase().execute();
        preferences.edit().putBoolean(Constants.SORT_BY_DATE_KEY, orderByDate).apply();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cursor.close();
        db.close();
    }

    public void putInNoteTable(String note){
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.NOTE_TEXT_COL, note);
        contentValues.put(Constants.NOTE_TITLE_COL, getSupportActionBar().getTitle().toString());
        Log.e(TAG, "noteId " + noteId);

        if(noteId<0) {
            contentValues.put(Constants.MILLIS_COL, creationTimeMillis);
            noteId = db.insert(Constants.NOTES_TABLE, null, contentValues);
            Log.e(TAG, "insert " + contentValues.toString());
        }else{
            db.update(Constants.NOTES_TABLE, contentValues, Constants.ID_COL + " = ?",
                    new String[]{Long.toString(noteId)});
            updateConnectedWidgets();
            Log.e(TAG, "update " + contentValues.toString());
        }
        Utils.showToast(this, "Saved");
    }

    public void removeFromNoteTable(){
        db.delete(Constants.NOTES_TABLE, Constants.ID_COL + " = ?", new String[]{Long.toString(noteId)});
        updateConnectedWidgets();
        Utils.showToast(this, "Note removed");
    }

    private void updateConnectedWidgets(){
        WidgetProvider widgetProvider = new WidgetProvider();
        Cursor widgetCursor = db.query(Constants.WIDGETS_TABLE, new String[]{Constants.WIDGET_ID_COL},
                Constants.CONNECTED_NOTE_ID_COL + " = ?", new String[]{Long.toString(noteId)}, null,null, null);
        widgetCursor.moveToFirst();
        int[] widgetIds = new int[widgetCursor.getCount()];
        for (int i=0; i<widgetCursor.getCount(); i++){
            widgetIds[i] = widgetCursor.getInt(0);
            widgetCursor.moveToNext();
        }
        widgetProvider.onUpdate(this, AppWidgetManager.getInstance(this), widgetIds);
    }

    public void setOnTitleClickListener(boolean enable){
        try {
            Field titleField = Toolbar.class.getDeclaredField("mTitleTextView");
            titleField.setAccessible(true);
            TextView barTitleView = (TextView) titleField.get(toolbar);
            if(enable){
                barTitleView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        setNoteTitleDialog().show();
                    }
                });
            } else {
                barTitleView.setOnClickListener(null);
            }

        } catch (NoSuchFieldException e){
            Log.e(TAG, "" + e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "" + e);
        }
    }

    private Dialog setNoteTitleDialog(){
        final RobotoEditText titleEditText = new RobotoEditText(this);
        titleEditText.setSingleLine(true);
        titleEditText.setText(getSupportActionBar().getTitle().toString());
        titleEditText.setSelection(titleEditText.length());
        return new AlertDialog.Builder(this).setTitle("Set title").setView(titleEditText)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getSupportActionBar().setTitle(titleEditText.getText().toString());//TODO cap first letter to fix sorting
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Utils.showToast(MainActivity.this, "Canceled");
                    }
                }).create();
    }

    @Override
    public void onItemClicked(int position) {

        cursor.moveToPosition(position);
        fragmentManager.beginTransaction().
                replace(R.id.container, NoteFragment.newInstance(false), Constants.FRAGMENT_NOTE).commit();
        getSupportActionBar().setTitle(cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TITLE_COL)));
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.MILLIS_COL)));
        getSupportActionBar().setSubtitle(String.format("%1$tb %1$te, %1$tY %1$tT", calendar));
        noteId = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.ID_COL));
    }

    public Cursor getCursor() {
        return cursor;
    }

    public FloatingActionButton getFab() {
        return fab;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.fab:
                switch (fragmentManager.findFragmentById(R.id.container).getTag()){
                    case Constants.FRAGMENT_LIST:
                        fragmentManager.beginTransaction().
                                replace(R.id.container, NoteFragment.newInstance(true), Constants.FRAGMENT_NOTE).commit();
                        getSupportActionBar().setTitle("Untitled");
                        Calendar calendar = Calendar.getInstance();
                        creationTimeMillis = calendar.getTimeInMillis();
                        Log.e(TAG, "millis "+ creationTimeMillis);
                        getSupportActionBar().setSubtitle(String.format("%1$tb %1$te, %1$tY %1$tT", calendar));
                        noteId = -1;
                        break;
                    case Constants.FRAGMENT_NOTE:
                        onBackPressed();
                        break;
                }
                break;
        }
    }

    private class LoadDatabase extends AsyncTask<Void, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if(db == null){
                    Log.e(TAG, "Database is not exist");
                    SQLiteOpenHelper helper = new DatabaseHelper(MainActivity.this);
                    db = helper.getWritableDatabase();
                }
                String orderColumn = sortByDate? Constants.MILLIS_COL : Constants.NOTE_TITLE_COL;
                Log.e(TAG, orderColumn);
                cursor = db.query(Constants.NOTES_TABLE, new String[]{Constants.ID_COL, Constants.MILLIS_COL,
                                Constants.NOTE_TITLE_COL, Constants.NOTE_TEXT_COL},
                        null, null, null, null, orderColumn + " ASC");

                return true;

            } catch(SQLiteException e) {
                return false;

            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if(aBoolean){
                Log.e(TAG, "database Loaded");
                cursor.moveToFirst();

                fragmentManager.beginTransaction()
                        .replace(R.id.container, new NoteListFragment(), Constants.FRAGMENT_LIST).commit();
            }
            else {
                Toast.makeText(MainActivity.this, "Database unavailable", Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }
}
