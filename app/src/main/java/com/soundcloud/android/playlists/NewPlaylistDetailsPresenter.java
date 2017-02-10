package com.soundcloud.android.playlists;

import static com.soundcloud.android.events.EventQueue.URN_STATE_CHANGED;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.java.collections.Lists.transform;
import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.of;
import static rx.Observable.combineLatest;
import static rx.Observable.empty;
import static rx.Observable.just;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
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
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.playqueue.PlayQueueHelper;
import com.soundcloud.android.rx.CrashOnTerminateSubscriber;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.transformers.Transformers;
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AutoFactory
class NewPlaylistDetailsPresenter implements PlaylistDetailsInputs {

    private final PlaylistOperations playlistOperations;
    private final PlaylistUpsellOperations playlistUpsellOperations;
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
    private final PublishSubject<Object> refresh = PublishSubject.create();
    private final PublishSubject<Void> playNext = PublishSubject.create();
    private final PublishSubject<Void> delete = PublishSubject.create();
    private final PublishSubject<Void> offlineAvailable = PublishSubject.create();
    private final PublishSubject<Void> offlineUnavailable = PublishSubject.create();
    private final PublishSubject<Void> onCreatorClicked = PublishSubject.create();
    private final PublishSubject<Void> onMakeOfflineUpsell = PublishSubject.create();
    private final PublishSubject<List<PlaylistDetailTrackItem>> tracklistUpdated = PublishSubject.create();
    private final PublishSubject<PlaylistDetailUpsellItem> onUpsellItemClicked = PublishSubject.create();
    private final PublishSubject<PlaylistDetailUpsellItem> onUpsellDismissed = PublishSubject.create();
    private final PublishSubject<Void> headerPlayClicked = PublishSubject.create();
    private final PublishSubject<Boolean> like = PublishSubject.create();
    private final PublishSubject<Boolean> repost = PublishSubject.create();
    private final PublishSubject<PlaylistDetailTrackItem> playFromTrack = PublishSubject.create();

    // outputs
    private final BehaviorSubject<AsyncViewModel<PlaylistDetailsViewModel>> viewModelSubject = BehaviorSubject.create();
    private final PublishSubject<Urn> gotoCreator = PublishSubject.create();
    private final PublishSubject<Object> goBack = PublishSubject.create();
    private final PublishSubject<RepostOperations.RepostResult> showRepostResult = PublishSubject.create();
    private final PublishSubject<LikeOperations.LikeResult> showLikeResult = PublishSubject.create();
    private final PublishSubject<Urn> showPlaylistDeletionConfirmation = PublishSubject.create();
    private final PublishSubject<Urn> goToUpsell = PublishSubject.create();
    private final PublishSubject<PlaybackResult.ErrorReason> playbackError = PublishSubject.create();
    private final BehaviorSubject<PlaylistWithExtras> dataSource;
    private final DataSourceProvider dataSourceProvider;

    NewPlaylistDetailsPresenter(Urn playlistUrn,
                                String screen,
                                @Nullable SearchQuerySourceInfo searchQuerySourceInfo,
                                @Nullable PromotedSourceInfo promotedSourceInfo,
                                @Provided PlaylistUpsellOperations playlistUpsellOperations,
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
                                @Provided DataSourceProviderFactory dataSourceProviderFactory,
                                @Provided RepostOperations repostOperations) {
        this.searchQuerySourceInfo = searchQuerySourceInfo;
        this.promotedSourceInfo = promotedSourceInfo;
        this.screen = screen;
        this.playlistUpsellOperations = playlistUpsellOperations;
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
        dataSourceProvider = dataSourceProviderFactory.create(playlistUrn);

        this.dataSource = dataSourceProvider.data();
    }

