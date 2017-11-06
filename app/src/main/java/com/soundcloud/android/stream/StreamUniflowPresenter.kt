package com.soundcloud.android.stream

import com.soundcloud.android.rx.RxSignal
import com.soundcloud.android.utils.collection.AsyncLoader
import com.soundcloud.android.utils.collection.AsyncLoaderState
import com.soundcloud.android.view.BasePresenter
import com.soundcloud.android.view.BaseView
import com.soundcloud.java.optional.Optional
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject
import javax.inject.Provider

internal class StreamUniflowPresenter
@Inject
constructor(private val streamOperations: StreamUniflowOperations) : BasePresenter<List<StreamItem>, RxSignal, StreamUniflowView>() {

    override fun firstPageFunc(pageParams: RxSignal): Observable<AsyncLoader.PageResult<List<StreamItem>>> {
        return streamOperations.initialStreamItems()
                .observeOn(AndroidSchedulers.mainThread())
                .map {
                    AsyncLoader.PageResult.from(it, nextPage(it))
                }.toObservable()
    }

    override fun refreshFunc(pageParams: RxSignal): Observable<AsyncLoader.PageResult<List<StreamItem>>> {
        return streamOperations.updatedStreamItems()
                .observeOn(AndroidSchedulers.mainThread())
                .map {
                    AsyncLoader.PageResult.from(it, nextPage(it))
                }.toObservable()
    }

    override fun postProcessViewModel(viewModel: List<StreamItem>): Observable<List<StreamItem>> {
        return streamOperations.initialNotificationItem()
                .observeOn(AndroidSchedulers.mainThread())
                .map { notificationItem ->
                    viewModel.toMutableList().apply { add(0, notificationItem) }.toList()
                }.defaultIfEmpty(viewModel).toObservable()
    }

    private fun nextPage(currentPage: List<StreamItem>): Optional<Provider<Observable<AsyncLoader.PageResult<List<StreamItem>>>>> {
        return Optional.fromNullable(streamOperations.nextPageItems(currentPage)?.toObservable()?.let { Provider { it.map { AsyncLoader.PageResult.from(it, nextPage(it)) } } })
    }

    fun scrollToTop() {
        //        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun onFocusChange(hasFocus: Boolean) {

    }
}

internal interface StreamUniflowView : BaseView<AsyncLoaderState<List<StreamItem>>, RxSignal>
