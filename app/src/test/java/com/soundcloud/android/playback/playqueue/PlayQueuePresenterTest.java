package com.soundcloud.android.playback.playqueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricParams;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlayQueueManager.RepeatMode;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackStateProvider;
import com.soundcloud.android.playback.PlaylistExploder;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;
import rx.Observable;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayQueuePresenterTest extends AndroidUnitTest {

    private final static int UI_ITEMS_SIZE = 4;

    private final Subject<List<PlayQueueUIItem>, List<PlayQueueUIItem>> uiSubject = BehaviorSubject.create();
    @Mock private PlayQueueView playQueueViewContract;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlayQueueDataProvider playQueueDataProvider;
    @Mock private PlaySessionController playSessionController;
    @Mock private PlaybackStateProvider playbackStateProvider;
    @Mock private PlayQueueUIItem item;
    @Mock private PlaylistExploder playlistExploder;
    @Mock private Resources resources;
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;

    @Captor private ArgumentCaptor<ArrayList<PlayQueueUIItem>> itemsCaptor;
    @Captor private ArgumentCaptor<PerformanceMetric> performanceMetricCaptor;

    private PlayQueuePresenter presenter;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        final PlayQueueUIItemMapper playQueueUIItemMapper = new PlayQueueUIItemMapper(context(), playQueueManager, resources);

        presenter = new PlayQueuePresenter(
                playQueueManager,
                playbackStateProvider,
                playSessionController,
                playQueueDataProvider,
                playlistExploder,
                eventBus,
                playQueueUIItemMapper,
                performanceMetricsEngine);
        when(playQueueManager.getRepeatMode()).thenReturn(RepeatMode.REPEAT_NONE);
        when(playQueueManager.isShuffled()).thenReturn(false);
        when(playQueueManager.getCollectionUrn()).thenReturn(Urn.NOT_SET);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123)));
        when(item.isTrack()).thenReturn(true);
        when(playbackStateProvider.isSupposedToBePlaying()).thenReturn(true);
        setUIItems();
        presenter.attachView(playQueueViewContract);
    }

    private OngoingStubbing<Observable<List<PlayQueueUIItem>>> setUIItems() {
        List<PlayQueueUIItem> uiItems = Lists.newArrayList(headerPlayQueueUIItem(false),
                                                           trackPlayQueueUIItemWithPlayState(PlayState.PLAYED),
                                                           trackPlayQueueUIItemWithPlayState(PlayState.PLAYING),
                                                           trackPlayQueueUIItemWithPlayState(PlayState.COMING_UP));
        uiSubject.onNext(uiItems);
        return when(playQueueDataProvider.getPlayQueueUIItems()).thenReturn(uiSubject);
    }

    @Test
    public void shouldNotCallToContractWhenDetached() {
        presenter.detachContract();

        reset(playQueueViewContract);

        presenter.repeatClicked();
        presenter.shuffleClicked(true);

        verifyZeroInteractions(playQueueViewContract);
    }

    @Test
    public void shouldSetViewOnAttach() {
        verify(playQueueViewContract).showLoadingIndicator();
        verify(playQueueViewContract).setItems(any());
        verify(playQueueViewContract).setShuffledState(false);
        verify(playQueueViewContract).setRepeatNone();
        verify(playQueueViewContract).scrollTo(0);
        verify(playQueueViewContract).removeLoadingIndicator();
    }

    @Test
    public void shouldSetDefaultPlayerStrip() {
        presenter.trackClicked(1);
        verify(playQueueViewContract).setDefaultPlayerStrip();
    }

    @Test
    public void shouldSetGoPlayerStrip() {
        TrackPlayQueueUIItem trackPlayQueueUIItem = Mockito.mock(TrackPlayQueueUIItem.class);
        when(trackPlayQueueUIItem.isTrack()).thenReturn(true);
        when(trackPlayQueueUIItem.isGoTrack()).thenReturn(true);
        when(trackPlayQueueUIItem.getPlayState()).thenReturn(PlayState.COMING_UP);
        uiSubject.onNext(Lists.newArrayList(trackPlayQueueUIItem));

        presenter.trackClicked(0);

        verify(playQueueViewContract).setGoPlayerStrip();
    }

    @Test
    public void shouldScrollToPositionOnNext() {
        presenter.onNextClick();

        verify(playQueueViewContract, times(2)).scrollTo(0);
    }

    @Test
    public void shouldSwitchItems() {
        presenter.switchItems(1, 3);

        verify(playQueueViewContract).switchItems(1, 3);
    }

    @Test
    public void returnTrueWhenUpcomingTrack() {
        assertThat(presenter.isRemovable(2)).isTrue();
    }

    @Test
    public void returnFalseWhenCurrentTrack() {
        presenter.trackClicked(2);

        assertThat(presenter.isRemovable(2)).isFalse();
    }

    @Test
    public void returnFalseWhenPlayedTrack() {
        presenter.trackClicked(2);

        assertThat(presenter.isRemovable(1)).isFalse();
    }

    @Test
    public void returnTrueWhenUpComingHeader() {
        List<PlayQueueUIItem> uiItems = Lists.newArrayList(headerPlayQueueUIItem(false),
                                                           trackPlayQueueUIItemWithPlayState(PlayState.PLAYING),
                                                           headerPlayQueueUIItem(true),
                                                           trackPlayQueueUIItemWithPlayState(PlayState.COMING_UP),
                                                           trackPlayQueueUIItemWithPlayState(PlayState.COMING_UP));
        uiSubject.onNext(uiItems);

        presenter.trackClicked(1);

        assertThat(presenter.isRemovable(0)).isFalse();
        assertThat(presenter.isRemovable(1)).isFalse();
        assertThat(presenter.isRemovable(2)).isTrue();
        assertThat(presenter.isRemovable(3)).isTrue();
        assertThat(presenter.isRemovable(4)).isTrue();
    }

    @Test
    public void shouldSetPlayStateWhenTrack() {
        presenter.trackClicked(2);

        verify(playQueueManager).setCurrentPlayQueueItem(any(PlayQueueItem.class));
    }

    @Test
    public void shouldTogglePlayBack() {
        when(playSessionController.isPlayingCurrentPlayQueueItem()).thenReturn(true);

        presenter.trackClicked(2);

        verify(playSessionController).togglePlayback();
    }

    @Test
    public void shouldStartPlayback() {
        when(playSessionController.isPlayingCurrentPlayQueueItem()).thenReturn(false);

        presenter.trackClicked(2);

        verify(playSessionController).play();
    }

    @Test
    public void shouldSubscribeToPlayQueueChangedAndFilterOutItemChanges() {
        reset(playQueueViewContract);

        final PlayQueueEvent event = PlayQueueEvent.fromQueueUpdateMoved(Urn.NOT_SET);

        eventBus.publish(EventQueue.PLAY_QUEUE, event);

        verify(playQueueViewContract, never()).setItems(anyList());
    }

    @Test
    public void shouldSetRepeatModeNone() {
        when(playQueueManager.getRepeatMode()).thenReturn(RepeatMode.REPEAT_ALL);

        presenter.repeatClicked();

        verify(playQueueManager).setRepeatMode(RepeatMode.REPEAT_NONE);
        verify(playQueueViewContract, times(2)).setRepeatNone();
    }

    @Test
    public void shouldSetRepeatModeOne() {
        when(playQueueManager.getRepeatMode()).thenReturn(RepeatMode.REPEAT_NONE);

        presenter.repeatClicked();

        verify(playQueueManager).setRepeatMode(RepeatMode.REPEAT_ONE);
        verify(playQueueViewContract, times(1)).setRepeatOne();
    }

    @Test
    public void shouldSetRepeatModeAll() {
        when(playQueueManager.getRepeatMode()).thenReturn(RepeatMode.REPEAT_ONE);

        presenter.repeatClicked();

        verify(playQueueManager).setRepeatMode(RepeatMode.REPEAT_ALL);
        verify(playQueueViewContract, times(1)).setRepeatAll();
    }

    @Test
    public void shouldTrackRepeatModeChanges() {
        when(playQueueManager.getRepeatMode()).thenReturn(RepeatMode.REPEAT_NONE);

        presenter.repeatClicked();

        final UIEvent trackingEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent.kind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_REPEAT);
        assertThat(trackingEvent.playQueueRepeatMode().get()).isEqualTo(RepeatMode.REPEAT_ONE.get());
    }

    @Test
    public void shouldToggleShuffleModeOn() {
        presenter.shuffleClicked(true);

        verify(playQueueManager).shuffle();
    }

    @Test
    public void shouldToggleShuffleModeOff() {
        presenter.shuffleClicked(false);

        verify(playQueueManager).unshuffle();
    }

    @Test
    public void shouldRemoveATrack() {
        uiSubject.onNext(Lists.newArrayList(trackPlayQueueUIItemWithPlayState(PlayState.COMING_UP),
                                            trackPlayQueueUIItemWithPlayState(PlayState.COMING_UP),
                                            trackPlayQueueUIItemWithPlayState(PlayState.COMING_UP)));
        reset(playQueueViewContract);

        final int position = 1;

        presenter.remove(position);

        verify(playQueueManager).removeItem(any());
        verify(playQueueViewContract).showUndo();
        verify(playQueueViewContract).removeItem(position);
    }

    @Test
    public void shouldRemoveSectionWhenHeaderRemoved() {
        uiSubject.onNext(Lists.newArrayList(headerPlayQueueUIItem(false),
                                            trackPlayQueueUIItemWithPlayState(PlayState.PLAYING),
                                            headerPlayQueueUIItem(true),
                                            trackPlayQueueUIItemWithPlayState(PlayState.COMING_UP)));
        presenter.trackClicked(1);

        reset(playQueueViewContract);
        presenter.remove(2);

        verify(playQueueViewContract).setItems(itemsCaptor.capture());

        assertThat(itemsCaptor.getAllValues().get(0).size()).isEqualTo(3);
    }

    @Test
    public void shouldNotRemoveAHeaderWhenSectionPlaying() {
        uiSubject.onNext(Lists.newArrayList(headerPlayQueueUIItem(false),
                                            trackPlayQueueUIItemWithPlayState(PlayState.PLAYING)));
        presenter.trackClicked(1);

        reset(playQueueViewContract);
        presenter.remove(0);

        assertThat(presenter.isRemovable(0)).isFalse();
        verify(playQueueManager, never()).removeItem(any(PlayQueueItem.class));
        verify(playQueueViewContract, never()).showUndo();
        verify(playQueueViewContract, never()).setItems(anyList());
    }

    @Test
    public void shouldRemoveHeadersWhenOneTrackInSection() {
        uiSubject.onNext(Lists.newArrayList(headerPlayQueueUIItem(false),
                                            trackPlayQueueUIItemWithPlayState(PlayState.COMING_UP),
                                            headerPlayQueueUIItem(false)));
        reset(playQueueViewContract);

        final int position = 1;

        presenter.remove(position);

        verify(playQueueViewContract).showUndo();
        verify(playQueueViewContract).setItems(Collections.emptyList());
    }

    @Test
    public void shouldEmitUIEventWhenClosing() {
        presenter.closePlayQueue();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_CLOSE.toString());
    }

    @Test
    public void shouldTrackShufflingOn() {
        presenter.shuffleClicked(true);

        final UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.kind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_SHUFFLE);
        assertThat(event.clickName().get()).isEqualTo(UIEvent.ClickName.SHUFFLE_ON);
    }

    @Test
    public void shouldTrackShufflingOff() {
        presenter.shuffleClicked(false);

        final UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.kind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_SHUFFLE);
        assertThat(event.clickName().get()).isEqualTo(UIEvent.ClickName.SHUFFLE_OFF);
    }

    @Test
    public void shouldTrackReorder() {
        presenter.moveItems(0, 1);

        final UIEvent trackingEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent.kind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_TRACK_REORDER);
    }

    @Test
    public void shouldReorderInPlayQueueManager() {
        presenter.moveItems(0, 2);

        verify(playQueueManager).moveItem(0, 1);
    }

    @Test
    public void shouldTrackRemoval() {
        presenter.remove(2);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind())
                .isEqualTo(UIEvent.Kind.PLAY_QUEUE_TRACK_REMOVE.toString());
    }

    @Test
    public void shouldTrackRemovalUndo() {
        presenter.remove(2);
        presenter.undoClicked();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind())
                .isEqualTo(UIEvent.Kind.PLAY_QUEUE_TRACK_REMOVE_UNDO.toString());
    }

    @Test
    public void shouldExplodePlaylistsWhenScrollingUp() {
        presenter.scrollUp(5);

        verify(playlistExploder).explodePlaylists(0, 5);
    }

    @Test
    public void shouldResolvePosition() {
        presenter.scrollUp(0);

        verify(playlistExploder).explodePlaylists(0, 5);
    }

    @Test
    public void shouldExplodePlaylistsWhenScrollingDown() {
        presenter.scrollDown(5);

        verify(playlistExploder).explodePlaylists(5, 5);
    }

    public void shouldAddTrackOnUndo() {
        presenter.remove(2);
        reset(playQueueViewContract);
        presenter.undoClicked();

        verify(playQueueManager).insertItemAtPosition(eq(0), any());
        verify(playQueueViewContract).setItems(anyList());
    }

    @Test
    public void shouldNotShowLoadingIndicatorOnRotation() {
        presenter.detachContract();

        reset(playQueueViewContract);

        presenter.attachView(playQueueViewContract);

        verify(playQueueViewContract, never()).showLoadingIndicator();
    }

    @Test
    public void shouldMoveToNextRecommendationWhenMagicBoxClicked() {
        presenter.magicBoxClicked();

        verify(playQueueManager).moveToNextRecommendationItem();
    }

    @Test
    public void shouldSetAutoPlayWhenMagicBoxToggled() {
        presenter.magicBoxToggled(false);
        presenter.magicBoxToggled(true);

        verify(playQueueManager).setAutoPlay(false);
        verify(playQueueManager).setAutoPlay(true);
    }

    @Test
    public void shouldStartMeasuringOnAttach() {
        // View is attached on setup method
        verify(performanceMetricsEngine).startMeasuring(MetricType.PLAY_QUEUE_LOAD);
    }

    @Test
    public void shouldEndMeasuringOnItemsLoaded() {
        // Setup method already performed the initialization
        verify(performanceMetricsEngine).endMeasuring(performanceMetricCaptor.capture());

        PerformanceMetric performanceMetric = performanceMetricCaptor.getValue();
        MetricParams params = performanceMetric.metricParams();

        assertThat(params.toBundle().getLong(MetricKey.PLAY_QUEUE_SIZE.toString())).isEqualTo(UI_ITEMS_SIZE);
    }

    private TrackPlayQueueUIItem trackPlayQueueUIItemWithPlayState(PlayState playState) {
        return trackPlayQueueUIItemWithPlayState(playState, Optional.absent());
    }

    private HeaderPlayQueueUIItem headerPlayQueueUIItem(boolean isRemovable) {
        return new HeaderPlayQueueUIItem(
                PlayState.PLAYING,
                RepeatMode.REPEAT_ONE,
                isRemovable, 0, "header");
    }

    private TrackPlayQueueUIItem trackPlayQueueUIItemWithPlayState(PlayState playState, Optional<String> contextTitle) {
        final Urn track = Urn.forTrack(123);
        final TrackPlayQueueUIItem playQueueUIItem = TrackPlayQueueUIItem
                .from(TestPlayQueueItem.createTrack(track),
                      PlayableFixtures.expectedTrackForListItem(track),
                      context(),
                      contextTitle,
                      RepeatMode.REPEAT_ONE);

        playQueueUIItem.setPlayState(playState);
        return playQueueUIItem;
    }

}

