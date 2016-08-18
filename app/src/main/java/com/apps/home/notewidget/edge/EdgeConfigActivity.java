package com.apps.home.notewidget.edge;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.Utils;

public class EdgeConfigActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener{
    private static final String TAG = "EdgeConfigActivity";
    private static SharedPreferences preferences;
    private RecyclerView notesRV, edgeRV;
    private SwitchCompat ignoreTabsSwitch;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edge_config);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        preferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        ignoreTabsSwitch = (SwitchCompat) findViewById(R.id.switch1);
        notesRV = (RecyclerView) findViewById(R.id.recycler_view1);
        edgeRV = (RecyclerView) findViewById(R.id.recycler_view2);

        ignoreTabsSwitch.setOnCheckedChangeListener(this);
        ignoreTabsSwitch.setChecked(preferences.getBoolean(Constants.IGNORE_TABS_IN_WIDGETS_KEY, false));

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()){
            case R.id.switch1:
                preferences.edit().putBoolean(Constants.IGNORE_TABS_IN_EDGE_PANEL_KEY, isChecked).apply();
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        Utils.updateAllEdgePanels(this);
    }
}
