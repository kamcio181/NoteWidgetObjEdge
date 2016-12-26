package com.apps.home.notewidget.objects;


import java.io.Serializable;

public class Event implements Serializable {
    private long id;
    private String title;
    private boolean allDay;
    private long start;
    private long end;
    private String location;
    private int notification;
    private int color;

    public Event(long id) {
        this.id = id;
    }

    public Event(long id, String title, boolean allDay, long start, long end, String location, int notification, int color) {
        this.id = id;
        this.title = title;
        this.allDay = allDay;
        this.start = start;
        this.end = end;
        this.location = location;
        this.notification = notification;
        this.color = color;
    }

    public Event(String title, boolean allDay, long start, long end, String location, int notification, int color) {
        this.title = title;
        this.allDay = allDay;
        this.start = start;
        this.end = end;
        this.location = location;
        this.notification = notification;
        this.color = color;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public String getLocation() {
        return location;
    }

    public int getNotification() {
        return notification;
    }

    public int getColor() {
        return color;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAllDay(boolean allDay) {
        this.allDay = allDay;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setNotification(int notification) {
        this.notification = notification;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public String toString() {
        return "id " + id + " t " + title + " a " + allDay + " s " + start + " e " + end + " l "
                + location + " n " + notification + " c " + color;
    }
}
