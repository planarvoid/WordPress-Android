package com.soundcloud.android.search.topresults;

import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.TrackingStateProvider;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.associations.FollowingStateProvider;
import com.soundcloud.android.associations.FollowingStatuses;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.rx.observers.LambdaSubscriber;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.search.SearchPlayQueueFilter;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.collection.CollectionLoader;
import com.soundcloud.android.utils.collection.CollectionLoaderState;
import com.soundcloud.android.view.collection.CollectionRendererState;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.List;

public class TopResultsPresenter {

    interface TopResultsView {

        Observable<Pair<String, Optional<Urn>>> searchIntent();

        Observable<Void> refreshIntent();

        Observable<Void> enterScreen();

    }

    private final BehaviorSubject<TopResultsViewModel> viewModel = BehaviorSubject.create();
    private final PublishSubject<SearchItem> searchItemClicked = PublishSubject.create();
    private final PublishSubject<PlaybackResult.ErrorReason> playbackError = PublishSubject.create();
    private final PublishSubject<TopResultsViewAllArgs> viewAllClicked = PublishSubject.create();

    private final Subscription loaderSubscription;
    private CompositeSubscription viewSubscription = new CompositeSubscription();
    private final SearchPlayQueueFilter playQueueFilter;
    private final PlaybackInitiator playbackInitiator;
    private final EventTracker eventTracker;
    private final EventBus eventBus;
    private final EntityItemCreator entityItemCreator;
    private final TrackingStateProvider trackingStateProvider;

    private final BehaviorSubject<Pair<String, Optional<Urn>>> firstPageIntent = BehaviorSubject.create();
    private final BehaviorSubject<Pair<String, Optional<Urn>>> refreshIntent = BehaviorSubject.create();

    @Inject
    TopResultsPresenter(TopResultsOperations operations,
                        SearchPlayQueueFilter playQueueFilter,
                        LikesStateProvider likedStatuses,
                        FollowingStateProvider followingStatuses,
                        PlaybackInitiator playbackInitiator,
                        EventTracker eventTracker,
                        EventBus eventBus,
                        EntityItemCreator entityItemCreator,
                        TrackingStateProvider trackingStateProvider) {
        this.playQueueFilter = playQueueFilter;
        this.playbackInitiator = playbackInitiator;
        this.eventTracker = eventTracker;
        this.eventBus = eventBus;
        this.entityItemCreator = entityItemCreator;
        this.trackingStateProvider = trackingStateProvider;

        CollectionLoader<ApiTopResults, Pair<String, Optional<Urn>>> loader = new CollectionLoader<>(
                firstPageIntent,
                operations::search,
                refreshIntent,
                operations::search);

        final Observable<TopResultsViewModel> rObservable = Observable.combineLatest(loader.pages(),
                                                                                     likedStatuses.likedStatuses(),
                                                                                     followingStatuses.followingStatuses(),
                                                                                     this::transformBucketsToViewModel);

        loaderSubscription = rObservable.subscribe(viewModel);
    }

    public PublishSubject<SearchItem> searchItemClicked() {
        return searchItemClicked;
    }

    PublishSubject<TopResultsViewAllArgs> viewAllClicked() {
        return viewAllClicked;
    }

