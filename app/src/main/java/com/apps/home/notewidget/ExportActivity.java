package com.apps.home.notewidget;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;

import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.CursorRecyclerAdapter;
import com.apps.home.notewidget.utils.DividerItemDecoration;
import com.apps.home.notewidget.utils.Utils;

import java.io.File;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ExportActivity extends AppCompatActivity {
    private static final String TAG = "ExportActivity";
    private Context context;
    private RecyclerView recyclerView;
    private String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_export);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        path = Environment.getExternalStorageDirectory().getAbsolutePath();

        new GetFiles().execute();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    private class GetFiles extends AsyncTask<Void, Void, Boolean>{
        private ArrayList<String> dirs;
        private ArrayList<String> files;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            /*if(path.equals(""))
                path="/";*/
            path = path+"/";
            setTitle(path);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            File file = new File(path);
            Log.e(TAG, "dir "+file.isDirectory());
            String[] items = file.list();
            dirs = new ArrayList<>();
            files = new ArrayList<>();
            Log.e(TAG, "GF "+path);
            if(items !=null) {
                Log.e(TAG, "not null "+items.length);
                Log.e(TAG, "item " + items[0]);
                for (String i : items) {
                    if (new File(path+i).isDirectory())
                        dirs.add(i);
                    else if (i.endsWith(".txt"))
                        files.add(i.substring(0, i.lastIndexOf(".")));
                }
                Collections.sort(dirs);
                Collections.sort(files);
                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (result) {
                Log.e(TAG, "set adapter");
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
                recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
                final int dirsCount = dirs.size();
                dirs.addAll(files);
                recyclerView.setAdapter(new RecyclerViewAdapter(dirs, dirsCount, new RecyclerViewAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        if (position < dirsCount) {
                            if(new File(path+dirs.get(position)).canRead()) {
                                path = path + dirs.get(position);
                                new GetFiles().execute();
                            } else
                                Utils.showToast(context, "You are not allowed to view this folder");
                        } else {
                            //TODO save file - overrides
                            Utils.showToast(context, "file was clicked");
                        }
                    }
                }));
            }
        }
    }

    @Override
    public void onBackPressed() {
        Log.e(TAG, "onBP st "+path);
        if(path.equals("/"))
            super.onBackPressed();
        else{
            path = path.substring(0, path.length()-1);
            path = path.substring(0, path.lastIndexOf("/"));
            Log.e(TAG, "onBP end "+path);
            new GetFiles().execute();
        }
    }
}
class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{
    private RecyclerViewAdapter.OnItemClickListener listener;
    private ArrayList<String> items;
    private int dirsCount;

    public interface OnItemClickListener{
        void onItemClick(View view, int position);
    }

    public RecyclerViewAdapter(ArrayList<String> items, int dirsCount, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
        this.dirsCount = dirsCount;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.single_line_recycle_view_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.titleTextView.setText(items.get(position));
        if(position>=dirsCount)
            holder.icon.setImageResource(R.drawable.ic_exp_grey_file);
        Log.e("adapter", "pos "+position+" name "+items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        public RobotoTextView titleTextView;
        public ImageView icon;

        public ViewHolder(final View itemView){
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                        if(listener != null)
                            listener.onItemClick(itemView, getLayoutPosition());
                }
            });

            titleTextView = (RobotoTextView) itemView.findViewById(R.id.textView2);
            icon = (ImageView) itemView.findViewById(R.id.imageView);

        }
    }
}