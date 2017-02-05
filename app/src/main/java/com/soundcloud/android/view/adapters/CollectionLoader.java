package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.transformers.Transformers.takeWhen;

import com.soundcloud.android.view.adapters.CollectionViewState.PartialState;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import android.support.annotation.NonNull;

import java.util.List;

public class CollectionLoader<ItemType, ParamsType> {

    private Func0<Observable<List<ItemType>>> firstPage;
    private Func0<Observable<List<ItemType>>> refreshWith;
    private Func1<ParamsType, Observable<List<ItemType>>> dataFromParams;
    private Func1<List<ItemType>, Optional<ParamsType>> paramsFromData;

    private final Observable<Void> refreshRequested;
    private final Observable<Void> firstPageRequested;
    private final Observable<Void> nextPageRequested;

    private final BehaviorSubject<Optional<ParamsType>> nextPageParams = BehaviorSubject.create();
    private final PublishSubject<PartialState<ItemType>> refreshStateSubject = PublishSubject.create();

    public CollectionLoader(@NonNull Func0<Observable<List<ItemType>>> refreshWith,
                            @NonNull Func0<Observable<List<ItemType>>> firstPage,
                            @NonNull Func1<ParamsType, Observable<List<ItemType>>> dataFromParams,
                            @NonNull Func1<List<ItemType>, Optional<ParamsType>> paramsFromData,
                            @NonNull Observable<Void> firstPageRequested,
                            @NonNull Observable<Void> nextPageRequested,
                            @NonNull Observable<Void> refreshRequested) {
        this.refreshWith = refreshWith;
        this.firstPage = firstPage;
        this.dataFromParams = dataFromParams;
        this.paramsFromData = paramsFromData;
        this.refreshRequested = refreshRequested;
        this.firstPageRequested = firstPageRequested;
        this.nextPageRequested = nextPageRequested;
    }

    public Observable<CollectionViewState<ItemType>> pages() {

        // two things that can result in a new paging sequence. When either emits, start from first page
        Observable<PartialState<ItemType>> pageSequenceStarters = Observable.merge(
                refreshIntent(),
                loadFirstPageIntent()
        );

        return pageSequenceStarters.switchMap(

                partialState -> Observable.merge(
                        Observable.just(partialState), // initial state
                        refreshStateSubject, // refresh state
                        loadNextPageIntent() // next page state
                ).scan(

                        CollectionViewState.loadingFirstPage(),
                        updateLatestState()
                )
        ).startWith(CollectionViewState.loadingFirstPage())
                         .distinctUntilChanged()
                         .cache();
    }

    @NonNull
    private Func2<CollectionViewState<ItemType>, PartialState<ItemType>, CollectionViewState<ItemType>> updateLatestState() {
        return (collectionLoadingState, itemTypePartialState) -> itemTypePartialState.newState(collectionLoadingState);
    }

    private Observable<PartialState<ItemType>> refreshIntent() {
        return refreshRequested.flatMap(aVoid -> Observable.defer(refreshWith)
                                                           .doOnNext(this::keepParams)
                                                           .doOnSubscribe(() -> refreshStateSubject.onNext(new PartialState.RefreshStarted<>()))
                                                           .doOnError(throwable -> refreshStateSubject.onNext(new PartialState.RefreshError<>(throwable)))
                .<PartialState<ItemType>>map(PartialState.FirstPageLoaded::new));
    }

    @NonNull
    private Observable<PartialState<ItemType>> loadFirstPageIntent() {
        return firstPageRequested.flatMap(ignored -> firstPage.call()
                                                              .doOnNext(this::keepParams)
                .<PartialState<ItemType>>map(PartialState.FirstPageLoaded::new)
                .onErrorReturn(PartialState.FirstPageError::new)
        );
    }

    @NonNull
    private Observable<PartialState<ItemType>> loadNextPageIntent() {
        return nextPageParams.compose(takeWhen(nextPageRequested))
                             .flatMap(params -> dataFromParams.call(params.get())
                                                              .doOnNext(this::keepParams)
                                     .<PartialState<ItemType>>map(items -> new PartialState.NextPageLoaded<>(items, hasMorePages()))
                                     .onErrorReturn(PartialState.NextPageError::new)
                                     .startWith(new PartialState.NextPageLoading<>()));
    }

    private void keepParams(List<ItemType> datas) {
        nextPageParams.onNext(paramsFromData.call(datas));
    }

    private boolean hasMorePages() {
        return nextPageParams.hasValue() && nextPageParams.getValue().isPresent();
    }
}
