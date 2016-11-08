package com.soundcloud.android.playback.ui;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.PlayerAdData;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.playqueue.PlayQueueFragment;
import com.soundcloud.android.playback.playqueue.PlayQueueFragmentFactory;
import com.soundcloud.android.playback.playqueue.PlayQueueUIEvent;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycles;
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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class PlayerPresenter extends SupportFragmentLightCycleDispatcher<PlayerFragment> {

    private static final int CHANGE_TRACKS_MESSAGE = 0;
    private static final int CHANGE_TRACKS_DELAY = 350;
    private static final int UNSKIPPABLE_UNLOCK = 500;

    @LightCycle final PlayerPagerPresenter presenter;

    private final EventBus eventBus;
    private final PlayQueueManager playQueueManager;
    private final PlaySessionController playSessionController;
    private final AdsOperations adsOperations;
    private final PlayQueueFragmentFactory playQueueFragmentFactory;

    private final PlayerPagerScrollListener playerPagerScrollListener;

    private CompositeSubscription subscription = new CompositeSubscription();
    private Subscription unblockPagerSubscription = RxUtils.invalidSubscription();
    private Handler changeTracksHandler;

    private boolean isResumed;
    private boolean setPlayQueueAfterScroll;
    private FragmentManager fragmentManager;

    private final Func1<CurrentPlayQueueItemEvent, Boolean> isCurrentItemAd = new Func1<CurrentPlayQueueItemEvent, Boolean>() {
        @Override
        public Boolean call(CurrentPlayQueueItemEvent currentItemEvent) {
            return currentItemEvent.getCurrentPlayQueueItem().isAd();
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

    private final Action1<CurrentPlayQueueItemEvent> allowScrollAfterAdSkipTimeout = new Action1<CurrentPlayQueueItemEvent>() {
        @Override
        public void call(CurrentPlayQueueItemEvent currentItemEvent) {
            unblockPagerSubscription = eventBus.queue(EventQueue.PLAYBACK_PROGRESS)
                    .first(isBecomingSkippable)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(getRestoreQueueSubscriber());
        }
    };

    private final Func1<PlaybackProgressEvent, Boolean> isBecomingSkippable = new Func1<PlaybackProgressEvent, Boolean>() {
        @Override
        public Boolean call(PlaybackProgressEvent playbackProgressEvent) {
            boolean isSkippableAd = ((PlayerAdData) adsOperations.getCurrentTrackAdData().get()).isSkippable();
            PlaybackProgress progress = playbackProgressEvent.getPlaybackProgress();
            return (isSkippableAd && progress.isPastPosition(AdConstants.UNSKIPPABLE_TIME_MS))
                    || progress.isPastPosition(progress.getDuration() - UNSKIPPABLE_UNLOCK);
        }
    };

    private final Action1<CurrentPlayQueueItemEvent> onItemChanged = new Action1<CurrentPlayQueueItemEvent>() {
        @Override
        public void call(CurrentPlayQueueItemEvent currentItemEvent) {
            presenter.onTrackChange();
            unblockPagerSubscription.unsubscribe();
        }
    };

    private final Func1<CurrentPlayQueueItemEvent, Boolean> notWaitingForScroll = new Func1<CurrentPlayQueueItemEvent, Boolean>() {
        @Override
        public Boolean call(CurrentPlayQueueItemEvent currentItemEvent) {
            return !setPlayQueueAfterScroll;
        }
    };

    private static final Predicate<PlayQueueItem> IS_PLAYABLE = new Predicate<PlayQueueItem>() {
        @Override
        public boolean apply(PlayQueueItem input) {
            return input.isTrack() || input.isAd();
        }
    };

    @Inject
    public PlayerPresenter(PlayerPagerPresenter presenter,
                           EventBus eventBus,
                           PlayQueueManager playQueueManager,
                           PlaySessionController playSessionController,
                           PlayerPagerScrollListener playerPagerScrollListener,
                           final AdsOperations adsOperations,
                           PlayQueueFragmentFactory playQueueFragmentFactory) {
        this.presenter = presenter;
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
        this.playSessionController = playSessionController;
        this.playerPagerScrollListener = playerPagerScrollListener;
        this.adsOperations = adsOperations;
        this.playQueueFragmentFactory = playQueueFragmentFactory;

        LightCycles.bind(this);

        changeTracksHandler = new ChangeTracksHandler(this);
    }

    private void setPositionToDisplayedTrack() {
        playSessionController.setCurrentPlayQueueItem(getDisplayedItem());
    }

    public void onPlayerSlide(float slideOffset) {
        presenter.onPlayerSlide(slideOffset);
    }

    @Override
    public void onCreate(PlayerFragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        fragmentManager = fragment.getFragmentManager();
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

        setPager(fragment.getPlayerPager());
        setupTrackChangeSubscribers();
        setupScrollingSubscribers();
    }

    private void setupTrackChangeSubscribers() {

        subscription.add(eventBus.subscribeImmediate(EventQueue.PLAY_QUEUE_UI, new PlayQueueVisibilitySubscriber()));

        // play queue changes
        subscription.add(eventBus.subscribeImmediate(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber()));

        // setup player ad
        subscription.add(eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                .doOnNext(onItemChanged)
                .filter(isCurrentItemAd)
                .doOnNext(allowScrollAfterAdSkipTimeout)
                .subscribe(new ShowAudioAdSubscriber()));

        // set position from track change
        subscription.add(eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                .filter(notWaitingForScroll)
                .subscribe(new UpdateCurrentTrackSubscriber()));
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

        super.onDestroyView(playerFragment);
    }

    public boolean handleBackPressed() {
        Fragment fragment = fragmentManager.findFragmentByTag(PlayQueueFragment.TAG);
        if (fragment == null) {
            return false;
        } else {
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.ak_fade_in, R.anim.ak_fade_out)
                    .remove(fragment)
                    .commitAllowingStateLoss();
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.unlockPlayQueue());
            return true;
        }
    }

    private void setPager(final PlayerTrackPager trackPager) {
        refreshPlayQueue();
        playerPagerScrollListener.initialize(trackPager, presenter);
    }

    private void setFullQueue() {
        final List<PlayQueueItem> playQueueItems = playQueueManager.getPlayQueueItems(IS_PLAYABLE);
        final int indexOfCurrentPlayQueueitem = getIndexOfPlayQueueItem(playQueueItems);

        presenter.setCurrentPlayQueue(playQueueItems, indexOfCurrentPlayQueueitem);
        presenter.setCurrentItem(indexOfCurrentPlayQueueitem, false);
        setPlayQueueAfterScroll = false;
    }

    private int getIndexOfCurrentPlayQueueitem() {
        return getIndexOfPlayQueueItem(presenter.getCurrentPlayQueue());
    }

    private int getIndexOfPlayQueueItem(List<PlayQueueItem> playQueue) {
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        return Iterables.indexOf(playQueue, PlayQueue.isMatchingItem(currentPlayQueueItem));
    }

    private void setAdPlayQueue() {
        presenter.setCurrentPlayQueue(newArrayList(playQueueManager.getCurrentPlayQueueItem()), 0);
        presenter.setCurrentItem(0, false);
    }

    private void showAd() {
        if (isShowingCurrentAd() || !isResumed) {
            setAdPlayQueue();
        } else {
            setPlayQueueAfterScroll = true;
            setQueuePositionToCurrent();
        }
    }

    private void setQueuePositionToCurrent() {
        int position = getIndexOfCurrentPlayQueueitem();
        boolean isAdjacentTrack = Math.abs(presenter.getCurrentItemPosition() - position) <= 1;
        presenter.setCurrentItem(position, isAdjacentTrack);
    }

    private boolean isShowingCurrentAd() {
        return adsOperations.isCurrentItemAd()
                && playQueueManager.isCurrentItem(getDisplayedItem());
    }

    private PlayQueueItem getDisplayedItem() {
        return presenter.getCurrentItem();
    }

    private void refreshPlayQueue() {
        setFullQueue();
    }

    private final class PlayQueueVisibilitySubscriber extends DefaultSubscriber<PlayQueueUIEvent> {

        @Override
        public void onNext(PlayQueueUIEvent playQueueUIEvent) {
            Fragment fragment = fragmentManager.findFragmentByTag(PlayQueueFragment.TAG);
            if (playQueueUIEvent.isDisplayEvent()) {
                if (fragment == null) {
                    eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.lockPlayQueue());
                    fragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.ak_fade_in, R.anim.ak_fade_out)
                            .add(R.id.player_pager_holder,
                                    playQueueFragmentFactory.create(),
                                    PlayQueueFragment.TAG)
                            .commitAllowingStateLoss();
                }
            } else if (playQueueUIEvent.isHideEvent()) {
                if (fragment != null) {
                    fragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.ak_fade_in, R.anim.ak_fade_out)
                            .remove(fragment)
                            .commitAllowingStateLoss();
                    eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.unlockPlayQueue());
                }
            }
        }

    }

    private final class PlayQueueSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent event) {
            if (event.adsRemoved() && isLookingAtAdWithFullQueue()) {
                setPlayQueueAfterScroll = true;
                presenter.setCurrentItem(presenter.getCurrentItemPosition() + 1, true);
            } else if (!setPlayQueueAfterScroll) {
                refreshPlayQueue();
            }
        }
    }

    private boolean isLookingAtAdWithFullQueue() {
        return presenter.getCurrentItem().isAd() && isResumed && presenter.getCount() > 1;
    }

    private final class SetQueueOnScrollSubscriber extends DefaultSubscriber<Integer> {
        @Override
        public void onNext(Integer ignored) {
            if (setPlayQueueAfterScroll) {
                if (adsOperations.isCurrentItemAd()) {
                    setAdPlayQueue();
                } else {
                    refreshPlayQueue();
                }
            }
        }
    }

    private final class ChangeTracksSubscriber extends DefaultSubscriber<Integer> {
        @Override
        public void onNext(Integer ignored) {
            changeTracksHandler.removeMessages(CHANGE_TRACKS_MESSAGE);
            changeTracksHandler.sendEmptyMessageDelayed(CHANGE_TRACKS_MESSAGE, CHANGE_TRACKS_DELAY);
        }
    }

    private final class ShowAudioAdSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {
        @Override
        public void onNext(CurrentPlayQueueItemEvent ignored) {
            showAd();
        }
    }

    private DefaultSubscriber<PlaybackProgressEvent> getRestoreQueueSubscriber() {
        return new DefaultSubscriber<PlaybackProgressEvent>() {
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

    private class UpdateCurrentTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {
        @Override
        public void onNext(CurrentPlayQueueItemEvent args) {
            setQueuePositionToCurrent();
        }
    }
}
