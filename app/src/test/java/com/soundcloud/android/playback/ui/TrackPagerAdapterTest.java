package com.soundcloud.android.playback.ui;

import static android.support.v4.view.PagerAdapter.POSITION_NONE;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.Playa;
import com.soundcloud.android.playback.Playa.PlayaState;
import com.soundcloud.android.playback.Playa.Reason;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

public class TrackPagerAdapterTest extends AndroidUnitTest {

    private static final Urn TRACK1_URN = Urn.forTrack(123L);
    private static final Urn TRACK2_URN = Urn.forTrack(234L);
    private static final Urn TRACK2_RELATED_URN = Urn.forTrack(678L);
    private static final Urn AD_URN = Urn.forTrack(235L);
    private static final Urn MONETIZABLE_TRACK_URN = Urn.forTrack(456L);

    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private TrackRepository trackRepository;
    @Mock private TrackPagePresenter trackPagePresenter;
    @Mock private AdPagePresenter adPagePresenter;
    @Mock private CastConnectionHelper castConnectionHelper;

    @Mock private ViewGroup container;
    @Mock private SkipListener skipListener;
    @Mock private ViewVisibilityProvider viewVisibilityProvider;

    @Mock private View view1;
    @Mock private View view2;
    @Mock private View view3;
    @Mock private View view4;
    @Mock private View view5;
    @Mock private View view6;
    @Mock private View adView;

    private TestEventBus eventBus;
    private TrackPagerAdapter adapter;
    private PropertySet track;

    private List<TrackPageData> trackPageData = newArrayList(
            new TrackPageData(0, TRACK1_URN, PropertySet.create(), Urn.NOT_SET),
            new TrackPageData(1, TRACK2_URN, PropertySet.create(), TRACK2_RELATED_URN),
            new TrackPageData(2, AD_URN, getAudioAd(), Urn.NOT_SET),
            new TrackPageData(3, MONETIZABLE_TRACK_URN, TestPropertySets.interstitialForPlayer(), Urn.NOT_SET));

    @Before
    public void setUp() throws Exception {

        when(trackPagePresenter.createItemView(container, skipListener)).thenReturn(view1, view2, view3, view4, view5, view6);
        when(adPagePresenter.createItemView(container, skipListener)).thenReturn(adView);

        eventBus = new TestEventBus();
        adapter = new TrackPagerAdapter(playQueueManager,
                playSessionStateProvider,
                trackRepository,
                trackPagePresenter,
                adPagePresenter,
                castConnectionHelper,
                eventBus);
        adapter.onViewCreated(container, skipListener, viewVisibilityProvider);
        adapter.setCurrentData(trackPageData);

        track = PropertySet.from(TrackProperty.URN.bind(TRACK1_URN),
                PlayableProperty.TITLE.bind("title"),
                PlayableProperty.CREATOR_NAME.bind("artist"));

        when(trackRepository.track(MONETIZABLE_TRACK_URN)).thenReturn(Observable.just(
                PropertySet.from(
                        TrackProperty.URN.bind(MONETIZABLE_TRACK_URN),
                        PlayableProperty.TITLE.bind("title"),
                        PlayableProperty.CREATOR_NAME.bind("artist"))
        ));

        when(trackRepository.track(TRACK2_RELATED_URN)).thenReturn(Observable.just(
                PropertySet.from(
                        TrackProperty.URN.bind(TRACK2_RELATED_URN),
                        PlayableProperty.TITLE.bind("related title"),
                        PlayableProperty.CREATOR_NAME.bind("related artist"))
        ));
    }

    @Test
    public void getCountReturnsCurrentPlayQueueSize() {
        when(playQueueManager.getQueueSize()).thenReturn(4);
        assertThat(adapter.getCount()).isEqualTo(4);
    }

    @Test
    public void onPlayingStateEventCallsSetPlayStateOnPresenter() {
        adapter.onResume();
        final View currentTrackView = getPageView();
        Playa.StateTransition state = new Playa.StateTransition(PlayaState.PLAYING, Reason.NONE, TRACK1_URN);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);

