package com.soundcloud.android.playback.ui;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.PlayableAdData;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
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
import com.soundcloud.android.rx.observers.DefaultMaybeObserver;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

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

    private final EventBusV2 eventBus;
    private final PlayQueueManager playQueueManager;
    private final PlaySessionController playSessionController;
    private final AdsOperations adsOperations;
    private final PlayQueueFragmentFactory playQueueFragmentFactory;

    private final PlayerPagerScrollListener playerPagerScrollListener;

    private final CompositeDisposable disposables = new CompositeDisposable();
    private Disposable unblockPagerDisposable = RxUtils.emptyDisposable();
    private final Handler changeTracksHandler;
    private boolean isResumed;
    private boolean isPlayQueueVisible;
    private boolean setPlayQueueAfterScroll;
    private FragmentManager fragmentManager;

    @Inject
    PlayerPresenter(PlayerPagerPresenter presenter,
                    EventBusV2 eventBus,
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
        this.changeTracksHandler = new ChangeTracksHandler(this);
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
        disposables.add(eventBus.queue(EventQueue.PLAYER_UI)
                                .filter(PlayerUIEvent.PLAYER_IS_COLLAPSED_V2)
                                .map(playerUIEvent -> playerUIEvent.getKind() == PlayerUIEvent.PLAYER_COLLAPSED ?
                                                      PlayQueueUIEvent.createHideEvent() : PlayQueueUIEvent.createDisplayEvent())
                                .subscribeWith(new PlayQueueVisibilitySubscriber()));
    }

    @Override
    public void onPause(PlayerFragment fragment) {
        isResumed = false;
        super.onPause(fragment);
    }

    @Override
    public void onViewCreated(PlayerFragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        setPager(fragment.getPlayerPager());
        setupTrackChangeSubscribers();
        setupScrollingSubscribers();
    }

    @Override
    public void onDestroyView(PlayerFragment playerFragment) {
        unblockPagerDisposable.dispose();
        playerPagerScrollListener.unsubscribe();
        changeTracksHandler.removeMessages(CHANGE_TRACKS_MESSAGE);
        disposables.clear();
        super.onDestroyView(playerFragment);
    }

    private void setPositionToDisplayedTrack() {
        if (isResumed) {
            playSessionController.setCurrentPlayQueueItem(getDisplayedItem());
        }
    }

    private void setupTrackChangeSubscribers() {
        disposables.add(eventBus.subscribeImmediate(EventQueue.PLAY_QUEUE_UI, new PlayQueueVisibilitySubscriber()));

        // play queue changes
        disposables.add(eventBus.subscribeImmediate(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber()));

        // setup player ad
        disposables.add(eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                                .doOnNext(itemChangeEvent -> {
                                    presenter.onTrackChange();
                                    unblockPagerDisposable.dispose();
                                })
                                .filter(currentItemEvent -> currentItemEvent.getCurrentPlayQueueItem().isAd())
                                .doOnNext(allowScrollAfterAdSkipTimeout)
                                .subscribeWith(new ShowAudioAdSubscriber()));

        // set position from track change
        disposables.add(eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                                .filter(event -> !isPlayQueueVisible)
                                .filter(currentItemEvent -> !setPlayQueueAfterScroll)
                                .subscribeWith(new UpdateCurrentTrackSubscriber()));
    }

    private final Consumer<CurrentPlayQueueItemEvent> allowScrollAfterAdSkipTimeout = new Consumer<CurrentPlayQueueItemEvent>() {
        @Override
        public void accept(CurrentPlayQueueItemEvent currentItemEvent) {
            unblockPagerDisposable = eventBus.queue(EventQueue.PLAYBACK_PROGRESS)
                                             .filter(playbackProgressEvent -> isBecomingSkippable(playbackProgressEvent))
                                             .firstElement()
                                             .observeOn(AndroidSchedulers.mainThread())
                                             .subscribeWith(new DefaultMaybeObserver<PlaybackProgressEvent>() {
                                                 @Override
                                                 public void onSuccess(@NonNull PlaybackProgressEvent playbackProgressEvent) {
                                                     setFullQueue();
                                                 }
                                             });
        }
    };

    private boolean isBecomingSkippable(PlaybackProgressEvent playbackProgressEvent) {
        boolean isSkippableAd = ((PlayableAdData) adsOperations.getCurrentTrackAdData().get()).isSkippable();
        PlaybackProgress progress = playbackProgressEvent.getPlaybackProgress();
        return (isSkippableAd && progress.isPastPosition(AdConstants.UNSKIPPABLE_TIME_MS))
                || progress.isPastPosition(progress.getDuration() - UNSKIPPABLE_UNLOCK);
    }

    private void setupScrollingSubscribers() {
        final Observable<Integer> pageChangedObservable = playerPagerScrollListener.getPageChangedObservable();

        disposables.add(pageChangedObservable
                                .subscribeWith(new SetQueueOnScrollSubscriber()));

        disposables.add(pageChangedObservable
                                .doOnNext(ignoredObject -> presenter.onTrackChange())
                                .filter(ignoredObject -> isResumed)
                                .subscribeWith(new ChangeTracksSubscriber()));
    }

    private void setPager(final PlayerTrackPager trackPager) {
        refreshPlayQueue();
        playerPagerScrollListener.initialize(trackPager, presenter);
    }

    private void setFullQueue() {
        final List<PlayQueueItem> playQueueItems = playQueueManager.getPlayQueueItems(input -> input.isTrack() || input.isAd());
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

    private boolean isLookingAtAdWithFullQueue() {
        return presenter.getCurrentItem().isAd() && isResumed && presenter.getCount() > 1;
    }

    private void removePlayQueue(Fragment fragment) {
        if (fragment != null) {
            fragmentManager.beginTransaction()
                           .setCustomAnimations(R.anim.ak_fade_in, R.anim.ak_fade_out)
                           .remove(fragment)
                           .commitAllowingStateLoss();
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.unlockPlayQueue());
        }
    }

    private void addPlayQueue(Fragment fragment) {
        if (fragment == null) {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.lockPlayQueue());
            fragmentManager.beginTransaction()
                           .setCustomAnimations(R.anim.ak_fade_in, R.anim.ak_fade_out)
                           .add(R.id.player_pager_holder,
                                playQueueFragmentFactory.create(),
                                PlayQueueFragment.TAG)
                           .commitAllowingStateLoss();
        }
    }

    boolean handleBackPressed() {
        Fragment fragment = fragmentManager.findFragmentByTag(PlayQueueFragment.TAG);
        if (fragment == null) {
            return false;
        } else {
            isPlayQueueVisible = false;
            setQueuePositionToCurrent();
            removePlayQueue(fragment);
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueClose());
            return true;
        }
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

    private final class PlayQueueVisibilitySubscriber extends DefaultObserver<PlayQueueUIEvent> {
        @Override
        public void onNext(PlayQueueUIEvent playQueueUIEvent) {
            Fragment fragment = fragmentManager.findFragmentByTag(PlayQueueFragment.TAG);
            if (playQueueUIEvent.isDisplayEvent()) {
                isPlayQueueVisible = true;
                addPlayQueue(fragment);
            } else if (playQueueUIEvent.isHideEvent()) {
                isPlayQueueVisible = false;
                setQueuePositionToCurrent();
                removePlayQueue(fragment);
            }
        }

    }

    private final class PlayQueueSubscriber extends DefaultObserver<PlayQueueEvent> {
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

    private final class SetQueueOnScrollSubscriber extends DefaultObserver<Integer> {
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

    private final class ChangeTracksSubscriber extends DefaultObserver<Integer> {
        @Override
        public void onNext(Integer ignored) {
            changeTracksHandler.removeMessages(CHANGE_TRACKS_MESSAGE);
            changeTracksHandler.sendEmptyMessageDelayed(CHANGE_TRACKS_MESSAGE, CHANGE_TRACKS_DELAY);
        }
    }

    private final class ShowAudioAdSubscriber extends DefaultObserver<CurrentPlayQueueItemEvent> {
        @Override
        public void onNext(CurrentPlayQueueItemEvent ignored) {
            showAd();
        }
    }

    private class UpdateCurrentTrackSubscriber extends DefaultObserver<CurrentPlayQueueItemEvent> {
        @Override
        public void onNext(CurrentPlayQueueItemEvent args) {
            setQueuePositionToCurrent();
        }
    }
}
