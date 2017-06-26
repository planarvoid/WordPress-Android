package com.soundcloud.android.playlists;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.playlistItem;
import static com.soundcloud.java.collections.Lists.transform;
import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.TrackingStateProvider;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.associations.RepostStatuses;
import com.soundcloud.android.associations.RepostsStateProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineProperties;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.PlaybackResult.ErrorReason;
import com.soundcloud.android.playback.playqueue.PlayQueueHelper;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.share.SharePresenter;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.testsupport.fixtures.TestSyncJobResults;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.rx.eventbus.TestEventBus;
import edu.emory.mathcs.backport.java.util.Collections;
import io.reactivex.Single;
import io.reactivex.subjects.SingleSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.shadows.ShadowLog;
import rx.Observable;
import rx.observers.AssertableSubscriber;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

import android.support.annotation.NonNull;

import javax.inject.Provider;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class PlaylistDetailsPresenterTest extends AndroidUnitTest {

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
    @Mock private DataSourceProvider dataSourceProvider;
    @Mock private RepostOperations repostOperations;
    @Mock private RepostsStateProvider repostsStateProvider;
    @Mock private OfflineSettingsStorage offlineSettingsStorage;
    @Mock private TrackingStateProvider trackingStateProvider;
    @Mock private PlaylistDetailsPresenter.PlaylistDetailView playlistDetailView;

    private TestEventBus eventBus = new TestEventBus();

    private final Track track1 = ModelFixtures.track();
    private final TrackItem trackItem1 = ModelFixtures.trackItem(track1);
    private final Track track2 = ModelFixtures.track();
    private final TrackItem trackItem2 = ModelFixtures.trackItem(track2);
    private final Track track3 = ModelFixtures.track();
    private final TrackItem trackItem3 = ModelFixtures.trackItem(track3);

    private final List<TrackItem> trackItems = asList(trackItem1, trackItem2);
    private final List<TrackItem> updatedTrackItems = asList(trackItem1, trackItem2, trackItem3);

    private final Playlist initialPlaylist = ModelFixtures.playlistBuilder().build();
    private final Playlist updatedPlaylist = initialPlaylist.toBuilder().urn(initialPlaylist.urn()).title("new-title").build();

    private final String screen = Screen.PLAYLIST_DETAILS.get();
    private final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(Urn.forGenre("asdf"), "querystring");
    private final PromotedSourceInfo promotedSourceInfo = new PromotedSourceInfo("ad-urn", Urn.forTrack(1), absent(), emptyList());

    private final PlaylistDetailsViewModel initialModel = PlaylistDetailFixtures.create(resources(), initialPlaylist, trackItems);
    private final PlaylistDetailsViewModel updatedModel = PlaylistDetailFixtures.create(resources(), updatedPlaylist, updatedTrackItems);
    private final Urn playlistUrn = initialPlaylist.urn();

    private final BehaviorSubject<LikedStatuses> likeStatuses = BehaviorSubject.create();
    private final PublishSubject<RepostStatuses> repostStatuses = PublishSubject.create();
    private final rx.subjects.BehaviorSubject<OfflineProperties> offlineProperties = rx.subjects.BehaviorSubject.create(OfflineProperties.empty());
    private final SingleSubject<SyncJobResult> syncPlaylist = SingleSubject.create();

    private final PlaylistWithExtras initialPlaylistWithTrackExtras = PlaylistWithExtras.create(initialPlaylist, of(asList(track1, track2)), false);
    private final PlaylistWithExtras updatedPlaylistWithTrackExtras = PlaylistWithExtras.create(updatedPlaylist, of(asList(track1, track2, track3)), false);
    private final Playlist otherPlaylistByUser = ModelFixtures.playlist();
    private final PlaylistWithExtras initialPlaylistWithAllExtras = PlaylistWithExtras.create(initialPlaylist, of(asList(track1, track2)), singletonList(otherPlaylistByUser), false);

    private final rx.subjects.BehaviorSubject<PlaylistWithExtrasState> dataSource = rx.subjects.BehaviorSubject.create();

    private final PlaylistDetailsInputs inputs = PlaylistDetailsInputs.create();
    private PlaylistDetailsPresenter newPlaylistPresenter;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;

        when(playlistDetailView.onEnterScreenTimestamp()).thenReturn(io.reactivex.Observable.just(123L));
        when(upsellOperations.getUpsell(any(Playlist.class), anyList())).thenReturn(absent());
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(featureOperations.upsellOfflineContent()).thenReturn(false);

        when(dataSourceProvider.dataWith(any(Urn.class), eq(inputs.refresh))).thenReturn(dataSource);

        when(likesStateProvider.likedStatuses()).thenReturn(likeStatuses);
        when(likesStateProvider.latest()).thenReturn(LikedStatuses.create(emptySet()));
        when(repostsStateProvider.repostedStatuses()).thenReturn(repostStatuses);
        when(syncInitiator.syncPlaylist(playlistUrn)).thenReturn(syncPlaylist);
        when(offlinePropertiesProvider.states()).thenReturn(offlineProperties);
        when(offlineSettingsStorage.isOfflineContentAccessible()).thenReturn(true);

        Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider = TestSubscribers.expandPlayerSubscriber(eventBus);
        newPlaylistPresenter = new PlaylistDetailsPresenter(screen,
                                                            searchQuerySourceInfo,
                                                            promotedSourceInfo,
                                                            resources(),
                                                            upsellOperations,
                                                            playbackInitiator,
                                                            playlistOperations,
                                                            likesStateProvider,
                                                            repostsStateProvider,
                                                            playQueueHelper,
                                                            offlinePropertiesProvider,
                                                            eventBus,
                                                            offlineContentOperations,
                                                            eventTracker,
                                                            likeOperations,
                                                            dataSourceProvider,
                                                            repostOperations,
                                                            accountOperations,
                                                            expandPlayerSubscriberProvider,
                                                            ModelFixtures.entityItemCreator(),
                                                            featureOperations,
                                                            offlineSettingsStorage,
                                                            trackingStateProvider);
    }

    private void connect() {
        connect(initialPlaylistWithTrackExtras);
    }

    private void connect(PlaylistWithExtras initialEmission) {
        newPlaylistPresenter.connect(inputs, playlistDetailView, playlistUrn);
        emitLikedEntities();
        emitRepostedEntities();
        emitOfflineEntities(Collections.<Urn, OfflineState>emptyMap());
        dataSource.onNext(PlaylistWithExtrasState.builder().playlistWithExtras(of(initialEmission)).build());
    }

    @Test
    public void emitsPlaylistFromStorage() {
        connect();

        newPlaylistPresenter.viewModel()
                            .test()
                            .assertValue(getIdleViewModel(initialModel));
    }

    @Test
    public void emitsPlaylistWithOtherPlaylistsByUserFromStorage() {
        connect(initialPlaylistWithAllExtras);

        PlaylistDetailOtherPlaylistsItem otherPlaylistsItem = new PlaylistDetailOtherPlaylistsItem(initialPlaylist.creatorName(),
                                                                                                   singletonList(playlistItem(otherPlaylistByUser)),
                                                                                                   initialPlaylist.isAlbum());

        newPlaylistPresenter.viewModel()
                            .test()
                            .assertValue(getIdleViewModel(PlaylistDetailFixtures.create(resources(), initialPlaylist, trackItems, otherPlaylistsItem)));
    }

    @Test
    public void goesToCreatorOnCreatorClick() throws Exception {
        connect();

        AssertableSubscriber<Urn> test = newPlaylistPresenter.goToCreator().test();

        inputs.onCreatorClicked();

        test.assertReceivedOnNext(singletonList(initialPlaylist.creatorUrn()));
    }

    @Test
    public void onEnterEditModeGoesToEditMode() throws Exception {
        connect();

        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();

        inputs.onEnterEditMode();

        modelUpdates.assertValues(getIdleViewModel(initialModel), getIdleViewModel(toEditMode(initialModel)));
    }

    @Test
    public void onExitEditModeGoesBackToEditMode() throws Exception {
        connect();

        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();

        inputs.onEnterEditMode();
        inputs.onExitEditMode();

        modelUpdates.assertValues(getIdleViewModel(initialModel), getIdleViewModel(toEditMode(initialModel)), getIdleViewModel(initialModel));
    }

    @Test
    public void makeAvailableOfflineWithOwnerPlaylistMakesAvailableOffline() throws Exception {
        connect();

        rx.subjects.PublishSubject<Void> offlineSubject = rx.subjects.PublishSubject.create();
        when(accountOperations.isLoggedInUser(initialPlaylist.creatorUrn())).thenReturn(true);
        when(offlineContentOperations.makePlaylistAvailableOffline(playlistUrn)).thenReturn(offlineSubject);
        when(likesStateProvider.latest()).thenReturn(LikedStatuses.create(emptySet()));
        when(accountOperations.isLoggedInUser(initialPlaylist.creatorUrn())).thenReturn(true);
        when(likeOperations.toggleLike(playlistUrn, true)).thenReturn(Single.just(LikeOperations.LikeResult.LIKE_FAILED)); // should not be called, but just being safe

        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();

        inputs.onMakeOfflineAvailable();

        offlineSubject.onNext(null);

        assertThat(offlineSubject.hasObservers()).isTrue();
        modelUpdates.assertValues(getIdleViewModel(initialModel), getIdleViewModel(initialModel.updateWithMarkedForOffline(true)));
    }

    @Test
    public void makeOfflineDoesNotMakeOtherUsersPlaylistOfflineWithFailedLike() throws Exception {
        connect();

        rx.subjects.PublishSubject<Void> offlineSubject = rx.subjects.PublishSubject.create();
        when(offlineContentOperations.makePlaylistAvailableOffline(playlistUrn)).thenReturn(offlineSubject);
        when(likesStateProvider.latest()).thenReturn(LikedStatuses.create(emptySet()));
        when(accountOperations.isLoggedInUser(initialPlaylist.creatorUrn())).thenReturn(false);
        when(likeOperations.toggleLike(playlistUrn, true)).thenReturn(Single.just(LikeOperations.LikeResult.LIKE_FAILED));

        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();

        inputs.onMakeOfflineAvailable();

        assertThat(offlineSubject.hasObservers()).isFalse();
        modelUpdates.assertValues(getIdleViewModel(initialModel), getIdleViewModel(initialModel));
    }

    @Test
    public void makeOfflineMakesPlaylistOfflineAfterSuccessfulLike() throws Exception {
        connect();

        rx.subjects.PublishSubject<Void> offlineSubject = rx.subjects.PublishSubject.create();
        when(offlineContentOperations.makePlaylistAvailableOffline(playlistUrn)).thenReturn(offlineSubject);
        when(likesStateProvider.latest()).thenReturn(LikedStatuses.create(emptySet()));
        when(accountOperations.isLoggedInUser(initialPlaylist.creatorUrn())).thenReturn(false);
        when(likeOperations.toggleLike(playlistUrn, true)).thenReturn(Single.just(LikeOperations.LikeResult.LIKE_SUCCEEDED));

        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();

        inputs.onMakeOfflineAvailable();

        offlineSubject.onNext(null);

        assertThat(offlineSubject.hasObservers()).isTrue();
        modelUpdates.assertValues(getIdleViewModel(initialModel), getIdleViewModel(initialModel.updateWithMarkedForOffline(true)));
    }

    @Test
    public void onMakeAvailableOfflineEmitsTracking() throws Exception {
        connect();

        rx.subjects.PublishSubject<Void> offlineSubject = rx.subjects.PublishSubject.create();
        when(offlineContentOperations.makePlaylistAvailableOffline(playlistUrn)).thenReturn(offlineSubject);
        when(accountOperations.isLoggedInUser(initialPlaylist.creatorUrn())).thenReturn(true);

        inputs.onMakeOfflineAvailable();

        offlineSubject.onNext(null);

        OfflineInteractionEvent event = eventBus.lastEventOn(EventQueue.TRACKING, OfflineInteractionEvent.class);
        assertThat(event.clickObject()).isEqualTo(of(playlistUrn));
        assertThat(event.clickName()).isEqualTo(of(OfflineInteractionEvent.Kind.KIND_OFFLINE_PLAYLIST_ADD));
        assertThat(event.eventName()).isEqualTo(OfflineInteractionEvent.EventName.CLICK);
    }

    @Test
    public void onMakeAvailableOfflineHandlesOfflineContentNotAccessible() {
        connect();

        rx.subjects.PublishSubject<Void> offlineSubject = rx.subjects.PublishSubject.create();
        final AssertableSubscriber<Void> assertableSubscriber = newPlaylistPresenter.onShowOfflineStorageErrorDialog().test();
        when(offlineSettingsStorage.isOfflineContentAccessible()).thenReturn(false);

        inputs.onMakeOfflineAvailable();

        offlineSubject.onNext(null);

        assertableSubscriber.assertValue(null);
        verifyZeroInteractions(offlineContentOperations);
    }

    @Test
    public void onMakeUnavailableOfflineMakesOfflineUnavailable() throws Exception {
        connect();

        rx.subjects.PublishSubject<Void> offlineSubject = rx.subjects.PublishSubject.create();
        when(offlineContentOperations.makePlaylistUnavailableOffline(playlistUrn)).thenReturn(offlineSubject);

        inputs.onMakeOfflineUnavailable();

        assertThat(offlineSubject.hasObservers()).isTrue();
    }

    @Test
    public void onMakeUnavailableOfflineShowsConfirmationDialogWhenOfflineCollectionIsEnabled() throws Exception {
        when(offlineContentOperations.isOfflineCollectionEnabled()).thenReturn(true);
        connect();

        AssertableSubscriber<Pair<Urn, PlaySessionSource>> goToUpsell = newPlaylistPresenter.onShowDisableOfflineCollectionConfirmation().test();

        inputs.onMakeOfflineUnavailable();

        goToUpsell.assertValue(Pair.of(playlistUrn, createPlaySessionSource()));
    }

    @Test
    public void onMakeUnavailableOfflineEmitsTracking() throws Exception {
        connect();

        when(offlineContentOperations.makePlaylistUnavailableOffline(playlistUrn)).thenReturn(rx.subjects.PublishSubject.create());

        inputs.onMakeOfflineUnavailable();

        OfflineInteractionEvent event = eventBus.lastEventOn(EventQueue.TRACKING, OfflineInteractionEvent.class);
        assertThat(event.clickObject()).isEqualTo(of(playlistUrn));
        assertThat(event.clickName()).isEqualTo(of(OfflineInteractionEvent.Kind.KIND_OFFLINE_PLAYLIST_REMOVE));
        assertThat(event.eventName()).isEqualTo(OfflineInteractionEvent.EventName.CLICK);
    }

    @Test
    public void headerSendsExpandPlayerCommandAfterPlayback() throws Exception {
        connect();

        List<Urn> trackUrns = transform(trackItems, PlayableItem::getUrn);
        when(playlistOperations.trackUrnsForPlayback(playlistUrn)).thenReturn(Observable.just(trackUrns));
        when(playbackInitiator.playTracks(trackUrns, 0, createPlaySessionSource())).thenReturn(Single.just(PlaybackResult.success()));

        inputs.onHeaderPlayButtonClicked();

        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isExpand()).isTrue();
    }

    @Test
    public void headerPlayEmitsPlaybackError() throws Exception {
        connect();

        List<Urn> trackUrns = transform(trackItems, PlayableItem::getUrn);
        when(playlistOperations.trackUrnsForPlayback(playlistUrn)).thenReturn(Observable.just(trackUrns));
        when(playbackInitiator.playTracks(trackUrns, 0, createPlaySessionSource())).thenReturn(Single.just(PlaybackResult.error(ErrorReason.MISSING_PLAYABLE_TRACKS)));

        AssertableSubscriber<ErrorReason> test = newPlaylistPresenter.onPlaybackError().test();

        inputs.onHeaderPlayButtonClicked();

        test.assertReceivedOnNext(singletonList(ErrorReason.MISSING_PLAYABLE_TRACKS));
    }

    @Test
    public void playNextAddsToUpNextWithHelper() throws Exception {
        connect();

        inputs.onPlayNext();

        verify(playQueueHelper).playNext(playlistUrn);
    }

    @Test
    public void likeShowsResult() throws Exception {
        connect();

        LikeOperations.LikeResult likeResult = LikeOperations.LikeResult.LIKE_SUCCEEDED;
        when(likeOperations.toggleLike(playlistUrn, true)).thenReturn(Single.just(likeResult));

        AssertableSubscriber<LikeOperations.LikeResult> subscriber = newPlaylistPresenter.onLikeResult().test();

        inputs.onToggleLike(true);

        subscriber.assertValue(likeResult);
    }

    @Test
    public void likeSendsTracking() throws Exception {
        connect();

        when(likeOperations.toggleLike(playlistUrn, true)).thenReturn(SingleSubject.create());

        inputs.onToggleLike(true);

        verify(eventTracker).trackEngagement(UIEvent.fromToggleLike(true, playlistUrn, eventContext(initialPlaylist), promotedSourceInfo, createEntityMetadata(initialPlaylist)));
    }

    @Test
    public void unlikeSendsResult() throws Exception {
        connect();

        LikeOperations.LikeResult likeResult = LikeOperations.LikeResult.UNLIKE_SUCCEEDED;
        when(likeOperations.toggleLike(playlistUrn, false)).thenReturn(Single.just(likeResult));

        AssertableSubscriber<LikeOperations.LikeResult> subscriber = newPlaylistPresenter.onLikeResult().test();

        inputs.onToggleLike(false);

        subscriber.assertValue(likeResult);
    }

    @Test
    public void unlikeSendsTracking() throws Exception {
        connect();

        when(likeOperations.toggleLike(playlistUrn, false)).thenReturn(SingleSubject.create());

        inputs.onToggleLike(false);

        verify(eventTracker).trackEngagement(UIEvent.fromToggleLike(false, playlistUrn, eventContext(initialPlaylist), promotedSourceInfo, createEntityMetadata(initialPlaylist)));
    }

    @Test
    public void repostShowsResult() {
        connect();

        when(repostOperations.toggleRepost(playlistUrn, true)).thenReturn(Single.just(RepostOperations.RepostResult.REPOST_SUCCEEDED));

        AssertableSubscriber<RepostOperations.RepostResult> subscriber = newPlaylistPresenter.onRepostResult().test();

        inputs.onToggleRepost(true);

        subscriber.assertValue(RepostOperations.RepostResult.REPOST_SUCCEEDED);
    }

    @Test
    public void unpostShowsResult() {
        connect();

        when(repostOperations.toggleRepost(playlistUrn, false)).thenReturn(Single.just(RepostOperations.RepostResult.UNREPOST_SUCCEEDED));

        AssertableSubscriber<RepostOperations.RepostResult> subscriber = newPlaylistPresenter.onRepostResult().test();

        inputs.onToggleRepost(false);

        subscriber.assertValue(RepostOperations.RepostResult.UNREPOST_SUCCEEDED);
    }

    @Test
    public void repostSendsTracking() {
        connect();

        when(repostOperations.toggleRepost(playlistUrn, true)).thenReturn(Single.just(RepostOperations.RepostResult.REPOST_SUCCEEDED));

        inputs.onToggleRepost(true);

        verify(eventTracker).trackEngagement(UIEvent.fromToggleRepost(true, playlistUrn, eventContext(initialPlaylist), promotedSourceInfo, createEntityMetadata(initialPlaylist)));
    }

    @Test
    public void unpostSendsTracking() {
        connect();

        when(repostOperations.toggleRepost(playlistUrn, false)).thenReturn(Single.just(RepostOperations.RepostResult.UNREPOST_SUCCEEDED));

        inputs.onToggleRepost(false);

        verify(eventTracker).trackEngagement(UIEvent.fromToggleRepost(false, playlistUrn, eventContext(initialPlaylist), promotedSourceInfo, createEntityMetadata(initialPlaylist)));
    }

    @Test
    public void upsellsOfflineFromMakeOffline() {
        connect();

        AssertableSubscriber<Urn> goToUpsell = newPlaylistPresenter.goToOfflineUpsell().test();

        UpgradeFunnelEvent expectedEvent = UpgradeFunnelEvent.forPlaylistPageClick(playlistUrn);

        inputs.onMakeOfflineUpsell();

        UpgradeFunnelEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);
        assertThat(trackingEvent.getKind()).isEqualTo(expectedEvent.getKind());

        goToUpsell.assertValue(playlistUrn);
    }

    @Test
    public void emitsUpdatedPlaylistAfterLike() {
        connect();

        final PlaylistDetailsMetadata likedHeader = initialModel.metadata().toBuilder().isLikedByUser(true).build();
        final PlaylistDetailsViewModel likedPlaylist = initialModel.toBuilder().metadata(likedHeader).build();

        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();
        modelUpdates.assertValues(getIdleViewModel(initialModel));

        emitLikedEntities(Urn.forTrack(123L));
        modelUpdates.assertValues(getIdleViewModel(initialModel));

        emitLikedEntities(Urn.forTrack(123L), playlistUrn);
        modelUpdates.assertValues(getIdleViewModel(initialModel), getIdleViewModel(likedPlaylist));
    }

    @Test
    public void emitsUpdatedPlaylistAfterOfflineStateChange() throws Exception {
        connect();

        final PlaylistDetailsMetadata offlineHeader = initialModel.metadata().toBuilder()
                                                                  .isMarkedForOffline(true)
                                                                  .offlineState(OfflineState.DOWNLOADED).build();
        final PlaylistDetailsViewModel offlinePlaylist = initialModel.toBuilder().metadata(offlineHeader).build();

        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();
        modelUpdates.assertValues(getIdleViewModel(initialModel));

        emitOfflineEntities(singletonMap(Urn.forTrack(123L), OfflineState.DOWNLOADED));
        modelUpdates.assertValues(getIdleViewModel(initialModel));

        emitOfflineEntities(singletonMap(playlistUrn, OfflineState.DOWNLOADED));
        modelUpdates.assertValues(getIdleViewModel(initialModel), getIdleViewModel(offlinePlaylist));
    }

    @Test
    public void doesNotEmitsUpdatedPlaylistAfterOfflineStateChangeWhileEditing() throws Exception {
        connect();

        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();
        modelUpdates.assertValues(getIdleViewModel(initialModel));

        emitOfflineEntities(singletonMap(Urn.forTrack(123L), OfflineState.DOWNLOADED));
        modelUpdates.assertValues(getIdleViewModel(initialModel));

        inputs.onEnterEditMode();

        emitOfflineEntities(singletonMap(playlistUrn, OfflineState.DOWNLOADED));
        modelUpdates.assertValues(getIdleViewModel(initialModel), getIdleViewModel(toEditMode(initialModel)));
    }

    @Test
    public void emitsRefreshSequence() {
        connect();
        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();

        dataSource.onNext(PlaylistWithExtrasState.builder()
                                                 .playlistWithExtras(of(initialPlaylistWithTrackExtras))
                                                 .isRefreshing(true)
                                                 .build());

        dataSource.onNext(PlaylistWithExtrasState.builder()
                                                 .playlistWithExtras(of(updatedPlaylistWithTrackExtras))
                                                 .isRefreshing(true)
                                                 .build());

        dataSource.onNext(PlaylistWithExtrasState.builder()
                                                 .playlistWithExtras(of(updatedPlaylistWithTrackExtras))
                                                 .build());

        syncPlaylist.onSuccess(TestSyncJobResults.successWithChange());
        modelUpdates.assertValues(getIdleViewModel(initialModel),
                                  getRefreshingModel(initialModel),
                                  getRefreshingModel(updatedModel),
                                  getIdleViewModel(updatedModel));
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

    @Test
    public void fireUpsellImpressionSendsUpsellTrackingEvent() {
        connect();

        UpgradeFunnelEvent expectedEvent = UpgradeFunnelEvent.forPlaylistTracksImpression(playlistUrn);

        newPlaylistPresenter.firePlaylistTracksUpsellImpression();

        UpgradeFunnelEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);
        assertThat(trackingEvent.getKind()).isEqualTo(expectedEvent.getKind());
    }

    @Test
    public void onItemTriggeredForTrackStartsPlaybackOnGivenTrack() {
        connect();

        when(playbackInitiator.playTracks(any(List.class), anyInt(), any(PlaySessionSource.class))).thenReturn(Single.just(PlaybackResult.success()));

        inputs.onItemTriggered(initialModel.tracks().get(1));

        final List<Urn> tracks = transform(initialModel.tracks(), PlaylistDetailTrackItem::getUrn);
        verify(playbackInitiator).playTracks(tracks, 1, createPlaySessionSource());
    }

    @Test
    public void onItemDismissed() {
        final PlaylistDetailUpsellItem upsellItem = new PlaylistDetailUpsellItem(trackItem2);
        when(upsellOperations.getUpsell(any(Playlist.class), anyList())).thenReturn(of(upsellItem));

        final PlaylistDetailsViewModel initialModelWithUpsell = initialModel.toBuilder().upsell(upsellItem).build();
        final PlaylistDetailsViewModel modelWithoutUpsell = initialModelWithUpsell.toBuilder().upsell(absent()).build();

        connect();
        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();

        modelUpdates.assertValues(getIdleViewModel(initialModelWithUpsell));

        when(upsellOperations.getUpsell(any(Playlist.class), anyList())).thenReturn(absent());
        inputs.onItemDismissed(upsellItem);

        modelUpdates.assertValues(getIdleViewModel(initialModelWithUpsell), getIdleViewModel(modelWithoutUpsell));
    }

    @Test
    public void savesPlaylistWithNewTracklist() throws Exception {
        connect();

        when(playlistOperations.editPlaylistTracks(
                updatedPlaylist.urn(),
                asList(trackItem2.getUrn(), trackItem1.getUrn())
        )).thenReturn(Observable.just(asList(track2, track1)));

        inputs.actionUpdateTrackList(asList(
                getPlaylistDetailTrackItem(trackItem2),
                getPlaylistDetailTrackItem(trackItem1)));

        PlaylistDetailsViewModel viewModelWithReversedTracks = PlaylistDetailFixtures.create(resources(), initialPlaylist,
                                                                                             asList(trackItem2, trackItem1));

        newPlaylistPresenter.viewModel()
                            .test()
                            .assertValue(getIdleViewModel(viewModelWithReversedTracks));
    }

    @Test
    public void shareShares() throws Exception {
        connect();

        AssertableSubscriber<SharePresenter.ShareOptions> testSubscriber = newPlaylistPresenter.onShare().test();

        inputs.onShareClicked();


        testSubscriber.assertValue(SharePresenter.ShareOptions.create(initialPlaylist.permalinkUrl().get(),
                                                                      eventContext(initialPlaylist),
                                                                      promotedSourceInfo,
                                                                      createEntityMetadata(initialPlaylist)));
    }

    @Test
    public void overflowUpsellTracksAndGoesToOfflineUpsell() {
        connect();

        AssertableSubscriber<Urn> testSubscriber = newPlaylistPresenter.goToOfflineUpsell().test();

        inputs.onOverflowUpsell();

        testSubscriber.assertValue(playlistUrn);

        TrackingEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent).isEqualTo(UpgradeFunnelEvent.forPlaylistOverflowClick(playlistUrn));
    }

    @Test
    public void overflowFiresUpsellImpression() {
        connect();

        inputs.onOverflowUpsellImpression();

        TrackingEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent).isEqualTo(UpgradeFunnelEvent.forPlaylistOverflowImpression(playlistUrn));
    }

    @Test
    public void playShuffledPlaysShuffled() {
        connect();

        AssertableSubscriber<ErrorReason> testSubscriber = newPlaylistPresenter.onPlaybackError().test();

        ArgumentCaptor<Single<List<Urn>>> tracklistCaptor = ArgumentCaptor.forClass(Single.class);
        when(playbackInitiator.playTracksShuffled(tracklistCaptor.capture(), eq(createPlaySessionSource()))).thenReturn(Single.just(PlaybackResult.success()));

        inputs.onPlayShuffled();

        tracklistCaptor.getValue().test().assertValue(transform(trackItems, PlayableItem::getUrn));
        testSubscriber.assertNoValues();
        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND)).isEqualTo(PlayerUICommand.expandPlayer());
    }

    @Test
    public void playShuffledHandlesError() {
        connect();

        AssertableSubscriber<ErrorReason> testSubscriber = newPlaylistPresenter.onPlaybackError().test();

        ArgumentCaptor<Single<List<Urn>>> tracklistCaptor = ArgumentCaptor.forClass(Single.class);
        when(playbackInitiator.playTracksShuffled(tracklistCaptor.capture(), eq(createPlaySessionSource()))).thenReturn(Single.just(PlaybackResult.error(ErrorReason.TRACK_UNAVAILABLE_CAST)));

        inputs.onPlayShuffled();

        tracklistCaptor.getValue().test().assertValue(transform(trackItems, PlayableItem::getUrn));
        testSubscriber.assertValue(ErrorReason.TRACK_UNAVAILABLE_CAST);
    }

    private PlaylistDetailTrackItem getPlaylistDetailTrackItem(TrackItem trackItem) {
        return PlaylistDetailTrackItem.builder().trackItem(trackItem).build();
    }

    private void emitLikedEntities(Urn... urns) {
        final HashSet<Urn> likedEntities = new HashSet<>(asList(urns));
        likeStatuses.onNext(LikedStatuses.create(likedEntities));
    }

    private void emitRepostedEntities(Urn... urns) {
        final HashSet<Urn> repostedEntities = new HashSet<>(asList(urns));
        repostStatuses.onNext(RepostStatuses.create(repostedEntities));
    }

    private void emitOfflineEntities(Map<Urn, OfflineState> offlineStateMap) {
        offlineProperties.onNext(OfflineProperties.from(offlineStateMap, OfflineState.NOT_OFFLINE));
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
                     .otherPlaylists(absent())
                     .build();
    }

    private EventContextMetadata eventContext(Playlist playlist) {
        return EventContextMetadata.builder()
                                   .pageName(Screen.PLAYLIST_DETAILS.get())
                                   .pageUrn(playlist.urn())
                                   .build();
    }

    private EntityMetadata createEntityMetadata(Playlist playlist) {
        return EntityMetadata.from(playlist.creatorName(), playlist.creatorUrn(),
                                   playlist.title(), playlist.urn());
    }

    @NonNull
    private AsyncViewModel<PlaylistDetailsViewModel> getIdleViewModel(PlaylistDetailsViewModel model) {
        return AsyncViewModel.<PlaylistDetailsViewModel>builder()
                .data(of(model))
                .isLoadingNextPage(false)
                .isRefreshing(false)
                .error(absent())
                .refreshError(absent())
                .build();
    }

    @NonNull
    private AsyncViewModel<PlaylistDetailsViewModel> getRefreshingModel(PlaylistDetailsViewModel initialModel) {
        return AsyncViewModel.<PlaylistDetailsViewModel>builder()
                .data(of(initialModel))
                .isLoadingNextPage(false)
                .isRefreshing(true)
                .error(absent())
                .refreshError(absent())
                .build();
    }

}
