package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.util.AnimUtils;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleBinder;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;

class PlayerPresenter extends SupportFragmentLightCycleDispatcher<PlayerFragment> {

    private static final int CHANGE_TRACKS_MESSAGE = 0;
    private static final int CHANGE_TRACKS_DELAY = 350;

    @LightCycle final PlayerPagerPresenter presenter;

    private final EventBus eventBus;
    private final PlayQueueManager playQueueManager;
    private final PlaySessionController playSessionController;
    private final AdsOperations adsOperations;

    private final Observable<PlaybackProgressEvent> checkAdProgress;
    private final PlayerPagerScrollListener playerPagerScrollListener;
    private final Provider<PlayQueueDataSource> playQueueDataSwitcherProvider;

    private PlayQueueDataSource playQueueDataSource;
    private CompositeSubscription subscription = new CompositeSubscription();
    private Subscription unblockPagerSubscription = RxUtils.invalidSubscription();
    private Handler changeTracksHandler;

    private PlayerTrackPager trackPager;
    private boolean isResumed;
    private boolean setPlayQueueAfterScroll;

    private final Func1<CurrentPlayQueueTrackEvent, Boolean> isCurrentTrackAudioAd = new Func1<CurrentPlayQueueTrackEvent, Boolean>() {
        @Override
        public Boolean call(CurrentPlayQueueTrackEvent ignored) {
            return adsOperations.isCurrentTrackAudioAd();
        }
    };

    private final Action1<Object> updateAdapter = new Action1<Object>() {
        @Override
        public void call(Object ignored) {
            presenter.onTrackChange();
        }
    };

    private final Func1<? super Integer, Boolean> isInForeground = new Func1<Integer, Boolean>() {
        @Override
        public Boolean call(Integer integer) {
            return isResumed;
        }
    };

    private final Action1<CurrentPlayQueueTrackEvent> allowScrollAfterTimeout = new Action1<CurrentPlayQueueTrackEvent>() {
        @Override
        public void call(CurrentPlayQueueTrackEvent currentPlayQueueTrackEvent) {
            unblockPagerSubscription = checkAdProgress.observeOn(AndroidSchedulers.mainThread()).subscribe(getRestoreQueueSubscriber());
        }
    };

    private final Action1<CurrentPlayQueueTrackEvent> onTrackChanged = new Action1<CurrentPlayQueueTrackEvent>() {
        @Override
        public void call(CurrentPlayQueueTrackEvent currentPlayQueueTrackEvent) {
            presenter.onTrackChange();
            unblockPagerSubscription.unsubscribe();
        }
    };

    private final Func1<CurrentPlayQueueTrackEvent, Boolean> notWaitingForScroll = new Func1<CurrentPlayQueueTrackEvent, Boolean>() {
        @Override
        public Boolean call(CurrentPlayQueueTrackEvent currentPlayQueueTrackEvent) {
            return !setPlayQueueAfterScroll;
        }
    };

    private final DefaultSubscriber<CurrentPlayQueueTrackEvent> setPagerPositionFromPlayQueueManager = new DefaultSubscriber<CurrentPlayQueueTrackEvent>() {
        @Override
        public void onNext(CurrentPlayQueueTrackEvent args) {
            setQueuePosition(playQueueManager.getCurrentPosition());
        }
    };

    @Inject
    public PlayerPresenter(PlayerPagerPresenter presenter, EventBus eventBus,
                           PlayQueueManager playQueueManager, PlaySessionController playSessionController,
                           Provider<PlayQueueDataSource> playQueueDataSwitcherProvider,
                           PlayerPagerScrollListener playerPagerScrollListener, AdsOperations adsOperations) {
        this.presenter = presenter;
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
        this.playSessionController = playSessionController;
        this.playQueueDataSwitcherProvider = playQueueDataSwitcherProvider;
        this.playerPagerScrollListener = playerPagerScrollListener;
        this.adsOperations = adsOperations;

        LightCycleBinder.bind(this);

        changeTracksHandler = new ChangeTracksHandler(this);

        checkAdProgress = eventBus.queue(EventQueue.PLAYBACK_PROGRESS).first(new Func1<PlaybackProgressEvent, Boolean>() {
            @Override
            public Boolean call(PlaybackProgressEvent playbackProgressEvent) {
                return playbackProgressEvent.getPlaybackProgress().getPosition() >= AdConstants.UNSKIPPABLE_TIME_MS;
            }
        });
    }

    private void setPositionToDisplayedTrack(){
        playSessionController.setPlayQueuePosition(getDisplayedPositionInPlayQueue());
    }

    public void onPlayerSlide(float slideOffset) {
        presenter.onPlayerSlide(slideOffset);
    }

    @Override
    public void onResume(PlayerFragment fragment) {
        super.onResume(fragment);
        isResumed = true;
    }

    @Override
    public void onPause(PlayerFragment fragment) {
        super.onPause(fragment);
        isResumed = false;
    }

    @Override
    public void onViewCreated(PlayerFragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        setPager((PlayerTrackPager) view.findViewById(R.id.player_track_pager));
        setupTrackChangeSubscribers();
        setupScrollingSubscribers();
    }

