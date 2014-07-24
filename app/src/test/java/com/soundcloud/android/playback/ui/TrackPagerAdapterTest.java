package com.soundcloud.android.playback.ui;

import static android.support.v4.view.PagerAdapter.POSITION_NONE;
import static android.support.v4.view.PagerAdapter.POSITION_UNCHANGED;
import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.playback.service.Playa.Reason;
import static com.soundcloud.android.playback.service.Playa.StateTransition;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.support.v4.view.PagerAdapter;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

@RunWith(SoundCloudTestRunner.class)
public class TrackPagerAdapterTest {

    private static final TrackUrn TRACK_URN = Urn.forTrack(123L);

    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private TrackOperations trackOperations;
    @Mock private TrackPagePresenter trackPagePresenter;
    @Mock private AdPagePresenter adPagePresenter;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private ViewGroup container;

    @Captor private ArgumentCaptor<PropertySet> captorPropertySet;

    private TrackUrn trackUrn = Urn.forTrack(123L);
    private TestEventBus eventBus;
    private TrackPagerAdapter adapter;
    private PropertySet track;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        adapter = new TrackPagerAdapter(playQueueManager, playSessionStateProvider, trackOperations, trackPagePresenter, adPagePresenter, eventBus);
        final View mockedView1 = mock(View.class);
        final View mockedView2 = mock(View.class);
        when(trackPagePresenter.createItemView(container)).thenReturn(mockedView1, mockedView2);
        track = PropertySet.from(TrackProperty.URN.bind(TRACK_URN));
    }

    @Test
    public void getCountReturnsCurrentPlayQueueSize() {
        when(playQueueManager.getQueueSize()).thenReturn(10);
        expect(adapter.getCount()).toBe(10);
    }

    @Test
    public void getViewReturnsConvertViewWhenNotNull() {
        View view = mock(View.class);
        when(trackOperations.track(TRACK_URN)).thenReturn(Observable.<PropertySet>empty());
        when(playQueueManager.getUrnAtPosition(0)).thenReturn(Urn.forTrack(123L));
        when(trackPagePresenter.clearItemView(any(View.class))).thenReturn(view);
        expect(adapter.getView(0, view, container)).toBe(view);
    }

    @Test
    public void getViewReturnsCreatedViewWhenConvertViewIsNull() {
        when(trackOperations.track(TRACK_URN)).thenReturn(Observable.<PropertySet>empty());

        getPageView();

        verify(trackPagePresenter).createItemView(container);
    }

    @Test
    public void onPlayingStateEventCallsSetPlayStateOnPresenter() {
        final View currentTrackView = getPageView();
        StateTransition state = new StateTransition(PlayaState.PLAYING, Reason.NONE, trackUrn);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);

        verify(trackPagePresenter).setPlayState(currentTrackView, state, true);
    }

    @Test
    public void onPlayingStateEventCallsSetPlayStateForOtherPage() {
        setCurrentTrackState(3, Urn.forTrack(123L), true);
        setCurrentTrackState(4, Urn.forTrack(234L), false);
        final View viewForCurrentTrack = getPageView(3, Urn.forTrack(123L));
        final View viewForOtherTrack = getPageView(4, Urn.forTrack(234L));

        Mockito.reset(trackPagePresenter);
        StateTransition state = new StateTransition(PlayaState.PLAYING, Reason.NONE, trackUrn);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);

        verify(trackPagePresenter).setPlayState(viewForCurrentTrack, state, true);
        verify(trackPagePresenter).setPlayState(viewForOtherTrack, state, false);
    }

    @Test
    public void onPlayableChangedEventSetsLikeStatusOnTrackPage() {
        View currentPageView = getPageView();
        PlayableChangedEvent likeEvent = PlayableChangedEvent.forLike(Urn.forTrack(123L), true, 1);

        eventBus.publish(EventQueue.PLAYABLE_CHANGED, likeEvent);

        verify(trackPagePresenter).updateAssociations(currentPageView, likeEvent.getChangeSet());
    }

    @Test
    public void onPlayableChangedEventIsIgnoredForPlaylistAssociations() {
        getPageView();
        PlayableChangedEvent likeEvent = PlayableChangedEvent.forLike(Urn.forPlaylist(123L), true, 1);

        eventBus.publish(EventQueue.PLAYABLE_CHANGED, likeEvent);

        verify(trackPagePresenter, never()).updateAssociations(any(View.class), any(PropertySet.class));
    }

    @Test
    public void onPlaybackProgressEventSetsProgressOnCurrentPlayingTrackPage() {
        View currentPageView = getPageView();
        PlaybackProgressEvent event = new PlaybackProgressEvent(new PlaybackProgress(5l, 10l), Urn.forTrack(123L));

        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, event);

        verify(trackPagePresenter).setProgress(currentPageView, event.getPlaybackProgress());
    }

    @Test
    public void onPlaybackProgressEventDoNotSetProgressOnOtherTrackPage() {
        View currentPageView = getPageView();
        PlaybackProgressEvent event = new PlaybackProgressEvent(new PlaybackProgress(5l, 10l), Urn.forTrack(234L));

        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, event);

        verify(trackPagePresenter, never()).setProgress(currentPageView, event.getPlaybackProgress());
    }

    @Test
    public void onPlaybackProgressEventDoesNotSetProgressOnNotPlayingTrackPage() {
        View currentPageView = getPageView();
        PlaybackProgressEvent event = new PlaybackProgressEvent(new PlaybackProgress(5l, 10l), Urn.forTrack(999L));

        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, event);

        verify(trackPagePresenter, never()).setProgress(currentPageView, event.getPlaybackProgress());
    }

    @Test
    public void shouldSetExpandModeOnSubscribe() {
        View currentTrackView = getPageView();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forCollapsePlayer());

        verify(trackPagePresenter).setCollapsed(currentTrackView);
    }

    @Test
    public void onPlayerExpandedEventSetsFullScreenPlayerMode() {
        View currentTrackView = getPageView();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        InOrder inOrder = Mockito.inOrder(trackPagePresenter);
        inOrder.verify(trackPagePresenter).setCollapsed(currentTrackView);
        inOrder.verify(trackPagePresenter).setExpanded(eq(currentTrackView), anyBoolean());
    }

    @Test
    public void creatingNewTrackViewSetThePlayState() {
        View currentPageView = getPageView();

        verify(trackPagePresenter).setPlayState(eq(currentPageView), any(StateTransition.class), eq(true));
    }

    @Test
    public void recyclingTrackViewDoesNotSetThePlayState() {
        final View view = getPageView();
        Mockito.reset(trackPagePresenter);
        when(trackPagePresenter.clearItemView(view)).thenReturn(view);
        View currentPageView = adapter.getView(3, view, container);

        verify(trackPagePresenter, never()).setPlayState(eq(currentPageView), any(StateTransition.class), eq(true));
    }

    @Test
    public void getViewClearsRecycledViewWithUrnForCurrentPosition() {
        final View view = getPageView();
        TrackUrn trackUrn = Urn.forTrack(123L);
        when(playQueueManager.getUrnAtPosition(2)).thenReturn(trackUrn);
        when(trackOperations.track(TRACK_URN)).thenReturn(Observable.<PropertySet>empty());

        adapter.getView(2, view, container);

        verify(trackPagePresenter).clearItemView(view);
    }

    @Test
    public void getViewUsesCachedObservableIfAlreadyInCache() {
        getPageView();
        verify(trackOperations).track(TRACK_URN);
    }

    @Test
    public void shouldCreateTrackViewForTracks() {
        when(playQueueManager.isAudioAdAtPosition(3)).thenReturn(false);
        getPageView();

        verify(trackPagePresenter).createItemView(container);
    }

    @Test
    public void shouldBindTrackViewForTracks() {
        when(playQueueManager.isAudioAdAtPosition(3)).thenReturn(false);
        final View pageView = getPageView();

        verify(trackPagePresenter).bindItemView(pageView, track);
    }

    @Test
    public void shouldCreateAdViewForAudioAds() {
        setupAudioAd();
        getPageView();

        verify(adPagePresenter).createItemView(container);
    }

    @Test
    public void shouldBindAdViewForAudioAds() {
        setupAudioAd();
        final View pageView = getPageView();

        verify(adPagePresenter).bindItemView(eq(pageView), any(PropertySet.class));
    }

    @Test
    public void shouldBindAdViewWithAdPropertiesForAudioAds() {
        PropertySet adPropertySet = PropertySet.create().put(AdProperty.ARTWORK, Uri.parse("http://artwork.com"));
        setupAudioAd(adPropertySet);
        View pageView = getPageView();

        verify(adPagePresenter).bindItemView(eq(pageView), captorPropertySet.capture());

        expect(captorPropertySet.getValue().get(AdProperty.ARTWORK)).toBe(adPropertySet.get(AdProperty.ARTWORK));
    }

    @Test
    public void shouldBindAdViewWithoutAdPropertiesForTracks() {
        View pageView = getPageView();

        verify(trackPagePresenter).bindItemView(eq(pageView), captorPropertySet.capture());

        expect(captorPropertySet.getValue().contains(AdProperty.ARTWORK)).toBeFalse();
    }

    @Test
    public void resetAllScrubStateWhenTrackChange() {
        View currentPageView = getPageView();
        when(trackPagePresenter.accept(currentPageView)).thenReturn(true);

        adapter.onTrackChange();

        verify(trackPagePresenter).clearScrubState(currentPageView);
    }

    @Test
    public void trackChangeSetsProgressOnAllTrackViews() {
        TrackUrn firstUrn = Urn.forTrack(123L);
        TrackUrn secondUrn = Urn.forTrack(125L);
        PlaybackProgress firstProgress = new PlaybackProgress(100, 200);
        PlaybackProgress secondProgress = new PlaybackProgress(50, 100);
        View firstTrack = getPageView(0, firstUrn);
        getPageView(1, Urn.forTrack(124L)); // Audio ad
        View secondTrack = getPageView(2, secondUrn);
        when(trackPagePresenter.accept(firstTrack)).thenReturn(true);
        when(trackPagePresenter.accept(secondTrack)).thenReturn(true);
        when(playSessionStateProvider.getCurrentProgress(firstUrn)).thenReturn(firstProgress);
        when(playSessionStateProvider.getCurrentProgress(secondUrn)).thenReturn(secondProgress);

        adapter.onTrackChange();

        verify(trackPagePresenter).setProgress(firstTrack, firstProgress);
        verify(trackPagePresenter).setProgress(secondTrack, secondProgress);
        verify(adPagePresenter, never()).setProgress(any(View.class), any(PlaybackProgress.class));
    }

    @Test
    public void reusePageWhenTracksDidNotChangeInThePlayQueue() {
        expect(adapter.getItemPosition(getPageView())).toBe(POSITION_UNCHANGED);
    }

    @Test
    public void recreatePageWhenTracksChangedInThePlayQueue() {
        getPageView();
        final View differentView = mock(View.class);
        expect(adapter.getItemPosition(differentView)).toBe(POSITION_NONE);
    }

    @Test
    public void returnNewPositionWhenTrackHasChangedPositionsInQueue() throws Exception {
        View view = getPageView();
        when(playQueueManager.getUrnAtPosition(3)).thenReturn(Urn.forTrack(456L));
        when(playQueueManager.getPositionForUrn(eq(Urn.forTrack(123L)))).thenReturn(2);
        expect(adapter.getItemPosition(view)).toBe(2);
    }

    @Test
    public void getItemPositionReturnsUnchangedAfterUpdatingTrackPosition() throws Exception {
        View view = getPageView();
        when(playQueueManager.getUrnAtPosition(3)).thenReturn(Urn.forTrack(456L));
        when(playQueueManager.getPositionForUrn(eq(Urn.forTrack(123L)))).thenReturn(2);
        adapter.getItemPosition(view); // updates position

        when(playQueueManager.getUrnAtPosition(2)).thenReturn(Urn.forTrack(123L));
        expect(adapter.getItemPosition(view)).toBe(PagerAdapter.POSITION_UNCHANGED);

    }

    private View getPageView() {
        setCurrentTrackState(3, Urn.forTrack(123L), true);
        return getPageView(3, Urn.forTrack(123L));
    }

    private View getPageView(int position, TrackUrn trackUrn) {
        setupGetCurrentViewPreconditions(position, trackUrn);
        return adapter.getView(position, null, container);
    }

    private void setupGetCurrentViewPreconditions(int position, TrackUrn trackUrn) {
        when(playQueueManager.getUrnAtPosition(position)).thenReturn(trackUrn);
        when(trackOperations.track(trackUrn)).thenReturn(Observable.just(track));
    }

    private void setCurrentTrackState(int position, TrackUrn trackUrn, boolean isCurrentTrack) {
        if (isCurrentTrack) {
            when(playQueueManager.getCurrentPosition()).thenReturn(position);
        }
        when(playQueueManager.isCurrentPosition(position)).thenReturn(isCurrentTrack);
        when(playQueueManager.isCurrentTrack(trackUrn)).thenReturn(isCurrentTrack);
        when(playQueueManager.getUrnAtPosition(position)).thenReturn(trackUrn);
    }

    private void setupAudioAd() {
        setupAudioAd(PropertySet.create());
    }

    private void setupAudioAd(PropertySet propertySet) {
        when(playQueueManager.getAudioAd()).thenReturn(propertySet);
        when(playQueueManager.isAudioAdAtPosition(3)).thenReturn(true);
    }
}