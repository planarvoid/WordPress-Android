package com.soundcloud.android.search.topresults;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.TrackingStateProvider;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Entity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.rx.observers.LambdaSubscriber;
import com.soundcloud.android.search.SearchPlayQueueFilter;
import com.soundcloud.android.search.SearchTracker;
import com.soundcloud.android.search.topresults.TopResults.Bucket;
import com.soundcloud.android.view.collection.CollectionRendererState;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import java.util.List;

public class TopResultsPresenter {

    interface TopResultsView {

        Observable<SearchParams> searchIntent();

        Observable<SearchParams> refreshIntent();

        Observable<Void> enterScreen();

        Observable<SearchItem.Track> trackClick();

        Observable<SearchItem.Playlist> playlistClick();

        Observable<SearchItem.User> userClick();

        Observable<TopResults.Bucket.Kind> viewAllClick();

        Observable<Void> helpClick();

        String searchQuery();

        void navigateToPlaylist(GoToItemArgs goToItemArgs);

        void navigateToUser(GoToItemArgs goToItemArgs);

        void navigateToViewAll(TopResultsViewAllArgs topResultsViewAllArgs);

        void navigateToHelp();

        void renderNewState(CollectionRendererState<TopResultsBucketViewModel> newState);

        void showError(PlaybackResult.ErrorReason errorReason);
    }

    private final PublishSubject<PlaybackResult.ErrorReason> playbackError = PublishSubject.create();

    private final TopResultsDataSource dataSource;
    private final SearchPlayQueueFilter playQueueFilter;
    private final PlaybackInitiator playbackInitiator;
    private final EventTracker eventTracker;
    private final SearchTracker searchTracker;
    private final EventBus eventBus;
    private final TrackingStateProvider trackingStateProvider;

    private CompositeSubscription viewSubscription = new CompositeSubscription();

    @Inject
    TopResultsPresenter(TopResultsDataSource dataSource,
                        SearchPlayQueueFilter playQueueFilter,
                        PlaybackInitiator playbackInitiator,
                        EventTracker eventTracker,
                        SearchTracker searchTracker,
                        EventBus eventBus,
                        TrackingStateProvider trackingStateProvider) {
        this.dataSource = dataSource;
        this.playQueueFilter = playQueueFilter;
        this.playbackInitiator = playbackInitiator;
        this.eventTracker = eventTracker;
        this.searchTracker = searchTracker;
        this.eventBus = eventBus;
        this.trackingStateProvider = trackingStateProvider;
    }

    void attachView(TopResultsView topResultsView) {
        viewSubscription = new CompositeSubscription();
        viewSubscription.addAll(inputs(topResultsView));
        viewSubscription.addAll(
                dataSource.viewModel().map(TopResultsViewModel::buckets)
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(LambdaSubscriber.onNext(topResultsView::renderNewState)),
                playbackError.subscribe(LambdaSubscriber.onNext(topResultsView::showError)));
        viewSubscription.addAll(clicks(topResultsView, topResultsView.searchQuery()));
    }

    private Subscription[] inputs(TopResultsView topResultsView) {
        return new Subscription[]{
                dataSource.subscribe(topResultsView.searchIntent(), topResultsView.refreshIntent()),
                topResultsView.searchIntent()
                        .subscribe(LambdaSubscriber.onNext(this::trackFirstSearch)),
                topResultsView.enterScreen()
                              .flatMap(ignore -> dataSource.queryUrn().map(queryUrn -> Pair.of(topResultsView.searchQuery(), queryUrn)).take(1))
                        .subscribe(LambdaSubscriber.onNext(this::trackPageView))
        };
    }

    private Subscription[] clicks(TopResultsView topResultsView, String searchQuery) {
        return new Subscription[]{
                topResultsView.trackClick().withLatestFrom(dataSource.viewModel(), Pair::of)
                          .flatMap(pair -> playTrack(pair.first(), searchQuery, pair.second()))
                        .subscribe(LambdaSubscriber.onNext(this::showPlaybackResult)),
                topResultsView.playlistClick().withLatestFrom(dataSource.viewModel(), (clickedItem, viewModel) -> toGoToItemArgs(clickedItem, viewModel, searchQuery))
                        .subscribe(LambdaSubscriber.onNext(args -> onSearchItemClicked(topResultsView::navigateToPlaylist, args))),
                topResultsView.userClick().withLatestFrom(dataSource.viewModel(), (clickedItem, viewModel) -> toGoToItemArgs(clickedItem, viewModel, searchQuery))
                        .subscribe(LambdaSubscriber.onNext(args -> onSearchItemClicked(topResultsView::navigateToUser, args))),
                topResultsView.viewAllClick().withLatestFrom(dataSource.queryUrn(), TopResultsViewAllArgs::create)
                        .subscribe(LambdaSubscriber.onNext(topResultsView::navigateToViewAll)),
                topResultsView.helpClick().subscribe(LambdaSubscriber.onNext(ignore -> onHelpClicked(topResultsView)))
        };
    }

    private void onHelpClicked(TopResultsView topResultsView) {
        searchTracker.trackResultsUpsellClick(Screen.SEARCH_EVERYTHING);
        topResultsView.navigateToHelp();
    }

    private void onSearchItemClicked(Action1<GoToItemArgs> navigateAction, GoToItemArgs args) {
        eventTracker.trackSearch(SearchEvent.tapItemOnScreen(Screen.SEARCH_EVERYTHING, args.searchQuerySourceInfo(), args.clickSource()));
        navigateAction.call(args);
    }

