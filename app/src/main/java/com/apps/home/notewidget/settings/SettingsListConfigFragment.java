package com.apps.home.notewidget.settings;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatRadioButton;
import android.util.Log;
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
    private static final String TAG = "SettingsListConfigFr";
    private SharedPreferences preferences;
    private Context context;
    private AppCompatRadioButton small, medium, big, veryBig, color, strikethrough, moveToTop, moveToBottom;
    private RadioGroup sizeGroup, styleGroup, behaviorGroup;
    private NumberPicker picker;
    private TextInputEditText itemLengthEditText;
    private RobotoTextView example;
    private RelativeLayout tile;
    private int tileSize, style, textSize, newlyBoughtItemBehavior, itemLength;

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

        ((AppCompatActivity)context).getSupportActionBar().setTitle(R.string.list_configuration);

        small = (AppCompatRadioButton) view.findViewById(R.id.radioButton);
        medium = (AppCompatRadioButton) view.findViewById(R.id.radioButton2);
        big = (AppCompatRadioButton) view.findViewById(R.id.radioButton3);
        veryBig = (AppCompatRadioButton) view.findViewById(R.id.radioButton4);
        color = (AppCompatRadioButton) view.findViewById(R.id.radioButton5);
        strikethrough = (AppCompatRadioButton) view.findViewById(R.id.radioButton6);
        moveToTop = (AppCompatRadioButton) view.findViewById(R.id.radioButton7);
        moveToBottom = (AppCompatRadioButton) view.findViewById(R.id.radioButton8);
        sizeGroup = (RadioGroup) view.findViewById(R.id.radioGroup);
        styleGroup = (RadioGroup) view.findViewById(R.id.radioGroup2);
        behaviorGroup = (RadioGroup) view.findViewById(R.id.radioGroup3);
        picker = (NumberPicker) view.findViewById(R.id.numberPicker2);
        example = (RobotoTextView) view.findViewById(R.id.textView2);
        tile = (RelativeLayout) view.findViewById(R.id.relativeLayout);
        itemLengthEditText = (TextInputEditText) view.findViewById(R.id.lengthEditText);

        tileSize = preferences.getInt(Constants.LIST_TILE_SIZE_KEY, Constants.DEFAULT_LIST_TILE_SIZE);
        style = preferences.getInt(Constants.BOUGHT_ITEM_STYLE_KEY, Constants.COLOR);
        textSize = preferences.getInt(Constants.LIST_TILE_TEXT_SIZE, Constants.DEFAULT_LIST_TILE_TEXT_SIZE);
        newlyBoughtItemBehavior = preferences.getInt(Constants.NEWLY_BOUGHT_ITEM_BEHAVIOR, Constants.MOVE_TO_BOTTOM);
        itemLength = preferences.getInt(Constants.LIST_ITEM_LENGTH, Constants.DEFAULT_LIST_ITEM_LENGTH);

        switch (tileSize){
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
        params.height = Utils.convertPxToDP(context, tileSize);
        tile.setLayoutParams(params);

        switch (style){
            case Constants.COLOR:
                color.setChecked(true);
                example.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                break;
            case Constants.STRIKETHROUGH:
                strikethrough.setChecked(true);
                example.setStrikeEnabled(true);
                break;
        }

        picker.setMinValue(1);
        picker.setMaxValue(30);
        picker.setValue(textSize);
        picker.setOnValueChangedListener(this);

        example.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);

        switch (newlyBoughtItemBehavior){
            case Constants.MOVE_TO_TOP:
                moveToTop.setChecked(true);
                break;
            case Constants.MOVE_TO_BOTTOM:
                moveToBottom.setChecked(true);
                break;
        }

        itemLengthEditText.setText(String.valueOf(itemLength));

        sizeGroup.setOnCheckedChangeListener(this);
        styleGroup.setOnCheckedChangeListener(this);
        behaviorGroup.setOnCheckedChangeListener(this);
    }



    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (group.getId()){
            case R.id.radioGroup:
                switch (checkedId){
                    case R.id.radioButton:
                        tileSize = 48;
                        break;
                    case R.id.radioButton2:
                        tileSize = 56;
                        break;
                    case R.id.radioButton3:
                        tileSize = 64;
                        break;
                    case R.id.radioButton4:
                        tileSize = 72;
                        break;
                }

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)tile.getLayoutParams();
                params.height = Utils.convertPxToDP(context, tileSize);
                tile.setLayoutParams(params);
                break;
            case R.id.radioGroup2:
                switch (checkedId){
                    case R.id.radioButton5:
                        style = Constants.COLOR;
                        example.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                        example.setStrikeEnabled(false);
                        break;
                    case R.id.radioButton6:
                        style = Constants.STRIKETHROUGH;
                        example.setStrikeEnabled(true);
                        example.setTextColor(Color.BLACK);
                        break;
                }
                break;
            case R.id.radioGroup3:
                switch (checkedId){
                    case R.id.radioButton7:
                        newlyBoughtItemBehavior = Constants.MOVE_TO_TOP;
                        break;
                    case R.id.radioButton8:
                        newlyBoughtItemBehavior = Constants.MOVE_TO_BOTTOM;
                        break;
                }
                break;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            itemLength = Integer.parseInt(itemLengthEditText.getText().toString().trim());
        } catch (NumberFormatException e){
            Log.e(TAG, "Wrong item length value, using the current one");
        }

        preferences.edit().putInt(Constants.LIST_TILE_SIZE_KEY, tileSize).
                putInt(Constants.BOUGHT_ITEM_STYLE_KEY, style).
                putInt(Constants.LIST_TILE_TEXT_SIZE, textSize).
                putInt(Constants.NEWLY_BOUGHT_ITEM_BEHAVIOR, newlyBoughtItemBehavior).
                putInt(Constants.LIST_ITEM_LENGTH, itemLength).apply();
        context.sendBroadcast(new Intent(Constants.ACTION_UPDATE_NOTE_PARAMETERS));
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        textSize = newVal;
        example.setTextSize(TypedValue.COMPLEX_UNIT_SP, newVal);
    }
}
