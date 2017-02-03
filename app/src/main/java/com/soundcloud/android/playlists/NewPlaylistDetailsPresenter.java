package com.soundcloud.android.playlists;

import static com.soundcloud.android.events.EventQueue.URN_STATE_CHANGED;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.java.collections.Lists.transform;
import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.of;
import static java.util.Collections.singleton;
import static rx.Observable.combineLatest;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.associations.RepostStatuses;
import com.soundcloud.android.associations.RepostsStateProvider;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlaylistChangedEvent;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UrnStateChangedEvent;
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
import com.soundcloud.android.playback.playqueue.PlayQueueHelper;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.transformers.Transformers;
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@AutoFactory
class NewPlaylistDetailsPresenter implements PlaylistDetailsInputs {

    private final PlaylistOperations playlistOperations;
    private final PlaybackInitiator playbackInitiator;
    private final LikesStateProvider likesStateProvider;
    private final RepostsStateProvider repostsStateProvider;
    private final PlayQueueHelper playQueueHelper;
    private final OfflinePropertiesProvider offlinePropertiesProvider;
    private final SyncInitiator syncInitiator;
    private final EventBus eventBus;
    private final OfflineContentOperations offlineContentOperations;
    private final EventTracker eventTracker;
    private final LikeOperations likeOperations;
    private final RepostOperations repostOperations;
    private final SearchQuerySourceInfo searchQuerySourceInfo;
    private final PromotedSourceInfo promotedSourceInfo;
    private final String screen;

    private final PlaylistDetailsViewModelCreator viewModelCreator;

    private Subscription subscription = RxUtils.invalidSubscription();

    // state
    private final BehaviorSubject<Boolean> refreshState = BehaviorSubject.create(false);
    private final BehaviorSubject<Boolean> editMode = BehaviorSubject.create(false);

    // inputs
    private final PublishSubject<Void> refresh = PublishSubject.create();
    private final PublishSubject<Void> playNext = PublishSubject.create();
    private final PublishSubject<Void> delete = PublishSubject.create();
    private final PublishSubject<Void> offlineAvailable = PublishSubject.create();
    private final PublishSubject<Void> offlineUnavailable = PublishSubject.create();
    private final PublishSubject<Void> onCreatorClicked = PublishSubject.create();
    private final PublishSubject<Void> headerPlayClicked = PublishSubject.create();
    private final PublishSubject<Integer> trackPlayClicked = PublishSubject.create();
    private final PublishSubject<Boolean> like = PublishSubject.create();
    private final PublishSubject<Boolean> repost = PublishSubject.create();

    // outputs
    private final BehaviorSubject<AsyncViewModel<PlaylistDetailsViewModel>> viewModelSubject = BehaviorSubject.create();
    private final PublishSubject<Urn> gotoCreator = PublishSubject.create();
    private final PublishSubject<Object> goBack = PublishSubject.create();
    private final PublishSubject<RepostOperations.RepostResult> showRepostResult = PublishSubject.create();
    private final PublishSubject<LikeOperations.LikeResult> showLikeResult = PublishSubject.create();
    private final PublishSubject<Urn> showPlaylistDeletionConfirmation = PublishSubject.create();
    private final PublishSubject<PlaybackResult.ErrorReason> playbackError = PublishSubject.create();
    private final BehaviorSubject<PlaylistWithTracks> dataSource;

    NewPlaylistDetailsPresenter(Urn playlistUrn,
                                String screen,
                                @Nullable SearchQuerySourceInfo searchQuerySourceInfo,
                                @Nullable PromotedSourceInfo promotedSourceInfo,
                                @Provided PlaybackInitiator playbackInitiator,
                                @Provided PlaylistOperations playlistOperations,
                                @Provided LikesStateProvider likesStateProvider,
                                @Provided RepostsStateProvider repostsStateProvider,
                                @Provided PlayQueueHelper playQueueHelper,
                                @Provided OfflinePropertiesProvider offlinePropertiesProvider,
                                @Provided SyncInitiator syncInitiator,
                                @Provided EventBus eventBus,
                                @Provided OfflineContentOperations offlineContentOperations,
                                @Provided EventTracker eventTracker,
                                @Provided LikeOperations likeOperations,
                                @Provided PlaylistDetailsViewModelCreator viewModelCreator,
                                @Provided NewPlaylistDetailsPresenter_DataSourceProviderFactory dataSourceProviderFactory,
                                @Provided RepostOperations repostOperations) {
        this.searchQuerySourceInfo = searchQuerySourceInfo;
        this.promotedSourceInfo = promotedSourceInfo;
        this.screen = screen;
        this.playbackInitiator = playbackInitiator;
        this.playlistOperations = playlistOperations;
        this.likesStateProvider = likesStateProvider;
        this.repostsStateProvider = repostsStateProvider;
        this.playQueueHelper = playQueueHelper;
        this.offlinePropertiesProvider = offlinePropertiesProvider;
        this.syncInitiator = syncInitiator;
        this.eventBus = eventBus;
        this.offlineContentOperations = offlineContentOperations;
        this.eventTracker = eventTracker;
        this.likeOperations = likeOperations;
        this.viewModelCreator = viewModelCreator;
        this.repostOperations = repostOperations;

        // Note: do NOT store this urn. Always get it from DataSource, as it may change when a local playlist is pushed
        this.dataSource = dataSourceProviderFactory.create(playlistUrn).data();
    }

