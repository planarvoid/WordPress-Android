package com.soundcloud.android.search.topresults;

import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.associations.FollowingStateProvider;
import com.soundcloud.android.associations.FollowingStatuses;
import com.soundcloud.android.events.AttributingActivity;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LinkType;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.rx.observers.LambdaSubscriber;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.search.SearchPlayQueueFilter;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.CollectionLoader;
import com.soundcloud.android.view.adapters.CollectionViewState;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
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
    }

    private final BehaviorSubject<TopResultsViewModel> viewModel = BehaviorSubject.create();
    private final PublishSubject<SearchItem> searchItemClicked = PublishSubject.create();
    private final PublishSubject<PlaybackResult.ErrorReason> playbackError = PublishSubject.create();
    private final PublishSubject<TopResultsViewAllArgs> viewAllClicked = PublishSubject.create();

    private final Subscription loaderSubscription;
    private CompositeSubscription viewSubscription = new CompositeSubscription();
    private final SearchPlayQueueFilter playQueueFilter;
    private final PlaybackInitiator playbackInitiator;
    private final EventBus eventBus;

    private final BehaviorSubject<Pair<String, Optional<Urn>>> firstPageIntent = BehaviorSubject.create();
    private final BehaviorSubject<Pair<String, Optional<Urn>>> refreshIntent = BehaviorSubject.create();

    @Inject
    TopResultsPresenter(TopResultsOperations operations,
                        SearchPlayQueueFilter playQueueFilter,
                        LikesStateProvider likedStatuses,
                        FollowingStateProvider followingStatuses,
                        PlaybackInitiator playbackInitiator,
                        EventBus eventBus) {
        this.playQueueFilter = playQueueFilter;
        this.playbackInitiator = playbackInitiator;
        this.eventBus = eventBus;

        CollectionLoader<ApiTopResultsBucket, Pair<String, Optional<Urn>>> loader = new CollectionLoader<>(
                firstPageIntent,
                operations::search,
                refreshIntent,
                operations::search);

        loaderSubscription = Observable.combineLatest(loader.pages(),
                                                      likedStatuses.likedStatuses(),
                                                      followingStatuses.followingStatuses(),
                                                      this::transformBucketToViewModel)
                                       .subscribe(viewModel);
    }

    public PublishSubject<SearchItem> searchItemClicked() {
        return searchItemClicked;
    }

    public PublishSubject<TopResultsViewAllArgs> viewAllClicked() {
        return viewAllClicked;
    }

    private Observable<PlaybackResult> playTrack(SearchItem clickedItem, Pair<String, Optional<Urn>> searchQueryPair, TopResultsViewModel viewModel) {
        final SearchItem.Track trackSearchItem = (SearchItem.Track) clickedItem;
        final int bucketPosition = clickedItem.bucketPosition();
        final TopResultsBucketViewModel currentBucket = viewModel.buckets().items().get(bucketPosition);
        final String query = searchQueryPair.first();
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(currentBucket.queryUrn(),
                                                                                      bucketPosition,
                                                                                      trackSearchItem.trackItem().getUrn(),
                                                                                      query);
        final List<TrackItem> tracks = Lists.newArrayList(Iterables.transform(filter(currentBucket.items(),
                                                                                     SearchItem.Track.class),
                                                                              SearchItem.Track::trackItem));
        final int position = tracks.indexOf(trackSearchItem.trackItem());

        List<TrackItem> adjustedQueue = playQueueFilter.correctQueue(tracks, position);
        int adjustedPosition = playQueueFilter.correctPosition(position);

        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SEARCH_TOP_RESULTS);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);


        List<Urn> transform = transform(adjustedQueue, TrackItem::getUrn);
        return playbackInitiator.playTracks(transform, adjustedPosition, playSessionSource);
    }

    public Observable<GoToPlaylistArgs> onGoToPlaylist() {
        return searchItemClicked.filter(clickedItem -> clickedItem.kind() == SearchItem.Kind.PLAYLIST)
                                .flatMap(clickedItem -> firstPageIntent.map(searchQuery -> toGoToPlaylistArgs(clickedItem, searchQuery.first())));
    }

    public Observable<GoToProfileArgs> onGoToProfile() {
        return searchItemClicked.filter(clickedItem -> clickedItem.kind() == SearchItem.Kind.USER)
                                .flatMap(clickedItem -> firstPageIntent.map(searchQuery -> toGoToProfileArgs(clickedItem, searchQuery.first())));
    }

    public Observable<TopResultsViewAllArgs> goToViewAllPage() {
        return viewAllClicked.flatMap(clickedItem -> firstPageIntent.map(query -> clickedItem.copyWithSearchQuery(query.first())));
    }

    void attachView(TopResultsView topResultsView) {
        viewSubscription = new CompositeSubscription();

        viewSubscription.addAll(
                topResultsView.searchIntent()
                              .subscribe(firstPageIntent),
                topResultsView.refreshIntent()
                              .flatMap(ignored -> topResultsView.searchIntent())
                              .subscribe(refreshIntent),

                searchItemClicked.filter(clickedItem -> clickedItem.kind() == SearchItem.Kind.TRACK)
                                 .flatMap(clickedItem ->
                                                  firstPageIntent.flatMap(searchQueryPair ->
                                                                                  viewModel.flatMap(viewModel ->
                                                                                                            playTrack(clickedItem, searchQueryPair, viewModel))))
                                 .subscribe(LambdaSubscriber.onNext(showPlaybackResult()))

        );
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

    @NonNull
    private TopResultsViewModel transformBucketToViewModel(
            CollectionViewState<ApiTopResultsBucket> collectionViewState,
            LikedStatuses likedStatuses,
            FollowingStatuses followingStatuses) {

        return TopResultsViewModel.create(collectionViewState.withNewType(transformBucket(collectionViewState, likedStatuses, followingStatuses)));
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

    private Function<ApiTopResultsBucket, TopResultsBucketViewModel> transformBucket(CollectionViewState<ApiTopResultsBucket> collectionViewState,
                                                                                     LikedStatuses likedStatuses,
                                                                                     FollowingStatuses followingStatuses) {
        return apiBucket -> {
            final List<ApiUniversalSearchItem> apiUniversalSearchItems = apiBucket.collection().getCollection();
            final int bucketPosition = collectionViewState.items().indexOf(apiBucket);
            final List<SearchItem> result = SearchItemHelper.transformApiSearchItems(likedStatuses, followingStatuses, apiUniversalSearchItems, bucketPosition);
            return TopResultsBucketViewModel.create(result, apiBucket.urn(), apiBucket.totalResults(), apiBucket.queryUrn().or(Urn.NOT_SET));
        };
    }

    private GoToPlaylistArgs toGoToPlaylistArgs(SearchItem searchItem, String query) {
        final SearchItem.Playlist clickedItem = (SearchItem.Playlist) searchItem;
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(viewModel.getValue().buckets().items().get(clickedItem.bucketPosition()).queryUrn(),
                                                                                      clickedItem.bucketPosition(),
                                                                                      clickedItem.playlistItem().getUrn(),
                                                                                      query);
        return GoToPlaylistArgs.create(searchQuerySourceInfo,
                                       clickedItem.playlistItem().getUrn(),
                                       getEventContextMetadata(clickedItem.playlistItem().creatorUrn()));
    }

    private EventContextMetadata getEventContextMetadata(Urn creatorUrn) {
        final EventContextMetadata.Builder builder = EventContextMetadata.builder()
                                                                         .invokerScreen(ScreenElement.LIST.get())
                                                                         .contextScreen(Screen.SEARCH_TOP_RESULTS.get())
                                                                         .pageName(Screen.SEARCH_TOP_RESULTS.get())
                                                                         .attributingActivity(AttributingActivity.create(AttributingActivity.POSTED, Optional.of(creatorUrn)))
                                                                         .linkType(LinkType.SELF);

        return builder.build();
    }

    private GoToProfileArgs toGoToProfileArgs(SearchItem searchItem, String query) {
        final SearchItem.User clickedItem = (SearchItem.User) searchItem;
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(viewModel.getValue().buckets().items().get(clickedItem.bucketPosition()).queryUrn(),
                                                                                      clickedItem.bucketPosition(),
                                                                                      clickedItem.userItem().getUrn(),
                                                                                      query);
        return GoToProfileArgs.create(searchQuerySourceInfo, clickedItem.userItem().getUrn());
    }
}
