package com.apps.home.notewidget.Objects;

public class Widget {
    private long id;
    private long widgetId;
    private long noteId;
    private int mode;
    private int theme;
    private int textSize;

    public Widget() {
    }

    public Widget(long widgetId, long noteId, int mode, int theme, int textSize) {
        this.widgetId = widgetId;
        this.noteId = noteId;
        this.mode = mode;
        this.theme = theme;
        this.textSize = textSize;
    }

    public Widget(long id, long widgetId, long noteId, int mode, int theme, int textSize) {
        this.id = id;
        this.widgetId = widgetId;
        this.noteId = noteId;
        this.mode = mode;
        this.theme = theme;
        this.textSize = textSize;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setWidgetId(long widgetId) {
        this.widgetId = widgetId;
    }

    public void setNoteId(long noteId) {
        this.noteId = noteId;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void setTheme(int theme) {
        this.theme = theme;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public long getId() {
        return id;
    }

    public long getWidgetId() {
        return widgetId;
    }

    public long getNoteId() {
        return noteId;
    }

    public int getMode() {
        return mode;
    }

    public int getTheme() {
        return theme;
    }

    public int getTextSize() {
        return textSize;
    }

    public String toString(){
        return "i: " + id + " ,w: " + widgetId + " ,n: " + noteId + " ,m: " + mode + " ,t: "
                + theme + " ,s: " + textSize;
    }
}
