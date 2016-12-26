package com.apps.home.notewidget.objects;

/**
 * Created by kamil on 10.12.16.
 */

public class Color {
    private String name;
    private String value;

    public Color(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
