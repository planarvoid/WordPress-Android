package com.soundcloud.android.utils.collection;

import static com.soundcloud.android.transformers.Transformers.doOnFirst;

import com.soundcloud.android.model.AsyncLoadingState;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.subjects.BehaviorSubject;

import android.support.annotation.NonNull;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

public class AsyncLoader<PageData, FirstPageParamsType> extends Observable<AsyncLoaderState<PageData>> {

    private final Function<FirstPageParamsType, Observable<PageResult<PageData>>> paramsToFirstPage;
    private final Function<FirstPageParamsType, Observable<PageResult<PageData>>> paramsToRefresh;
    private final Observable<FirstPageParamsType> refreshRequested;
    private final Observable<FirstPageParamsType> firstPageRequested;
    private final Observable<Object> nextPageRequested;
    private final Optional<BiFunction<PageData, PageData, PageData>> pageCombiner;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final BehaviorSubject<Optional<Provider<Observable<PageResult<PageData>>>>> nextPage = BehaviorSubject.create();
    private final BehaviorSubject<RefreshState> refreshStateSubject = BehaviorSubject.createDefault(new RefreshState(false, Optional.absent()));

    public static class PageResult<PageData> {

        PageData data;
        Optional<Provider<Observable<PageResult<PageData>>>> nextPage;

        PageResult(PageData data, Optional<Provider<Observable<PageResult<PageData>>>> nextPage) {
            this.data = data;
            this.nextPage = nextPage;
        }

        public static <PageData> PageResult<PageData> from(PageData data, Provider<Observable<PageResult<PageData>>> nextPage) {
            return from(data, Optional.of(nextPage));
        }

        public static <PageData> PageResult<PageData> from(PageData data) {
            return from(data, Optional.absent());
        }

        public static <PageData> PageResult<PageData> from(PageData data, Optional<Provider<Observable<PageResult<PageData>>>> nextPage) {
            return new PageResult<>(data, nextPage);
        }
    }

    private final class RefreshState {

        private final boolean loading;
        private final Optional<Throwable> error;

        RefreshState(boolean loading, Optional<Throwable> error) {
            this.loading = loading;
            this.error = error;
        }

    }

    private final class PageState {

        private final Optional<PageData> data;
        private final boolean loading;
        private final Optional<Throwable> error;

        PageState(Optional<PageData> data, boolean loading, Optional<Throwable> error) {
            this.data = data;
            this.loading = loading;
            this.error = error;
        }
    }

    AsyncLoader(@NonNull Observable<FirstPageParamsType> firstPageRequested,
                @NonNull Function<FirstPageParamsType, Observable<PageResult<PageData>>> paramsToFirstPage,
                @NonNull Observable<FirstPageParamsType> refreshRequested,
                @NonNull Function<FirstPageParamsType, Observable<PageResult<PageData>>> paramsToRefresh,
                @NonNull Observable<Object> nextPageRequested,
                Optional<BiFunction<PageData, PageData, PageData>> pageCombiner) {
        this.paramsToRefresh = paramsToRefresh;
        this.paramsToFirstPage = paramsToFirstPage;
        this.refreshRequested = refreshRequested;
        this.firstPageRequested = firstPageRequested;
        this.nextPageRequested = nextPageRequested;
        this.pageCombiner = pageCombiner;
    }


    @Override
    protected void subscribeActual(Observer<? super AsyncLoaderState<PageData>> observer) {
        Observable<Observable<PageState>> sequenceStarters = refreshes().startWith(initialLoad());
        Observable.combineLatest(sequenceStarters.switchMap(this::createSequenceState),
                                        refreshStateSubject,
                                        this::updateWithRefresh)
                         .doOnDispose(this::cleanup)
                .subscribe(observer);
    }

    private void cleanup() {
        compositeDisposable.dispose();
    }


    private Observable<Observable<PageState>> refreshes() {
        return refreshRequested
                .map(params -> {
                    ConnectableObservable<PageResult<PageData>> replay = paramsToRefresh.apply(params).replay(1);
                    compositeDisposable.add(replay.connect());
                    return replay;
                })
                .flatMap(refreshObservable -> Observable.create(e -> refreshObservable.take(1)
                                                                                      .doOnSubscribe(disposable -> refreshStateSubject.onNext(new RefreshState(true, Optional.absent())))
                                                                                      .doOnError(throwable -> refreshStateSubject.onNext(new RefreshState(false, Optional.of(throwable))))
                                                                                      .doOnNext(p -> refreshStateSubject.onNext(new RefreshState(false, Optional.absent())))
                                                                                      .subscribe(pageState -> e.onNext(refreshObservable.lift(doOnFirst(this::keepNextPageObservable))
                                                                                                                                        .map(AsyncLoader.this::loadedPageState)))));
    }

    private Observable<AsyncLoaderState<PageData>> createSequenceState(Observable<PageState> firstPage) {
        return pageEmitter().startWith(firstPage)
                            .scan(new ArrayList<>(), this::addNewPage)
                            .switchMap(this::stateFromPages);
    }

    private AsyncLoaderState<PageData> updateWithRefresh(AsyncLoaderState<PageData> pageDataAsyncLoaderState, RefreshState refreshState) {
        return pageDataAsyncLoaderState.updateWithRefreshState(refreshState.loading, refreshState.error);
    }

    @NonNull
    private List<Observable<PageState>> addNewPage(List<Observable<PageState>> observables, Observable<PageState> partialStateObservable) {
        observables.add(replayLastAndConnect(partialStateObservable));
        return observables;
    }

