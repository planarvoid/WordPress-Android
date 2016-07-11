package com.soundcloud.android.playback.ui;

import android.view.View;

public interface ViewVisibilityProvider {

    ViewVisibilityProvider EMPTY = new ViewVisibilityProvider() {
        @Override
        public boolean isCurrentlyVisible(View view) {
            return false;
        }
    };

    ViewVisibilityProvider ALWAYS_VISIBLE = new ViewVisibilityProvider() {
        @Override
        public boolean isCurrentlyVisible(View view) {
            return true;
        }
    };

    boolean isCurrentlyVisible(View view);
}
