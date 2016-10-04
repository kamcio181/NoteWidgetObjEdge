package com.apps.home.notewidget.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.os.TransactionTooLargeException;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.edge.EdgePanelProvider;
import com.apps.home.notewidget.objects.Folder;
import com.apps.home.notewidget.objects.Widget;
import com.apps.home.notewidget.widget.WidgetProvider;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class Utils {
    private static final String TAG = "Utils";
    private static Toast toast;
    private static int[][][] widgetLayouts;
    private static int idArray[];
    private static SharedPreferences preferences;
    private static long myNotesNavId = -1;
    private static long trashNavId = -1;
    private static ArrayList<Folder> folders;

    public static void showToast(Context context, String message){
        if(toast != null)
            toast.cancel();
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static int getLayoutFile(Context context, int themeMode, int widgetMode){
        if(widgetLayouts == null) {
            widgetLayouts = new int[3][2][2];
            widgetLayouts[0][0][0] = R.layout.appwidget_title_miui_light;
            widgetLayouts[0][0][1] = R.layout.appwidget_config_miui_light;
            widgetLayouts[0][1][0] = R.layout.appwidget_title_miui_dark;
            widgetLayouts[0][1][1] = R.layout.appwidget_config_miui_dark;
            widgetLayouts[1][0][0] = R.layout.appwidget_title_lollipop_light;
            widgetLayouts[1][0][1] = R.layout.appwidget_config_lollipop_light;
            widgetLayouts[1][1][0] = R.layout.appwidget_title_lollipop_dark;
            widgetLayouts[1][1][1] = R.layout.appwidget_config_lollipop_dark;
            widgetLayouts[2][0][0] = R.layout.appwidget_title_simple_light;
            widgetLayouts[2][0][1] = R.layout.appwidget_config_simple_light;
            widgetLayouts[2][1][0] = R.layout.appwidget_title_simple_dark;
            widgetLayouts[2][1][1] = R.layout.appwidget_config_simple_dark;
        }

        return widgetLayouts[context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).
                getInt(Constants.WIDGET_THEME_KEY, 0)][themeMode][widgetMode];
    }

    public static int switchWidgetMode(int currentMode){
        return currentMode == Constants.WIDGET_MODE_TITLE? Constants.WIDGET_MODE_CONFIG : Constants.WIDGET_MODE_TITLE;
    }

    public static int switchThemeMode(int currentMode){
        return currentMode == Constants.WIDGET_THEME_LIGHT? Constants.WIDGET_THEME_DARK : Constants.WIDGET_THEME_LIGHT;
    }

    public static void setTitleMarquee(Toolbar toolbar){
        try {
            Field titleField = Toolbar.class.getDeclaredField("mTitleTextView");
            titleField.setAccessible(true);
            setMarquee((TextView) titleField.get(toolbar));

        } catch (NoSuchFieldException e){
            Log.e(TAG, "" + e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, " " + e);
        }
    }
//    public static void setSubtitleMarquee(Toolbar toolbar){
//        try {
//            Field titleField = Toolbar.class.getDeclaredField("mSubtitleTextView");
//            titleField.setAccessible(true);
//            setMarquee((TextView) titleField.get(toolbar));
//
//        } catch (NoSuchFieldException e){
//            Log.e(TAG, "" + e);
//        } catch (IllegalAccessException e) {
//            Log.e(TAG, " " + e);
//        }
//    }

    private static void setMarquee(TextView textView){
        textView.setSingleLine(true);
        textView.setSelected(true);
        textView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        textView.setMarqueeRepeatLimit(-1);
    }

    public static void showOrHideKeyboard(Window window, boolean show){
        if(show)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        else
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public static File getBackupDir(Context context){
        return new File(Environment.getExternalStorageDirectory() + "/backup/" + context.getPackageName());
    }

    public static File getPrefsFile(Context context){
        return new File(context.getApplicationInfo().dataDir +"/shared_prefs/", Constants.PREFS_NAME + ".xml");
    }

    public static File getDbFile(Context context){
        return context.getDatabasePath(Constants.DB_NAME);
    }

    public static Dialog getMultilevelNoteManualDialog(final Context context){
        LayoutInflater inflater = ((AppCompatActivity)context).getLayoutInflater();
        @SuppressLint("InflateParams") View layout = inflater.inflate(R.layout.dialog_multilevel_note_manual, null);
        final AppCompatCheckBox checkBox = (AppCompatCheckBox) layout.findViewById(R.id.checkBox);
        return new AlertDialog.Builder(context).setTitle(context.getString(R.string.tip)).setView(layout).setCancelable(false).
                setPositiveButton(context.getString(R.string.i_have_got_it), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (checkBox.isChecked()) {
                            if (preferences == null)
                                preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
                            preferences.edit().putBoolean(Constants.SKIP_MULTILEVEL_NOTE_MANUAL_DIALOG_KEY, true).apply();
                        }
                    }
                }).create();
    }

    public static Dialog getConfirmationDialog(final Context context, String title, DialogInterface.OnClickListener action){
        return new AlertDialog.Builder(context).setMessage(title)
                .setPositiveButton(context.getString(R.string.confirm), action)
                .setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Utils.showToast(context, context.getString(R.string.canceled));
                    }
                }).create();
    }

        public interface OnNameSet {
        void onNameSet(String name);
    }

    private static Dialog getEdiTextDialog(final Context context, final String text, String title,
                                           final OnNameSet action, String hint, boolean hideContent, final int charLimit, String neutralText, final OnNameSet neutralAction){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = ((AppCompatActivity)context).getLayoutInflater();
        @SuppressLint("InflateParams") View layout = inflater.inflate(R.layout.dialog_edit_text, null);
        final TextInputEditText editText = (TextInputEditText) layout.findViewById(R.id.titleEditText);
        final TextInputLayout inputLayout = (TextInputLayout) layout.findViewById(R.id.editTextLayout);
        final AppCompatTextView charNumberTV = (AppCompatTextView) layout.findViewById(R.id.textView8);
        editText.setText(text);
        inputLayout.setHint(hint);

        if(charLimit > 0) {
            String numbers = text.length() + "/" + charLimit;
            charNumberTV.setText(numbers);
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    String text = s.toString().trim();
                    String numbers = text.length() + "/" + charLimit;
                    charNumberTV.setText(numbers);
                    if (text.length() > charLimit) {
                        editText.setText(text.substring(0, charLimit));
                        editText.setSelection(charLimit);
                    }
                }
            });
        } else
            charNumberTV.setVisibility(View.GONE);

        if(hideContent)
            editText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        editText.setSelection(0, editText.length());
        builder.setTitle(title).setView(layout)
                .setPositiveButton(R.string.confirm, null)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showToast(context, context.getString(R.string.canceled));
                        showOrHideKeyboard(((AppCompatActivity) context).getWindow(), false);
                    }
                });
        if(neutralText != null){
            builder.setNeutralButton(neutralText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if(neutralAction != null)
                        neutralAction.onNameSet(editText.getText().toString().trim());
                }
            });
        }
        final AlertDialog editTextDialog = builder.create();
        editTextDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                editTextDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String text = editText.getText().toString().trim();
                        if(charLimit == 0 || text.length() <= charLimit) {
                            showOrHideKeyboard(((AppCompatActivity) context).getWindow(), false);
                            if (action != null)
                                action.onNameSet(text);
                            editTextDialog.dismiss();
                        } else
                            showToast(context, "The maximum length is " + charLimit);
                    }
                });
            }
        });
