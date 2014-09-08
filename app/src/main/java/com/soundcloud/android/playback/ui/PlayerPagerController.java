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
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.ui.view.PlaybackToastViewController;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.view.View;

import javax.inject.Inject;

class PlayerPagerController implements ViewPager.OnPageChangeListener, PlayerTrackPager.OnBlockedSwipeListener {

    private static final int CHANGE_TRACKS_MESSAGE = 0;
    private static final int CHANGE_TRACKS_DELAY = 350;

    private final TrackPagerAdapter adapter;
    private final EventBus eventBus;
    private final PlayQueueManager playQueueManager;
    private final PlaybackOperations playbackOperations;
    private final PlaybackToastViewController playbackToastViewController;
    private final PlayerPresenter presenter;
    private final Observable<PlaybackProgressEvent> checkAdProgress;
    private CompositeSubscription subscription;
    private Subscription unblockPagerSubscription = Subscriptions.empty();
    private PlayerTrackPager trackPager;
    private boolean shouldChangeTrackOnIdle;
    private boolean isResumed;

    private final Handler changeTracksHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            playbackOperations.setPlayQueuePosition(trackPager.getCurrentItem());
        }
    };

    private final Subscriber<PlaybackProgressEvent> unlockPager = new DefaultSubscriber<PlaybackProgressEvent>() {
        @Override
        public void onNext(PlaybackProgressEvent ignored) {
            trackPager.setPagingEnabled(true);
        }
    };

    private final Func1<Playa.StateTransition, Boolean> wasError = new Func1<Playa.StateTransition, Boolean>() {
        @Override
        public Boolean call(Playa.StateTransition stateTransition) {
            return stateTransition.wasError();
        }
    };

    @Inject
    public PlayerPagerController(TrackPagerAdapter adapter, PlayerPresenter playerPresenter, EventBus eventBus,
                                 PlayQueueManager playQueueManager, PlaybackOperations playbackOperations,
                                 PlaybackToastViewController playbackToastViewController) {
        this.adapter = adapter;
        this.presenter = playerPresenter;
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
        this.playbackOperations = playbackOperations;
        this.playbackToastViewController = playbackToastViewController;

        checkAdProgress = eventBus.queue(EventQueue.PLAYBACK_PROGRESS).first(new Func1<PlaybackProgressEvent, Boolean>() {
            @Override
            public Boolean call(PlaybackProgressEvent playbackProgressEvent) {
                return playbackProgressEvent.getPlaybackProgress().getPosition() >= AdConstants.UNSKIPPABLE_TIME_MS;
            }
        });
    }

    @Override
    public void onBlockedSwipe() {
        playbackToastViewController.showUnkippableAdToast();
    }

    public void onPlayerSlide(float slideOffset) {
        adapter.onPlayerSlide(slideOffset);
    }

    public void onResume() {
        isResumed = true;
        adapter.onResume();
    }

    public void onPause() {
        isResumed = false;
        adapter.onPause();
    }

    void onViewCreated(View view) {
        setPager((PlayerTrackPager) view.findViewById(R.id.player_track_pager));

        subscription = new CompositeSubscription();
        subscription.add(eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED).filter(wasError).subscribe(new ErrorSubscriber()));
        subscription.add(eventBus.subscribeImmediate(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber()));
        subscription.add(eventBus.subscribeImmediate(EventQueue.PLAY_QUEUE_TRACK, new PlayQueueTrackSubscriber()));
    }

    void onDestroyView() {
        subscription.unsubscribe();
        unblockPagerSubscription.unsubscribe();
        adapter.unsubscribe();
        changeTracksHandler.removeMessages(CHANGE_TRACKS_MESSAGE);
        ObjectAnimator.clearAllAnimations();
    }

    private void setPager(final PlayerTrackPager trackPager) {
        this.presenter.initialize(trackPager);
        this.trackPager = trackPager;
        this.trackPager.setOnBlockedSwipeListener(this);
        this.trackPager.setOnPageChangeListener(this);
        trackPager.setAdapter(adapter);
        setQueuePosition(playQueueManager.getCurrentPosition());

        adapter.setSkipListener(getSkipListener(trackPager));
        adapter.warmupViewCache(trackPager);
    }

    private SkipListener getSkipListener(final PlayerTrackPager trackPager) {
        return new SkipListener() {
            @Override
            public void onNext() {
                trackPager.setCurrentItem(trackPager.getCurrentItem() + 1);
            }

            @Override
            public void onPrevious() {
                trackPager.setCurrentItem(trackPager.getCurrentItem() - 1);
            }
        };
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
        trackPager.setPagingEnabled(!playQueueManager.isAudioAdAtPosition(position));
        shouldChangeTrackOnIdle = true;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (shouldChangeTrackOnIdle && state == ViewPager.SCROLL_STATE_IDLE) {
            changeTracksIfInForeground();
            adapter.onTrackChange();
            shouldChangeTrackOnIdle = false;
        }
    }

    private void changeTracksIfInForeground() {
        if (isResumed) {
            changeTracksHandler.removeMessages(CHANGE_TRACKS_MESSAGE);
            changeTracksHandler.sendEmptyMessageDelayed(CHANGE_TRACKS_MESSAGE, CHANGE_TRACKS_DELAY);
        }
    }

    private final class ErrorSubscriber extends DefaultSubscriber<Playa.StateTransition>{
        @Override
        public void onNext(Playa.StateTransition newState) {
            playbackToastViewController.showError(newState.getReason());
        }
    }
}
