package com.apps.home.notewidget.objects;

import java.io.Serializable;

public class Folder implements Serializable {
    private long id;
    private String name;
    private int icon;
    private int count;

    public Folder() {
    }

    public Folder(String name, int icon) {
        this.name = name;
        this.icon = icon;
    }

    public Folder(long id, String name, int icon, int count) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.count = count;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getIcon() {
        return icon;
    }

    public int getCount() {
        return count;
    }

    public String toString(){
        return "i: " + id + " ,n: " + name + " ,c: " + count;
    }
}
