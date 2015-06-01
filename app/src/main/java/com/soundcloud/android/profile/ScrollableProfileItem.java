package com.soundcloud.android.profile;

public interface ScrollableProfileItem {
    void setScrollListener(Listener scrollListener);

    void configureOffsets(int currentHeaderHeight, int maxHeaderHeight);

    interface Listener {
        void onVerticalScroll(int dy, int visibleHeaderHeight);
    }
}