    private void setupTrackChangeSubscribers() {
        // play queue changes
        subscription.add(eventBus.subscribeImmediate(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber()));

        // setup audio ad
        subscription.add(eventBus.queue(EventQueue.PLAY_QUEUE_TRACK)
                .doOnNext(onTrackChanged)
                .filter(isCurrentTrackAudioAd)
                .doOnNext(allowScrollAfterTimeout)
                .subscribe(new ShowAudioAdSubscriber()));

        // set position from track change
        subscription.add(eventBus.queue(EventQueue.PLAY_QUEUE_TRACK)
                .filter(notWaitingForScroll)
                .subscribe(setPagerPositionFromPlayQueueManager));
    }

    private void setupScrollingSubscribers() {
        final Observable<Integer> pageChangedObservable = playerPagerScrollListener
                .getPageChangedObservable();

        subscription.add(pageChangedObservable
                .subscribe(new SetQueueOnScrollSubscriber()));

        subscription.add(pageChangedObservable
                .doOnNext(updateAdapter)
                .filter(isInForeground)
                .subscribe(new ChangeTracksSubscriber()));
    }

    @Override
    public void onDestroyView(PlayerFragment playerFragment) {

        unblockPagerSubscription.unsubscribe();
        playerPagerScrollListener.unsubscribe();
        changeTracksHandler.removeMessages(CHANGE_TRACKS_MESSAGE);

        subscription.unsubscribe();
        subscription = new CompositeSubscription();

        AnimUtils.clearAllAnimations();
    }

    private void setPager(final PlayerTrackPager trackPager) {
        this.trackPager = trackPager;
        refreshPlayQueue();
        playerPagerScrollListener.initialize(trackPager, presenter);
    }

    private void setQueuePosition(int position) {
        boolean isAdjacentTrack = Math.abs(trackPager.getCurrentItem() - position) <= 1;
        trackPager.setCurrentItem(position, isAdjacentTrack);
    }

    private void setFullQueue() {
        presenter.setCurrentData(playQueueDataSource.getFullQueue());
        trackPager.setCurrentItem(playQueueManager.getCurrentPosition(), false);
        setPlayQueueAfterScroll = false;
    }

    private void setAdPlayQueue() {
        presenter.setCurrentData(playQueueDataSource.getCurrentTrackAsQueue());
        trackPager.setCurrentItem(0, false);
    }

    private void showAudioAd() {
        if (isShowingCurrentAudioAd() || !isResumed){
            setAdPlayQueue();
        } else {
            setPlayQueueAfterScroll = true;
            setQueuePosition(playQueueManager.getCurrentPosition());
        }
    }

    private boolean isShowingCurrentAudioAd() {
        return adsOperations.isCurrentTrackAudioAd()
                && playQueueManager.isCurrentPosition(getDisplayedPositionInPlayQueue());
    }

    private int getDisplayedPositionInPlayQueue() {
        return presenter.getPlayQueuePosition(trackPager.getCurrentItem());
    }

    private void refreshPlayQueue() {
        playQueueDataSource = playQueueDataSwitcherProvider.get();
        setFullQueue();
    }

    private final class PlayQueueSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent event) {
            if (event.audioAdRemoved() && isLookingAtAdWithFullQueue()) {
                setPlayQueueAfterScroll = true;
                trackPager.setCurrentItem(trackPager.getCurrentItem() + 1, true);
            } else if (!setPlayQueueAfterScroll) {
                refreshPlayQueue();
            }
        }
    }

    private boolean isLookingAtAdWithFullQueue() {
        return presenter.isAudioAdAtPosition(trackPager.getCurrentItem()) &&
                isResumed && trackPager.getAdapter().getCount() > 1;
    }

    private final class SetQueueOnScrollSubscriber extends DefaultSubscriber<Integer> {
        @Override
        public void onNext(Integer ignored) {
            if(setPlayQueueAfterScroll){
                if (adsOperations.isCurrentTrackAudioAd()){
                    setAdPlayQueue();
                } else {
                    refreshPlayQueue();
                }
            }
        }
    }

    private final class ChangeTracksSubscriber extends DefaultSubscriber<Integer>  {
        @Override
        public void onNext(Integer ignored) {
            changeTracksHandler.removeMessages(CHANGE_TRACKS_MESSAGE);
            changeTracksHandler.sendEmptyMessageDelayed(CHANGE_TRACKS_MESSAGE, CHANGE_TRACKS_DELAY);
        }
    }

    private final class ShowAudioAdSubscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent>  {
        @Override
        public void onNext(CurrentPlayQueueTrackEvent ignored) {
            showAudioAd();
        }
    }

    private DefaultSubscriber<PlaybackProgressEvent> getRestoreQueueSubscriber() {
        return new DefaultSubscriber<PlaybackProgressEvent>(){
            @Override
            public void onNext(PlaybackProgressEvent ignored) {
                setFullQueue();
            }
        };
    }

    private static class ChangeTracksHandler extends Handler {
        private final PlayerPresenter playerPresenter;

        private ChangeTracksHandler(PlayerPresenter playerPresenter) {
            this.playerPresenter = playerPresenter;
        }

        @Override
        public void handleMessage(Message msg) {
            playerPresenter.setPositionToDisplayedTrack();
        }
    }
}
