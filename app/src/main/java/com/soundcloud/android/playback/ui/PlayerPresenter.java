package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.events.PlaybackProgressEvent;

import android.content.res.Resources;
import android.support.v4.view.ViewPager;
import android.view.View;

import javax.inject.Inject;

class PlayerPresenter {

    private final ViewPager trackPager;
    private final TrackPagerAdapter adapter;
    private final Listener listener;

    interface Listener {
        void onTrackChanged(int position);
    }

    PlayerPresenter(Resources resources, TrackPagerAdapter adapter, Listener listener, View view) {
        this.adapter = adapter;
        this.listener = listener;

        trackPager = (ViewPager) view.findViewById(R.id.player_track_pager);
        trackPager.setPageMargin(resources.getDimensionPixelSize(R.dimen.player_pager_spacing));
        trackPager.setPageMarginDrawable(R.color.black);
        trackPager.setOnPageChangeListener(new TrackPageChangeListener());
    }

    void setQueuePosition(int position) {
        boolean isAdjacentTrack = Math.abs(trackPager.getCurrentItem() - position) <= 1;
        trackPager.setCurrentItem(position, isAdjacentTrack);
        adapter.setProgressOnAllViews();
    }

    void onPlayQueueChanged() {
        adapter.notifyDataSetChanged();
        if (trackPager.getAdapter() == null) {
            trackPager.setAdapter(adapter);
        }
    }

    void onPlayStateChanged(boolean isPlaying){
        adapter.setPlayState(isPlaying);
    }

    public void setFullScreenPlayer(boolean fullScreen) {
        adapter.fullScreenMode(fullScreen);
    }

    public void onPlayerProgress(PlaybackProgressEvent progress) {
        adapter.setProgressOnCurrentTrack(progress);
    }

    private class TrackPageChangeListener implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // No-op
        }

        @Override
        public void onPageSelected(int position) {
            listener.onTrackChanged(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // No-op
        }
    }

    public static class Factory {

        private final Resources resources;
        private final TrackPagerAdapter trackPagerAdapter;
        private final PlayerListener listener;

        @Inject
        public Factory(Resources resources, TrackPagerAdapter trackPagerAdapter, PlayerListener listener) {
            this.resources = resources;
            this.trackPagerAdapter = trackPagerAdapter;
            this.listener = listener;
        }

        public PlayerPresenter create(View view){
            return new PlayerPresenter(resources, trackPagerAdapter, listener, view);
        }
    }

}
