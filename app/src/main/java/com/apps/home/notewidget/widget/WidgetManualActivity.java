package com.apps.home.notewidget.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.Utils;

public class WidgetManualActivity extends AppCompatActivity{
    private RelativeLayout modeClickDesc, titleClickDesc, noteClickDesc, modeClickDesc2,
    incClickDesc, decClickDesc, themeSwitchDesc;
    private FrameLayout widgetTitleCont, widgetConfigCont;
    private Button button;
    private CheckBox checkBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_widget_manual);
        //setTitle(getString(R.string.tip));
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.activity_widget_manual, null);


        modeClickDesc = (RelativeLayout) layout.findViewById(R.id.modeClickDesc);
        titleClickDesc = (RelativeLayout) layout.findViewById(R.id.titleClickDesc);
        noteClickDesc = (RelativeLayout) layout.findViewById(R.id.noteClickDesc);
        widgetTitleCont = (FrameLayout) layout.findViewById(R.id.widgetTitleCont);
        modeClickDesc2 = (RelativeLayout) layout.findViewById(R.id.modeClickDesc2);
        incClickDesc = (RelativeLayout) layout.findViewById(R.id.incClickDesc);
        decClickDesc = (RelativeLayout) layout.findViewById(R.id.decClickDesc);
        themeSwitchDesc = (RelativeLayout) layout.findViewById(R.id.themeSwitchClickDesc);
        widgetConfigCont = (FrameLayout) layout.findViewById(R.id.widgetConfigCont);
        checkBox = (CheckBox) layout.findViewById(R.id.checkBox);

        widgetTitleCont.addView(LayoutInflater.from(this).inflate(
                Utils.getLayoutFile(this, getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).
                        getInt(Constants.WIDGET_THEME_KEY, Constants.WIDGET_THEME_MIUI),
                        Constants.WIDGET_MODE_TITLE),
                widgetTitleCont, false));
        widgetConfigCont.addView(LayoutInflater.from(this).inflate(
                Utils.getLayoutFile(this, getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).
                                getInt(Constants.WIDGET_THEME_KEY, Constants.WIDGET_THEME_MIUI),
                        Constants.WIDGET_MODE_CONFIG),
                widgetConfigCont, false));

        TextView modeDesc = (TextView) modeClickDesc.findViewById(R.id.textView6);
        TextView titleDesc = (TextView) titleClickDesc.findViewById(R.id.textView6);
        TextView noteDesc = (TextView) noteClickDesc.findViewById(R.id.textView6);
        TextView modeDesc2 = (TextView) modeClickDesc2.findViewById(R.id.textView6);
        TextView incDesc = (TextView) incClickDesc.findViewById(R.id.textView6);
        TextView decDesc = (TextView) decClickDesc.findViewById(R.id.textView6);
        TextView themeDesc = (TextView) themeSwitchDesc.findViewById(R.id.textView6);

        TextView title = (TextView) widgetTitleCont.findViewById(R.id.titleTextView);
        title.setText("Example note title");
        modeDesc.setText("Change widget mode");
        titleDesc.setText("Open application");
        noteDesc.setText("Edit note");
        modeDesc2.setText("Change widget mode");
        incDesc.setText("Increase text size");
        decDesc.setText("Decrease text size");
        themeDesc.setText("Light/dark theme");


        ListView noteListView = (ListView) widgetTitleCont.findViewById(R.id.noteListView);
        noteListView.setAdapter(new ArrayAdapter<>(this, R.layout.note_text_light, R.id.noteTextView,
                new String[]{"\n\n\n" +
                        "Example note text\nExample note text"}));

        ListView noteListView2 = (ListView) widgetConfigCont.findViewById(R.id.noteListView);
        noteListView2.setAdapter(new ArrayAdapter<>(this, R.layout.note_text_light, R.id.noteTextView,
                new String[]{"\n\n\n\n\n\n\n"}));

        new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme_AlertDialog)).setTitle(this.getString(R.string.tip)).setView(layout).setCancelable(false).
                setPositiveButton(this.getString(R.string.i_have_got_it), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (checkBox.isChecked()) {
                        SharedPreferences preferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
                        preferences.edit().putBoolean(Constants.SKIP_WIDGET_MANUAL_DIALOG_KEY, true).apply();
                        }
                        finish();
                    }
                }).create().show();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }
}
