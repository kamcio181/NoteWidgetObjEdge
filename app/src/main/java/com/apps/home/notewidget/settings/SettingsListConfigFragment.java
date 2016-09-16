package com.apps.home.notewidget.settings;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatRadioButton;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.customviews.RobotoTextView;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.Utils;

public class SettingsListConfigFragment extends Fragment implements NumberPicker.OnValueChangeListener,
        RadioGroup.OnCheckedChangeListener{
    private SharedPreferences preferences;
    private Context context;

    private AppCompatRadioButton small, medium, big, veryBig, color, strikethorugh;
    private RadioGroup sizeGroup, styleGroup;
    private NumberPicker picker;
    private RobotoTextView example;
    private RelativeLayout tile;

    public SettingsListConfigFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();

        preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings_list_config, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((AppCompatActivity)context).getSupportActionBar().setTitle("Tile settings");

        small = (AppCompatRadioButton) view.findViewById(R.id.radioButton);
        medium = (AppCompatRadioButton) view.findViewById(R.id.radioButton2);
        big = (AppCompatRadioButton) view.findViewById(R.id.radioButton3);
        veryBig = (AppCompatRadioButton) view.findViewById(R.id.radioButton4);
        color = (AppCompatRadioButton) view.findViewById(R.id.radioButton5);
        strikethorugh = (AppCompatRadioButton) view.findViewById(R.id.radioButton6);
        sizeGroup = (RadioGroup) view.findViewById(R.id.radioGroup);
        styleGroup = (RadioGroup) view.findViewById(R.id.radioGroup2);
        picker = (NumberPicker) view.findViewById(R.id.numberPicker2);
        example = (RobotoTextView) view.findViewById(R.id.textView2);
        tile = (RelativeLayout) view.findViewById(R.id.relativeLayout);

        int size = preferences.getInt(Constants.LIST_TILE_SIZE_KEY, 56);
        int style = preferences.getInt(Constants.BOUGHT_ITEM_STYLE_KEY, Constants.COLOR);
        int textSize = preferences.getInt(Constants.LIST_TILE_TEXT_SIZE, 16);

        switch (size){
            case 48:
                small.setChecked(true);
                break;
            case 56:
                medium.setChecked(true);
                break;
            case 64:
                big.setChecked(true);
                break;
            case 72:
                veryBig.setChecked(true);
                break;
        }

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)tile.getLayoutParams();
        params.height = Utils.convertPxToDP(context, size);
        tile.setLayoutParams(params);

        switch (style){
            case Constants.COLOR:
                color.setChecked(true);
                example.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                break;
            case Constants.STRIKETHROUGH:
                strikethorugh.setChecked(true);
                example.setStrikeEnabled(true);
                break;
        }

        picker.setMinValue(1);
        picker.setMaxValue(30);
        picker.setValue(textSize);
        picker.setOnValueChangedListener(this);

        example.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);

        sizeGroup.setOnCheckedChangeListener(this);
        styleGroup.setOnCheckedChangeListener(this);
    }



    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (group.getId()){
            case R.id.radioGroup:
                int tileSize = 56;
                switch (checkedId){
                    case R.id.radioButton:
                        tileSize = 48;
                        break;
                    case R.id.radioButton3:
                        tileSize = 64;
                        break;
                    case R.id.radioButton4:
                        tileSize = 72;
                        break;
                }
                preferences.edit().putInt(Constants.LIST_TILE_SIZE_KEY, tileSize).apply();
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)tile.getLayoutParams();
                params.height = Utils.convertPxToDP(context, tileSize);
                tile.setLayoutParams(params);
                break;
            case R.id.radioGroup2:
                int style = Constants.COLOR;
                switch (checkedId){
                    case R.id.radioButton5:
                        example.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                        example.setStrikeEnabled(false);
                        break;
                    case R.id.radioButton6:
                        style = Constants.STRIKETHROUGH;
                        example.setStrikeEnabled(true);
                        example.setTextColor(Color.BLACK);
                        break;
                }
                preferences.edit().putInt(Constants.BOUGHT_ITEM_STYLE_KEY, style).apply();
                break;
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        context.sendBroadcast(new Intent(Constants.ACTION_UPDATE_NOTE_PARAMETERS));
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        example.setTextSize(TypedValue.COMPLEX_UNIT_SP, newVal);
        preferences.edit().putInt(Constants.LIST_TILE_TEXT_SIZE, newVal).apply();
    }
}
