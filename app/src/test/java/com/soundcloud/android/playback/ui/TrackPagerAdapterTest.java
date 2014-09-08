package com.soundcloud.android.playback.ui;

import static android.support.v4.view.PagerAdapter.POSITION_NONE;
import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.Playa.PlayaState;
import com.soundcloud.android.playback.service.Playa.Reason;
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
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

@RunWith(SoundCloudTestRunner.class)
public class TrackPagerAdapterTest {

    private static final TrackUrn TRACK_URN = Urn.forTrack(123L);
    private static final TrackUrn MONETIZABLE_TRACK_URN = Urn.forTrack(456L);

    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private TrackOperations trackOperations;
    @Mock private TrackPagePresenter trackPagePresenter;
    @Mock private AdPagePresenter adPagePresenter;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private ViewGroup container;
    @Mock private SkipListener skipListener;

    @Mock private View view1;
    @Mock private View view2;
    @Mock private View view3;
    @Mock private View view4;
    @Mock private View view5;
    @Mock private View view6;

    @Captor private ArgumentCaptor<PropertySet> captorPropertySet;

    private TestEventBus eventBus;
    private TrackPagerAdapter adapter;
    private PropertySet track;

    @Before
    public void setUp() throws Exception {

        when(trackPagePresenter.createItemView(container, skipListener)).thenReturn(view1, view2, view3, view4, view5, view6);

        eventBus = new TestEventBus();
        adapter = new TrackPagerAdapter(playQueueManager, playSessionStateProvider, trackOperations, trackPagePresenter, adPagePresenter, eventBus);
        adapter.initialize(container, skipListener);

        track = PropertySet.from(TrackProperty.URN.bind(TRACK_URN));

        when(trackOperations.track(MONETIZABLE_TRACK_URN)).thenReturn(Observable.just(
                PropertySet.from(
                        TrackProperty.URN.bind(MONETIZABLE_TRACK_URN),
                        PlayableProperty.TITLE.bind("title"),
                        PlayableProperty.CREATOR_NAME.bind("artist"))));
    }

    @Test
    public void getCountReturnsCurrentPlayQueueSize() {
        when(playQueueManager.getQueueSize()).thenReturn(10);
        expect(adapter.getCount()).toBe(10);
    }

    @Test
    public void onPlayingStateEventCallsSetPlayStateOnPresenter() {
        final View currentTrackView = getPageView();
        Playa.StateTransition state = new Playa.StateTransition(PlayaState.PLAYING, Reason.NONE, TRACK_URN);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);

