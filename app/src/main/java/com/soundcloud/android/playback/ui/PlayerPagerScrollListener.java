package com.soundcloud.android.playback.ui;

import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.rx.observers.LambdaObserver;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.ReplaySubject;

import android.support.v4.view.ViewPager;

import javax.inject.Inject;

public class PlayerPagerScrollListener implements ViewPager.OnPageChangeListener {

    private final ReplaySubject<Integer> scrollStateSubject = ReplaySubject.createWithSize(1);
    private final PlaybackFeedbackHelper playbackFeedbackHelper;
    private final PlayQueueManager playQueueManager;
    private final AdsOperations adsOperations;

    private CompositeDisposable disposable;
    private PlayerTrackPager trackPager;
    private PlayerPagerPresenter presenter;
    private boolean wasPageChange;

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
        disposable = new CompositeDisposable();
        disposable.add(scrollStateSubject
                               .filter(state -> !wasPageChange && state == ViewPager.SCROLL_STATE_IDLE && adsOperations.isCurrentItemAd())
                               .subscribeWith(LambdaObserver.onNext(__ -> playbackFeedbackHelper.showUnskippableAdFeedback())));
    }

    public void unsubscribe() {
        disposable.dispose();
    }

    Observable<Integer> getPageChangedObservable() {
        return scrollStateSubject.filter(state -> wasPageChange && state == ViewPager.SCROLL_STATE_IDLE);
    }
}
