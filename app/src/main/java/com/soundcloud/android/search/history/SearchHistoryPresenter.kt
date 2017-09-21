package com.soundcloud.android.search.history

import com.soundcloud.android.rx.RxSignal
import com.soundcloud.android.utils.collection.AsyncLoader.PageResult
import com.soundcloud.android.utils.collection.AsyncLoaderState
import com.soundcloud.android.utils.extensions.plusAssign
import com.soundcloud.android.view.BasePresenter
import com.soundcloud.android.view.BaseView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class SearchHistoryPresenter
@Inject
internal constructor(private val searchHistoryStorage: SearchHistoryStorage)
    : BasePresenter<List<SearchHistoryItem>, RxSignal, RxSignal, SearchHistoryView>() {

    var itemClickListener: PublishSubject<SearchHistoryItem> = PublishSubject.create()
    var autocompleteArrowClickListener: PublishSubject<SearchHistoryItem> = PublishSubject.create()

    override fun attachView(view: SearchHistoryView) {
        super.attachView(view)
        compositeDisposable += view.itemClickListener.subscribe { itemClickListener.onNext(it) }
        compositeDisposable += view.autocompleteArrowClickListener.subscribe { autocompleteArrowClickListener.onNext(it) }
    }

    override fun firstPageFunc(pageParams: RxSignal): Observable<PageResult<List<SearchHistoryItem>, RxSignal>> =
            searchHistoryStorage.getSearchHistory()
                    .doOnNext { searchHistoryStorage.truncate(MAX_HISTORY_ITEMS) }
                    .map { PageResult<List<SearchHistoryItem>, RxSignal>(it) }

    companion object {
        private const val MAX_HISTORY_ITEMS = 5
    }
}

interface SearchHistoryView : BaseView<AsyncLoaderState<List<SearchHistoryItem>, RxSignal>, RxSignal, RxSignal> {
    val itemClickListener: Observable<SearchHistoryItem>
    val autocompleteArrowClickListener: Observable<SearchHistoryItem>
}
