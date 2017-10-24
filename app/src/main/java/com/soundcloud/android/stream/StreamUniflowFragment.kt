package com.soundcloud.android.stream

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.R
import com.soundcloud.android.SoundCloudApplication
import com.soundcloud.android.main.MainPagerAdapter
import com.soundcloud.android.rx.RxSignal
import com.soundcloud.android.search.SearchEmptyStateProvider
import com.soundcloud.android.utils.collection.AsyncLoaderState
import com.soundcloud.android.view.BaseFragment
import com.soundcloud.android.view.collection.CollectionRenderer
import com.soundcloud.android.view.collection.CollectionRendererState
import dagger.Lazy
import io.reactivex.Observable
import javax.inject.Inject

internal class StreamUniflowFragment : BaseFragment<StreamUniflowPresenter>(), StreamUniflowView, MainPagerAdapter.ScrollContent, MainPagerAdapter.FocusListener {
    @Inject internal lateinit var presenterLazy: Lazy<StreamUniflowPresenter>
    @Inject internal lateinit var adapter: StreamAdapter

    private lateinit var collectionRenderer: CollectionRenderer<StreamItem, RecyclerView.ViewHolder>

    init {
        SoundCloudApplication.getObjectGraph().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectionRenderer = CollectionRenderer(adapter = adapter,
                                                sameIdentity = { firstItem, secondItem -> firstItem.identityEquals(secondItem) },
                                                emptyStateProvider = SearchEmptyStateProvider(),
                                                animateLayoutChangesInItems = true,
                                                parallaxImageScrolling = true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.recyclerview_with_refresh_without_empty, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        collectionRenderer.attach(view, layoutManager = LinearLayoutManager(view.context))
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        collectionRenderer.detach()
        super.onDestroyView()
    }

    override fun createPresenter() = presenterLazy.get()

    override fun connectPresenter(presenter: StreamUniflowPresenter) {
        presenter.attachView(this)
    }

    override fun disconnectPresenter(presenter: StreamUniflowPresenter) {
        presenter.detachView()
    }

    override fun requestContent(): Observable<RxSignal> = Observable.just(RxSignal.SIGNAL)

    override fun refreshSignal(): Observable<RxSignal> = collectionRenderer.onRefresh()

    override fun accept(viewModel: AsyncLoaderState<List<StreamItem>>) {
        collectionRenderer.render(CollectionRendererState(viewModel.asyncLoadingState, viewModel.data ?: emptyList()))
    }

    override fun resetScroll() {
        presenter?.scrollToTop()
    }

    override fun onFocusChange(hasFocus: Boolean) {
        presenter?.onFocusChange(hasFocus)
    }
}