    public void connect() {
        subscription.unsubscribe();
        subscription = new CompositeSubscription(

                actionRefresh(refresh),
                actionGoToCreator(onCreatorClicked),
                actionMakeAvailableOffline(offlineAvailable),
                actionMakeOfflineUnavailableOffline(offlineUnavailable),
                actionPlayNext(playNext),
                actionDeletePlaylist(delete),
                actionLike(like),
                actionRepost(repost),
                actionPlayPlaylist(headerPlayClicked),
                actionPlayPlaylistAtPosition(trackPlayClicked),

                onPlaylistDeleted(),

                emitViewModel()
        );
    }

    void actionUpdateTrackList(List<PlaylistDetailTrackItem> trackItems) {
        final PlaylistWithTracks previousState = dataSource.getValue();
        final Playlist playlist = previousState.playlist();

        fireAndForget(savePlaylist(playlist, transform(trackItems, PlaylistDetailTrackItem::getUrn)));
    }

    private Observable<Playlist> savePlaylist(Playlist playlist, List<Urn> tracks) {
        return playlistOperations.editPlaylist(playlist, tracks);
    }

    private Subscription emitViewModel() {
        return combineLatest(
                refreshState,
                editMode,
                dataSource,
                likesStateProvider.likedStatuses(),
                repostsStateProvider.repostedStatuses(),
                offlinePropertiesProvider.states(),
                this::combine)
                .distinctUntilChanged()
                .doOnNext(viewModelSubject::onNext)
                .subscribe(new DefaultSubscriber<>());
    }

    private Subscription actionPlayPlaylist(PublishSubject<Void> trigger) {
        return dataSource.compose(Transformers.takeWhen(trigger))
                         .map(playlistWithTracks -> playlistWithTracks.playlist().urn())
                         .flatMap(playlistOperations::trackUrnsForPlayback)
                         .withLatestFrom(playSessionSource(), Pair::of)
                         .flatMap(pair -> playbackInitiator.playTracks(pair.first(), 0, pair.second()))
                         .subscribe(showPlaybackResult());
    }

    private Subscription actionPlayPlaylistAtPosition(PublishSubject<Integer> trigger) {
        return dataSource
                .compose(Transformers.takePairWhen(trigger))
                .withLatestFrom(playSessionSource(), (playlistWithTracksIntegerPair, playSessionSource) -> {
                    final List<Track> tracks = playlistWithTracksIntegerPair.first.tracks();
                    final Integer position = playlistWithTracksIntegerPair.second;
                    return playTracksFromPosition(tracks, position, playSessionSource);
                })
                .flatMap(x -> x)
                .subscribe(showPlaybackResult());
    }

    private Subscription onPlaylistDeleted() {
        Observable<UrnStateChangedEvent> playlistDeleted = eventBus
                .queue(URN_STATE_CHANGED)
                .filter(event -> event.kind() == UrnStateChangedEvent.Kind.ENTITY_DELETED)
                .filter(UrnStateChangedEvent::containsPlaylist);

        return dataSource
                .map(playlistWithTracks -> playlistWithTracks.playlist().urn())
                .compose(Transformers.takePairWhen(playlistDeleted))
                .filter(pair -> pair.second.urns().contains(pair.first))
                .subscribe(goBack);
    }

    private Observable<PlaybackResult> playTracksFromPosition(List<Track> tracks, Integer position, PlaySessionSource playSessionSource) {
        return playbackInitiator.playTracks(
                transform(tracks, Track::urn),
                position,
                playSessionSource
        );
    }

