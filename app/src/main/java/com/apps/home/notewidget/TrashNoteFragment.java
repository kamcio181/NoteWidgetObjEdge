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
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.Utils;
import com.apps.home.notewidget.widget.WidgetProvider;

import java.util.Calendar;

public class TrashNoteFragment extends Fragment implements Utils.FinishListener{
    private static final String TAG = "TrashNoteFragment";
    private static final String ARG_PARAM1 = "param1";
    private RobotoTextView noteTextView;
    private long creationTimeMillis;
    private SQLiteDatabase db;
    private long noteId;
    private Context context;
    private int action;


    public TrashNoteFragment() {
        // Required empty public constructor
    }

    public static TrashNoteFragment newInstance( long noteId) {
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

        new LoadNote().execute();
    }

    public void reloadNote(){
        new LoadNote().execute();
    }

    @Override
    public void onFinished(int task, boolean result) {
        ((AppCompatActivity) context).onBackPressed();

    }

    private class LoadNote extends AsyncTask<Void, Integer, Boolean> {
        private Cursor cursor;

        @Override
        protected Boolean doInBackground(Void... params) {
            if((db = Utils.getDb(context)) != null) {
                cursor = db.query(Constants.NOTES_TABLE, new String[]{Constants.MILLIS_COL,
                                Constants.NOTE_TITLE_COL, Constants.NOTE_TEXT_COL},
                        Constants.ID_COL + " = ?", new String[]{Long.toString(noteId)}, null, null, null);
                return (cursor.getCount()>0);
            } else
                return false;

        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if(aBoolean){
                cursor.moveToFirst();

                noteTextView.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TEXT_COL))));
                ((AppCompatActivity)context).getSupportActionBar().setTitle(cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TITLE_COL)));
                Calendar calendar = Calendar.getInstance();
                creationTimeMillis = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.MILLIS_COL));
                calendar.setTimeInMillis(creationTimeMillis);
                ((AppCompatActivity)context).getSupportActionBar().setSubtitle(String.format("%1$tb %1$te, %1$tY %1$tT", calendar));

                cursor.close();
            } else {
                ((AppCompatActivity)context).onBackPressed();
            }
        }
    }

    public void removeOrRestoreFromTrash(int action){
        this.action = action;
        Utils.restoreOrRemoveNoteFromTrash(context, noteId, action, this);
    }
}

