package com.soundcloud.android.playlists;

import static com.soundcloud.android.events.EventQueue.URN_STATE_CHANGED;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.java.collections.Lists.transform;
import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.of;
import static java.util.Collections.emptyList;
import static rx.Observable.empty;
import static rx.Observable.just;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
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
import com.soundcloud.android.likes.LikeOperations;
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
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.share.SharePresenter;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.transformers.Transformers;
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import io.reactivex.Single;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

import android.content.res.Resources;
import android.support.annotation.Nullable;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

@AutoFactory
public class PlaylistDetailsPresenter {

    interface ActionResult {
        AsyncViewModel<PlaylistDetailsViewModel> apply(AsyncViewModel<PlaylistDetailsViewModel> previous);
    }

    private final Resources resources;
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
    private final AccountOperations accountOperations;
    private final SearchQuerySourceInfo searchQuerySourceInfo;
    private final PromotedSourceInfo promotedSourceInfo;
    private final String screen;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final OfflineSettingsStorage offlineSettingsStorage;

    private final EntityItemCreator entityItemCreator;

    private Subscription subscription = RxUtils.invalidSubscription();

    // outputs
    private final BehaviorSubject<AsyncViewModel<PlaylistDetailsViewModel>> viewModelSubject = BehaviorSubject.create();
    private final PublishSubject<Urn> gotoCreator = PublishSubject.create();
    private final PublishSubject<Object> goBack = PublishSubject.create();
    private final PublishSubject<RepostOperations.RepostResult> showRepostResult = PublishSubject.create();
    private final PublishSubject<LikeOperations.LikeResult> showLikeResult = PublishSubject.create();
    private final PublishSubject<Urn> showPlaylistDeletionConfirmation = PublishSubject.create();
    private final PublishSubject<Pair<Urn, PlaySessionSource>> showDisableOfflineCollectionConfirmation = PublishSubject.create();
    private final PublishSubject<Void> showOfflineStorageErrorDialog = PublishSubject.create();
    private final PublishSubject<SharePresenter.ShareOptions> sharePlaylist = PublishSubject.create();
    private final PublishSubject<Urn> goToContentUpsell = PublishSubject.create();
    private final PublishSubject<Urn> goToOfflineUpsell = PublishSubject.create();
    private final PublishSubject<PlaybackResult.ErrorReason> playbackError = PublishSubject.create();
    private final PublishSubject<ViewError> refreshError = PublishSubject.create();
    private final DataSourceProvider dataSourceProvider;
    private final FeatureOperations featureOperations;

    PlaylistDetailsPresenter(String screen,
                             @Nullable SearchQuerySourceInfo searchQuerySourceInfo,
                             @Nullable PromotedSourceInfo promotedSourceInfo,
                             @Provided Resources resources,
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
                             @Provided DataSourceProvider dataSourceProvider,
                             @Provided RepostOperations repostOperations,
                             @Provided AccountOperations accountOperations,
                             @Provided Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                             @Provided EntityItemCreator entityItemCreator,
                             @Provided FeatureOperations featureOperations,
                             @Provided OfflineSettingsStorage offlineSettingsStorage) {
        this.resources = resources;
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
        this.repostOperations = repostOperations;
        this.accountOperations = accountOperations;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.entityItemCreator = entityItemCreator;
        this.featureOperations = featureOperations;
        this.offlineSettingsStorage = offlineSettingsStorage;
        this.dataSourceProvider = dataSourceProvider;
    }

    public void connect(PlaylistDetailsInputs inputs, Urn playlistUrn) {
        subscription.unsubscribe();
        subscription = new CompositeSubscription(
                subscribeToCommands(inputs),
                emitViewModel(inputs, playlistUrn).subscribe(new CrashOnTerminateSubscriber<>())
        );

        if (featureOperations.upsellOfflineContent()) {
            firePlaylistPageUpsellImpression();
        }
    }

    private Observable<AsyncViewModel<PlaylistDetailsViewModel>> emitViewModel(PlaylistDetailsInputs inputs, Urn playlistUrn) {
        return PlaylistWithExtrasStateIntent.dataSource(inputs, playlistUrn, dataSourceProvider, resources, featureOperations, playlistUpsellOperations, entityItemCreator)
                                            .flatMap(data -> actions(inputs, data))
                                            .scan(AsyncViewModel.initial(), this::toViewModel)
                                            .distinctUntilChanged()
                                            .doOnNext(this::showRefreshErrorIfPresent)
                                            .doOnNext(viewModelSubject::onNext);
    }