    private Subscription actionLike(PublishSubject<Boolean> trigger) {
        return dataSource.compose(Transformers.takePairWhen(trigger))
                         .withLatestFrom(playSessionSource(), this::like)
                         .flatMap(x -> x)
                         .subscribe(showLikeResult);
    }

    private Subscription actionRepost(PublishSubject<Boolean> trigger) {
        return dataSource.compose(Transformers.takePairWhen(trigger))
                         .withLatestFrom(playSessionSource(), this::repost)
                         .flatMap(x -> x)
                         .subscribe(showRepostResult);
    }

    private Subscription actionMakeOfflineUnavailableOffline(PublishSubject<Void> trigger) {
        return dataSource.compose(Transformers.takeWhen(trigger))
                         .map(playlistWithTracks -> playlistWithTracks.playlist().urn())
                         .withLatestFrom(playSessionSource(), Pair::of)
                         .subscribe(makePlaylistUnavailableOffline());
    }

    private Subscription actionMakeAvailableOffline(PublishSubject<Void> trigger) {
        return dataSource.compose(Transformers.takeWhen(trigger))
                         .map(playlistWithTracks -> playlistWithTracks.playlist().urn())
                         .withLatestFrom(playSessionSource(), Pair::of)
                         .subscribe(makePlaylistAvailableOffline());
    }

    private Observable<PlaySessionSource> playSessionSource() {
        return dataSource.map(createPlaySessionSource());
    }

    private Subscription actionRefresh(PublishSubject<Void> trigger) {
        return dataSource.compose(Transformers.takeWhen(trigger))
                         .filter(playlistWithTracks -> isNotRefreshing())
                         .map(playlistWithTracks -> playlistWithTracks.playlist().urn())
                         .subscribe(this::doSync);
    }

    private boolean isNotRefreshing() {
        return !refreshState.hasValue() || !refreshState.getValue();
    }

    private void doSync(Urn urn) {
        syncInitiator.syncPlaylist(urn)
                     .subscribe(new DefaultSubscriber<SyncJobResult>() {
                         @Override
                         public void onStart() {
                             refreshState.onNext(true);
                         }

                         @Override
                         public void onCompleted() {
                             refreshState.onNext(false);
                         }
                     });
    }

    private Subscription actionGoToCreator(PublishSubject<Void> trigger) {
        return dataSource.compose(Transformers.takeWhen(trigger))
                         .map(playlistWithTracks -> playlistWithTracks.playlist().creatorUrn())
                         .subscribe(gotoCreator);
    }

    private Subscription actionPlayNext(PublishSubject<Void> trigger) {
        return dataSource.compose(Transformers.takeWhen(trigger))
                         .map(playlistWithTracks -> playlistWithTracks.playlist().urn())
                         .subscribe(playQueueHelper::playNext);
    }

    private Subscription actionDeletePlaylist(PublishSubject<Void> trigger) {
        return dataSource.compose(Transformers.takeWhen(trigger))
                         .map(playlistWithTracks -> playlistWithTracks.playlist().urn())
                         .subscribe(showPlaylistDeletionConfirmation);
    }

    private Action1<Pair<Urn, PlaySessionSource>> makePlaylistAvailableOffline() {
        return urnSourcePair -> {
            fireAndForget(offlineContentOperations.makePlaylistAvailableOffline(urnSourcePair.first()));
            eventBus.publish(EventQueue.TRACKING, getOfflinePlaylistTrackingEvent(urnSourcePair.first(), true, urnSourcePair.second()));
        };
    }

    private Action1<Pair<Urn, PlaySessionSource>> makePlaylistUnavailableOffline() {
        return urnSourcePair -> {
            fireAndForget(offlineContentOperations.makePlaylistUnavailableOffline(urnSourcePair.first()));
            eventBus.publish(EventQueue.TRACKING, getOfflinePlaylistTrackingEvent(urnSourcePair.first(), false, urnSourcePair.second()));

        };
    }

    private EntityMetadata createEntityMetadata(Playlist playlist) {
        return EntityMetadata.from(playlist.creatorName(), playlist.creatorUrn(),
                                   playlist.title(), playlist.urn());
    }

    private Action1<PlaybackResult> showPlaybackResult() {
        return playbackResult -> {
            if (playbackResult.isSuccess()) {
                eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
            } else {
                playbackError.onNext(playbackResult.getErrorReason());
            }
        };
    }

    private Func1<PlaylistWithTracks, PlaySessionSource> createPlaySessionSource() {
        return playlistWithTracks -> {
            final Playlist playlist = playlistWithTracks.playlist();
            PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(screen, playlist.urn(), playlist.creatorUrn(), playlist.trackCount());
            if (promotedSourceInfo != null) {
                playSessionSource.setPromotedSourceInfo(promotedSourceInfo);
            } else if (searchQuerySourceInfo != null) {
                playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
            }
            return playSessionSource;
        };
    }

