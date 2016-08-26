package com.apps.home.notewidget.objects;


import java.io.Serializable;

public class Note implements Serializable {
    private long id;
    private long createdAt;
    private String title;
    private String note;
    private long folderId;
    private int deletedState;
    private int type;

    public Note() {
    }

    public Note(long id, long createdAt, String title, String note, long folderId, int deletedState, int type) {
        this.id = id;
        this.createdAt = createdAt;
        this.title = title;
        this.note = note;
        this.folderId = folderId;
        this.deletedState = deletedState;
        this.type = type;
    }

    public Note(long createdAt, String title, long folderId, int deletedState, int type) {
        this.createdAt = createdAt;
        this.title = title;
        this.folderId = folderId;
        this.deletedState = deletedState;
        this.type = type;
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

    public void setType(int type) {
        this.type = type; }

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

    public int getType() {
        return type; }

    public String toString(){
        return "i: " + id + " ,c: " + createdAt + " ,t: " + title + " ,n: " + note + " ,f: "
                + folderId + " ,d: " + deletedState + ", t: " + type;
    }
}
