package com.apps.home.notewidget.calendar;

import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.TitleChangeListener;
import com.apps.home.notewidget.utils.Utils;

public class CalendarActivity extends AppCompatActivity implements View.OnClickListener, CalendarFragment.OnEventClickListener {
    private static final String TAG = CalendarActivity.class.getSimpleName();
    private static final int FRAGMENT_CONTAINER_ID = R.id.container;
    private FragmentManager fragmentManager;
    private FloatingActionButton fab;
    private Toolbar toolbar;
    private ActionBar actionBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        fragmentManager = getSupportFragmentManager();

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

        Utils.hideShadowSinceLollipop(this);

        attachFragment(Constants.FRAGMENT_CALENDAR);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.fab:
                attachFragment(Constants.FRAGMENT_CALENDAR_EVENT_EDIT);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if(fragmentManager.findFragmentById(FRAGMENT_CONTAINER_ID) != null) {
            switch (fragmentManager.findFragmentById(FRAGMENT_CONTAINER_ID).getTag()) {
                case Constants.FRAGMENT_CALENDAR:
                    getMenuInflater().inflate(R.menu.menu_empty, menu);
                    break;
                case Constants.FRAGMENT_CALENDAR_EVENT_EDIT:
                    getMenuInflater().inflate(R.menu.menu_calendar_event, menu);
                    break;
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        switch (fragmentManager.findFragmentById(FRAGMENT_CONTAINER_ID).getTag()) {
            case Constants.FRAGMENT_CALENDAR_EVENT_EDIT:
                attachFragment(Constants.FRAGMENT_CALENDAR);
                break;
            default:
                super.onBackPressed();
                break;
        }
    }

    private void attachFragment(String fragment){
        attachFragment(fragment, -1);
    }

    private void attachFragment (String fragment, long id){
        Log.d(TAG, fragment);
        Fragment fragmentToAttach = null;
        switch (fragment){
            case Constants.FRAGMENT_CALENDAR:
                fab.show();
                setOnTitleClickListener(false);
                fragmentToAttach = new CalendarFragment();
                actionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.colorPrimary)));
                break;
            case Constants.FRAGMENT_CALENDAR_EVENT_EDIT:
                fab.hide();
                setOnTitleClickListener(true);
                if(id == -1)
                    fragmentToAttach = new CalendarEventEditFragment();
                else
                    fragmentToAttach = CalendarEventEditFragment.newInstance(id);
                break;
        }
        fragmentManager.beginTransaction().replace(FRAGMENT_CONTAINER_ID, fragmentToAttach, fragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commitAllowingStateLoss();
        Log.d(TAG, "attached fragment");
    }

    public void setOnTitleClickListener(boolean enable){
        if(enable)
            toolbar.setOnClickListener(noteTitleChangeOrFolderNameListener());
        else
            toolbar.setOnClickListener(null);
    }

    private View.OnClickListener noteTitleChangeOrFolderNameListener(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.getNameDialog(CalendarActivity.this, actionBar.getTitle().toString(),
                        "Set event name",
                        50, "Event name",
                        new Utils.OnNameSet() {
                            @Override
                            public void onNameSet(String name) {
                                setNewTitle(name);
                            }
                        }).show();
            }
        };
    }

    private void setNewTitle(String title){
        title = setTitle(title);

        Fragment fragment = fragmentManager.findFragmentById(FRAGMENT_CONTAINER_ID);
        String fragmentTag = fragment.getTag();
        if(fragmentTag.equals(Constants.FRAGMENT_CALENDAR_EVENT_EDIT))
            ((TitleChangeListener)fragment).onTitleChanged(title);
    }

    private String setTitle(String title){
        if(title.equals(""))
            title = getString(R.string.untitled);
        else
            title = Utils.capitalizeFirstLetter(title);
        actionBar.setTitle(title);
        Log.d(TAG, "setToolbarTitle " + title);

        return title;
    }

    @Override
    public void onEventClicked(long id) {
        attachFragment(Constants.FRAGMENT_CALENDAR_EVENT_EDIT, id);
    }
}
