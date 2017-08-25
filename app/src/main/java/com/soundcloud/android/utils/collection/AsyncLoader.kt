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
import io.reactivex.functions.Function3
import io.reactivex.observables.ConnectableObservable
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Provider

@SuppressWarnings("TooManyFunctions")
class AsyncLoader<PageData, ActionType, FirstPageParamsType> internal constructor(private val firstPageRequested: Observable<FirstPageParamsType>,
                                                                                  private val paramsToFirstPage: Function<FirstPageParamsType, Observable<PageResult<PageData, ActionType>>>,
                                                                                  private val refreshRequested: Observable<FirstPageParamsType>,
                                                                                  private val paramsToRefresh: Function<FirstPageParamsType, Observable<PageResult<PageData, ActionType>>>,
                                                                                  private val nextPageRequested: Observable<Any>,
                                                                                  private val pageCombiner: Optional<BiFunction<PageData, PageData, PageData>>,
                                                                                  private val actionPerformedSignal: Observable<ActionType>)
    : Observable<AsyncLoaderState<PageData, ActionType>>() {

    private val compositeDisposable = CompositeDisposable()
    private val nextPage = BehaviorSubject.create<Optional<Provider<Observable<PageResult<PageData, ActionType>>>>>()
    private val refreshStateSubject = BehaviorSubject.createDefault(RefreshState(false, Optional.absent<Throwable>()))
    private val actionPerformedSubject = BehaviorSubject.createDefault(Optional.absent<ActionType>())

    class PageResult<PageData, ActionType> internal constructor(internal var data: PageData,
                                                                internal var action: Optional<ActionType> = Optional.absent(),
                                                                internal var nextPage: Optional<Provider<Observable<PageResult<PageData, ActionType>>>> = Optional.absent()) {
        companion object {

            fun <PageData, ActionType> from(data: PageData, nextPage: Provider<Observable<PageResult<PageData, ActionType>>>): PageResult<PageData, ActionType> {
                return PageResult(data, nextPage = Optional.of(nextPage))
            }

            fun <PageData, ActionType> from(data: PageData): PageResult<PageData, ActionType> {
                return PageResult(data)
            }

            fun <PageData, ActionType> from(data: PageData, nextPage: Optional<Provider<Observable<PageResult<PageData, ActionType>>>>): PageResult<PageData, ActionType> {
                return PageResult(data, nextPage = nextPage)
            }
        }
    }

    private data class RefreshState internal constructor(internal val loading: Boolean, internal val error: Optional<Throwable> = Optional.absent())

    private inner class PageState internal constructor(internal val data: Optional<PageData> = Optional.absent(),
                                                       internal val action: Optional<ActionType> = Optional.absent(),
                                                       internal val loading: Boolean = false,
                                                       internal val error: Optional<Throwable> = Optional.absent())

    override fun subscribeActual(observer: Observer<in AsyncLoaderState<PageData, ActionType>>) {
        compositeDisposable.add(actionPerformedSignal.subscribe { actionPerformedSubject.onNext(Optional.of(it)) })
        val sequenceStarters = refreshes().startWith(initialLoad())

        combineLatest(sequenceStarters.switchMap { createSequenceState(it) },
                      refreshStateSubject,
                      actionPerformedSubject,
                      Function3<AsyncLoaderState<PageData, ActionType>, RefreshState, Optional<ActionType>, AsyncLoaderState<PageData, ActionType>>
                      { pageDataAsyncLoaderState, refreshState, action ->
                          val updateWithRefresh = updateWithRefresh(pageDataAsyncLoaderState, refreshState)
                          if (updateWithRefresh.action.isPresent && action.isPresent && updateWithRefresh.action.get() == action.get()) {
                              updateWithRefresh.stripAction()
                          } else {
                              updateWithRefresh
                          }

                      })
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

    private fun createSequenceState(firstPage: Observable<PageState>): Observable<AsyncLoaderState<PageData, ActionType>> {
        return pageEmitter().startWith(firstPage)
                .scan(mutableListOf<Observable<PageState>>(), { observables, partialStateObservable -> this.addNewPage(observables, partialStateObservable) })
                .switchMap { this.stateFromPages(it) }
    }

    private fun updateWithRefresh(pageDataAsyncLoaderState: AsyncLoaderState<PageData, ActionType>, refreshState: RefreshState): AsyncLoaderState<PageData, ActionType> {
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

    private fun stateFromPages(arr: List<Observable<PageState>>): Observable<AsyncLoaderState<PageData, ActionType>> {
        return Observable.combineLatest(arr) { this.combinePages(it) }
    }

    @Throws(Exception::class)
    private fun combinePages(objects: Array<Any>): AsyncLoaderState<PageData, ActionType> {
        val combinedData = combinePageData(objects)
        val lastPageState = toPageState(objects.last())
        val loadingState = AsyncLoadingState.builder()
                .isLoadingNextPage(lastPageState.loading)
                .requestMoreOnScroll(!lastPageState.loading && !lastPageState.error.isPresent && hasMorePages())
                .nextPageError(lastPageState.error.transform({ ViewError.from(it) }))
                .build()

        return if (combinedData != null) AsyncLoaderState(data = Optional.of(combinedData),
                                                          action = lastPageState.action,
                                                          asyncLoadingState = loadingState)
        else AsyncLoaderState(asyncLoadingState = loadingState,
                              action = lastPageState.action)
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
        return any as AsyncLoader<PageData, ActionType, FirstPageParamsType>.PageState
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
                    .lift(doOnFirst<PageResult<PageData, ActionType>>(this::keepNextPageObservable))
                    .map { this.loadedPageState(it) }
                    .onErrorReturn { this.errorPageState(it) }
                    .startWith(loadingPageState())
        }
    }

    private fun errorPageState(t: Throwable): PageState {
        return PageState(error = Optional.of(t))
    }

    private fun nextPageObservable(nextPage: Observable<PageResult<PageData, ActionType>>): Observable<PageState> {
        return nextPage.lift(doOnFirst<PageResult<PageData, ActionType>>(this::keepNextPageObservable))
                .map { this.loadedPageState(it) }
                .onErrorReturn { this.errorPageState(it) }
                .startWith(loadingPageState())
    }

    private fun loadingPageState(): PageState {
        return PageState(loading = true)
    }

    private fun loadedPageState(result: PageResult<PageData, ActionType>): PageState {
        return PageState(data = Optional.of(result.data), action = result.action)
    }

    private fun keepNextPageObservable(datas: PageResult<PageData, ActionType>) {
        nextPage.onNext(datas.nextPage)
    }

    private fun hasMorePages(): Boolean {
        return nextPage.hasValue() && nextPage.value.isPresent
    }

    class Builder<PageData, ActionType, FirstPageParamsType>(private val firstPageRequested: Observable<FirstPageParamsType>,
                                                             private val firstPage: Function<FirstPageParamsType, Observable<PageResult<PageData, ActionType>>>) {
        private var refreshRequested = Observable.never<FirstPageParamsType>()
        private var refreshWith: Function<FirstPageParamsType, Observable<PageResult<PageData, ActionType>>> = Function() { empty() }
        private var nextPageRequested = Observable.never<Any>()
        private var combinerOpt = Optional.absent<BiFunction<PageData, PageData, PageData>>()
        private var actionPerformed: Observable<ActionType> = Observable.empty()

        fun withRefresh(refreshSignal: Observable<FirstPageParamsType>,
                        paramsToRefreshOp: Function<FirstPageParamsType, Observable<PageResult<PageData, ActionType>>>): Builder<PageData, ActionType, FirstPageParamsType> {
            this.refreshRequested = refreshSignal
            this.refreshWith = paramsToRefreshOp
            return this
        }

        fun withPaging(nextPageSignal: Observable<Any>,
                       pageCombiner: BiFunction<PageData, PageData, PageData>): Builder<PageData, ActionType, FirstPageParamsType> {
            this.nextPageRequested = nextPageSignal
            this.combinerOpt = Optional.of(pageCombiner)
            return this
        }

        fun withAction(actionPerformed: Observable<ActionType>): Builder<PageData, ActionType, FirstPageParamsType> {
            this.actionPerformed = actionPerformed
            return this
        }

        fun build(): AsyncLoader<PageData, ActionType, FirstPageParamsType> {
            return AsyncLoader(
                    firstPageRequested,
                    firstPage,
                    refreshRequested,
                    refreshWith,
                    nextPageRequested,
                    combinerOpt,
                    actionPerformed
            )
        }
    }

    companion object {

        @JvmStatic
        fun <PageData, ActionType, FirstPageParamsType> startWith(initialLoadSignal: Observable<FirstPageParamsType>,
                                                                  loadInitialPageWith: Function<FirstPageParamsType, Observable<PageResult<PageData, ActionType>>>):
                Builder<PageData, ActionType, FirstPageParamsType> {
            return Builder(initialLoadSignal, loadInitialPageWith)
        }
    }
}
