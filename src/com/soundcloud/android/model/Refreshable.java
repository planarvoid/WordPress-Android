package com.soundcloud.android.model;


public interface Refreshable {
    long getRefreshableId();
    ScModel getRefreshableResource();
    boolean isStale();
}
