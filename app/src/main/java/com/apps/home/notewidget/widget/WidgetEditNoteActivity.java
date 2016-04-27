package com.apps.home.notewidget.widget;

import android.content.Context;
import android.os.Bundle;
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


public class WidgetEditNoteActivity extends AppCompatActivity{
    private static final String TAG = "WidgetEditNoteActivity";
    private Context context;
    private long noteId = -1;
    private FragmentManager fragmentManager;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_edit_note);
        context = this;
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fragmentManager = getSupportFragmentManager();

        setOnTitleClickListener();

        Utils.hideShadowSinceLollipop(this);

        if(getIntent().getExtras()!=null){
            noteId = getIntent().getLongExtra(Constants.ID_COL, -1);
        }

        if(noteId>0) {
            fragmentManager.beginTransaction().replace(R.id.container,
                    NoteFragment.newInstance(false, noteId), Constants.FRAGMENT_NOTE).commit();
        }
    }

    public void setOnTitleClickListener(){
        toolbar.setOnClickListener(noteTitleChangeOrFolderNameListener());
    }

    private View.OnClickListener noteTitleChangeOrFolderNameListener(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.getNameDialog(context, getSupportActionBar().getTitle().toString(), getString(R.string.set_note_title),
                        new Utils.OnNameSet() {
                            @Override
                            public void onNameSet(String name) {
                                setNoteTitle(name);
                            }
                        }).show();
            }
        };
    }

    private void setNoteTitle(String title){
        title = setTitle(title);

        if(fragmentManager.findFragmentByTag(Constants.FRAGMENT_NOTE) != null) // Note fragment is displayed
            ((NoteFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_NOTE)).titleChanged(title);
    }

    private String setTitle(String title){
        if(title.equals(""))
            title = getString(R.string.untitled);
        else
            title = Utils.capitalizeFirstLetter(title);
        getSupportActionBar().setTitle(title);

        return title;
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).edit().putBoolean(Constants.NOTE_UPDATED_FROM_WIDGET, true).apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_widget_note, menu);
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
            case R.id.action_discard_changes:
                ((NoteFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_NOTE)).discardChanges();
                finish();
                break;
            case R.id.action_save:
                finish();
                break;
            case R.id.action_share:
                Utils.sendShareIntent(this, ((NoteFragment) fragmentManager.
                                findFragmentByTag(Constants.FRAGMENT_NOTE)).getNoteText(),
                        getSupportActionBar().getTitle().toString());
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
