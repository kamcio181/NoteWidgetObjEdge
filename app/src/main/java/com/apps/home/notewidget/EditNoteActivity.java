package com.apps.home.notewidget;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.apps.home.notewidget.NoteFragment;
import com.apps.home.notewidget.R;
import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.DatabaseHelper;
import com.apps.home.notewidget.utils.TitleChangeListener;
import com.apps.home.notewidget.utils.Utils;


public class EditNoteActivity extends AppCompatActivity{
    private static final String TAG = "EditNoteActivity";
    private Context context;
    private long noteId = -1;
    private FragmentManager fragmentManager;
    private Toolbar toolbar;
    private boolean skipSaving = false;

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
            DatabaseHelper helper = new DatabaseHelper(context);
            helper.getNote(false, noteId, new DatabaseHelper.OnNoteLoadListener() {
                @Override
                public void onNoteLoaded(Note note) {
                    if(note != null)
                        fragmentManager.beginTransaction().replace(R.id.container,
                            NoteFragment.newInstance(false, note), Constants.FRAGMENT_NOTE).commit();
                }
            });

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

        Fragment fragment = fragmentManager.findFragmentById(R.id.container);
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
        if(!skipSaving)
            getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).edit().
                    putBoolean(Constants.NOTE_UPDATED_FROM_WIDGET, true).apply();
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
                skipSaving = true;
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
