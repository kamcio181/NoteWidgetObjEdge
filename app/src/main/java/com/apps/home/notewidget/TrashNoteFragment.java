package com.apps.home.notewidget;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.utils.DatabaseHelper;
import com.apps.home.notewidget.utils.Utils;

import java.util.Calendar;

public class TrashNoteFragment extends Fragment {
    private static final String TAG = "TrashNoteFragment";
    private static final String ARG_PARAM1 = "param1";
    private AppCompatTextView noteTextView;
    private long noteId;
    private Context context;


    public TrashNoteFragment() {
        // Required empty public constructor
    }

    public static TrashNoteFragment newInstance(long noteId) {
        TrashNoteFragment fragment = new TrashNoteFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM1, noteId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            noteId = getArguments().getLong(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();
        ((AppCompatActivity)context).invalidateOptionsMenu();
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_trash_note, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        noteTextView = (AppCompatTextView) view.findViewById(R.id.noteEditText);

        DatabaseHelper helper = new DatabaseHelper(context);
        helper.getNote(true, noteId, new DatabaseHelper.OnNoteLoadListener() {
            @Override
            public void onNoteLoaded(Note note) {
                noteTextView.setText(Html.fromHtml(note.getNote()));
                ActionBar actionBar = ((AppCompatActivity) context).getSupportActionBar();
                if(actionBar != null){
                    actionBar.setTitle(note.getTitle());
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(note.getCreatedAt());
                    actionBar.setSubtitle(String.format("%1$tb %1$te, %1$tY %1$tT", calendar));
                }
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.v(TAG, "onCreateOptionsMenu");
        super.onCreateOptionsMenu(menu, inflater);

        getActivity().getMenuInflater().inflate(R.menu.menu_note_trash, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(TAG, "onOptionsItemSelected");
        return super.onOptionsItemSelected(item);
    }
}

