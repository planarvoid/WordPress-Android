package com.soundcloud.android.utils.collection;

import static com.soundcloud.android.transformers.Transformers.takeWhen;

import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import android.support.annotation.NonNull;

public class CollectionLoader<ItemType, ParamsType> {

    private Func1<ParamsType, Observable<ItemType>> firstPage;
    private Func1<ParamsType, Observable<ItemType>> refreshWith;
    private Func1<ParamsType, Observable<ItemType>> dataFromParams;
    private Func1<ItemType, Optional<ParamsType>> paramsFromData;

    private final Observable<ParamsType> refreshRequested;
    private final Observable<ParamsType> firstPageRequested;
    private final Observable<Void> nextPageRequested;

    private final BehaviorSubject<Optional<ParamsType>> nextPageParams = BehaviorSubject.create();
    private final PublishSubject<CollectionLoaderState.PartialState<ItemType>> refreshStateSubject = PublishSubject.create();
    private final Optional<Func2<ItemType, ItemType, ItemType>> combinerOpt;

    public CollectionLoader(@NonNull Observable<ParamsType> firstPageRequested,
                            @NonNull Func1<ParamsType, Observable<ItemType>> firstPage,
                            @NonNull Observable<ParamsType> refreshRequested,
                            @NonNull Func1<ParamsType, Observable<ItemType>> refreshWith) {
        this(firstPageRequested,
             firstPage,
             refreshRequested,
             refreshWith,
             Observable.empty(),
             paramsType -> Observable.empty(),
             itemTypes -> Optional.absent(),
             Optional.absent());
    }

    CollectionLoader(@NonNull Observable<ParamsType> firstPageRequested,
                     @NonNull Func1<ParamsType, Observable<ItemType>> firstPage,
                     @NonNull Observable<ParamsType> refreshRequested,
                     @NonNull Func1<ParamsType, Observable<ItemType>> refreshWith,
                     @NonNull Observable<Void> nextPageRequested,
                     @NonNull Func1<ParamsType, Observable<ItemType>> dataFromParams,
                     @NonNull Func1<ItemType, Optional<ParamsType>> paramsFromData,
                     @NonNull Func2<ItemType, ItemType, ItemType> combiner) {
        this(firstPageRequested,
             firstPage,
             refreshRequested,
             refreshWith,
             nextPageRequested,
             dataFromParams,
             paramsFromData,
             Optional.of(combiner));
    }

    private CollectionLoader(@NonNull Observable<ParamsType> firstPageRequested,
                             @NonNull Func1<ParamsType, Observable<ItemType>> firstPage,
                             @NonNull Observable<ParamsType> refreshRequested,
                             @NonNull Func1<ParamsType, Observable<ItemType>> refreshWith,
                             @NonNull Observable<Void> nextPageRequested,
                             @NonNull Func1<ParamsType, Observable<ItemType>> dataFromParams,
                             @NonNull Func1<ItemType, Optional<ParamsType>> paramsFromData,
                             Optional<Func2<ItemType, ItemType, ItemType>> combinerOpt) {
        this.refreshWith = refreshWith;
        this.firstPage = firstPage;
        this.dataFromParams = dataFromParams;
        this.paramsFromData = paramsFromData;
        this.refreshRequested = refreshRequested;
        this.firstPageRequested = firstPageRequested;
        this.nextPageRequested = nextPageRequested;
        this.combinerOpt = combinerOpt;
    }

    public Observable<CollectionLoaderState<ItemType>> pages() {

        // two things that can result in a new paging sequence. When either emits, start from first page
        Observable<CollectionLoaderState.PartialState<ItemType>> pageSequenceStarters = Observable.merge(
                refreshIntent(),
                loadFirstPageIntent()
        );

        return pageSequenceStarters.switchMap(
                partialState -> Observable.merge(
                        Observable.just(partialState), // initial state
                        refreshStateSubject, // refresh state
                        loadNextPageIntent() // next page state
                ).scan(
                        CollectionLoaderState.loadingNextPage(),
                        updateLatestState()
                )
        ).distinctUntilChanged()
                                   .cache();
    }

    @NonNull
    private Func2<CollectionLoaderState<ItemType>, CollectionLoaderState.PartialState<ItemType>, CollectionLoaderState<ItemType>> updateLatestState() {
        return (collectionLoadingState, itemTypePartialState) -> itemTypePartialState.newState(collectionLoadingState);
    }

    private Observable<CollectionLoaderState.PartialState<ItemType>> refreshIntent() {
        return refreshRequested.flatMap(params -> refreshWith.call(params)
                                                             .doOnNext(this::keepParams)
                                                             .doOnSubscribe(() -> refreshStateSubject.onNext(new CollectionLoaderState.PartialState.RefreshStarted<>()))
                                                             .doOnError(throwable -> refreshStateSubject.onNext(new CollectionLoaderState.PartialState.RefreshError<>(throwable)))
                                                             .onErrorResumeNext(Observable.empty())
                .<CollectionLoaderState.PartialState<ItemType>>map(items -> new CollectionLoaderState.PartialState.NextPageLoaded<>(items, hasMorePages(), Optional.absent())));
    }

    @NonNull
    private Observable<CollectionLoaderState.PartialState<ItemType>> loadFirstPageIntent() {
        return firstPageRequested.flatMap(params -> firstPage.call(params)
                                                             .doOnNext(this::keepParams)
                .<CollectionLoaderState.PartialState<ItemType>>map(items -> new CollectionLoaderState.PartialState.NextPageLoaded<>(items, hasMorePages(), Optional.absent()))
                .onErrorReturn(CollectionLoaderState.PartialState.NextPageError::new)
        );
    }

    @NonNull
    private Observable<CollectionLoaderState.PartialState<ItemType>> loadNextPageIntent() {
        return nextPageParams.compose(takeWhen(nextPageRequested))
                             .flatMap(params -> dataFromParams.call(params.get())
                                                              .doOnNext(this::keepParams)
                                     .<CollectionLoaderState.PartialState<ItemType>>map(items -> new CollectionLoaderState.PartialState.NextPageLoaded<>(items, hasMorePages(), combinerOpt))
                                     .onErrorReturn(CollectionLoaderState.PartialState.NextPageError::new)
                                     .startWith(new CollectionLoaderState.PartialState.NextPageLoading<>()));
    }

    private void keepParams(ItemType datas) {
        nextPageParams.onNext(paramsFromData.call(datas));
    }

    private boolean hasMorePages() {
        return nextPageParams.hasValue() && nextPageParams.getValue().isPresent();
    }
}
