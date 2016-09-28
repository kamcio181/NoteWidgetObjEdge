package com.apps.home.notewidget.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.Utils;

public class WidgetManualActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = getLayoutInflater();
        @SuppressLint("InflateParams") View layout = inflater.inflate(R.layout.activity_widget_manual, null);


        final RelativeLayout modeClickDesc = (RelativeLayout) layout.findViewById(R.id.modeClickDesc);
        final RelativeLayout titleClickDesc = (RelativeLayout) layout.findViewById(R.id.titleClickDesc);
        final RelativeLayout noteClickDesc = (RelativeLayout) layout.findViewById(R.id.noteClickDesc);
        final FrameLayout widgetTitleCont = (FrameLayout) layout.findViewById(R.id.widgetTitleCont);
        final FrameLayout widgetTitleCont2 = (FrameLayout) layout.findViewById(R.id.widgetTitleCont2);
        final RelativeLayout modeClickDesc2 = (RelativeLayout) layout.findViewById(R.id.modeClickDesc2);
        final RelativeLayout incClickDesc = (RelativeLayout) layout.findViewById(R.id.incClickDesc);
        final RelativeLayout decClickDesc = (RelativeLayout) layout.findViewById(R.id.decClickDesc);
        final RelativeLayout themeSwitchDesc = (RelativeLayout) layout.findViewById(R.id.themeSwitchClickDesc);
        final FrameLayout widgetConfigCont = (FrameLayout) layout.findViewById(R.id.widgetConfigCont);
        final FrameLayout widgetConfigCont2 = (FrameLayout) layout.findViewById(R.id.widgetConfigCont2);
        final AppCompatCheckBox checkBox = (AppCompatCheckBox) layout.findViewById(R.id.checkBox);

        widgetTitleCont2.addView(LayoutInflater.from(this).inflate(
                Utils.getLayoutFile(this, Constants.WIDGET_THEME_LIGHT,
                        Constants.WIDGET_MODE_TITLE),
                widgetTitleCont, false));
        widgetConfigCont2.addView(LayoutInflater.from(this).inflate(
                Utils.getLayoutFile(this, Constants.WIDGET_THEME_LIGHT,
                        Constants.WIDGET_MODE_CONFIG),
                widgetConfigCont, false));

        final TextView modeDesc = (TextView) modeClickDesc.findViewById(R.id.textView6);
        final TextView titleDesc = (TextView) titleClickDesc.findViewById(R.id.textView6);
        final TextView noteDesc = (TextView) noteClickDesc.findViewById(R.id.textView6);
        final View noteDescVerticalLine = noteClickDesc.findViewById(R.id.verticalLine);
        final View noteDescVerticalLine2 = noteClickDesc.findViewById(R.id.verticalLine2);
        final TextView modeDesc2 = (TextView) modeClickDesc2.findViewById(R.id.textView6);
        final TextView incDesc = (TextView) incClickDesc.findViewById(R.id.textView6);
        final TextView decDesc = (TextView) decClickDesc.findViewById(R.id.textView6);
        final TextView themeDesc = (TextView) themeSwitchDesc.findViewById(R.id.textView6);

        final TextView title = (TextView) widgetTitleCont.findViewById(R.id.titleTextView);
        title.setText(R.string.example_note_title);
        modeDesc.setText(R.string.change_widget_mode);
        titleDesc.setText(R.string.open_menu);
        noteDesc.setText(R.string.edit_note);
        modeDesc2.setText(R.string.change_widget_mode);
        incDesc.setText(R.string.increase_text_size);
        decDesc.setText(R.string.decrease_text_size);
        themeDesc.setText(R.string.light_dark_theme);
        noteDescVerticalLine.setVisibility(View.GONE);
        noteDescVerticalLine2.setVisibility(View.GONE);

        final ListView noteListView = (ListView) widgetTitleCont.findViewById(R.id.noteListView);
        noteListView.setAdapter(new ArrayAdapter<>(this, R.layout.note_text_light, R.id.noteTextView,
                new String[]{getString(R.string.example_note_text)}));

        final ListView noteListView2 = (ListView) widgetConfigCont.findViewById(R.id.noteListView);
        noteListView2.setAdapter(new ArrayAdapter<>(this, R.layout.note_text_light, R.id.noteTextView,
                new String[]{"\n\n\n\n"}));

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
