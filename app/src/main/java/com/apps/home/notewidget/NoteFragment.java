package com.apps.home.notewidget;


import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Calendar;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NoteFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NoteFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private Cursor cursor;
    private RobotoEditText noteEditText;
    private boolean deleteNote = false;

    private boolean isNewNote;


    public NoteFragment() {
        // Required empty public constructor
    }

    public static NoteFragment newInstance(boolean isNewNote) {
        NoteFragment fragment = new NoteFragment(); //TODO handle delete/close without saving
        Bundle args = new Bundle();
        args.putBoolean(ARG_PARAM1, isNewNote);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isNewNote = getArguments().getBoolean(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().invalidateOptionsMenu();
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_note, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity)getActivity()).getFab().setImageDrawable(ContextCompat.getDrawable(getActivity(), R.mipmap.ic_create_white_24dp));
        noteEditText = (RobotoEditText) view.findViewById(R.id.noteEditText);
        if(!isNewNote){
            cursor = ((MainActivity)getActivity()).getCursor();
            noteEditText.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TEXT_COL))));
        }
        ((MainActivity)getActivity()).setOnTitleClickListener(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(!deleteNote){
            ((MainActivity)getActivity()).putInNoteTable(noteEditText.getText().toString().replace("\n", "<br/>"));
        } else if(!isNewNote){
            ((MainActivity)getActivity()).removeFromNoteTable();
            //TODO remove from table -> move to deleted table
        } else {
            Utils.showToast(getActivity(), "Closed without saving");
        }
    }

    public void deleteNote() {
        this.deleteNote = true;
        getActivity().onBackPressed();
    }
}
