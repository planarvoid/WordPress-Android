package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.AnimUtils;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.os.Handler;
import android.os.Message;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;

class PlayerPagerController {

    private static final int CHANGE_TRACKS_MESSAGE = 0;
    private static final int CHANGE_TRACKS_DELAY = 350;

    private final TrackPagerAdapter adapter;
    private final EventBus eventBus;
    private final PlayQueueManager playQueueManager;
    private final PlaybackOperations playbackOperations;
    private final PlayerPresenter presenter;
    private final AdsOperations adsOperations;

    private final Observable<PlaybackProgressEvent> checkAdProgress;
    private final PlayerPagerScrollListener playerPagerScrollListener;
    private final Provider<PlayQueueDataSource> playQueueDataSwitcherProvider;

    private PlayQueueDataSource playQueueDataSource;
    private CompositeSubscription subscription;
    private Subscription unblockPagerSubscription = Subscriptions.empty();

    private PlayerTrackPager trackPager;
    private boolean isResumed;
    private boolean setPlayQueueAfterScroll;

    private final Handler changeTracksHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            playbackOperations.setPlayQueuePosition(getDisplayedPositionInPlayQueue());
        }
    };

    private final Func1<CurrentPlayQueueTrackEvent, Boolean> isCurrentTrackAudioAd = new Func1<CurrentPlayQueueTrackEvent, Boolean>() {
        @Override
        public Boolean call(CurrentPlayQueueTrackEvent ignored) {
            return adsOperations.isCurrentTrackAudioAd();
        }
    };

    private final Action1<Integer> updateAdapter = new Action1<Integer>() {
        @Override
        public void call(Integer integer) {
            adapter.onTrackChange();
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

    private final Action1<CurrentPlayQueueTrackEvent> unsubscribeFromUnblockPager = new Action1<CurrentPlayQueueTrackEvent>() {
        @Override
        public void call(CurrentPlayQueueTrackEvent currentPlayQueueTrackEvent) {
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
    public PlayerPagerController(TrackPagerAdapter adapter, PlayerPresenter playerPresenter, EventBus eventBus,
                                 PlayQueueManager playQueueManager, PlaybackOperations playbackOperations,
                                 Provider<PlayQueueDataSource> playQueueDataSwitcherProvider,
                                 PlayerPagerScrollListener playerPagerScrollListener, AdsOperations adsOperations) {
        this.adapter = adapter;
        this.presenter = playerPresenter;
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
        this.playbackOperations = playbackOperations;
        this.playQueueDataSwitcherProvider = playQueueDataSwitcherProvider;
        this.playerPagerScrollListener = playerPagerScrollListener;
        this.adsOperations = adsOperations;

        checkAdProgress = eventBus.queue(EventQueue.PLAYBACK_PROGRESS).first(new Func1<PlaybackProgressEvent, Boolean>() {
            @Override
            public Boolean call(PlaybackProgressEvent playbackProgressEvent) {
                return playbackProgressEvent.getPlaybackProgress().getPosition() >= AdConstants.UNSKIPPABLE_TIME_MS;
            }
        });
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
        adapter.onViewCreated(trackPager, getSkipListener(trackPager), new PlayerViewVisibilityProvider(trackPager));

        subscription = new CompositeSubscription();
        subscription.add(eventBus.subscribeImmediate(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber()));
        setupTrackChangeSubscribers();
        setupScrollingSubscribers();
    }

    private void setupTrackChangeSubscribers() {
        // setup audio ad
        subscription.add(eventBus.queue(EventQueue.PLAY_QUEUE_TRACK)
                .doOnNext(unsubscribeFromUnblockPager)
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

    void onDestroyView() {
        subscription.unsubscribe();
        unblockPagerSubscription.unsubscribe();
        adapter.onViewDestroyed();
        playerPagerScrollListener.unsubscribe();
        changeTracksHandler.removeMessages(CHANGE_TRACKS_MESSAGE);
        AnimUtils.clearAllAnimations();
    }

    private void setPager(final PlayerTrackPager trackPager) {
        this.presenter.initialize(trackPager);
        this.trackPager = trackPager;
        this.trackPager.setAdapter(adapter);
        refreshPlayQueue();

        playerPagerScrollListener.initialize(trackPager, adapter);
    }

    private SkipListener getSkipListener(final PlayerTrackPager trackPager) {
        return new SkipListener() {
            @Override
            public void onNext() {
                trackPager.setCurrentItem(trackPager.getCurrentItem() + 1);
                eventBus.publish(EventQueue.TRACKING, PlayControlEvent.skip(PlayControlEvent.SOURCE_FULL_PLAYER));
            }

            @Override
            public void onPrevious() {
                trackPager.setCurrentItem(trackPager.getCurrentItem() - 1);
                eventBus.publish(EventQueue.TRACKING, PlayControlEvent.previous(PlayControlEvent.SOURCE_FULL_PLAYER));
            }
        };
    }

    private void setQueuePosition(int position) {
        boolean isAdjacentTrack = Math.abs(trackPager.getCurrentItem() - position) <= 1;
        trackPager.setCurrentItem(position, isAdjacentTrack);
    }

    private void setFullQueue() {
        adapter.setCurrentData(playQueueDataSource.getFullQueue());
        trackPager.setCurrentItem(playQueueManager.getCurrentPosition(), false);
        setPlayQueueAfterScroll = false;
    }

    private void setAdPlayQueue() {
        adapter.setCurrentData(playQueueDataSource.getCurrentTrackAsQueue());
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
        return adapter.getPlayQueuePosition(trackPager.getCurrentItem());
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
            } else {
                refreshPlayQueue();
                setPlayQueueAfterScroll = false;
            }
        }
    }

    private boolean isLookingAtAdWithFullQueue() {
        return adapter.isAudioAdAtPosition(trackPager.getCurrentItem()) &&
                isResumed && adapter.getCount() > 1;
    }

    private final class SetQueueOnScrollSubscriber extends DefaultSubscriber<Integer> {
        @Override
        public void onNext(Integer args) {
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
        public void onNext(Integer args) {
            changeTracksHandler.removeMessages(CHANGE_TRACKS_MESSAGE);
            changeTracksHandler.sendEmptyMessageDelayed(CHANGE_TRACKS_MESSAGE, CHANGE_TRACKS_DELAY);
        }
    }

    private final class ShowAudioAdSubscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent>  {
        @Override
        public void onNext(CurrentPlayQueueTrackEvent args) {
            showAudioAd();
        }
    }

    private DefaultSubscriber<PlaybackProgressEvent> getRestoreQueueSubscriber() {
        return new DefaultSubscriber<PlaybackProgressEvent>(){
            @Override
            public void onNext(PlaybackProgressEvent args) {
                setFullQueue();
            }
        };
    }
}
