package com.soundcloud.android.search.history

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.R
import com.soundcloud.android.SoundCloudApplication
import com.soundcloud.android.rx.RxSignal
import com.soundcloud.android.search.history.SearchHistoryAdapter.ViewHolder
import com.soundcloud.android.utils.collection.AsyncLoaderState
import com.soundcloud.android.view.BaseFragment
import com.soundcloud.android.view.collection.CollectionRenderer
import com.soundcloud.android.view.collection.CollectionRendererState
import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class SearchHistoryFragment : BaseFragment<SearchHistoryPresenter>(), SearchHistoryView {

    override val itemClickListener: PublishSubject<SearchHistoryItem> = PublishSubject.create()
    override val autocompleteArrowClickListener: PublishSubject<SearchHistoryItem> = PublishSubject.create()

    @Inject internal lateinit var searchHistoryCellRenderer: SearchHistoryCellRenderer.Factory
    @Inject internal lateinit var adapterFactory: SearchHistoryAdapter.Factory
    @Inject lateinit var presenterLazy: Lazy<SearchHistoryPresenter>
    private lateinit var collectionRenderer: CollectionRenderer<SearchHistoryItem, ViewHolder>

    override fun requestContent(): Observable<RxSignal> = Observable.just(RxSignal.SIGNAL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectionRenderer = CollectionRenderer(adapterFactory.create(searchHistoryCellRenderer,
                                                                      itemClickListener,
                                                                      autocompleteArrowClickListener),
                                                sameIdentity = { (item1), (item2) -> item1 == item2 },
                                                emptyStateProvider = null,
                                                animateLayoutChangesInItems = false,
                                                showDividers = false,
                                                parallaxImageScrolling = false)
    }

    override fun accept(loaderState: AsyncLoaderState<List<SearchHistoryItem>, RxSignal>) = with(loaderState) {
        collectionRenderer.render(CollectionRendererState(asyncLoadingState, data.or(emptyList())))
    }

    override fun disconnectPresenter(presenter: SearchHistoryPresenter) = presenter.detachView()

    override fun connectPresenter(presenter: SearchHistoryPresenter) = presenter.attachView(this)

    override fun createPresenter(): SearchHistoryPresenter = presenterLazy.get()

    init {
        SoundCloudApplication.getObjectGraph().inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.recyclerview, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        collectionRenderer.attach(view, false, LinearLayoutManager(view.context))
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        collectionRenderer.detach()
        super.onDestroyView()
    }

    companion object {
        const val TAG = "search_history"
    }
}

