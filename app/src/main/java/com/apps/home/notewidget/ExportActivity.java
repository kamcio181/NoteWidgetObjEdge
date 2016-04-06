package com.apps.home.notewidget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.apps.home.notewidget.customviews.RobotoEditText;
import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.DividerItemDecoration;
import com.apps.home.notewidget.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

public class ExportActivity extends AppCompatActivity implements View.OnClickListener{
    //private static final String TAG = "ExportActivity";
    private Context context;
    private RecyclerView recyclerView;
    private String path;
    private String note;
    private String title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        note = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        title = getIntent().getStringExtra(Constants.TITLE_KEY);

        setContentView(R.layout.activity_export);
        setupToolbarAndFab();

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        path = Environment.getExternalStorageDirectory().getAbsolutePath();

        new GetFiles().execute();
    }

    private void setupToolbarAndFab(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar()!=null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private DialogInterface.OnClickListener getFileOverrideAction(final String name){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new SaveToFile().execute(name);
            }
        };
    }

    private Dialog setFileName(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_roboto_edit_text, null);
        final RobotoEditText titleEditText = (RobotoEditText) layout.findViewById(R.id.titleEditText);
        titleEditText.setText(title);
        titleEditText.setSelection(0, titleEditText.length());

        AlertDialog dialog = builder.setTitle("Set file name").setView(layout)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = titleEditText.getText().toString().length() == 0 ? "Untitled"
                                : titleEditText.getText().toString();
                        int i = 0;
                        String suffix = "";
                        while (new File(path, name + suffix + ".txt").exists()) {
                            i++;
                            suffix = Integer.toString(i);
                        }
                        new SaveToFile().execute(name + suffix + ".txt");
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Utils.showToast(context, "Canceled");
                    }
                }).create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        return dialog;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.fab:
                if(new File(path).canWrite())
                    setFileName().show();
                else
                    Utils.showToast(this, "You are not allowed to write in this folder");
                break;
        }
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
            String[] items = file.list();
            dirs = new ArrayList<>();
            files = new ArrayList<>();
            if(items != null) {
                //Log.e(TAG, "item " + items[0]);
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
                final int dirsCount = dirs.size();
                dirs.addAll(files);
                if (recyclerView.getAdapter() == null) {
                    recyclerView.setLayoutManager(new LinearLayoutManager(context));
                    recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
                    recyclerView.setAdapter(new RecyclerViewAdapter(dirs, dirsCount, new RecyclerViewAdapter.OnItemClickListener() {
                        @Override
                        public void onItemClick(boolean isDir, String item) {
                            if (isDir) {
                                if (new File(path + item).canRead()) {
                                    path = path + item;
                                    new GetFiles().execute();
                                } else
                                    Utils.showToast(context, "You are not allowed to view this folder");
                            } else {
                                Utils.getConfirmationDialog(context, "Do you want to override this file?",
                                        getFileOverrideAction(item+".txt")).show();
                            }
                        }
                    }));
                } else {
                    ((RecyclerViewAdapter)recyclerView.getAdapter()).changeData(dirs, dirsCount);
                }
            }
        }
    }

    private class SaveToFile extends AsyncTask<String, Void, Boolean>{
        @Override
        protected Boolean doInBackground(String... params) {
            File file = new File(path, params[0]);
            FileOutputStream f;

            try {
                f = new FileOutputStream(file);
                PrintWriter p = new PrintWriter(f);
                String[] lines = note.split("<br/>");
                for(String line : lines)
                    p.println(line);
                p.flush();
                p.close();
                f.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            if(aBoolean) {
                Utils.showToast(context, "Saved");
                finish();
            }
            else
                Utils.showToast(context, "Failed");
        }
    }

    @Override
    public void onBackPressed() {
        if(path.equals("/"))
            super.onBackPressed();
        else{
            path = path.substring(0, path.length()-1);
            path = path.substring(0, path.lastIndexOf("/"));
            new GetFiles().execute();
        }
    }
}
class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{
    private RecyclerViewAdapter.OnItemClickListener listener;
    private ArrayList<String> items;
    private int dirsCount;

    public interface OnItemClickListener{
        void onItemClick(boolean isDir, String item);
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
        else
            holder.icon.setImageResource(R.drawable.ic_exp_grey_folder);
    }

    public void changeData(ArrayList<String> items, int dirsCount){
        this.items = items;
        this.dirsCount = dirsCount;
        notifyDataSetChanged();
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
                            listener.onItemClick(getLayoutPosition()<dirsCount, items.get(getLayoutPosition()));
                }
            });

            titleTextView = (RobotoTextView) itemView.findViewById(R.id.textView2);
            icon = (ImageView) itemView.findViewById(R.id.imageView);

        }
    }
}