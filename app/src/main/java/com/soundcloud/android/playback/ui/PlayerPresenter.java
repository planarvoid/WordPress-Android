package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.events.PlaybackProgressEvent;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;

import javax.inject.Inject;

class PlayerPresenter implements View.OnClickListener {

    private final ViewPager trackPager;
    private final Button play;
    private final Button next;
    private final Button previous;

    private final TrackPagerAdapter adapter;
    private final Listener listener;

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

        play = (Button) view.findViewById(R.id.player_play);
        next = (Button) view.findViewById(R.id.player_next);
        previous = (Button) view.findViewById(R.id.player_previous);
        play.setOnClickListener(this);
        next.setOnClickListener(this);
        previous.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.player_play:
                setPlayControlsVisible(false);
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
        adapter.setPlayState(isPlaying);
        setPlayControlsVisible(!isPlaying);
    }

    private void setPlayControlsVisible(boolean visble) {
        play.setVisibility(visble ? View.VISIBLE : View.GONE);
        next.setVisibility(visble ? View.VISIBLE : View.GONE);
        previous.setVisibility(visble ? View.VISIBLE : View.GONE);
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
