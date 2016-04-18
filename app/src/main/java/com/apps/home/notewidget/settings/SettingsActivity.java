package com.apps.home.notewidget.settings;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

public class SettingsActivity extends AppCompatActivity implements SettingsListFragment.OnItemClickListener { //TODO use empty layout as container for fragments
    private Context context;
    private FragmentManager fragmentManager;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        context = this;
        fragmentManager = getSupportFragmentManager();
        preferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);

        Utils.hideShadowSinceLollipop(this);

        if(getSupportActionBar()!=null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        fragmentManager.beginTransaction().replace(R.id.container,
                new SettingsListFragment(), Constants.FRAGMENT_SETTINGS_LIST).commit();
    }

    @Override
    public void onBackPressed() {
        if(fragmentManager.findFragmentByTag(Constants.FRAGMENT_SETTINGS_WIDGET_CONFIG) != null)
            fragmentManager.beginTransaction().replace(R.id.container,
                    new SettingsListFragment(), Constants.FRAGMENT_SETTINGS_LIST).commit();
        else
            super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    private Dialog getNoteSizeDialog(){
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_number_picker, null);
        final NumberPicker picker = (NumberPicker) layout.findViewById(R.id.numberPicker);
        final TextView example = (TextView) layout.findViewById(R.id.textView5);
        picker.setMinValue(10);
        picker.setMaxValue(30);
        picker.setValue(preferences.getInt(Constants.NOTE_TEXT_SIZE_KEY, 14));
        picker.setWrapSelectorWheel(false);
        example.setTextSize(TypedValue.COMPLEX_UNIT_SP, picker.getValue());
        picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                example.setTextSize(TypedValue.COMPLEX_UNIT_SP, newVal);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        return builder.setTitle("Set note text size").setView(layout).
                setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.e("settings", "click "+picker.getValue());
                        preferences.edit().putInt(Constants.NOTE_TEXT_SIZE_KEY, picker.getValue()).putBoolean(Constants.NOTE_TEXT_SIZE_UPDATED, true).apply();
                        Utils.showToast(context, "Text size changed");
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Utils.showToast(context, "Canceled");
            }
        }).create();
    }

    private Dialog getBackupOrRestoreDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        return builder.setItems(new CharSequence[]{"Backup", "Restore"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        getBackupDialog().show();
                        break;
                    case 1:
                        //TODO restore dialog
                        break;
                }
            }
        }).create();
    }

    private Dialog getBackupDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        return builder.setItems(new CharSequence[]{"Data", "Settings", "Both"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        new BackupData().execute(0);
                        break;
                    case 1:
                        new BackupData().execute(1);
                        break;
                    case 2:
                        new BackupData().execute(2);
                        break;
                }
            }
        }).create();
    }

    @Override
    public void onItemClicked(int position) {
        switch (position){
            case 0:
                fragmentManager.beginTransaction().replace(R.id.container,
                        new SettingsWidgetConfigFragment(), Constants.FRAGMENT_SETTINGS_WIDGET_CONFIG).commit();
                break;
            case 1:
                getNoteSizeDialog().show();
                break;
            case 2:
                Dialog dialog = Utils.getAllFolderListDialog(context, "Choose starting folder", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        preferences.edit().putInt(Constants.STARTING_FOLDER, Utils.getFolderId(which)).apply();
                        Utils.showToast(context, "Starting folder was set");
                    }
                });
                if(dialog != null)
                    dialog.show();
                break;
            case 3:
                //getBackupOrRestoreDialog().show();
                break;
        }
    }

    private void copy(File src, File dst) throws IOException{
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private class BackupData extends AsyncTask<Integer,Void, Boolean>{

        @Override
        protected Boolean doInBackground(Integer... params) {

            Calendar calendar = Calendar.getInstance();

            File database = getDatabasePath(Constants.DB_NAME);
            File backupDir = new File(Environment.getExternalStorageDirectory() + "/backup/" + getPackageName());

            File config = new File(getApplicationInfo().dataDir +"/shared_prefs/", Constants.PREFS_NAME + ".xml");

            if(!backupDir.exists())
                backupDir.mkdirs();

            try{
                if(params[0] == 0 || params[0] == 2)
                    copy(database, new File(backupDir, calendar.getTimeInMillis() + "_db.bak"));
                if(params[0] == 1 || params[0] == 2)
                    copy(config, new File(backupDir, calendar.getTimeInMillis() + "_cfg.bak"));

            } catch (IOException e){
                Log.e("Setting", "" + e);
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            if(aBoolean)
                Utils.showToast(context, "Backup finished");
            else
                Utils.showToast(context, "Failed");
        }
    }
}
