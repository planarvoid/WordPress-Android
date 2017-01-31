package com.soundcloud.android.playlists;

import static com.soundcloud.android.view.AsyncViewModel.fromIdle;
import static com.soundcloud.android.view.AsyncViewModel.fromRefreshing;
import static com.soundcloud.java.collections.Lists.transform;
import static com.soundcloud.java.optional.Optional.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.events.PlaylistMarkedForOfflineStateChangedEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineProperties;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.PlaybackResult.ErrorReason;
import com.soundcloud.android.playback.playqueue.PlayQueueHelper;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.AssertableSubscriber;
import rx.subjects.PublishSubject;

import java.util.HashSet;
import java.util.List;

public class NewPlaylistDetailsPresenterTest extends AndroidUnitTest {

    @Mock private PlaylistOperations playlistOperations;
    @Mock private LikesStateProvider likesStateProvider;
    @Mock private SyncInitiator syncInitiator;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private FeatureOperations featureOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private PlaylistUpsellOperations upsellOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private OfflinePropertiesProvider offlinePropertiesProvider;
    @Mock private PlayQueueHelper playQueueHelper;
    @Mock private EventTracker eventTracker;
    @Mock private LikeOperations likeOperations;
    @Mock private NewPlaylistDetailsPresenter_DataSourceProviderFactory dataSourceProviderFactory;
    @Mock private NewPlaylistDetailsPresenter.DataSourceProvider dataSourceProvider;

    private TestEventBus eventBus = new TestEventBus();

    private final Track track1 = ModelFixtures.track();
    private final TrackItem trackItem1 = TrackItem.from(track1);
    private final Track track2 = ModelFixtures.track();
    private final TrackItem trackItem2 = TrackItem.from(track2);
    private final Track track3 = ModelFixtures.track();
    private final TrackItem trackItem3 = TrackItem.from(track3);

    private final List<TrackItem> trackItems = asList(trackItem1, trackItem2);
    private final List<TrackItem> updatedTrackItems = asList(trackItem1, trackItem2, trackItem3);

    private final Playlist initialPlaylist = ModelFixtures.playlistBuilder().build();
    private final Playlist updatedPlaylist = initialPlaylist.toBuilder().urn(initialPlaylist.urn()).title("new-title").build();

    private final String screen = "screen";
    private final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(Urn.forGenre("asdf"), "querystring");
    private final PromotedSourceInfo promotedSourceInfo = new PromotedSourceInfo("ad-urn", Urn.forTrack(1), Optional.absent(), emptyList());

    private final PlaylistDetailsViewModel initialModel = PlaylistDetailFixtures.create(resources(), initialPlaylist, trackItems);
    private final PlaylistDetailsViewModel updatedModel = PlaylistDetailFixtures.create(resources(), updatedPlaylist, updatedTrackItems);
    private final Urn playlistUrn = initialPlaylist.urn();

    private final PublishSubject<LikedStatuses> likeStatuses = PublishSubject.create();
    private final PublishSubject<SyncJobResult> syncPlaylist = PublishSubject.create();

    private final NewPlaylistDetailsPresenter.PlaylistWithTracks initialPlaylistWithTracks = NewPlaylistDetailsPresenter.PlaylistWithTracks.create(initialPlaylist, asList(track1, track2));
    private final NewPlaylistDetailsPresenter.PlaylistWithTracks updatedPlaylistWithTracks = NewPlaylistDetailsPresenter.PlaylistWithTracks.create(updatedPlaylist, asList(track1, track2, track3));
    private final PublishSubject<NewPlaylistDetailsPresenter.PlaylistWithTracks> dataSource = PublishSubject.create();

    private NewPlaylistDetailsPresenter newPlaylistPresenter;

