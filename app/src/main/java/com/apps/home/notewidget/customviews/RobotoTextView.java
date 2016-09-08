package com.apps.home.notewidget.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.TextView;

import com.apps.home.notewidget.R;

public class RobotoTextView extends TextView{
    private Paint paint;
    private boolean strikeEnabled = false;
    private static final int OFFSET = 3;

    public RobotoTextView(Context context) {
        super(context);
        createFont();
        init(context);
    }

    public RobotoTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        createFont();
        init(context);
    }

    public RobotoTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        createFont();
        init(context);
    }

    private void createFont() {
        Typeface font = Typeface.createFromAsset(getContext().getAssets(), "Roboto-Regular.ttf");
        setTypeface(font);
    }

    private void init(Context context) {
        paint = new Paint();
        paint.setColor(ContextCompat.getColor(context, R.color.colorAccent));
        paint.setStrokeWidth(getResources().getDisplayMetrics().density*2);

    }

    public void setStrikeEnabled(boolean strikeEnabled) {
        this.strikeEnabled = strikeEnabled;
    }

    private float spaceOffset(String string){
        if(string.equals(" "))
            return -getPaint().measureText(" ");
        return 0;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);
        if (strikeEnabled) {
            canvas.drawLine(getPaddingLeft() - OFFSET, getHeight() / 2,
                    getPaint().measureText(getText().subSequence(getLayout().getLineStart(0),
                            getLayout().getLineEnd(0)).toString()) + getPaddingLeft() + OFFSET +
                            spaceOffset(getText().subSequence(getLayout().getLineEnd(0)-1,
                                    getLayout().getLineEnd(0)).toString()), getHeight() / 2, paint);

//            switch (getContext().getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE).
//                    getInt(Constants.TILE_SIZE_KEY,Constants.TILE_MEDIUM)){
//                case Constants.TILE_SMALL:
//                    canvas.drawLine(getPaddingLeft() - OFFSET, getHeight() / 2,
//                            getPaint().measureText(getText().subSequence(getLayout().getLineStart(0),
//                                    getLayout().getLineEnd(0)).toString()) + getPaddingLeft() + OFFSET +
//                                    spaceOffset(getText().subSequence(getLayout().getLineEnd(0)-1,
//                                            getLayout().getLineEnd(0)).toString()), getHeight() / 2, paint);
//                    break;
//                case Constants.TILE_MEDIUM:
//                    switch (getLineCount()){
//                        case 1:
//                            canvas.drawLine(getPaddingLeft() - OFFSET, getHeight() / 2, getPaint().measureText(getText().toString()) + getPaddingLeft() + OFFSET, getHeight() / 2, paint);
//                            break;
//                        case 2:
//                        default:
//                            for(int i = 1; i <= 2; i++)
//                                canvas.drawLine(getPaddingLeft() - OFFSET, i*getLineHeight(), getPaint().measureText(getText().subSequence(getLayout().getLineStart(i-1),getLayout().getLineEnd(i-1)).toString()) + getPaddingLeft() + OFFSET + spaceOffset(getText().subSequence(getLayout().getLineEnd(i-1)-1,
//                                        getLayout().getLineEnd(i-1)).toString()), i*getLineHeight(), paint);
//                            Log.v("custom","a"+spaceOffset(getText().subSequence(getLayout().getLineEnd(0)-1,
//                                    getLayout().getLineEnd(0)).toString())+"a");
//                            break;
//                    }
//                    break;
//                case Constants.TILE_BIG:
//                    switch (getLineCount()){
//                        case 1:
//                            canvas.drawLine(getPaddingLeft() - OFFSET, getHeight() / 2, getPaint().measureText(getText().toString()) + getPaddingLeft() + OFFSET, getHeight() / 2, paint);
//                            break;
//                        case 2:
//                            for(int i = 1; i <= 2; i++)
//                                canvas.drawLine(getPaddingLeft() - OFFSET, i*getLineHeight() + getLineHeight()/2, getPaint().measureText(getText().subSequence(getLayout().getLineStart(i-1), getLayout().getLineEnd(i-1)).toString()) + getPaddingLeft() + OFFSET + spaceOffset(getText().subSequence(getLayout().getLineEnd(i-1)-1,
//                                        getLayout().getLineEnd(i-1)).toString()), i*getLineHeight() + getLineHeight()/2, paint);
//                            break;
//                        case 3:
//                        default:
//                            for(int i = 1; i <= 3; i++)
//                                canvas.drawLine(getPaddingLeft() - OFFSET, i*getLineHeight(), getPaint().measureText(getText().subSequence(getLayout().getLineStart(i - 1), getLayout().getLineEnd(i - 1)).toString()) + getPaddingLeft() + OFFSET + spaceOffset(getText().subSequence(getLayout().getLineEnd(i-1)-1,
//                                        getLayout().getLineEnd(i-1)).toString()), i*getLineHeight(), paint);
//                            break;
//                    }
//                    break;
//            }
        }
    }
}