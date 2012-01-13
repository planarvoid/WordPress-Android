package com.soundcloud.android.model;


public interface Resource {
    long getResourceId();
    long getLastUpdated();
    long getStaleTime();
}
