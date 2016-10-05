package com.apps.home.notewidget;


import android.os.Bundle;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.utils.BasicTrashFragment;
import com.apps.home.notewidget.utils.Utils;

public class TrashNoteFragment extends BasicTrashFragment {
    private static final String TAG = "TrashNoteFragment";
    private static final String ARG_PARAM1 = "param1";
    private AppCompatTextView noteTextView;


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
        if (getArguments() != null) {
            note = new Note(getArguments().getLong(ARG_PARAM1));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_trash_note, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        noteTextView = (AppCompatTextView) view.findViewById(R.id.noteEditText);

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void setNoteViews() {
        super.setNoteViews();
        noteTextView.setText(Utils.getHtmlFormattedText(note.getNote()));
    }

    //    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        Log.v(TAG, "onCreateOptionsMenu");
//        super.onCreateOptionsMenu(menu, inflater);
//
//        getActivity().getMenuInflater().inflate(R.menu.menu_note_trash, menu);
//    }

}

