package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricParams;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.EventQueue;
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
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PlayQueuePresenter {

    private static final int EXPLOSION_LOOK_AHEAD = 5;

    private final PlayQueueManager playQueueManager;
    private final PlaybackStateProvider playbackStateProvider;
    private final PlaySessionController playSessionController;
    private final PlaylistExploder playlistExploder;
    private final PlayQueueDataProvider playQueueDataProvider;
    private final EventBus eventBus;
    private final PlayQueueUIItemMapper playQueueUIItemMapper;
    private final PerformanceMetricsEngine performanceMetricsEngine;
    private final CompositeSubscription subscriptions = new CompositeSubscription();
    private final Subject<Boolean, Boolean> rebuildSubject = PublishSubject.create();

    private Optional<PlayQueueView> playQueueView = Optional.absent();
    private Optional<UndoHolder> undoHolder = Optional.absent();
    private List<PlayQueueUIItem> items = new ArrayList<>();
    private boolean resetUI = true;

    @Inject
    PlayQueuePresenter(PlayQueueManager playQueueManager,
                       PlaybackStateProvider playbackStateProvider,
                       PlaySessionController playSessionController,
                       PlayQueueDataProvider dataProvider,
                       PlaylistExploder playlistExploder,
                       EventBus eventBus,
                       PlayQueueUIItemMapper playQueueUIItemMapper,
                       PerformanceMetricsEngine performanceMetricsEngine) {
        this.playQueueManager = playQueueManager;
        this.playbackStateProvider = playbackStateProvider;
        this.playSessionController = playSessionController;
        this.playQueueDataProvider = dataProvider;
        this.playlistExploder = playlistExploder;
        this.eventBus = eventBus;
        this.playQueueUIItemMapper = playQueueUIItemMapper;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    void attachView(PlayQueueView playQueueView) {
        this.playQueueView = Optional.of(playQueueView);
        performanceMetricsEngine.startMeasuring(MetricType.PLAY_QUEUE_LOAD);
        playQueueView.setShuffledState(playQueueManager.isShuffled());
        setRepeatMode(playQueueManager.getRepeatMode());


        if (items.isEmpty()) {
            this.playQueueView.get().showLoadingIndicator();
        }
        subscriptions.add(playQueueDataProvider.getPlayQueueUIItems()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(newItems -> items = newItems)
                .subscribe(new PlayQueueSubscriber()));
        setUpPlaybackStream();
        setUpRebuildStream();
    }

    void detachContract() {
        playQueueView = Optional.absent();
        subscriptions.clear();
        resetUI = true;
    }

    private void setUpPlaybackStream() {
        subscriptions.add(eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .map(event -> items)
                .subscribe(new PlayQueueSubscriber()));
    }

    private void setUpRebuildStream() {
        subscriptions.add(rebuildSubject.map(ignored -> items)
                .flatMap(items -> Observable.zip(createTracksFromItems(items), createTitlesFromItems(items), playQueueUIItemMapper))
                .doOnNext(newItems -> items = newItems)
                .doOnNext(items -> setNowPlaying())
                .subscribe(new PlayQueueSubscriber()));
    }

    Observable<List<TrackAndPlayQueueItem>> createTracksFromItems(List<PlayQueueUIItem> playQueueUIItems) {
        return Observable.just(playQueueUIItems)
                .map(items -> Iterables.filter(items, input -> input.isTrack()))
                .map(trackItems -> Lists.newArrayList(Iterables.transform(trackItems, input -> {
                    TrackPlayQueueUIItem item = (TrackPlayQueueUIItem) input;
                    return new TrackAndPlayQueueItem(item.getTrackItem(), (TrackQueueItem) item.getPlayQueueItem());
                })));
    }

    public void undoClicked() {
        if (playQueueView.isPresent() && undoHolder.isPresent()) {
            UndoHolder undoHolder = this.undoHolder.get();
            items.addAll(undoHolder.adapterPosition, undoHolder.playQueueUIItems);
            rebuildSubject.onNext(true);
            playQueueManager.insertItemsAtPosition(undoHolder.playQueuePosition, undoHolder.playQueueItems);

            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueRemoveUndo(Screen.PLAY_QUEUE));
        }
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

    public void trackClicked(int listPosition) {
        if (items.get(listPosition).isTrack()) {
            TrackPlayQueueUIItem trackItem = (TrackPlayQueueUIItem) items.get(listPosition);
            PerformanceMetric performanceMetric = PerformanceMetric.create(MetricType.TIME_TO_PLAY);

            updateNowPlaying(listPosition, playbackStateProvider.isSupposedToBePlaying());
            playQueueView.get().setItems(items);
            playQueueManager.setCurrentPlayQueueItem(trackItem.getPlayQueueItem());

            if (trackItem.isGoTrack()) {
                playQueueView.get().setGoPlayerStrip();
            } else {
                playQueueView.get().setDefaultPlayerStrip();
            }

            if (playSessionController.isPlayingCurrentPlayQueueItem()) {
                playSessionController.togglePlayback();
            } else {
                performanceMetricsEngine.startMeasuring(performanceMetric);
                playSessionController.play();
            }
        }
    }

    private int getScrollPosition() {

        int currentPlayQueuePosition = getAdapterPosition(playQueueManager.getCurrentPlayQueueItem());

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
        final RepeatMode nextRepeatMode = getNextRepeatMode();
        playQueueManager.setRepeatMode(nextRepeatMode);
        setRepeatMode(nextRepeatMode);
        updateItemsInRepeatMode(nextRepeatMode);
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueRepeat(Screen.PLAY_QUEUE, nextRepeatMode));
    }

    private void setRepeatMode(RepeatMode nextRepeatMode) {
        if (playQueueView.isPresent()) {
            switch (nextRepeatMode) {
                case REPEAT_ONE:
                    playQueueView.get().setRepeatOne();
                    break;
                case REPEAT_ALL:
                    playQueueView.get().setRepeatAll();
                    break;
                case REPEAT_NONE:
                default:
                    playQueueView.get().setRepeatNone();
            }
        }
    }

    private void updateItemsInRepeatMode(PlayQueueManager.RepeatMode repeatMode) {
        for (PlayQueueUIItem item : items) {
            item.setRepeatMode(repeatMode);
        }
        if (playQueueView.isPresent()) {
            playQueueView.get().setItems(items);
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
            if (adapterPosition >= 0 && adapterPosition < items.size()) {
                final PlayQueueUIItem item = items.get(adapterPosition);
                return item.isRemoveable();
            }
        }
        return false;
    }

    public void remove(int adapterPosition) {
        if (playQueueView.isPresent()) {
            final PlayQueueUIItem adapterItem = items.get(adapterPosition);
            if (adapterItem.isTrack()) {
                final PlayQueueItem playQueueItem = ((TrackPlayQueueUIItem) adapterItem).getPlayQueueItem();
                final int playQueuePosition = playQueueManager.indexOfPlayQueueItem(playQueueItem);
                if (isSingleTrackSection(adapterPosition)) {
                    int headerPosition = adapterPosition - 1;
                    removeSection(headerPosition);
                } else {
                    removeSingleItem(adapterPosition, playQueueItem, playQueuePosition);
                }
            } else if (adapterItem.isHeader()) {
                removeSection(adapterPosition);
            }
        }
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueRemove(Screen.PLAY_QUEUE));
    }

    private void removeSingleItem(int adapterPosition, PlayQueueItem playQueueItem, int playQueuePosition) {
        playQueueView.get().showUndo();
        playQueueView.get().removeItem(adapterPosition);
        undoHolder = Optional.of(new UndoHolder(Lists.newArrayList(playQueueItem), playQueuePosition, Lists.newArrayList(items.remove(adapterPosition)), adapterPosition));
        if (playQueuePosition >= 0) {
            playQueueManager.removeItem(playQueueItem);
        }
    }

    private boolean isSingleTrackSection(int adapterPosition) {
        return isHeader(adapterPosition - 1) && isHeader(adapterPosition + 1);
    }

    private void removeSection(int headerPosition) {
        List<PlayQueueUIItem> playQueueUIItems = new ArrayList<>();
        playQueueUIItems.add(items.get(headerPosition));
        for (int i = headerPosition + 1; i < items.size(); i++) {
            PlayQueueUIItem playQueueUIItem = items.get(i);
            if (!playQueueUIItem.isTrack()) {
                break;
            }
            if (!playQueueUIItem.isRemoveable()) {
                return;
            }
            playQueueUIItems.add(playQueueUIItem);
        }

        playQueueView.get().showUndo();
        items.removeAll(playQueueUIItems);
        rebuildSubject.onNext(true);
        List<PlayQueueItem> trackPlayQueueItems = new ArrayList<>();
        int playQueuePosition = -1;
        for (PlayQueueUIItem playQueueUIItem : playQueueUIItems) {
            if (playQueueUIItem.isTrack()) {
                trackPlayQueueItems.add(((TrackPlayQueueUIItem) playQueueUIItem).getPlayQueueItem());
                if (playQueuePosition == -1) {
                    playQueuePosition = playQueueManager.indexOfPlayQueueItem(trackPlayQueueItems.get(0));
                }
                playQueueManager.removeItem(((TrackPlayQueueUIItem) playQueueUIItem).getPlayQueueItem());
            }
        }
        undoHolder = Optional.of(new UndoHolder(trackPlayQueueItems, playQueuePosition, playQueueUIItems, headerPosition));

    }

    private boolean isHeader(int adapterPosition) {
        return items.size() >= adapterPosition && adapterPosition >= 0 && items.get(adapterPosition).isHeader();
    }

    void switchItems(int fromPosition, int toPosition) {
        if (playQueueView.isPresent()) {
            Collections.swap(items, fromPosition, toPosition);
            playQueueView.get().switchItems(fromPosition, toPosition);
        }
    }

    void magicBoxClicked() {
        playQueueManager.moveToNextRecommendationItem();
    }

    void magicBoxToggled(boolean checked) {
        playQueueManager.setAutoPlay(checked);
    }

    void moveItems(int fromAdapterPosition, int toAdapterPosition) {
        if (playQueueView.isPresent()) {
            rebuildSubject.onNext(true);
            playQueueManager.moveItem(getQueuePosition(fromAdapterPosition),
                    getQueuePosition(toAdapterPosition));
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueReorder(Screen.PLAY_QUEUE));
        }
    }

    void scrollDown(int lastVisibleItemPosition) {
        playlistExploder.explodePlaylists(lastVisibleItemPosition, EXPLOSION_LOOK_AHEAD);
    }

    void scrollUp(int firstVisibleItemPosition) {
        int resolvedPosition = (firstVisibleItemPosition - EXPLOSION_LOOK_AHEAD < 0) ? 0 : firstVisibleItemPosition - EXPLOSION_LOOK_AHEAD;
        playlistExploder.explodePlaylists(resolvedPosition, EXPLOSION_LOOK_AHEAD);
    }

    private int getQueuePosition(int adapterPosition) {
        int cursor = 0;

        for (int i = 0; i < items.size(); i++) {
            if (i == adapterPosition) {
                return cursor;
            }

            if (items.get(i).isTrack()) {
                cursor++;
            }
        }
        return 0;
    }

    private Observable<Map<Urn, String>> createTitlesFromItems(List<PlayQueueUIItem> items) {
        return Observable.just(items)
                .map(playQueueUIItems -> {
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
                });
    }

    private int getAdapterPosition(final PlayQueueItem currentPlayQueueItem) {
        return Iterables.indexOf(items, getPositionForItemPredicate(currentPlayQueueItem));
    }

    private static Predicate<PlayQueueUIItem> getPositionForItemPredicate(final PlayQueueItem currentPlayQueueItem) {
        return input -> input.isTrack() &&
                ((TrackPlayQueueUIItem) input).getPlayQueueItem().equals(currentPlayQueueItem);
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<List<PlayQueueUIItem>> {

        @Override
        public void onNext(List<PlayQueueUIItem> items) {
            if (playQueueView.isPresent()) {
                setNowPlaying();
                playQueueView.get().setItems(items);
                if (resetUI) {
                    playQueueView.get().scrollTo(getScrollPosition());
                    playQueueView.get().removeLoadingIndicator();
                    resetUI = false;
                    measurePlayQueueLoadTime(items.size());
                }
            }
        }
    }

    private void measurePlayQueueLoadTime(int items) {
        MetricParams params = new MetricParams().putLong(MetricKey.PLAY_QUEUE_SIZE, items);
        performanceMetricsEngine.endMeasuring(PerformanceMetric.builder()
                .metricType(MetricType.PLAY_QUEUE_LOAD)
                .metricParams(params)
                .build());
    }

    private void updateNowPlaying(int position, boolean isPlaying) {
        if (items.size() == position && items.get(position).isTrack()
                && items.get(position).isPlayingOrPaused()) {
            return;
        }

        Optional<HeaderPlayQueueUIItem> lastHeaderPlayQueueUiItem = Optional.absent();
        boolean headerPlayStateSet = false;
        for (int i = 0; i < items.size(); i++) {
            final PlayQueueUIItem item = items.get(i);
            if (item.isTrack()) {
                final TrackPlayQueueUIItem trackItem = (TrackPlayQueueUIItem) item;
                setPlayState(position, i, trackItem, isPlaying);
                trackItem.setRemoveable(trackItem.getPlayState().equals(PlayState.COMING_UP));
                if (!headerPlayStateSet && lastHeaderPlayQueueUiItem.isPresent()) {
                    headerPlayStateSet = shouldAddHeader(trackItem);
                    lastHeaderPlayQueueUiItem.get().setPlayState(trackItem.getPlayState());
                    lastHeaderPlayQueueUiItem.get().setRemoveable(trackItem.getPlayState().equals(PlayState.COMING_UP));
                }
            } else if (item.isHeader()) {
                lastHeaderPlayQueueUiItem = Optional.of((HeaderPlayQueueUIItem) item);
                headerPlayStateSet = false;
            }
        }
    }

    private void setNowPlaying() {
        final int adapterPosition = getAdapterPosition(playQueueManager.getCurrentPlayQueueItem());
        updateNowPlaying(adapterPosition, playbackStateProvider.isSupposedToBePlaying());
    }

    private boolean shouldAddHeader(TrackPlayQueueUIItem trackItem) {
        return trackItem.isPlayingOrPaused() || PlayState.COMING_UP.equals(trackItem.getPlayState());
    }

    private void setPlayState(int currentlyPlayingPosition, int itemPosition, TrackPlayQueueUIItem item, boolean isPlaying) {
        if (currentlyPlayingPosition == itemPosition) {
            item.setPlayState(isPlaying ? PlayState.PLAYING : PlayState.PAUSED);
        } else if (itemPosition > currentlyPlayingPosition) {
            item.setPlayState(PlayState.COMING_UP);
        } else {
            item.setPlayState(PlayState.PLAYED);
        }
    }

    private class UndoHolder {
        private final List<PlayQueueItem> playQueueItems;
        private final int playQueuePosition;
        private final List<PlayQueueUIItem> playQueueUIItems;
        private final int adapterPosition;

        public UndoHolder(List<PlayQueueItem> playQueueItems, int playQueuePosition, List<PlayQueueUIItem> playQueueUIItems, int adapterPosition) {
            this.playQueueItems = playQueueItems;
            this.playQueuePosition = playQueuePosition;
            this.playQueueUIItems = playQueueUIItems;
            this.adapterPosition = adapterPosition;
        }

    }

}
