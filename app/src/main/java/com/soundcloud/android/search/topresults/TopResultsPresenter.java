package com.soundcloud.android.search.topresults;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.android.viewstate.PartialState;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.functions.Func2;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.CompositeSubscription;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.Collections;

public class TopResultsPresenter {

    private final BehaviorSubject<Pair<String, Optional<Urn>>> searchQuery = BehaviorSubject.create();
    private final BehaviorSubject<Void> refreshSubject = BehaviorSubject.create();
    private final BehaviorSubject<AsyncViewModel<TopResultsViewModel>> viewModel = BehaviorSubject.create();

    private final TopResultsLoader topResultsLoader;

    private CompositeSubscription subscription = new CompositeSubscription();

    @Inject
    TopResultsPresenter(TopResultsLoader topResultsLoader) {
        this.topResultsLoader = topResultsLoader;
    }

    void connect() {
        AsyncViewModel<TopResultsViewModel> initialState = AsyncViewModel.fromIdle(TopResultsViewModel.create(Collections.emptyList()));
        subscription = new CompositeSubscription();
        subscription.add(
                Observable.merge(
                        searchIntent(),
                        refreshIntent()
                ).scan(initialState, reduceStates()).subscribe(viewModel)
        );
    }

    void disconnect() {
        subscription.unsubscribe();
    }

    Observable<AsyncViewModel<TopResultsViewModel>> viewModel() {
        return viewModel;
    }

    void search(String query, Optional<Urn> queryUrn) {
        searchQuery.onNext(Pair.of(query, queryUrn));
    }

    void refresh() {
        refreshSubject.onNext(null);
    }

    @NonNull
    private Observable<PartialState<TopResultsViewModel>> searchIntent() {
        return searchQuery.flatMap(this::doSearch);
    }

    @NonNull
    private Observable<PartialState<TopResultsViewModel>> refreshIntent() {
        return refreshSubject.flatMap(aVoid -> searchQuery.hasValue() ? doRefresh(searchQuery.getValue()) : Observable.empty());
    }

    @NonNull
    private Observable<PartialState<TopResultsViewModel>> doSearch(Pair<String, Optional<Urn>> searchParams) {
        return topResultsLoader.getTopSearchResults(searchParams)
                .<PartialState<TopResultsViewModel>>map(UpdatedResults::new)
                .onErrorReturn(PartialState.Error::new);
    }

    @NonNull
    private Observable<PartialState<TopResultsViewModel>> doRefresh(Pair<String, Optional<Urn>> searchParams) {
        return doSearch(searchParams)
                .startWith(new PartialState.RefreshStarted<>());
    }

    @NonNull
    private Func2<AsyncViewModel<TopResultsViewModel>, PartialState<TopResultsViewModel>, AsyncViewModel<TopResultsViewModel>> reduceStates() {
        return (oldState, partialState) -> partialState.createNewState(oldState);
    }

    private class UpdatedResults implements PartialState<TopResultsViewModel> {

        private TopResultsViewModel updatedResults;

        UpdatedResults(TopResultsViewModel updatedResults) {
            this.updatedResults = updatedResults;
        }

        @Override
        public AsyncViewModel<TopResultsViewModel> createNewState(AsyncViewModel<TopResultsViewModel> oldState) {
            return AsyncViewModel.fromIdle(updatedResults);
        }
    }
}
