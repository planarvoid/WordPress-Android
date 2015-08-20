package com.soundcloud.android.playback.ui;

import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;
import rx.subscriptions.CompositeSubscription;

import android.support.v4.view.ViewPager;

import javax.inject.Inject;

public class PlayerPagerScrollListener implements ViewPager.OnPageChangeListener {

    private final ReplaySubject<Integer> scrollStateSubject = ReplaySubject.createWithSize(1);
    private final PlaybackToastHelper playbackToastHelper;
    private final PlayQueueManager playQueueManager;
    private final EventBus eventBus;
    private final AdsOperations adsOperations;

    private CompositeSubscription subscription;
    private PlayerTrackPager trackPager;
    private TrackPagerAdapter adapter;
    private boolean wasPageChange;
    private boolean wasDragging;

    private final DefaultSubscriber<Integer> trackPageChanged = new DefaultSubscriber<Integer>() {
        @Override
        public void onNext(Integer integer) {
            if (wasDragging) {
                eventBus.queue(EventQueue.PLAYER_UI).first().subscribe(trackPlayerSwipeAction);
            }
        }
    };

    private final DefaultSubscriber<Integer> showBlockedSwipeToast = new DefaultSubscriber<Integer>() {
        @Override
        public void onNext(Integer args) {
            playbackToastHelper.showUnskippableAdToast();
        }
    };

    private final Func1<? super Integer, Boolean> settledOnNewPage = new Func1<Integer, Boolean>() {
        @Override
        public Boolean call(Integer state) {
            return wasPageChange && state == ViewPager.SCROLL_STATE_IDLE;
        }
    };

    private final Action1<PlayerUIEvent> trackPlayerSwipeAction = new Action1<PlayerUIEvent>() {
        @Override
        public void call(PlayerUIEvent event) {
            final boolean isExpanded = event.getKind() == PlayerUIEvent.PLAYER_EXPANDED;
            final PlayControlEvent trackEvent = isSwipeNext() ?
                    PlayControlEvent.swipeSkip(isExpanded) : PlayControlEvent.swipePrevious(isExpanded);
            eventBus.publish(EventQueue.TRACKING, trackEvent);
        }
    };

    private final Func1<? super Integer, Boolean> noPageChangedScrollOnAd = new Func1<Integer, Boolean>() {
        @Override
        public Boolean call(Integer state) {
            return !wasPageChange && state == ViewPager.SCROLL_STATE_IDLE && adsOperations.isCurrentTrackAudioAd();
        }
    };

    @Inject
    PlayerPagerScrollListener(PlayQueueManager playQueueManager, PlaybackToastHelper playbackToastHelper,
                              EventBus eventBus, AdsOperations adsOperations) {
        this.playQueueManager = playQueueManager;
        this.eventBus = eventBus;
        this.playbackToastHelper = playbackToastHelper;
        this.adsOperations = adsOperations;
    }

    public void initialize(PlayerTrackPager trackPager, TrackPagerAdapter adapter) {
        this.trackPager = trackPager;
        this.adapter = adapter;
        this.trackPager.setOnPageChangeListener(this);
        subscribe();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // no-op
    }

    @Override
    public void onPageSelected(int position) {
        final int playQueuePosition = adapter.getPlayQueuePosition(position);
        final boolean notAudioAd = !adsOperations.isAudioAdAtPosition(playQueuePosition);
        final boolean currentPosition = playQueueManager.isCurrentPosition(playQueuePosition);
        trackPager.setPagingEnabled(notAudioAd || currentPosition);
        wasPageChange = true;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        scrollStateSubject.onNext(state);
        configureFromScrollState(state);
    }

    private void configureFromScrollState(int state) {
        if (state == ViewPager.SCROLL_STATE_DRAGGING) {
            wasDragging = true;
        } else if (state == ViewPager.SCROLL_STATE_IDLE) {
            wasDragging = false;
            wasPageChange = false;
        }
    }

    private void subscribe() {
        subscription = new CompositeSubscription();
        subscription.add(scrollStateSubject
                .filter(noPageChangedScrollOnAd)
                .subscribe(showBlockedSwipeToast));
        subscription.add(getPageChangedObservable().subscribe(trackPageChanged));
    }

    public void unsubscribe() {
        subscription.unsubscribe();
    }

    public Observable<Integer> getPageChangedObservable() {
        return scrollStateSubject.filter(settledOnNewPage);
    }

    private boolean isSwipeNext() {
        return trackPager.getCurrentItem() > playQueueManager.getCurrentPosition();
    }
}