        verify(trackPagePresenter).setPlayState(currentTrackView, state, true);
    }

    @Test
    public void onPlayingStateEventCallsSetPlayStateForOtherPage() {
        setCurrentTrackState(3, TRACK_URN, true);
        setCurrentTrackState(4, Urn.forTrack(234L), false);
        final View viewForCurrentTrack = getPageView(3, TrackPagerAdapterTest.TRACK_URN);
        final View viewForOtherTrack = getPageView(4, Urn.forTrack(234L));

        Mockito.reset(trackPagePresenter);
        Playa.StateTransition state = new Playa.StateTransition(PlayaState.PLAYING, Reason.NONE, TRACK_URN);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);

        verify(trackPagePresenter).setPlayState(viewForCurrentTrack, state, true);
        verify(trackPagePresenter).setPlayState(viewForOtherTrack, state, false);
    }

    @Test
    public void onPlayableChangedEventSetsLikeStatusOnTrackPage() {
        View currentPageView = getPageView();
        PlayableUpdatedEvent likeEvent = PlayableUpdatedEvent.forLike(TrackPagerAdapterTest.TRACK_URN, true, 1);

        eventBus.publish(EventQueue.PLAYABLE_CHANGED, likeEvent);

        verify(trackPagePresenter).onPlayableUpdated(currentPageView, likeEvent);
    }

    @Test
    public void onPlayableChangedEventIsIgnoredForPlaylistAssociations() {
        getPageView();
        PlayableUpdatedEvent likeEvent = PlayableUpdatedEvent.forLike(Urn.forPlaylist(123L), true, 1);

        eventBus.publish(EventQueue.PLAYABLE_CHANGED, likeEvent);

        verify(trackPagePresenter, never()).onPlayableUpdated(any(View.class), any(PlayableUpdatedEvent.class));
    }

    @Test
    public void onPlaybackProgressEventSetsProgressOnCurrentPlayingTrackPage() {
        View currentPageView = getPageView();
        PlaybackProgressEvent event = new PlaybackProgressEvent(new PlaybackProgress(5l, 10l), TrackPagerAdapterTest.TRACK_URN);

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
    public void setsCollapsedModeOnSubscribeForCollapsePlayerEvent() {
        View currentTrackView = getPageView();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        verify(trackPagePresenter).setCollapsed(currentTrackView);
    }

    @Test
    public void setsCollapsedModeOnSubscribeForCollapsingPlayerEvent() {
        View currentTrackView = getPageView();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        verify(trackPagePresenter).setCollapsed(currentTrackView);
    }

    @Test
    public void setsExpandedModeOnSubscribeForExpandPlayerEvent() {
        View currentTrackView = getPageView();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(trackPagePresenter).setExpanded(currentTrackView);
    }

    @Test
    public void creatingNewTrackViewSetThePlayState() {
        View currentPageView = getPageView();

        verify(trackPagePresenter).setPlayState(eq(currentPageView), any(Playa.StateTransition.class), eq(true));
    }

    @Test
    public void recyclingTrackViewDoesNotSetThePlayState() {
        final View view = getPageView();
        Mockito.reset(trackPagePresenter);
        when(trackPagePresenter.clearItemView(view)).thenReturn(view);
        when(trackPagePresenter.accept(view)).thenReturn(true);

        adapter.destroyItem(container, 3, view);

        View currentPageView = (View) adapter.instantiateItem(container, 3);

        verify(trackPagePresenter, never()).setPlayState(eq(currentPageView), any(Playa.StateTransition.class), eq(true));
    }

    @Test
    public void getViewClearsRecycledViewWithUrnForCurrentPosition() {
        when(trackPagePresenter.accept(any(View.class))).thenReturn(true);
        when(playQueueManager.getUrnAtPosition(2)).thenReturn(TRACK_URN);
        when(trackOperations.track(TRACK_URN)).thenReturn(Observable.<PropertySet>empty());

        adapter.instantiateItem(container, 2);

        verify(trackPagePresenter).clearItemView(any(View.class));
    }

    @Test
    public void getViewUsesCachedObservableIfAlreadyInCache() {
        getPageView();
        verify(trackOperations).track(TRACK_URN);
    }

    @Test
    public void shouldCreateTrackViewForTracks() {
        when(playQueueManager.isAudioAdAtPosition(3)).thenReturn(false);
        expect(getPageView()).toBe(view6);
    }

    @Test
    public void shouldBindTrackViewForTracks() {
        when(playQueueManager.isAudioAdAtPosition(3)).thenReturn(false);
        final View pageView = getPageView();

        verify(trackPagePresenter).bindItemView(pageView, track, true);
    }

    @Test
    public void shouldCreateAdViewForAudioAds() {
        adapter.initialize(container, skipListener);
        setupAudioAd();
        getPageView();


        verify(adPagePresenter).createItemView(container, skipListener);
    }

    @Test
    public void shouldBindAdViewForAudioAds() {
        setupAudioAd();
        View pageView = getPageView();

        verify(adPagePresenter).bindItemView(eq(pageView), captorPropertySet.capture(), eq(true));

        expect(captorPropertySet.getValue().contains(AdProperty.ARTWORK)).toBeTrue();
        expect(captorPropertySet.getValue().get(AdProperty.MONETIZABLE_TRACK_URN)).toEqual(MONETIZABLE_TRACK_URN);
        expect(captorPropertySet.getValue().get(AdProperty.MONETIZABLE_TRACK_TITLE)).toEqual("title");
        expect(captorPropertySet.getValue().get(AdProperty.MONETIZABLE_TRACK_CREATOR)).toEqual("artist");

        expect(captorPropertySet.getValue().contains(AdProperty.DEFAULT_TEXT_COLOR)).toBeTrue();
        expect(captorPropertySet.getValue().contains(AdProperty.DEFAULT_BACKGROUND_COLOR)).toBeTrue();
        expect(captorPropertySet.getValue().contains(AdProperty.FOCUSED_TEXT_COLOR)).toBeTrue();
        expect(captorPropertySet.getValue().contains(AdProperty.FOCUSED_BACKGROUND_COLOR)).toBeTrue();
        expect(captorPropertySet.getValue().contains(AdProperty.PRESSED_BACKGROUND_COLOR)).toBeTrue();
        expect(captorPropertySet.getValue().contains(AdProperty.PRESSED_TEXT_COLOR)).toBeTrue();
    }

    @Test
    public void resetAllScrubStateWhenTrackChange() {
        View currentPageView = getPageView();
        when(trackPagePresenter.accept(currentPageView)).thenReturn(true);

        adapter.onTrackChange();

        verify(trackPagePresenter).onPageChange(currentPageView);
    }

    @Test
    public void trackChangeSetsProgressOnAllTrackViews() {
        TrackUrn firstUrn = TrackPagerAdapterTest.TRACK_URN;
        TrackUrn secondUrn = Urn.forTrack(125L);
        PlaybackProgress firstProgress = new PlaybackProgress(100, 200);
        PlaybackProgress secondProgress = new PlaybackProgress(50, 100);
        View firstTrack = getPageView(0, firstUrn);
        getPageView(1, Urn.forTrack(124L)); // Audio ad
        View secondTrack = getPageView(2, secondUrn);
        when(trackPagePresenter.accept(firstTrack)).thenReturn(true);
        when(trackPagePresenter.accept(secondTrack)).thenReturn(true);
        when(playSessionStateProvider.hasCurrentProgress(firstUrn)).thenReturn(true);
        when(playSessionStateProvider.getLastProgressByUrn(firstUrn)).thenReturn(firstProgress);
        when(playSessionStateProvider.hasCurrentProgress(secondUrn)).thenReturn(true);
        when(playSessionStateProvider.getLastProgressByUrn(secondUrn)).thenReturn(secondProgress);

        adapter.onTrackChange();

        verify(trackPagePresenter).setProgress(firstTrack, firstProgress);
        verify(trackPagePresenter).setProgress(secondTrack, secondProgress);
        verify(adPagePresenter, never()).setProgress(any(View.class), any(PlaybackProgress.class));
    }

    @Test
    public void getItemPositionReturnsNoneForCustomRecycling() {
        expect(adapter.getItemPosition(getPageView())).toBe(POSITION_NONE);
    }

    @Test
    public void onPlayerSlideForwardsPositionToAdPresenter() {
        setupAudioAd();
        final View pageView = getPageView();
        adapter.onPlayerSlide(0.5f);
        verify(adPagePresenter).onPlayerSlide(pageView, 0.5f);
    }

    @Test
    public void onPlayerSlideForwardsPositionToTrackPresenter() {
        final View pageView = getPageView();
        adapter.onPlayerSlide(0.5f);
        verify(trackPagePresenter).onPlayerSlide(pageView, 0.5f);
    }

    @Test
    public void bindingTrackViewSetsPositionOnPresenter() {
        when(playQueueManager.getPositionForUrn(TrackPagerAdapterTest.TRACK_URN)).thenReturn(3);
        when(playQueueManager.getQueueSize()).thenReturn(5);
        final View pageView = getPageView();

        verify(trackPagePresenter).onPositionSet(pageView, 3, 5);
    }

    @Test
    public void onPauseSetsBackgroundStateOnPresenter() {
        final View pageView = getPageView();
        adapter.onPause();
        verify(trackPagePresenter).onBackground(pageView);
    }

    @Test
    public void onResumeSetsForegroundStateOnPresenter() {
        final View pageView = getPageView();
        adapter.onResume();
        verify(trackPagePresenter).onForeground(pageView);
    }

    @Test
    public void reusingExistingViewSetsForegroundStateOnPresenter() {
        final View view = getPageView();
        when(trackPagePresenter.clearItemView(view)).thenReturn(view);
        when(trackPagePresenter.accept(view)).thenReturn(true);

        adapter.destroyItem(container, 3, view);

        View currentPageView = (View) adapter.instantiateItem(container, 3);

        verify(trackPagePresenter).onForeground(currentPageView);
    }

    @Test
    public void destroyingViewDoesNotSetBackgroundStateOnPresenterOnCurrentTrack() {
        final View view = getPageView();
        when(trackPagePresenter.accept(view)).thenReturn(true);
        when(playQueueManager.isCurrentTrack(eq(TRACK_URN))).thenReturn(true);

        adapter.destroyItem(container, 3, view);

        verify(trackPagePresenter, never()).onBackground(view);
    }

    @Test
    public void destroyingViewSetsBackgroundStateOnPresenter() {
        final View view = getPageView();
        when(trackPagePresenter.accept(view)).thenReturn(true);
        when(playQueueManager.isCurrentTrack(eq(TRACK_URN))).thenReturn(false);

        adapter.destroyItem(container, 3, view);

        verify(trackPagePresenter).onBackground(view);
    }

    private View getPageView() {
        setCurrentTrackState(3, TrackPagerAdapterTest.TRACK_URN, true);
        return getPageView(3, TrackPagerAdapterTest.TRACK_URN);
    }

    private View getPageView(int position, TrackUrn trackUrn) {
        setupGetCurrentViewPreconditions(position, trackUrn);
        return (View) adapter.instantiateItem(container, position);
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
        setupAudioAd(PropertySet.from(
                AdProperty.ARTWORK.bind(Uri.parse("http://artwork.com")),
                AdProperty.MONETIZABLE_TRACK_URN.bind(MONETIZABLE_TRACK_URN),
                AdProperty.DEFAULT_TEXT_COLOR.bind("#111111"),
                AdProperty.DEFAULT_BACKGROUND_COLOR.bind("#222222"),
                AdProperty.PRESSED_TEXT_COLOR.bind("#333333"),
                AdProperty.PRESSED_BACKGROUND_COLOR.bind("#444444"),
                AdProperty.FOCUSED_TEXT_COLOR.bind("#555555"),
                AdProperty.FOCUSED_BACKGROUND_COLOR.bind("#666666")));
    }

    private void setupAudioAd(PropertySet propertySet) {
        when(playQueueManager.getAudioAd()).thenReturn(propertySet);
        when(playQueueManager.isAudioAdAtPosition(3)).thenReturn(true);
    }
}