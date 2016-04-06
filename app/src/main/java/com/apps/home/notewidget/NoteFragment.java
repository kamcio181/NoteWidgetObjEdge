package com.apps.home.notewidget;


import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.apps.home.notewidget.customviews.RobotoEditText;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.Utils;
import com.apps.home.notewidget.widget.WidgetProvider;

import java.util.Calendar;

public class NoteFragment extends Fragment {
    private static final String TAG = "NoteFragment";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_PARAM3 = "param3";
    private static final String ARG_PARAM4 = "param4";
    private DatabaseUpdated mListener;
    private Cursor cursor;
    private RobotoEditText noteEditText;
    private boolean deleteNote = false;
    private boolean titleChanged = false;
    private long creationTimeMillis;
    private boolean isNewNote;
    private boolean moveToEnd;
    private SQLiteDatabase db;
    private long noteId;
    private AppWidgetManager mgr;
    private boolean reloadNote = false;
    private Context context;
    private int folderId;
    private String title = "Untitled";
    private TextWatcher textWatcher;
    private boolean skipTextCheck = true;
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
            moveToEnd = getArguments().getBoolean(ARG_PARAM3);
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

        mgr = AppWidgetManager.getInstance(context);
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        noteEditText = (RobotoEditText) view.findViewById(R.id.noteEditText);
        noteEditText.addTextChangedListener(textWatcher);
        noteEditText.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 5, metrics )); //TODO change size from settings inc to 20
        newLine = System.getProperty("line.separator");

        if(!isNewNote)
            new LoadNote().execute();
        else {
            ((AppCompatActivity) context).getSupportActionBar().setTitle("Untitled");
            Calendar calendar = Calendar.getInstance();
            creationTimeMillis = calendar.getTimeInMillis();
            Log.e(TAG, "millis " + creationTimeMillis);
            ((AppCompatActivity) context).getSupportActionBar().setSubtitle(String.format("%1$tb %1$te, %1$tY %1$tT", calendar));
            showSoftKeyboard(0);
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
                    String newText = start>0? s.subSequence(start-1, (start + count)).toString() : s.subSequence(start, (start + count)).toString();
                    Log.e(TAG, "newText "+newText);
                    if((start > 0 && (newText.contains(newLine+"-") || newText.contains(newLine+"+")))
                            || start == 0 && (newText.contains("-") || newText.contains("+"))) {
                        Log.e(TAG, "contains ");
                        skipTextCheck = true;
                        editTextSelection = newText.contains("-") ? start+count + 2 : start+count + 3;
                        if(start>0)
                            newText = newText.substring(1);
                        newText = newText.contains("-") ? newText.replace("-", "\u0009- ") :
                                newText.replace("+", "\u0009\u0009+ ");
                        String fullText = s.subSequence(0, start) + newText + s.subSequence(start + count, s.length());
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
        if(!reloadNote) {
            if (!deleteNote) {
                Utils.showToast(context, "Saving");
                new PutNoteInTable().execute();
            } else if (!isNewNote) {
                Utils.showToast(context, "Note moved to trash");
                new RemoveNoteFromTable().execute();
            } else {
                Utils.showToast(context, "Closed without saving");
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DatabaseUpdated) {
            mListener = (DatabaseUpdated) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnItemClickListener");
        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        if(cursor!=null && !cursor.isClosed())
            cursor.close();
    }

    public interface DatabaseUpdated {
        void databaseUpdated();
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    private void updateConnectedWidgets(){
        new UpdateConnectedWidgets().execute();
    }

    public void deleteNote() {
        this.deleteNote = true;
    }

    public void titleChanged(String title) {
        this.titleChanged = true;
        this.title = title;
    }

    public String getNoteText(){
        if(noteEditText.getText().length() == 0) {
            Utils.showToast(context, "Note is empty or was not loaded yet");
        }
        return noteEditText.getText().toString();
    }

    public void reloadNote(){
        this.reloadNote = true;
    }

    private void showSoftKeyboard(int index){
        noteEditText.requestFocus();
        noteEditText.setSelection(index);
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    private class LoadNote extends AsyncTask<Void, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {

            Log.e(TAG, "LOADING NOTE");
            if((db = Utils.getDb(context)) != null) {
                cursor = db.query(Constants.NOTES_TABLE, new String[]{Constants.MILLIS_COL,
                                Constants.NOTE_TITLE_COL, Constants.NOTE_TEXT_COL},
                        Constants.ID_COL + " = ?", new String[]{Long.toString(noteId)}, null, null, null);
                Log.e(TAG, "cursor count "+cursor.getCount());
                return (cursor.getCount()>0);
            } else {
                Log.e(TAG, "db error");
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if(aBoolean){
                cursor.moveToFirst();

                noteEditText.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TEXT_COL))));
                title = cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TITLE_COL));
                ((AppCompatActivity)context).getSupportActionBar().setTitle(title);

                Calendar calendar = Calendar.getInstance();
                creationTimeMillis = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.MILLIS_COL));
                calendar.setTimeInMillis(creationTimeMillis);
                ((AppCompatActivity)context).getSupportActionBar().setSubtitle(String.format("%1$tb %1$te, %1$tY %1$tT", calendar));

                if(moveToEnd) {
                    int index = noteEditText.getText().length() < 0 ? 0 : noteEditText.getText().length();
                    showSoftKeyboard(index);
                }
            } else {
                ((AppCompatActivity)context).onBackPressed();
            }
        }
    }

    private class PutNoteInTable extends AsyncTask<Void, Void, Boolean>
    {   private ContentValues contentValues;

        @Override
        protected void onPreExecute()
        {
            contentValues = new ContentValues();
            contentValues.put(Constants.NOTE_TITLE_COL, title);
            contentValues.put(Constants.NOTE_TEXT_COL, noteEditText.getText().toString().replace(newLine, "<br/>"));
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... p1)
        {
            if((db = Utils.getDb(context)) != null) {
                if (noteId < 0) {
                    contentValues.put(Constants.MILLIS_COL, creationTimeMillis);
                    contentValues.put(Constants.FOLDER_ID_COL, folderId);
                    contentValues.put(Constants.DELETED_COL, 0);
                    noteId = db.insert(Constants.NOTES_TABLE, null, contentValues);
                    Log.e(TAG, "insert " + contentValues.toString());
                    return false;
                } else {
                    db.update(Constants.NOTES_TABLE, contentValues, Constants.ID_COL + " = ?",
                            new String[]{Long.toString(noteId)});
                    Log.e(TAG, "update " + contentValues.toString());
                    return true;
                }
            } else
                return false;

        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            if(result)
                updateConnectedWidgets();

            if(mListener != null)
                mListener.databaseUpdated();

            super.onPostExecute(result);
        }
    }

    private class RemoveNoteFromTable extends AsyncTask<Void,Void,Boolean>
    {

        @Override
        protected Boolean doInBackground(Void[] p1)
        {
            if((db = Utils.getDb(context)) != null) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(Constants.DELETED_COL, 1);
                db.update(Constants.NOTES_TABLE, contentValues, Constants.ID_COL + " = ?", new String[]{Long.toString(noteId)});
                //db.delete(Constants.NOTES_TABLE, Constants.ID_COL + " = ?", new String[]{Long.toString(noteId)});
                return true;
            } else
                return false;
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            if(result){
                updateConnectedWidgets();
                if(mListener != null)
                    mListener.databaseUpdated();
            }
            super.onPostExecute(result);
        }
    }

    private class UpdateConnectedWidgets extends AsyncTask<Void, Void, Boolean>
    {
        private int[] widgetIds;
        @Override
        protected Boolean doInBackground(Void[] p1)
        {
            if((db = Utils.getDb(context)) != null) {
                Cursor widgetCursor = db.query(Constants.WIDGETS_TABLE, new String[]{Constants.WIDGET_ID_COL},
                        Constants.CONNECTED_NOTE_ID_COL + " = ?", new String[]{Long.toString(noteId)}, null, null, null);
                widgetCursor.moveToFirst();
                widgetIds = new int[widgetCursor.getCount()];
                for (int i = 0; i < widgetCursor.getCount(); i++) {
                    widgetIds[i] = widgetCursor.getInt(0);
                    widgetCursor.moveToNext();
                }
                widgetCursor.close();
                return true;
            } else
                return false;
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            if(result){
                if(deleteNote || titleChanged){
                    WidgetProvider widgetProvider = new WidgetProvider();
                    widgetProvider.onUpdate(context, mgr, widgetIds);
                } else
                    mgr.notifyAppWidgetViewDataChanged(widgetIds, R.id.noteListView);
            }
            super.onPostExecute(result);
        }
    }
}

