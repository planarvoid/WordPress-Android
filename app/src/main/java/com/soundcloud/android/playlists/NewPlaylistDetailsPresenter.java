package com.soundcloud.android.playlists;

import static com.soundcloud.android.events.EventQueue.URN_STATE_CHANGED;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;
import static com.soundcloud.java.collections.Lists.transform;
import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.of;
import static rx.Observable.combineLatest;
import static rx.Observable.empty;
import static rx.Observable.just;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.associations.RepostStatuses;
import com.soundcloud.android.associations.RepostsStateProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.feedback.Feedback;
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
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.playqueue.PlayQueueHelper;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.rx.CrashOnTerminateSubscriber;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.settings.OfflineStorageErrorDialog;
import com.soundcloud.android.share.SharePresenter;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.transformers.Transformers;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

import android.content.Context;
import android.support.annotation.Nullable;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AutoFactory
public class NewPlaylistDetailsPresenter implements PlaylistDetailsInputs {

    private final PlaylistOperations playlistOperations;
    private final PlaylistUpsellOperations playlistUpsellOperations;
    private final PlaybackInitiator playbackInitiator;
    private final LikesStateProvider likesStateProvider;
    private final RepostsStateProvider repostsStateProvider;
    private final PlayQueueHelper playQueueHelper;
    private final OfflinePropertiesProvider offlinePropertiesProvider;
    private final EventBus eventBus;
    private final OfflineContentOperations offlineContentOperations;
    private final EventTracker eventTracker;
    private final LikeOperations likeOperations;
    private final RepostOperations repostOperations;
    private final FeedbackController feedbackController;
    private final AccountOperations accountOperations;
    private final SearchQuerySourceInfo searchQuerySourceInfo;
    private final PromotedSourceInfo promotedSourceInfo;
    private final String screen;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final OfflineSettingsStorage offlineSettingsStorage;

    private final EntityItemCreator entityItemCreator;
    private final PlaylistDetailsViewModelCreator viewModelCreator;

    private Subscription subscription = RxUtils.invalidSubscription();

    // state
    private final BehaviorSubject<Boolean> editMode = BehaviorSubject.create(false);
    private final BehaviorSubject<PlaylistWithExtras> undoTracklistState = BehaviorSubject.create();

