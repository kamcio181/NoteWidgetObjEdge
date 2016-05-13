package com.apps.home.notewidget.widget;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.apps.home.notewidget.R;
import com.apps.home.notewidget.utils.Constants;
import com.apps.home.notewidget.utils.Utils;

public class WidgetManualActivity extends AppCompatActivity {
    private RelativeLayout modeClickDesc, titleClickDesc, noteClickDesc;
    private LinearLayout titleWidget;
    private FrameLayout widgetTitleCont;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_manual);

        //titleWidget = (LinearLayout) findViewById(R.id.widgetTitleLayout);
        modeClickDesc = (RelativeLayout) findViewById(R.id.modeClickDesc);
        titleClickDesc = (RelativeLayout) findViewById(R.id.titleClickDesc);
        noteClickDesc = (RelativeLayout) findViewById(R.id.noteClickDesc);
        widgetTitleCont = (FrameLayout) findViewById(R.id.widgetTitleCont);
        TextView modeDesc = (TextView) modeClickDesc.findViewById(R.id.textView6);
        TextView titleDesc = (TextView) titleClickDesc.findViewById(R.id.textView6);
        TextView noteDesc = (TextView) noteClickDesc.findViewById(R.id.textView6);
        widgetTitleCont.addView(LayoutInflater.from(this).inflate(
                Utils.getLayoutFile(this, getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).
                        getInt(Constants.WIDGET_THEME_KEY, Constants.WIDGET_THEME_MIUI),
                        Constants.WIDGET_MODE_TITLE),
                widgetTitleCont, false));

        TextView title = (TextView) widgetTitleCont.findViewById(R.id.titleTextView);
        title.setText("Example note title");
        modeDesc.setText("Change widget mode");
        titleDesc.setText("Open application");
        noteDesc.setText("Edit note");

        ListView noteListView = (ListView) widgetTitleCont.findViewById(R.id.noteListView);
        noteListView.setAdapter(new ArrayAdapter<>(this, R.layout.note_text_light, R.id.noteTextView,
                new String[]{"Example note text\nExample note text\nExample note text\n" +
                        "Example note text\nExample note text"}));
    }
}
