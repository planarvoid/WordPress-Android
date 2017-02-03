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
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.associations.RepostStatuses;
import com.soundcloud.android.associations.RepostsStateProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.events.PlaylistMarkedForOfflineStateChangedEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.likes.LikesStateProvider;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.observers.AssertableSubscriber;
import rx.subjects.BehaviorSubject;
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
    @Mock private RepostOperations repostOperations;
    @Mock private RepostsStateProvider repostsStateProvider;

    @Captor private ArgumentCaptor<UIEvent> uiEventArgumentCaptor;

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
    private final PublishSubject<RepostStatuses> repostStatuses = PublishSubject.create();
    private final PublishSubject<SyncJobResult> syncPlaylist = PublishSubject.create();

    private final NewPlaylistDetailsPresenter.PlaylistWithTracks initialPlaylistWithTracks = NewPlaylistDetailsPresenter.PlaylistWithTracks.create(initialPlaylist, asList(track1, track2));
    private final NewPlaylistDetailsPresenter.PlaylistWithTracks updatedPlaylistWithTracks = NewPlaylistDetailsPresenter.PlaylistWithTracks.create(updatedPlaylist, asList(track1, track2, track3));
    private final BehaviorSubject<NewPlaylistDetailsPresenter.PlaylistWithTracks> dataSource = BehaviorSubject.create();

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
        when(repostsStateProvider.repostedStatuses()).thenReturn(repostStatuses);
        when(syncInitiator.syncPlaylist(playlistUrn)).thenReturn(syncPlaylist);
        when(offlinePropertiesProvider.states()).thenReturn(just(OfflineProperties.empty()));

        newPlaylistPresenter = new NewPlaylistDetailsPresenter(playlistUrn,
                                                               screen,
                                                               searchQuerySourceInfo,
                                                               promotedSourceInfo,
                                                               playbackInitiator,
                                                               playlistOperations,
                                                               likesStateProvider,
                                                               repostsStateProvider,
                                                               playQueueHelper,
                                                               offlinePropertiesProvider,
                                                               syncInitiator,
                                                               eventBus,
                                                               offlineContentOperations,
                                                               eventTracker,
                                                               likeOperations,
                                                               viewModelCreator,
                                                               dataSourceProviderFactory, repostOperations);

    }

    private void connect() {
        newPlaylistPresenter.connect();
        emitLikedEntities();
        emitRepostedEntities();
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

        modelUpdates.assertValues(fromIdle(initialModel), fromIdle(toEditMode(initialModel)));
    }

    @Test
    public void onExitEditModeGoesBackToEditMode() throws Exception {
        connect();

        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();

        newPlaylistPresenter.onEnterEditMode();
        newPlaylistPresenter.onExitEditMode();

        modelUpdates.assertValues(fromIdle(initialModel), fromIdle(toEditMode(initialModel)), fromIdle(initialModel));
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
    public void likeShowsResult() throws Exception {
        connect();

        LikeOperations.LikeResult likeResult = LikeOperations.LikeResult.LIKE_SUCCEEDED;
        when(likeOperations.toggleLike(playlistUrn, true)).thenReturn(just(likeResult));

        AssertableSubscriber<LikeOperations.LikeResult> subscriber = newPlaylistPresenter.onLikeResult().test();

        newPlaylistPresenter.onToggleLike(true);

        subscriber.assertValue(likeResult);
    }

    @Test
    public void likeSendsTracking() throws Exception {
        connect();

        when(likeOperations.toggleLike(playlistUrn, true)).thenReturn(PublishSubject.create());

        newPlaylistPresenter.onToggleLike(true);

        assertEngagementEvent(UIEvent.Kind.LIKE, UIEvent.ClickName.LIKE);
    }

    @Test
    public void unlikeSendsResult() throws Exception {
        connect();

        LikeOperations.LikeResult likeResult = LikeOperations.LikeResult.UNLIKE_SUCCEEDED;
        when(likeOperations.toggleLike(playlistUrn, false)).thenReturn(just(likeResult));

        AssertableSubscriber<LikeOperations.LikeResult> subscriber = newPlaylistPresenter.onLikeResult().test();

        newPlaylistPresenter.onToggleLike(false);

        subscriber.assertValue(likeResult);
    }

    @Test
    public void unlikeSendsTracking() throws Exception {
        connect();

        when(likeOperations.toggleLike(playlistUrn, false)).thenReturn(PublishSubject.create());

        newPlaylistPresenter.onToggleLike(false);

        assertEngagementEvent(UIEvent.Kind.UNLIKE, UIEvent.ClickName.UNLIKE);
    }

    @Test
    public void repostShowsResult(){
        connect();

        when(repostOperations.toggleRepost(playlistUrn, true)).thenReturn(just(RepostOperations.RepostResult.REPOST_SUCCEEDED));

        AssertableSubscriber<RepostOperations.RepostResult> subscriber = newPlaylistPresenter.onRepostResult().test();

        newPlaylistPresenter.onToggleRepost(true);

        subscriber.assertValue(RepostOperations.RepostResult.REPOST_SUCCEEDED);
    }

    @Test
    public void unpostShowsResult(){
        connect();

        RepostsStatusEvent.RepostStatus repostStatus = RepostsStatusEvent.RepostStatus.createUnposted(playlistUrn);
        when(repostOperations.toggleRepost(playlistUrn, false)).thenReturn(just(RepostOperations.RepostResult.UNREPOST_SUCCEEDED));

        AssertableSubscriber<RepostOperations.RepostResult> subscriber = newPlaylistPresenter.onRepostResult().test();

        newPlaylistPresenter.onToggleRepost(false);

        subscriber.assertValue(RepostOperations.RepostResult.UNREPOST_SUCCEEDED);
    }

    @Test
    public void repostSendsTracking(){
        connect();

        when(repostOperations.toggleRepost(playlistUrn, true)).thenReturn(just(RepostOperations.RepostResult.REPOST_SUCCEEDED));

        newPlaylistPresenter.onToggleRepost(true);

        assertEngagementEvent(UIEvent.Kind.REPOST, UIEvent.ClickName.REPOST);
    }

    @Test
    public void unpostSendsTracking(){
        connect();

        RepostsStatusEvent.RepostStatus repostStatus = RepostsStatusEvent.RepostStatus.createUnposted(playlistUrn);
        when(repostOperations.toggleRepost(playlistUrn, false)).thenReturn(just(RepostOperations.RepostResult.UNREPOST_SUCCEEDED));

        newPlaylistPresenter.onToggleRepost(false);

        assertEngagementEvent(UIEvent.Kind.UNREPOST, UIEvent.ClickName.UNREPOST);
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

    @Test
    public void goesBackWhenDeleted() throws Exception {
        connect();

        AssertableSubscriber<Object> subscriber = newPlaylistPresenter.onGoBack().test();

        subscriber.assertNoValues();

        eventBus.publish(EventQueue.URN_STATE_CHANGED, UrnStateChangedEvent.fromEntitiesDeleted(singleton(playlistUrn)));

        subscriber.assertValueCount(1);
    }

    @Test
    public void ignoresDeletionOfOtherPlaylist() throws Exception {
        connect();

        AssertableSubscriber<Object> subscriber = newPlaylistPresenter.onGoBack().test();

        subscriber.assertNoValues();

        eventBus.publish(EventQueue.URN_STATE_CHANGED, UrnStateChangedEvent.fromEntitiesDeleted(singleton(Urn.forPlaylist(8765))));

        subscriber.assertNoValues();
    }

    private void emitLikedEntities(Urn... urns) {
        final HashSet<Urn> likedEntities = new HashSet<>(asList(urns));
        likeStatuses.onNext(LikedStatuses.create(likedEntities));
    }

    private void emitRepostedEntities(Urn... urns) {
        final HashSet<Urn> repostedEntities = new HashSet<>(asList(urns));
        repostStatuses.onNext(RepostStatuses.create(repostedEntities));
    }

    private PlaySessionSource createPlaySessionSource() {
        PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(screen, initialPlaylist.urn(), initialPlaylist.creatorUrn(), initialPlaylist.trackCount());
        playSessionSource.setPromotedSourceInfo(promotedSourceInfo);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        return playSessionSource;
    }

    private PlaylistDetailsViewModel toEditMode(PlaylistDetailsViewModel source) {
        final List<PlaylistDetailTrackItem> expectedTracks = transform(source.tracks(), track -> track.toBuilder().inEditMode(true).build());
        final PlaylistDetailsMetadata expectedMetaData = source.metadata()
                                                               .toBuilder()
                                                               .isInEditMode(true)
                                                               .build();
        return source.toBuilder()
                     .metadata(expectedMetaData)
                     .tracks(expectedTracks)
                     .build();
    }

    private void assertEngagementEvent(UIEvent.Kind kind, UIEvent.ClickName clickName) {
        verify(eventTracker).trackEngagement(uiEventArgumentCaptor.capture());
        UIEvent uiEvent = uiEventArgumentCaptor.getValue();
        assertThat(uiEvent.kind()).isEqualTo(kind);
        assertThat(uiEvent.clickName()).isEqualTo(Optional.of(clickName));
        assertThat(uiEvent.clickCategory()).isEqualTo(Optional.of(UIEvent.ClickCategory.ENGAGEMENT));
        assertThat(uiEvent.clickObjectUrn()).isEqualTo(Optional.of(playlistUrn));
        assertThat(uiEvent.adUrn()).isEqualTo(Optional.of("ad-urn"));
    }



}
