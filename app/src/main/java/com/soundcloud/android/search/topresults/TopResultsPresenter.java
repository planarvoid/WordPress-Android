package com.soundcloud.android.search.topresults;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.TrackingStateProvider;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Entity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.search.SearchTracker;
import com.soundcloud.android.search.topresults.TopResults.Bucket;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class TopResultsPresenter {

    private static final Screen SCREEN = Screen.SEARCH_EVERYTHING;

    private final EventTracker eventTracker;
    private final SearchTracker searchTracker;
    private final TrackingStateProvider trackingStateProvider;
    private final TopResultsOperations operations;
    private final Scheduler scheduler;
    private final Navigator navigator;
    private final NavigationExecutor navigationExecutor;
    private final PlaybackFeedbackHelper playbackFeedbackHelper;
    private final SearchClickListener searchClickListener;
    private final CompositeDisposable viewSubscription = new CompositeDisposable();

    interface TopResultsView extends Consumer<ViewModel> {

        Observable<UiAction.Search> searchIntent();

        Observable<UiAction.Refresh> refreshIntent();

        Observable<UiAction.Enter> enterScreen();

        Observable<UiAction.TrackClick> trackClick();

        Observable<UiAction.PlaylistClick> playlistClick();

        Observable<UiAction.UserClick> userClick();

        Observable<UiAction.ViewAllClick> viewAllClick();

        Observable<UiAction.HelpClick> helpClick();

        void handleActionResult(ClickResultAction action);
    }

    @Inject
    TopResultsPresenter(EventTracker eventTracker,
                        SearchTracker searchTracker,
                        TrackingStateProvider trackingStateProvider,
                        TopResultsOperations operations,
                        @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                        Navigator navigator,
                        NavigationExecutor navigationExecutor,
                        PlaybackFeedbackHelper playbackFeedbackHelper,
                        SearchClickListener searchClickListener) {
        this.eventTracker = eventTracker;
        this.searchTracker = searchTracker;
        this.trackingStateProvider = trackingStateProvider;
        this.operations = operations;
        this.scheduler = scheduler;
        this.navigator = navigator;
        this.navigationExecutor = navigationExecutor;
        this.playbackFeedbackHelper = playbackFeedbackHelper;
        this.searchClickListener = searchClickListener;
    }

    void attachView(TopResultsView topResultsView) {
        final Observable<ViewModel> viewModel = Observable.merge(topResultsView.searchIntent().map(UiAction.Search::searchParams).doOnNext(this::trackFirstSearch),
                                                                 topResultsView.refreshIntent().map(UiAction.Refresh::searchParams))
                                                          .flatMap(operations::search)
                                                          .scan(ViewModel.empty(), ViewModel::with)
                                                          .distinctUntilChanged()
                                                          .share()
                                                          .subscribeOn(scheduler)
                                                          .observeOn(AndroidSchedulers.mainThread());

        final Observable<TopResultsViewModel> searchResultModel = viewModel.filter(item -> item.data().isPresent()).map(item -> item.data().get());
        //View model subscription
        viewSubscription.add(viewModel.subscribe(topResultsView));

        //Handling enter screen event and tracking of page view, emits only when new enter screen event is emitted
        viewSubscription.add(Observable.combineLatest(topResultsView.enterScreen(), searchResultModel, (action, model) -> Pair.of(action, model.queryUrn()))
                                       .distinctUntilChanged(Pair::first)
                                       .subscribe(this::trackPageView));

        //Handling clicks
        viewSubscription.addAll(topResultsView.trackClick()
                                              .withLatestFrom(searchResultModel, this::toTrackClickParams)
                                              .flatMap(searchClickListener::trackClickToPlaybackResult)
                                              .compose(showErrorFromPlaybackResult())
                                              .subscribe(topResultsView::handleActionResult),
                                topResultsView.playlistClick()
                                              .withLatestFrom(searchResultModel, this::toClickParams)
                                              .doOnNext(this::trackClickOnItem)
                                              .map(searchClickListener::playlistClickToNavigateAction)
                                              .subscribe(topResultsView::handleActionResult),
                                topResultsView.userClick()
                                              .withLatestFrom(searchResultModel, this::toClickParams)
                                              .doOnNext(this::trackClickOnItem)
                                              .map(searchClickListener::userClickToNavigateAction)
                                              .subscribe(topResultsView::handleActionResult),
                                topResultsView.viewAllClick()
                                              .withLatestFrom(searchResultModel.map(TopResultsViewModel::queryUrn), this::toAction)
                                              .subscribe(topResultsView::handleActionResult),
                                topResultsView.helpClick()
                                              .doOnNext(click -> searchTracker.trackResultsUpsellClick(SCREEN))
                                              .map(click -> this.helpClickToNavigateAction())
                                              .subscribe(topResultsView::handleActionResult));
    }

    void detachView() {
        viewSubscription.clear();
    }

    private ObservableTransformer<PlaybackResult, ClickResultAction> showErrorFromPlaybackResult() {
        return trackClick -> trackClick.filter(playbackResult -> !playbackResult.isSuccess())
                                       .map(playbackResult -> context -> playbackFeedbackHelper.showFeedbackOnPlaybackError(playbackResult.getErrorReason()));
    }

    private ClickResultAction helpClickToNavigateAction() {
        return context -> navigationExecutor.openUpgrade(context, UpsellContext.PREMIUM_CONTENT);
    }

    private void trackClickOnItem(SearchClickListener.ClickParams clickParams) {
        eventTracker.trackSearch(SearchEvent.tapItemOnScreen(clickParams.screen(), clickParams.searchQuerySourceInfo(), clickParams.clickSource()));
    }

    private void trackPageView(Pair<UiAction.Enter, Optional<Urn>> searchQueryPair) {
        eventTracker.trackScreen(ScreenEvent.create(SCREEN.get(), new SearchQuerySourceInfo(searchQueryPair.second().or(Urn.NOT_SET), searchQueryPair.first().searchQuery())),
                                 trackingStateProvider.getLastEvent());
    }

    private void trackFirstSearch(SearchParams params) {
        searchTracker.trackSearchFormulationEnd(SCREEN, params.userQuery(), params.queryUrn(), params.queryPosition());
    }

    private SearchClickListener.TrackClickParams toTrackClickParams(UiAction.TrackClick trackClick, TopResultsViewModel viewModel) {
        final SearchItem.Track clickedItem = trackClick.clickedTrack();
        final List<TopResultsBucketViewModel> buckets = viewModel.buckets();
        final Optional<TopResultsBucketViewModel> currentBucket = buckets.isEmpty() ? Optional.absent() : Optional.of(buckets.get(clickedItem.bucketPosition()));
        final List<PlayableItem> playableItems = buckets.isEmpty() ? Lists.newArrayList(clickedItem.trackItem()) : filterPlayableItems(viewModel.buckets());
        final int position = playableItems.indexOf(clickedItem.trackItem());

        final List<Urn> transform = transform(playableItems, Entity::getUrn);
        final Bucket.Kind kind = currentBucket.transform(TopResultsBucketViewModel::kind).or(Bucket.Kind.TRACKS);
        return SearchClickListener.TrackClickParams.create(clickedItem.trackItem().getUrn(),
                                                           trackClick.searchQuery(),
                                                           viewModel.queryUrn(),
                                                           position,
                                                           getContextFromBucket(kind, position),
                                                           SCREEN,
                                                           kind.toClickSource(),
                                                           transform);
    }


    private SearchClickListener.ClickParams toClickParams(UiAction.PlaylistClick clickAction, TopResultsViewModel viewModel) {
        return toClickParams(viewModel, clickAction.clickedPlaylist(), clickAction.clickedPlaylist().playlistItem().getUrn(), clickAction.searchQuery());
    }

    private SearchClickListener.ClickParams toClickParams(UiAction.UserClick clickAction, TopResultsViewModel viewModel) {
        return toClickParams(viewModel, clickAction.clickedUser(), clickAction.clickedUser().userItem().getUrn(), clickAction.searchQuery());
    }

    private SearchClickListener.ClickParams toClickParams(TopResultsViewModel viewModel, SearchItem searchItem, Urn urn, String searchQuery) {
        final List<TopResultsBucketViewModel> buckets = viewModel.buckets();
        final TopResultsBucketViewModel itemBucket = buckets.get(searchItem.bucketPosition());
        int position = getPosition(searchItem, itemBucket, buckets);
        return SearchClickListener.ClickParams.create(urn,
                                                      searchQuery,
                                                      viewModel.queryUrn(),
                                                      position,
                                                      getContextFromBucket(itemBucket.kind(), position),
                                                      SCREEN,
                                                      itemBucket.kind().toClickSource());
    }

    private ClickResultAction toAction(UiAction.ViewAllClick viewAllClick, Optional<Urn> queryUrn) {
        final boolean isPremium = viewAllClick.bucketKind() == Bucket.Kind.GO_TRACKS;
        return activity -> navigator.navigateTo(NavigationTarget.forSearchViewAll(activity, queryUrn, viewAllClick.searchQuery(), viewAllClick.bucketKind(), isPremium));
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
