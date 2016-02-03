package com.apps.home.notewidget;

/**
 * Created by k.kaszubski on 1/20/16.
 */
public interface Constants {
    String NOTES_TABLE = "NOTES";
    String ID_COL = "_id";
    String MILLIS_COL = "millis";
    String NOTE_TITLE_COL = "noteTitle";
    String NOTE_TEXT_COL = "noteText";

    String WIDGETS_TABLE = "WIDGETS";
    String WIDGET_ID = "widgetId";
    String CONNECTED_NOTE_ID = "noteID";
    String CURRENT_MODE = "mode";
    String CURRENT_THEME = "theme";
    String CURRENT_TEXT_SIZE = "textSize";

    String PREFS_NAME = "prefs";
    String CONFIGURED_KEY = " configured";

    int WIDGET_TITLE_MODE = R.layout.appwidget_title_lollipop;
    int WIDGET_CONFIG_MODE = R.layout.appwidget_config_lollipop;
   /* int WIDGET_TITLE_MODE = R.layout.appwidget_title_miui;
    int WIDGET_CONFIG_MODE = R.layout.appwidget_config_miui;*/

    int WIDGET_THEME_LIGHT = 0;
    int WIDGET_THEME_DARK = 1;

}