    private Observable<? extends ActionResult> actions(PlaylistDetailsInputs inputs, PlaylistWithExtrasStateIntent.PlaylistWithExtrasStateResult data) {
        if (data.playlistWithExtrasState.playlistWithExtras().isPresent()) {
            final Urn urn = data.playlistWithExtrasState.playlistWithExtras().get().playlist().urn();
            return Observable
                    .merge(
                            UpdateTrackListIntent.toResult(resources, inputs, urn, playlistOperations),
                            DismissUpsellIntent.toResult(inputs.onUpsellDismissed, playlistUpsellOperations),
                            NowPlayingIntent.toResult(currentTrackPlaying()),
                            EditModeChangedIntent.toResult(inputs.editMode, offlinePropertiesProvider.states()),
                            LikeStateChangedIntent.toResult(urn, likesStateProvider),
                            PlaylistRepostedIntent.toResult(urn, RxJava.toV1Observable(repostsStateProvider.repostedStatuses())),
                            OfflineStateChangedIntent.toResult(offlinePropertiesProvider.states(), inputs.editMode)
                    )
                    .startWith(data);
        } else {
            return just(data);
        }
    }

    private AsyncViewModel<PlaylistDetailsViewModel> toViewModel(AsyncViewModel<PlaylistDetailsViewModel> previous, ActionResult result) {
        return result.apply(previous);
    }

