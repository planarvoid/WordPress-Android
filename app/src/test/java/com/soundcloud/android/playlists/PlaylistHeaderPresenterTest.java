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
import static org.mockito.Mockito.verifyZeroInteractions;
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
import com.soundcloud.android.share.SharePresenter;
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
    @Mock private SharePresenter sharePresenter;
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
    private PlaylistDetailsViewModel viewModel;
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
        viewModel = createPlaylistInfoWithSharingAndIfLikedByCurrentUser(Sharing.PUBLIC, false);
        when(playlistOperations.trackUrnsForPlayback(viewModel.metadata().getUrn())).thenReturn(playlistTrackurns);

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
                sharePresenter,
                offlinePropertiesProvider,
                playQueueHelper,
                coverRenderer,
                engagementsView,
                featureFlags,
                accountOperations);

        view = View.inflate(activity(), R.layout.playlist_details_view, null);
        presenter.bindItemView(0, view, null);
    }

    @Test
    public void updatesEngagementsForNewLike() {
        setPlaylistInfo();

        LikesStatusEvent likesStatusEvent = LikesStatusEvent.create(viewModel.metadata().getUrn(),
                                                         true,
                                                         viewModel.metadata().likesCount());
        eventBus.publish(EventQueue.LIKE_CHANGED,
                         likesStatusEvent);

        final PlaylistDetailsMetadata metadata = viewModel.metadata().updatedWithLikeStatus(likesStatusEvent.likeStatusForUrn(viewModel.metadata().getUrn()).get());
        verify(engagementsView).bind(
                same(view),
                eq(updateVithMetadata(metadata)),
                any());
    }

    @Test
    public void updatesEngagementsForNewRepost() {
        setPlaylistInfo();

        RepostsStatusEvent repostStatusEvent = RepostsStatusEvent.create(RepostStatus.createReposted(viewModel.metadata().getUrn()));
        eventBus.publish(EventQueue.REPOST_CHANGED,
                         repostStatusEvent);

        final PlaylistDetailsMetadata metadata = viewModel.metadata().updatedWithRepostStatus(repostStatusEvent.repostStatusForUrn(viewModel.metadata().getUrn()).get());
        verify(engagementsView).bind(
                same(view),
                eq(updateVithMetadata(metadata)),
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
        when(likeOperations.toggleLike(viewModel.metadata().getUrn(),
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
                                                                        .pageUrn(viewModel.metadata().getUrn())
                                                                        .invokerScreen(Screen.PLAYLIST_DETAILS.get())
                                                                        .build();
        verify(sharePresenter).share(getContext(), viewModel.metadata().permalinkUrl().get(), eventContextMetadata, null, entityMetadata());
    }

    @Test
    public void shouldNotPublishUIEventWhenTrackIsNull() {
        presenter.onShare();

        verify(sharePresenter, never())
                .share(any(Context.class), any(TrackItem.class),
                       any(EventContextMetadata.class), any(PromotedSourceInfo.class));
    }

    @Test
    public void shouldLikePlaylistWhenCheckingLikeButton() {
        setPlaylistInfo();
        when(likeOperations.toggleLike(viewModel.metadata().getUrn(),
                                       true)).thenReturn(Observable.just(LikeOperations.LikeResult.LIKE_SUCCEEDED));

        presenter.onToggleLike(true);

        verify(likeOperations).toggleLike(viewModel.metadata().getUrn(), true);
    }

    @Test
    public void shouldRepostTrackWhenCheckingRepostButton() {
        setPlaylistInfo();
        when(repostOperations.toggleRepost(any(Urn.class),
                                           anyBoolean())).thenReturn(Observable.just(RepostOperations.RepostResult.REPOST_SUCCEEDED));

        presenter.toggleRepost(true, false);

        verify(repostOperations).toggleRepost(eq(viewModel.metadata().getUrn()), eq(true));
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

        presenter.onItemTriggered(new PlaylistDetailUpsellItem(TrackItem.EMPTY));

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
        when(accountOperations.isLoggedInUser(viewModel.metadata().creatorUrn())).thenReturn(true);

        OfflineContentChangedEvent requested = requested(singletonList(viewModel.metadata().getUrn()), false);
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, requested);

        PlaylistDetailsMetadata updated = viewModel.metadata().toBuilder()
                                                    .offlineState(requested.state)
                                                    .isMarkedForOffline(true)
                                                    .build();

        verify(engagementsView).bind(same(view),
                                     eq(updateVithMetadata(updated)),
                                     any());
    }

    @Test
    public void shouldGetContextFromOriginProvider() {
        setPlaylistInfo();

        presenter.onShare();
        EventContextMetadata eventContextMetadata = EventContextMetadata.builder()
                                                                        .contextScreen(SCREEN.get())
                                                                        .pageName(Screen.PLAYLIST_DETAILS.get())
                                                                        .pageUrn(viewModel.metadata().getUrn())
                                                                        .invokerScreen(Screen.PLAYLIST_DETAILS.get())
                                                                        .build();
        verify(sharePresenter).share(getContext(), viewModel.metadata().permalinkUrl().get(), eventContextMetadata, null, entityMetadata());
    }

    private EntityMetadata entityMetadata() {
        return EntityMetadata.from(viewModel.metadata().creatorName(), viewModel.metadata().creatorUrn(), viewModel.metadata().title(), viewModel.metadata().getUrn());
    }

    @Test
    public void shouldSaveOfflineOnMakeOfflineAvailableIfPlaylistOwnedByCurrentUser() {
        setPlaylistInfo();
        final PublishSubject<Void> makePlaylistAvailableOffline = PublishSubject.create();
        when(offlineContentOperations.makePlaylistAvailableOffline(viewModel.metadata().getUrn())).thenReturn(
                makePlaylistAvailableOffline);
        when(accountOperations.isLoggedInUser(viewModel.metadata().creatorUrn())).thenReturn(true);

        presenter.onMakeOfflineAvailable();

        assertThat(makePlaylistAvailableOffline.hasObservers()).isTrue();
        verifyZeroInteractions(likeOperations);
    }

    @Test
    public void shouldSaveOfflineOnMakeOfflineAvailableIfPlaylistLikedByCurrentUser() {
        viewModel = createPlaylistInfoWithSharingAndIfLikedByCurrentUser(Sharing.PUBLIC, true);
        setPlaylistInfo();
        final PublishSubject<Void> makePlaylistAvailableOffline = PublishSubject.create();
        when(offlineContentOperations.makePlaylistAvailableOffline(viewModel.metadata().getUrn())).thenReturn(
                makePlaylistAvailableOffline);

        presenter.onMakeOfflineAvailable();

        assertThat(makePlaylistAvailableOffline.hasObservers()).isTrue();
        verifyZeroInteractions(likeOperations);
    }

    @Test
    public void shouldLikeAndSaveOfflineOnMakeOfflineAvailable() {
        setPlaylistInfo();
        final PublishSubject<Void> makePlaylistAvailableOffline = PublishSubject.create();
        when(offlineContentOperations.makePlaylistAvailableOffline(viewModel.metadata().getUrn())).thenReturn(
                makePlaylistAvailableOffline);
        when(likeOperations.toggleLike(viewModel.metadata().getUrn(), true)).thenReturn(Observable.just(LikeOperations.LikeResult.LIKE_SUCCEEDED));

        presenter.onMakeOfflineAvailable();

        assertThat(makePlaylistAvailableOffline.hasObservers()).isTrue();
        verify(likeOperations).toggleLike(viewModel.metadata().getUrn(), true);
    }

    @Test
    public void makeOfflineUnavailableUsesOfflineOperationsToMakeOfflineUnavailable() {
        setPlaylistInfo();
        final PublishSubject<Void> makePlaylistUnavailableOffline = PublishSubject.create();
        when(offlineContentOperations.makePlaylistUnavailableOffline(viewModel.metadata().getUrn())).thenReturn(
                makePlaylistUnavailableOffline);

        presenter.onMakeOfflineUnavailable();

        assertThat(makePlaylistUnavailableOffline.hasObservers()).isTrue();
    }

    @Test
    public void showDefaultDownloadStateWhenCurrentDownloadEmitsDownloadRemoved() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setPlaylistInfo();
        reset(engagementsView);

        OfflineContentChangedEvent removed = removed(viewModel.metadata().getUrn());
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, removed);

        final PlaylistDetailsMetadata metadata = viewModel.metadata().toBuilder().offlineState(removed.state).build();
        verify(engagementsView).bind(same(view), eq(updateVithMetadata(metadata)), any());
    }

    @Test
    public void showDefaultDownloadStateWhenCurrentDownloadEmitsRequested() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setPlaylistInfo();
        OfflineContentChangedEvent requested = requested(singletonList(viewModel.metadata().getUrn()), false);
        reset(engagementsView);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         requested);

        PlaylistDetailsMetadata updated = viewModel.metadata().toBuilder()
                                                    .offlineState(requested.state)
                                                    .isMarkedForOffline(true).build();

        verify(engagementsView).bind(same(view), eq(updateVithMetadata(updated)), any());
    }

    @Test
    public void setDownloadingStateWhenCurrentDownloadEmitsDownloading() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        setPlaylistInfo();

        final ApiTrack track = ModelFixtures.create(ApiTrack.class);
        OfflineContentChangedEvent downloading = downloading(Arrays.asList(track.getUrn(), viewModel.metadata().getUrn()), true);
        reset(engagementsView);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         downloading);

        PlaylistDetailsMetadata updated = viewModel.metadata().toBuilder()
                                                    .offlineState(downloading.state)
                                                    .isMarkedForOffline(true).build();

        verify(engagementsView).bind(same(view), eq(updateVithMetadata(updated)), any());
    }

    @Test
    public void setDownloadedStateWhenCurrentDownloadEmitsDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        setPlaylistInfo();
        OfflineContentChangedEvent downloaded = downloaded(singletonList(viewModel.metadata().getUrn()), false);
        reset(engagementsView);

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         downloaded);

        PlaylistDetailsViewModel updated = updateVithMetadata(viewModel.metadata().toBuilder()
                                                                        .offlineState(downloaded.state).isMarkedForOffline(true).build());
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

        verify(playQueueHelper, times(1)).playNext(viewModel.metadata().urn());
    }

    private void setPlaylistInfo() {
        setPlaylistInfo(viewModel, getPlaySessionSource());
    }

    private void setPlaylistInfo(PlaylistDetailsViewModel viewModel, PlaySessionSource playSessionSource) {
        presenter.onAttach(fragmentRule.getFragment(), fragmentRule.getActivity());
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onResume(fragmentRule.getFragment());
        presenter.setScreen(SCREEN.get());
        presenter.setPlaylist(viewModel, playSessionSource);
    }

    private PlaylistDetailsViewModel createPlaylistInfoWithSharingAndIfLikedByCurrentUser(Sharing sharing, boolean isLikedByCurrentUser) {
        final Playlist.Builder playlistBuilder = createPlaylistBuilder(sharing, isLikedByCurrentUser);
        final PlaylistDetailsMetadata metaData = createPlaylistHeaderItem(playlistBuilder.build());

        return PlaylistDetailsViewModel
                .builder()
                .metadata(metaData)
                .tracks(Optional.absent())
                .upsell(Optional.absent())
                .otherPlaylists(Optional.absent())
                .build();
    }

    private PlaylistDetailsMetadata createPlaylistHeaderItem(Playlist playlistItem) {
        return createPlaylistHeaderItem(playlistItem, playlistItem.isLikedByCurrentUser().or(false));
    }

    private PlaylistDetailsMetadata createPlaylistHeaderItem(Playlist playlistItem, boolean isLiked) {
        return PlaylistDetailsMetadata.from(playlistItem, emptyList(), isLiked,
                                            playlistItem.isRepostedByCurrentUser().or(false),
                                            false, OfflineState.NOT_OFFLINE, 0, PlaylistDetailsMetadata.OfflineOptions.AVAILABLE, resources(), false);
    }

    private Playlist.Builder createPlaylistBuilder(Sharing sharing, boolean isLikedByCurrentUser) {
        final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);
        apiPlaylist.setSharing(sharing);

        return playlistBuilder(apiPlaylist, isLikedByCurrentUser);
    }

    private Playlist.Builder playlistBuilder(ApiPlaylist apiPlaylist, boolean isLikedByCurrentUser) {
        return ModelFixtures.playlistBuilder(apiPlaylist)
                            .isMarkedForOffline(false)
                            .isLikedByCurrentUser(isLikedByCurrentUser)
                            .isRepostedByCurrentUser(false);
    }


    private PlaySessionSource getPlaySessionSource() {
        return new PlaySessionSource(Screen.PLAYLIST_DETAILS);
    }

    protected Context getContext() {
        return fragmentRule.getFragment().getContext();
    }

    private PlaylistDetailsViewModel updateVithMetadata(PlaylistDetailsMetadata metadata) {
        return viewModel.toBuilder().metadata(metadata).build();
    }
}
