package com.apps.home.notewidget.utils;

public interface Constants {
    String DB_NAME = "database";
    String NOTES_TABLE = "NOTES";
    String ID_COL = "_id";
    String MILLIS_COL = "millis";
    String NOTE_TITLE_COL = "noteTitle";
    String NOTE_TEXT_COL = "noteText";
    String FOLDER_ID_COL = "folderId";
    String DELETED_COL = "deleted";
    String TYPE_COL = "type";

    String WIDGETS_TABLE = "WIDGETS";
    String WIDGET_ID_COL = "widgetId";
    String CONNECTED_NOTE_ID_COL = "noteId";
    String CURRENT_WIDGET_MODE_COL = "mode";
    String CURRENT_THEME_MODE_COL = "themeMode";
    String CURRENT_TEXT_SIZE_COL = "textSize";

    String FOLDER_TABLE = "FOLDERS";
    String FOLDER_NAME_COL = "folderName";
    String NOTES_COUNT_COL = "notesCount";

    String PREFS_NAME = "prefs";
    String CONFIGURED_KEY = "configured";
    String SORT_BY_DATE_KEY = "sortByDate";
    String WIDGET_THEME_KEY = "widgetTheme";
    String MY_NOTES_ID_KEY = "myNotesId";
    String TRASH_ID_KEY = "trashId";
    String TITLE_KEY = "title";
    String SEARCH_IN_TITLE = "titleSearch";
    String SEARCH_IN_CONTENT = "contentSearch";
    String IGNORE_TABS_IN_WIDGETS_KEY = "ignoreTabsInWidget";
    String NOTE_TEXT_SIZE_KEY = "noteTextSize";
    String LIST_TILE_SIZE_KEY = "listTileSize";
    String BOUGHT_ITEM_STYLE_KEY = "boughtItemStyle";
    String LIST_TILE_TEXT_SIZE = "listTileTextSize";
    String STARTING_FOLDER_KEY = "startingFolder";
    String SKIP_MULTILEVEL_NOTE_MANUAL_DIALOG_KEY = "skipMultilevelNoteManualDialog";
    String SKIP_WIDGET_MANUAL_DIALOG_KEY = "skipWidgetManualDialog";
    String RELOAD_MAIN_ACTIVITY_AFTER_RESTORE_KEY = "reloadMainActivityAfterRestore";
    String RECONFIGURE = "reconfigure";

    String NOTE_UPDATED_FROM_WIDGET = "noteUpdatedFromWidget";
    String NOTE_PARAMETERS_UPDATED = "noteTextSizeUpdated";

    String FRAGMENT_FOLDER = "ListFragment";
    String FRAGMENT_NOTE = "NoteFragment";
    String FRAGMENT_LIST = "ListNoteFragment";
    String FRAGMENT_TRASH_NOTE = "TrashNoteFragment";
    String FRAGMENT_TRASH_LIST = "TrashListFragment";
    String FRAGMENT_SEARCH = "SearchFragment";
    String FRAGMENT_SETTINGS_LIST = "SettingsListFragment";
    String FRAGMENT_SETTINGS_WIDGET_CONFIG = "SettingsWidgetConfigFragment";
    String FRAGMENT_SETTINGS_LIST_CONFIG = "SettingsListConfigFragment";
    String FRAGMENT_SETTINGS_RESTORE_LIST = "SettingsRestoreListFragment";

    String EDGE_VISIBLE_NOTES_KEY = "EdgeVisibleNotes";
    String EDGE_NOTES_ORDER_KEY = "EdgeNotesOrder";
    String EDGE_TEXT_SIZE_KEY = "TextSize";
    String EDGE_IGNORE_TABS_KEY = "ignoreTabsInEdgePanel";
    String EDGE_HIDE_CONTENT_KEY = "hideContent";
    String EDGE_WAS_LOCKED_KEY = "wasLocked";

    int FALSE = 0;
    int TRUE = 1;

    int COLOR = 0;
    int STRIKETHROUGH = 1;

    int HEADER_VIEW = 0;
    int NEW_ITEM_VIEW = 1;
    int ENABLED_ITEM_VIEW = 2;
    int DISABLED_ITEM_VIEW = 3;

    int TYPE_INT = 0;

    int TYPE_NOTE = 0;
    int TYPE_LIST = 1;

    int READ_PERMISSION = 0;
    int WRITE_PERMISSION = 1;

    int WIDGET_MODE_TITLE = 0;
    int WIDGET_MODE_CONFIG = 1;

    int WIDGET_THEME_MIUI = 0;
    int WIDGET_THEME_MATERIAL = 1;
    int WIDGET_THEME_SIMPLE = 2;

    int WIDGET_THEME_LIGHT = 0;
    int WIDGET_THEME_DARK = 1;

}
