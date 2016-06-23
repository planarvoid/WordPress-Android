package com.soundcloud.android.playback.ui;

import com.soundcloud.android.playback.ui.view.PlayerTrackPager;

import android.graphics.Rect;
import android.view.View;

import java.lang.ref.WeakReference;

class PlayerViewVisibilityProvider implements ViewVisibilityProvider {

    private final WeakReference<PlayerTrackPager> playerTrackPagerRef;

    public PlayerViewVisibilityProvider(PlayerTrackPager playerTrackPager) {
        this.playerTrackPagerRef = new WeakReference<>(playerTrackPager);
    }

    @Override
    public boolean isCurrentlyVisible(View view) {
        PlayerTrackPager playerTrackPager = playerTrackPagerRef.get();
        if (playerTrackPager != null) {
            Rect scrollBounds = new Rect();
            playerTrackPager.getHitRect(scrollBounds);
            return view.getLocalVisibleRect(scrollBounds);
        } else {
            return false;
        }
    }
}
