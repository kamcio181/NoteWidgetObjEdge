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
import android.view.View;
import android.view.ViewGroup;

import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.Utils;
import com.apps.home.notewidget.widget.WidgetProvider;

import java.util.Calendar;

public class TrashNoteFragment extends Fragment {
    private static final String TAG = "TrashNoteFragment";
    private static final String ARG_PARAM1 = "param1";
    private RobotoTextView noteTextView;
    private long creationTimeMillis;
    private SQLiteDatabase db;
    private long noteId;
    private AppWidgetManager mgr;
    private Context context;


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

        mgr = AppWidgetManager.getInstance(context);
        noteTextView = (RobotoTextView) view.findViewById(R.id.noteEditText);

        new LoadNote().execute();

    }

    private void updateConnectedWidgets(){
        new UpdateConnectedWidgets().execute();
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
        new RestoreOrRemoveNoteFromTrash().execute(action);
    }

    private class RestoreOrRemoveNoteFromTrash extends AsyncTask<Integer,Void,Boolean[]>
    {

        @Override
        protected Boolean[] doInBackground(Integer[] p1)
        {
            if((db = Utils.getDb(context)) != null) {
                if (p1[0] == R.id.action_delete_from_trash) { //remove note
                    db.delete(Constants.NOTES_TABLE, Constants.ID_COL + " = ?", new String[]{Long.toString(noteId)});
                    Log.e(TAG, "delete all");
                    return new Boolean[]{true, true};
                } else { //restore note
                    Log.e(TAG, "restore");
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(Constants.DELETED_COL, 0);
                    db.update(Constants.NOTES_TABLE, contentValues, Constants.ID_COL + " = ?", new String[]{Long.toString(noteId)});
                    return new Boolean[]{true, false};
                }
            } else
                return new Boolean[]{false};
        }

        @Override
        protected void onPostExecute(Boolean[] result)
        {
            super.onPostExecute(result);
            if(!result[0]){
                ((AppCompatActivity) context).onBackPressed();
            }
            else if(result[1]) {
                Utils.showToast(context, "Note was removed");
                updateConnectedWidgets();
            }
            else {
                Utils.showToast(context, "Notes was restored");
                ((AppCompatActivity) context).onBackPressed();
            }
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
                WidgetProvider widgetProvider = new WidgetProvider();
                widgetProvider.onUpdate(context, mgr, widgetIds);
            }
            ((AppCompatActivity)context).onBackPressed();
            super.onPostExecute(result);
        }
    }
}

