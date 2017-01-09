package com.soundcloud.android.playback.ui;

import android.view.View;

public interface ViewVisibilityProvider {

    ViewVisibilityProvider EMPTY = view -> false;

    ViewVisibilityProvider ALWAYS_VISIBLE = view -> true;

    boolean isCurrentlyVisible(View view);
}
