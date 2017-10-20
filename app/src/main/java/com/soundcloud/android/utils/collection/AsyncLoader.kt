package com.soundcloud.android.utils.collection

import com.soundcloud.android.model.AsyncLoadingState
import com.soundcloud.android.transformers.Transformers.doOnFirst
import com.soundcloud.android.view.ViewError
import com.soundcloud.java.optional.Optional
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observables.ConnectableObservable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Provider

class AsyncLoader<PageData, FirstPageParamsType> internal constructor(private val firstPageRequested: Observable<FirstPageParamsType>,
                                                                                  private val paramsToFirstPage: (FirstPageParamsType) -> Observable<PageResult<PageData>>,
                                                                                  private val refreshRequested: Observable<FirstPageParamsType>,
                                                                                  private val paramsToRefresh: (FirstPageParamsType) -> Observable<PageResult<PageData>>,
                                                                                  private val nextPageRequested: Observable<Any>,
                                                                                  private val pageCombiner: Optional<(PageData, PageData) -> PageData>)
    : Observable<AsyncLoaderState<PageData>>() {

    private val compositeDisposable = CompositeDisposable()
    private val nextPage = BehaviorSubject.create<Optional<Provider<Observable<PageResult<PageData>>>>>()
    private val refreshStateSubject = BehaviorSubject.createDefault(RefreshState(false, Optional.absent<Throwable>()))

    class PageResult<PageData> internal constructor(internal var data: PageData,
                                                                internal var nextPage: Optional<Provider<Observable<PageResult<PageData>>>> = Optional.absent()) {
        companion object {

            fun <PageData> from(data: PageData, nextPage: Provider<Observable<PageResult<PageData>>>): PageResult<PageData> = PageResult(data, nextPage = Optional.of(nextPage))

            fun <PageData> from(data: PageData): PageResult<PageData> = PageResult(data)

            fun <PageData> from(data: PageData, nextPage: Optional<Provider<Observable<PageResult<PageData>>>>): PageResult<PageData> = PageResult(data, nextPage = nextPage)
        }
    }

    private data class RefreshState internal constructor(internal val loading: Boolean, internal val error: Optional<Throwable> = Optional.absent())

    private inner class PageState internal constructor(internal val data: Optional<PageData> = Optional.absent(),
                                                       internal val loading: Boolean = false,
                                                       internal val error: Optional<Throwable> = Optional.absent())

    override fun subscribeActual(observer: Observer<in AsyncLoaderState<PageData>>) {
        val sequenceStarters = refreshes().startWith(initialLoad())

        Observables.combineLatest(sequenceStarters.switchMap { createSequenceState(it) },
                                       refreshStateSubject,
                                       { pageDataAsyncLoaderState, refreshState -> updateWithRefresh(pageDataAsyncLoaderState, refreshState)})
                .doOnDispose { compositeDisposable.clear() }
                .subscribe(observer)
    }

    private fun refreshes(): Observable<Observable<PageState>> {
        return refreshRequested
                .map { params ->
                    replayLastAndConnect(paramsToRefresh(params))
                }
                .flatMap { refreshObservable ->
                    Observable.create<Observable<PageState>> { e ->
                        compositeDisposable.add(refreshObservable.take(1)
                                                        .doOnSubscribe { refreshStateSubject.onNext(RefreshState(true)) }
                                                        .subscribeBy(onNext = {
                                                            refreshStateSubject.onNext(RefreshState(false))
                                                            e.onNext(refreshObservable.lift(doOnFirst(this::keepNextPageObservable)).map { loadedPageState(it) })
                                                        },
                                                                     onError = { throwable -> refreshStateSubject.onNext(RefreshState(false, Optional.of(throwable))) }))
                    }
                }
    }

    private fun createSequenceState(firstPage: Observable<PageState>): Observable<AsyncLoaderState<PageData>> {
        return pageEmitter().startWith(firstPage)
                .scan(mutableListOf<Observable<PageState>>(), { observables, partialStateObservable -> this.addNewPage(observables, partialStateObservable) })
                .switchMap { this.stateFromPages(it) }
    }

    private fun updateWithRefresh(pageDataAsyncLoaderState: AsyncLoaderState<PageData>, refreshState: RefreshState): AsyncLoaderState<PageData> =
            pageDataAsyncLoaderState.updateWithRefreshState(refreshState.loading, refreshState.error)

    private fun addNewPage(observables: MutableList<Observable<PageState>>, partialStateObservable: Observable<PageState>): MutableList<Observable<PageState>> {
        observables.add(replayLastAndConnect(partialStateObservable))
        return observables
    }

    private fun <T> replayLastAndConnect(partialStateObservable: Observable<T>): ConnectableObservable<T> {
        val replay = partialStateObservable.replay(1)
        compositeDisposable.add(replay.connect())
        return replay
    }

    private fun stateFromPages(arr: List<Observable<PageState>>): Observable<AsyncLoaderState<PageData>> = Observable.combineLatest(arr) { this.combinePages(it) }

    @Throws(Exception::class)
    private fun combinePages(objects: Array<Any>): AsyncLoaderState<PageData> {
        val combinedData = combinePageData(objects)
        val lastPageState = toPageState(objects.last())
        val loadingState = AsyncLoadingState.builder()
                .isLoadingNextPage(lastPageState.loading)
                .requestMoreOnScroll(!lastPageState.loading && !lastPageState.error.isPresent && hasMorePages())
                .nextPageError(lastPageState.error.transform { ViewError.from(it) })
                .build()

        return if (combinedData != null) AsyncLoaderState(data = Optional.of(combinedData),
                                                          asyncLoadingState = loadingState)
        else AsyncLoaderState(asyncLoadingState = loadingState)
    }

    @Throws(Exception::class)
    private fun combinePageData(objects: Array<Any>): PageData? {
        val empty: PageData? = null
        return objects.map { toPageState(it) }
                .filter { it.data.isPresent }.fold(empty) { acc, pageState ->
            if (acc == null) {
                pageState.data.get()
            } else {
                pageCombiner.get()(acc, pageState.data.get())
            }
        }
    }

    @Suppress("UNCHECKED_CAST", "UnsafeCast")
    private fun toPageState(any: Any): PageState = any as AsyncLoader<PageData, FirstPageParamsType>.PageState

    private fun pageEmitter(): Observable<Observable<PageState>> {
        return nextPageRequested.flatMap { nextPage.take(1) }
                .filter { it.isPresent }
                .map { nextPageOpt -> nextPageOpt.get().get() }
                .map { this.nextPageObservable(it) }
    }

    private fun initialLoad(): Observable<PageState> {
        return firstPageRequested.flatMap { params ->
            paramsToFirstPage(params)
                    .lift(doOnFirst<PageResult<PageData>>(this::keepNextPageObservable))
                    .map { this.loadedPageState(it) }
                    .onErrorReturn { this.errorPageState(it) }
                    .startWith(loadingPageState())
        }
    }

    private fun errorPageState(t: Throwable): PageState = PageState(error = Optional.of(t))

    private fun nextPageObservable(nextPage: Observable<PageResult<PageData>>): Observable<PageState> {
        return nextPage.lift(doOnFirst<PageResult<PageData>>(this::keepNextPageObservable))
                .map { this.loadedPageState(it) }
                .onErrorReturn { this.errorPageState(it) }
                .startWith(loadingPageState())
    }

    private fun loadingPageState(): PageState = PageState(loading = true)

    private fun loadedPageState(result: PageResult<PageData>): PageState = PageState(data = Optional.of(result.data))

    private fun keepNextPageObservable(datas: PageResult<PageData>) {
        nextPage.onNext(datas.nextPage)
    }

    private fun hasMorePages(): Boolean = nextPage.hasValue() && nextPage.value.isPresent

    class Builder<PageData, FirstPageParamsType>(private val firstPageRequested: Observable<FirstPageParamsType>,
                                                             private val firstPage: (FirstPageParamsType) -> Observable<PageResult<PageData>>) {
        private var refreshRequested = Observable.never<FirstPageParamsType>()
        private var refreshWith: (FirstPageParamsType) -> Observable<PageResult<PageData>> = { empty() }
        private var nextPageRequested = Observable.never<Any>()
        private var combinerOpt = Optional.absent<(PageData, PageData) -> PageData>()

        fun withRefresh(refreshSignal: Observable<FirstPageParamsType>,
                        paramsToRefreshOp: (FirstPageParamsType) -> Observable<PageResult<PageData>>): Builder<PageData, FirstPageParamsType> {
            this.refreshRequested = refreshSignal
            this.refreshWith = paramsToRefreshOp
            return this
        }

        fun withPaging(nextPageSignal: Observable<Any>,
                       pageCombiner: (PageData, PageData) -> PageData): Builder<PageData, FirstPageParamsType> {
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
                                                                  loadInitialPageWith: (FirstPageParamsType) -> Observable<PageResult<PageData>>):
                Builder<PageData, FirstPageParamsType> = Builder(initialLoadSignal, loadInitialPageWith)
    }
}
