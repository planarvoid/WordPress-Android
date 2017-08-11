package com.soundcloud.android.utils.collection

import com.soundcloud.android.model.AsyncLoadingState
import com.soundcloud.android.transformers.Transformers.doOnFirst
import com.soundcloud.android.view.ViewError
import com.soundcloud.java.optional.Optional
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function
import io.reactivex.observables.ConnectableObservable
import io.reactivex.subjects.BehaviorSubject
import java.util.*
import javax.inject.Provider

class AsyncLoader<PageData, FirstPageParamsType> internal constructor(private val firstPageRequested: Observable<FirstPageParamsType>,
                                                                      private val paramsToFirstPage: Function<FirstPageParamsType, Observable<PageResult<PageData>>>,
                                                                      private val refreshRequested: Observable<FirstPageParamsType>,
                                                                      private val paramsToRefresh: Function<FirstPageParamsType, Observable<PageResult<PageData>>>,
                                                                      private val nextPageRequested: Observable<Any>,
                                                                      private val pageCombiner: Optional<BiFunction<PageData, PageData, PageData>>)
    : Observable<AsyncLoaderState<PageData>>() {

    private val compositeDisposable = CompositeDisposable()
    private val nextPage = BehaviorSubject.create<Optional<Provider<Observable<PageResult<PageData>>>>>()
    private val refreshStateSubject = BehaviorSubject.createDefault(RefreshState(false, Optional.absent<Throwable>()))

    class PageResult<PageData> internal constructor(internal var data: PageData, internal var nextPage: Optional<Provider<Observable<PageResult<PageData>>>>) {
        companion object {

            fun <PageData> from(data: PageData, nextPage: Provider<Observable<PageResult<PageData>>>): PageResult<PageData> {
                return from(data, Optional.of(nextPage))
            }

            fun <PageData> from(data: PageData): PageResult<PageData> {
                return from(data, Optional.absent<Provider<Observable<PageResult<PageData>>>>())
            }

            fun <PageData> from(data: PageData, nextPage: Optional<Provider<Observable<PageResult<PageData>>>>): PageResult<PageData> {
                return PageResult(data, nextPage)
            }
        }
    }

    private data class RefreshState internal constructor(internal val loading: Boolean, internal val error: Optional<Throwable> = Optional.absent())

    private inner class PageState internal constructor(internal val data: Optional<PageData>, internal val loading: Boolean, internal val error: Optional<Throwable>)


    override fun subscribeActual(observer: Observer<in AsyncLoaderState<PageData>>) {
        val sequenceStarters = refreshes().startWith(initialLoad())
        Observable.combineLatest(sequenceStarters.switchMap { this.createSequenceState(it) },
                refreshStateSubject,
                BiFunction<AsyncLoaderState<PageData>, RefreshState, AsyncLoaderState<PageData>> { pageDataAsyncLoaderState, refreshState -> this.updateWithRefresh(pageDataAsyncLoaderState, refreshState) })
                .doOnDispose { compositeDisposable.clear() }
                .subscribe(observer)
    }

    private fun refreshes(): Observable<Observable<PageState>> {
        return refreshRequested
                .map { params ->
                    replayLastAndConnect(paramsToRefresh.apply(params))
                }
                .flatMap { refreshObservable ->
                    Observable.create<Observable<PageState>> { e ->
                        compositeDisposable.add(refreshObservable.take(1)
                                .doOnSubscribe { refreshStateSubject.onNext(RefreshState(true)) }
                                .doOnError { throwable -> refreshStateSubject.onNext(RefreshState(false, Optional.of(throwable))) }
                                .doOnNext { p -> refreshStateSubject.onNext(RefreshState(false)) }
                                .subscribe { e.onNext(refreshObservable.lift(doOnFirst(this::keepNextPageObservable)).map { loadedPageState(it) }) })
                    }
                }
    }

    private fun createSequenceState(firstPage: Observable<PageState>): Observable<AsyncLoaderState<PageData>> {
        return pageEmitter().startWith(firstPage)
                .scan(ArrayList<Observable<PageState>>(), BiFunction<MutableList<Observable<PageState>>, Observable<PageState>, MutableList<Observable<PageState>>> { observables, partialStateObservable -> this.addNewPage(observables, partialStateObservable) })
                .switchMap { this.stateFromPages(it) }
    }

    private fun updateWithRefresh(pageDataAsyncLoaderState: AsyncLoaderState<PageData>, refreshState: RefreshState): AsyncLoaderState<PageData> {
        return pageDataAsyncLoaderState.updateWithRefreshState(refreshState.loading, refreshState.error)
    }

    private fun addNewPage(observables: MutableList<Observable<PageState>>, partialStateObservable: Observable<PageState>): MutableList<Observable<PageState>> {
        observables.add(replayLastAndConnect(partialStateObservable))
        return observables
    }

    private fun <T> replayLastAndConnect(partialStateObservable: Observable<T>): ConnectableObservable<T> {
        val replay = partialStateObservable.replay(1)
        compositeDisposable.add(replay.connect())
        return replay
    }

    private fun stateFromPages(arr: List<Observable<PageState>>): Observable<AsyncLoaderState<PageData>> {
        return Observable.combineLatest(arr) { this.combinePages(it) }
    }

    @Throws(Exception::class)
    private fun combinePages(objects: Array<Any>): AsyncLoaderState<PageData> {
        val combinedData = combinePageData(objects)
        val lastPageState = toPageState(objects.last())
        val loadingState = AsyncLoadingState.builder()
                .isLoadingNextPage(lastPageState.loading)
                .requestMoreOnScroll(!lastPageState.loading && !lastPageState.error.isPresent && hasMorePages())
                .nextPageError(lastPageState.error.transform({ ViewError.from(it) }))
                .build()

        return AsyncLoaderState.builder<PageData>().data(Optional.fromNullable(combinedData))
                .asyncLoadingState(loadingState)
                .build()
    }

    @Throws(Exception::class)
    private fun combinePageData(objects: Array<Any>): PageData? {
        var combinedData: PageData? = null
        objects.map { toPageState(it) }
                .filterNotNull() // if we make data nullable instead of Optional
                .filter { it.data.isPresent } // if we keep the Optional
                .forEach {
                    if (combinedData == null) {
                        combinedData = it.data.get()
                    } else {
                        combinedData = pageCombiner.get().apply(combinedData!!, it.data.get())
                    }
                }
        return combinedData
    }

    @Suppress("UNCHECKED_CAST")
    private fun toPageState(any: Any): PageState {
        return any as AsyncLoader<PageData, FirstPageParamsType>.PageState
    }

    private fun pageEmitter(): Observable<Observable<PageState>> {
        return nextPageRequested.flatMap { nextPage.take(1) }
                .filter { it.isPresent }
                .map { nextPageOpt -> nextPageOpt.get().get() }
                .map { this.nextPageObservable(it) }
    }

    private fun initialLoad(): Observable<PageState> {
        return firstPageRequested.flatMap { params ->
            paramsToFirstPage.apply(params)
                    .lift(doOnFirst<PageResult<PageData>>(this::keepNextPageObservable))
                    .map { this.loadedPageState(it) }
                    .onErrorReturn { this.errorPageState(it) }
                    .startWith(loadingPageState())
        }
    }

    private fun errorPageState(t: Throwable): PageState {
        return PageState(Optional.absent<PageData>(), false, Optional.of(t))
    }

    private fun nextPageObservable(nextPage: Observable<PageResult<PageData>>): Observable<PageState> {
        return nextPage.lift(doOnFirst<PageResult<PageData>>(this::keepNextPageObservable))
                .map { this.loadedPageState(it) }
                .onErrorReturn { this.errorPageState(it) }
                .startWith(loadingPageState())
    }

    private fun loadingPageState(): PageState {
        return PageState(Optional.absent<PageData>(), true, Optional.absent<Throwable>())
    }

    private fun loadedPageState(result: PageResult<PageData>): PageState {
        return PageState(Optional.of(result.data), false, Optional.absent<Throwable>())
    }

    private fun keepNextPageObservable(datas: PageResult<PageData>) {
        nextPage.onNext(datas.nextPage)
    }

    private fun hasMorePages(): Boolean {
        return nextPage.hasValue() && nextPage.value.isPresent
    }

    class Builder<PageData, FirstPageParamsType>(private val firstPageRequested: Observable<FirstPageParamsType>,
                                                 private val firstPage: Function<FirstPageParamsType, Observable<PageResult<PageData>>>) {
        private var refreshRequested = Observable.never<FirstPageParamsType>()
        private var refreshWith: Function<FirstPageParamsType, Observable<PageResult<PageData>>> = Function() { empty() }
        private var nextPageRequested = Observable.never<Any>()
        private var combinerOpt = Optional.absent<BiFunction<PageData, PageData, PageData>>()

        fun withRefresh(refreshSignal: Observable<FirstPageParamsType>,
                        paramsToRefreshOp: Function<FirstPageParamsType, Observable<PageResult<PageData>>>): Builder<PageData, FirstPageParamsType> {
            this.refreshRequested = refreshSignal
            this.refreshWith = paramsToRefreshOp
            return this
        }

        fun withPaging(nextPageSignal: Observable<Any>,
                       pageCombiner: BiFunction<PageData, PageData, PageData>): Builder<PageData, FirstPageParamsType> {
            this.nextPageRequested = nextPageSignal
            this.combinerOpt = Optional.of(pageCombiner)
            return this
        }

        fun build(): AsyncLoader<PageData, FirstPageParamsType> {
            return AsyncLoader(
                    firstPageRequested,
                    firstPage,
                    refreshRequested,
                    refreshWith,
                    nextPageRequested,
                    combinerOpt
            )
        }
    }

    companion object {

        @JvmStatic
        fun <PageData, FirstPageParamsType> startWith(initialLoadSignal: Observable<FirstPageParamsType>,
                                                      loadInitialPageWith: Function<FirstPageParamsType, Observable<PageResult<PageData>>>): Builder<PageData, FirstPageParamsType> {
            return Builder(initialLoadSignal, loadInitialPageWith)
        }
    }
}
