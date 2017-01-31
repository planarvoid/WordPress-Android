package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.playback.playqueue.TrackPlayQueueUIItem.ONLY_TRACKS;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlayQueueManager.RepeatMode;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlayableQueueItem;
import com.soundcloud.android.playback.PlaybackStateProvider;
import com.soundcloud.android.playback.PlaylistExploder;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PlayQueuePresenter {

    private static final Func1<TrackPlayQueueUIItem, TrackAndPlayQueueItem> TO_TRACK_AND_PLAY_QUEUE_ITEM = item ->
            new TrackAndPlayQueueItem(item.getTrackItem(), (TrackQueueItem) item.getPlayQueueItem());
    private static final int EXPLOSION_LOOK_AHEAD = 5;

    private final PlayQueueManager playQueueManager;
    private final PlaybackStateProvider playbackStateProvider;
    private final PlaySessionController playSessionController;
    private final PlayQueueOperations playQueueOperations;
    private final PlaylistExploder playlistExploder;

    private final EventBus eventBus;
    private final CompositeSubscription eventSubscriptions = new CompositeSubscription();

    private final PlayQueueUIItemMapper playQueueUIItemMapper;

    private Subscription updateSubscription = RxUtils.invalidSubscription();
    private Optional<PlayQueueView> playQueueView = Optional.absent();
    private Observable<List<TrackAndPlayQueueItem>> cachedTracks = Observable.empty();
    private Observable<Map<Urn, String>> cachedTitles = Observable.empty();
    private Optional<UndoHolder> undoHolder = Optional.absent();

    @Inject
    PlayQueuePresenter(PlayQueueManager playQueueManager,
                       PlaybackStateProvider playbackStateProvider,
                       PlaySessionController playSessionController,
                       PlayQueueOperations playQueueOperations,
                       PlaylistExploder playlistExploder, EventBus eventBus,
                       PlayQueueUIItemMapper playQueueUIItemMapper) {
        this.playQueueManager = playQueueManager;
        this.playbackStateProvider = playbackStateProvider;
        this.playSessionController = playSessionController;
        this.playQueueOperations = playQueueOperations;
        this.playlistExploder = playlistExploder;
        this.eventBus = eventBus;
        this.playQueueUIItemMapper = playQueueUIItemMapper;
    }

    public void trackClicked(int listPosition) {
        if (playQueueView.get().getItem(listPosition).isTrack()) {
            playQueueView.get().updateNowPlaying(listPosition, true, playbackStateProvider.isSupposedToBePlaying());
            playQueueManager.setCurrentPlayQueueItem(((TrackPlayQueueUIItem) playQueueView.get().getItem(listPosition)).getPlayQueueItem());
            if (!playSessionController.isPlayingCurrentPlayQueueItem()) {
                playSessionController.play();
            } else {
                playSessionController.togglePlayback();
            }
        }
    }

    public void undoClicked() {
        if (playQueueView.isPresent() && undoHolder.isPresent()) {
            UndoHolder undoHolder = this.undoHolder.get();
            playQueueManager.insertItemAtPosition(undoHolder.playQueuePosition, undoHolder.playQueueItem);
            playQueueView.get().addItem(undoHolder.adapterPosition, undoHolder.playQueueUIItem);

            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueRemoveUndo(Screen.PLAY_QUEUE));
        }
    }

    public void nowPlayingChanged(TrackPlayQueueUIItem trackItem) {
        if (playQueueView.isPresent()) {
            if (trackItem.isGoTrack()) {
                playQueueView.get().setGoPlayerStrip();
            } else {
                playQueueView.get().setDefaultPlayerStrip();
            }
        }
    }

    void attachView(PlayQueueView playQueueView) {
        this.playQueueView = Optional.of(playQueueView);
        if (this.playQueueView.isPresent()) {
            this.playQueueView.get().setRepeatMode(playQueueManager.getRepeatMode());
            this.playQueueView.get().setShuffledState(playQueueManager.isShuffled());
            loadPlayQueueUIItems();
            subscribeToEvents();
        }
    }

    void detachContract() {
        playQueueView = Optional.absent();
        eventSubscriptions.clear();
        updateSubscription.unsubscribe();
    }

    private void subscribeToEvents() {
        eventSubscriptions.add(eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                                       .filter(currentPlayQueueItemEvent -> !isPlayingCurrent())
                                       .flatMap(event -> fetchPlayQueueUIItems())
                                       .observeOn(AndroidSchedulers.mainThread())
                                       .subscribe(new UpdateNowPlayingSubscriber()));
        eventSubscriptions.add(eventBus.queue(EventQueue.PLAY_QUEUE)
                                       .filter(playQueueEvent -> !playQueueEvent.itemChanged())
                                       .observeOn(AndroidSchedulers.mainThread())
                                       .subscribe(new ChangePlayQueueSubscriber()));
        eventSubscriptions.add(eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                                       .observeOn(AndroidSchedulers.mainThread())
                                       .subscribe(e -> setNowPlaying(false)));
    }

    void closePlayQueue() {
        eventBus.publish(EventQueue.PLAY_QUEUE_UI, PlayQueueUIEvent.createHideEvent());
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueClose());
    }

    void onNextClick() {
        if (playQueueView.isPresent()) {
            playQueueView.get().scrollTo(getScrollPosition());
        }
    }

    private void loadPlayQueueUIItems() {
        setCachedObservables();
        updateSubscription = fetchPlayQueueUIItems().subscribe(new PlayQueueSubscriber());
    }


    private void setCachedObservables() {
        cachedTracks = playQueueOperations.getTracks().cache();
        cachedTitles = playQueueOperations.getContextTitles().cache();
    }

    private Observable<List<PlayQueueUIItem>> fetchPlayQueueUIItems() {
        return cachedTracks
                .zipWith(cachedTitles, playQueueUIItemMapper)
                .observeOn(AndroidSchedulers.mainThread());
    }

    private int getScrollPosition() {

        int currentPlayQueuePosition = playQueueView.get().getAdapterPosition(playQueueManager.getCurrentPlayQueueItem());

        if (currentPlayQueuePosition > 0) {
            currentPlayQueuePosition -= 1;
        } else if (currentPlayQueuePosition < 2) {
            return 0;
        } else {
            return currentPlayQueuePosition - 2;
        }

        return currentPlayQueuePosition;
    }

    void repeatClicked() {
        if (playQueueView.isPresent()) {
            final RepeatMode nextRepeatMode = getNextRepeatMode();
            playQueueManager.setRepeatMode(nextRepeatMode);
            if (playQueueView.isPresent()) {
                playQueueView.get().setRepeatMode(nextRepeatMode);
            }
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueRepeat(Screen.PLAY_QUEUE, nextRepeatMode));
        }
    }

    void shuffleClicked(boolean isShuffled) {
        if (isShuffled) {
            playQueueManager.shuffle();
        } else {
            playQueueManager.unshuffle();
        }

        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueShuffle(isShuffled));
    }

    private RepeatMode getNextRepeatMode() {
        final RepeatMode[] repeatModes = RepeatMode.values();
        final int currentOrdinal = playQueueManager.getRepeatMode().ordinal();
        return repeatModes[(currentOrdinal + 1) % repeatModes.length];
    }

    boolean isRemovable(int adapterPosition) {
        if (playQueueView.isPresent()) {
            if (adapterPosition >= 0 && adapterPosition < playQueueView.get().getItemCount()) {
                final PlayQueueUIItem item = playQueueView.get().getItem(adapterPosition);
                return item.isTrack() && PlayState.COMING_UP.equals(item.getPlayState());
            }
        }
        return false;
    }

    public void remove(int adapterPosition) {
        if (playQueueView.isPresent()) {
            final PlayQueueUIItem adapterItem = playQueueView.get().getItem(adapterPosition);

            if (adapterItem.isTrack()) {
                final PlayQueueItem playQueueItem = ((TrackPlayQueueUIItem) adapterItem).getPlayQueueItem();
                final int playQueuePosition = playQueueManager.indexOfPlayQueueItem(playQueueItem);

                playQueueView.get().removeItem(adapterPosition);
                undoHolder = Optional.of(new UndoHolder(playQueueItem, playQueuePosition, adapterItem, adapterPosition));
                if (playQueuePosition >= 0) {
                    playQueueManager.removeItem(playQueueItem);
                    playQueueView.get().showUndo();
                    setCachedObservables();
                }

                rebuildLabels();
                eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueRemove(Screen.PLAY_QUEUE));
            }
        }
    }

    void switchItems(int fromPosition, int toPosition) {
        if (playQueueView.isPresent()) {
            playQueueView.get().switchItems(fromPosition, toPosition);
        }
    }

    void moveItems(int fromAdapterPosition, int toAdapterPosition) {
        if (playQueueView.isPresent()) {
            playQueueManager.moveItem(playQueueView.get().getQueuePosition(fromAdapterPosition),
                                      playQueueView.get().getQueuePosition(toAdapterPosition));
            rebuildLabels();
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueReorder(Screen.PLAY_QUEUE));
        }
    }

    void scrollDown(int lastVisibleItemPosition) {
        playlistExploder.explodePlaylists(lastVisibleItemPosition, EXPLOSION_LOOK_AHEAD);
    }

    void scrollUp(int firstVisibleItemPosition) {
        int resolvedPosition = (firstVisibleItemPosition -EXPLOSION_LOOK_AHEAD < 0) ? 0 : firstVisibleItemPosition -EXPLOSION_LOOK_AHEAD;
        playlistExploder.explodePlaylists(resolvedPosition, EXPLOSION_LOOK_AHEAD);
    }

    private void rebuildLabels() {
        rebuildPlayQueueUIItemsObservable(playQueueView.get().getItems())
                .subscribe(new RebuildSubscriber());
    }

    @VisibleForTesting
    Observable<List<PlayQueueUIItem>> rebuildPlayQueueUIItemsObservable(List<PlayQueueUIItem> uiItems) {
        return uiItemsFromExistingTitles(uiItems, uiItemsToTrackAndPlayQueue(uiItems));
    }

    private Observable<List<PlayQueueUIItem>> rebuildPlayQueueUIItemsObservable(List<PlayQueueUIItem> uiItems, List<PlayQueueItem> pqItems) {
        return uiItemsFromExistingTitles(uiItems, uiItemsToTrackAndPlayQueue(uiItems)
                .sorted((a, b) -> pqItems.indexOf(a.playQueueItem) - pqItems.indexOf(b.playQueueItem)));
    }

    private Observable<TrackAndPlayQueueItem> uiItemsToTrackAndPlayQueue(List<PlayQueueUIItem> uiItems) {
        return Observable.from(uiItems)
                         .filter(ONLY_TRACKS)
                         .cast(TrackPlayQueueUIItem.class)
                         .map(TO_TRACK_AND_PLAY_QUEUE_ITEM);
    }

    private Observable<List<PlayQueueUIItem>> uiItemsFromExistingTitles(List<PlayQueueUIItem> uiItems, Observable<TrackAndPlayQueueItem> observable) {
        final Map<Urn, String> existingTitles = buildTitlesMap(uiItems);
        return observable.toList()
                         .zipWith(Observable.just(existingTitles), playQueueUIItemMapper);
    }

    private Map<Urn, String> buildTitlesMap(List<PlayQueueUIItem> items) {
        final Map<Urn, String> titles = new HashMap<>();

        for (PlayQueueUIItem item : items) {
            if (item.isTrack()) {
                final TrackPlayQueueUIItem trackUIItem = (TrackPlayQueueUIItem) item;
                final Optional<String> contextTitle = trackUIItem.getContextTitle();
                if (contextTitle.isPresent()) {
                    final PlayableQueueItem playQueueItem = (PlayableQueueItem) trackUIItem.getPlayQueueItem();
                    final Optional<Urn> urn = playQueueItem.getPlaybackContext().urn();
                    if (urn.isPresent()) {
                        titles.put(urn.get(), contextTitle.get());
                    }
                }
            }
        }
        return titles;
    }

    private void rebuildAdapter(List<PlayQueueUIItem> items) {
        if (playQueueView.isPresent()) {
            playQueueView.get().clear();

            for (PlayQueueUIItem item : items) {
                playQueueView.get().addItem(item);
            }

            playQueueView.get().notifyDataSetChanged();
        }
    }

    private boolean isPlayingCurrent() {
        final int adapterPosition = playQueueView.get().getAdapterPosition(playQueueManager.getCurrentPlayQueueItem());
        final boolean isWithinRange = adapterPosition < playQueueView.get().getItems().size() && adapterPosition >= 0;
        return isWithinRange && playQueueView.get().getItem(adapterPosition).isPlayingOrPaused();
    }

    private class UpdateNowPlayingSubscriber extends DefaultSubscriber<List<PlayQueueUIItem>> {

        @Override
        public void onNext(List<PlayQueueUIItem> items) {
            if (items.size() != playQueueView.get().getItemCount()) {
                rebuildAdapter(items);
            }
            if (playQueueView.isPresent()) {
                setNowPlaying(true);
            }
        }
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<List<PlayQueueUIItem>> {

        @Override
        public void onNext(List<PlayQueueUIItem> items) {
            if (playQueueView.isPresent()) {
                playQueueView.get().removeLoadingIndicator();
                boolean wasEmpty = playQueueView.get().isEmpty();
                rebuildAdapter(items);
                if (wasEmpty) {
                    playQueueView.get().scrollTo(getScrollPosition());
                }
                setNowPlaying(true);
            }
        }

    }

    private void setNowPlaying(boolean notifyListener) {
        if (playQueueView.isPresent()) {
            final int adapterPosition = playQueueView.get().getAdapterPosition(playQueueManager.getCurrentPlayQueueItem());
            playQueueView.get().updateNowPlaying(adapterPosition, notifyListener, playbackStateProvider.isSupposedToBePlaying());
        }
    }

    private class RebuildSubscriber extends DefaultSubscriber<List<PlayQueueUIItem>> {

        @Override
        public void onNext(List<PlayQueueUIItem> items) {
            if (playQueueView.isPresent()) {
                rebuildAdapter(items);
                setNowPlaying(false);
            }
        }
    }

    private class ChangePlayQueueSubscriber extends DefaultSubscriber<PlayQueueEvent> {

        @Override
        public void onNext(PlayQueueEvent playQueueEvent) {
            if (!playQueueView.get().isEmpty() && playQueueEvent.isQueueReorder()) {
                rebuildPlayQueueUIItemsObservable(playQueueView.get().getItems(), playQueueOperations.getQueueTracks())
                        .subscribe(new RebuildSubscriber());
            } else {
                updateSubscription.unsubscribe();
                loadPlayQueueUIItems();
            }
        }
    }

    private class UndoHolder {
        private final PlayQueueItem playQueueItem;
        private final int playQueuePosition;
        private final PlayQueueUIItem playQueueUIItem;
        private final int adapterPosition;

        public UndoHolder(PlayQueueItem playQueueItem, int playQueuePosition, PlayQueueUIItem playQueueUIItem, int adapterPosition) {
            this.playQueueItem = playQueueItem;
            this.playQueuePosition = playQueuePosition;
            this.playQueueUIItem = playQueueUIItem;
            this.adapterPosition = adapterPosition;
        }

    }

}
