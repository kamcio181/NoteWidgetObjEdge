package com.apps.home.notewidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SearchView;

import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.CursorRecyclerAdapter;
import com.apps.home.notewidget.utils.DividerItemDecoration;
import com.apps.home.notewidget.utils.Utils;


public class SearchFragment extends Fragment implements CompoundButton.OnCheckedChangeListener,
        SearchView.OnQueryTextListener{
    private static final String ARG_PARAM1 = "param1";
    private SearchView searchView;
    private CheckBox titleSearch, contentSearch;
    private RecyclerView recyclerView;
    private SharedPreferences preferences;
    private Context context;
    private SearchNotes searchNotes;
    private Cursor cursor;

    private OnItemClickListener mListener;

    private String textToFind;

    public SearchFragment() {
        // Required empty public constructor
    }

    public static SearchFragment newInstance(String textToFind) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, textToFind);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getActivity().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        context = getActivity();
        ((AppCompatActivity)context).invalidateOptionsMenu();

        if (getArguments() != null) {
            textToFind = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchView = (SearchView) view.findViewById(R.id.searchView);
        titleSearch = (CheckBox) view.findViewById(R.id.titleCheckBox);
        contentSearch = (CheckBox) view.findViewById(R.id.contentCheckBox);
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        searchView.setIconified(false);
        searchView.setOnQueryTextListener(this);

        titleSearch.setChecked(preferences.getBoolean(Constants.SEARCH_IN_TITLE, true));
        contentSearch.setChecked(preferences.getBoolean(Constants.SEARCH_IN_CONTENT, true));
        titleSearch.setOnCheckedChangeListener(this);
        contentSearch.setOnCheckedChangeListener(this);

        if(textToFind.length()!=0)
            searchView.setQuery(textToFind, true);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnItemClickListener) {
            mListener = (OnItemClickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        if(cursor != null && !cursor.isClosed())
            cursor.close();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()){
            case R.id.titleCheckBox:
                preferences.edit().putBoolean(Constants.SEARCH_IN_TITLE, isChecked).apply();
                search(searchView.getQuery().toString());
                break;
            case R.id.contentCheckBox:
                preferences.edit().putBoolean(Constants.SEARCH_IN_CONTENT, isChecked).apply();
                search(searchView.getQuery().toString());
                break;
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Log.e("search ", " new text   "+newText);
        search(newText);
        return true;
    }

    private void search(String text){
        if(searchNotes != null)
            searchNotes.cancel(true);
        searchNotes = new SearchNotes();
        searchNotes.execute(text);
    }

    public interface OnItemClickListener {
        void onItemClicked(int noteId, boolean deleted, String textToFind);
    }

    private class SearchNotes extends AsyncTask<String, Integer, Boolean>
    {

        SQLiteDatabase db;

        boolean skipSearch = false;
        String where;
        boolean doubleArgs = false;
        boolean searchInTitle;
        boolean searchInContent;
        String textToFind;
        String textToFindLowerCase;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            searchInTitle = titleSearch.isChecked();
            searchInContent = contentSearch.isChecked();

            if(!searchInTitle && !searchInContent)
                skipSearch = true;
            else if(searchInTitle && !searchInContent){
                where = "LOWER(" + Constants.NOTE_TITLE_COL + ") LIKE ?";
            } else if (!searchInTitle){
                where = "LOWER(" + Constants.NOTE_TEXT_COL + ") LIKE ?";
            } else {
                doubleArgs = true;
                where = "LOWER(" + Constants.NOTE_TITLE_COL + ") LIKE ? OR LOWER(" +Constants.NOTE_TEXT_COL + ") LIKE ?";
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {
            if(!skipSearch) {
                if ((db = Utils.getDb(context)) != null) {

                    if (params[0].trim().length() == 0)
                        return false;
                    textToFind = params[0];
                    textToFindLowerCase = textToFind.toLowerCase();

                    String arg = "%" + textToFindLowerCase + "%";
                    String[] args = doubleArgs? new String[]{arg, arg} : new String[]{arg};
                    Log.e("search" ,  "where " +where);
                    Log.e("search" ,  "arg " + params[0]);
                    cursor = db.query(Constants.NOTES_TABLE, new String[]{Constants.ID_COL, Constants.NOTE_TITLE_COL,
                            Constants.NOTE_TEXT_COL, Constants.DELETED_COL}, where, args, null, null, null);
                    Log.e("search", "count "+cursor.getCount());

                    return true;
                } else
                    return false;
            } else
                return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if(aBoolean){
                cursor.moveToFirst();
                Log.e("SZUKAJKA", "TRUE");
                if (recyclerView.getAdapter() == null) {
                    Log.e("search", "setadapter");
                            recyclerView.setLayoutManager(new LinearLayoutManager(context));
                    recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
                    recyclerView.setAdapter(new SearchCursorRecyclerAdapter(cursor, searchInTitle, searchInContent, textToFindLowerCase,
                            new SearchCursorRecyclerAdapter.OnItemClickListener() {
                                @Override
                                public void onItemClick(View view, int position) {
                                    if (mListener != null){
                                        cursor.moveToPosition(position);
                                        mListener.onItemClicked(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.ID_COL)),
                                                cursor.getInt(cursor.getColumnIndexOrThrow(Constants.DELETED_COL)) == 1, textToFind);
                                    }
                                }
                            }));
                } else {
                    Log.e("search", "swap");
                    ((CursorRecyclerAdapter)recyclerView.getAdapter()).changeCursor(cursor);
                    ((SearchCursorRecyclerAdapter)recyclerView.getAdapter()).setSearchInTitle(searchInTitle);
                    ((SearchCursorRecyclerAdapter)recyclerView.getAdapter()).setSearchInContent(searchInContent);
                    ((SearchCursorRecyclerAdapter)recyclerView.getAdapter()).setTextToFind(textToFindLowerCase);

                }

            } else {
                recyclerView.setAdapter(null);
                if (skipSearch)
                    Utils.showToast(context, "Choose search mode");
            }
        }
    }
}
class SearchCursorRecyclerAdapter extends CursorRecyclerAdapter<SearchCursorRecyclerAdapter.DoubleLineViewHolder> {
    private SearchCursorRecyclerAdapter.OnItemClickListener listener;
    private boolean searchInTitle;
    private boolean searchInContent;
    private String textToFind;

