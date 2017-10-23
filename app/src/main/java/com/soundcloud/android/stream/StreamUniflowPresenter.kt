package com.soundcloud.android.stream

import com.soundcloud.android.rx.RxSignal
import com.soundcloud.android.utils.collection.AsyncLoader
import com.soundcloud.android.utils.collection.AsyncLoaderState
import com.soundcloud.android.view.BasePresenter
import com.soundcloud.android.view.BaseView
import com.soundcloud.java.optional.Optional
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Provider

internal class StreamUniflowPresenter
@Inject
constructor(private val streamOperations: StreamOperations) : BasePresenter<List<StreamItem>, RxSignal, StreamUniflowView>() {

    override fun firstPageFunc(pageParams: RxSignal): Observable<AsyncLoader.PageResult<List<StreamItem>>> {
        return streamOperations.initialStreamItems().map {
            AsyncLoader.PageResult.from(it, nextPage(it))
        }.toObservable()
    }

    override fun refreshFunc(pageParams: RxSignal): Observable<AsyncLoader.PageResult<List<StreamItem>>> {
        return streamOperations.updatedStreamItems().map {
            AsyncLoader.PageResult.from(it, nextPage(it))
        }.toObservable()
    }

    private fun nextPage(currentPage: List<StreamItem>): Optional<Provider<Observable<AsyncLoader.PageResult<List<StreamItem>>>>> {
        return streamOperations.nextPageItems(currentPage).transform { Provider { it.map { AsyncLoader.PageResult.from(it, nextPage(it)) } } }
    }

    fun scrollToTop() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun onFocusChange(hasFocus: Boolean) {

    }
}

internal interface StreamUniflowView : BaseView<AsyncLoaderState<List<StreamItem>>, RxSignal>
