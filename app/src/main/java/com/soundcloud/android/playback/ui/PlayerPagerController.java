package com.soundcloud.android.playback.ui;

import com.nineoldandroids.animation.ObjectAnimator;
import com.soundcloud.android.R;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.subscriptions.CompositeSubscription;

import android.support.v4.view.ViewPager;
import android.view.View;

import javax.inject.Inject;

public class PlayerPagerController {

    private final TrackPagerAdapter adapter;
    private final EventBus eventBus;
    private final PlayQueueManager playQueueManager;
    private final PlaybackOperations playbackOperations;
    private final PlayerPresenter presenter;
    private CompositeSubscription subscription;
    private ViewPager trackPager;

    @Inject
    public PlayerPagerController(TrackPagerAdapter adapter, PlayerPresenter playerPresenter, EventBus eventBus, PlayQueueManager playQueueManager, PlaybackOperations playbackOperations) {
        this.adapter = adapter;
        this.presenter = playerPresenter;
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
        this.playbackOperations = playbackOperations;
        this.subscription = new CompositeSubscription();
    }

    void onViewCreated(View view) {
        setPager((ViewPager) view.findViewById(R.id.player_track_pager));
        subscription.add(eventBus.subscribeImmediate(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber()));
        subscription.add(eventBus.subscribeImmediate(EventQueue.PLAY_QUEUE_TRACK, new PlayQueueTrackSubscriber()));
    }

    void onDestroyView() {
        subscription.unsubscribe();
        adapter.unsubscribe();
        ObjectAnimator.clearAllAnimations();
    }

    private void setPager(ViewPager trackPager) {
        this.presenter.initialize(trackPager);
        this.trackPager = trackPager;
        this.trackPager.setOnPageChangeListener(new TrackPageChangeListener());
        trackPager.setAdapter(adapter);
        setQueuePosition(playQueueManager.getCurrentPosition());
    }

    private void setQueuePosition(int position) {
        boolean isAdjacentTrack = Math.abs(trackPager.getCurrentItem() - position) <= 1;
        trackPager.setCurrentItem(position, isAdjacentTrack);
    }

    private void onPlayQueueChanged() {
        adapter.notifyDataSetChanged();
    }

    private final class PlayQueueSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent event) {
            onPlayQueueChanged();
        }
    }

    private final class PlayQueueTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent> {
        @Override
        public void onNext(CurrentPlayQueueTrackEvent event) {
            setQueuePosition(playQueueManager.getCurrentPosition());
        }
    }

    private class TrackPageChangeListener implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageSelected(int position) {
            playbackOperations.setPlayQueuePosition(position);
            adapter.onTrackChange();
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // No-op
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // No-op
        }
    }
}
