package com.apps.home.notewidget;


import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.utils.AdvancedNoteFragment;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.NoteUpdateListener;
import com.apps.home.notewidget.utils.ParametersUpdateListener;
import com.apps.home.notewidget.utils.Utils;

import java.util.regex.Pattern;

public class NoteFragment extends AdvancedNoteFragment implements  NoteUpdateListener,
         ParametersUpdateListener{
    private static final String TAG = "NoteFragment";
    private AppCompatEditText noteEditText;
    private TextWatcher textWatcher;
    private boolean skipTextCheck = false;
    private int editTextSelection;
    private String newLine;


    public NoteFragment() {
        // Required empty public constructor
    }

    public static NoteFragment newInstance(Note note) {
        NoteFragment fragment = new NoteFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PARAM1, true);
        args.putSerializable(ARG_PARAM2, note);
        fragment.setArguments(args);
        return fragment;
    }

    public static NoteFragment newInstance(long noteId) {
        NoteFragment fragment = new NoteFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PARAM1, false);
        args.putLong(ARG_PARAM3, noteId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setTextWatcher();
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_note, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if(!context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).
                getBoolean(Constants.SKIP_MULTILEVEL_NOTE_MANUAL_DIALOG_KEY, false))
            Utils.getMultilevelNoteManualDialog(context).show();

        noteEditText = (AppCompatEditText) view.findViewById(R.id.noteEditText);
        noteEditText.addTextChangedListener(textWatcher);
        noteEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).getInt(Constants.NOTE_TEXT_SIZE_KEY, 14));
        newLine = System.getProperty("line.separator");
        Log.e(TAG, "skip start " + skipTextCheck);

        super.onViewCreated(view, savedInstanceState);
    }

//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        Log.v(TAG, "onCreateOptionsMenu");
//        super.onCreateOptionsMenu(menu, inflater);
//
//        getActivity().getMenuInflater().inflate(R.menu.menu_note, menu);
//    }

    @Override
    public void setNoteViews() {
        super.setNoteViews();
        noteEditText.setText(Html.fromHtml(note.getNote()));
    }

    private void setTextWatcher(){
        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //Log.e(TAG, "before "+s.toString() + " start "+start+" count "+count+" aft ");
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if(!skipTextCheck && (start+count) <= s.length()){
                    boolean isFirstLine = start == 0;
                    String newText = isFirstLine ? s.subSequence(0, count).toString() : s.subSequence(start - 1, start + count).toString();
                    if((!isFirstLine && (newText.startsWith(newLine + "-") || newText.startsWith(newLine + "+")
                            || newText.startsWith(newLine + "*"))) || (isFirstLine && (newText.startsWith("-")
                            || newText.startsWith("+") || newText.startsWith("*")))) {
                        skipTextCheck = true;
                        editTextSelection = newText.contains("-") ? start+count + 2 : newText.contains("+")?
                                start+count + 3 : start+count + 4;
                        if(!isFirstLine)
                            newText = newText.substring(1);
                        newText = newText.startsWith("-") ? newText.replaceFirst("-", "\u0009- ") :  newText.startsWith("+")?
                                newText.replaceFirst(Pattern.quote("+"), "\u0009\u0009+ ") : newText.replaceFirst(Pattern.quote("*"), "\u0009\u0009\u0009* ");
                        String fullText = s.subSequence(0,start) + newText +s.subSequence(start+count, s.length());
                        noteEditText.setText(fullText);

                    }
                } else if (skipTextCheck) {
                    skipTextCheck = false;
                    noteEditText.setSelection(editTextSelection);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
    }

    @Override
    public void onParametersUpdated() {
        noteEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).getInt(Constants.NOTE_TEXT_SIZE_KEY, 14));
    }

    public void deleteNote() {
        note.setNote(noteEditText.getText().toString());
        super.deleteNote();
    }

    @Override
    public void saveNote(final boolean quitAfterSaving) {
        note.setNote(noteEditText.getText().toString());
        super.saveNote(quitAfterSaving);
    }

    @Override
    public String getContent() {
        return noteEditText.getText().toString();
    }
}

