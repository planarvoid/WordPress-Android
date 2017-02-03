package com.soundcloud.android.playlists;

import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloaded;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloading;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.removed;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.requested;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
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
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.playqueue.PlayQueueHelper;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.share.ShareOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import java.util.Arrays;
import java.util.List;

public class PlaylistHeaderPresenterTest extends AndroidUnitTest {

    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123);
    private static final Screen SCREEN = Screen.SEARCH_MAIN;

    private TestEventBus eventBus;
    @Mock private Navigator navigator;
    @Mock private PlaylistHeaderScrollHelper profileHeaderScrollHelper;
    @Mock private RepostOperations repostOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private LikeOperations likeOperations;
    @Mock private PlaylistEngagementsRenderer engagementsView;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private PlaylistOperations playlistOperations;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private PlaybackToastHelper playbackToastHelper;
    @Mock private NetworkConnectionHelper connectionHelper;
    @Mock private OfflineSettingsOperations offlineSettings;
    @Mock private ShareOperations shareOperations;
    @Mock private PlaylistCoverRenderer playlistCoverRenderer;
    @Mock private PlaylistHeaderPresenterListener headerListener;
    @Mock private PlaylistCoverRenderer coverRenderer;
    @Mock private PlayQueueHelper playQueueHelper;
    @Mock private ScreenProvider screenProvider;
    @Mock private EventTracker eventTracker;
    @Mock private OfflinePropertiesProvider offlinePropertiesProvider;
    @Mock private FeatureFlags featureFlags;

    @Captor private ArgumentCaptor<UIEvent> uiEventCaptor;

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.playlist_fragment, fragmentArgs());

    private Observable<List<Urn>> playlistTrackurns;
    private PlaylistDetailsMetadata headerItem;
    private PlaylistHeaderPresenter presenter;
    private View view;

    private static Bundle fragmentArgs() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(PlaylistDetailFragment.EXTRA_URN, PLAYLIST_URN);
        return bundle;
    }

    @Before
    public void setup() {
        eventBus = new TestEventBus();
        playlistTrackurns = Observable.just(singletonList(Urn.forTrack(1)));
        headerItem = createPlaylistInfoWithSharing(Sharing.PUBLIC);
        when(playlistOperations.trackUrnsForPlayback(headerItem.getUrn())).thenReturn(playlistTrackurns);

        presenter = new PlaylistHeaderPresenter(
                eventBus,
                eventTracker,
                navigator,
                profileHeaderScrollHelper,
                featureOperations,
                offlineContentOperations,
                playbackInitiator,
                playlistOperations,
                playbackToastHelper,
                likeOperations,
                repostOperations,
                shareOperations,
                offlinePropertiesProvider,
                playQueueHelper,
                coverRenderer,
                engagementsView,
                featureFlags);

        view = View.inflate(activity(), R.layout.playlist_details_view, null);
        presenter.bindItemView(0, view, null);
    }

    @Test
    public void updatesEngagementsForNewLike() {
        setPlaylistInfo();

        LikesStatusEvent likesStatusEvent = LikesStatusEvent.create(headerItem.getUrn(),
                                                         true,
                                                         headerItem.likesCount());
        eventBus.publish(EventQueue.LIKE_CHANGED,
                         likesStatusEvent);

        verify(engagementsView).bind(
                same(view),
                eq(headerItem.updatedWithLikeStatus(likesStatusEvent.likeStatusForUrn(headerItem.getUrn()).get())),
                any());
    }

    @Test
    public void updatesEngagementsForNewRepost() {
        setPlaylistInfo();

        RepostsStatusEvent repostStatusEvent = RepostsStatusEvent.create(RepostStatus.createReposted(headerItem.getUrn()));
        eventBus.publish(EventQueue.REPOST_CHANGED,
                         repostStatusEvent);

        verify(engagementsView).bind(
                same(view),
                eq(headerItem.updatedWithRepostStatus(repostStatusEvent.repostStatusForUrn(headerItem.getUrn()).get())),
                any());
    }

    @Test
    public void shouldNotUpdateLikeOrRepostStatusStateForOtherPlayables() {
        setPlaylistInfo();

        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(Urn.forTrack(2L), true, 1));
        eventBus.publish(EventQueue.REPOST_CHANGED, RepostsStatusEvent.createReposted(Urn.forTrack(2L)));

        verify(engagementsView).bind(any(), any(), any());
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
                                       false)).thenReturn(Observable.just(LikeOperations.LikeResult.UNLIKE_SUCCEEDED));
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
                                           anyBoolean())).thenReturn(Observable.just(RepostOperations.RepostResult.REPOST_SUCCEEDED));
        doNothing().when(eventTracker).trackEngagement(uiEventCaptor.capture());

        presenter.toggleRepost(true, false);

        UIEvent uiEvent = uiEventCaptor.getValue();
        assertThat(uiEvent.kind()).isSameAs(UIEvent.Kind.REPOST);
        assertThat(uiEvent.contextScreen().get()).isEqualTo(SCREEN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnrepostingPlayable() {
        setPlaylistInfo();
        when(repostOperations.toggleRepost(any(Urn.class),
                                           anyBoolean())).thenReturn(Observable.just(RepostOperations.RepostResult.UNREPOST_SUCCEEDED));
        doNothing().when(eventTracker).trackEngagement(uiEventCaptor.capture());

        presenter.toggleRepost(false, false);

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
                                       true)).thenReturn(Observable.just(LikeOperations.LikeResult.LIKE_SUCCEEDED));

        presenter.onToggleLike(true);

        verify(likeOperations).toggleLike(headerItem.getUrn(), true);
    }

    @Test
    public void shouldRepostTrackWhenCheckingRepostButton() {
        setPlaylistInfo();
        when(repostOperations.toggleRepost(any(Urn.class),
                                           anyBoolean())).thenReturn(Observable.just(RepostOperations.RepostResult.REPOST_SUCCEEDED));

        presenter.toggleRepost(true, false);

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

        presenter.onUpsell();

        verify(navigator).openUpgrade(getContext());
    }

    @Test
    public void shouldOpenUpgradeScreenWhenClickingOnOverflowUpsell() {
        setPlaylistInfo();

        presenter.onOverflowUpsell();

        verify(navigator).openUpgrade(getContext());
    }

    @Test
    public void shouldUpdateOfflineAvailabilityOnMarkedForOfflineChange() {
        setPlaylistInfo();
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(accountOperations.isLoggedInUser(headerItem.creatorUrn())).thenReturn(true);

        OfflineContentChangedEvent requested = requested(singletonList(headerItem.getUrn()), false);
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, requested);

        PlaylistDetailsMetadata updated = headerItem.toBuilder()
                                                    .offlineState(requested.state)
                                                    .isMarkedForOffline(true)
                                                    .build();

        verify(engagementsView).bind(same(view),
                                     eq(updated),
                                     any());
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

        presenter.onMakeOfflineAvailable();

        assertThat(makePlaylistAvailableOffline.hasObservers()).isTrue();
    }

    @Test
    public void makeOfflineUnavailableUsesOfflineOperationsToMakeOfflineUnavailable() {
        setPlaylistInfo();
        final PublishSubject<Void> makePlaylistUnavailableOffline = PublishSubject.create();
        when(offlineContentOperations.makePlaylistUnavailableOffline(headerItem.getUrn())).thenReturn(
                makePlaylistUnavailableOffline);

        presenter.onMakeOfflineUnavailable();

        assertThat(makePlaylistUnavailableOffline.hasObservers()).isTrue();
    }

    @Test
    public void showDefaultDownloadStateWhenCurrentDownloadEmitsDownloadRemoved() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setPlaylistInfo();
        reset(engagementsView);

        OfflineContentChangedEvent removed = removed(headerItem.getUrn());
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, removed);

        verify(engagementsView).bind(same(view), eq(headerItem.toBuilder().offlineState(removed.state).build()), any());
    }

    @Test
    public void showDefaultDownloadStateWhenCurrentDownloadEmitsRequested() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setPlaylistInfo();
        OfflineContentChangedEvent requested = requested(singletonList(headerItem.getUrn()), false);
        reset(engagementsView);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         requested);

        PlaylistDetailsMetadata updated = headerItem.toBuilder()
                                                    .offlineState(requested.state)
                                                    .isMarkedForOffline(true).build();

        verify(engagementsView).bind(same(view), eq(updated), any());
    }

    @Test
    public void setDownloadingStateWhenCurrentDownloadEmitsDownloading() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setPlaylistInfo();

        final ApiTrack track = ModelFixtures.create(ApiTrack.class);
        OfflineContentChangedEvent downloading = downloading(Arrays.asList(track.getUrn(), headerItem.getUrn()), true);
        reset(engagementsView);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         downloading);

        PlaylistDetailsMetadata updated = headerItem.toBuilder()
                                                    .offlineState(downloading.state)
                                                    .isMarkedForOffline(true).build();

        verify(engagementsView).bind(same(view), eq(updated), any());
    }

    @Test
    public void setDownloadedStateWhenCurrentDownloadEmitsDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        setPlaylistInfo();
        OfflineContentChangedEvent downloaded = downloaded(singletonList(headerItem.getUrn()), false);
        reset(engagementsView);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         downloaded);

        PlaylistDetailsMetadata updated = headerItem.toBuilder()
                                                    .offlineState(downloaded.state)
                                                    .isMarkedForOffline(true).build();
        verify(engagementsView).bind(same(view), eq(updated), any());
    }

    @Test
    public void ignoreDownloadStateWhenCurrentDownloadEmitsAnUnrelatedEvent() {
        setPlaylistInfo();
        reset(engagementsView);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         downloaded(singletonList(Urn.forPlaylist(12344443212L)), false));

        verify(engagementsView, never()).bind(any(), any(), any());
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
        setPlaylistInfo();

        presenter.onPlayNext();

        verify(playQueueHelper, times(1)).playNext(headerItem.urn());
    }

    private void setPlaylistInfo() {
        setPlaylistInfo(headerItem, getPlaySessionSource());
    }

    private void setPlaylistInfo(PlaylistDetailsMetadata playlistDetailsMetadata, PlaySessionSource playSessionSource) {
        presenter.onAttach(fragmentRule.getFragment(), fragmentRule.getActivity());
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onResume(fragmentRule.getFragment());
        presenter.setScreen(SCREEN.get());
        presenter.setPlaylist(playlistDetailsMetadata, playSessionSource);
    }

    private PlaylistDetailsMetadata createPlaylistInfoWithSharing(Sharing sharing) {
        final Playlist.Builder playlistBuilder = createPlaylistBuilder(sharing);
        return createPlaylistHeaderItem(playlistBuilder.build());
    }

    private PlaylistDetailsMetadata createPlaylistHeaderItem(Playlist playlistItem) {
        return createPlaylistHeaderItem(playlistItem, playlistItem.isLikedByCurrentUser().or(false));
    }

    private PlaylistDetailsMetadata createPlaylistHeaderItem(Playlist playlistItem, boolean isLiked) {
        return PlaylistDetailsMetadata.from(playlistItem, emptyList(), isLiked,
                                            playlistItem.isRepostedByCurrentUser().or(false),
                                            false, OfflineState.NOT_OFFLINE, 0, PlaylistDetailsMetadata.OfflineOptions.AVAILABLE, resources(), false);
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