//        AlertDialog dialog = builder.setTitle(title).setView(layout)
//                .setPositiveButton(context.getString(R.string.confirm), new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        showOrHideKeyboard(((AppCompatActivity) context).getWindow(), false);
//                        if(action != null)
//                            action.onNameSet(titleEditText.getText().toString().trim());
//                    }
//                })
//                .setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        showToast(context, context.getString(R.string.canceled));
//                        showOrHideKeyboard(((AppCompatActivity) context).getWindow(), false);
//                    }
//                }).create();


        showOrHideKeyboard(editTextDialog.getWindow(), true);

        return editTextDialog;
    }

    public static Dialog getNameDialog(final Context context, final String text, final String title,
                                       int charLimit, String hint, final OnNameSet action){

        return getEdiTextDialog(context, text, title, action, hint, false, charLimit, null, null);
    }

    public static Dialog getNameDialog(final Context context, final String text, final String title,
                                       int charLimit, String hint, final OnNameSet action, String neutralText, OnNameSet neutralAction){

        return getEdiTextDialog(context, text, title, action, hint, false, charLimit, neutralText, neutralAction);
    }

    public static Dialog getFolderListDialog(Context context, Menu menu, int[] exclusions, String title,
                                             DialogInterface.OnClickListener action){
        int menuSize = menu.size();
        int size = menuSize - (exclusions == null? 0 : exclusions.length) - 2;
        CharSequence[] nameArray = new CharSequence[size];
        idArray = new int[size];
        int j = 0;
        for(int i = 0; i<menuSize; i++){
            Log.e(TAG, "loop " + i);

            int id = menu.getItem(i).getItemId();
            boolean excluded = false;

            if(exclusions != null)
            for(int excludedId : exclusions){
                if(id == excludedId){
                    excluded = true;
                    break;
                }
            }

            if(!excluded && id != R.id.nav_settings && id != R.id.nav_about)
            {
                nameArray[j] = menu.getItem(i).getTitle().toString();
                idArray[j] = menu.getItem(i).getItemId();
                j++;
            }
        }
        Log.e(TAG, "after loop");
        if(nameArray.length > 0)
            return new AlertDialog.Builder(context).setTitle(title)
                    .setItems(nameArray, action).create();
        else{
            showToast(context, context.getString(R.string.you_have_only_one_folder));
            return null;
        }
    }



    public static Dialog getAllFolderListDialog(final Context context, final String title,
                                             final DialogInterface.OnClickListener action){

        DatabaseHelper helper = new DatabaseHelper(context);
        ArrayList<Folder> folders = helper.getFoldersOnDemand();
        if (folders != null) {
            folders.remove(1); //Remove trash object
            if(folders.size() == 1){
                showToast(context, context.getString(R.string.you_have_only_one_folder));
                return null;
            }
            Utils.folders = folders;
            CharSequence[] folderNames = new CharSequence[folders.size()];
            for (int i = 0; i < folderNames.length; i++)
                folderNames[i] = folders.get(i).getName();
            return new AlertDialog.Builder(context).setTitle(title).
                    setItems(folderNames, action).create();
        } else {
            showToast(context, context.getString(R.string.unable_to_load_folders_list_try_again_later));
            return null;
        }
    }

    public static void removeAllMenuItems(Menu menu){
        Log.e(TAG, " size " + menu.size());
        while (menu.size()>0)
            menu.removeItem(menu.getItem(0).getItemId());
    }

    public static int getFolderId(int which){
        return (int) folders.get(which).getId();
    }

    public static int getFolderIdFromArray(int which){
        return idArray[which];
    }

    public static void updateAllWidgets(Context context){
        WidgetProvider widgetProvider = new WidgetProvider();
        ComponentName componentName = new ComponentName(context, WidgetProvider.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        widgetProvider.onUpdate(context, appWidgetManager, appWidgetManager.getAppWidgetIds(componentName));
    }

    public static void updateAllEdgePanels(Context context){
        EdgePanelProvider edgePanelProvider = new EdgePanelProvider();
        ComponentName componentName = new ComponentName(context, EdgePanelProvider.class);
        SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(context);
        edgePanelProvider.onUpdate(context, cocktailManager, cocktailManager.getCocktailIds(componentName));
    }

    public static String capitalizeFirstLetter(String text){
        if (text.length() <= 1)
            return text.toUpperCase();
        else
            return text.substring(0, 1).toUpperCase() + text.substring(1);

    }

    public static void hideShadowSinceLollipop(Context context){
        if(Build.VERSION.SDK_INT >= 21){
            ((Activity)context).findViewById(R.id.shadowImageView).setVisibility(View.GONE);
        }
    }
    public static void sendShareIntent(Context context, String text, String title) {
        if(text.length()!=0) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            intent.putExtra(Constants.TITLE_KEY, title);
            try {
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_via)));
            } catch (RuntimeException e){
                Log.e(TAG, "" + e);
                Log.e(TAG, "" + e.getCause());
                if(e.getCause() instanceof TransactionTooLargeException)
                    showToast(context, "The note is too big to share it");
            }
        } else
            Utils.showToast(context, context.getString(R.string.note_is_empty_or_was_not_loaded_yet));
    }

    public static long getMyNotesNavId(Context context){
        if(myNotesNavId > 0)
            return myNotesNavId;
        else {
            if(preferences == null)
                preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
            return (myNotesNavId = preferences.getLong(Constants.MY_NOTES_ID_KEY, 1));
        }
    }

    public static long getTrashNavId(Context context){
        if(trashNavId > 0)
            return trashNavId;
        else {
            if(preferences == null)
                preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
            return (trashNavId = preferences.getLong(Constants.TRASH_ID_KEY, 2));
        }
    }

    public static void incrementFolderCount(Menu m, int folderId, int inc){
        MenuItem menuItem = m.findItem(folderId);
        AppCompatTextView view = (AppCompatTextView) menuItem.getActionView();
        view.setText(Integer.toString(Integer.parseInt(view.getText().toString()) + inc));
    }

    public static void decrementFolderCount(Menu m, int folderId, int dec){
        MenuItem menuItem = m.findItem(folderId);
        AppCompatTextView view = (AppCompatTextView) menuItem.getActionView();
        view.setText(Integer.toString(Integer.parseInt(view.getText().toString()) - dec));
    }

    public static void setFolderCount(Menu m, int folderId, int count){
        MenuItem menuItem = m.findItem(folderId);
        AppCompatTextView view = (AppCompatTextView) menuItem.getActionView();
        view.setText(Integer.toString(count));
    }

    public static void updateConnectedWidgets(final Context context, long noteId){
        DatabaseHelper helper = new DatabaseHelper(context);
        helper.getWidgetsWithNote(noteId, new DatabaseHelper.OnWidgetsLoadListener() {
            @Override
            public void onWidgetsLoaded(ArrayList<Widget> widgets) {
                if(widgets != null){
                    int[] widgetIds = new int[widgets.size()];

                    for (int i = 0; i < widgets.size(); i++) {
                        widgetIds[i] = widgets.get(i).getWidgetId();
                    }

                    WidgetProvider widgetProvider = new WidgetProvider();
                    widgetProvider.onUpdate(context, AppWidgetManager.getInstance(context), widgetIds);
                }
            }
        });
    }

    public static int convertPxToDP(Context context, int px){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, context.getResources().getDisplayMetrics());
    }
}
