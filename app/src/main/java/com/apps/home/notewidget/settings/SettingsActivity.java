package com.apps.home.notewidget.settings;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
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

public class SettingsActivity extends AppCompatActivity implements SettingsListFragment.OnItemClickListener,
        SettingsRestoreListFragment.OnItemClickListener{ //TODO use empty layout as container for fragments
    private Context context;
    private FragmentManager fragmentManager;
    private SharedPreferences preferences;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        context = this;
        fragmentManager = getSupportFragmentManager();
        preferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
        if(fragmentManager.findFragmentByTag(Constants.FRAGMENT_SETTINGS_WIDGET_CONFIG) != null
                || fragmentManager.findFragmentByTag(Constants.FRAGMENT_SETTINGS_RESTORE_LIST) != null)
            fragmentManager.beginTransaction().replace(R.id.container,
                    new SettingsListFragment(), Constants.FRAGMENT_SETTINGS_LIST).commit();
        else
            super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        switch (fragmentManager.findFragmentById(R.id.container).getTag()){
            case Constants.FRAGMENT_SETTINGS_RESTORE_LIST:
                getMenuInflater().inflate(R.menu.menu_settings_restore, menu);
                break;
            default:
                getMenuInflater().inflate(R.menu.menu_empty, menu);
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_help:
                getRestoreHelpDialog().show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private Dialog getRestoreHelpDialog(){
        return new AlertDialog.Builder(context).setTitle("Help").setMessage("Backups have to be placed in below folder in internal memory\n"+
        "backup/" + getPackageName() + "/").setPositiveButton("I've got it!", null).create();
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
                        getRestoreDialog().show();
                        break;
                }
            }
        }).create();
    }

    private Dialog getBackupDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        return builder.setTitle("Backup").setItems(new CharSequence[]{"Data", "Settings", "Data and settings"}, new DialogInterface.OnClickListener() {
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

    private Dialog getRestoreDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        return builder.setTitle("Restore").setItems(new CharSequence[]{"Data", "Settings"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                fragmentManager.beginTransaction().replace(R.id.container,
                        SettingsRestoreListFragment.newInstance(which == 0), Constants.FRAGMENT_SETTINGS_RESTORE_LIST).commit();
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
                        preferences.edit().putInt(Constants.STARTING_FOLDER_KEY, Utils.getFolderId(which)).apply();
                        Utils.showToast(context, "Starting folder was set");
                    }
                });
                if(dialog != null)
                    dialog.show();
                break;
            case 3:
                getBackupOrRestoreDialog().show();
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

    @Override
    public void onItemClicked(final boolean dbRestore, final File file) {
        String message = dbRestore? "Warning! This action will replace your current database and all" +
                " current notes and folders will be lost.\n" + "Do you want to continue?" :
                "Warning! This action will replace your current settings and all current configuration will be lost.\n" +
                        "Do you want to continue?";
        Utils.getConfirmationDialog(context, message, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new RestoreData(file, dbRestore).execute();
            }
        }).show();
    }

    private class BackupData extends AsyncTask<Integer,Void, Boolean>{

        @Override
        protected Boolean doInBackground(Integer... params) {

            Calendar calendar = Calendar.getInstance();

            File database = Utils.getDbFile(context);
            File backupDir = Utils.getBackupDir(context);
            File config = Utils.getPrefsFile(context);

            boolean dirExists = true;

            if(!backupDir.exists())
                dirExists = backupDir.mkdirs();

            if(dirExists) {
                try {
                    if (params[0] == 0 || params[0] == 2)
                        copy(database, new File(backupDir, calendar.getTimeInMillis() + "_db.bak"));
                    if (params[0] == 1 || params[0] == 2)
                        copy(config, new File(backupDir, calendar.getTimeInMillis() + "_cfg.bak"));

                } catch (IOException e) {
                    Log.e("Setting", "" + e);
                    return false;
                }
                return true;
            } else
                return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            if(aBoolean)
                Utils.showToast(context, "Backup saved to:\nbackup/" + getPackageName() + "/");
            else
                Utils.showToast(context, "Failed");
        }
    }

    private class RestoreData extends AsyncTask<Void,Void, Boolean>{
        private File backupFile;
        private boolean dbRestore;

        public RestoreData(File backupFile, boolean dbRestore){
            this.backupFile = backupFile;
            this.dbRestore = dbRestore;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            File database = Utils.getDbFile(context);
            File config = Utils.getPrefsFile(context);

            try {
                if (dbRestore)
                    copy(backupFile, database);
                else
                    copy(backupFile, config);


            } catch (IOException e) {
                Log.e("Setting", "" + e);
                return false;
            }
            return true;
            }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            if(aBoolean) {
                if (dbRestore) {
                    Utils.showToast(context, "Notes restored");
                    Utils.clearWidgetsTable(context, new Utils.FinishListener() {
                        @Override
                        public void onFinished(boolean result) {
                            preferences.edit().remove(Constants.STARTING_FOLDER_KEY)
                                    .putBoolean(Constants.RELOAD_MAIACTIVITY_AFTER_RESTORE_KEY, true).apply();
                            Utils.updateAllWidgets(context);
                            onBackPressed();
                        }
                    });
                } else {
                    Utils.showToast(context, "Settings restored");
                    preferences.edit().remove(Constants.STARTING_FOLDER_KEY)
                            .putBoolean(Constants.RELOAD_MAIACTIVITY_AFTER_RESTORE_KEY, true).apply();
                    Utils.updateAllWidgets(context);
                    onBackPressed();
                }
            } else
                Utils.showToast(context, "Failed");
        }
    }
}