        verify(trackPagePresenter).setPlayState(currentTrackView, state, true, true);
    }

    @Test
    public void onPlayingStateEventCallsSetPlayStateForOtherPage() {
        adapter.onResume();
        setCurrentTrackState(0, TRACK1_URN, true);
        setCurrentTrackState(1, TRACK2_URN, false);

        final View viewForCurrentTrack = getPageView(0, TRACK1_URN);
        final View viewForOtherTrack = getPageView(1, TRACK2_URN);

        Mockito.reset(trackPagePresenter);
        Playa.StateTransition state = new Playa.StateTransition(PlayaState.PLAYING, Reason.NONE, TRACK1_URN);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);

        verify(trackPagePresenter).setPlayState(viewForCurrentTrack, state, true, true);
        verify(trackPagePresenter).setPlayState(viewForOtherTrack, state, false, true);
    }

    @Test
    public void onPlayableChangedEventSetsLikeStatusOnTrackPage() {
        View currentPageView = getPageView();
        EntityStateChangedEvent likeEvent = EntityStateChangedEvent.fromLike(TRACK1_URN, true, 1);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, likeEvent);

        verify(trackPagePresenter).onPlayableUpdated(currentPageView, likeEvent);
    }

    @Test
    public void onPlayableChangedEventIsIgnoredForPlaylistAssociations() {
        getPageView();
        EntityStateChangedEvent likeEvent = EntityStateChangedEvent.fromLike(Urn.forPlaylist(123L), true, 1);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, likeEvent);

        verify(trackPagePresenter, never()).onPlayableUpdated(any(View.class), any(EntityStateChangedEvent.class));
    }

    @Test
    public void onPlaybackProgressEventSetsProgressOnCurrentPlayingTrackPage() {
        View currentPageView = getPageView();
        PlaybackProgressEvent event = new PlaybackProgressEvent(new PlaybackProgress(5l, 10l), TRACK1_URN);

        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, event);

        verify(trackPagePresenter).setProgress(currentPageView, event.getPlaybackProgress());
    }

    @Test
    public void onPlaybackProgressEventDoNotSetsProgressForPausedAdapter() {
        View currentPageView = getPageView();
        PlaybackProgressEvent event = new PlaybackProgressEvent(new PlaybackProgress(5l, 10l), TRACK1_URN);
        adapter.onPause();

        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, event);

        verify(trackPagePresenter, never()).setProgress(currentPageView, event.getPlaybackProgress());
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
        adapter.onResume();
        Playa.StateTransition state = new Playa.StateTransition(PlayaState.PLAYING, Reason.NONE, TRACK1_URN);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);

        View currentPageView = getPageView();

        verify(trackPagePresenter).setPlayState(eq(currentPageView), any(Playa.StateTransition.class), eq(true), eq(true));
    }

    @Test
    public void recyclingTrackViewDoesNotSetThePlayState() {
        final View view = getPageView();
        Mockito.reset(trackPagePresenter);
        when(trackPagePresenter.clearItemView(view)).thenReturn(view);
        when(trackPagePresenter.accept(view)).thenReturn(true);

        adapter.destroyItem(container, 3, view);

        View currentPageView = (View) adapter.instantiateItem(container, 3);

        verify(trackPagePresenter, never()).setPlayState(eq(currentPageView), any(Playa.StateTransition.class), eq(true), eq(true));
    }

    @Test
    public void getViewClearsRecycledViewWithUrnForCurrentPosition() {
        when(trackPagePresenter.accept(any(View.class))).thenReturn(true);
        when(playQueueManager.getUrnAtPosition(0)).thenReturn(TRACK1_URN);
        when(trackRepository.track(TRACK1_URN)).thenReturn(Observable.<PropertySet>empty());

        adapter.instantiateItem(container, 0);

        verify(trackPagePresenter).clearItemView(any(View.class));
    }

    @Test
    public void getViewUsesCachedObservableIfAlreadyInCache() {
        getPageView();
        verify(trackRepository).track(TRACK1_URN);
    }

    @Test
    public void shouldCreateTrackViewForTracks() {
        assertThat(getPageView()).isSameAs(view6);
    }

    @Test
    public void shouldBindTrackViewForTracks() {
        ArgumentCaptor<PlayerTrackState> captorPropertySet = ArgumentCaptor.forClass(PlayerTrackState.class);

        adapter.onResume();
        final View pageView = getPageView();

        verify(trackPagePresenter).bindItemView(same(pageView), captorPropertySet.capture());

        assertThat(captorPropertySet.getValue().getTrackUrn()).isEqualTo(TRACK1_URN);
        assertThat(captorPropertySet.getValue().getTitle()).isEqualTo("title");
        assertThat(captorPropertySet.getValue().getUserName()).isEqualTo("artist");
        assertThat(captorPropertySet.getValue().isForeground()).isTrue();
        assertThat(captorPropertySet.getValue().isCurrentTrack()).isTrue();
        assertThat(captorPropertySet.getValue().getViewVisibilityProvider()).isSameAs(viewVisibilityProvider);
    }

    @Test
    public void shouldBindTrackViewForTrackWithRelatedTrack() {
        ArgumentCaptor<PlayerTrackState> captorPropertySet = ArgumentCaptor.forClass(PlayerTrackState.class);

        adapter.onResume();

        setCurrentTrackState(1, TRACK2_URN, true);
        final View pageView = getPageView(1, TRACK2_URN);

        verify(trackPagePresenter).bindItemView(same(pageView), captorPropertySet.capture());

        assertThat(captorPropertySet.getValue().getTrackUrn()).isEqualTo(TRACK2_URN);
        assertThat(captorPropertySet.getValue().getTitle()).isEqualTo("title");
        assertThat(captorPropertySet.getValue().getUserName()).isEqualTo("artist");
        assertThat(captorPropertySet.getValue().isForeground()).isTrue();
        assertThat(captorPropertySet.getValue().isCurrentTrack()).isTrue();
        assertThat(captorPropertySet.getValue().getViewVisibilityProvider()).isSameAs(viewVisibilityProvider);
        assertThat(captorPropertySet.getValue().getRelatedTrackUrn()).isEqualTo(TRACK2_RELATED_URN);
        assertThat(captorPropertySet.getValue().getRelatedTrackTitle()).isEqualTo("related title");
    }

    @Test
    public void shouldCreateAdViewForAudioAds() {
        adapter.onViewCreated(container, skipListener, viewVisibilityProvider);
        setupAudioAd();
        getAdPageView();

        verify(adPagePresenter).createItemView(container, skipListener);
    }

    @Test
    public void shouldBindAdViewForAudioAds() {
        adapter.onResume();
        setupAudioAd();
        View pageView = getAdPageView();
        ArgumentCaptor<PlayerAd> captorPropertySet = ArgumentCaptor.forClass(PlayerAd.class);

        verify(adPagePresenter).bindItemView(eq(pageView), captorPropertySet.capture());

        assertThat(captorPropertySet.getValue().getArtwork()).isNotNull();
        assertThat(captorPropertySet.getValue().getMonetizableTrack()).isEqualTo(MONETIZABLE_TRACK_URN);
        assertThat(captorPropertySet.getValue().getPreviewTitle(resources())).isEqualTo("Next up: title (artist)");
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
        PlaybackProgress firstProgress = new PlaybackProgress(100, 200);
        PlaybackProgress secondProgress = new PlaybackProgress(50, 100);

        View firstTrack = getPageView(0, TRACK1_URN);
        View secondTrack = getPageView(1, TRACK2_URN);
        getPageView(2, AD_URN);

        when(trackPagePresenter.accept(firstTrack)).thenReturn(true);
        when(trackPagePresenter.accept(secondTrack)).thenReturn(true);
        when(playSessionStateProvider.hasLastKnownProgress(TRACK1_URN)).thenReturn(true);
        when(playSessionStateProvider.getLastProgressForTrack(TRACK1_URN)).thenReturn(firstProgress);
        when(playSessionStateProvider.hasLastKnownProgress(TRACK2_URN)).thenReturn(true);
        when(playSessionStateProvider.getLastProgressForTrack(TRACK2_URN)).thenReturn(secondProgress);
        Mockito.reset(adPagePresenter); // progress gets set on initial bind, which we are not testing

        adapter.onTrackChange();

        verify(trackPagePresenter).setProgress(firstTrack, firstProgress);
        verify(trackPagePresenter).setProgress(secondTrack, secondProgress);
        verify(adPagePresenter, never()).setProgress(any(View.class), any(PlaybackProgress.class));
    }

    @Test
    public void getItemPositionReturnsNoneForCustomRecycling() {
        assertThat(adapter.getItemPosition(getPageView())).isSameAs(POSITION_NONE);
    }

    @Test
    public void onPlayerSlideForwardsPositionToAdPresenter() {
        setupAudioAd();
        final View pageView = getAdPageView();
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
        final View pageView = getPageView(1, TRACK2_URN);

        verify(trackPagePresenter).onPositionSet(pageView, 1, 4);
    }

    @Test
    public void bindingTrackViewSetsCastDeviceNameOnPresenter() {
        final String castDeviceName = "the google cast device name";
        when(castConnectionHelper.getDeviceName()).thenReturn(castDeviceName);

        final View pageView = getPageView(1, TRACK2_URN);

        verify(trackPagePresenter).setCastDeviceName(pageView, castDeviceName);
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
        adapter.onResume();
        final View view = getPageView();
        when(trackPagePresenter.clearItemView(view)).thenReturn(view);
        when(trackPagePresenter.accept(view)).thenReturn(true);

        adapter.destroyItem(container, 0, view);

        View currentPageView = (View) adapter.instantiateItem(container, 0);

        verify(trackPagePresenter, times(2)).onForeground(currentPageView);
    }

    @Test
    public void destroyingViewDoesNotSetBackgroundStateOnPresenterOnCurrentTrack() {
        final View view = getPageView();
        when(trackPagePresenter.accept(view)).thenReturn(true);
        when(playQueueManager.isCurrentTrack(eq(TRACK1_URN))).thenReturn(true);

        adapter.destroyItem(container, 0, view);

        verify(trackPagePresenter, never()).onBackground(view);
    }

    @Test
    public void destroyingViewSetsBackgroundStateOnPresenter() {
        final View view = getPageView();
        when(trackPagePresenter.accept(view)).thenReturn(true);
        when(playQueueManager.isCurrentTrack(eq(TRACK1_URN))).thenReturn(false);

        adapter.destroyItem(container, 0, view);

        verify(trackPagePresenter).onBackground(view);
    }

    @Test
    public void instantiateItemForMonetizableSetsAdOverlay() throws Exception {
        final View viewForTrack = getPageView(3, MONETIZABLE_TRACK_URN);
        final PropertySet value = TestPropertySets.interstitialForPlayer()
                .put(TrackProperty.URN, MONETIZABLE_TRACK_URN)
                .put(TrackProperty.TITLE, "title")
                .put(TrackProperty.CREATOR_NAME, "artist");

        verify(trackPagePresenter).setAdOverlay(same(viewForTrack), eq(value));
    }

    @Test
    public void instantiateItemWithoutLeaveBehindClearsLeaveBehind() throws Exception {
        final View view = getPageView();
        verify(trackPagePresenter).clearAdOverlay(view);
    }

    @Test
    public void trackChangeEventDismissesLeaveBehindsOnNonPlayingTracks() throws Exception {
        final View viewForCurrentTrack = getPageView(1, TRACK2_URN);
        final View viewForOtherTrack = getPageView(3, MONETIZABLE_TRACK_URN);
        setCurrentTrackState(3, MONETIZABLE_TRACK_URN, true);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(MONETIZABLE_TRACK_URN, Urn.NOT_SET, 0));

        verify(trackPagePresenter, times(2)).clearAdOverlay(viewForCurrentTrack);
        verify(trackPagePresenter, never()).clearAdOverlay(viewForOtherTrack);

    }

    private View getPageView() {
        setCurrentTrackState(0, TRACK1_URN, true);
        return getPageView(0, TRACK1_URN);
    }

    private View getAdPageView() {
        setCurrentTrackState(0, AD_URN, true);
        return getPageView(0, AD_URN);
    }

    private View getPageView(int position, Urn trackUrn) {
        setupGetCurrentViewPreconditions(position, trackUrn);
        return (View) adapter.instantiateItem(container, position);
    }

    private void setupGetCurrentViewPreconditions(int position, Urn trackUrn) {
        track.put(TrackProperty.URN, trackUrn);
        when(playQueueManager.getUrnAtPosition(position)).thenReturn(trackUrn);
        when(trackRepository.track(trackUrn)).thenReturn(Observable.just(track));
    }

    private void setCurrentTrackState(int position, Urn trackUrn, boolean isCurrentTrack) {
        if (isCurrentTrack) {
            when(playQueueManager.getCurrentPosition()).thenReturn(position);
        }
        when(playQueueManager.isCurrentPosition(position)).thenReturn(isCurrentTrack);
        when(playQueueManager.isCurrentTrack(trackUrn)).thenReturn(isCurrentTrack);
        when(playQueueManager.getUrnAtPosition(position)).thenReturn(trackUrn);
    }

    private void setupAudioAd() {
        setupAudioAd(getAudioAd());
    }

    private PropertySet getAudioAd() {
        return PropertySet.from(
                AdProperty.AUDIO_AD_URN.bind(AD_URN.toString()),
                AdProperty.ARTWORK.bind(Uri.parse("http://artwork.com")),
                AdProperty.MONETIZABLE_TRACK_URN.bind(MONETIZABLE_TRACK_URN),
                AdProperty.DEFAULT_TEXT_COLOR.bind("#111111"),
                AdProperty.DEFAULT_BACKGROUND_COLOR.bind("#222222"),
                AdProperty.PRESSED_TEXT_COLOR.bind("#333333"),
                AdProperty.PRESSED_BACKGROUND_COLOR.bind("#444444"),
                AdProperty.FOCUSED_TEXT_COLOR.bind("#555555"),
                AdProperty.FOCUSED_BACKGROUND_COLOR.bind("#666666"));
    }

    private void setupAudioAd(PropertySet propertySet) {
        adapter.setCurrentData(Arrays.asList(new TrackPageData(2, AD_URN, propertySet, Urn.NOT_SET)));
    }
}
