package com.apps.home.notewidget;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.utils.Utils;

import java.util.Calendar;

public class TrashNoteFragment extends Fragment implements Utils.LoadListener{
    private static final String TAG = "TrashNoteFragment";
    private static final String ARG_PARAM1 = "param1";
    private RobotoTextView noteTextView;
    private long creationTimeMillis;
    private SQLiteDatabase db;
    private long noteId;
    private Context context;


    public TrashNoteFragment() {
        // Required empty public constructor
    }

    public static TrashNoteFragment newInstance(long noteId) {
        TrashNoteFragment fragment = new TrashNoteFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PARAM1, noteId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        noteTextView = (RobotoTextView) view.findViewById(R.id.noteEditText);

        reloadNote();
    }

    public void reloadNote(){
        Utils.loadNote(context, noteId, this);
    }

    @Override
    public void onLoad(String[] note) {
        if(note != null){
            noteTextView.setText(Html.fromHtml(note[1]));
            ((AppCompatActivity)context).getSupportActionBar().setTitle(note[0]);
            Calendar calendar = Calendar.getInstance();
            creationTimeMillis = Long.parseLong(note[2]);
            calendar.setTimeInMillis(creationTimeMillis);
            ((AppCompatActivity)context).getSupportActionBar().setSubtitle(String.format("%1$tb %1$te, %1$tY %1$tT", calendar));
        } else {
            ((AppCompatActivity)context).onBackPressed();
        }
    }
}