    @NonNull
    private ConnectableObservable<PageState> replayLastAndConnect(Observable<PageState> partialStateObservable) {
        ConnectableObservable<PageState> replay = partialStateObservable.replay(1);
        compositeDisposable.add(replay.connect());
        return replay;
    }

    private Observable<AsyncLoaderState<PageData>> stateFromPages(List<Observable<PageState>> arr) {
        return Observable.combineLatest(arr, this::combinePages);
    }

    private AsyncLoaderState<PageData> combinePages(Object[] objects) throws Exception {
        PageData combinedData = combinePageData(objects);
        PageState lastPageState = lastPageState(objects);
        AsyncLoadingState loadingState = AsyncLoadingState.builder()
                                                          .isLoadingNextPage(lastPageState.loading)
                                                          .requestMoreOnScroll(!lastPageState.loading && !lastPageState.error.isPresent() && hasMorePages())
                                                          .nextPageError(lastPageState.error.transform(ViewError::from))
                                                          .build();

        return AsyncLoaderState.<PageData>builder().data(Optional.fromNullable(combinedData))
                                                   .asyncLoadingState(loadingState)
                                                   .build();
    }

    private PageData combinePageData(Object[] objects) throws Exception {
        PageData combinedData = null;
        for (Object object : objects) {
            PageState partialState = (PageState) object;
            if (partialState.data.isPresent()) {
                if (combinedData == null) {
                    combinedData = partialState.data.get();
                } else {
                    combinedData = pageCombiner.get().apply(combinedData, partialState.data.get());
                }
            }
        }
        return combinedData;
    }

    private PageState lastPageState(Object[] objects) {
        return (PageState) objects[objects.length - 1];
    }

    @NonNull
    private Observable<Observable<PageState>> pageEmitter() {
        return nextPageRequested.withLatestFrom(nextPage, (__, nextPage1) -> nextPage1)
                                .filter(Optional::isPresent)
                                .map(nextPageOpt -> nextPageOpt.get().get())
                                .map(this::nextPageObservable);
    }

    private Observable<PageState> initialLoad() {
        return firstPageRequested.flatMap(params -> paramsToFirstPage.apply(params)
                                                                     .lift(doOnFirst(this::keepNextPageObservable))
                                                                     .map(this::loadedPageState)
                                                                     .onErrorReturn(this::errorPageState)
                                                                     .startWith(loadingPageState()));
    }

    @NonNull
    private PageState errorPageState(Throwable t) {
        return new PageState(Optional.absent(), false, Optional.of(t));
    }

    private Observable<PageState> nextPageObservable(Observable<PageResult<PageData>> nextPage) {
        return nextPage.lift(doOnFirst(this::keepNextPageObservable))
                       .map(this::loadedPageState)
                       .onErrorReturn(this::errorPageState)
                       .startWith(loadingPageState());
    }

    @NonNull
    private PageState loadingPageState() {
        return new PageState(Optional.absent(), true, Optional.absent());
    }

    @NonNull
    private PageState loadedPageState(PageResult<PageData> result) {
        return new PageState(Optional.of(result.data), false, Optional.absent());
    }

    private void keepNextPageObservable(PageResult<PageData> datas) {
        try {
            nextPage.onNext(datas.nextPage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean hasMorePages() {
        return nextPage.hasValue() && nextPage.getValue().isPresent();
    }

    public static <PageData, FirstPageParamsType> Builder<PageData, FirstPageParamsType> startWith(Observable<FirstPageParamsType> initialLoadSignal,
                                                                                                   Function<FirstPageParamsType, Observable<PageResult<PageData>>> loadInitialPageWith) {
        return new Builder<>(initialLoadSignal, loadInitialPageWith);
    }

    public static class Builder<PageData, FirstPageParamsType> {
        private final Observable<FirstPageParamsType> firstPageRequested;
        private final Function<FirstPageParamsType, Observable<PageResult<PageData>>> firstPage;
        private Observable<FirstPageParamsType> refreshRequested = Observable.never();
        private Function<FirstPageParamsType, Observable<PageResult<PageData>>> refreshWith = __ -> Observable.empty();
        private Observable<Object> nextPageRequested = Observable.never();
        private Optional<BiFunction<PageData, PageData, PageData>> combinerOpt = Optional.absent();

        public Builder(Observable<FirstPageParamsType> initialLoadSignal,
                       Function<FirstPageParamsType, Observable<PageResult<PageData>>> paramsToLoadOp) {
            this.firstPageRequested = initialLoadSignal;
            this.firstPage = paramsToLoadOp;
        }

        public Builder<PageData, FirstPageParamsType> withRefresh(Observable<FirstPageParamsType> refreshSignal,
                                                                  Function<FirstPageParamsType, Observable<PageResult<PageData>>> paramsToRefreshOp) {
            this.refreshRequested = refreshSignal;
            this.refreshWith = paramsToRefreshOp;
            return this;
        }

        public Builder<PageData, FirstPageParamsType> withPaging(Observable<Object> nextPageSignal,
                                                                 BiFunction<PageData, PageData, PageData> pageCombiner) {
            this.nextPageRequested = nextPageSignal;
            this.combinerOpt = Optional.of(pageCombiner);
            return this;
        }

        public AsyncLoader<PageData, FirstPageParamsType> build() {
            return new AsyncLoader<>(
                    firstPageRequested,
                    firstPage,
                    refreshRequested,
                    refreshWith,
                    nextPageRequested,
                    combinerOpt
            );
        }
    }
}