    private Subscription subscribeToCommands(PlaylistDetailsInputs inputs) {

        return new CompositeSubscription(
                // -> Show playlist deletion confirmation
                actionDeletePlaylist(inputs.delete).subscribe(showPlaylistDeletionConfirmation),
                // -> Share playlist
                actionSharePlaylist(inputs.share).subscribe(sharePlaylist),
                // -> Show like toResult
                actionLike(inputs.like).subscribe(showLikeResult),
                // -> Show Repost toResult
                actionRepost(inputs.repost).subscribe(showRepostResult),
                // -> Go back
                onPlaylistDeleted().subscribe(goBack),
                // -> Go to creator
                actionGoToCreator(inputs.onCreatorClicked).subscribe(gotoCreator),
                // -> Go to upsell
                actionOnMakeOfflineUpsell(inputs.onMakeOfflineUpsell).subscribe(goToOfflineUpsell),
                actionOnOverflowMakeOfflineUpsell(inputs.onOverflowMakeOfflineUpsell).subscribe(goToOfflineUpsell),
                actionGoToUpsellFromTrack(inputs.onUpsellItemClicked).subscribe(goToContentUpsell),

                // -> Start playback
                actionPlayNext(inputs.playNext).subscribe(playQueueHelper::playNext),
                actionPlayPlaylistStartingFromTrack(inputs.playFromTrack).subscribe(expandPlayerSubscriberProvider.get()),
                actionPlayShuffled(inputs.playShuffled).subscribe(expandPlayerSubscriberProvider.get()),
                actionPlayPlaylist(inputs.headerPlayClicked).subscribe(expandPlayerSubscriberProvider.get()),

                // -> Offline operations
                actionMakeAvailableOffline(inputs.makeOfflineAvailable).subscribe(),
                actionMakeOfflineUnavailableOffline(inputs.offlineUnavailable).subscribe(),
                onOverflowUpsellImpression(inputs.overflowUpsellImpression).subscribe()
        );
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

    private Observable<Urn> currentTrackPlaying() {
        return eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                       .filter(event -> event.getCurrentPlayQueueItem() != PlayQueueItem.EMPTY)
                       .map(item -> item.getCurrentPlayQueueItem().getUrn()).startWith(Urn.NOT_SET);
    }

    private void showRefreshErrorIfPresent(AsyncViewModel<PlaylistDetailsViewModel> playlistWithExtrasState) {
        if (playlistWithExtrasState.refreshError().isPresent()) {
            final ViewError viewError = playlistWithExtrasState.refreshError().get();
            refreshError.onNext(viewError);
        }
    }

    private Observable<PlaylistDetailsViewModel> model() {
        return viewModelSubject.filter(model -> model.data().isPresent()).map(model -> model.data().get());
    }

    private Observable<PlaybackResult> actionPlayPlaylist(PublishSubject<Void> trigger) {
        return model().compose(Transformers.takeWhen(trigger))
                      .map(model -> model.metadata().urn())
                      .flatMap(playlistOperations::trackUrnsForPlayback)
                      .withLatestFrom(playSessionSource(), Pair::of)
                      .flatMap(pair -> RxJava.toV1Observable(playbackInitiator.playTracks(pair.first(), 0, pair.second())))
                      .doOnNext(this::sendErrorIfUnsuccessful);
    }

    private Observable<PlaybackResult> actionPlayPlaylistStartingFromTrack(PublishSubject<PlaylistDetailTrackItem> trigger) {
        return models()
                .compose(Transformers.takePairWhen(trigger))
                .withLatestFrom(playSessionSource(), (modelWithItem, playSessionSource) -> {
                    final Urn trackToPlay = modelWithItem.second.getUrn();
                    final List<Urn> tracksList = transform(modelWithItem.first.tracks(), PlaylistDetailTrackItem::getUrn);
                    final int position = tracksList.indexOf(trackToPlay);
                    // Here for investigation
                    if (position == -1) {
                        throw new IllegalStateException(String.format("Could not find track %s in %s", trackToPlay, tracksList));
                    }
                    //
                    return playTracksFromPosition(position, playSessionSource, tracksList);
                })
                .flatMap(x -> x)
                .doOnNext(this::sendErrorIfUnsuccessful);
    }

    private void sendErrorIfUnsuccessful(PlaybackResult playbackResult) {
        if (!playbackResult.isSuccess()) {
            playbackError.onNext(playbackResult.getErrorReason());
        }
    }

    private Observable<android.util.Pair<Urn, UrnStateChangedEvent>> onPlaylistDeleted() {
        Observable<UrnStateChangedEvent> playlistDeleted = eventBus
                .queue(URN_STATE_CHANGED)
                .filter(event -> event.kind() == UrnStateChangedEvent.Kind.ENTITY_DELETED)
                .filter(UrnStateChangedEvent::containsPlaylist);

        return model()
                .map(model -> model.metadata().urn())
                .compose(Transformers.takePairWhen(playlistDeleted))
                .filter(pair -> pair.second.urns().contains(pair.first));
    }

    private Observable<PlaybackResult> playTracksFromPosition(Integer position, PlaySessionSource playSessionSource, List<Urn> tracks) {
        return RxJava.toV1Observable(playbackInitiator.playTracks(
                tracks,
                position,
                playSessionSource
        ));
    }

    private Observable<LikeOperations.LikeResult> actionLike(PublishSubject<Boolean> trigger) {
        return model().compose(Transformers.takePairWhen(trigger))
                      .withLatestFrom(playSessionSource(), this::like)
                      .flatMap(x -> x);
    }

    private Observable<RepostOperations.RepostResult> actionRepost(PublishSubject<Boolean> trigger) {
        return model().compose(Transformers.takePairWhen(trigger))
                      .withLatestFrom(playSessionSource(), this::repost)
                      .flatMap(x -> x);
    }

    private Observable<Pair<Urn, PlaySessionSource>> actionMakeOfflineUnavailableOffline(PublishSubject<Void> trigger) {
        return model().compose(Transformers.takeWhen(trigger))
                      .map(model -> model.metadata().urn())
                      .withLatestFrom(playSessionSource(), Pair::of)
                      .publish(source -> Observable.merge(
                              source.filter(__ -> offlineContentOperations.isOfflineCollectionEnabled()).doOnNext(showDisableOfflineCollectionConfirmation::onNext),
                              source.filter(__ -> !offlineContentOperations.isOfflineCollectionEnabled()).doOnNext(this::makePlaylistUnAvailableOffline))
                      );
    }

    private Observable<AsyncViewModel<PlaylistDetailsViewModel>> actionMakeAvailableOffline(PublishSubject<Void> trigger) {
        return viewModelSubject.compose(Transformers.takeWhen(trigger))
                               .withLatestFrom(playSessionSource(), Pair::of)
                               .flatMap(this::makePlaylistAvailableOfflineIfPossible);
    }

    private Observable<AsyncViewModel<PlaylistDetailsViewModel>> makePlaylistAvailableOfflineIfPossible(Pair<AsyncViewModel<PlaylistDetailsViewModel>, PlaySessionSource> pair) {
        if (offlineSettingsStorage.isOfflineContentAccessible()) {
            return makePlaylistAvailableOffline(pair)
                    .doOnNext(viewModelSubject::onNext);
        } else {
            // TODO : is there a nicer way to write this
            showOfflineStorageErrorDialog.onNext(null);
            return empty();
        }
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
            return RxJava.toV1Observable(likeOperations.toggleLike(playlistUrn, true)).flatMap(whenLikeSucceeded(operation));
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
                .map(model -> model.toBuilder().data(model.data().get().updateWithMarkedForOffline(isMarkedForOffline)).build());
    }

    private boolean isInUserCollection(Urn playlistUrn, Urn creatorUrn) {
        return likesStateProvider.latest().isLiked(playlistUrn) || accountOperations.isLoggedInUser(creatorUrn);
    }

    private Observable<PlaySessionSource> playSessionSource() {
        return model().map(createPlaySessionSource());
    }

    private Observable<Urn> actionGoToCreator(PublishSubject<Void> trigger) {
        return model().compose(Transformers.takeWhen(trigger))
                      .map(model -> model.metadata().creatorUrn());
    }

    private Observable<Urn> actionPlayNext(PublishSubject<Void> trigger) {
        return model().compose(Transformers.takeWhen(trigger))
                      .map(model -> model.metadata().urn());
    }

    private Observable<Urn> actionDeletePlaylist(PublishSubject<Void> trigger) {
        return model().compose(Transformers.takeWhen(trigger))
                      .map(model -> model.metadata().urn());
    }

    private Observable<SharePresenter.ShareOptions> actionSharePlaylist(PublishSubject<Void> trigger) {
        return model().compose(Transformers.takeWhen(trigger))
                      .filter(model -> model.metadata().permalinkUrl().isPresent())
                      .withLatestFrom(playSessionSource(), Pair::of)
                      .map(pair -> {
                          final PlaylistDetailsMetadata metadata = pair.first().metadata();
                          return SharePresenter.ShareOptions.create(
                                  metadata.permalinkUrl().get(),
                                  getEventContext(metadata.urn()),
                                  pair.second().getPromotedSourceInfo(),
                                  createEntityMetadata(metadata)
                          );
                      });
    }

    private Observable<Urn> actionGoToUpsellFromTrack(PublishSubject<PlaylistDetailUpsellItem> trigger) {
        return models().compose(Transformers.takeWhen(trigger))
                       .map(playlistWithTracks -> playlistWithTracks.metadata().urn())
                       .doOnNext(urn -> eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistTracksClick(urn)));
    }

