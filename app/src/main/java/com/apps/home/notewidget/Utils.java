package com.apps.home.notewidget;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by Kamil on 2016-01-30.
 */
public class Utils {
    private static Toast toast;

    public static void showToast(Context context, String message){
        if(toast!=null)
            toast.cancel();
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }
}
