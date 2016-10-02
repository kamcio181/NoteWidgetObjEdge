package com.apps.home.notewidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.apps.home.notewidget.objects.Note;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.DatabaseHelper;
import com.apps.home.notewidget.utils.DividerItemDecoration;
import com.apps.home.notewidget.utils.Utils;

import java.util.ArrayList;


public class SearchFragment extends Fragment implements CompoundButton.OnCheckedChangeListener,
        SearchView.OnQueryTextListener{
    private static final String TAG = "SearchFragment";
    private static final String ARG_PARAM1 = "param1";
    private SearchView searchView;
    private AppCompatCheckBox titleSearch, contentSearch;
    private RecyclerView recyclerView;
    private SharedPreferences preferences;
    private Context context;
    private ArrayList<Note> notes;
    private DatabaseHelper helper;

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
        setHasOptionsMenu(true);

        preferences = getActivity().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        context = getActivity();
        ((AppCompatActivity)context).invalidateOptionsMenu();
        helper = new DatabaseHelper(context);

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
        titleSearch = (AppCompatCheckBox) view.findViewById(R.id.titleCheckBox);
        contentSearch = (AppCompatCheckBox) view.findViewById(R.id.contentCheckBox);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.v(TAG, "onCreateOptionsMenu");
        super.onCreateOptionsMenu(menu, inflater);

        getActivity().getMenuInflater().inflate(R.menu.menu_empty, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(TAG, "onOptionsItemSelected");
        return super.onOptionsItemSelected(item);
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

    private void search(final String text){
        if(text.length() > 0)
            helper.searchNotes(titleSearch.isChecked(), contentSearch.isChecked(), text, new DatabaseHelper.OnNotesLoadListener() {
                @Override
                public void onNotesLoaded(ArrayList<Note> notes) {
                    if(notes != null) {
                        SearchFragment.this.notes = notes;
                        if (recyclerView.getAdapter() == null) {
                            recyclerView.setLayoutManager(new LinearLayoutManager(context));
                            recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
                            recyclerView.setAdapter(new SearchRecyclerAdapter(SearchFragment.this.notes,
                                    titleSearch.isChecked(), contentSearch.isChecked(), text, new SearchRecyclerAdapter.OnItemClickListener() {
                                @Override
                                public void onItemClick(View view, int position) {
                                    if (mListener != null)
                                        mListener.onItemClicked(SearchFragment.this.notes.get(position), text);
                                }
                            }));
                        } else {
                            ((SearchRecyclerAdapter) recyclerView.getAdapter()).setNotes(SearchFragment.this.notes);
                            ((SearchRecyclerAdapter) recyclerView.getAdapter()).setSearchInTitle(titleSearch.isChecked());
                            ((SearchRecyclerAdapter) recyclerView.getAdapter()).setSearchInContent(contentSearch.isChecked());
                            ((SearchRecyclerAdapter) recyclerView.getAdapter()).setTextToFind(text.toLowerCase());
                        }
                    }
                    else
                        recyclerView.setAdapter(null);
                }
            });
        else
            recyclerView.setAdapter(null);
    }

    public interface OnItemClickListener {
        void onItemClicked(Note note, String textToFind);
    }
}

class SearchRecyclerAdapter extends RecyclerView.Adapter<SearchRecyclerAdapter.DoubleLineViewHolder> {
    private static OnItemClickListener listener;
    private ArrayList<Note> notes;
    private boolean searchInTitle;
    private boolean searchInContent;
    private String textToFind;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public SearchRecyclerAdapter(ArrayList<Note> notes, boolean searchInTitle,
                                 boolean searchInContent, String textToFind,
                                 OnItemClickListener listener) {
        this.notes = notes;
        SearchRecyclerAdapter.listener = listener;
        this.textToFind = textToFind;
        this.searchInTitle = searchInTitle;
        this.searchInContent = searchInContent;
        setHasStableIds(true);
    }

    public void setNotes(ArrayList<Note> notes) {
        this.notes = notes;
        notifyDataSetChanged();
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

    @Override
    public DoubleLineViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DoubleLineViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.double_line_recycle_view_item, parent, false));
    }

    @Override
    public void onBindViewHolder(DoubleLineViewHolder holder, int position) {
        String title = notes.get(position).getTitle();
        String content = notes.get(position).getNote();

        if (searchInTitle && title.toLowerCase().contains(textToFind)) {
            title = trimmedText(title);
        }
        if (searchInContent && content.toLowerCase().contains(textToFind)) {
            content = trimmedText(content);
        }

        holder.titleTextView.setText(Html.fromHtml(title));
        holder.subtitleTextView.setText(Html.fromHtml(content));
    }

    private String trimmedText(String text) {
        int beginIndex = text.toLowerCase().indexOf(textToFind);
        int endIndex = beginIndex + textToFind.length();
        int offset = 0;
        String prefix = "";

        if (beginIndex > 13) {
            prefix = "...";
            offset = beginIndex - 10;
        }
        return prefix + text.substring(offset, beginIndex) + "<b><u>" + text.substring(beginIndex, endIndex)
                + "</b></u>" + text.substring(endIndex);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class DoubleLineViewHolder extends RecyclerView.ViewHolder {
        public final AppCompatTextView titleTextView;
        public final AppCompatTextView subtitleTextView;

        public DoubleLineViewHolder(final View itemView) {
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null)
                        listener.onItemClick(itemView, getLayoutPosition());
                }
            });

            titleTextView = (AppCompatTextView) itemView.findViewById(R.id.textView2);
            subtitleTextView = (AppCompatTextView) itemView.findViewById(R.id.textView3);
        }
    }

    @Override
    public long getItemId(int position) {
        return notes.get(position).getId();
    }
}
