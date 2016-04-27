package com.apps.home.notewidget.Objects;


public class Note {
    private long id;
    private long createdAt;
    private String title;
    private String note;
    private long folderId;
    private int deletedState;

    public Note() {
    }

    public Note(long id, long createdAt, String title, String note, long folderId, int deletedState) {
        this.id = id;
        this.createdAt = createdAt;
        this.title = title;
        this.note = note;
        this.folderId = folderId;
        this.deletedState = deletedState;
    }

    public Note(long createdAt, String title, long folderId, int deletedState) {
        this.createdAt = createdAt;
        this.title = title;
        this.folderId = folderId;
        this.deletedState = deletedState;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setFolderId(long folderId) {
        this.folderId = folderId;
    }

    public void setDeletedState(int deletedState) {
        this.deletedState = deletedState;
    }

    public long getId() {
        return id;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getTitle() {
        return title;
    }

    public String getNote() {
        return note;
    }

    public long getFolderId() {
        return folderId;
    }

    public int getDeletedState() {
        return deletedState;
    }
}
