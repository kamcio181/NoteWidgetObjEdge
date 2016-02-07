package com.apps.home.notewidget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
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

        getSupportFragmentManager().beginTransaction().
                add(R.id.container, new NoteListFragment(), Constants.FRAGMENT_LIST).commit();

        new LoadDatabase().execute();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        Fragment fragment;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if ((fragment = getSupportFragmentManager().findFragmentByTag(Constants.FRAGMENT_NOTE)) != null) {
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
            new LoadDatabase().execute();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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

            Log.e(TAG, "update " + contentValues.toString());
        }
        Utils.showToast(this, "Saved");
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
                        getSupportActionBar().setTitle(titleEditText.getText().toString());
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
        getSupportFragmentManager().beginTransaction().
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

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.fab:
                getSupportFragmentManager().beginTransaction().
                        replace(R.id.container, NoteFragment.newInstance(true), Constants.FRAGMENT_NOTE).commit();
                getSupportActionBar().setTitle("Untitled");
                Calendar calendar = Calendar.getInstance();
                creationTimeMillis = calendar.getTimeInMillis();
                getSupportActionBar().setSubtitle(String.format("%1$tb %1$te, %1$tY %1$tT", calendar));
                noteId = -1;
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

                cursor = db.query(Constants.NOTES_TABLE, new String[]{Constants.ID_COL, Constants.MILLIS_COL,
                                Constants.NOTE_TITLE_COL, Constants.NOTE_TEXT_COL},
                        null, null, null, null, null);

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

                getSupportFragmentManager().beginTransaction()
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
