package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.transformers.Transformers.takeWhen;

import com.soundcloud.android.view.adapters.CollectionViewState.PartialState;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import android.support.annotation.NonNull;

import java.util.List;

public class CollectionLoader<ItemType, ParamsType> {

    private Func1<ParamsType, Observable<List<ItemType>>> firstPage;
    private Func1<ParamsType, Observable<List<ItemType>>> refreshWith;
    private Func1<ParamsType, Observable<List<ItemType>>> dataFromParams;
    private Func1<List<ItemType>, Optional<ParamsType>> paramsFromData;

    private final Observable<ParamsType> refreshRequested;
    private final Observable<ParamsType> firstPageRequested;
    private final Observable<Void> nextPageRequested;

    private final BehaviorSubject<Optional<ParamsType>> nextPageParams = BehaviorSubject.create();
    private final PublishSubject<PartialState<ItemType>> refreshStateSubject = PublishSubject.create();

    public CollectionLoader(@NonNull Observable<ParamsType> firstPageRequested,
                            @NonNull Func1<ParamsType, Observable<List<ItemType>>> firstPage,
                            @NonNull Observable<ParamsType> refreshRequested,
                            @NonNull Func1<ParamsType, Observable<List<ItemType>>> refreshWith) {
        this(firstPageRequested,
             firstPage,
             refreshRequested,
             refreshWith,
             Observable.empty(),
             paramsType -> Observable.empty(),
             itemTypes -> Optional.absent());
    }

    public CollectionLoader(@NonNull Observable<ParamsType> firstPageRequested,
                            @NonNull Func1<ParamsType, Observable<List<ItemType>>> firstPage,
                            @NonNull Observable<ParamsType> refreshRequested,
                            @NonNull Func1<ParamsType, Observable<List<ItemType>>> refreshWith,
                            @NonNull Observable<Void> nextPageRequested,
                            @NonNull Func1<ParamsType, Observable<List<ItemType>>> dataFromParams,
                            @NonNull Func1<List<ItemType>, Optional<ParamsType>> paramsFromData) {
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
                        CollectionViewState.loadingNextPage(),
                        updateLatestState()
                )
        ).distinctUntilChanged()
                                   .cache();
    }

    @NonNull
    private Func2<CollectionViewState<ItemType>, PartialState<ItemType>, CollectionViewState<ItemType>> updateLatestState() {
        return (collectionLoadingState, itemTypePartialState) -> itemTypePartialState.newState(collectionLoadingState);
    }

    private Observable<PartialState<ItemType>> refreshIntent() {
        return refreshRequested.flatMap(params -> refreshWith.call(params)
                                                             .doOnNext(this::keepParams)
                                                             .doOnSubscribe(() -> refreshStateSubject.onNext(new PartialState.RefreshStarted<>()))
                                                             .doOnError(throwable -> refreshStateSubject.onNext(new PartialState.RefreshError<>(throwable)))
                                                             .onErrorResumeNext(Observable.empty())
                .<PartialState<ItemType>>map(items -> new PartialState.NextPageLoaded<>(items, hasMorePages())));
    }

    @NonNull
    private Observable<PartialState<ItemType>> loadFirstPageIntent() {
        return firstPageRequested.flatMap(params -> firstPage.call(params)
                                                             .doOnNext(this::keepParams)
                .<PartialState<ItemType>>map(items -> new PartialState.NextPageLoaded<>(items, hasMorePages()))
                .onErrorReturn(PartialState.NextPageError::new)
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
