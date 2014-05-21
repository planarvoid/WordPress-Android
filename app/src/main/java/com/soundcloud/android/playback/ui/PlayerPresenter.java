package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.events.PlaybackProgressEvent;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ToggleButton;

import javax.inject.Inject;

class PlayerPresenter implements View.OnClickListener {

    private final ViewPager trackPager;
    private final ToggleButton playerToggle;
    private final Listener listener;
    private final TrackPagerAdapter adapter;

    interface Listener {
        void onTogglePlay();
        void onNext();
        void onPrevious();
        void onTrackChanged(int position);
    }

    PlayerPresenter(View view, TrackPagerAdapter adapter, Listener listener) {
        this.adapter = adapter;
        this.listener = listener;

        trackPager = (ViewPager) view.findViewById(R.id.player_track_pager);
        trackPager.setOnPageChangeListener(new TrackPageChangeListener());
        trackPager.setAdapter(adapter);

        playerToggle = (ToggleButton) view.findViewById(R.id.player_toggle);
        playerToggle.setOnClickListener(this);

        view.findViewById(R.id.player_next).setOnClickListener(this);
        view.findViewById(R.id.player_previous).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.player_toggle:
                listener.onTogglePlay();
                break;
            case R.id.player_next:
                listener.onNext();
                break;
            case R.id.player_previous:
                listener.onPrevious();
                break;
            default:
                throw new IllegalArgumentException("Unexpected view ID");
        }
    }

    void setQueuePosition(int position) {
        trackPager.setCurrentItem(position);
    }

    void onPlayQueueChanged() {
        adapter.notifyDataSetChanged();
    }

    void onPlayStateChanged(boolean isPlaying){
        playerToggle.setChecked(isPlaying);
        adapter.setPlayState(isPlaying);
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

        private final TrackPagerAdapter trackPagerAdapter;

        @Inject
        public Factory(TrackPagerAdapter trackPagerAdapter) {
            this.trackPagerAdapter = trackPagerAdapter;
        }

        public PlayerPresenter create(View view, Listener listener){
            return new PlayerPresenter(view, trackPagerAdapter, listener);
        }
    }

}