    @Before
    public void setUp() throws Exception {
        PlaylistDetailsViewModelCreator viewModelCreator = new PlaylistDetailsViewModelCreator(resources(), featureOperations, accountOperations, upsellOperations);

        when(upsellOperations.getUpsell(any(Playlist.class), anyList())).thenReturn(Optional.absent());
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(featureOperations.upsellOfflineContent()).thenReturn(false);
        when(dataSourceProviderFactory.create(playlistUrn)).thenReturn(dataSourceProvider);
        when(dataSourceProvider.data()).thenReturn(dataSource);
        when(likesStateProvider.likedStatuses()).thenReturn(likeStatuses);
        when(syncInitiator.syncPlaylist(playlistUrn)).thenReturn(syncPlaylist);
        when(offlinePropertiesProvider.states()).thenReturn(just(OfflineProperties.empty()));

        newPlaylistPresenter = new NewPlaylistDetailsPresenter(playlistUrn,
                                                               screen,
                                                               searchQuerySourceInfo,
                                                               promotedSourceInfo,
                                                               playbackInitiator,
                                                               playlistOperations,
                                                               likesStateProvider,
                                                               playQueueHelper,
                                                               offlinePropertiesProvider,
                                                               syncInitiator,
                                                               eventBus,
                                                               offlineContentOperations,
                                                               eventTracker,
                                                               likeOperations,
                                                               viewModelCreator,
                                                               dataSourceProviderFactory);

    }

    private void connect() {
        newPlaylistPresenter.connect();
        emitLikedEntities();
        dataSource.onNext(initialPlaylistWithTracks);
    }

    @Test
    public void emitsPlaylistFromStorage() {
        connect();

        newPlaylistPresenter.viewModel()
                            .test()
                            .assertValue(fromIdle(initialModel));
    }

    @Test
    public void goesToCreatorOnCreatorClick() throws Exception {
        connect();

        AssertableSubscriber<Urn> test = newPlaylistPresenter.goToCreator().test();

        newPlaylistPresenter.onCreatorClicked();

        test.assertReceivedOnNext(singletonList(initialPlaylist.creatorUrn()));
    }

    @Test
    public void onEnterEditModeGoesToEditMode() throws Exception {
        connect();

        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();

        newPlaylistPresenter.onEnterEditMode();

        final PlaylistDetailsMetadata inEditMode = initialModel.metadata().toBuilder().isInEditMode(true).build();
        modelUpdates.assertValues(fromIdle(initialModel), fromIdle(initialModel.toBuilder().metadata(inEditMode).build()));
    }

    @Test
    public void onExitEditModeGoesBackToEditMode() throws Exception {
        connect();

        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();

        newPlaylistPresenter.onEnterEditMode();
        newPlaylistPresenter.onExitEditMode();

        final PlaylistDetailsMetadata metaInEditMode = initialModel.metadata().toBuilder().isInEditMode(true).build();
        final PlaylistDetailsViewModel inEditMode = initialModel.toBuilder().metadata(metaInEditMode).build();
        modelUpdates.assertValues(fromIdle(initialModel), fromIdle(inEditMode), fromIdle(initialModel));
    }

    @Test
    public void onMakeAvailableOfflineMakesOfflineAvailable() throws Exception {
        connect();

        PublishSubject<Void> offlineSubject = PublishSubject.create();
        when(offlineContentOperations.makePlaylistAvailableOffline(playlistUrn)).thenReturn(offlineSubject);

        newPlaylistPresenter.onMakeOfflineAvailable();

        assertThat(offlineSubject.hasObservers()).isTrue();
    }

    @Test
    public void onMakeAvailableOfflineEmitsTracking() throws Exception {
        connect();

        when(offlineContentOperations.makePlaylistAvailableOffline(playlistUrn)).thenReturn(PublishSubject.create());

        newPlaylistPresenter.onMakeOfflineAvailable();

        OfflineInteractionEvent event = eventBus.lastEventOn(EventQueue.TRACKING, OfflineInteractionEvent.class);
        assertThat(event.clickObject()).isEqualTo(of(playlistUrn));
        assertThat(event.clickName()).isEqualTo(of(OfflineInteractionEvent.Kind.KIND_OFFLINE_PLAYLIST_ADD));
        assertThat(event.eventName()).isEqualTo(OfflineInteractionEvent.EventName.CLICK);
    }

    @Test
    public void onMakeUnavailableOfflineMakesOfflineUnavailable() throws Exception {
        connect();

        PublishSubject<Void> offlineSubject = PublishSubject.create();
        when(offlineContentOperations.makePlaylistUnavailableOffline(playlistUrn)).thenReturn(offlineSubject);

        newPlaylistPresenter.onMakeOfflineUnavailable();

        assertThat(offlineSubject.hasObservers()).isTrue();
    }