    public void connect() {

        this.dataSourceProvider.connect();

        subscription.unsubscribe();
        subscription = new CompositeSubscription(

                actionRefresh(refresh),
                actionGoToCreator(onCreatorClicked),
                actionOnMakeOfflineUpsell(onMakeOfflineUpsell),
                actionGoToUpsellFromTrack(onUpsellItemClicked),
                actionDismissUpsell(onUpsellDismissed),
                actionMakeAvailableOffline(offlineAvailable),
                actionMakeOfflineUnavailableOffline(offlineUnavailable),
                actionPlayNext(playNext),
                actionDeletePlaylist(delete),
                actionLike(like),
                actionRepost(repost),
                actionPlayPlaylist(headerPlayClicked),
                actionPlayPlaylistStartingFromTrack(playFromTrack),
                actionTracklistUpdated(tracklistUpdated),

                onPlaylistDeleted(),

                emitViewModel()
        );
    }

    @Override
    public void onItemTriggered(PlaylistDetailUpsellItem item) {
        onUpsellItemClicked.onNext(item);
    }

    void onItemDismissed(PlaylistDetailUpsellItem item) {
        onUpsellDismissed.onNext(item);
    }

    void fireUpsellImpression() {
        fireAndForget(lastModel()
                              .doOnNext(mode -> eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistTracksImpression(mode.metadata().urn()))));
    }

    private Observable<PlaylistDetailsViewModel> lastModel() {
        return viewModelSubject.flatMap(asyncModel -> asyncModel.data().isPresent() ? just(asyncModel.data().get()) : empty());
    }

    void actionUpdateTrackList(List<PlaylistDetailTrackItem> trackItems) {
        tracklistUpdated.onNext(trackItems);
    }

    private void savePlaylist(PlaylistWithExtras playlistWithExtras) {
        fireAndForget(playlistOperations.editPlaylist(playlistWithExtras.playlist().urn(),
                                               playlistWithExtras.playlist().title(),
                                               playlistWithExtras.playlist().isPrivate(),
                                               transform(playlistWithExtras.tracks().get(), Track::urn)));
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
                .subscribe(new CrashOnTerminateSubscriber<>());
    }

    private Subscription actionPlayPlaylist(PublishSubject<Void> trigger) {
        return dataSource.compose(Transformers.takeWhen(trigger))
                         .map(PlaylistWithExtras -> PlaylistWithExtras.playlist().urn())
                         .flatMap(playlistOperations::trackUrnsForPlayback)
                         .withLatestFrom(playSessionSource(), Pair::of)
                         .flatMap(pair -> playbackInitiator.playTracks(pair.first(), 0, pair.second()))
                         .subscribe(showPlaybackResult());
    }

    private Subscription actionPlayPlaylistStartingFromTrack(PublishSubject<PlaylistDetailTrackItem> trigger) {
        return lastModel()
                .compose(Transformers.takePairWhen(trigger))
                .withLatestFrom(playSessionSource(), (modelWithItem, playSessionSource) -> {
                    final List<PlaylistDetailTrackItem> tracks = modelWithItem.first.tracks().get();
                    final int position = tracks.indexOf(modelWithItem.second);
                    return playTracksFromPosition(position, playSessionSource, transform(tracks, PlaylistDetailTrackItem::getUrn));
                })
                .flatMap(x -> x)
                .subscribe(showPlaybackResult());
    }

    private Subscription actionTracklistUpdated(PublishSubject<List<PlaylistDetailTrackItem>> newTrackItemOrder) {
        return newTrackItemOrder.map(playlistDetailTrackItems -> transform(playlistDetailTrackItems, PlaylistDetailTrackItem::getUrn))
                                .withLatestFrom(dataSource, this::getSortedPlaylistWithExtras)
                                .doOnNext(this::savePlaylist)
                                .subscribe(dataSource);
    }

    private PlaylistWithExtras getSortedPlaylistWithExtras(List<Urn> urns, PlaylistWithExtras playlistWithExtras) {
        ArrayList<Track> sortedList = new ArrayList<>(playlistWithExtras.tracks().get());
        Collections.sort(sortedList, (left, right) -> urns.indexOf(left.urn()) - urns.indexOf(right.urn()));
        return playlistWithExtras.toBuilder().tracks(Optional.of(sortedList)).build();
    }

    private Subscription onPlaylistDeleted() {
        Observable<UrnStateChangedEvent> playlistDeleted = eventBus
                .queue(URN_STATE_CHANGED)
                .filter(event -> event.kind() == UrnStateChangedEvent.Kind.ENTITY_DELETED)
                .filter(UrnStateChangedEvent::containsPlaylist);

        return dataSource
                .map(PlaylistWithExtras -> PlaylistWithExtras.playlist().urn())
                .compose(Transformers.takePairWhen(playlistDeleted))
                .filter(pair -> pair.second.urns().contains(pair.first))
                .subscribe(goBack);
    }

    private Observable<PlaybackResult> playTracksFromPosition(Integer position, PlaySessionSource playSessionSource, List<Urn> tracks) {
        return playbackInitiator.playTracks(
                tracks,
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
                         .map(PlaylistWithExtras -> PlaylistWithExtras.playlist().urn())
                         .withLatestFrom(playSessionSource(), Pair::of)
                         .subscribe(makePlaylistUnavailableOffline());
    }

    private Subscription actionMakeAvailableOffline(PublishSubject<Void> trigger) {
        return dataSource.compose(Transformers.takeWhen(trigger))
                         .map(PlaylistWithExtras -> PlaylistWithExtras.playlist().urn())
                         .withLatestFrom(playSessionSource(), Pair::of)
                         .subscribe(makePlaylistAvailableOffline());
    }

    private Observable<PlaySessionSource> playSessionSource() {
        return dataSource.map(createPlaySessionSource());
    }

    private Subscription actionRefresh(PublishSubject<Object> trigger) {
        return dataSource.compose(Transformers.takeWhen(trigger))
                         .filter(PlaylistWithExtras -> isNotRefreshing())
                         .map(PlaylistWithExtras -> PlaylistWithExtras.playlist().urn())
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
                         public void onError(Throwable e) {
                             super.onError(e);
                             refreshState.onNext(false);
                         }

                         @Override
                         public void onCompleted() {
                             refreshState.onNext(false);
                         }
                     });
    }

    private Subscription actionGoToCreator(PublishSubject<Void> trigger) {
        return dataSource.compose(Transformers.takeWhen(trigger))
                         .map(PlaylistWithExtras -> PlaylistWithExtras.playlist().creatorUrn())
                         .subscribe(gotoCreator);
    }

    private Subscription actionPlayNext(PublishSubject<Void> trigger) {
        return dataSource.compose(Transformers.takeWhen(trigger))
                         .map(PlaylistWithExtras -> PlaylistWithExtras.playlist().urn())
                         .subscribe(playQueueHelper::playNext);
    }

    private Subscription actionDeletePlaylist(PublishSubject<Void> trigger) {
        return dataSource.compose(Transformers.takeWhen(trigger))
                         .map(PlaylistWithExtras -> PlaylistWithExtras.playlist().urn())
                         .subscribe(showPlaylistDeletionConfirmation);
    }

    private Subscription actionGoToUpsellFromTrack(PublishSubject<PlaylistDetailUpsellItem> trigger) {
        return lastModel().compose(Transformers.takeWhen(trigger))
                          .map(playlistWithTracks -> playlistWithTracks.metadata().urn())
                          .doOnNext(urn -> eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistTracksClick(urn)))
                          .subscribe(goToUpsell);
    }

    private Subscription actionOnMakeOfflineUpsell(PublishSubject<Void> trigger) {
        return lastModel().compose(Transformers.takeWhen(trigger))
                          .map(playlistWithTracks -> playlistWithTracks.metadata().urn())
                          .doOnNext(urn -> eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistPageClick(urn)))
                          .subscribe(goToUpsell);
    }

    private Subscription actionDismissUpsell(PublishSubject<PlaylistDetailUpsellItem> trigger) {
        return lastModel()
                .compose(Transformers.takeWhen(trigger))
                .doOnNext(e -> playlistUpsellOperations.disableUpsell())
                .map(ignored -> false)
                .subscribe(refresh);
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

    private Func1<PlaylistWithExtras, PlaySessionSource> createPlaySessionSource() {
        return PlaylistWithExtras -> {
            final Playlist playlist = PlaylistWithExtras.playlist();
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
                                                             PlaylistWithExtras playlistWithExtras,
                                                             LikedStatuses likedStatuses,
                                                             RepostStatuses repostStatuses,
                                                             OfflineProperties offlineProperties) {

        return AsyncViewModel.create(of(viewModelCreator.create(playlistWithExtras.playlist(),
                                                                updateTracks(playlistWithExtras, offlineProperties, isEditMode),
                                                                likedStatuses.isLiked(playlistWithExtras.playlist().urn()),
                                                                repostStatuses.isReposted(playlistWithExtras.playlist().urn()),
                                                                isEditMode,
                                                                isEditMode ? OfflineState.NOT_OFFLINE : offlineProperties.state(playlistWithExtras.playlist().urn()),
                                                                createOtherPlaylistsItem(playlistWithExtras, isEditMode)
        )), isRefreshing, Optional.absent());
    }

    private Optional<PlaylistDetailOtherPlaylistsItem> createOtherPlaylistsItem(PlaylistWithExtras playlistWithExtras, boolean isInEditMode) {
        if (!isInEditMode && playlistWithExtras.otherPlaylistsByCreator().isPresent() && !playlistWithExtras.otherPlaylistsByCreator().get().isEmpty()) {

            List<PlaylistItem> otherPlaylistItems = transform(playlistWithExtras.otherPlaylistsByCreator().get(), PlaylistItem::from);
            String creatorName = playlistWithExtras.playlist().creatorName();
            return of(new PlaylistDetailOtherPlaylistsItem(creatorName, otherPlaylistItems));

        } else {
            return absent();
        }

    }

    private Optional<List<TrackItem>> updateTracks(PlaylistWithExtras playlistWithExtras, OfflineProperties offlineProperties, boolean isInEditMode) {
        if (playlistWithExtras.tracks().isPresent()) {
            List<Track> tracks = playlistWithExtras.tracks().get();
            List<TrackItem> trackItems = new ArrayList<>(tracks.size());
            for (Track track : tracks) {
                // todo, use constructor instead of update
                trackItems.add(TrackItem.from(track).updatedWithOfflineState(isInEditMode ? OfflineState.NOT_OFFLINE : offlineProperties.state(track.urn())));
            }
            return of(trackItems);
        } else {
            return absent();
        }

    }

    void disconnect() {
        dataSourceProvider.disconnect();
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

    PublishSubject<Urn> goToUpsell() {
        return goToUpsell;
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
    public void onItemTriggered(PlaylistDetailTrackItem item) {
        playFromTrack.onNext(item);
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
    public void onMakeOfflineUpsell() {
        onMakeOfflineUpsell.onNext(null);
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

    private Observable<LikeOperations.LikeResult> like(android.util.Pair<PlaylistWithExtras, Boolean> PlaylistWithExtrasBooleanPair, PlaySessionSource playSessionSource) {
        Playlist playlist = PlaylistWithExtrasBooleanPair.first.playlist();
        final Boolean isLike = PlaylistWithExtrasBooleanPair.second;
        eventTracker.trackEngagement(UIEvent.fromToggleLike(isLike,
                                                            playlist.urn(),
                                                            getEventContext(playlist.urn()),
                                                            playSessionSource.getPromotedSourceInfo(),
                                                            createEntityMetadata(playlist)));

        return likeOperations.toggleLike(playlist.urn(), isLike);
    }

    private Observable<RepostOperations.RepostResult> repost(android.util.Pair<PlaylistWithExtras, Boolean> PlaylistWithExtrasBooleanPair, PlaySessionSource playSessionSource) {
        final Playlist playlist = PlaylistWithExtrasBooleanPair.first.playlist();
        final Boolean isReposted = PlaylistWithExtrasBooleanPair.second;

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

    /**
     * TODO :
     * - error states
     * - Upsells
     * - missing playlists
     * - show confirmation dialog when exiting offline mode
     */
}
