package com.apps.home.notewidget.utils;

/**
 * Created by k.kaszubski on 1/20/16.
 */
public interface Constants {
    String NOTES_TABLE = "NOTES";
    String ID_COL = "_id";
    String MILLIS_COL = "millis";
    String NOTE_TITLE_COL = "noteTitle";
    String NOTE_TEXT_COL = "noteText";
    String FOLDER_ID_COL = "folderId";
    String DELETED_COL = "deleted";
    String ENCRYPTED_COL = "encrypted";

    String WIDGETS_TABLE = "WIDGETS";
    String WIDGET_ID_COL = "widgetId";
    String CONNECTED_NOTE_ID_COL = "noteId";
    String CURRENT_WIDGET_MODE_COL = "mode";
    String CURRENT_THEME_MODE_COL = "themeMode";
    String CURRENT_TEXT_SIZE_COL = "textSize";

    String FOLDER_TABLE = "FOLDERS";
    String FOLDER_NAME_COL = "folderName";
    String FOLDER_ICON_COL = "folderIcon";
    String NOTES_COUNT_COL = "notesCount";

    String PREFS_NAME = "prefs";
    String CONFIGURED_KEY = "configured";
    String SORT_BY_DATE_KEY = "sortByDate";
    String CURRENT_WIDGET_THEME_KEY = "currentWidgetTheme";
    String MY_NOTES_ID_KEY = "myNotesId";
    String TRASH_ID_KEY = "trashId";
    String TITLE_KEY = "title";
    String SEARCH_IN_TITLE = "titleSearch";
    String SEARCH_IN_CONTENT = "contentSearch";


    String FRAGMENT_LIST = "ListFragment";
    String FRAGMENT_NOTE = "NoteFragment";
    String FRAGMENT_TRASH_NOTE = "TrashNoteFragment";
    String FRAGMENT_SEARCH = "SearchFragment";

    int WIDGET_MODE_TITLE = 0;
    int WIDGET_MODE_CONFIG = 1;

    int WIDGET_THEME_LOLLIPOP = 0;
    int WIDGET_THEME_MIUI = 1;
    int WIDGET_THEME_SIMPLE = 2;

    int WIDGET_THEME_LIGHT = 0;
    int WIDGET_THEME_DARK = 1;

}