package com.soundcloud.android.view

import com.soundcloud.android.utils.collection.AsyncLoader
import com.soundcloud.android.utils.collection.AsyncLoaderState
import com.soundcloud.android.utils.extensions.plusAssign
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.observables.ConnectableObservable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

abstract class BasePresenter<ViewModel, PageParams, in View : BaseView<AsyncLoaderState<ViewModel>, PageParams>> : Destroyable() {

    private val requestContentSignal = PublishSubject.create<PageParams>()
    private val refreshSignal = PublishSubject.create<PageParams>()
    private val refreshError = PublishSubject.create<ViewError>()
    val loader: ConnectableObservable<AsyncLoaderState<ViewModel>> =
            AsyncLoader.Companion.startWith<ViewModel, PageParams>(requestContentSignal.distinctUntilChanged(), { firstPageFunc(it) })
                    .withRefresh(refreshSignal, { refreshFunc(it).doOnError { refreshError.onNext(ViewError.from(it)) } })
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
                                   refreshError.subscribe(view.refreshErrorConsumer()))
    }

    open fun detachView() {
        compositeDisposable.clear()
    }

    abstract fun firstPageFunc(pageParams: PageParams): Observable<AsyncLoader.PageResult<ViewModel>>
    open fun refreshFunc(pageParams: PageParams): Observable<AsyncLoader.PageResult<ViewModel>> = Observable.empty()
}

interface BaseView<ViewModel, PageParams> : Consumer<ViewModel> {
    fun requestContent(): Observable<PageParams>
    fun refreshSignal(): Observable<PageParams> = Observable.empty<PageParams>()
    fun refreshErrorConsumer(): Consumer<ViewError> = Consumer {  }

}

private fun <Type> Observable<Type>.subscribeWithSubject(subject: Subject<Type>): Disposable = subscribe(subject::onNext, subject::onError, subject::onComplete)
