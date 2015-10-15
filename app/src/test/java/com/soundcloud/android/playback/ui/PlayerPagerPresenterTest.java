package com.soundcloud.android.playback.ui;

import static android.support.v4.view.PagerAdapter.POSITION_NONE;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.Player.PlayerState;
import com.soundcloud.android.playback.Player.Reason;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.net.Uri;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

public class PlayerPagerPresenterTest extends AndroidUnitTest {

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

    @Mock private PlayerTrackPager playerTrackPager;

    @Captor private ArgumentCaptor<SkipListener> skipListenerArgumentCaptor;
    @Mock private ViewGroup container;
    @Mock private ViewVisibilityProvider viewVisibilityProvider;

    @Mock private View view1;
    @Mock private View view2;
    @Mock private View view3;
    @Mock private View view4;
    @Mock private View view5;
    @Mock private View view6;
    @Mock private View adView;

    @Mock private PlayerFragment playerFragment;

    private TestEventBus eventBus;
    private PlayerPagerPresenter presenter;
    private PropertySet track;
    private PagerAdapter adapter;

    private List<TrackPageData> trackPageData = newArrayList(
            new TrackPageData(0, TRACK1_URN, PropertySet.create(), Urn.NOT_SET),
            new TrackPageData(1, TRACK2_URN, PropertySet.create(), TRACK2_RELATED_URN),
            new TrackPageData(2, AD_URN, getAudioAd(), Urn.NOT_SET),
            new TrackPageData(3, MONETIZABLE_TRACK_URN, TestPropertySets.interstitialForPlayer(), Urn.NOT_SET));

    @Before
    public void setUp() throws Exception {

        when(trackPagePresenter.createItemView(any(ViewGroup.class), any(SkipListener.class))).thenReturn(view1, view2, view3, view4, view5, view6);
        when(adPagePresenter.createItemView(any(ViewGroup.class), any(SkipListener.class))).thenReturn(adView);

        eventBus = new TestEventBus();
        presenter = new PlayerPagerPresenter(playQueueManager,
                playSessionStateProvider,
                trackRepository,
                trackPagePresenter,
                adPagePresenter,
                castConnectionHelper,
                eventBus);

        when(container.findViewById(R.id.player_track_pager)).thenReturn(playerTrackPager);
        when(container.getResources()).thenReturn(resources());
        presenter.onViewCreated(playerFragment, container, null);
        final ArgumentCaptor<PagerAdapter> pagerAdapterCaptor = ArgumentCaptor.forClass(PagerAdapter.class);
        verify(playerTrackPager).setAdapter(pagerAdapterCaptor.capture());
        adapter = pagerAdapterCaptor.getValue();

        presenter.setCurrentData(trackPageData);

        track = PropertySet.from(TrackProperty.URN.bind(TRACK1_URN),
                PlayableProperty.TITLE.bind("title"),
                PlayableProperty.CREATOR_NAME.bind("artist"),
                PlayableProperty.CREATOR_URN.bind(Urn.forUser(123l)));

        when(trackRepository.track(MONETIZABLE_TRACK_URN)).thenReturn(Observable.just(
                PropertySet.from(
                        TrackProperty.URN.bind(MONETIZABLE_TRACK_URN),
                        PlayableProperty.TITLE.bind("title"),
                        PlayableProperty.CREATOR_NAME.bind("artist"),
                        PlayableProperty.CREATOR_URN.bind(Urn.forUser(123l)))
        ));

        when(trackRepository.track(TRACK2_RELATED_URN)).thenReturn(Observable.just(
                PropertySet.from(
                        TrackProperty.URN.bind(TRACK2_RELATED_URN),
                        PlayableProperty.TITLE.bind("related title"),
                        PlayableProperty.CREATOR_NAME.bind("related artist"),
                        PlayableProperty.CREATOR_URN.bind(Urn.forUser(234l)))
        ));
    }

