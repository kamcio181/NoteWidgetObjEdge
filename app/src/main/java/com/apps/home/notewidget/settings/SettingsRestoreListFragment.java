package com.apps.home.notewidget.settings;

import android.content.Context;
import android.os.AsyncTask;
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
import com.apps.home.notewidget.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

public class SettingsRestoreListFragment extends Fragment {
    private Context context;
    private static final String TAG = "SettingsRestoreListFragment";
    private static final String ARG_PARAM1 = "param1";
    private static boolean dbRestore;
    private RecyclerView recyclerView;
    private static OnItemClickListener mListener;
    private static ArrayList<String> recyclerViewItems;
    private static ArrayList<String> files;
    private static File backupDir;
    private static String suffix;

    public SettingsRestoreListFragment() {
        // Required empty public constructor
    }

    public static SettingsRestoreListFragment newInstance(boolean dbRestore) {
        SettingsRestoreListFragment fragment = new SettingsRestoreListFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PARAM1, dbRestore);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            dbRestore = getArguments().getBoolean(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();
        ((AppCompatActivity)context).invalidateOptionsMenu();
        backupDir = Utils.getBackupDir(context);
        suffix = dbRestore? "_db.bak" : "_cfg.bak";
        return inflater.inflate(R.layout.fragment_note_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String title = dbRestore? "Restore data" : "Restore settings";

        ((AppCompatActivity)context).getSupportActionBar().setTitle(title);
        recyclerView = (RecyclerView) view;
    }

    static class SingleLineViewHolder extends RecyclerView.ViewHolder{
        public RobotoTextView titleTextView;

        public SingleLineViewHolder(View itemView){
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null)
                        mListener.onItemClicked(dbRestore, new File(backupDir, files.get(getLayoutPosition())));
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
    public void onResume() {
        super.onResume();

        new GetFiles().execute();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnItemClickListener {
        void onItemClicked(boolean dbRestore, File file);
    }

    private class GetFiles extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            if (!backupDir.exists())
                return false;

            String[] items = backupDir.list();
            recyclerViewItems = new ArrayList<>();
            files = new ArrayList<>();

            if(items != null) {
                Calendar calendar = Calendar.getInstance();


                for (String i : items) {
                    if (i.endsWith(suffix)){
                        files.add(i);
                        calendar.setTimeInMillis(Long.parseLong(i.substring(0, i.lastIndexOf("_"))));
                        recyclerViewItems.add(String.format("%1$tb %1$te, %1$tY %1$tT", calendar));
                    }
                }
                Collections.sort(recyclerViewItems, Collections.reverseOrder());
                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (result) {
                if (recyclerView.getAdapter() == null) {
                    recyclerView.setLayoutManager(new LinearLayoutManager(context));
                    recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
                    recyclerView.setAdapter(new RecyclerView.Adapter() {
                        @Override
                        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                            return new SingleLineViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.single_line_item, parent, false));
                        }

                        @Override
                        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                            ((SingleLineViewHolder)holder).titleTextView.setText(recyclerViewItems.get(position));
                        }

                        @Override
                        public int getItemCount() {
                            return recyclerViewItems.size();
                        }
                    });
                } else {
                    (recyclerView.getAdapter()).notifyDataSetChanged();
                }
            } else{
                Utils.showToast(context, "Backup folder is empty. Please push question mark to check " +
                        "where backup should be placed");
            }
        }
    }
}
