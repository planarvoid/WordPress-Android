package com.soundcloud.android.view

import com.soundcloud.android.utils.collection.AsyncLoader
import com.soundcloud.android.utils.collection.AsyncLoaderState
import com.soundcloud.android.utils.extensions.plusAssign
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import io.reactivex.observables.ConnectableObservable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

abstract class BasePresenter<ViewModel, ActionType, PageParams, in View : BaseView<AsyncLoaderState<ViewModel, ActionType>, ActionType, PageParams>> : Destroyable() {

    private val requestContentSignal = PublishSubject.create<PageParams>()
    private val refreshSignal = PublishSubject.create<PageParams>()
    private val actionPerformedSignal = PublishSubject.create<ActionType>()
    val loader: ConnectableObservable<AsyncLoaderState<ViewModel, ActionType>> = AsyncLoader.Companion.startWith<ViewModel, ActionType, PageParams>(requestContentSignal.distinctUntilChanged(),
                                                                                                                                                    Function { firstPageFunc(it) })
            .withRefresh(refreshSignal, Function { refreshFunc(it) })
            .withAction(actionPerformedSignal)
            .build().observeOn(AndroidSchedulers.mainThread())
            .distinctUntilChanged()
            .replay(1)
    private val activityLifeCycleDisposable = CompositeDisposable()

    val compositeDisposable = CompositeDisposable()

    init {
        activityLifeCycleDisposable += loader.connect()
    }

    open fun attachView(view: View) {
        compositeDisposable.addAll(loader.subscribe(view),
                                   view.requestContent().subscribeWithSubject(requestContentSignal),
                                   view.refreshSignal().subscribeWithSubject(refreshSignal),
                                   view.actionPerformedSignal().subscribeWithSubject(actionPerformedSignal))
    }

    open fun detachView() {
        compositeDisposable.clear()
    }

    abstract fun firstPageFunc(pageParams: PageParams): Observable<AsyncLoader.PageResult<ViewModel, ActionType>>
    open fun refreshFunc(pageParams: PageParams): Observable<AsyncLoader.PageResult<ViewModel, ActionType>> = Observable.empty()
}

interface BaseView<ViewModel, ActionType, PageParams> : Consumer<ViewModel> {
    fun requestContent(): Observable<PageParams>
    fun refreshSignal(): Observable<PageParams> = Observable.empty<PageParams>()
    fun actionPerformedSignal(): Observable<ActionType> = Observable.empty()
}

private fun <Type> Observable<Type>.subscribeWithSubject(subject: Subject<Type>): Disposable = subscribe(subject::onNext, subject::onError, subject::onComplete)
