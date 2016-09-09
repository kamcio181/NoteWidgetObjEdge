package com.apps.home.notewidget.settings;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioGroup;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.Utils;

public class SettingsWidgetConfigFragment extends Fragment implements CompoundButton.OnCheckedChangeListener,
        RadioGroup.OnCheckedChangeListener{
    private SharedPreferences preferences;
    private Context context;
    private SwitchCompat ignoreTabsSwitch;
    private AppCompatRadioButton miuiThemeRadioButton, materialThemeRadioButton, simpleThemeRadioButton;
    private RadioGroup widgetThemeRadioGroup;

    public SettingsWidgetConfigFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();

        preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings_widget_config, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((AppCompatActivity)context).getSupportActionBar().setTitle(R.string.widget_configuration);

        ignoreTabsSwitch = (SwitchCompat) view.findViewById(R.id.switch1);
        miuiThemeRadioButton = (AppCompatRadioButton) view.findViewById(R.id.radioButton);
        materialThemeRadioButton = (AppCompatRadioButton) view.findViewById(R.id.radioButton2);
        simpleThemeRadioButton = (AppCompatRadioButton) view.findViewById(R.id.radioButton3);
        widgetThemeRadioGroup = (RadioGroup) view.findViewById(R.id.radioGroup);

        ignoreTabsSwitch.setChecked(preferences.getBoolean(Constants.IGNORE_TABS_IN_WIDGETS_KEY, false));

        switch (preferences.getInt(Constants.WIDGET_THEME_KEY, Constants.WIDGET_THEME_MIUI)){
            case Constants.WIDGET_THEME_MIUI:
                miuiThemeRadioButton.setChecked(true);
                break;
            case Constants.WIDGET_THEME_MATERIAL:
                materialThemeRadioButton.setChecked(true);
                break;
            case Constants.WIDGET_THEME_SIMPLE:
                simpleThemeRadioButton.setChecked(true);
                break;
        }

        ignoreTabsSwitch.setOnCheckedChangeListener(this);
        widgetThemeRadioGroup.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()){
            case R.id.switch1:
                preferences.edit().putBoolean(Constants.IGNORE_TABS_IN_WIDGETS_KEY, isChecked).apply();
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (group.getId()){
            case R.id.radioGroup:
                switch (checkedId){
                    case R.id.radioButton:
                        preferences.edit().putInt(Constants.WIDGET_THEME_KEY, Constants.WIDGET_THEME_MIUI).apply();
                        break;
                    case R.id.radioButton2:
                        preferences.edit().putInt(Constants.WIDGET_THEME_KEY, Constants.WIDGET_THEME_MATERIAL).apply();
                        break;
                    case R.id.radioButton3:
                        preferences.edit().putInt(Constants.WIDGET_THEME_KEY, Constants.WIDGET_THEME_SIMPLE).apply();
                        break;
                }
                break;
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        Utils.updateAllWidgets(context);
    }
}
