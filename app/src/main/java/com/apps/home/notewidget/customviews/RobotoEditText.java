package com.apps.home.notewidget.customviews;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by kamil on 06.02.16.
 */
public class RobotoEditText extends EditText{


    public RobotoEditText(Context context) {
        super(context);
        createFont();
    }

    public RobotoEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        createFont();
    }

    public RobotoEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        createFont();
    }

    private void createFont() {
        Typeface font = Typeface.createFromAsset(getContext().getAssets(), "Roboto-Regular.ttf");
        setTypeface(font);
    }
}