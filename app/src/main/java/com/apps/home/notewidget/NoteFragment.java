package com.apps.home.notewidget;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.apps.home.notewidget.customviews.RobotoEditText;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.Utils;

import java.util.Calendar;
import java.util.regex.Pattern;

public class NoteFragment extends Fragment implements Utils.LoadListener{
    private static final String TAG = "NoteFragment";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_PARAM3 = "param3";
    private static final String ARG_PARAM4 = "param4";
    private RobotoEditText noteEditText;
    private boolean deleteNote = false;
    private boolean discardChanges = false;
    private long creationTimeMillis;
    private boolean isNewNote;
    private long noteId;
    private Context context;
    private int folderId;
    private String title = "Untitled";
    private TextWatcher textWatcher;
    private boolean skipTextCheck = false;
    private int editTextSelection;
    private String newLine;


    public NoteFragment() {
        // Required empty public constructor
    }

    public static NoteFragment newInstance(boolean isNewNote, long noteId, boolean moveToEnd, int folderId) {
        NoteFragment fragment = new NoteFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PARAM1, isNewNote);
        args.putLong(ARG_PARAM2, noteId);
        args.putBoolean(ARG_PARAM3, moveToEnd);
        args.putInt(ARG_PARAM4, folderId);
        fragment.setArguments(args);
        return fragment;
    }

    public static NoteFragment newInstance(boolean isNewNote, long noteId, boolean moveToEnd) {
        NoteFragment fragment = new NoteFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PARAM1, isNewNote);
        args.putLong(ARG_PARAM2, noteId);
        args.putBoolean(ARG_PARAM3, moveToEnd);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isNewNote = getArguments().getBoolean(ARG_PARAM1);
            noteId = getArguments().getLong(ARG_PARAM2);
            //moveToEnd = getArguments().getBoolean(ARG_PARAM3);
            folderId = getArguments().getInt(ARG_PARAM4);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();
        ((AppCompatActivity)context).invalidateOptionsMenu();
        setTextWatcher();
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_note, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(!context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).
                getBoolean(Constants.SKIP_MULTILEVEL_NOTE_MANUAL_DIALOG_KEY, false))
            Utils.getMultilevelNoteManualDialog(context).show();

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        noteEditText = (RobotoEditText) view.findViewById(R.id.noteEditText);
        noteEditText.addTextChangedListener(textWatcher);
        noteEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).getInt(Constants.NOTE_TEXT_SIZE_KEY, 14));
        newLine = System.getProperty("line.separator");
        Log.e(TAG, "skip start " + skipTextCheck);
        if(!isNewNote) {
            Log.e(TAG, "skip old " +skipTextCheck);
            reloadNote();
        } else {
            Log.e(TAG, "skip new " +skipTextCheck);
            setTitleAndSubtitle("Untitled", 0);
            //showSoftKeyboard(0);
        }
    }

    public void updateNoteTextSize(){
        noteEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).getInt(Constants.NOTE_TEXT_SIZE_KEY, 14));
    }

    public void reloadNote(){
        Utils.loadNote(context, noteId, this);
    }

    private void setTitleAndSubtitle(String title, long millis){
        ActionBar actionBar = ((AppCompatActivity) context).getSupportActionBar();
        if(actionBar !=null){
            actionBar.setTitle(title);
            Calendar calendar = Calendar.getInstance();
            creationTimeMillis = millis == 0? calendar.getTimeInMillis() : millis;
            calendar.setTimeInMillis(creationTimeMillis);
            Log.e(TAG, "millis " + creationTimeMillis);
            actionBar.setSubtitle(String.format("%1$tb %1$te, %1$tY %1$tT", calendar));
        }
    }

    private void setTextWatcher(){
        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //Log.e(TAG, "before "+s.toString() + " start "+start+" count "+count+" aft ");
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.e(TAG, "after " + s.toString() + " start " + start + " count " + count + " skip "+skipTextCheck);

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
    public void onStop() {
        super.onStop();
        if (!deleteNote && !discardChanges) {
            if(isNewNote)//TODO ENCRYPTION
                Utils.saveNewNote(context, noteId, title, noteEditText.getText().toString(), folderId,
                        creationTimeMillis, null);
            else
                Utils.updateNote(context, noteId, title, noteEditText.getText().toString(), null);
        } else if (deleteNote && !isNewNote) {
            Utils.moveToTrash(context, ((MainActivity)context).getNavigationViewMenu(),
                    noteId, title, noteEditText.getText().toString().replace(newLine, "<br/>"),
                    folderId);
        } else {
            Utils.showToast(context, context.getString(R.string.closed_without_saving));
        }
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    public void deleteNote() {
        this.deleteNote = true;
    }

    public void discardChanges(){ this.discardChanges = true; }

    public void titleChanged(String title) {
        this.title = title;
    }

    public String getNoteText(){
        if(noteEditText.getText().length() == 0) {
            Utils.showToast(context, "Note is empty or was not loaded yet");
        }
        return noteEditText.getText().toString();
    }

    @Override
    public void onLoad(String[] note) {
        if(note != null){
            noteEditText.setText(Html.fromHtml(note[1]));
            title = note[0];
            setTitleAndSubtitle(title, Long.parseLong(note[2]));
        } else {
            ((AppCompatActivity)context).onBackPressed();
        }
    }

//    private class PutNoteInTable extends AsyncTask<Void, Void, Boolean>
//    {   private ContentValues contentValues;
//
//        @Override
//        protected void onPreExecute()
//        {
//            contentValues = new ContentValues();
//            contentValues.put(Constants.NOTE_TITLE_COL, title);
//            contentValues.put(Constants.NOTE_TEXT_COL, noteEditText.getText().toString().replace(newLine, "<br/>"));
//            super.onPreExecute();
//        }
//
//        @Override
//        protected Boolean doInBackground(Void... p1)
//        {
//            SQLiteDatabase db;
//            if((db = Utils.getDb(context)) != null) {
//                if (isNewNote) {
//                    contentValues.put(Constants.MILLIS_COL, creationTimeMillis);
//                    contentValues.put(Constants.FOLDER_ID_COL, folderId);
//                    contentValues.put(Constants.DELETED_COL, 0);
//                    noteId = db.insert(Constants.NOTES_TABLE, null, contentValues);
//                    Log.e(TAG, "insert " + contentValues.toString());
//                } else {
//                    db.update(Constants.NOTES_TABLE, contentValues, Constants.ID_COL + " = ?",
//                            new String[]{Long.toString(noteId)});
//                    Log.e(TAG, "update " + contentValues.toString());
//                }
//                return true;
//            } else
//                return false;
//
//        }
//
//        @Override
//        protected void onPostExecute(Boolean result)
//        {
//            if(result){
//                if(isNewNote){
//                    Utils.incrementFolderCount(((MainActivity) context).getNavigationViewMenu(), folderId, 1);
//                }
//                else
//                    Utils.updateConnectedWidgets(context, noteId);
//            }
//            super.onPostExecute(result);
//        }
//    }
}