    public interface OnItemClickListener{
        void onItemClick(View view, int position);
    }

    public SearchCursorRecyclerAdapter (Cursor cursor, boolean searchInTitle, boolean searchInContent,
                                        String textToFind, OnItemClickListener listener){
        super(cursor);
        this.searchInTitle = searchInTitle;
        this.searchInContent = searchInContent;
        this.textToFind = textToFind;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(Cursor cursor) {
        return 0;
    }

    @Override
    public void onBindViewHolder(SearchCursorRecyclerAdapter.DoubleLineViewHolder holder, Cursor cursor) {
        String title = cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TITLE_COL));
        String content = cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TEXT_COL));

        if(searchInTitle && title.toLowerCase().contains(textToFind)){
            title = trimmedText(title);
        }
        if(searchInContent && content.toLowerCase().contains(textToFind)){
            content = trimmedText(content);
        }

        Log.e("ada", "title length "+title.length());
        Log.e("ada", "content length "+content.length());

        holder.titleTextView.setText(Html.fromHtml(title));
        holder.subtitleTextView.setText(Html.fromHtml(content));
    }

    @Override
    public SearchCursorRecyclerAdapter.DoubleLineViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DoubleLineViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.double_line_recycle_view_item, parent, false));
    }

    class DoubleLineViewHolder extends RecyclerView.ViewHolder{
        public RobotoTextView titleTextView, subtitleTextView;

        public DoubleLineViewHolder(final View itemView){
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null)
                        listener.onItemClick(itemView, getLayoutPosition());
                }
            });

            titleTextView = (RobotoTextView) itemView.findViewById(R.id.textView2);
            subtitleTextView = (RobotoTextView) itemView.findViewById(R.id.textView3);
        }
    }

    public void setSearchInTitle(boolean searchInTitle) {
        this.searchInTitle = searchInTitle;
    }

    public void setSearchInContent(boolean searchInContent) {
        this.searchInContent = searchInContent;
    }

    public void setTextToFind(String textToFind) {
        this.textToFind = textToFind;
    }

    private String trimmedText(String text){
        int beginIndex = text.toLowerCase().indexOf(textToFind);
        int endIndex = beginIndex + textToFind.length();
        int offset = 0;
        String prefix = "";

        if(beginIndex>13) {
            prefix = "...";
            offset = beginIndex-10;
        }
        return prefix + text.substring(offset, beginIndex) + "<b><u>" + text.substring(beginIndex, endIndex)
                + "</b></u>" + text.substring(endIndex);

/*
        if(midIndex > 15 && text.length() > 24){
            offset = midIndex - 12;
            prefix = "...";
            if(text.length() - midIndex > 15)
                suffix = "...";
            else{
                Log.e("ada ", text+" off "+offset);
                offset = offset + text.length() - midIndex - 12;

                Log.e("ada ", "off "+offset + " text "+text.length()+" mid "+midIndex);
                if(offset < 0)
                    offset = 0;
            }

        }

        return prefix + text.substring(offset, beginIndex) + "<b><u>" + text.substring(beginIndex, endIndex)
                + "</b></u>" + text.substring(endIndex) + suffix;*/
    }
}
