package com.soundcloud.android.playback.ui;

import com.nineoldandroids.animation.ObjectAnimator;
import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.support.v4.view.ViewPager;
import android.view.View;

import javax.inject.Inject;

class PlayerPagerController implements ViewPager.OnPageChangeListener {

    private final TrackPagerAdapter adapter;
    private final EventBus eventBus;
    private final PlayQueueManager playQueueManager;
    private final PlaybackOperations playbackOperations;
    private final PlayerPresenter presenter;
    private final Observable<PlaybackProgressEvent> checkAdProgress;
    private CompositeSubscription subscription;
    private Subscription unblockPagerSubscription = Subscriptions.empty();
    private PlayerTrackPager trackPager;
    private boolean shouldChangeTrackOnIdle;

    private final Action1<PlaybackProgressEvent> unlockPager = new Action1<PlaybackProgressEvent>() {
        @Override
        public void call(PlaybackProgressEvent ignored) {
            trackPager.setPagingEnabled(true);
        }
    };

    @Inject
    public PlayerPagerController(TrackPagerAdapter adapter, PlayerPresenter playerPresenter, EventBus eventBus, PlayQueueManager playQueueManager, PlaybackOperations playbackOperations) {
        this.adapter = adapter;
        this.presenter = playerPresenter;
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
        this.playbackOperations = playbackOperations;

        checkAdProgress = eventBus.queue(EventQueue.PLAYBACK_PROGRESS).first(new Func1<PlaybackProgressEvent, Boolean>() {
            @Override
            public Boolean call(PlaybackProgressEvent playbackProgressEvent) {
                return playbackProgressEvent.getPlaybackProgress().getPosition() >= AdConstants.UNSKIPPABLE_TIME_MS;
            }
        });
    }

    void onViewCreated(View view) {
        setPager((PlayerTrackPager) view.findViewById(R.id.player_track_pager));

        subscription = new CompositeSubscription();
        subscription.add(eventBus.subscribeImmediate(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber()));
        subscription.add(eventBus.subscribeImmediate(EventQueue.PLAY_QUEUE_TRACK, new PlayQueueTrackSubscriber()));
    }

    void onDestroyView() {
        subscription.unsubscribe();
        unblockPagerSubscription.unsubscribe();
        adapter.unsubscribe();
        ObjectAnimator.clearAllAnimations();
    }

    private void setPager(PlayerTrackPager trackPager) {
        this.presenter.initialize(trackPager);
        this.trackPager = trackPager;
        this.trackPager.setOnPageChangeListener(this);
        trackPager.setAdapter(adapter);
        setQueuePosition(playQueueManager.getCurrentPosition());
    }

    private void setQueuePosition(int position) {
        boolean isAdjacentTrack = Math.abs(trackPager.getCurrentItem() - position) <= 1;
        trackPager.setCurrentItem(position, isAdjacentTrack);
    }

    private void onPlayQueueChanged() {
        adapter.notifyDataSetChanged();
        setQueuePosition(playQueueManager.getCurrentPosition());
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
            unblockPagerSubscription.unsubscribe();
            setQueuePosition(playQueueManager.getCurrentPosition());

            if (playQueueManager.isCurrentTrackAudioAd()){
                trackPager.setPagingEnabled(false);
                unblockPagerSubscription = checkAdProgress.subscribe(unlockPager);
            } else {
                trackPager.setPagingEnabled(true);
            }
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // no-op
    }

    @Override
    public void onPageSelected(int position) {
        shouldChangeTrackOnIdle = true;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (shouldChangeTrackOnIdle && state == ViewPager.SCROLL_STATE_IDLE) {
            playbackOperations.setPlayQueuePosition(trackPager.getCurrentItem());
            adapter.onTrackChange();
            shouldChangeTrackOnIdle = false;
        }
    }
}
