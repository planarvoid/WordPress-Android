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

    interface TopResultsView {

        Observable<Pair<String, Optional<Urn>>> searchIntent();

        Observable<Void> refreshIntent();
    }

    private final BehaviorSubject<AsyncViewModel<TopResultsViewModel>> viewModel = BehaviorSubject.create();

    private final TopResultsLoader topResultsLoader;

    private CompositeSubscription subscription = new CompositeSubscription();

    @Inject
    TopResultsPresenter(TopResultsLoader topResultsLoader) {
        this.topResultsLoader = topResultsLoader;
    }

    void attachView(TopResultsView topResultsView) {
        AsyncViewModel<TopResultsViewModel> initialState = AsyncViewModel.fromIdle(TopResultsViewModel.create(Collections.emptyList()));
        subscription = new CompositeSubscription();

        Observable<PartialState<TopResultsViewModel>> searchIntent = topResultsView.searchIntent().cache().flatMap(this::doSearch);
        Observable<PartialState<TopResultsViewModel>> refreshIntent = topResultsView.refreshIntent()
                                                                                    .flatMap(ignored -> searchIntent.startWith(new PartialState.RefreshStarted<>()));

        subscription.add(
                Observable.merge(
                        searchIntent,
                        refreshIntent
                ).scan(initialState, reduceStates()).subscribe(viewModel)
        );
    }

    void detachView() {
        subscription.unsubscribe();
    }

    Observable<AsyncViewModel<TopResultsViewModel>> viewModel() {
        return viewModel;
    }

    @NonNull
    private Observable<PartialState<TopResultsViewModel>> doSearch(Pair<String, Optional<Urn>> searchParams) {
        return topResultsLoader.getTopSearchResults(searchParams)
                .<PartialState<TopResultsViewModel>>map(UpdatedResults::new)
                .onErrorReturn(PartialState.Error::new);
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
