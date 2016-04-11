package com.apps.home.notewidget.widget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.apps.home.notewidget.NoteFragment;
import com.apps.home.notewidget.R;
import com.apps.home.notewidget.customviews.RobotoEditText;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.Utils;

import java.lang.reflect.Field;

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
                    NoteFragment.newInstance(false, noteId, true), Constants.FRAGMENT_NOTE).commit();
        }
    }

    public void setOnTitleClickListener(){
        try {
            Field titleField = Toolbar.class.getDeclaredField("mTitleTextView");
            //Field subtitleField = Toolbar.class.getDeclaredField("mSubtitleTextView");
            titleField.setAccessible(true);
            //subtitleField.setAccessible(true);
            TextView barTitleView = (TextView) titleField.get(toolbar);
            //TextView barSubtitleView = (TextView) subtitleField.get(toolbar);
            barTitleView.setOnClickListener(noteTitleChangeOrFolderNameListener());
            //barSubtitleView.setOnClickListener(noteChangeListener());

        } catch (NoSuchFieldException e){
            Log.e(TAG, "" + e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, " " + e);
        }
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

        AlertDialog dialog = builder.setTitle("Set title").setView(layout)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setNoteTitle(titleEditText.getText().toString());
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

    private void setNoteTitle(String title){
        title = setTitle(title);

        if(fragmentManager.findFragmentByTag(Constants.FRAGMENT_NOTE) != null) // Note fragment is displayed
            ((NoteFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_NOTE)).titleChanged(title);
    }

    private String setTitle(String title){
        if(title.equals(""))
            title = "Untitled";
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
        Utils.closeDb();
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