    public void refresh() {
        refresh.onNext(null);
    }

    private AsyncViewModel<PlaylistDetailsViewModel> combine(Boolean isRefreshing,
                                                             Boolean isEditMode,
                                                             PlaylistWithTracks playlistWithTracks,
                                                             LikedStatuses likedStatuses,
                                                             RepostStatuses repostStatuses,
                                                             OfflineProperties offlineProperties) {
        return AsyncViewModel.create(of(viewModelCreator.create(playlistWithTracks.playlist(),
                                                                updateTracks(playlistWithTracks, offlineProperties),
                                                                likedStatuses.isLiked(playlistWithTracks.playlist().urn()),
                                                                repostStatuses.isReposted(playlistWithTracks.playlist().urn()),
                                                                isEditMode,
                                                                offlineProperties.state(playlistWithTracks.playlist().urn()),
                                                                absent()
        )), isRefreshing, Optional.absent());
    }

    private List<TrackItem> updateTracks(PlaylistWithTracks playlistWithTracks, OfflineProperties offlineProperties) {
        List<Track> tracks = playlistWithTracks.tracks();
        List<TrackItem> trackItems = new ArrayList<>(tracks.size());
        for (Track track : tracks) {
            // todo, use constructor instead of update
            trackItems.add(TrackItem.from(track).updatedWithOfflineState(offlineProperties.state(track.urn())));
        }
        return trackItems;
    }

    void disconnect() {
        subscription.unsubscribe();
    }

    Observable<Urn> goToCreator() {
        return gotoCreator;
    }

    PublishSubject<LikeOperations.LikeResult> onLikeResult() {
        return showLikeResult;
    }

    PublishSubject<RepostOperations.RepostResult> onRepostResult() {
        return showRepostResult;
    }

    Observable<PlaybackResult.ErrorReason> onPlaybackError() {
        return playbackError;
    }

    Observable<Object> onGoBack() {
        return goBack;
    }

    Observable<Urn> onRequestingPlaylistDeletion() {
        return showPlaylistDeletionConfirmation;
    }

    public Observable<AsyncViewModel<PlaylistDetailsViewModel>> viewModel() {
        return viewModelSubject;
    }

    @Override
    public void onCreatorClicked() {
        onCreatorClicked.onNext(null);
    }

    @Override
    public void onPlayAtPosition(Integer position) {
        trackPlayClicked.onNext(position);
    }

    @Override
    public void onEnterEditMode() {
        editMode.onNext(true);
    }

    @Override
    public void onExitEditMode() {
        editMode.onNext(false);
    }

    @Override
    public void onMakeOfflineAvailable() {
        offlineAvailable.onNext(null);
    }

    @Override
    public void onMakeOfflineUnavailable() {
        offlineUnavailable.onNext(null);
    }

    @Override
    public void onHeaderPlayButtonClicked() {
        headerPlayClicked.onNext(null);
    }

    @Override
    public void onPlayNext() {
        playNext.onNext(null);
    }

    @Override
    public void onToggleLike(boolean isLiked) {
        like.onNext(isLiked);
    }

    @Override
    public void onToggleRepost(boolean isReposted) {
        repost.onNext(isReposted);
    }

    @Override
    public void onShare() {

    }

    @Override
    public void onUpsell() {

    }

    @Override
    public void onOverflowUpsell() {

    }

    @Override
    public void onOverflowUpsellImpression() {

    }

    @Override
    public void onPlayShuffled() {

    }

    @Override
    public void onDeletePlaylist() {
        delete.onNext(null);
    }

    private Observable<LikeOperations.LikeResult> like(android.util.Pair<PlaylistWithTracks, Boolean> playlistWithTracksBooleanPair, PlaySessionSource playSessionSource) {
        Playlist playlist = playlistWithTracksBooleanPair.first.playlist();
        final Boolean isLike = playlistWithTracksBooleanPair.second;
        eventTracker.trackEngagement(UIEvent.fromToggleLike(isLike,
                                                            playlist.urn(),
                                                            getEventContext(playlist.urn()),
                                                            playSessionSource.getPromotedSourceInfo(),
                                                            createEntityMetadata(playlist)));

        return likeOperations.toggleLike(playlist.urn(), isLike);
    }

