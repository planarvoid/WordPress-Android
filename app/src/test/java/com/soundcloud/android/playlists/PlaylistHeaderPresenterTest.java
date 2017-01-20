package com.soundcloud.android.playlists;

import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloaded;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloading;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.removed;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.requested;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent.RepostStatus;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.playqueue.PlayQueueHelper;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.share.ShareOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
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

public class PlaylistHeaderPresenterTest extends AndroidUnitTest {

    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123);
    private static final Screen SCREEN = Screen.SEARCH_MAIN;

    private TestEventBus eventBus;
    @Mock private PlaylistHeaderViewFactory playlistHeaderViewFactory;
    @Mock private Navigator navigator;
    @Mock private PlaylistHeaderScrollHelper profileHeaderScrollHelper;
    @Mock private RepostOperations repostOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private LikeOperations likeOperations;
    @Mock private PlaylistEngagementsView engagementsView;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private PlaylistOperations playlistOperations;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private PlaybackToastHelper playbackToastHelper;
    @Mock private NetworkConnectionHelper connectionHelper;
    @Mock private OfflineSettingsOperations offlineSettings;
    @Mock private ShareOperations shareOperations;
    @Mock private PlaylistHeaderView playlistHeaderView;
    @Mock private PlaylistHeaderListener headerListener;
    @Mock private PlaylistHeaderView headerView;
    @Mock private PlayQueueHelper playQueueHelper;
    @Mock private ScreenProvider screenProvider;
    @Mock private EventTracker eventTracker;
    @Captor private ArgumentCaptor<UIEvent> uiEventCaptor;

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.playlist_fragment, fragmentArgs());

    private Observable<List<Urn>> playlistTrackurns;
    private PlaylistDetailHeaderItem headerItem;
    private PlaylistHeaderPresenter presenter;

    private static Bundle fragmentArgs() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(PlaylistDetailFragment.EXTRA_URN, PLAYLIST_URN);
        return bundle;
    }

    @Before
    public void setup() {
        eventBus = new TestEventBus();
        playlistTrackurns = Observable.just(Collections.singletonList(Urn.forTrack(1)));
        headerItem = createPlaylistInfoWithSharing(Sharing.PUBLIC);
        when(playlistOperations.trackUrnsForPlayback(headerItem.getUrn())).thenReturn(playlistTrackurns);
        when(playlistHeaderViewFactory.create(any(View.class))).thenReturn(headerView);

        presenter = new PlaylistHeaderPresenter(
                eventBus,
                eventTracker,
                playlistHeaderViewFactory,
                navigator,
                profileHeaderScrollHelper,
                featureOperations,
                engagementsView,
                accountOperations,
                offlineContentOperations,
                playbackInitiator,
                playlistOperations,
                playbackToastHelper,
                likeOperations,
                repostOperations,
                shareOperations,
                offlineSettings,
                connectionHelper,
                playQueueHelper);

        presenter.bindItemView(0, View.inflate(activity(), com.soundcloud.android.R.layout.playlist_details_view, null), null);
    }

    @Test
    public void shouldShowUsersOptions() {
        when(accountOperations.isLoggedInUser(headerItem.creatorUrn())).thenReturn(true);

        presenter.setPlaylist(headerItem, getPlaySessionSource());

        verify(engagementsView).showMyOptions();
    }

    @Test
    public void shouldHideUsersOptions() {
        when(accountOperations.isLoggedInUser(headerItem.creatorUrn())).thenReturn(false);

        presenter.setPlaylist(headerItem, getPlaySessionSource());

        verify(engagementsView).hideMyOptions();
    }

    @Test
    public void updatesLikeItemOnPresenterUpdate() {
        setPlaylistInfo();

        eventBus.publish(EventQueue.LIKE_CHANGED,
                         LikesStatusEvent.create(headerItem.getUrn(),
                                                 true,
                                                 headerItem.likesCount()));

        verify(engagementsView).updateLikeItem(Optional.of(headerItem.likesCount()), true);

    }

    @Test
    public void shouldUpdateLikeOrRepostButtonWhenCurrentPlayableChanged() {
        setPlaylistInfo();

        eventBus.publish(EventQueue.LIKE_CHANGED,
                         LikesStatusEvent.create(headerItem.getUrn(),
                                                 true,
                                                 headerItem.likesCount()));
        verify(engagementsView).updateLikeItem(Optional.of(headerItem.likesCount()), true);

        eventBus.publish(EventQueue.REPOST_CHANGED, RepostsStatusEvent.createReposted(headerItem.getUrn()));
        verify(engagementsView).showPublicOptions(true);
    }

    @Test
    public void shouldNotUpdateLikeOrRepostButtonStateForOtherPlayables() {
        setPlaylistInfo();

        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(Urn.forTrack(2L), true, 1));
        eventBus.publish(EventQueue.REPOST_CHANGED, RepostsStatusEvent.createReposted(Urn.forTrack(2L)));

        verify(engagementsView, never()).updateLikeItem(any(Optional.class), eq(true));
        verify(engagementsView, never()).showPublicOptions(eq(true));
    }

    @Test
    public void unsubscribesFromOngoingSubscriptionsWhenActivityDestroyed() {
        presenter.onResume(fragmentRule.getFragment());

        presenter.onPause(fragmentRule.getFragment());

        eventBus.verifyUnsubscribed();
    }

    @Test
    public void shouldPublishUIEventWhenLikingAPlaylist() {
        setPlaylistInfo();
        when(likeOperations.toggleLike(any(Urn.class), anyBoolean())).thenReturn(Observable.empty());
        doNothing().when(eventTracker).trackEngagement(uiEventCaptor.capture());

        presenter.onToggleLike(true);

        UIEvent uiEvent = uiEventCaptor.getValue();
        assertThat(uiEvent.kind()).isSameAs(UIEvent.Kind.LIKE);
        assertThat(uiEvent.contextScreen().get()).isEqualTo(SCREEN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnlikingPlaylist() {
        setPlaylistInfo();
        when(likeOperations.toggleLike(headerItem.getUrn(),
                                       false)).thenReturn(Observable.just(0));
        doNothing().when(eventTracker).trackEngagement(uiEventCaptor.capture());

        presenter.onToggleLike(false);

        UIEvent uiEvent = uiEventCaptor.getValue();
        assertThat(uiEvent.kind()).isSameAs(UIEvent.Kind.UNLIKE);
        assertThat(uiEvent.contextScreen().get()).isEqualTo(SCREEN.get());
    }

    @Test
    public void shouldPublishUIEventWhenRepostingPlayable() {
        setPlaylistInfo();
        when(repostOperations.toggleRepost(any(Urn.class),
                                           anyBoolean())).thenReturn(Observable.just(RepostStatus.createReposted(Urn.NOT_SET)));
        doNothing().when(eventTracker).trackEngagement(uiEventCaptor.capture());

        presenter.onToggleRepost(true, false);

        UIEvent uiEvent = uiEventCaptor.getValue();
        assertThat(uiEvent.kind()).isSameAs(UIEvent.Kind.REPOST);
        assertThat(uiEvent.contextScreen().get()).isEqualTo(SCREEN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnrepostingPlayable() {
        setPlaylistInfo();
        when(repostOperations.toggleRepost(any(Urn.class),
                                           anyBoolean())).thenReturn(Observable.just(RepostStatus.createReposted(Urn.NOT_SET)));
        doNothing().when(eventTracker).trackEngagement(uiEventCaptor.capture());

        presenter.onToggleRepost(false, false);

        UIEvent uiEvent = uiEventCaptor.getValue();
        assertThat(uiEvent.kind()).isSameAs(UIEvent.Kind.UNREPOST);
        assertThat(uiEvent.contextScreen().get()).isEqualTo(SCREEN.get());
    }

    @Test
    public void shouldPublishUIEventWhenSharingPlayable() {
        setPlaylistInfo();

        presenter.onShare();
        EventContextMetadata eventContextMetadata = EventContextMetadata.builder()
                                                                        .contextScreen(SCREEN.get())
                                                                        .pageName(Screen.PLAYLIST_DETAILS.get())
                                                                        .pageUrn(headerItem.getUrn())
                                                                        .invokerScreen(Screen.PLAYLIST_DETAILS.get())
                                                                        .build();
        verify(shareOperations).share(getContext(), headerItem.permalinkUrl().get(), eventContextMetadata, null, entityMetadata());
    }

    @Test
    public void shouldNotPublishUIEventWhenTrackIsNull() {
        presenter.onShare();

        verify(shareOperations, never())
                .share(any(Context.class), any(TrackItem.class),
                       any(EventContextMetadata.class), any(PromotedSourceInfo.class));
    }

    @Test
    public void shouldLikePlaylistWhenCheckingLikeButton() {
        setPlaylistInfo();
        when(likeOperations.toggleLike(headerItem.getUrn(),
                                       true)).thenReturn(Observable.just(0));

        presenter.onToggleLike(true);

        verify(likeOperations).toggleLike(headerItem.getUrn(), true);
    }

    @Test
    public void shouldRepostTrackWhenCheckingRepostButton() {
        setPlaylistInfo();
        when(repostOperations.toggleRepost(any(Urn.class),
                                           anyBoolean())).thenReturn(Observable.just(RepostStatus.createReposted(Urn.NOT_SET)));

        presenter.onToggleRepost(true, false);

        verify(repostOperations).toggleRepost(eq(headerItem.getUrn()), eq(true));
    }

    @Test
    public void shouldPlayShuffledThroughContentOperationsOnPlayShuffled() {
        setPlaylistInfo();
        final PublishSubject<PlaybackResult> subject = PublishSubject.create();
        when(playbackInitiator.playTracksShuffled(playlistTrackurns, getPlaySessionSource()))
                .thenReturn(subject);

        presenter.onPlayShuffled();

        assertThat(subject.hasObservers()).isTrue();
    }

    @Test
    public void shouldOpenUpgradeScreenWhenClickingOnUpsell() {
        setPlaylistInfo();

        presenter.onUpsell(getContext());

        verify(navigator).openUpgrade(getContext());
    }

    @Test
    public void shouldOpenUpgradeScreenWhenClickingOnOverflowUpsell() {
        setPlaylistInfo();

        presenter.onOverflowUpsell(getContext());

        verify(navigator).openUpgrade(getContext());
    }

    @Test
    public void shouldUpdateOfflineAvailabilityOnMarkedForOfflineChange() {
        setPlaylistInfo();
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(accountOperations.isLoggedInUser(headerItem.creatorUrn())).thenReturn(true);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         requested(singletonList(headerItem.getUrn()), false));

        verify(engagementsView).showOfflineOptions(true);
    }

    @Test
    public void shouldUpdateOfflineAvailabilityOnUnmarkedForOfflineChange() {
        setPlaylistInfo();
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(accountOperations.isLoggedInUser(headerItem.creatorUrn())).thenReturn(true);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, removed(headerItem.getUrn()));

        verify(engagementsView).showOfflineOptions(false);
    }

    @Test
    public void shouldGetContextFromOriginProvider() {
        setPlaylistInfo();

        presenter.onShare();
        EventContextMetadata eventContextMetadata = EventContextMetadata.builder()
                                                                        .contextScreen(SCREEN.get())
                                                                        .pageName(Screen.PLAYLIST_DETAILS.get())
                                                                        .pageUrn(headerItem.getUrn())
                                                                        .invokerScreen(Screen.PLAYLIST_DETAILS.get())
                                                                        .build();
        verify(shareOperations).share(getContext(), headerItem.permalinkUrl().get(), eventContextMetadata, null, entityMetadata());
    }

    private EntityMetadata entityMetadata() {
        return EntityMetadata.from(headerItem.creatorName(), headerItem.creatorUrn(), headerItem.title(), headerItem.getUrn());
    }

    @Test
    public void makeOfflineAvailableUsesOfflineOperationsToMakeOfflineAvailable() {
        setPlaylistInfo();
        final PublishSubject<Void> makePlaylistAvailableOffline = PublishSubject.create();
        when(offlineContentOperations.makePlaylistAvailableOffline(headerItem.getUrn())).thenReturn(
                makePlaylistAvailableOffline);

        presenter.onMakeOfflineAvailable(true);

        assertThat(makePlaylistAvailableOffline.hasObservers()).isTrue();
    }

    @Test
    public void makeOfflineUnavailableUsesOfflineOperationsToMakeOfflineUnavailable() {
        setPlaylistInfo();
        final PublishSubject<Void> makePlaylistUnavailableOffline = PublishSubject.create();
        when(offlineContentOperations.makePlaylistUnavailableOffline(headerItem.getUrn())).thenReturn(
                makePlaylistUnavailableOffline);

        presenter.onMakeOfflineAvailable(false);

        assertThat(makePlaylistUnavailableOffline.hasObservers()).isTrue();
    }

    @Test
    public void showsOfflineAvailableWhenOfflineContentIsEnabledAndPlaylistCurrentlyMarkedAvailable() {
        Playlist playlist = createPlaylistBuilder(Sharing.PUBLIC)
                .isMarkedForOffline(true)
                .build();

        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(accountOperations.isLoggedInUser(playlist.creatorUrn())).thenReturn(true);

        setPlaylistInfo(playlist);

        verify(engagementsView).showOfflineOptions(true);
    }

    @Test
    public void showsNotOfflineAvailableWhenOfflineContentIsEnabledAndPlaylistCurrentlyMarkedUnavailable() {
        Playlist playlistItem = createPlaylist(Sharing.PUBLIC);

        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(accountOperations.isLoggedInUser(playlistItem.creatorUrn())).thenReturn(true);

        setPlaylistInfo(playlistItem);

        verify(engagementsView).showOfflineOptions(false);
    }

    @Test
    public void showsUpsellWhenOfflineContentIsNotEnabledAndAllowedToShowUpsell() {
        when(accountOperations.isLoggedInUser(headerItem.creatorUrn())).thenReturn(true);
        when(featureOperations.upsellOfflineContent()).thenReturn(true);

        Playlist playlistItem = createPlaylistBuilder(Sharing.PUBLIC).isMarkedForOffline(true).build();
        setPlaylistInfo(playlistItem);

        verify(engagementsView).showUpsell();
    }

    @Test
    public void hidesOfflineOptionsWhenOfflineContentIsNotEnabledAndNotAllowedToShowUpsell() {
        when(accountOperations.isLoggedInUser(headerItem.creatorUrn())).thenReturn(true);

        Playlist playlistItem = createPlaylistBuilder(Sharing.PUBLIC).isMarkedForOffline(true).build();
        setPlaylistInfo(playlistItem);

        verify(engagementsView).hideOfflineOptions();
    }

    @Test
    public void hidesOfflineOptionsWhenPlaylistIsNotPostedByTheCurrentUser() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        setPlaylistInfo(createPlaylist(Sharing.PUBLIC));

        verify(engagementsView).hideOfflineOptions();
    }

    @Test
    public void showsOfflineOptionsWhenPlaylistIsLikedByTheCurrentUser() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        setPlaylistInfo(createPlaylist(Sharing.PUBLIC), true);

        verify(engagementsView).showOfflineOptions(false);
    }

    @Test
    public void hidesOfflineOptionsWhenPlaylistIsNotLikedByTheCurrentUser() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        setPlaylistInfo(createPlaylist(Sharing.PUBLIC));

        verify(engagementsView).hideOfflineOptions();
    }

    @Test
    public void showDefaultDownloadStateWhenCurrentDownloadEmitsDownloadRemoved() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setPlaylistInfo();
        reset(engagementsView);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, removed(headerItem.getUrn()));

        verify(engagementsView).showOfflineState(OfflineState.NOT_OFFLINE);
    }

    @Test
    public void showDefaultDownloadStateWhenCurrentDownloadEmitsRequested() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setPlaylistInfo();

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         requested(singletonList(headerItem.getUrn()), false));

        verify(engagementsView).showOfflineState(OfflineState.REQUESTED);
    }

    @Test
    public void setDownloadingStateWhenCurrentDownloadEmitsDownloading() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setPlaylistInfo();

        final ApiTrack track = ModelFixtures.create(ApiTrack.class);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         downloading(Arrays.asList(track.getUrn(), headerItem.getUrn()), true));

        verify(engagementsView).showOfflineState(OfflineState.DOWNLOADING);
    }

    @Test
    public void setDownloadedStateWhenCurrentDownloadEmitsDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        setPlaylistInfo();
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         downloaded(singletonList(headerItem.getUrn()), false));

        final InOrder inOrder = inOrder(engagementsView);
        inOrder.verify(engagementsView).showOfflineState(OfflineState.NOT_OFFLINE);
        inOrder.verify(engagementsView).showOfflineState(OfflineState.DOWNLOADED);
    }

    @Test
    public void ignoreDownloadStateWhenCurrentDownloadEmitsAnUnrelatedEvent() {
        setPlaylistInfo();
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         downloaded(singletonList(Urn.forPlaylist(12344443212L)), false));

        verify(engagementsView, never()).showOfflineState(OfflineState.DOWNLOADED);
    }

    @Test
    public void showDefaultDownloadStateWhenPlaylistDownloadStateIsDownloadNoOffline() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setUpHeaderItem(OfflineState.NOT_OFFLINE);
        setPlaylistInfo();

        verify(engagementsView).showOfflineState(OfflineState.NOT_OFFLINE);
    }

    @Test
    public void showDefaultDownloadStateWhenPlaylistDownloadStateRequested() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setUpHeaderItem(OfflineState.REQUESTED);
        setPlaylistInfo();

        verify(engagementsView).showOfflineState(OfflineState.REQUESTED);
    }

    @Test
    public void setDownloadingStateWhenPlaylistDownloadStateDownloading() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setUpHeaderItem(OfflineState.DOWNLOADING);
        setPlaylistInfo();

        verify(engagementsView).showOfflineState(OfflineState.DOWNLOADING);
    }

    @Test
    public void setDownloadedStateWhenPlaylistDownloadStateDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setUpHeaderItem(OfflineState.DOWNLOADED);
        setPlaylistInfo();

        verify(engagementsView).showOfflineState(OfflineState.DOWNLOADED);
    }

    @Test
    public void showWarningTextWhenPendingDownloadAndOffline() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setPlaylistInfo();
        when(connectionHelper.isNetworkConnected()).thenReturn(false);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         requested(singletonList(headerItem.getUrn()), false));

        verify(engagementsView).showNoConnection();
    }

    @Test
    public void showWarningTextWhenPendingDownloadAndWifiOnly() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setPlaylistInfo();
        when(offlineSettings.isWifiOnlyEnabled()).thenReturn(true);
        when(connectionHelper.isWifiConnected()).thenReturn(false);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         requested(singletonList(headerItem.getUrn()), false));

        verify(engagementsView).showNoWifi();
    }

    @Test
    public void doNotShowWarningTextForNonPendingStates() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setPlaylistInfo();
        when(connectionHelper.isNetworkConnected()).thenReturn(false);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         downloaded(singletonList(headerItem.getUrn()), false));

        verify(engagementsView, times(0)).showNoConnection();
    }

    @Test
    public void disablesShuffleWithOneTrack() throws Exception {
        when(accountOperations.isLoggedInUser(headerItem.creatorUrn())).thenReturn(true);
        final Playlist playlistItem = createPlaylistBuilder(Sharing.PUBLIC).trackCount(1).build();

        setPlaylistInfo(createPlaylistWithSingleTrack(playlistItem), getPlaySessionSource());

        verify(engagementsView).disableShuffle();
    }

    @Test
    public void enablesShuffleWithMoreThanOneTrack() throws Exception {
        when(accountOperations.isLoggedInUser(headerItem.creatorUrn())).thenReturn(true);
        final Playlist playlistItem = createPlaylist(Sharing.PUBLIC);

        setPlaylistInfo(playlistItem);

        verify(engagementsView).enableShuffle();
    }

    @Test
    public void shouldTrackUpsellImpressionInOnCreateWhenFeatureAvailable() {
        when(featureOperations.upsellOfflineContent()).thenReturn(true);

        presenter.onCreate(fragmentRule.getFragment(), null);

        UpgradeFunnelEvent event = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);
        assertThat(event.pageUrn().get()).isEqualTo(PLAYLIST_URN.toString());
    }

    @Test
    public void shouldNotTrackUpsellImpressionInOnCreateWhenFeatureNotAvailable() {
        when(featureOperations.upsellOfflineContent()).thenReturn(false);

        presenter.onCreate(fragmentRule.getFragment(), null);

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void shouldPlayNext() {
        presenter.onPlayNext(Urn.forPlaylist(1));

        verify(playQueueHelper, times(1)).playNext(Urn.forPlaylist(1));
    }

    private void setPlaylistInfo() {
        setPlaylistInfo(headerItem, getPlaySessionSource());
    }

    private void setPlaylistInfo(Playlist playlistItem) {
        setPlaylistInfo(createPlaylistHeaderItem(playlistItem, playlistItem.isLikedByCurrentUser().or(false)), getPlaySessionSource());
    }

    private void setPlaylistInfo(Playlist playlistItem, boolean isLiked) {
        setPlaylistInfo(createPlaylistHeaderItem(playlistItem, isLiked), getPlaySessionSource());
    }

    private void setPlaylistInfo(PlaylistDetailHeaderItem playlistDetailHeaderItem, PlaySessionSource playSessionSource) {
        presenter.onAttach(fragmentRule.getFragment(), fragmentRule.getActivity());
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onResume(fragmentRule.getFragment());
        presenter.setScreen(SCREEN.get());
        presenter.setPlaylist(playlistDetailHeaderItem, playSessionSource);
    }


    private void setUpHeaderItem(OfflineState offlineState) {
        headerItem = headerItem.toBuilder().offlineState(offlineState).build();
    }

    private PlaylistDetailHeaderItem createPlaylistInfoWithSharing(Sharing sharing) {
        final Playlist.Builder playlistBuilder = createPlaylistBuilder(sharing);
        return createPlaylistHeaderItem(playlistBuilder.build());
    }

    private PlaylistDetailHeaderItem createPlaylistHeaderItem(Playlist playlistItem) {
        return createPlaylistHeaderItem(playlistItem, playlistItem.isLikedByCurrentUser().or(false));
    }

    private PlaylistDetailHeaderItem createPlaylistHeaderItem(Playlist playlistItem, boolean isLiked) {
        return PlaylistDetailHeaderItem.from(playlistItem, emptyList(),isLiked, resources());
    }

    private PlaylistDetailHeaderItem createPlaylistWithSingleTrack(Playlist playlistItem) {
        return PlaylistDetailHeaderItem.from(playlistItem, ModelFixtures.trackItems(1), false, resources());
    }

    private Playlist createPlaylist(Sharing sharing) {
        return createPlaylistBuilder(sharing).build();
    }

    private Playlist.Builder createPlaylistBuilder(Sharing sharing) {
        final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);
        apiPlaylist.setSharing(sharing);

        return playlistBuilder(apiPlaylist);
    }

    private Playlist.Builder playlistBuilder(ApiPlaylist apiPlaylist) {
        return ModelFixtures.playlistBuilder(apiPlaylist)
                            .isMarkedForOffline(false)
                            .isLikedByCurrentUser(false)
                            .isRepostedByCurrentUser(false);
    }


    private PlaySessionSource getPlaySessionSource() {
        return new PlaySessionSource(Screen.PLAYLIST_DETAILS);
    }

    protected Context getContext() {
        return fragmentRule.getFragment().getContext();
    }
}
