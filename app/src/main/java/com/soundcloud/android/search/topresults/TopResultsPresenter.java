package com.soundcloud.android.search.topresults;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.TrackingStateProvider;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.search.SearchTracker;
import com.soundcloud.android.utils.collection.AsyncLoader;
import com.soundcloud.android.utils.collection.AsyncLoaderState;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observables.ConnectableObservable;

import javax.inject.Inject;

public class TopResultsPresenter {

    private static final Screen SCREEN = Screen.SEARCH_EVERYTHING;

    private final EventTracker eventTracker;
    private final SearchTracker searchTracker;
    private final TrackingStateProvider trackingStateProvider;
    private final TopResultsOperations operations;
    private final CompositeDisposable viewSubscription = new CompositeDisposable();
    private final PlaybackInitiator playbackInitiator;
    private final TopResultsMapper topResultsMapper;

    interface TopResultsView extends Consumer<AsyncLoaderState<TopResultsViewModel>> {

        Observable<UiAction.Search> searchIntent();

        Observable<UiAction.Refresh> refreshIntent();

        Observable<UiAction.Enter> enterScreen();

        Observable<UiAction.TrackClick> trackClick();

        Observable<UiAction.PlaylistClick> playlistClick();

        Observable<UiAction.UserClick> userClick();

        Observable<UiAction.ViewAllClick> viewAllClick();

        Observable<UiAction.HelpClick> helpClick();

        void showPlaybackResult(PlaybackResult playbackResult);

        void navigateTo(NavigationTarget navigationTarget);

        void openUpgrade(UpsellContext upsellContext);
    }

    @Inject
    TopResultsPresenter(EventTracker eventTracker,
                        SearchTracker searchTracker,
                        TrackingStateProvider trackingStateProvider,
                        TopResultsOperations operations,
                        PlaybackInitiator playbackInitiator,
                        TopResultsMapper topResultsMapper) {

        this.eventTracker = eventTracker;
        this.searchTracker = searchTracker;
        this.trackingStateProvider = trackingStateProvider;
        this.operations = operations;
        this.playbackInitiator = playbackInitiator;
        this.topResultsMapper = topResultsMapper;
    }

    void attachView(TopResultsView topResultsView) {
        ConnectableObservable<SearchParams> sharedSearchIntent = topResultsView.searchIntent().map(UiAction.Search::searchParams).publish();
        viewSubscription.add(sharedSearchIntent.take(1).subscribe(this::trackFirstSearch));
        Observable<AsyncLoaderState<TopResultsViewModel>> stateWithViewModel = AsyncLoader.startWith(sharedSearchIntent.autoConnect(), this::search)
                                                                                          .withRefresh(refreshIntent(topResultsView), this::search)
                                                                                          .build()
                                                                                          .publish()
                                                                                          .autoConnect()
                                                                                          .observeOn(AndroidSchedulers.mainThread());



        //View model subscription
        viewSubscription.add(stateWithViewModel.subscribe(topResultsView));


        Observable<TopResultsViewModel> viewModel = stateWithViewModel.filter(item -> item.data().isPresent()).map(item -> item.data().get());


        //Handling enter screen event and tracking of page view, emits only when new enter screen event is emitted
        viewSubscription.add(Observable.combineLatest(topResultsView.enterScreen(), viewModel, (action, model) -> Pair.of(action, model.queryUrn()))
                                       .subscribe(this::trackPageView));

        //Handling clicks
        viewSubscription.addAll(topResultsView.trackClick()
                                              .doOnNext(searchTrack -> trackClickOnItem(searchTrack.clickParams().get()))
                                              .flatMap(this::playSearchTrack)
                                              .subscribe(topResultsView::showPlaybackResult),

                                topResultsView.playlistClick()
                                              .map(uiAction -> uiAction.clickParams().get())
                                              .doOnNext(this::trackClickOnItem)
                                              .map(this::playlistClickToNavigateAction)
                                              .subscribe(topResultsView::navigateTo),

                                topResultsView.userClick()
                                              .map(uiAction -> uiAction.clickParams().get())
                                              .doOnNext(this::trackClickOnItem)
                                              .map(this::userClickToNavigateAction)
                                              .subscribe(topResultsView::navigateTo),

                                topResultsView.viewAllClick()
                                              .map(this::viewAllClickToNavigateAction)
                                              .subscribe(topResultsView::navigateTo),

                                topResultsView.helpClick()
                                              .doOnNext(click -> searchTracker.trackResultsUpsellClick(SCREEN))
                                              .subscribe(__ -> topResultsView.openUpgrade(UpsellContext.PREMIUM_CONTENT)));
    }

    private Observable<SearchParams> refreshIntent(TopResultsView topResultsView) {
        // do we not want to track refreshes?
        return topResultsView.refreshIntent().map(UiAction.Refresh::searchParams);
    }

    private Observable<AsyncLoader.PageResult<TopResultsViewModel>> search(SearchParams params) {
        return operations.apiSearch(params).toObservable()
                         .flatMap(result -> topResultsMapper.toViewModel(result, params.userQuery()))
                         .map(AsyncLoader.PageResult::from);
    }

    void detachView() {
        viewSubscription.clear();
    }

    private void trackClickOnItem(ClickParams clickParams) {
        eventTracker.trackSearch(SearchEvent.tapItemOnScreen(clickParams.screen(), clickParams.searchQuerySourceInfo(), clickParams.clickSource()));
    }

    private void trackPageView(Pair<UiAction.Enter, Optional<Urn>> searchQueryPair) {
        eventTracker.trackScreen(ScreenEvent.create(SCREEN.get(), new SearchQuerySourceInfo(searchQueryPair.second().or(Urn.NOT_SET), searchQueryPair.first().searchQuery())),
                                 trackingStateProvider.getLastEvent());
    }

    private void trackFirstSearch(SearchParams params) {
        searchTracker.trackSearchFormulationEnd(SCREEN, params.userQuery(), params.queryUrn(), params.queryPosition());
    }

    private Observable<PlaybackResult> playSearchTrack(UiAction.TrackClick trackClick) {
        return playbackInitiator.playPosts(trackClick.allTracks(),
                                           trackClick.trackUrn(),
                                           trackClick.trackPosition(),
                                           new PlaySessionSource(Screen.SEARCH_TOP_RESULTS.get(), trackClick.clickParams().get().searchQuerySourceInfo()))
                                .toObservable();
    }

    private NavigationTarget playlistClickToNavigateAction(ClickParams params) {
        return NavigationTarget.forPlaylist(params.urn(),
                                                        params.screen(),
                                                        Optional.of(params.searchQuerySourceInfo()),
                                                        Optional.absent(),
                                                        Optional.of(params.uiEvent()));
    }

    private NavigationTarget userClickToNavigateAction(ClickParams params) {
        return NavigationTarget.forProfile(params.urn(), Optional.of(params.uiEvent()), Optional.of(params.screen()), Optional.of(params.searchQuerySourceInfo()));
    }

    private NavigationTarget viewAllClickToNavigateAction(UiAction.ViewAllClick viewAllClick) {
        final boolean isPremium = viewAllClick.bucketKind() == TopResultsBucketViewModel.Kind.GO_TRACKS;
        ClickParams clickParams = viewAllClick.clickParams().get();
        return NavigationTarget.forSearchViewAll(clickParams.queryUrn(), clickParams.searchQuery(), viewAllClick.bucketKind(), isPremium);
    }
}
