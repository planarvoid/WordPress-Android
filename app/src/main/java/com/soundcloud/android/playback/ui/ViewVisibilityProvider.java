package com.soundcloud.android.playback.ui;

import android.view.View;

public interface ViewVisibilityProvider {

    ViewVisibilityProvider EMPTY = new ViewVisibilityProvider() {
        @Override
        public boolean isCurrentlyVisible(View view) {
            return false;
        }
    };

    boolean isCurrentlyVisible(View view);
}
