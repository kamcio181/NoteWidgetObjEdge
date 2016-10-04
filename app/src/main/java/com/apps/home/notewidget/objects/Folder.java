package com.apps.home.notewidget.objects;

import java.io.Serializable;

public class Folder implements Serializable {
    private long id;
    private String name;
    private int count;

    public Folder() {
    }

    public Folder(String name) {
        this.name = name;
    }

    public Folder(long id){
        this.id = id;
    }

    public Folder(long id, String name, int count) {
        this.id = id;
        this.name = name;
        this.count = count;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
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

    public int getCount() {
        return count;
    }

    public String toString(){
        return "i: " + id + " ,n: " + name + " ,c: " + count;
    }
}