    @Test
    public void onNextOnSkipListenerSetsPagerToNextPosition() {
        verify(trackPagePresenter, atLeastOnce()).createItemView(same(playerTrackPager), skipListenerArgumentCaptor.capture());
        when(playerTrackPager.getCurrentItem()).thenReturn(3);

        skipListenerArgumentCaptor.getValue().onNext();

        verify(playerTrackPager).setCurrentItem(eq(4));
    }

    @Test
    public void onNextOnSkipListenerEmitsPlayerSkipClickEvent() {
        verify(trackPagePresenter, atLeastOnce()).createItemView(same(playerTrackPager), skipListenerArgumentCaptor.capture());
        when(playerTrackPager.getCurrentItem()).thenReturn(3);

        skipListenerArgumentCaptor.getValue().onNext();

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event).isEqualTo(PlayControlEvent.skip(PlayControlEvent.SOURCE_FULL_PLAYER));
    }

    @Test
    public void onPreviousOnSkipListenerSetsPagerToPreviousPosition() {
        verify(trackPagePresenter, atLeastOnce()).createItemView(same(playerTrackPager), skipListenerArgumentCaptor.capture());
        when(playerTrackPager.getCurrentItem()).thenReturn(3);

        skipListenerArgumentCaptor.getValue().onPrevious();

        verify(playerTrackPager).setCurrentItem(eq(2));
    }

    @Test
    public void onPreviousOnSkipListenerEmitsPlayerPreviousClickEvent() {
        verify(trackPagePresenter, atLeastOnce()).createItemView(same(playerTrackPager), skipListenerArgumentCaptor.capture());
        when(playerTrackPager.getCurrentItem()).thenReturn(3);

        skipListenerArgumentCaptor.getValue().onPrevious();

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event).isEqualTo(PlayControlEvent.previous(PlayControlEvent.SOURCE_FULL_PLAYER));
    }

    @Test
    public void onPlayingStateEventCallsSetPlayStateOnPresenter() {
        presenter.onResume(playerFragment);
        final View currentTrackView = getPageView();
        Player.StateTransition state = new Player.StateTransition(PlayerState.PLAYING, Reason.NONE, TRACK1_URN);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);

        verify(trackPagePresenter).setPlayState(currentTrackView, state, true, true);
    }

    @Test
    public void onPlayingStateEventCallsSetPlayStateForOtherPage() {
        presenter.onResume(playerFragment);
        setCurrentTrackState(0, TRACK1_URN, true);
        setCurrentTrackState(1, TRACK2_URN, false);

        final View viewForCurrentTrack = getPageView(0, TRACK1_URN);
        final View viewForOtherTrack = getPageView(1, TRACK2_URN);

        Mockito.reset(trackPagePresenter);
        Player.StateTransition state = new Player.StateTransition(PlayerState.PLAYING, Reason.NONE, TRACK1_URN);
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
        presenter.onResume(playerFragment);
        View currentPageView = getPageView();
        PlaybackProgressEvent event = new PlaybackProgressEvent(new PlaybackProgress(5l, 10l), TRACK1_URN);

        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, event);

        verify(trackPagePresenter).setProgress(currentPageView, event.getPlaybackProgress());
    }

    @Test
    public void onPlaybackProgressEventDoNotSetsProgressForPausedAdapter() {
        presenter.onResume(playerFragment);
        View currentPageView = getPageView();
        PlaybackProgressEvent event = new PlaybackProgressEvent(new PlaybackProgress(5l, 10l), TRACK1_URN);
        presenter.onPause(playerFragment);

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
        presenter.onResume(playerFragment);
        Player.StateTransition state = new Player.StateTransition(PlayerState.PLAYING, Reason.NONE, TRACK1_URN);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);

        View currentPageView = getPageView();

        verify(trackPagePresenter).setPlayState(eq(currentPageView), any(Player.StateTransition.class), eq(true), eq(true));
    }

    @Test
    public void recyclingTrackViewDoesNotSetThePlayState() {
        final View view = getPageView();
        Mockito.reset(trackPagePresenter);
        when(trackPagePresenter.clearItemView(view)).thenReturn(view);
        when(trackPagePresenter.accept(view)).thenReturn(true);

        adapter.destroyItem(container, 3, view);

        View currentPageView = (View) adapter.instantiateItem(container, 3);

        verify(trackPagePresenter, never()).setPlayState(eq(currentPageView), any(Player.StateTransition.class), eq(true), eq(true));
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

        presenter.onResume(playerFragment);
        final View pageView = getPageView();

        verify(trackPagePresenter).bindItemView(same(pageView), captorPropertySet.capture());

        assertThat(captorPropertySet.getValue().getTrackUrn()).isEqualTo(TRACK1_URN);
        assertThat(captorPropertySet.getValue().getTitle()).isEqualTo("title");
        assertThat(captorPropertySet.getValue().getUserName()).isEqualTo("artist");
        assertThat(captorPropertySet.getValue().isForeground()).isTrue();
        assertThat(captorPropertySet.getValue().isCurrentTrack()).isTrue();
    }

    @Test
    public void shouldBindTrackViewForTrackWithRelatedTrack() {
        ArgumentCaptor<PlayerTrackState> captorPropertySet = ArgumentCaptor.forClass(PlayerTrackState.class);

        presenter.onResume(playerFragment);

        setCurrentTrackState(1, TRACK2_URN, true);
        final View pageView = getPageView(1, TRACK2_URN);

        verify(trackPagePresenter).bindItemView(same(pageView), captorPropertySet.capture());

        assertThat(captorPropertySet.getValue().getTrackUrn()).isEqualTo(TRACK2_URN);
        assertThat(captorPropertySet.getValue().getTitle()).isEqualTo("title");
        assertThat(captorPropertySet.getValue().getUserName()).isEqualTo("artist");
        assertThat(captorPropertySet.getValue().isForeground()).isTrue();
        assertThat(captorPropertySet.getValue().isCurrentTrack()).isTrue();
        assertThat(captorPropertySet.getValue().getRelatedTrackUrn()).isEqualTo(TRACK2_RELATED_URN);
        assertThat(captorPropertySet.getValue().getRelatedTrackTitle()).isEqualTo("related title");
    }

    @Test
    public void shouldCreateAdViewForAudioAds() {
        presenter.onViewCreated(playerFragment, container, null);
        setupAudioAd();
        getAdPageView();

        verify(adPagePresenter).createItemView(any(ViewGroup.class), any(SkipListener.class));
    }

    @Test
    public void shouldBindAdViewForAudioAds() {
        presenter.onResume(playerFragment);
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

        presenter.onTrackChange();

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

        presenter.onTrackChange();

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
        presenter.onPlayerSlide(0.5f);
        verify(adPagePresenter).onPlayerSlide(pageView, 0.5f);
    }

    @Test
    public void onPlayerSlideForwardsPositionToTrackPresenter() {
        final View pageView = getPageView();
        presenter.onPlayerSlide(0.5f);
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
        presenter.onPause(playerFragment);
        verify(trackPagePresenter).onBackground(pageView);
    }

    @Test
    public void onResumeSetsForegroundStateOnPresenter() {
        final View pageView = getPageView();
        presenter.onResume(playerFragment);
        verify(trackPagePresenter).onForeground(pageView);
    }

    @Test
    public void reusingExistingViewSetsForegroundStateOnPresenter() {
        presenter.onResume(playerFragment);
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
                .put(TrackProperty.CREATOR_NAME, "artist")
                .put(TrackProperty.CREATOR_URN, Urn.forUser(123l));

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
                AdProperty.AD_URN.bind(AD_URN.toString()),
                AdProperty.AD_TYPE.bind(AdProperty.AD_TYPE_AUDIO),
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
        presenter.setCurrentData(Arrays.asList(new TrackPageData(2, AD_URN, propertySet, Urn.NOT_SET)));
    }
}
