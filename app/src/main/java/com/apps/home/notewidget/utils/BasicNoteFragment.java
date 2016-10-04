package com.apps.home.notewidget.utils;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.objects.Note;

import java.util.Calendar;

public abstract class BasicNoteFragment extends Fragment {
    private static final String TAG = "BasicNoteFragment";
    public Context context;
    public DatabaseHelper helper;
    public Note note;
    public boolean isNewNote = false;
    public Menu menu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        context = getActivity();
        helper = new DatabaseHelper(context);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.e(TAG, "isNewNote " + isNewNote);
        if(isNewNote) {
            note.setTitle(getString(R.string.untitled));
            note.setCreatedAt(Calendar.getInstance().getTimeInMillis());
            note.setDeletedState(Constants.FALSE);
            setTitleAndSubtitle();
        } else {
            loadNote();
        }
    }

    public void loadNote(){
        Log.e(TAG, "load note " + note.getId());
        helper.getNote(true, note.getId(), new DatabaseHelper.OnNoteLoadListener() {
            @Override
            public void onNoteLoaded(Note note) {
                BasicNoteFragment.this.note = note;
                setNoteViews();
                setTitleAndSubtitle();
            }
        });
    }

    public void setNoteViews(){}

    public void setTitleAndSubtitle(){
        ActionBar actionBar = ((AppCompatActivity) context).getSupportActionBar();
        if(actionBar != null){
            actionBar.setTitle(note.getTitle());
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(note.getCreatedAt());
            Log.e(TAG, "millis " + note.getCreatedAt());
            actionBar.setSubtitle(String.format("%1$tb %1$te, %1$tY %1$tT", calendar));
//            Utils.setSubtitleMarquee(((MainActivity)context).getToolbar());
        }
    }
}
