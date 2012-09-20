package com.soundcloud.android.model;


public interface Refreshable {
    long getRefreshableId();
    ScResource getRefreshableResource();
    boolean isStale();
}
