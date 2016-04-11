package com.soundcloud.android.playlists;

import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloaded;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloading;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.removed;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.requested;
import static com.soundcloud.android.playlists.PlaylistEngagementsView.OnEngagementListener;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.share.ShareOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlaylistViewHeaderPresenterTest extends AndroidUnitTest {

    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123);
    private static final Screen SCREEN = Screen.SEARCH_MAIN;

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.playlist_fragment, fragmentArgs());

    private PlaylistViewHeaderPresenter controller;
    private PlaylistWithTracks playlistWithTracks;
    private PublishSubject<Void> publishSubject;
    private TestEventBus eventBus;
    private Observable<List<Urn>> playlistTrackurns = Observable.just(Collections.singletonList(Urn.forTrack(1)));

    @Mock private RepostOperations repostOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private LikeOperations likeOperations;
    @Mock private PlaylistEngagementsView engagementsView;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private PlaylistOperations playlistOperations;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private PlaybackToastHelper playbackToastHelper;
    @Mock private Navigator navigator;
    @Mock private NetworkConnectionHelper connectionHelper;
    @Mock private OfflineSettingsOperations offlineSettings;
    @Mock private ShareOperations shareOperations;
    @Mock private PlaylistDetailsViewFactory playlistDetailsViewFactory;
    @Mock private PlaylistDetailsView playlistDetailsView;
    @Mock private PlaylistHeaderListener headerListener;

    @Captor private ArgumentCaptor<OnEngagementListener> listenerCaptor;

    private static Bundle fragmentArgs() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(LegacyPlaylistDetailFragment.EXTRA_URN, PLAYLIST_URN);
        return bundle;
    }

    @Before
    public void setup() {
        eventBus = new TestEventBus();
        controller = new PlaylistViewHeaderPresenter(
                repostOperations,
                accountOperations,
                eventBus,
                likeOperations,
                engagementsView,
                featureOperations,
                offlineContentOperations,
                playbackInitiator,
                playlistOperations,
                playbackToastHelper,
                navigator,
                shareOperations,
                connectionHelper,
                offlineSettings);

        when(playlistDetailsViewFactory.create(any(View.class))).thenReturn(playlistDetailsView);
        controller.setListener(headerListener);
        playlistWithTracks = createPlaylistInfoWithSharing(Sharing.PUBLIC);

        publishSubject = PublishSubject.create();

        when(playlistOperations.trackUrnsForPlayback(playlistWithTracks.getUrn())).thenReturn(playlistTrackurns);
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
        controller.setScreen(SCREEN.get());
    }

    @After
    public void tearDown() {
        controller.onPause(fragmentRule.getFragment());
    }

    @Test
    public void shouldPublishUIEventWhenLikingAPlaylist() {
        setPlaylistInfo();
        when(likeOperations.toggleLike(any(Urn.class), anyBoolean())).thenReturn(Observable.<PropertySet>empty());

        controller.onToggleLike(true);

        UIEvent uiEvent = (UIEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getContextScreen()).isEqualTo(SCREEN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnlikingPlaylist() {
        setPlaylistInfo();
        when(likeOperations.toggleLike(playlistWithTracks.getUrn(), false)).thenReturn(Observable.just(PropertySet.create()));

        controller.onToggleLike(false);

        UIEvent uiEvent = (UIEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_UNLIKE);
        assertThat(uiEvent.getContextScreen()).isEqualTo(SCREEN.get());
    }

    @Test
    public void shouldPublishUIEventWhenRepostingPlayable() {
        setPlaylistInfo();
        when(repostOperations.toggleRepost(any(Urn.class), anyBoolean())).thenReturn(Observable.just(PropertySet.create()));

        controller.onToggleRepost(true, false);

        UIEvent uiEvent = (UIEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo(SCREEN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnrepostingPlayable() {
        setPlaylistInfo();
        when(repostOperations.toggleRepost(any(Urn.class), anyBoolean())).thenReturn(Observable.just(PropertySet.create()));

        controller.onToggleRepost(false, false);

        UIEvent uiEvent = (UIEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo(SCREEN.get());
    }

    @Test
    public void shouldPublishUIEventWhenSharingPlayable() {
        setPlaylistInfo();

        controller.onShare();
        EventContextMetadata eventContextMetadata = EventContextMetadata.builder()
                .contextScreen(SCREEN.get())
                .pageName(Screen.PLAYLIST_DETAILS.get())
                .pageUrn(playlistWithTracks.getUrn())
                .invokerScreen(Screen.PLAYLIST_DETAILS.get())
                .build();
        verify(shareOperations).share(getContext(), playlistWithTracks.getSourceSet(), eventContextMetadata, null);
    }

    @Test
    public void shouldNotPublishUIEventWhenTrackIsNull() {
        controller.onShare();

        verify(shareOperations, never())
                .share(any(Context.class), any(PropertySet.class),
                        any(EventContextMetadata.class), any(PromotedSourceInfo.class));
    }

    @Test
    public void shouldLikePlaylistWhenCheckingLikeButton() {
        setPlaylistInfo();
        when(likeOperations.toggleLike(playlistWithTracks.getUrn(), true)).thenReturn(Observable.just(PropertySet.create()));

        controller.onToggleLike(true);

        verify(likeOperations).toggleLike(playlistWithTracks.getUrn(), true);
    }

    @Test
    public void shouldRepostTrackWhenCheckingRepostButton() {
        setPlaylistInfo();
        when(repostOperations.toggleRepost(any(Urn.class), anyBoolean())).thenReturn(Observable.just(PropertySet.create()));

        controller.onToggleRepost(true, false);

        verify(repostOperations).toggleRepost(eq(playlistWithTracks.getUrn()), eq(true));
    }

    @Test
    public void shouldPlayShuffledThroughContentOperationsOnPlayShuffled() {
        setPlaylistInfo();
        final PublishSubject<PlaybackResult> subject = PublishSubject.create();
        when(playbackInitiator.playTracksShuffled(playlistTrackurns, getPlaySessionSource(), featureOperations.isOfflineContentEnabled()))
                .thenReturn(subject);

        controller.onPlayShuffled();

        assertThat(subject.hasObservers()).isTrue();
    }

    @Test
    public void shouldOpenUpgradeScreenWhenClickingOnUpsell() {
        setPlaylistInfo();

        controller.onUpsell(getContext());

        verify(navigator).openUpgrade(getContext());
    }

    @Test
    public void updatesLikeItemOnPresenterUpdate() {
        setPlaylistInfo();

        controller.update(EntityStateChangedEvent.fromLike(playlistWithTracks.getUrn(), true, playlistWithTracks.getLikesCount()));

        verify(engagementsView).updateLikeItem(playlistWithTracks.getLikesCount(), true);

    }

    @Test
    public void shouldUpdateLikeOrRepostButtonWhenCurrentPlayableChanged() {
        setPlaylistInfo();

        controller.update(EntityStateChangedEvent.fromLike(playlistWithTracks.getUrn(), true, playlistWithTracks.getLikesCount()));
        verify(engagementsView).updateLikeItem(playlistWithTracks.getLikesCount(), true);

        controller.update(EntityStateChangedEvent.fromRepost(playlistWithTracks.getUrn(), true));
        verify(engagementsView).showPublicOptions(true);
    }

    @Test
    public void shouldNotUpdateLikeOrRepostButtonStateForOtherPlayables() {
        setPlaylistInfo();

        controller.update(EntityStateChangedEvent.fromLike(Urn.forTrack(2L), true, 1));
        controller.update(EntityStateChangedEvent.fromRepost(Urn.forTrack(2L), true));

        verify(engagementsView, never()).updateLikeItem(anyInt(), eq(true));
        verify(engagementsView, never()).showPublicOptions(eq(true));
    }

    @Test
    public void shouldUpdateOfflineAvailabilityOnMarkedForOfflineChange() {
        setPlaylistInfo();
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, requested(singletonList(playlistWithTracks.getUrn()), false));

        verify(engagementsView).showMakeAvailableOfflineButton(true);
    }

    @Test
    public void shouldUpdateOfflineAvailabilityOnUnmarkedForOfflineChange() {
        setPlaylistInfo();
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, removed(playlistWithTracks.getUrn()));

        verify(engagementsView).showMakeAvailableOfflineButton(false);
    }

    @Test
    public void shouldGetContextFromOriginProvider() {
        setPlaylistInfo();

        controller.onShare();
        EventContextMetadata eventContextMetadata = EventContextMetadata.builder()
                .contextScreen(Screen.SEARCH_MAIN.get())
                .pageName(Screen.PLAYLIST_DETAILS.get())
                .pageUrn(playlistWithTracks.getUrn())
                .invokerScreen(Screen.PLAYLIST_DETAILS.get())
                .build();
        verify(shareOperations).share(getContext(), playlistWithTracks.getSourceSet(), eventContextMetadata, null);
    }

    @Test
    public void makeOfflineAvailableUsesOfflineOperationsToMakeOfflineAvailable() {
        setPlaylistInfo();
        when(offlineContentOperations.makePlaylistAvailableOffline(playlistWithTracks.getUrn())).thenReturn(publishSubject);

        controller.onMakeOfflineAvailable(true);

        assertThat(publishSubject.hasObservers()).isTrue();
    }

    @Test
    public void makeOfflineUnavailableUsesOfflineOperationsToMakeOfflineUnavailable() {
        setPlaylistInfo();
        when(offlineContentOperations.makePlaylistUnavailableOffline(playlistWithTracks.getUrn())).thenReturn(publishSubject);

        controller.onMakeOfflineAvailable(false);

        assertThat(publishSubject.hasObservers()).isTrue();
    }

    @Test
    public void showsOfflineAvailableWhenOfflineContentIsEnabledAndPlaylistCurrentlyMarkedAvailable() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        setPlaylistInfo(createPlaylistProperties(Sharing.PUBLIC)
                .put(OfflineProperty.IS_MARKED_FOR_OFFLINE, true)
                .put(PlaylistProperty.IS_POSTED, true));

        verify(engagementsView).showMakeAvailableOfflineButton(true);
    }

    @Test
    public void showsNotOfflineAvailableWhenOfflineContentIsEnabledAndPlaylistCurrentlyMarkedUnavailable() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        setPlaylistInfo(createPlaylistProperties(Sharing.PUBLIC)
                .put(PlaylistProperty.IS_POSTED, true));

        verify(engagementsView).showMakeAvailableOfflineButton(false);
    }

    @Test
    public void showsUpsellWhenOfflineContentIsNotEnabledAndAllowedToShowUpsell() {
        when(featureOperations.upsellOfflineContent()).thenReturn(true);

        setPlaylistInfo(createPlaylistProperties(Sharing.PUBLIC)
                .put(OfflineProperty.IS_MARKED_FOR_OFFLINE, true)
                .put(PlaylistProperty.IS_POSTED, true));

        verify(engagementsView).showUpsell();
    }

    @Test
    public void hidesOfflineOptionsWhenOfflineContentIsNotEnabledAndNotAllowedToShowUpsell() {
        setPlaylistInfo(createPlaylistProperties(Sharing.PUBLIC)
                .put(OfflineProperty.IS_MARKED_FOR_OFFLINE, true)
                .put(PlaylistProperty.IS_POSTED, true));

        verify(engagementsView).hideMakeAvailableOfflineButton();
    }

    @Test
    public void hidesOfflineOptionsWhenPlaylistIsNotPostedByTheCurrentUser() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        setPlaylistInfo(createPlaylistProperties(Sharing.PUBLIC));

        verify(engagementsView).hideMakeAvailableOfflineButton();
    }

    @Test
    public void showsOfflineOptionsWhenPlaylistIsLikedByTheCurrentUser() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        setPlaylistInfo(createPlaylistProperties(Sharing.PUBLIC)
                .put(PlaylistProperty.IS_USER_LIKE, true));

        verify(engagementsView).showMakeAvailableOfflineButton(false);
    }

    @Test
    public void hidesOfflineOptionsWhenPlaylistIsNotLikedByTheCurrentUser() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        setPlaylistInfo(createPlaylistProperties(Sharing.PUBLIC));

        verify(engagementsView).hideMakeAvailableOfflineButton();
    }

    @Test
    public void showDefaultDownloadStateWhenCurrentDownloadEmitsDownloadRemoved() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setPlaylistInfo();
        reset(engagementsView);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, removed(playlistWithTracks.getUrn()));

        verify(engagementsView).showOfflineState(OfflineState.NOT_OFFLINE);
    }

    @Test
    public void showDefaultDownloadStateWhenCurrentDownloadEmitsRequested() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setPlaylistInfo();

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, requested(singletonList(playlistWithTracks.getUrn()), false));

        verify(engagementsView).showOfflineState(OfflineState.REQUESTED);
    }

    @Test
    public void setDownloadingStateWhenCurrentDownloadEmitsDownloading() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setPlaylistInfo();

        final ApiTrack track = ModelFixtures.create(ApiTrack.class);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, downloading(Arrays.asList(track.getUrn(), playlistWithTracks.getUrn()), true));

        verify(engagementsView).showOfflineState(OfflineState.DOWNLOADING);
    }

    @Test
    public void setDownloadedStateWhenCurrentDownloadEmitsDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        setPlaylistInfo();
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                downloaded(singletonList(playlistWithTracks.getUrn()), false));

        final InOrder inOrder = inOrder(engagementsView);
        inOrder.verify(engagementsView).showOfflineState(OfflineState.NOT_OFFLINE);
        inOrder.verify(engagementsView).showOfflineState(OfflineState.DOWNLOADED);
    }

    @Test
    public void ignoreDownloadStateWhenCurrentDownloadEmitsAnUnrelatedEvent() {
        setPlaylistInfo();
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, downloaded(singletonList(Urn.forPlaylist(12344443212L)), false));

        verify(engagementsView, never()).showOfflineState(OfflineState.DOWNLOADED);
    }

    @Test
    public void showDefaultDownloadStateWhenPlaylistDownloadStateIsDownloadNoOffline() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        playlistWithTracks.getSourceSet().put(OfflineProperty.OFFLINE_STATE, OfflineState.NOT_OFFLINE);
        setPlaylistInfo();

        verify(engagementsView).showOfflineState(OfflineState.NOT_OFFLINE);
    }

    @Test
    public void showDefaultDownloadStateWhenPlaylistDownloadStateRequested() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        playlistWithTracks.getSourceSet().put(OfflineProperty.OFFLINE_STATE, OfflineState.REQUESTED);
        setPlaylistInfo();

        verify(engagementsView).showOfflineState(OfflineState.REQUESTED);
    }

    @Test
    public void setDownloadingStateWhenPlaylistDownloadStateDownloading() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        playlistWithTracks.getSourceSet().put(OfflineProperty.OFFLINE_STATE, OfflineState.DOWNLOADING);
        setPlaylistInfo();

        verify(engagementsView).showOfflineState(OfflineState.DOWNLOADING);
    }

    @Test
    public void setDownloadedStateWhenPlaylistDownloadStateDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        playlistWithTracks.getSourceSet().put(OfflineProperty.OFFLINE_STATE, OfflineState.DOWNLOADED);
        setPlaylistInfo();

        verify(engagementsView).showOfflineState(OfflineState.DOWNLOADED);
    }

    @Test
    public void showWarningTextWhenPendingDownloadAndOffline() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setPlaylistInfo();
        when(connectionHelper.isNetworkConnected()).thenReturn(false);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, requested(singletonList(playlistWithTracks.getUrn()), false));

        verify(engagementsView).showNoConnection();
    }

    @Test
    public void showWarningTextWhenPendingDownloadAndWifiOnly() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setPlaylistInfo();
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(true);
        when(connectionHelper.isWifiConnected()).thenReturn(false);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, requested(singletonList(playlistWithTracks.getUrn()), false));

        verify(engagementsView).showNoWifi();
    }

    @Test
    public void doNotShowWarningTextForNonPendingStates() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setPlaylistInfo();
        when(connectionHelper.isNetworkConnected()).thenReturn(false);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, downloaded(singletonList(playlistWithTracks.getUrn()), false));

        verify(engagementsView, times(0)).showNoConnection();
    }

    @Test
    public void disablesShuffleWithOneTrack() throws Exception {
        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC)
                .put(PlaylistProperty.IS_POSTED, true)
                .put(PlaylistProperty.TRACK_COUNT, 1);

        setPlaylistInfo(PlaylistHeaderItem.create(createPlaylistWithSingleTrack(sourceSet), getPlaySessionSource()));

        verify(engagementsView).disableShuffle();
    }

    @Test
    public void enablesShuffleWithMoreThanOneTrack() throws Exception {
        final PropertySet sourceSet = createPlaylistProperties(Sharing.PUBLIC)
                .put(PlaylistProperty.IS_POSTED, true);

        setPlaylistInfo(sourceSet);

        verify(engagementsView).enableShuffle();
    }

    @Test
    public void shouldTrackUpsellImpressionInOnCreateWhenFeatureAvailable() {
        when(featureOperations.upsellOfflineContent()).thenReturn(true);

        controller.onCreate(fragmentRule.getFragment(), null);

        UpgradeFunnelEvent event = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);
        assertThat(event.get(UpgradeFunnelEvent.KEY_PAGE_URN)).isEqualTo(PLAYLIST_URN.toString());
    }

    @Test
    public void shouldNotTrackUpsellImpressionInOnCreateWhenFeatureNotAvailable() {
        when(featureOperations.upsellOfflineContent()).thenReturn(false);

        controller.onCreate(fragmentRule.getFragment(), null);

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    private void setPlaylistInfo() {
        setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
    }

    private void setPlaylistInfo(PropertySet sourceSet) {
        setPlaylistInfo(PlaylistHeaderItem.create(createPlaylistWithTracks(sourceSet), getPlaySessionSource()));
    }

    private void setPlaylistInfo(PlaylistHeaderItem playlistHeaderItem) {
        controller.onCreate(fragmentRule.getFragment(), null);
        controller.onResume(fragmentRule.getFragment());
        controller.setPlaylistInfo(playlistHeaderItem);
    }

    private PlaylistWithTracks createPlaylistInfoWithSharing(Sharing sharing) {
        final PropertySet sourceSet = createPlaylistProperties(sharing)
                .put(PlaylistProperty.IS_POSTED, true);
        return createPlaylistWithTracks(sourceSet);
    }

    private PlaylistWithTracks createPlaylistWithTracks(PropertySet sourceSet) {
        return new PlaylistWithTracks(sourceSet, ModelFixtures.trackItems(10));
    }

    private PlaylistWithTracks createPlaylistWithSingleTrack(PropertySet sourceSet) {
        return new PlaylistWithTracks(sourceSet, ModelFixtures.trackItems(1));
    }

    private PropertySet createPlaylistProperties(Sharing sharing) {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        playlist.setSharing(sharing);
        return playlist.toPropertySet()
                .put(OfflineProperty.IS_MARKED_FOR_OFFLINE, false)
                .put(PlaylistProperty.IS_USER_LIKE, false)
                .put(PlaylistProperty.IS_USER_REPOST, false)
                .put(PlaylistProperty.IS_POSTED, false);
    }

    private PlaySessionSource getPlaySessionSource() {
        return new PlaySessionSource(Screen.PLAYLIST_DETAILS);
    }

    protected Context getContext() {
        return fragmentRule.getFragment().getContext();
    }

}