    private Observable<Urn> actionOnMakeOfflineUpsell(PublishSubject<Void> trigger) {
        return models().compose(Transformers.takeWhen(trigger))
                       .map(playlistWithTracks -> playlistWithTracks.metadata().urn())
                       .doOnNext(urn -> eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistPageClick(urn)));
    }

    private Observable<Urn> actionOnOverflowMakeOfflineUpsell(PublishSubject<Void> trigger) {
        return models().compose(Transformers.takeWhen(trigger))
                       .map(playlistWithTracks -> playlistWithTracks.metadata().urn())
                       .doOnNext(urn -> eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistOverflowClick(urn)));
    }

    private void makePlaylistUnAvailableOffline(Pair<Urn, PlaySessionSource> urnSourcePair) {
        fireAndForget(offlineContentOperations.makePlaylistUnavailableOffline(urnSourcePair.first()));
        eventBus.publish(EventQueue.TRACKING, getOfflinePlaylistTrackingEvent(urnSourcePair.first(), false, urnSourcePair.second()));
    }

    private EntityMetadata createEntityMetadata(PlaylistDetailsMetadata metadata) {
        return EntityMetadata.from(metadata.creatorName(), metadata.creatorUrn(), metadata.title(), metadata.urn());
    }

    private Func1<PlaylistDetailsViewModel, PlaySessionSource> createPlaySessionSource() {
        return model -> {
            final PlaylistDetailsMetadata metadata = model.metadata();
            PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(screen, metadata.urn(), metadata.creatorUrn(), metadata.trackCount());
            if (promotedSourceInfo != null) {
                playSessionSource.setPromotedSourceInfo(promotedSourceInfo);
            } else if (searchQuerySourceInfo != null) {
                playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
            }
            return playSessionSource;
        };
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

    PublishSubject<Urn> goToContentUpsell() {
        return goToContentUpsell;
    }

    PublishSubject<Urn> goToOfflineUpsell() {
        return goToOfflineUpsell;
    }

    Observable<PlaybackResult.ErrorReason> onPlaybackError() {
        return playbackError;
    }

    Observable<ViewError> onRefreshError() {
        return refreshError;
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

    PublishSubject<Void> onShowOfflineStorageErrorDialog() {
        return showOfflineStorageErrorDialog;
    }

    Observable<SharePresenter.ShareOptions> onShare() {
        return sharePlaylist;
    }

    public Observable<AsyncViewModel<PlaylistDetailsViewModel>> viewModel() {
        return viewModelSubject;
    }

    private Observable<PlaylistDetailsViewModel> onOverflowUpsellImpression(PublishSubject<Void> trigger) {
        return models().compose(Transformers.takeWhen(trigger))
                       .doOnNext(model -> eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistOverflowImpression(model.metadata().getUrn())));
    }

    private Observable<PlaybackResult> actionPlayShuffled(PublishSubject<Void> trigger) {
        return models().compose(Transformers.takeWhen(trigger))
                       .withLatestFrom(playSessionSource(), Pair::of)
                       .flatMap(pair -> {
                           PlaylistDetailsViewModel viewModel = pair.first();
                           return RxJava.toV1Observable(playbackInitiator
                                   .playTracksShuffled(Single.just(transform(viewModel.tracks(), PlaylistDetailTrackItem::getUrn)), pair.second()))
                                   .doOnCompleted(() -> eventBus.publish(EventQueue.TRACKING, UIEvent.fromShuffle(getEventContext(viewModel.metadata().urn()))));
                       }).doOnNext(this::sendErrorIfUnsuccessful);
    }

    private Observable<LikeOperations.LikeResult> like(android.util.Pair<PlaylistDetailsViewModel, Boolean> pair, PlaySessionSource playSessionSource) {
        final PlaylistDetailsViewModel model = pair.first;
        final Boolean isLike = pair.second;

        final PlaylistDetailsMetadata metadata = model.metadata();
        final Urn playlistUrn = metadata.urn();
        final EntityMetadata entityMetadata = createEntityMetadata(metadata);
        eventTracker.trackEngagement(UIEvent.fromToggleLike(isLike,
                                                            playlistUrn,
                                                            getEventContext(playlistUrn),
                                                            playSessionSource.getPromotedSourceInfo(),
                                                            entityMetadata));

        return RxJava.toV1Observable(likeOperations.toggleLike(playlistUrn, isLike));
    }

    private Observable<RepostOperations.RepostResult> repost(android.util.Pair<PlaylistDetailsViewModel, Boolean> playlistWithExtrasBooleanPair, PlaySessionSource playSessionSource) {
        final PlaylistDetailsMetadata playlist = playlistWithExtrasBooleanPair.first.metadata();
        final Boolean isReposted = playlistWithExtrasBooleanPair.second;

        eventTracker.trackEngagement(UIEvent.fromToggleRepost(isReposted,
                                                              playlist.urn(),
                                                              getEventContext(playlist.urn()),
                                                              playSessionSource.getPromotedSourceInfo(),
                                                              createEntityMetadata(playlist)));

        return RxJava.toV1Observable(repostOperations.toggleRepost(playlist.urn(), isReposted));
    }

    private EventContextMetadata getEventContext(Urn playlistUrn) {
        return EventContextMetadata.builder()
                                   .pageName(Screen.PLAYLIST_DETAILS.get())
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

    static class LikeStateChangedIntent {

        static class PlaylistLikedResult implements ActionResult {
            final boolean isPlaylistLiked;

            PlaylistLikedResult(boolean isPlaylistLiked) {
                this.isPlaylistLiked = isPlaylistLiked;
            }

            @Override
            public AsyncViewModel<PlaylistDetailsViewModel> apply(AsyncViewModel<PlaylistDetailsViewModel> previous) {
                final Optional<PlaylistDetailsViewModel> data = previous.data();
                final PlaylistDetailsMetadata previousMetadata = data.get().metadata();
                final PlaylistDetailsMetadata metadata = previousMetadata
                        .toBuilder()
                        .isLikedByUser(this.isPlaylistLiked)
                        .build();
                final PlaylistDetailsViewModel updatedData = data.get().toBuilder().metadata(metadata).build();
                return previous.toBuilder().data(of(updatedData)).build();
            }
        }

        static Observable<ActionResult> toResult(Urn urn, LikesStateProvider likesStateProvider) {
            return likesStateProvider.likedStatuses().map(likeStatuses -> new PlaylistLikedResult(likeStatuses.isLiked(urn)));
        }
    }

    static class EditModeChangedIntent {

        static class EditModeResult implements ActionResult {
            final boolean isEditMode;
            final OfflineProperties offlineProperties;

            EditModeResult(boolean isEditMode, OfflineProperties propertiesOptional) {
                this.isEditMode = isEditMode;
                this.offlineProperties = propertiesOptional;
            }

            @Override
            public AsyncViewModel<PlaylistDetailsViewModel> apply(AsyncViewModel<PlaylistDetailsViewModel> previous) {
                final PlaylistDetailsViewModel previousModel = previous.data().get();
                final PlaylistDetailsMetadata previousMetadata = previousModel.metadata();
                final PlaylistDetailsMetadata metadata = previousMetadata
                        .toBuilder()
                        .isInEditMode(this.isEditMode)
                        .build();

                final List<PlaylistDetailTrackItem> updatedTracks = transform(previousModel.tracks(), track -> track.toBuilder().inEditMode(this.isEditMode).build());
                final PlaylistDetailsViewModel data = previousModel
                        .toBuilder()
                        .metadata(metadata)
                        .tracks(updatedTracks)
                        .build();
                final AsyncViewModel<PlaylistDetailsViewModel> updated = previous.toBuilder().data(of(data)).build();
                return OfflineStateChangedIntent.toModel(updated, this.offlineProperties);
            }
        }

        static Observable<EditModeResult> toResult(BehaviorSubject<Boolean> editMode, Observable<OfflineProperties> states) {
            return editMode
                    .withLatestFrom(states, (isEditMode, offlineProperties) -> {
                        final OfflineProperties properties = isEditMode ? OfflineProperties.empty() : offlineProperties;
                        return new EditModeResult(isEditMode, properties);
                    });
        }
    }

    static class OfflineStateChangedIntent {

        static class OfflineStateResult implements ActionResult {
            final OfflineProperties offlineProperties;

            OfflineStateResult(OfflineProperties playlistOfflineState) {
                this.offlineProperties = playlistOfflineState;
            }

            @Override
            public AsyncViewModel<PlaylistDetailsViewModel> apply(AsyncViewModel<PlaylistDetailsViewModel> previous) {
                return OfflineStateChangedIntent.toModel(previous, this.offlineProperties);
            }
        }

        static Observable<OfflineStateResult> toResult(Observable<OfflineProperties> states, BehaviorSubject<Boolean> editMode) {
            return states
                    .withLatestFrom(editMode, Pair::of).filter(__ -> !__.second()).map(Pair::first)
                    .map(OfflineStateResult::new);
        }

        static AsyncViewModel<PlaylistDetailsViewModel> toModel(AsyncViewModel<PlaylistDetailsViewModel> previous, OfflineProperties offlineProperties) {
            final Optional<PlaylistDetailsViewModel> data = previous.data();
            final PlaylistDetailsViewModel previousModel = data.get();
            final PlaylistDetailsMetadata previousMetadata = previousModel.metadata();
            final OfflineState playlistOfflineState = offlineProperties.state(previousMetadata.urn());


            final PlaylistDetailsMetadata metadata = previousMetadata
                    .toBuilder()
                    .with(playlistOfflineState)
                    .build();
            final PlaylistDetailsViewModel updatedModel = previousModel
                    .toBuilder()
                    .tracks(applyOfflineStateChange(previousModel, offlineProperties))
                    .metadata(metadata)
                    .build();
            return previous.toBuilder().data(of(updatedModel)).build();
        }

        private static List<PlaylistDetailTrackItem> applyOfflineStateChange(PlaylistDetailsViewModel playlistWithExtras, OfflineProperties offlineProperties) {
            final List<PlaylistDetailTrackItem> tracks = playlistWithExtras.tracks();
            final List<PlaylistDetailTrackItem> trackItems = new ArrayList<>(tracks.size());
            final boolean isInEditMode = playlistWithExtras.metadata().isInEditMode();

            for (PlaylistDetailTrackItem actualPlaylistDetailItem : tracks) {
                final OfflineState updatedOfflineState = isInEditMode ? OfflineState.NOT_OFFLINE : offlineProperties.state(actualPlaylistDetailItem.getUrn());

                if (updatedOfflineState != actualPlaylistDetailItem.trackItem().offlineState()) {
                    final TrackItem updatedTrackItem = actualPlaylistDetailItem.trackItem().toBuilder().offlineState(updatedOfflineState).build();
                    final PlaylistDetailTrackItem updatedPlaylistDetailItem = actualPlaylistDetailItem
                            .toBuilder()
                            .trackItem(updatedTrackItem)
                            .build();
                    trackItems.add(updatedPlaylistDetailItem);
                } else {
                    trackItems.add(actualPlaylistDetailItem);
                }
            }
            return trackItems;
        }
    }

    static class PlaylistRepostedIntent {

        static class PlaylistRepostedResult implements ActionResult {
            final boolean isPlaylistReposted;

            PlaylistRepostedResult(boolean isPlaylistReposted) {
                this.isPlaylistReposted = isPlaylistReposted;
            }

            @Override
            public AsyncViewModel<PlaylistDetailsViewModel> apply(AsyncViewModel<PlaylistDetailsViewModel> previous) {
                final Optional<PlaylistDetailsViewModel> data = previous.data();
                final PlaylistDetailsMetadata previousMetadata = data.get().metadata();
                final PlaylistDetailsMetadata metadata = previousMetadata
                        .toBuilder()
                        .isRepostedByUser(this.isPlaylistReposted)
                        .build();
                return previous.toBuilder()
                               .data(of(data.get().toBuilder().metadata(metadata).build()))
                               .build();
            }
        }

        static Observable<PlaylistRepostedResult> toResult(Urn urn, Observable<RepostStatuses> repostedStatuses) {
            return repostedStatuses.map(repostStatuses -> new PlaylistRepostedResult(repostStatuses.isReposted(urn)));
        }
    }

    static class NowPlayingIntent {

        static class NowPlayingResult implements ActionResult {
            final Urn track;

            NowPlayingResult(Urn track) {
                this.track = track;
            }


            @Override
            public AsyncViewModel<PlaylistDetailsViewModel> apply(AsyncViewModel<PlaylistDetailsViewModel> previous) {
                final PlaylistDetailsViewModel previousData = previous.data().get();
                final List<PlaylistDetailTrackItem> updateTracks = toModel(previousData, this.track);
                final PlaylistDetailsViewModel updatedData = previousData.toBuilder().tracks(updateTracks).build();
                return previous.toBuilder().data(of(updatedData)).build();
            }

            static List<PlaylistDetailTrackItem> toModel(PlaylistDetailsViewModel playlistWithExtras, Urn currentTrackPlaying) {
                final List<PlaylistDetailTrackItem> actualPlaylistTrackItems = playlistWithExtras.tracks();
                final List<PlaylistDetailTrackItem> updatedPlaylistTrackItems = new ArrayList<>(actualPlaylistTrackItems.size());

                for (PlaylistDetailTrackItem actualPlaylistTrackItem : actualPlaylistTrackItems) {

                    final boolean updatedPlayState = actualPlaylistTrackItem.getUrn().equals(currentTrackPlaying);
                    if (updatedPlayState != actualPlaylistTrackItem.trackItem().isPlaying()) {
                        final TrackItem updatedTackItem = actualPlaylistTrackItem.trackItem().toBuilder().isPlaying(updatedPlayState).build();
                        final PlaylistDetailTrackItem updatedPlaylistTrackItem = actualPlaylistTrackItem.toBuilder().trackItem(updatedTackItem).build();
                        updatedPlaylistTrackItems.add(updatedPlaylistTrackItem);
                    } else {
                        updatedPlaylistTrackItems.add(actualPlaylistTrackItem);
                    }
                }
                return updatedPlaylistTrackItems;
            }
        }

        static Observable<NowPlayingResult> toResult(Observable<Urn> trackPlaying) {
            return trackPlaying.map(NowPlayingResult::new);
        }

    }

    static class DismissUpsellIntent {
        static class DismissUpsellResult implements ActionResult {

            @Override
            public AsyncViewModel<PlaylistDetailsViewModel> apply(AsyncViewModel<PlaylistDetailsViewModel> previous) {
                final PlaylistDetailsViewModel updatedDetailsModel = previous.data().get().toBuilder().upsell(absent()).build();
                return previous.toBuilder().data(updatedDetailsModel).build();
            }
        }

        static Observable<DismissUpsellResult> toResult(PublishSubject<PlaylistDetailUpsellItem> onUpsellDismissed, PlaylistUpsellOperations playlistUpsellOperations) {
            return onUpsellDismissed
                    .doOnNext(__ -> playlistUpsellOperations.disableUpsell())
                    .map(__ -> new DismissUpsellResult());
        }
    }

    static class UpdateTrackListIntent {

        static class UpdateTrackListResult implements ActionResult {

            final Resources resources;
            final List<Track> updatedTracksList;

            UpdateTrackListResult(Resources resources, List<Track> updatedTracksList) {
                this.resources = resources;
                this.updatedTracksList = updatedTracksList;
                System.out.println("### UpdateTrackListResult " + updatedTracksList);
            }

            @Override
            public AsyncViewModel<PlaylistDetailsViewModel> apply(AsyncViewModel<PlaylistDetailsViewModel> previous) {
                final List<TrackItem> tracksList = transform(this.updatedTracksList, TrackItem::from);
                final PlaylistDetailsViewModel previewViewModel = previous.data().get();

                final PlaylistDetailTrackItem.Builder detailTrackItemBuilder = PlaylistDetailTrackItem.builder().inEditMode(previewViewModel.metadata().isInEditMode());
                final List<PlaylistDetailTrackItem> updatedTracksList = transform(tracksList, track -> detailTrackItemBuilder.trackItem(track).build());
                final PlaylistDetailsMetadata updatedMetadata = previewViewModel.metadata().toBuilder().with(resources, tracksList).build();

                final PlaylistDetailsViewModel updatedData = previewViewModel.toBuilder()
                                                                             .metadata(updatedMetadata)
                                                                             .tracks(updatedTracksList)
                                                                             .build();
                final AsyncViewModel<PlaylistDetailsViewModel> build = previous.toBuilder().data(updatedData).build();
                System.out.println("### apply > " + build);
                System.out.println("### apply > track:" + build.data().get().tracks());
                return build;
            }
        }

        static Observable<UpdateTrackListResult> toResult(Resources resources, PlaylistDetailsInputs inputs, Urn playlistUrn, PlaylistOperations playlistOperations) {
            return inputs
                    .tracklistUpdated
                    .flatMap(tracks -> playlistOperations.editPlaylistTracks(playlistUrn, transform(tracks, PlaylistDetailTrackItem::getUrn)))
                    .map(tracks -> new UpdateTrackListResult(resources, tracks));
        }
    }

    static class PlaylistWithExtrasStateIntent {


        static class PlaylistWithExtrasStateResult implements ActionResult {
            private final PlaylistWithExtrasState playlistWithExtrasState;
            private final Resources resources;
            private final EntityItemCreator entityItemCreator;
            // TODO: do not store operations but states
            private final FeatureOperations featureOperations;
            private final PlaylistUpsellOperations playlistUpsellOperations;

            PlaylistWithExtrasStateResult(PlaylistWithExtrasState playlistWithExtrasState,
                                          Resources resources,
                                          EntityItemCreator entityItemCreator,
                                          FeatureOperations featureOperations,
                                          PlaylistUpsellOperations upsellOperations) {
                this.playlistWithExtrasState = playlistWithExtrasState;
                this.resources = resources;
                this.entityItemCreator = entityItemCreator;
                this.featureOperations = featureOperations;
                this.playlistUpsellOperations = upsellOperations;
            }

            @Override
            public AsyncViewModel<PlaylistDetailsViewModel> apply(AsyncViewModel<PlaylistDetailsViewModel> previous) {
                if (playlistWithExtrasState.playlistWithExtras().isPresent()) {
                    return modelWithPlaylist(previous);
                } else {
                    return modelWithoutPlaylist(previous);
                }
            }


            // TODO: Use featureOperations, playlistUpsellOperations when building the Result in the `dataSource` function
            private AsyncViewModel<PlaylistDetailsViewModel> modelWithPlaylist(AsyncViewModel<PlaylistDetailsViewModel> previous) {
                final PlaylistWithExtras updatedPlaylistWithExtras = this.playlistWithExtrasState.playlistWithExtras().get();
                final Optional<PlaylistDetailsViewModel> data = previous.data();
                final List<TrackItem> updatedTrackItems = toTrackItems(updatedPlaylistWithExtras.tracks());
                final Playlist playlist = updatedPlaylistWithExtras.playlist();
                final PlaylistDetailsViewModel.Builder viewModelBuilder;
                final boolean inEditMode;
                final PlaylistDetailsMetadata updatedMetadata;

                final PlaylistDetailsMetadata.OfflineOptions offlineOptions = PlaylistDetailsMetadata.Builder.toOfflineOptions(featureOperations);

                if (data.isPresent()) {
                    final PlaylistDetailsViewModel previousModel = data.get();
                    final PlaylistDetailsMetadata metadata = previousModel.metadata();
                    updatedMetadata = metadata.toBuilder()
                                              .with(resources,
                                                    playlist,
                                                    updatedTrackItems,
                                                    updatedPlaylistWithExtras.isLoggedInUserOwner(),
                                                    offlineOptions)
                                              .build();

                    viewModelBuilder = previousModel.toBuilder();
                    inEditMode = previousModel.metadata().isInEditMode();
                } else {
                    inEditMode = false;
                    viewModelBuilder = PlaylistDetailsViewModel.builder();
                    updatedMetadata = PlaylistDetailsMetadata.builder()
                                                             .with(resources,
                                                                   playlist,
                                                                   updatedTrackItems,
                                                                   updatedPlaylistWithExtras.isLoggedInUserOwner(),
                                                                   offlineOptions)
                                                             .with(OfflineState.NOT_OFFLINE)
                                                             .isRepostedByUser(false)
                                                             .isLikedByUser(false)
                                                             .isInEditMode(false)
                                                             .build();
                }

                final PlaylistDetailTrackItem.Builder detailTrackItemBuilder = PlaylistDetailTrackItem.builder().inEditMode(inEditMode);
                viewModelBuilder
                        .tracks(transform(updatedTrackItems, track -> detailTrackItemBuilder.trackItem(track).build()))
                        .upsell(playlistUpsellOperations.getUpsell(playlist, updatedTrackItems))
                        .otherPlaylists(createOtherPlaylistsItem(updatedPlaylistWithExtras, inEditMode, entityItemCreator))
                        .metadata(updatedMetadata);

                return previous.toBuilder()
                               .data(of(viewModelBuilder.build()))
                               .isRefreshing(this.playlistWithExtrasState.isRefreshing())
                               .error(this.playlistWithExtrasState.viewError())
                               .isLoadingNextPage(false)
                               .build();
            }

            private AsyncViewModel<PlaylistDetailsViewModel> modelWithoutPlaylist(AsyncViewModel<PlaylistDetailsViewModel> previous) {
                return previous.toBuilder()
                               .isRefreshing(this.playlistWithExtrasState.isRefreshing())
                               .isLoadingNextPage(!this.playlistWithExtrasState.viewError().isPresent())
                               .error(this.playlistWithExtrasState.viewError())
                               .data(absent())
                               .build();
            }

            private static List<TrackItem> toTrackItems(Optional<List<Track>> tracksOptional) {
                if (tracksOptional.isPresent()) {
                    return transform(tracksOptional.get(), TrackItem::from);
                } else {
                    return emptyList();
                }
            }

            private static Optional<PlaylistDetailOtherPlaylistsItem> createOtherPlaylistsItem(PlaylistWithExtras playlistWithExtras, boolean isInEditMode, EntityItemCreator entityItemCreator) {
                if (!isInEditMode && !playlistWithExtras.otherPlaylistsByCreator().isEmpty()) {

                    List<PlaylistItem> otherPlaylistItems = transform(playlistWithExtras.otherPlaylistsByCreator(), entityItemCreator::playlistItem);
                    String creatorName = playlistWithExtras.playlist().creatorName();
                    return of(new PlaylistDetailOtherPlaylistsItem(creatorName, otherPlaylistItems, playlistWithExtras.playlist().isAlbum()));

                } else {
                    return absent();
                }

            }
        }


        static Observable<PlaylistWithExtrasStateResult> dataSource(PlaylistDetailsInputs inputs,
                                                                    Urn playlistUrn,
                                                                    DataSourceProvider dataSourceProvider,
                                                                    Resources resources,
                                                                    FeatureOperations featureOperations,
                                                                    PlaylistUpsellOperations playlistUpsellOperations,
                                                                    EntityItemCreator entityItemCreator) {
            return dataSourceProvider
                    .dataWith(playlistUrn, inputs.refresh)
                    .map((playlistWithExtrasState) -> new PlaylistWithExtrasStateResult(playlistWithExtrasState, resources, entityItemCreator, featureOperations, playlistUpsellOperations));
        }
    }
}