    // inputs
    private final PublishSubject<Object> refresh = PublishSubject.create();
    private final PublishSubject<Void> playNext = PublishSubject.create();
    private final PublishSubject<Void> delete = PublishSubject.create();
    private final PublishSubject<Void> share = PublishSubject.create();
    private final PublishSubject<Void> offlineAvailable = PublishSubject.create();
    private final PublishSubject<Void> offlineUnavailable = PublishSubject.create();
    private final PublishSubject<Void> onCreatorClicked = PublishSubject.create();
    private final PublishSubject<Void> onMakeOfflineUpsell = PublishSubject.create();
    private final PublishSubject<Void> onOverflowMakeOfflineUpsell = PublishSubject.create();
    private final PublishSubject<List<PlaylistDetailTrackItem>> tracklistUpdated = PublishSubject.create();
    private final PublishSubject<Void> undoTrackRemoval = PublishSubject.create();
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
    private final PublishSubject<Pair<Urn, PlaySessionSource>> showDisableOfflineCollectionConfirmation = PublishSubject.create();
    private final PublishSubject<SharePresenter.ShareOptions> sharePlaylist = PublishSubject.create();
    private final PublishSubject<Urn> goToUpsell = PublishSubject.create();
    private final PublishSubject<PlaybackResult.ErrorReason> playbackError = PublishSubject.create();
    private final BehaviorSubject<PlaylistWithExtrasState> dataSource;
    private final DataSourceProvider dataSourceProvider;
    private final FeatureOperations featureOperations;

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
                                @Provided EventBus eventBus,
                                @Provided OfflineContentOperations offlineContentOperations,
                                @Provided EventTracker eventTracker,
                                @Provided LikeOperations likeOperations,
                                @Provided PlaylistDetailsViewModelCreator viewModelCreator,
                                @Provided DataSourceProviderFactory dataSourceProviderFactory,
                                @Provided RepostOperations repostOperations,
                                @Provided FeedbackController feedbackController,
                                @Provided AccountOperations accountOperations,
                                @Provided Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                                @Provided EntityItemCreator entityItemCreator,
                                @Provided FeatureOperations featureOperations,
                                @Provided OfflineSettingsStorage offlineSettingsStorage) {
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
        this.eventBus = eventBus;
        this.offlineContentOperations = offlineContentOperations;
        this.eventTracker = eventTracker;
        this.likeOperations = likeOperations;
        this.viewModelCreator = viewModelCreator;
        this.repostOperations = repostOperations;
        this.feedbackController = feedbackController;
        this.accountOperations = accountOperations;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.entityItemCreator = entityItemCreator;
        this.featureOperations = featureOperations;
        this.offlineSettingsStorage = offlineSettingsStorage;

        // Note: do NOT store this urn. Always get it from DataSource, as it may change when a local playlist is pushed
        dataSourceProvider = dataSourceProviderFactory.create(playlistUrn, refresh);

        this.dataSource = dataSourceProvider.data();
    }

    public void connect() {

        this.dataSourceProvider.connect();

        subscription.unsubscribe();
        subscription = new CompositeSubscription(

                actionGoToCreator(onCreatorClicked),
                actionOnMakeOfflineUpsell(onMakeOfflineUpsell),
                actionOnOverflowMakeOfflineUpsell(onOverflowMakeOfflineUpsell),
                actionGoToUpsellFromTrack(onUpsellItemClicked),
                actionDismissUpsell(onUpsellDismissed),
                actionMakeAvailableOffline(offlineAvailable),
                actionMakeOfflineUnavailableOffline(offlineUnavailable),
                actionPlayNext(playNext),
                actionDeletePlaylist(delete),
                actionSharePlaylist(share),
                actionLike(like),
                actionRepost(repost),
                actionPlayPlaylist(headerPlayClicked),
                actionPlayPlaylistStartingFromTrack(playFromTrack),
                actionTracklistUpdated(tracklistUpdated),
                actionTrackRemovalUndo(undoTrackRemoval),

                onPlaylistDeleted(),

                emitViewModel()
        );

        if (featureOperations.upsellOfflineContent()) {
            firePlaylistPageUpsellImpression();
        }
    }

    private void firePlaylistPageUpsellImpression() {
        fireAndForget(
                viewModel()
                        .filter(model -> model.data().isPresent())
                        .first()
                        .doOnNext(model -> {
                            final Urn playlistUrn = model.data().get().metadata().urn();
                            final UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlaylistPageImpression(playlistUrn);
                            eventBus.publish(EventQueue.TRACKING, event);
                        })
        );
    }

    @Override
    public void onItemTriggered(PlaylistDetailUpsellItem item) {
        onUpsellItemClicked.onNext(item);
    }

    void onItemDismissed(PlaylistDetailUpsellItem item) {
        onUpsellDismissed.onNext(item);
    }

    void firePlaylistTracksUpsellImpression() {
        fireAndForget(lastModel().doOnNext(mode -> eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistTracksImpression(mode.metadata().urn()))));
    }

    private Observable<PlaylistDetailsViewModel> lastModel() {
        return models().first();
    }

    private Observable<PlaylistDetailsViewModel> models() {
        return viewModelSubject
                .flatMap(asyncModel -> asyncModel.data().isPresent() ? just(asyncModel.data().get()) : empty());
    }

    void actionUpdateTrackList(List<PlaylistDetailTrackItem> trackItems) {
        tracklistUpdated.onNext(trackItems);
    }

    void actionUpdateTrackListWithUndo(List<PlaylistDetailTrackItem> updatedTrackItems) {
        undoTracklistState.onNext(dataSource.getValue().playlistWithExtras().get());
        tracklistUpdated.onNext(updatedTrackItems);
        feedbackController.showFeedback(Feedback.create(R.string.track_removed, R.string.undo, view -> undoTrackRemoval.onNext(null)));
    }

    private void savePlaylist(PlaylistWithExtras playlistWithExtras) {
        fireAndForget(playlistOperations.editPlaylist(playlistWithExtras.playlist().urn(),
                                                      playlistWithExtras.playlist().title(),
                                                      playlistWithExtras.playlist().isPrivate(),
                                                      transform(playlistWithExtras.tracks().get(), Track::urn)));
    }

    private Subscription emitViewModel() {
        return combineLatest(
                currentTrackPlaying(),
                editMode,
                dataSource.doOnNext(this::showRefreshErrorIfPresent),
                likesStateProvider.likedStatuses(),
                repostsStateProvider.repostedStatuses(),
                offlinePropertiesProvider.states(),
                this::combine)
                .distinctUntilChanged()
                .doOnNext(viewModelSubject::onNext)
                .subscribe(new CrashOnTerminateSubscriber<>());
    }

    private Observable<Urn> currentTrackPlaying() {
        return eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                       .filter(event -> event.getCurrentPlayQueueItem() != PlayQueueItem.EMPTY)
                       .map(item -> item.getCurrentPlayQueueItem().getUrn()).startWith(Urn.NOT_SET);
    }

    private void showRefreshErrorIfPresent(PlaylistWithExtrasState playlistWithExtrasState) {
        if (playlistWithExtrasState.refreshError().isPresent()) {
            feedbackController.showFeedback(Feedback.create(ErrorUtils.emptyMessageFromViewError(playlistWithExtrasState.refreshError().get())));
        }
    }

    private Observable<PlaylistWithExtras> playlistWithExtras() {
        return dataSource.filter(playlistWithExtrasState -> playlistWithExtrasState.playlistWithExtras().isPresent())
                         .map(playlistWithExtrasState -> playlistWithExtrasState.playlistWithExtras().get());
    }

    private Subscription actionPlayPlaylist(PublishSubject<Void> trigger) {
        return playlistWithExtras().compose(Transformers.takeWhen(trigger))
                                   .map(PlaylistWithExtras -> PlaylistWithExtras.playlist().urn())
                                   .flatMap(playlistOperations::trackUrnsForPlayback)
                                   .withLatestFrom(playSessionSource(), Pair::of)
                                   .flatMap(pair -> playbackInitiator.playTracks(pair.first(), 0, pair.second()))
                                   .doOnNext(this::sendErrorIfUnsuccessful)
                                   .subscribe(expandPlayerSubscriberProvider.get());
    }

    private Subscription actionPlayPlaylistStartingFromTrack(PublishSubject<PlaylistDetailTrackItem> trigger) {
        return models()
                .compose(Transformers.takePairWhen(trigger))
                .withLatestFrom(playSessionSource(), (modelWithItem, playSessionSource) -> {
                    final List<PlaylistDetailTrackItem> tracks = modelWithItem.first.tracks();
                    final int position = tracks.indexOf(modelWithItem.second);
                    return playTracksFromPosition(position, playSessionSource, transform(tracks, PlaylistDetailTrackItem::getUrn));
                })
                .flatMap(x -> x)
                .doOnNext(this::sendErrorIfUnsuccessful)
                .subscribe(expandPlayerSubscriberProvider.get());
    }

    private void sendErrorIfUnsuccessful(PlaybackResult playbackResult) {
        if (!playbackResult.isSuccess()) {
            playbackError.onNext(playbackResult.getErrorReason());
        }
    }

    private Subscription actionTracklistUpdated(PublishSubject<List<PlaylistDetailTrackItem>> newTrackItemOrder) {
        return newTrackItemOrder.map(playlistDetailTrackItems -> transform(playlistDetailTrackItems, PlaylistDetailTrackItem::getUrn))
                                .withLatestFrom(playlistWithExtras(), this::getSortedPlaylistWithExtras)
                                .doOnNext(this::savePlaylist)
                                .map(playlistWithExtras -> PlaylistWithExtrasState.builder().playlistWithExtras(of(playlistWithExtras)).build())
                                .subscribe(dataSource);
    }

    private Subscription actionTrackRemovalUndo(PublishSubject<Void> undoTracklistRemoval) {
        return undoTracklistState.compose(Transformers.takeWhen(undoTracklistRemoval))
                                 .doOnNext(this::savePlaylist)
                                 .map(playlistWithExtras -> dataSource.getValue().toBuilder().playlistWithExtras(of(playlistWithExtras)).build())
                                 .subscribe(dataSource);

    }

    private PlaylistWithExtras getSortedPlaylistWithExtras(List<Urn> urns, PlaylistWithExtras playlistWithExtras) {
        ArrayList<Track> sortedList = new ArrayList<>();
        for (Track track : playlistWithExtras.tracks().get()) {
            if (urns.contains(track.urn())) {
                sortedList.add(track);
            }
        }
        Collections.sort(sortedList, (left, right) -> urns.indexOf(left.urn()) - urns.indexOf(right.urn()));
        return playlistWithExtras.toBuilder().tracks(of(sortedList)).build();
    }

    private Subscription onPlaylistDeleted() {
        Observable<UrnStateChangedEvent> playlistDeleted = eventBus
                .queue(URN_STATE_CHANGED)
                .filter(event -> event.kind() == UrnStateChangedEvent.Kind.ENTITY_DELETED)
                .filter(UrnStateChangedEvent::containsPlaylist);

        return playlistWithExtras()
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
        return playlistWithExtras().compose(Transformers.takePairWhen(trigger))
                                   .withLatestFrom(playSessionSource(), this::like)
                                   .flatMap(x -> x)
                                   .subscribe(showLikeResult);
    }

    private Subscription actionRepost(PublishSubject<Boolean> trigger) {
        return playlistWithExtras().compose(Transformers.takePairWhen(trigger))
                                   .withLatestFrom(playSessionSource(), this::repost)
                                   .flatMap(x -> x)
                                   .subscribe(showRepostResult);
    }

    private Subscription actionMakeOfflineUnavailableOffline(PublishSubject<Void> trigger) {
        return playlistWithExtras().compose(Transformers.takeWhen(trigger))
                                   .map(PlaylistWithExtras -> PlaylistWithExtras.playlist().urn())
                                   .withLatestFrom(playSessionSource(), Pair::of)
                                   .subscribe(data -> {
                                             if (offlineContentOperations.isOfflineCollectionEnabled()) {
                                                 showDisableOfflineCollectionConfirmation.onNext(data);
                                             } else {
                                                 makePlaylistUnAvailableOffline(data);
                                             }
                                         });
    }

    private Subscription actionMakeAvailableOffline(PublishSubject<Void> trigger) {
        return viewModelSubject.compose(Transformers.takeWhen(trigger))
                               .withLatestFrom(playSessionSource(), Pair::of)
                               .flatMap(this::makePlaylistAvailableOffline)
                               .doOnNext(viewModelSubject::onNext)
                               .subscribe(new DefaultSubscriber<>());
    }

    private Observable<AsyncViewModel<PlaylistDetailsViewModel>> makePlaylistAvailableOffline(Pair<AsyncViewModel<PlaylistDetailsViewModel>, PlaySessionSource> pair) {
        final PlaylistDetailsViewModel model = pair.first().data().get();
        final Urn playlistUrn = model.metadata().urn();
        final Urn creatorUrn = model.metadata().creatorUrn();
        final PlaySessionSource playSessionSource = pair.second();

        final Observable<Void> operation = offlineContentOperations.makePlaylistAvailableOffline(playlistUrn)
                                                                   .doOnNext(aVoid -> {
                                                                       final OfflineInteractionEvent event = getOfflinePlaylistTrackingEvent(playlistUrn, true, playSessionSource);
                                                                       eventBus.publish(EventQueue.TRACKING, event);
                                                                   });
        if (isInUserCollection(playlistUrn, creatorUrn)) {
            return operation.flatMap(ignored -> submitUpdateViewModel(true));
        } else {
            return likeOperations.toggleLike(playlistUrn, true).flatMap(whenLikeSucceeded(operation));
        }
    }

    private Func1<LikeOperations.LikeResult, Observable<? extends AsyncViewModel<PlaylistDetailsViewModel>>> whenLikeSucceeded(Observable<Void> action) {
        return likeResult -> likeResult == LikeOperations.LikeResult.LIKE_SUCCEEDED ?
                             action.flatMap(ignored -> submitUpdateViewModel(true)) :
                             submitUpdateViewModel(false);
    }

    private Observable<AsyncViewModel<PlaylistDetailsViewModel>> submitUpdateViewModel(boolean isMarkedForOffline) {
        return viewModelSubject
                .first()
                .map(model -> model.withNewData(model.data().get().updateWithMarkedForOffline(isMarkedForOffline)));
    }

    private boolean isInUserCollection(Urn playlistUrn, Urn creatorUrn) {
        return likesStateProvider.latest().isLiked(playlistUrn) || accountOperations.isLoggedInUser(creatorUrn);
    }

    private Observable<PlaySessionSource> playSessionSource() {
        return playlistWithExtras().map(createPlaySessionSource());
    }

    private Subscription actionGoToCreator(PublishSubject<Void> trigger) {
        return playlistWithExtras().compose(Transformers.takeWhen(trigger))
                                   .map(PlaylistWithExtras -> PlaylistWithExtras.playlist().creatorUrn())
                                   .subscribe(gotoCreator);
    }

    private Subscription actionPlayNext(PublishSubject<Void> trigger) {
        return playlistWithExtras().compose(Transformers.takeWhen(trigger))
                                   .map(PlaylistWithExtras -> PlaylistWithExtras.playlist().urn())
                                   .subscribe(playQueueHelper::playNext);
    }

    private Subscription actionDeletePlaylist(PublishSubject<Void> trigger) {
        return playlistWithExtras().compose(Transformers.takeWhen(trigger))
                                   .map(PlaylistWithExtras -> PlaylistWithExtras.playlist().urn())
                                   .subscribe(showPlaylistDeletionConfirmation);
    }

    private Subscription actionSharePlaylist(PublishSubject<Void> trigger) {
        return playlistWithExtras().compose(Transformers.takeWhen(trigger))
                                   .filter(playlistWithExtras -> playlistWithExtras.playlist().permalinkUrl().isPresent())
                                   .withLatestFrom(playSessionSource(), Pair::of)
                                   .map(pair -> {
                                             Playlist playlist = pair.first().playlist();
                                             return SharePresenter.ShareOptions.create(
                                                     playlist.permalinkUrl().get(),
                                                     getEventContext(playlist.urn()),
                                                     pair.second().getPromotedSourceInfo(),
                                                     createEntityMetadata(playlist)
                                             );
                                         })
                                   .subscribe(sharePlaylist);

    }

    private Subscription actionGoToUpsellFromTrack(PublishSubject<PlaylistDetailUpsellItem> trigger) {
        return models().compose(Transformers.takeWhen(trigger))
                       .map(playlistWithTracks -> playlistWithTracks.metadata().urn())
                       .doOnNext(urn -> eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistTracksClick(urn)))
                       .subscribe(goToUpsell);
    }

    private Subscription actionOnMakeOfflineUpsell(PublishSubject<Void> trigger) {
        return models().compose(Transformers.takeWhen(trigger))
                       .map(playlistWithTracks -> playlistWithTracks.metadata().urn())
                       .doOnNext(urn -> eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistPageClick(urn)))
                       .subscribe(goToUpsell);
    }

    private Subscription actionOnOverflowMakeOfflineUpsell(PublishSubject<Void> trigger) {
        return models().compose(Transformers.takeWhen(trigger))
                       .map(playlistWithTracks -> playlistWithTracks.metadata().urn())
                       .doOnNext(urn -> eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistOverflowClick(urn)))
                       .subscribe(goToUpsell);
    }

    private Subscription actionDismissUpsell(PublishSubject<PlaylistDetailUpsellItem> trigger) {
        return trigger
                .doOnNext(e -> playlistUpsellOperations.disableUpsell())
                .flatMap(ignored -> dataSource.first())
                .subscribe(dataSource);
    }

    private void makePlaylistUnAvailableOffline(Pair<Urn, PlaySessionSource> urnSourcePair) {
        fireAndForget(offlineContentOperations.makePlaylistUnavailableOffline(urnSourcePair.first()));
        eventBus.publish(EventQueue.TRACKING, getOfflinePlaylistTrackingEvent(urnSourcePair.first(), false, urnSourcePair.second()));
    }

    private EntityMetadata createEntityMetadata(Playlist playlist) {
        return EntityMetadata.from(playlist.creatorName(), playlist.creatorUrn(),
                                   playlist.title(), playlist.urn());
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

    private AsyncViewModel<PlaylistDetailsViewModel> combine(Urn currentTrackPlaying,
                                                             Boolean isEditMode,
                                                             PlaylistWithExtrasState playlistWithExtrasState,
                                                             LikedStatuses likedStatuses,
                                                             RepostStatuses repostStatuses,
                                                             OfflineProperties offlineProperties) {
        /**
         * This is a mess. Clean this up when we get rid of the old fragment, and can properly build the model in one place
         */
        Optional<ViewError> viewError = playlistWithExtrasState.viewError();
        if (playlistWithExtrasState.playlistWithExtras().isPresent()) {
            PlaylistWithExtras playlistWithExtras = playlistWithExtrasState.playlistWithExtras().get();
            Playlist playlist = playlistWithExtras.playlist();
            Urn urn = playlist.urn();
            return AsyncViewModel.create(of(viewModelCreator.create(playlist,
                                                                    updateTracks(currentTrackPlaying, playlistWithExtras, offlineProperties, isEditMode),
                                                                    likedStatuses.isLiked(urn),
                                                                    repostStatuses.isReposted(urn),
                                                                    isEditMode,
                                                                    isEditMode ? OfflineState.NOT_OFFLINE : offlineProperties.state(urn),
                                                                    createOtherPlaylistsItem(playlistWithExtras, isEditMode)
            )), !playlistWithExtras.tracks().isPresent(), playlistWithExtrasState.isRefreshing(), viewError);
        } else {
            if (viewError.isPresent()) {
                return AsyncViewModel.create(absent(), false, playlistWithExtrasState.isRefreshing(), viewError);
            } else {
                return AsyncViewModel.create(absent(), true, playlistWithExtrasState.isRefreshing(), viewError);
            }
        }

    }

    private Optional<PlaylistDetailOtherPlaylistsItem> createOtherPlaylistsItem(PlaylistWithExtras playlistWithExtras, boolean isInEditMode) {
        if (!isInEditMode && !playlistWithExtras.otherPlaylistsByCreator().isEmpty()) {

            List<PlaylistItem> otherPlaylistItems = transform(playlistWithExtras.otherPlaylistsByCreator(), entityItemCreator::playlistItem);
            String creatorName = playlistWithExtras.playlist().creatorName();
            return of(new PlaylistDetailOtherPlaylistsItem(creatorName, otherPlaylistItems, playlistWithExtras.playlist().isAlbum()));

        } else {
            return absent();
        }

    }

    private List<TrackItem> updateTracks(Urn currentTrackPlaying, PlaylistWithExtras playlistWithExtras, OfflineProperties offlineProperties, boolean isInEditMode) {
        Optional<List<Track>> tracksOpt = playlistWithExtras.tracks();
        if (tracksOpt.isPresent()) {
            List<Track> tracks = tracksOpt.get();
            List<TrackItem> trackItems = new ArrayList<>(tracks.size());
            for (Track track : tracks) {
                final OfflineState offlineState = isInEditMode ? OfflineState.NOT_OFFLINE : offlineProperties.state(track.urn());
                final TrackItem trackItem = entityItemCreator.trackItem(track).toBuilder().offlineState(offlineState).isPlaying(track.urn().equals(currentTrackPlaying)).build();
                trackItems.add(trackItem);
            }
            return trackItems;
        } else {
            return Collections.emptyList();
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

    PublishSubject<Pair<Urn, PlaySessionSource>> onShowDisableOfflineCollectionConfirmation() {
        return showDisableOfflineCollectionConfirmation;
    }

    Observable<SharePresenter.ShareOptions> onShare() {
        return sharePlaylist;
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
    public void onMakeOfflineAvailable(Context context) {
        if (offlineSettingsStorage.isOfflineContentAccessible()) {
            offlineAvailable.onNext(null);
        } else {
            OfflineStorageErrorDialog.show(getFragmentActivity(context).getSupportFragmentManager());
        }
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
    public void onShareClicked() {
        share.onNext(null);
    }

    @Override
    public void onOverflowUpsell() {
        onOverflowMakeOfflineUpsell.onNext(null);
    }

    @Override
    public void onOverflowUpsellImpression() {
        fireAndForget(lastModel().doOnNext(model -> eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistOverflowImpression(model.metadata().getUrn()))));
    }

    @Override
    public void onPlayShuffled() {
        lastModel()
                .withLatestFrom(playSessionSource(), Pair::of)
                .flatMap(pair -> {
                    PlaylistDetailsViewModel viewModel = pair.first();
                    return playbackInitiator
                            .playTracksShuffled(just(transform(viewModel.tracks(), PlaylistDetailTrackItem::getUrn)), pair.second())
                            .doOnCompleted(() -> eventBus.publish(EventQueue.TRACKING, UIEvent.fromShuffle(getEventContext(viewModel.metadata().urn()))));
                }).doOnNext(this::sendErrorIfUnsuccessful)
                .subscribe(expandPlayerSubscriberProvider.get());
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
     * - Upsells
     * - show confirmation dialog when exiting offline mode
     * - artwork keeps changing
     */
}
