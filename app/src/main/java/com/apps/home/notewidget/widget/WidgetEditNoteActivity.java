package com.apps.home.notewidget.widget;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.apps.home.notewidget.NoteFragment;
import com.apps.home.notewidget.R;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.Utils;

public class WidgetEditNoteActivity extends AppCompatActivity implements View.OnClickListener,
    NoteFragment.DatabaseUpdated{ //TODO if title was edited widget does not update it
    //private static final String TAG = "WidgetEditNoteActivity";
    private long noteId = -1;
    private FloatingActionButton fab;
    private FragmentManager fragmentManager;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_edit_note);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fragmentManager = getSupportFragmentManager();


        if(getIntent().getExtras()!=null){
            noteId = getIntent().getLongExtra(Constants.ID_COL, -1);
        }

        fab = (FloatingActionButton) findViewById(R.id.fab);
        if(fab!=null)
            fab.setOnClickListener(this);

        if(noteId>0) {
            fragmentManager.beginTransaction().replace(R.id.container,
                    NoteFragment.newInstance(false, noteId, true), Constants.FRAGMENT_NOTE).commit();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Utils.closeDb();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note, menu);
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
            case R.id.action_delete:
                ((NoteFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_NOTE)).deleteNote();
                finish();
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.fab:
                finish();
                break;
        }
    }

    @Override
    public void databaseUpdated() {
    }
}