    @Test
    public void onMakeUnavailableOfflineEmitsTracking() throws Exception {
        connect();

        when(offlineContentOperations.makePlaylistUnavailableOffline(playlistUrn)).thenReturn(PublishSubject.create());

        newPlaylistPresenter.onMakeOfflineUnavailable();

        OfflineInteractionEvent event = eventBus.lastEventOn(EventQueue.TRACKING, OfflineInteractionEvent.class);
        assertThat(event.clickObject()).isEqualTo(of(playlistUrn));
        assertThat(event.clickName()).isEqualTo(of(OfflineInteractionEvent.Kind.KIND_OFFLINE_PLAYLIST_REMOVE));
        assertThat(event.eventName()).isEqualTo(OfflineInteractionEvent.EventName.CLICK);
    }

    @Test
    public void headerSendsExpandPlayerCommandAfterPlayback() throws Exception {
        connect();

        List<Urn> trackUrns = transform(trackItems, PlayableItem::getUrn);
        when(playlistOperations.trackUrnsForPlayback(playlistUrn)).thenReturn(just(trackUrns));
        when(playbackInitiator.playTracks(trackUrns, 0, createPlaySessionSource())).thenReturn(just(PlaybackResult.success()));

        newPlaylistPresenter.onHeaderPlayButtonClicked();

        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isExpand()).isTrue();
    }

    @Test
    public void headerPlayEmitsPlaybackError() throws Exception {
        connect();

        List<Urn> trackUrns = transform(trackItems, PlayableItem::getUrn);
        when(playlistOperations.trackUrnsForPlayback(playlistUrn)).thenReturn(just(trackUrns));
        when(playbackInitiator.playTracks(trackUrns, 0, createPlaySessionSource())).thenReturn(just(PlaybackResult.error(ErrorReason.MISSING_PLAYABLE_TRACKS)));

        AssertableSubscriber<ErrorReason> test = newPlaylistPresenter.onPlaybackError().test();

        newPlaylistPresenter.onHeaderPlayButtonClicked();

        test.assertReceivedOnNext(singletonList(ErrorReason.MISSING_PLAYABLE_TRACKS));
    }

    @Test
    public void playNextAddsToUpNextWithHelper() throws Exception {
        connect();

        newPlaylistPresenter.onPlayNext();

        verify(playQueueHelper).playNext(playlistUrn);
    }

    @Test
    public void likeTogglesLikeUsingOperations() throws Exception {
        connect();

        PublishSubject<Integer> likeSubject = PublishSubject.create();

        when(likeOperations.toggleLike(playlistUrn, true)).thenReturn(likeSubject);

        newPlaylistPresenter.onToggleLike(true);

        assertThat(likeSubject.hasObservers()).isTrue();
    }

    @Test
    public void likeSendsTracking() throws Exception {
        connect();

        when(likeOperations.toggleLike(playlistUrn, true)).thenReturn(PublishSubject.create());

        newPlaylistPresenter.onToggleLike(true);

        eventTracker.trackEngagement(UIEvent.fromToggleLike(true, playlistUrn,
                                                            getEventContext(),
                                                            promotedSourceInfo,
                                                            createEntityMetadata(initialPlaylist)));
    }

    @Test
    public void unlikeTogglesLikeUsingOperations() throws Exception {
        connect();

        PublishSubject<Integer> likeSubject = PublishSubject.create();

        when(likeOperations.toggleLike(playlistUrn, false)).thenReturn(likeSubject);

        newPlaylistPresenter.onToggleLike(false);

        assertThat(likeSubject.hasObservers()).isTrue();
    }

    @Test
    public void unlikeSendsTracking() throws Exception {
        connect();

        when(likeOperations.toggleLike(playlistUrn, false)).thenReturn(PublishSubject.create());

        newPlaylistPresenter.onToggleLike(false);

        eventTracker.trackEngagement(UIEvent.fromToggleLike(true, playlistUrn,
                                                            getEventContext(),
                                                            promotedSourceInfo,
                                                            createEntityMetadata(initialPlaylist)));
    }

    @Test
    public void emitsUpdatedPlaylistAfterLike() {
        connect();

        final PlaylistDetailsMetadata likedHeader = initialModel.metadata().toBuilder().isLikedByUser(true).build();
        final PlaylistDetailsViewModel likedPlaylist = initialModel.toBuilder().metadata(likedHeader).build();

        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();
        modelUpdates.assertValues(fromIdle(initialModel));

        emitLikedEntities(Urn.forTrack(123L));
        modelUpdates.assertValues(fromIdle(initialModel));

        emitLikedEntities(Urn.forTrack(123L), playlistUrn);
        modelUpdates.assertValues(fromIdle(initialModel), fromIdle(likedPlaylist));
    }

    @Test
    public void emitsUpdatedPlaylistAfterRefresh() {
        connect();

        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();

        modelUpdates.assertValues(fromIdle(initialModel));

        newPlaylistPresenter.refresh();
        modelUpdates.assertValues(fromIdle(initialModel), fromRefreshing(initialModel));

        dataSource.onNext(updatedPlaylistWithTracks);
        syncPlaylist.onCompleted();
        modelUpdates.assertValues(fromIdle(initialModel),
                                  fromRefreshing(initialModel),
                                  fromRefreshing(updatedModel),
                                  fromIdle(updatedModel));
    }

    @Test
    public void emitsUpdatedTrackstAfterRefresh() {
        connect();

        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();

        modelUpdates.assertValues(fromIdle(initialModel));

        newPlaylistPresenter.refresh();
        modelUpdates.assertValues(fromIdle(initialModel), fromRefreshing(initialModel));

        dataSource.onNext(updatedPlaylistWithTracks);
        syncPlaylist.onCompleted();
        modelUpdates.assertValues(fromIdle(initialModel),
                                  fromRefreshing(initialModel),
                                  fromRefreshing(updatedModel),
                                  fromIdle(updatedModel));
    }

    @Test
    public void emitsUpdatedModelWhenPlaylistUpdated() {
        connect();

        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();

        modelUpdates.assertValues(fromIdle(initialModel));

        dataSource.onNext(updatedPlaylistWithTracks);

        modelUpdates.assertValues(fromIdle(initialModel), fromIdle(updatedModel));
    }

    @Test
    public void doesNotEmitUpdatedPlaylistAfterMarkedForDownloadEvent() {
        connect();

        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();

        modelUpdates.assertValues(fromIdle(initialModel));

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistMarkedForOfflineStateChangedEvent.fromPlaylistsMarkedForDownload(asList(playlistUrn)));

        modelUpdates.assertValues(fromIdle(initialModel));
    }

    @Test
    public void ignoredUnrelatedPlaylistsUpdates() {
        connect();

        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();
        final Playlist unrelatedPlaylist = ModelFixtures.playlist();

        modelUpdates.assertValues(fromIdle(initialModel));

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.forUpdate(singleton(unrelatedPlaylist)));
        modelUpdates.assertValues(fromIdle(initialModel));
    }


    private void emitLikedEntities(Urn... urns) {
        final HashSet<Urn> likedEntities = new HashSet<>(asList(urns));
        likeStatuses.onNext(LikedStatuses.create(likedEntities));
    }

    private PlaySessionSource createPlaySessionSource() {
        PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(screen, initialPlaylist.urn(), initialPlaylist.creatorUrn(), initialPlaylist.trackCount());
        playSessionSource.setPromotedSourceInfo(promotedSourceInfo);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        return playSessionSource;
    }

    private EntityMetadata createEntityMetadata(Playlist playlist) {
        return EntityMetadata.from(playlist.creatorName(), playlist.creatorUrn(),
                                   playlist.title(), playlist.urn());
    }

    private EventContextMetadata getEventContext() {
        return EventContextMetadata.builder()
                                   .contextScreen(screen)
                                   .pageName(Screen.PLAYLIST_DETAILS.get())
                                   .invokerScreen(Screen.PLAYLIST_DETAILS.get())
                                   .pageUrn(playlistUrn)
                                   .build();
    }

}