    private Observable<RepostOperations.RepostResult> repost(android.util.Pair<PlaylistWithTracks, Boolean> playlistWithTracksBooleanPair, PlaySessionSource playSessionSource) {
        final Playlist playlist = playlistWithTracksBooleanPair.first.playlist();
        final Boolean isReposted = playlistWithTracksBooleanPair.second;

        eventTracker.trackEngagement(UIEvent.fromToggleRepost(isReposted,
                                                              playlist.urn(),
                                                              getEventContext(playlist.urn()),
                                                              playSessionSource.getPromotedSourceInfo(),
                                                              createEntityMetadata(playlist)));

        return repostOperations.toggleRepost(playlist.urn(), isReposted);
    }

    private EventContextMetadata getEventContext(Urn playlistUrn) {
        return EventContextMetadata.builder()
                                   .contextScreen(screen)
                                   .pageName(Screen.PLAYLIST_DETAILS.get())
                                   .invokerScreen(Screen.PLAYLIST_DETAILS.get())
                                   .pageUrn(playlistUrn)
                                   .build();
    }

    private OfflineInteractionEvent getOfflinePlaylistTrackingEvent(Urn urn, boolean isMarkedForOffline, PlaySessionSource playSessionSource) {
        return isMarkedForOffline ?
               OfflineInteractionEvent.fromAddOfflinePlaylist(
                       Screen.PLAYLIST_DETAILS.get(),
                       urn,
                       playSessionSource.getPromotedSourceInfo()) :
               OfflineInteractionEvent.fromRemoveOfflinePlaylist(
                       Screen.PLAYLIST_DETAILS.get(),
                       urn,
                       playSessionSource.getPromotedSourceInfo());
    }

    @VisibleForTesting
    @AutoValue
    static abstract class PlaylistWithTracks {

        static PlaylistWithTracks create(Playlist playlist, List<Track> tracks) {
            return new AutoValue_NewPlaylistDetailsPresenter_PlaylistWithTracks(playlist, tracks);
        }

        abstract Playlist playlist();

        abstract List<Track> tracks();
    }

    @AutoFactory(allowSubclasses = true)
    static class DataSourceProvider {

        private PlaylistRepository playlistRepository;
        private TrackRepository trackRepository;
        private EventBus eventBus;
        private Urn playlistUrn;

        private final BehaviorSubject<PlaylistWithTracks> data = BehaviorSubject.create();
        private final Scheduler scheduler;

        @Inject
        DataSourceProvider(Urn initialUrn,
                           @Provided @Named(ApplicationModule.LOW_PRIORITY) Scheduler scheduler,
                           @Provided PlaylistRepository playlistRepository,
                           @Provided TrackRepository trackRepository,
                           @Provided EventBus eventBus) {
            this.scheduler = scheduler;
            this.playlistRepository = playlistRepository;
            this.trackRepository = trackRepository;
            this.eventBus = eventBus;
            this.playlistUrn = initialUrn;
            dataSource().subscribe(data);
        }

        public BehaviorSubject<PlaylistWithTracks> data() {
            return data;
        }

        Observable<PlaylistWithTracks> dataSource() {
            return playlistUpdates().startWith(singleton(null))
                                    .switchMap(ignored -> combineLatest(playlist(), tracks(), PlaylistWithTracks::create));
        }

        private Observable<List<Track>> tracks() {
            return trackRepository.forPlaylist(playlistUrn)
                                  .startWith(Collections.<Track>emptyList())
                                  .debounce(100, TimeUnit.MILLISECONDS, scheduler);
        }

        private Observable<Playlist> playlist() {
            return playlistRepository.withUrn(playlistUrn);
        }

        private Observable<PlaylistChangedEvent> playlistUpdates() {
            return eventBus.queue(EventQueue.PLAYLIST_CHANGED)
                           .filter(event -> event.changeMap().containsKey(playlistUrn))
                           .filter(event -> !event.isPlaylistEdited() && (event.isEntityChangeEvent() || event.isTracklistChangeEvent()))
                           .doOnNext(this::storeUpdatedUrn);
        }

        private void storeUpdatedUrn(PlaylistChangedEvent event) {
            if (event.kind() == PlaylistChangedEvent.Kind.PLAYLIST_PUSHED_TO_SERVER) {
                playlistUrn = ((PlaylistEntityChangedEvent) event).changeMap().get(playlistUrn).urn();
            }
        }
    }

    /**
     * TODO :
     * - error states
     * - Upsells
     * - Other playlists by user
     * - missing playlists
     * - offline logic
     * - show confirmation dialog when exiting offline mode
     */
}
