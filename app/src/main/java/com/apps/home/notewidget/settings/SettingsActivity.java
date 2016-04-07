package com.apps.home.notewidget.settings;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.apps.home.notewidget.NoteFragment;
import com.apps.home.notewidget.R;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.Utils;

public class SettingsActivity extends AppCompatActivity implements SettingsListFragment.OnItemClickListener { //TODO use empty layout as container for fragments
    private Context context;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        context = this;
        fragmentManager = getSupportFragmentManager();

        Utils.hideShadowSinceLollipop(this);

        if(getSupportActionBar()!=null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        fragmentManager.beginTransaction().replace(R.id.container,
                new SettingsListFragment(), Constants.FRAGMENT_NOTE).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClicked(int position) {

    }
}
