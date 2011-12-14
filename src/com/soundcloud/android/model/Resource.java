package com.soundcloud.android.model;


public interface Resource {
    long getId();
    Track getTrack();
    User getUser();
    long getLastUpdated();
}
