package com.soundcloud.android.playback.ui;

import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Observable;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;
import rx.subscriptions.CompositeSubscription;

import android.support.v4.view.ViewPager;

import javax.inject.Inject;

public class PlayerPagerScrollListener implements ViewPager.OnPageChangeListener {

    private final ReplaySubject<Integer> scrollStateSubject = ReplaySubject.createWithSize(1);
    private final PlaybackFeedbackHelper playbackFeedbackHelper;
    private final PlayQueueManager playQueueManager;
    private final AdsOperations adsOperations;

    private CompositeSubscription subscription;
    private PlayerTrackPager trackPager;
    private PlayerPagerPresenter presenter;
    private boolean wasPageChange;

    private final DefaultSubscriber<Integer> showBlockedSwipeFeedback = new DefaultSubscriber<Integer>() {
        @Override
        public void onNext(Integer args) {
            playbackFeedbackHelper.showUnskippableAdFeedback();
        }
    };

    private final Func1<? super Integer, Boolean> settledOnNewPage = new Func1<Integer, Boolean>() {
        @Override
        public Boolean call(Integer state) {
            return wasPageChange && state == ViewPager.SCROLL_STATE_IDLE;
        }
    };

    private final Func1<? super Integer, Boolean> noPageChangedScrollOnAd = new Func1<Integer, Boolean>() {
        @Override
        public Boolean call(Integer state) {
            return !wasPageChange && state == ViewPager.SCROLL_STATE_IDLE && adsOperations.isCurrentItemAd();
        }
    };

    @Inject
    PlayerPagerScrollListener(PlayQueueManager playQueueManager, PlaybackFeedbackHelper playbackFeedbackHelper,
                              AdsOperations adsOperations) {
        this.playQueueManager = playQueueManager;
        this.playbackFeedbackHelper = playbackFeedbackHelper;
        this.adsOperations = adsOperations;
    }

    public void initialize(PlayerTrackPager trackPager, PlayerPagerPresenter presenter) {
        this.trackPager = trackPager;
        this.presenter = presenter;
        this.trackPager.setOnPageChangeListener(this);
        subscribe();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // no-op
    }

    @Override
    public void onPageSelected(int position) {
        final PlayQueueItem itemAtPosition = presenter.getItemAtPosition(position);
        final boolean notAd = !itemAtPosition.isAd();
        final boolean currentPosition = playQueueManager.isCurrentItem(itemAtPosition);
        trackPager.setPagingEnabled(notAd || currentPosition);
        wasPageChange = true;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        scrollStateSubject.onNext(state);
        configureFromScrollState(state);
    }

    private void configureFromScrollState(int state) {
        if (state == ViewPager.SCROLL_STATE_IDLE) {
            wasPageChange = false;
        }
    }

    private void subscribe() {
        subscription = new CompositeSubscription();
        subscription.add(scrollStateSubject
                                 .filter(noPageChangedScrollOnAd)
                                 .subscribe(showBlockedSwipeFeedback));
    }

    public void unsubscribe() {
        subscription.unsubscribe();
    }

    public Observable<Integer> getPageChangedObservable() {
        return scrollStateSubject.filter(settledOnNewPage);
    }
}
