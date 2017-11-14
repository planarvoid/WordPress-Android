package com.soundcloud.android.search.main

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.R
import com.soundcloud.android.SoundCloudApplication
import com.soundcloud.android.main.Screen
import com.soundcloud.android.rx.RxSignal
import com.soundcloud.android.search.SearchEmptyStateProvider
import com.soundcloud.android.search.SearchItemRenderer
import com.soundcloud.android.utils.collection.AsyncLoaderState
import com.soundcloud.android.view.BaseFragment
import com.soundcloud.android.view.collection.CollectionRenderer
import com.soundcloud.android.view.collection.CollectionRendererState
import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class SearchFragment : BaseFragment<SearchPresenter>(), SearchView, SearchItemRenderer.SearchListener {

    @Inject internal lateinit var presenterLazy: Lazy<SearchPresenter>
    private lateinit var collectionRenderer: CollectionRenderer<SearchItemViewModel, RecyclerView.ViewHolder>

    override val presenterKey = "SearchPresenterKey"
    override val searchClick: PublishSubject<RxSignal> = PublishSubject.create<RxSignal>()

    init {
        SoundCloudApplication.getObjectGraph().inject(this)
    }

    override fun getScreen(): Screen = Screen.SEARCH_MAIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectionRenderer = CollectionRenderer(adapter = SearchAdapter(searchListener = this),
                                                sameIdentity = this::areItemsTheSame,
                                                emptyStateProvider = SearchEmptyStateProvider(),
                                                animateLayoutChangesInItems = true,
                                                parallaxImageScrolling = true)
    }

    private fun areItemsTheSame(item1: SearchItemViewModel, item2: SearchItemViewModel): Boolean =
            when {
                item1 is SearchItemViewModel.SearchCard && item2 is SearchItemViewModel.SearchCard -> true
                item1 is SearchItemViewModel.EmptyCard && item2 is SearchItemViewModel.EmptyCard -> true
                else -> {
                    false
                }
            }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.recyclerview, container, false)
        view.setBackgroundColor(resources.getColor(R.color.page_background))
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        collectionRenderer.attach(view, layoutManager = LinearLayoutManager(view.context))
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        collectionRenderer.detach()
        super.onDestroyView()
    }

    override fun createPresenter(): SearchPresenter = presenterLazy.get()

    override fun requestContent(): Observable<RxSignal> = Observable.just(RxSignal.SIGNAL)

    override fun accept(viewModel: AsyncLoaderState<List<SearchItemViewModel>>) {
        collectionRenderer.render(CollectionRendererState(viewModel.asyncLoadingState, viewModel.data ?: emptyList()))
    }

    override fun connectPresenter(presenter: SearchPresenter) = presenter.attachView(this)

    override fun disconnectPresenter(presenter: SearchPresenter) = presenter.detachView()

    override fun onSearchClicked(context: Context) {
        searchClick.onNext(RxSignal.SIGNAL)
    }

}
