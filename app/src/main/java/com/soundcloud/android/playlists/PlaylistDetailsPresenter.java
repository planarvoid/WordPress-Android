package com.soundcloud.android.playlists;

import static com.soundcloud.android.events.EventQueue.URN_STATE_CHANGED;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.java.collections.Lists.transform;
import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.of;
import static io.reactivex.Observable.combineLatest;
import static io.reactivex.Observable.empty;
import static io.reactivex.Observable.just;
import static java.util.Collections.emptyList;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
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
import com.soundcloud.android.events.ScreenEvent;
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
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.playqueue.PlayQueueHelper;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.rx.CrashOnTerminateSubscriber;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.rx.RxSignal;
import com.soundcloud.android.share.SharePresenter;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.transformers.Transformers;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import android.content.res.Resources;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AutoFactory
public class PlaylistDetailsPresenter {

    private final Resources resources;
    private final PlaylistOperations playlistOperations;
    private final PlaylistUpsellOperations playlistUpsellOperations;
    private final PlaybackInitiator playbackInitiator;
    private final LikesStateProvider likesStateProvider;
    private final RepostsStateProvider repostsStateProvider;
    private final PlayQueueHelper playQueueHelper;
    private final OfflinePropertiesProvider offlinePropertiesProvider;
    private final EventBusV2 eventBus;
    private final OfflineContentOperations offlineContentOperations;
    private final EventTracker eventTracker;
    private final LikeOperations likeOperations;
    private final RepostOperations repostOperations;
    private final AccountOperations accountOperations;
    private final SearchQuerySourceInfo searchQuerySourceInfo;
    private final PromotedSourceInfo promotedSourceInfo;
    private final String screen;
    private final OfflineSettingsStorage offlineSettingsStorage;
    private final TrackingStateProvider trackingStateProvider;

    private final EntityItemCreator entityItemCreator;

    private CompositeDisposable disposable = new CompositeDisposable();

