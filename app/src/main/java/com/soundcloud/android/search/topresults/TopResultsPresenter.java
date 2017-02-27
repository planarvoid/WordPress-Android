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
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.rx.observers.LambdaSubscriber;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.search.SearchPlayQueueFilter;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
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
import java.util.ArrayList;
import java.util.List;

public class TopResultsPresenter {

    interface TopResultsView {

        Observable<Pair<String, Optional<Urn>>> searchIntent();

        Observable<Void> refreshIntent();
    }

    private final BehaviorSubject<TopResultsViewModel> viewModel = BehaviorSubject.create();
    private final PublishSubject<SearchItem> searchItemClicked = PublishSubject.create();
    private final PublishSubject<PlaybackResult.ErrorReason> playbackError = PublishSubject.create();

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


    private Observable<PlaybackResult> playTrack(SearchItem clickedItem, Pair<String, Optional<Urn>> searchQueryPair, TopResultsViewModel viewModel) {
        final int bucketPosition = clickedItem.bucketPosition();
        final TopResultsBucketViewModel currentBucket = viewModel.buckets().items().get(bucketPosition);
        final String query = searchQueryPair.first();
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(currentBucket.queryUrn(),
                                                                                      bucketPosition,
                                                                                      clickedItem.urn(),
                                                                                      query);
        final List<TrackItem> tracks = Lists.newArrayList(Iterables.transform(filter(currentBucket.items(),
                                                                                     SearchItem.Track.class),
                                                                              SearchItem.Track::trackItem));
        final int position = tracks.indexOf(((SearchItem.Track) clickedItem).trackItem());

        List<TrackItem> adjustedQueue = playQueueFilter.correctQueue(tracks, position);
        int adjustedPosition = playQueueFilter.correctPosition(position);

        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SEARCH_TOP_RESULTS);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);


        List<Urn> transform = transform(adjustedQueue, TrackItem::getUrn);
        return playbackInitiator.playTracks(transform, adjustedPosition, playSessionSource);
    }

    public Observable<GoToPlaylistArgs> onGoToPlaylist() {
        return searchItemClicked.filter(clickedItem -> clickedItem.kind() == SearchItem.Kind.PLAYLIST)
                                .map(this::addSearchQuery)
                                .map(this::toGoToPlaylistArgs);
    }

    public Observable<GoToProfileArgs> onGoToProfile() {
        return searchItemClicked.filter(clickedItem -> clickedItem.kind() == SearchItem.Kind.USER)
                                .map(this::addSearchQuery)
                                .map(this::toGoToProfileArgs);
    }

    private Pair<SearchItem, String> addSearchQuery(SearchItem searchItem) {
        return Pair.of(searchItem, firstPageIntent.getValue().first());
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

    @NonNull
    static Function<ApiTopResultsBucket, TopResultsBucketViewModel> transformBucket(CollectionViewState<ApiTopResultsBucket> collectionViewState,
                                                                                    LikedStatuses likedStatuses,
                                                                                    FollowingStatuses followingStatuses) {
        return apiBucket -> {
            List<ApiUniversalSearchItem> apiUniversalSearchItems = apiBucket.collection().getCollection();
            List<SearchItem> result = new ArrayList<>(apiUniversalSearchItems.size());
            int index = collectionViewState.items().indexOf(apiBucket);

            for (ApiUniversalSearchItem searchItem : apiUniversalSearchItems) {
                if (searchItem.track().isPresent()) {
                    result.add(SearchItem.Track.create(createTrackItem(searchItem, likedStatuses), index));

                } else if (searchItem.playlist().isPresent()) {
                    result.add(SearchItem.Playlist.create(createPlaylistItem(searchItem, likedStatuses), index));

                } else if (searchItem.user().isPresent()) {
                    result.add(SearchItem.User.create(createUserItem(searchItem, followingStatuses), index));
                }
            }
            return TopResultsBucketViewModel.create(result, apiBucket.urn(), apiBucket.totalResults(), apiBucket.queryUrn().or(Urn.NOT_SET));
        };
    }

    private static UserItem createUserItem(ApiUniversalSearchItem searchItem, FollowingStatuses followingStatuses) {
        return UserItem.from(searchItem.user().get(), followingStatuses.isFollowed(searchItem.user().get().getUrn()));
    }

    private static PlaylistItem createPlaylistItem(ApiUniversalSearchItem searchItem, LikedStatuses likedStatuses) {
        return PlaylistItem.fromLiked(searchItem.playlist().get(), likedStatuses.isLiked(searchItem.playlist().get().getUrn()));
    }

    private static TrackItem createTrackItem(ApiUniversalSearchItem searchItem, LikedStatuses likedStatuses) {
        return TrackItem.fromLiked(searchItem.track().get(), likedStatuses.isLiked(searchItem.track().get().getUrn()));
    }

    private GoToPlaylistArgs toGoToPlaylistArgs(Pair<SearchItem, String> data) {
        final SearchItem.Playlist clickedItem = (SearchItem.Playlist) data.first();
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(viewModel.getValue().buckets().items().get(clickedItem.bucketPosition()).queryUrn(),
                                                                                      clickedItem.bucketPosition(),
                                                                                      clickedItem.urn(),
                                                                                      data.second());
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

    private GoToProfileArgs toGoToProfileArgs(Pair<SearchItem, String> data) {
        final SearchItem.User clickedItem = (SearchItem.User) data.first();
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(viewModel.getValue().buckets().items().get(clickedItem.bucketPosition()).queryUrn(),
                                                                                      clickedItem.bucketPosition(),
                                                                                      clickedItem.urn(),
                                                                                      data.second());
        return GoToProfileArgs.create(searchQuerySourceInfo, clickedItem.userItem().getUrn());
    }


}
