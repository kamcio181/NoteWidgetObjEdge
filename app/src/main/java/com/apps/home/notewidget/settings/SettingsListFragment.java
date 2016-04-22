package com.apps.home.notewidget.settings;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.utils.DividerItemDecoration;

public class SettingsListFragment extends Fragment {
    private Context context;
    private RecyclerView recyclerView;
    private OnItemClickListener mListener;
    private String[] items;

    public SettingsListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();
        ((AppCompatActivity)context).invalidateOptionsMenu();
        return inflater.inflate(R.layout.fragment_note_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((AppCompatActivity)context).getSupportActionBar().setTitle(getString(R.string.settings));
        items = context.getResources().getStringArray(R.array.settings_items);

        recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
        recyclerView.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new SingleLineViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.single_line_item, parent, false));
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                ((SingleLineViewHolder)holder).titleTextView.setText(items[position]);
            }

            @Override
            public int getItemCount() {
                return items.length;
            }
        });


    }

    class SingleLineViewHolder extends RecyclerView.ViewHolder{
        public RobotoTextView titleTextView;

        public SingleLineViewHolder(View itemView){
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null)
                        mListener.onItemClicked(getLayoutPosition());
                }
            });
            titleTextView = (RobotoTextView) itemView.findViewById(R.id.textView2);
        }
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

    public interface OnItemClickListener {
        void onItemClicked(int position);
    }
}