    // outputs
    private final BehaviorSubject<PlaylistAsyncViewModel<PlaylistDetailsViewModel>> viewModelSubject = BehaviorSubject.create();
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
                             @Provided EventBusV2 eventBus,
                             @Provided OfflineContentOperations offlineContentOperations,
                             @Provided EventTracker eventTracker,
                             @Provided LikeOperations likeOperations,
                             @Provided DataSourceProvider dataSourceProvider,
                             @Provided RepostOperations repostOperations,
                             @Provided AccountOperations accountOperations,
                             @Provided EntityItemCreator entityItemCreator,
                             @Provided FeatureOperations featureOperations,
                             @Provided OfflineSettingsStorage offlineSettingsStorage,
                             @Provided TrackingStateProvider trackingStateProvider) {
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
        this.entityItemCreator = entityItemCreator;
        this.featureOperations = featureOperations;
        this.offlineSettingsStorage = offlineSettingsStorage;
        this.dataSourceProvider = dataSourceProvider;
        this.trackingStateProvider = trackingStateProvider;
    }

    public void connect(PlaylistDetailsInputs inputs, PlaylistDetailView view, Urn playlistUrn) {
        disposable.clear();
        disposable = new CompositeDisposable(
                subscribeToCommands(inputs, view),
                emitViewModel(inputs, playlistUrn).subscribeWith(new CrashOnTerminateSubscriber<>())
        );

        if (featureOperations.upsellOfflineContent()) {
            disposable.add(firePlaylistPageUpsellImpression().subscribe());
        }
    }

    private Observable<PlaylistAsyncViewModel<PlaylistDetailsViewModel>> emitViewModel(PlaylistDetailsInputs inputs, Urn playlistUrn) {
        return PlaylistWithExtrasStateIntent.dataSource(inputs, playlistUrn, dataSourceProvider, resources, featureOperations, playlistUpsellOperations, entityItemCreator)
                                            .flatMap(data -> actions(inputs, data))
                                            .scan(PlaylistAsyncViewModel.initial(), this::toViewModel)
                                            .distinctUntilChanged()
                                            .doOnNext(viewModelSubject::onNext);
    }

    private Observable<? extends ActionResult> actions(PlaylistDetailsInputs inputs, PlaylistWithExtrasStateIntent.PlaylistWithExtrasStateResult data) {
        if (data.playlistWithExtrasState.playlistWithExtras().isPresent()) {
            final Urn urn = data.playlistWithExtrasState.playlistWithExtras().get().playlist().urn();
            return Observable
                    .merge(
                            Arrays.asList(
                                    UpdateTrackListIntent.toResult(resources, inputs, urn, playlistOperations),
                                    DismissUpsellIntent.toResult(inputs.onUpsellDismissed, playlistUpsellOperations),
                                    NowPlayingIntent.toResult(currentTrackPlaying()),
                                    EditModeChangedIntent.toResult(inputs.editMode, offlinePropertiesProvider.states()),
                                    LikeStateChangedIntent.toResult(urn, likesStateProvider),
                                    PlaylistRepostedIntent.toResult(urn, repostsStateProvider.repostedStatuses()),
                                    OfflineStateChangedIntent.toResult(offlinePropertiesProvider.states(), inputs.editMode)
                            )
                    )
                    .startWith(data);
        } else {
            return just(data);
        }
    }

    private PlaylistAsyncViewModel<PlaylistDetailsViewModel> toViewModel(PlaylistAsyncViewModel<PlaylistDetailsViewModel> previous, ActionResult result) {
        return result.apply(previous);
    }

    private Disposable subscribeToCommands(PlaylistDetailsInputs inputs, PlaylistDetailView view) {

        return new CompositeDisposable(
                // Track pageview event on screen enter
                trackPageViewEvent(view),

                // -> Show playlist deletion confirmation
                actionDeletePlaylist(inputs.delete).subscribe(view::showPlaylistDetailConfirmation),
                // -> Share playlist
                actionSharePlaylist(inputs.share).subscribe(view::sharePlaylist),
                // -> Show like toResult
                actionLike(inputs.like).subscribe(view::showLikeResult),
                // -> Show Repost toResult
                actionRepost(inputs.repost).subscribe(view::showRepostResult),
                // -> Go back
                onPlaylistDeleted().subscribe(view::goBack),
                // -> Go to creator
                actionGoToCreator(inputs.onCreatorClicked).subscribe(view::goToCreator),
                // -> Go to upsell
                actionOnMakeOfflineUpsell(inputs.onMakeOfflineUpsell).subscribe(view::goToOfflineUpsell),
                actionOnOverflowMakeOfflineUpsell(inputs.onOverflowMakeOfflineUpsell).subscribe(view::goToOfflineUpsell),
                actionGoToUpsellFromTrack(inputs.onUpsellItemClicked).subscribe(view::goToContentUpsell),

                // -> Start playback
                actionPlayNext(inputs.playNext).subscribe(playQueueHelper::playNext),
                actionPlayPlaylistStartingFromTrack(inputs.playFromTrack).subscribe(view::showPlaybackResult),
                actionPlayShuffled(inputs.playShuffled).subscribe(view::showPlaybackResult),
                actionPlayPlaylist(inputs.headerPlayClicked).subscribe(view::showPlaybackResult),

                // -> Offline operations
                actionMakeAvailableOffline(inputs.makeOfflineAvailable).subscribe(),
                actionMakeAvailableOfflineFailed(inputs.makeOfflineAvailable).subscribe(view::showOfflineStorageErrorDialog),

                actionMakeOfflineUnavailableOfflineWithoutWarning(inputs.offlineUnavailable).subscribe(this::makePlaylistUnAvailableOffline),
                actionMakeOfflineUnavailableOfflineWithWarning(inputs.offlineUnavailable).subscribe(view::showDisableOfflineCollectionConfirmation),

                onOverflowUpsellImpression(inputs.overflowUpsellImpression).subscribe(),
                onFirstTrackUpsellImpression(inputs.firstTrackUpsellImpression).subscribe(),
                actionPlaylistTrackUpsellImpression(inputs.firstTrackUpsellImpression).subscribe(),

                viewModelSubject.filter(viewModel -> viewModel.refreshError().isPresent())
                                .map(viewModel -> viewModel.refreshError().get())
                                .subscribe(view::showRefreshError)
        );
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

    private Observable<PlaylistDetailsViewModel> model() {
        return viewModelSubject.filter(model -> model.data().isPresent()).map(model -> model.data().get());
    }

    private Observable<PlaybackResult> actionPlayPlaylist(PublishSubject<RxSignal> trigger) {
        return model().compose(Transformers.takeWhenV2(trigger))
                      .map(model -> model.metadata().urn())
                      .flatMap(urn -> RxJava.toV2Observable(playlistOperations.trackUrnsForPlayback(urn)))
                      .withLatestFrom(playSessionSource(), Pair::of)
                      .flatMap(pair -> playbackInitiator.playTracks(pair.first(), 0, pair.second()).toObservable());
    }

    private Observable<PlaybackResult> actionPlayPlaylistStartingFromTrack(PublishSubject<PlaylistDetailTrackItem> trigger) {
        return models()
                .compose(Transformers.takePairWhenV2(trigger))
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
                .flatMap(x -> x);
    }

    private Disposable trackPageViewEvent(PlaylistDetailView view) {
        return combineLatest(
                view.onEnterScreenTimestamp(),
                viewModel().filter(viewModel -> viewModel.data().isPresent()),
                (timestamp, viewModel) -> viewModel
        ).firstElement()
         .subscribe(viewModel -> {
             PlaylistDetailsViewModel playlistDetailsViewModel = viewModel.data().get();
             ScreenEvent pageviewEvent = ScreenEvent.createForPlaylist(Screen.PLAYLIST_DETAILS, Optional.fromNullable(playlistDetailsViewModel.metadata().urn()));
             eventTracker.trackScreen(pageviewEvent, trackingStateProvider.getLastEvent());
         });
    }


    private Observable<android.util.Pair<Urn, UrnStateChangedEvent>> onPlaylistDeleted() {
        Observable<UrnStateChangedEvent> playlistDeleted = eventBus.queue(URN_STATE_CHANGED)
                                                                   .filter(event -> event.kind() == UrnStateChangedEvent.Kind.ENTITY_DELETED)
                                                                   .filter(UrnStateChangedEvent::containsPlaylist);

        return model()
                .map(model -> model.metadata().urn())
                .compose(Transformers.takePairWhenV2(playlistDeleted))
                .filter(pair -> pair.second.urns().contains(pair.first))
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<PlaybackResult> playTracksFromPosition(Integer position, PlaySessionSource playSessionSource, List<Urn> tracks) {
        return playbackInitiator.playTracks(
                tracks,
                position,
                playSessionSource
        ).toObservable();
    }

    private Observable<LikeOperations.LikeResult> actionLike(PublishSubject<Boolean> trigger) {
        return model().compose(Transformers.takePairWhenV2(trigger))
                      .withLatestFrom(playSessionSource(), this::like)
                      .flatMap(x -> x);
    }

    private Observable<RepostOperations.RepostResult> actionRepost(PublishSubject<Boolean> trigger) {
        return model().compose(Transformers.takePairWhenV2(trigger))
                      .withLatestFrom(playSessionSource(), this::repost)
                      .flatMap(x -> x);
    }

    private Observable<Pair<Urn, PlaySessionSource>> actionMakeOfflineUnavailableOfflineWithoutWarning(PublishSubject<RxSignal> trigger) {
        return model().compose(Transformers.takeWhenV2(trigger))
                      .map(model -> model.metadata().urn())
                      .withLatestFrom(playSessionSource(), Pair::of)
                      .filter(__ -> !offlineContentOperations.isOfflineCollectionEnabled());
    }

    private Observable<Pair<Urn, PlaySessionSource>> actionMakeOfflineUnavailableOfflineWithWarning(PublishSubject<RxSignal> trigger) {
        return model().compose(Transformers.takeWhenV2(trigger))
                      .map(model -> model.metadata().urn())
                      .withLatestFrom(playSessionSource(), Pair::of)
                      .filter(__ -> offlineContentOperations.isOfflineCollectionEnabled());
    }

    private Observable<PlaylistAsyncViewModel<PlaylistDetailsViewModel>> actionMakeAvailableOffline(PublishSubject<RxSignal> trigger) {
        return viewModelSubject.compose(Transformers.takeWhenV2(trigger))
                               .withLatestFrom(playSessionSource(), Pair::of)
                               .filter(__ -> offlineSettingsStorage.isOfflineContentAccessible())
                               .flatMap(pair -> makePlaylistAvailableOffline(pair).doOnNext(viewModelSubject::onNext));
    }

    private Observable<Pair<PlaylistAsyncViewModel<PlaylistDetailsViewModel>, PlaySessionSource>> actionMakeAvailableOfflineFailed(PublishSubject<RxSignal> trigger) {
        return viewModelSubject.compose(Transformers.takeWhenV2(trigger))
                               .withLatestFrom(playSessionSource(), Pair::of)
                               .filter(__ -> !offlineSettingsStorage.isOfflineContentAccessible());
    }

    private Observable<PlaylistAsyncViewModel<PlaylistDetailsViewModel>> makePlaylistAvailableOffline(Pair<PlaylistAsyncViewModel<PlaylistDetailsViewModel>, PlaySessionSource> pair) {
        final PlaylistDetailsViewModel model = pair.first().data().get();
        final Urn playlistUrn = model.metadata().urn();
        final Urn creatorUrn = model.metadata().creatorUrn();
        final PlaySessionSource playSessionSource = pair.second();

        final Observable<RxSignal> operation = RxJava.toV2Observable(offlineContentOperations.makePlaylistAvailableOffline(playlistUrn)
                                                                                         .doOnNext(signal -> {
                                                                                             final OfflineInteractionEvent event = getOfflinePlaylistTrackingEvent(playlistUrn,
                                                                                                                                                                   true,
                                                                                                                                                                   playSessionSource);
                                                                                             eventBus.publish(EventQueue.TRACKING, event);
                                                                                         }));
        if (isInUserCollection(playlistUrn, creatorUrn)) {
            return operation.flatMap(ignored -> submitUpdateViewModel(true));
        } else {
            return likeOperations.toggleLike(playlistUrn, true).toObservable().flatMap(whenLikeSucceeded(operation));
        }
    }

    private Function<LikeOperations.LikeResult, Observable<? extends PlaylistAsyncViewModel<PlaylistDetailsViewModel>>> whenLikeSucceeded(Observable<RxSignal> action) {
        return likeResult -> likeResult == LikeOperations.LikeResult.LIKE_SUCCEEDED ?
                             action.flatMap(ignored -> submitUpdateViewModel(true)) :
                             submitUpdateViewModel(false);
    }

    private Observable<PlaylistAsyncViewModel<PlaylistDetailsViewModel>> submitUpdateViewModel(boolean isMarkedForOffline) {
        return viewModelSubject
                .firstElement()
                .map(model -> model.toBuilder().data(model.data().get().updateWithMarkedForOffline(isMarkedForOffline)).build()).toObservable();
    }

    private boolean isInUserCollection(Urn playlistUrn, Urn creatorUrn) {
        return likesStateProvider.latest().isLiked(playlistUrn) || accountOperations.isLoggedInUser(creatorUrn);
    }

    private Observable<PlaySessionSource> playSessionSource() {
        return model().map(createPlaySessionSource());
    }

    private Observable<Urn> actionGoToCreator(PublishSubject<RxSignal> trigger) {
        return model().compose(Transformers.takeWhenV2(trigger))
                      .map(model -> model.metadata().creatorUrn());
    }

    private Observable<Urn> actionPlayNext(PublishSubject<RxSignal> trigger) {
        return model().compose(Transformers.takeWhenV2(trigger))
                      .map(model -> model.metadata().urn());
    }

    private Observable<Urn> actionDeletePlaylist(PublishSubject<RxSignal> trigger) {
        return model().compose(Transformers.takeWhenV2(trigger))
                      .map(model -> model.metadata().urn());
    }

    private Observable<SharePresenter.ShareOptions> actionSharePlaylist(PublishSubject<RxSignal> trigger) {
        return model().compose(Transformers.takeWhenV2(trigger))
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
        return models().compose(Transformers.takeWhenV2(trigger))
                       .map(playlistWithTracks -> playlistWithTracks.metadata().urn())
                       .doOnNext(urn -> eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistTracksClick(urn)));
    }

    private Observable<Urn> actionOnMakeOfflineUpsell(PublishSubject<RxSignal> trigger) {
        return models().compose(Transformers.takeWhenV2(trigger))
                       .map(playlistWithTracks -> playlistWithTracks.metadata().urn())
                       .doOnNext(urn -> eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistPageClick(urn)));
    }

    private Observable<Urn> actionOnOverflowMakeOfflineUpsell(PublishSubject<RxSignal> trigger) {
        return models().compose(Transformers.takeWhenV2(trigger))
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

    private Function<PlaylistDetailsViewModel, PlaySessionSource> createPlaySessionSource() {
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
        disposable.clear();
    }

    public Observable<PlaylistAsyncViewModel<PlaylistDetailsViewModel>> viewModel() {
        return viewModelSubject;
    }

    private Observable<PlaylistDetailsViewModel> onOverflowUpsellImpression(PublishSubject<RxSignal> trigger) {
        return models().compose(Transformers.takeWhenV2(trigger))
                       .doOnNext(model -> eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistOverflowImpression(model.metadata().getUrn())));
    }

    private Observable<PlaylistDetailsViewModel> onFirstTrackUpsellImpression(PublishSubject<RxSignal> trigger) {
        return models().compose(Transformers.takeWhenV2(trigger))
                       .doOnNext(model -> eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistTracksImpression(model.metadata().urn())));
    }

    private Observable<UpgradeFunnelEvent> firePlaylistPageUpsellImpression() {
        return model().take(1).map(model -> UpgradeFunnelEvent.forPlaylistPageImpression(model.metadata().urn()))
                      .doOnNext(event -> eventBus.publish(EventQueue.TRACKING, event));
    }

    private Observable<UpgradeFunnelEvent> actionPlaylistTrackUpsellImpression(Subject<RxSignal> trigger) {
        return model().compose(Transformers.takeWhenV2(trigger))
                      .map(model -> UpgradeFunnelEvent.forPlaylistTracksImpression(model.metadata().urn()))
                      .doOnNext(event -> eventBus.publish(EventQueue.TRACKING, event));
    }

    private Observable<PlaybackResult> actionPlayShuffled(PublishSubject<RxSignal> trigger) {
        return models().compose(Transformers.takeWhenV2(trigger))
                       .withLatestFrom(playSessionSource(), Pair::of)
                       .flatMap(pair -> {
                           PlaylistDetailsViewModel viewModel = pair.first();
                           return playbackInitiator
                                   .playTracksShuffled(Single.just(transform(viewModel.tracks(), PlaylistDetailTrackItem::getUrn)), pair.second())
                                   .doOnSuccess(playbackResult -> eventBus.publish(EventQueue.TRACKING, UIEvent.fromShuffle(getEventContext(viewModel.metadata().urn())))).toObservable();
                       });
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

        return likeOperations.toggleLike(playlistUrn, isLike).toObservable();
    }

    private Observable<RepostOperations.RepostResult> repost(android.util.Pair<PlaylistDetailsViewModel, Boolean> playlistWithExtrasBooleanPair, PlaySessionSource playSessionSource) {
        final PlaylistDetailsMetadata playlist = playlistWithExtrasBooleanPair.first.metadata();
        final Boolean isReposted = playlistWithExtrasBooleanPair.second;

        eventTracker.trackEngagement(UIEvent.fromToggleRepost(isReposted,
                                                              playlist.urn(),
                                                              getEventContext(playlist.urn()),
                                                              playSessionSource.getPromotedSourceInfo(),
                                                              createEntityMetadata(playlist)));

        return repostOperations.toggleRepost(playlist.urn(), isReposted).toObservable();
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

    interface PlaylistDetailView {
        io.reactivex.Observable<Long> onEnterScreenTimestamp();

        void goToCreator(Urn urn);

        void goBack(Object ignored);

        void showRepostResult(RepostOperations.RepostResult result);

        void showLikeResult(LikeOperations.LikeResult result);

        void showPlaylistDetailConfirmation(Urn urn);

        void showDisableOfflineCollectionConfirmation(Pair<Urn, PlaySessionSource> params);

        void showOfflineStorageErrorDialog(Object ignored);

        void sharePlaylist(SharePresenter.ShareOptions options);

        void goToContentUpsell(Urn urn);

        void goToOfflineUpsell(Urn urn);

        void showRefreshError(ViewError refreshError);

        void showPlaybackResult(PlaybackResult playbackResult);
    }

    static final class LikeStateChangedIntent {

        private LikeStateChangedIntent() {
            // hide utility class
        }

        static class PlaylistLikedResult implements ActionResult {
            final boolean isPlaylistLiked;

            PlaylistLikedResult(boolean isPlaylistLiked) {
                this.isPlaylistLiked = isPlaylistLiked;
            }

            @Override
            public PlaylistAsyncViewModel<PlaylistDetailsViewModel> apply(PlaylistAsyncViewModel<PlaylistDetailsViewModel> previous) {
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

    static final class EditModeChangedIntent {

        private EditModeChangedIntent() {
            // hide utility class
        }

        static class EditModeResult implements ActionResult {
            final boolean isEditMode;
            final OfflineProperties offlineProperties;

            EditModeResult(boolean isEditMode, OfflineProperties propertiesOptional) {
                this.isEditMode = isEditMode;
                this.offlineProperties = propertiesOptional;
            }

            @Override
            public PlaylistAsyncViewModel<PlaylistDetailsViewModel> apply(PlaylistAsyncViewModel<PlaylistDetailsViewModel> previous) {
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
                final PlaylistAsyncViewModel<PlaylistDetailsViewModel> updated = previous.toBuilder().data(of(data)).build();
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

    static final class OfflineStateChangedIntent {

        private OfflineStateChangedIntent() {
            // hide utility class
        }

        static class OfflineStateResult implements ActionResult {
            final OfflineProperties offlineProperties;

            OfflineStateResult(OfflineProperties playlistOfflineState) {
                this.offlineProperties = playlistOfflineState;
            }

            @Override
            public PlaylistAsyncViewModel<PlaylistDetailsViewModel> apply(PlaylistAsyncViewModel<PlaylistDetailsViewModel> previous) {
                return OfflineStateChangedIntent.toModel(previous, this.offlineProperties);
            }
        }

        static Observable<OfflineStateResult> toResult(Observable<OfflineProperties> states, BehaviorSubject<Boolean> editMode) {
            return states
                    .withLatestFrom(editMode, Pair::of).filter(__ -> !__.second()).map(Pair::first)
                    .map(OfflineStateResult::new);
        }

        static PlaylistAsyncViewModel<PlaylistDetailsViewModel> toModel(PlaylistAsyncViewModel<PlaylistDetailsViewModel> previous, OfflineProperties offlineProperties) {
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

    static final class PlaylistRepostedIntent {

        private PlaylistRepostedIntent() {
            // hide utility class
        }

        static class PlaylistRepostedResult implements ActionResult {
            final boolean isPlaylistReposted;

            PlaylistRepostedResult(boolean isPlaylistReposted) {
                this.isPlaylistReposted = isPlaylistReposted;
            }

            @Override
            public PlaylistAsyncViewModel<PlaylistDetailsViewModel> apply(PlaylistAsyncViewModel<PlaylistDetailsViewModel> previous) {
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

    static final class NowPlayingIntent {

        private NowPlayingIntent() {
            // hide utility class
        }

        static class NowPlayingResult implements ActionResult {
            final Urn track;

            NowPlayingResult(Urn track) {
                this.track = track;
            }


            @Override
            public PlaylistAsyncViewModel<PlaylistDetailsViewModel> apply(PlaylistAsyncViewModel<PlaylistDetailsViewModel> previous) {
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

    static final class DismissUpsellIntent {

        private DismissUpsellIntent() {
            // hide utility class
        }

        static class DismissUpsellResult implements ActionResult {

            @Override
            public PlaylistAsyncViewModel<PlaylistDetailsViewModel> apply(PlaylistAsyncViewModel<PlaylistDetailsViewModel> previous) {
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

    static final class UpdateTrackListIntent {

        private UpdateTrackListIntent() {
            // hide utility class
        }

        static class UpdateTrackListResult implements ActionResult {

            final Resources resources;
            final List<Track> updatedTracksList;

            UpdateTrackListResult(Resources resources, List<Track> updatedTracksList) {
                this.resources = resources;
                this.updatedTracksList = updatedTracksList;
                System.out.println("### UpdateTrackListResult " + updatedTracksList);
            }

            @Override
            public PlaylistAsyncViewModel<PlaylistDetailsViewModel> apply(PlaylistAsyncViewModel<PlaylistDetailsViewModel> previous) {
                final List<TrackItem> tracksList = transform(this.updatedTracksList, TrackItem::from);
                final PlaylistDetailsViewModel previewViewModel = previous.data().get();

                final PlaylistDetailTrackItem.Builder detailTrackItemBuilder = PlaylistDetailTrackItem.builder().inEditMode(previewViewModel.metadata().isInEditMode());
                final List<PlaylistDetailTrackItem> updatedTracksList = transform(tracksList, track -> detailTrackItemBuilder.trackItem(track).build());
                final PlaylistDetailsMetadata updatedMetadata = previewViewModel.metadata().toBuilder().with(resources, tracksList).build();

                final PlaylistDetailsViewModel updatedData = previewViewModel.toBuilder()
                                                                             .metadata(updatedMetadata)
                                                                             .tracks(updatedTracksList)
                                                                             .build();
                final PlaylistAsyncViewModel<PlaylistDetailsViewModel> build = previous.toBuilder().data(updatedData).build();
                System.out.println("### apply > " + build);
                System.out.println("### apply > track:" + build.data().get().tracks());
                return build;
            }
        }

        static Observable<UpdateTrackListResult> toResult(Resources resources, PlaylistDetailsInputs inputs, Urn playlistUrn, PlaylistOperations playlistOperations) {
            return inputs
                    .tracklistUpdated
                    .flatMap(tracks -> RxJava.toV2Observable(playlistOperations.editPlaylistTracks(playlistUrn, transform(tracks, PlaylistDetailTrackItem::getUrn))))
                    .map(tracks -> new UpdateTrackListResult(resources, tracks));
        }
    }

    static final class PlaylistWithExtrasStateIntent {

        private PlaylistWithExtrasStateIntent() {
            // hide utility class
        }

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
            public PlaylistAsyncViewModel<PlaylistDetailsViewModel> apply(PlaylistAsyncViewModel<PlaylistDetailsViewModel> previous) {
                if (playlistWithExtrasState.playlistWithExtras().isPresent()) {
                    return modelWithPlaylist(previous);
                } else {
                    return modelWithoutPlaylist(previous);
                }
            }


            // TODO: Use featureOperations, playlistUpsellOperations when building the Result in the `dataSource` function
            private PlaylistAsyncViewModel<PlaylistDetailsViewModel> modelWithPlaylist(PlaylistAsyncViewModel<PlaylistDetailsViewModel> previous) {
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

            private PlaylistAsyncViewModel<PlaylistDetailsViewModel> modelWithoutPlaylist(PlaylistAsyncViewModel<PlaylistDetailsViewModel> previous) {
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


        @SuppressWarnings("sc.RxJava1Usage")
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

    interface ActionResult {
        PlaylistAsyncViewModel<PlaylistDetailsViewModel> apply(PlaylistAsyncViewModel<PlaylistDetailsViewModel> previous);
    }
}