    private Observable<PlaybackResult> playTrack(SearchItem clickedItem, String searchQuery, TopResultsViewModel viewModel) {
        final List<TopResultsBucketViewModel> buckets = viewModel.buckets().items();
        final SearchItem.Track trackSearchItem = (SearchItem.Track) clickedItem;
        final TopResultsBucketViewModel currentBucket = buckets.get(clickedItem.bucketPosition());
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(viewModel.queryUrn().or(Urn.NOT_SET),
                                                                                      getPosition(clickedItem, currentBucket, buckets),
                                                                                      trackSearchItem.trackItem().getUrn(),
                                                                                      searchQuery);
        final List<TrackItem> tracks = Lists.newArrayList(Iterables.transform(filter(currentBucket.items(),
                                                                                     SearchItem.Track.class),
                                                                              SearchItem.Track::trackItem));
        final int position = tracks.indexOf(trackSearchItem.trackItem());

        List<TrackItem> adjustedQueue = playQueueFilter.correctQueue(tracks, position);
        int adjustedPosition = playQueueFilter.correctPosition(position);

        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SEARCH_EVERYTHING);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);


        List<Urn> transform = transform(adjustedQueue, TrackItem::getUrn);
        return playbackInitiator.playTracks(transform, adjustedPosition, playSessionSource)
                                .doOnNext(args -> eventTracker.trackSearch(SearchEvent.tapItemOnScreen(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo, currentBucket.kind().toClickSource())));
    }

    Observable<GoToItemArgs> onGoToPlaylist() {
        return searchItemClicked.filter(clickedItem -> clickedItem.kind() == SearchItem.Kind.PLAYLIST).flatMap(this::onSearchItemClicked);
    }

    Observable<GoToItemArgs> onGoToProfile() {
        return searchItemClicked.filter(clickedItem -> clickedItem.kind() == SearchItem.Kind.USER).flatMap(this::onSearchItemClicked);
    }

    private Observable<GoToItemArgs> onSearchItemClicked(SearchItem clickedItem) {
        return Observable.combineLatest(firstPageIntent.map(Pair::first), viewModel, (searchQuery, viewModel) -> toGoToItemArgs(clickedItem, searchQuery, viewModel))
                         .doOnNext(args -> eventTracker.trackSearch(SearchEvent.tapItemOnScreen(Screen.SEARCH_EVERYTHING, args.searchQuerySourceInfo(), args.clickSource())));
    }

    Observable<TopResultsViewAllArgs> goToViewAllPage() {
        return viewAllClicked.flatMap(clickedItem -> Observable.combineLatest(firstPageIntent,
                                                                              viewModel,
                                                                              (searchQuery, viewModel) -> clickedItem.copyWithSearchQuery(searchQuery.first(), viewModel.queryUrn())));
    }

    void attachView(TopResultsView topResultsView) {
        viewSubscription = new CompositeSubscription();

        viewSubscription.addAll(
                topResultsView.searchIntent()
                              .subscribe(firstPageIntent),
                topResultsView.refreshIntent()
                              .flatMap(ignored -> topResultsView.searchIntent())
                              .subscribe(refreshIntent),
                topResultsView.enterScreen()
                              .flatMap(event -> Observable.combineLatest(firstPageIntent.map(Pair::first),
                                                                         viewModel.map(TopResultsViewModel::queryUrn).filter(Optional::isPresent).map(Optional::get),
                                                                         Pair::of))
                              .subscribe(LambdaSubscriber.onNext(this::trackPageView)),
                searchItemClicked.filter(clickedItem -> clickedItem.kind() == SearchItem.Kind.TRACK)
                                 .flatMap(clickedItem -> firstPageIntent.flatMap(searchQueryPair -> viewModel.flatMap(viewModel -> playTrack(clickedItem, searchQueryPair.first(), viewModel))))
                                 .subscribe(LambdaSubscriber.onNext(this::showPlaybackResult))

        );
    }

    private void trackPageView(Pair<String, Urn> searchQueryPair) {
        eventTracker.trackScreen(ScreenEvent.create(Screen.SEARCH_EVERYTHING.get(), new SearchQuerySourceInfo(searchQueryPair.second(), searchQueryPair.first())),
                                 trackingStateProvider.getLastEvent());
    }

    private void showPlaybackResult(PlaybackResult playbackResult) {
        if (playbackResult.isSuccess()) {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
        } else {
            playbackError.onNext(playbackResult.getErrorReason());
        }
    }

    @NonNull
    private TopResultsViewModel transformBucketsToViewModel(
            CollectionLoaderState<ApiTopResults> collectionLoaderState,
            LikedStatuses likedStatuses,
            FollowingStatuses followingStatuses) {

        Optional<Urn> queryUrn = Optional.absent();
        final List<TopResultsBucketViewModel> viewModelItems = Lists.newArrayList();
        if (collectionLoaderState.data().isPresent()) {
            final ApiTopResults apiTopResults = collectionLoaderState.data().get();
            final ModelCollection<ApiTopResultsBucket> resultCollection = apiTopResults.buckets();
            queryUrn = resultCollection.getQueryUrn();
            final List<ApiTopResultsBucket> apiTopResultsBuckets = Lists.newArrayList(Iterables.filter(resultCollection.getCollection(),
                                                                                                       bucket -> TopResultsBucketViewModel.isValidBucketUrn(bucket.urn().toString())));
            for (ApiTopResultsBucket apiBucket : apiTopResultsBuckets) {
                final List<ApiUniversalSearchItem> apiUniversalSearchItems = apiBucket.collection().getCollection();
                final int bucketPosition = apiTopResultsBuckets.indexOf(apiBucket);
                final List<SearchItem> result = SearchItemHelper.transformApiSearchItems(entityItemCreator, likedStatuses, followingStatuses, apiUniversalSearchItems, bucketPosition);
                viewModelItems.add(TopResultsBucketViewModel.create(result, apiBucket.urn(), apiBucket.totalResults()));
            }
        }
        return TopResultsViewModel.create(queryUrn, CollectionRendererState.create(collectionLoaderState.collectionLoadingState(), viewModelItems));
    }

    void detachView() {
        viewSubscription.unsubscribe();
        loaderSubscription.unsubscribe();
    }

    BehaviorSubject<TopResultsViewModel> viewModel() {
        return viewModel;
    }

    Observable<PlaybackResult.ErrorReason> playbackError() {
        return playbackError;
    }

    private GoToItemArgs toGoToItemArgs(SearchItem searchItem, String searchQuery, TopResultsViewModel viewModel) {

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

    private EventContextMetadata getEventContextMetadata(TopResultsBucketViewModel.Kind bucketKind, int positionInBucket) {
        final EventContextMetadata.Builder builder = EventContextMetadata.builder()
                                                                         .invokerScreen(ScreenElement.LIST.get())
                                                                         .contextScreen(Screen.SEARCH_EVERYTHING.get())
                                                                         .pageName(Screen.SEARCH_EVERYTHING.get())
                                                                         .module(getContextFromBucket(bucketKind, positionInBucket));
        return builder.build();
    }

    private Module getContextFromBucket(TopResultsBucketViewModel.Kind bucketKind, int positionInBucket) {
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
