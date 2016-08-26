package com.apps.home.notewidget.objects;

/**
 * Created by k.kaszubski on 8/25/16.
 */
public class ShoppingListItem {
    private static int itemId = 0;
    private int id;
    private String content;
    private int viewType;

    public ShoppingListItem(String content, int viewType){
        id = itemId;
        this.content = content;
        this.viewType = viewType;

        itemId++;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getViewType() {
        return viewType;
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
    }
}
