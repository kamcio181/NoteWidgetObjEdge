package com.apps.home.notewidget;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Calendar;

public class NoteListFragment extends Fragment {
    private static final String TAG = "NoteListFragment";

    private RecyclerView recyclerView;
    private OnItemClickListener mListener;

    public NoteListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().invalidateOptionsMenu();



        return inflater.inflate(R.layout.fragment_note_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("My Notes");
        ((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle("");
        ((MainActivity)getActivity()).setOnTitleClickListener(false);
        ((MainActivity)getActivity()).getFab().setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_add_white));
        recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        recyclerView.setAdapter(new NotesCursorRecyclerAdapter(((MainActivity) getActivity()).getCursor(),
                new NotesCursorRecyclerAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        if (mListener != null)
                            mListener.onItemClicked(position);
                    }
                }));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnItemClickListener) {
            mListener = (OnItemClickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnItemClickListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnItemClickListener {
        void onItemClicked(int position);
    }
}


class NotesCursorRecyclerAdapter extends CursorRecyclerAdapter<NotesCursorRecyclerAdapter.DoubleLineViewHolder>{
    private Calendar calendar;
    private NotesCursorRecyclerAdapter.OnItemClickListener listener;

    public interface OnItemClickListener{
        void onItemClick(View view, int position);
    }

    public NotesCursorRecyclerAdapter (Cursor cursor, OnItemClickListener listener){
        super(cursor);
        this.listener = listener;
        calendar = Calendar.getInstance();
    }

    @Override
    public int getItemViewType(Cursor cursor) {
        return 0;
    }

    @Override
    public void onBindViewHolder(NotesCursorRecyclerAdapter.DoubleLineViewHolder holder, Cursor cursor) {
        calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(Constants.MILLIS_COL)));
        Log.e("RecycleViewAdapter", "millis " + cursor.getLong(cursor.getColumnIndexOrThrow(Constants.MILLIS_COL)));
        holder.titleTextView.setText(cursor.getString(cursor.getColumnIndexOrThrow(Constants.NOTE_TITLE_COL)));
        holder.subtitleTextView.setText(String.format("%1$tb %1$te, %1$tY %1$tT", calendar));
    }

    @Override
    public NotesCursorRecyclerAdapter.DoubleLineViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DoubleLineViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.double_line_recycle_view_item, parent, false));
    }

    class DoubleLineViewHolder extends RecyclerView.ViewHolder{
        public RobotoTextView titleTextView, subtitleTextView;

        public DoubleLineViewHolder(final View itemView){
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener != null)
                        listener.onItemClick(itemView, getLayoutPosition());
                }
            });

            titleTextView = (RobotoTextView) itemView.findViewById(R.id.textView2);
            subtitleTextView = (RobotoTextView) itemView.findViewById(R.id.textView3);
        }
    }
}