    private void trackPageView(Pair<String, Optional<Urn>> searchQueryPair) {
        eventTracker.trackScreen(ScreenEvent.create(Screen.SEARCH_EVERYTHING.get(), new SearchQuerySourceInfo(searchQueryPair.second().or(Urn.NOT_SET), searchQueryPair.first())),
                                 trackingStateProvider.getLastEvent());
    }

    private void trackFirstSearch(SearchParams params) {
        searchTracker.trackSearchFormulationEnd(Screen.SEARCH_EVERYTHING, params.userQuery(), params.queryUrn(), params.queryPosition());
    }

    private void showPlaybackResult(PlaybackResult playbackResult) {
        if (playbackResult.isSuccess()) {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
        } else {
            playbackError.onNext(playbackResult.getErrorReason());
        }
    }

    void detachView() {
        viewSubscription.unsubscribe();
    }

    @VisibleForTesting
    Observable<TopResultsViewModel> viewModel() {
        return dataSource.viewModel();
    }

    @VisibleForTesting
    Observable<PlaybackResult.ErrorReason> playbackError() {
        return playbackError;
    }

    private Observable<PlaybackResult> playTrack(SearchItem clickedItem, String searchQuery, TopResultsViewModel viewModel) {
        final List<TopResultsBucketViewModel> buckets = viewModel.buckets().items();
        // TODO this should always be present, see https://soundcloud.atlassian.net/browse/DROID-1292
        final Optional<TopResultsBucketViewModel> currentBucket = buckets.isEmpty() ? Optional.absent() : Optional.of(buckets.get(clickedItem.bucketPosition()));
        final SearchItem.Track trackSearchItem = (SearchItem.Track) clickedItem;
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(viewModel.queryUrn().or(Urn.NOT_SET),
                                                                                      currentBucket.isPresent() ? getPosition(clickedItem, currentBucket.get(), buckets) : 0,
                                                                                      trackSearchItem.trackItem().getUrn(),
                                                                                      searchQuery);
        final List<PlayableItem> playableItems = buckets.isEmpty() ? Lists.newArrayList(trackSearchItem.trackItem()) : filterPlayableItems(viewModel.buckets().items());
        final int position = playableItems.indexOf(trackSearchItem.trackItem());

        List<PlayableItem> adjustedQueue = playQueueFilter.correctQueue(playableItems, position);
        int adjustedPosition = playQueueFilter.correctPosition(position);

        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SEARCH_EVERYTHING);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);


        List<Urn> transform = transform(adjustedQueue, Entity::getUrn);
        return playbackInitiator.playPosts(transform, trackSearchItem.itemUrn().get(), adjustedPosition, playSessionSource)
                                .doOnNext(args -> {
                                    if (currentBucket.isPresent()) {
                                        eventTracker.trackSearch(SearchEvent.tapItemOnScreen(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo, currentBucket.get().kind().toClickSource()));
                                    }
                                });
    }

    private GoToItemArgs toGoToItemArgs(SearchItem searchItem, TopResultsViewModel viewModel, String searchQuery) {

        final List<TopResultsBucketViewModel> buckets = viewModel.buckets().items();
        final TopResultsBucketViewModel itemBucket = buckets.get(searchItem.bucketPosition());
        int position = getPosition(searchItem, itemBucket, buckets);
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(viewModel.queryUrn().or(Urn.NOT_SET), position, searchItem.itemUrn().get(), searchQuery);
        return GoToItemArgs.create(searchQuerySourceInfo, searchItem.itemUrn().get(), getEventContextMetadata(itemBucket.kind(), position), itemBucket.kind().toClickSource());
    }

    private int getPosition(SearchItem searchItem, TopResultsBucketViewModel itemBucket, List<TopResultsBucketViewModel> buckets) {
        int position = itemBucket.items().indexOf(searchItem);
        for (TopResultsBucketViewModel bucket : buckets) {
            if (bucket.equals(itemBucket)) {
                break;
            }
            position += bucket.items().size();
        }
        return position;
    }

    private List<PlayableItem> filterPlayableItems(List<TopResultsBucketViewModel> buckets) {
        List<PlayableItem> playables = Lists.newArrayList();
        for (TopResultsBucketViewModel bucket : buckets) {
            for (SearchItem item : bucket.items()) {
                if (item.kind() == SearchItem.Kind.PLAYLIST) {
                    playables.add(((SearchItem.Playlist) item).playlistItem());
                } else if (item.kind() == SearchItem.Kind.TRACK) {
                    playables.add(((SearchItem.Track) item).trackItem());
                }
            }
        }
        return playables;
    }

    private EventContextMetadata getEventContextMetadata(Bucket.Kind bucketKind, int positionInBucket) {
        final EventContextMetadata.Builder builder = EventContextMetadata.builder()
                                                                         .pageName(Screen.SEARCH_EVERYTHING.get())
                                                                         .module(getContextFromBucket(bucketKind, positionInBucket));
        return builder.build();
    }

    private Module getContextFromBucket(Bucket.Kind bucketKind, int positionInBucket) {
        switch (bucketKind) {
            case TOP_RESULT:
                return Module.create(Module.SEARCH_TOP_RESULT, positionInBucket);
            case TRACKS:
                return Module.create(Module.SEARCH_TRACKS, positionInBucket);
            case GO_TRACKS:
                return Module.create(Module.SEARCH_HIGH_TIER, positionInBucket);
            case USERS:
                return Module.create(Module.SEARCH_PEOPLE, positionInBucket);
            case PLAYLISTS:
                return Module.create(Module.SEARCH_PLAYLISTS, positionInBucket);
            case ALBUMS:
                return Module.create(Module.SEARCH_ALBUMS, positionInBucket);
            default:
                throw new IllegalArgumentException("Unexpected bucket type");
        }
    }
}
