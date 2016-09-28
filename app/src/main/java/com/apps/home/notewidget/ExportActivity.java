package com.apps.home.notewidget;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.DividerItemDecoration;
import com.apps.home.notewidget.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

public class ExportActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "ExportActivity";
    private Context context;
    private RecyclerView recyclerView;
    private String path;
    private String note;
    private String title;
    private boolean exit = false;
    private final Handler handler = new Handler();
    private Runnable exitRunnable;
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_export);

        Utils.hideShadowSinceLollipop(this);

        setResetExitFlagRunnable();

        note = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        title = getIntent().getStringExtra(Constants.TITLE_KEY);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        setupToolbarAndFab();

        readFiles();
    }

    private void setupToolbarAndFab(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar()!=null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        try {
            Field titleField = Toolbar.class.getDeclaredField("mTitleTextView");
            titleField.setAccessible(true);
            TextView barTitleView = (TextView) titleField.get(toolbar);
            barTitleView.setEllipsize(TextUtils.TruncateAt.START);
            barTitleView.setFocusable(true);
            barTitleView.setFocusableInTouchMode(true);
            barTitleView.requestFocus();
            barTitleView.setSingleLine(true);
            barTitleView.setSelected(true);
            //barTitleView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            //barTitleView.setMarqueeRepeatLimit(-1);

        } catch (NoSuchFieldException e){
            Log.e(TAG, "" + e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, " " + e);
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if(fab!=null)
            fab.setOnClickListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if(!exit){
                exit = true;
                handler.postDelayed(exitRunnable, 5000);
                Utils.showToast(this, getString(R.string.press_back_button_again_to_exit));
            } else {
                finish();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void setResetExitFlagRunnable(){
        exitRunnable = new Runnable() {
            @Override
            public void run() {
                exit = false;
            }
        };
    }

    private DialogInterface.OnClickListener getFileOverrideAction(final String name){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveNote(name);
            }
        };
    }

    private void saveNote(String name){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
            new SaveToFile().execute(name);
        else {
            this.name = name;
            ActivityCompat.requestPermissions(ExportActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Constants.WRITE_PERMISSION);
        }
    }

    private void readFiles(){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED){
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
            new GetFiles().execute();
        }
        else {
            ActivityCompat.requestPermissions(ExportActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    Constants.WRITE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.WRITE_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(name != null)
                        new SaveToFile().execute(name);
                    else readFiles();

                } else {
                    Utils.showToast(context, "Write permission is required to export notes");
                    finish();
                }
                break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.fab:
                if(new File(path).canWrite())
                    Utils.getNameDialog(this, title, getString(R.string.set_file_name), 32, getString(R.string.file_name), new Utils.OnNameSet() {
                        @Override
                        public void onNameSet(String name) {
                            if (name.length() == 0)
                                name = getString(R.string.untitled);
                            int i = 0;
                            String suffix = "";
                            while (new File(path, name + suffix + ".txt").exists()) {
                                i++;
                                suffix = Integer.toString(i);
                            }
                            saveNote(name + suffix + ".txt");
                            Utils.showOrHideKeyboard(((AppCompatActivity) context).getWindow(), false);
                        }
                    }).show();
                else
                    Utils.showToast(this, getString(R.string.you_are_not_allowed_to_write_in_this_folder));
                break;
        }
    }

    //    private Dialog setFileName(){
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        LayoutInflater inflater = getLayoutInflater();
//        @SuppressLint("InflateParams") View layout = inflater.inflate(R.layout.dialog_edit_text, null);
//        final AppCompatEditText titleEditText = (AppCompatEditText) layout.findViewById(R.id.titleEditText);
//        titleEditText.setText(title);
//        titleEditText.setSelection(0, titleEditText.length());
//
//        AlertDialog dialog = builder.setTitle().setView(layout)
//                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        String name = titleEditText.getText().toString().length() == 0 ? getString(R.string.untitled)
//                                : titleEditText.getText().toString();
//                        int i = 0;
//                        String suffix = "";
//                        while (new File(path, name + suffix + ".txt").exists()) {
//                            i++;
//                            suffix = Integer.toString(i);
//                        }
//                        saveNote(name + suffix + ".txt");
//                        Utils.showOrHideKeyboard(((AppCompatActivity) context).getWindow(), false);
//                    }
//                })
//                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        Utils.showToast(context, getString(R.string.canceled));
//                        Utils.showOrHideKeyboard(((AppCompatActivity) context).getWindow(), false);
//                    }
//                }).create();
//        Utils.showOrHideKeyboard(dialog.getWindow(), true);
//        return dialog;
//    }

    private class GetFiles extends AsyncTask<Void, Void, Boolean>{
        private ArrayList<String> dirs;
        private ArrayList<String> files;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
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
                for (String i : items) {
                    if (new File(path+i).isDirectory())
                        dirs.add(i);
                    else if (i.endsWith(".txt"))
                        files.add(i.substring(0, i.lastIndexOf(".")));
                }
                Collections.sort(dirs, String.CASE_INSENSITIVE_ORDER);
                Collections.sort(files, String.CASE_INSENSITIVE_ORDER);
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
                                    Utils.showToast(context, getString(R.string.you_are_not_allowed_to_write_in_this_folder));
                            } else {
                                Utils.getConfirmationDialog(context, getString(R.string.do_you_want_to_override_this_file),
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
                Utils.showToast(context, getString(R.string.saving_to_txt_file));
                finish();
            }
            else
                Utils.showToast(context, getString(R.string.failed));
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
    private static RecyclerViewAdapter.OnItemClickListener listener;
    private static ArrayList<String> items;
    private static int dirsCount;

    public interface OnItemClickListener{
        void onItemClick(boolean isDir, String item);
    }

    public RecyclerViewAdapter(ArrayList<String> items, int dirsCount, OnItemClickListener listener) {
        RecyclerViewAdapter.items = items;
        RecyclerViewAdapter.listener = listener;
        RecyclerViewAdapter.dirsCount = dirsCount;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.single_line_with_icon_item, parent, false));
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
        RecyclerViewAdapter.items = items;
        RecyclerViewAdapter.dirsCount = dirsCount;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        public final AppCompatTextView titleTextView;
        public final AppCompatImageView icon;

        public ViewHolder(final View itemView){
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                        if(listener != null)
                            listener.onItemClick(getLayoutPosition()<dirsCount, items.get(getLayoutPosition()));
                }
            });

            titleTextView = (AppCompatTextView) itemView.findViewById(R.id.textView2);
            icon = (AppCompatImageView) itemView.findViewById(R.id.imageView);

        }
    }
}