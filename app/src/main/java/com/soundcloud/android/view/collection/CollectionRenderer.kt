package com.soundcloud.android.view.collection

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.View
import com.soundcloud.android.R
import com.soundcloud.android.model.AsyncLoadingState
import com.soundcloud.android.presentation.DividerItemDecoration
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter
import com.soundcloud.android.presentation.RecyclerItemAdapter
import com.soundcloud.android.presentation.RecyclerViewPaginator
import com.soundcloud.android.rx.RxSignal
import com.soundcloud.android.view.EmptyStatus
import com.soundcloud.android.view.MultiSwipeRefreshLayout
import com.soundcloud.android.view.adapters.RecyclerViewParallaxer
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

@Suppress("TooManyFunctions")
class CollectionRenderer<ItemT, VH : RecyclerView.ViewHolder>(private val adapter: PagingRecyclerItemAdapter<ItemT, VH>,
                                                              private val sameIdentity: (ItemT, ItemT) -> Boolean,
                                                              private val sameContest: (ItemT, ItemT) -> Boolean,
                                                              private val emptyStateProvider: EmptyStateProvider?,
                                                              private val animateLayoutChangesInItems: Boolean = false,
                                                              private val showDividers: Boolean = false,
                                                              private val parallaxImageScrolling: Boolean = false) {
    var recyclerView: RecyclerView? = null
    var swipeRefreshLayout: MultiSwipeRefreshLayout? = null

    private var emptyViewObserver: RecyclerView.AdapterDataObserver? = null

    private val onRefresh = PublishSubject.create<RxSignal>()
    private val onNextPage = PublishSubject.create<RxSignal>()
    private var requestMoreOnScroll: Boolean = false
    private var emptyAdapter: EmptyAdapter? = null
    private var paginator: RecyclerViewPaginator? = null

    init {
        adapter.setOnErrorRetryListener { _ -> onNextPage.onNext(RxSignal.SIGNAL) }
    }

    fun attach(view: View, renderEmptyAtTop: Boolean, layoutmanager: RecyclerView.LayoutManager) {

        if (recyclerView != null) {
            throw IllegalStateException("Recycler View already atteched. Did you forget to detach?")
        }
        recyclerView = view.findViewById(R.id.ak_recycler_view)
        swipeRefreshLayout = view.findViewById(R.id.str_layout)

        configureRecyclerView(layoutmanager)
        emptyStateProvider?.apply {
            emptyAdapter = EmptyAdapter(emptyStateProvider, renderEmptyAtTop)
        }

        // handle swipe to refresh
        swipeRefreshLayout?.apply {
            setSwipeableChildren(recyclerView)
            setOnRefreshListener { onRefresh.onNext(RxSignal.SIGNAL) }
        }
        recyclerView?.let {
            paginator = RecyclerViewPaginator(it) { this.nextPage() }
            paginator?.start()
        }
    }

    private fun nextPage() {
        if (requestMoreOnScroll) {
            onNextPage.onNext(RxSignal.SIGNAL)
        }
    }

    fun onRefresh(): PublishSubject<RxSignal> = onRefresh

    fun onNextPage(): Observable<RxSignal> = onNextPage

    fun detach() {
        paginator?.stop()

        emptyViewObserver?.let {
            adapter.unregisterAdapterDataObserver(it)
        }

        emptyViewObserver = null
        recyclerView?.adapter = null
        recyclerView = null
    }

    fun render(state: CollectionRendererState<ItemT>) {

        requestMoreOnScroll = state.collectionLoadingState.requestMoreOnScroll()

        adapter.setNewAppendState(getAppendState(state.collectionLoadingState))

        swipeRefreshLayout?.isRefreshing = state.collectionLoadingState.isRefreshing
        if (state.items.isEmpty()) {
            if (recyclerView?.adapter !== emptyAdapter) {
                recyclerView?.adapter = emptyAdapter
                setAnimateItemChanges(true)
            }
            updateEmptyView(state)
        } else {
            if (recyclerView?.adapter !== adapter) {
                recyclerView?.adapter = adapter
                setAnimateItemChanges(animateLayoutChangesInItems)
                populateAdapter(state.items)
                adapter.notifyDataSetChanged()
            } else {
                onNewItems(state.items)
            }
        }
    }

    private fun getAppendState(asyncLoadingState: AsyncLoadingState): PagingRecyclerItemAdapter.AppendState {
        return when {
            asyncLoadingState.isLoadingNextPage -> PagingRecyclerItemAdapter.AppendState.LOADING
            asyncLoadingState.nextPageError().isPresent -> PagingRecyclerItemAdapter.AppendState.ERROR
            else -> PagingRecyclerItemAdapter.AppendState.IDLE
        }
    }

    private fun setAnimateItemChanges(supportsChangeAnimations: Boolean) {
        recyclerView?.itemAnimator.apply {
            if (this is SimpleItemAnimator) {
                this.supportsChangeAnimations = supportsChangeAnimations
            }
        }
    }

    private fun configureRecyclerView(layoutManager: RecyclerView.LayoutManager) {
        recyclerView?.apply {
            this.layoutManager = layoutManager
            if (showDividers) {
                addListDividers(this)
            }
            if (parallaxImageScrolling) {
                addOnScrollListener(RecyclerViewParallaxer())
            }
        }

    }

    private fun addListDividers(recyclerView: RecyclerView) {
        val divider = ContextCompat.getDrawable(recyclerView.context, com.soundcloud.androidkit.R.drawable.ak_list_divider_item)
        val dividerHeight = recyclerView.resources.getDimensionPixelSize(com.soundcloud.androidkit.R.dimen.ak_list_divider_horizontal_height)
        recyclerView.addItemDecoration(NewDividerItemDecoration(divider, dividerHeight))
    }

    private fun updateEmptyView(state: CollectionRendererState<ItemT>) {
        val viewErrorOptional = state.collectionLoadingState.nextPageError()
        emptyAdapter?.apply {
            setEmptyStatus(EmptyStatus.fromErrorAndLoading(viewErrorOptional, state.collectionLoadingState.isLoadingNextPage))
            notifyDataSetChanged()
        }
    }

    fun adapter(): RecyclerItemAdapter<ItemT, VH> = adapter

    private fun onNewItems(newItems: List<ItemT>) {
        val oldItems = adapter().items
        val diffResult = DiffUtil.calculateDiff(AdapterDiffCallback(oldItems, newItems), true)

        populateAdapter(newItems)
        diffResult.dispatchUpdatesTo(adapter())
    }

    private fun populateAdapter(newItems: List<ItemT>) {
        adapter.clear()
        for (item in newItems) {
            adapter.addItem(item)
        }
    }

    interface EmptyStateProvider {

        fun waitingView(): Int

        fun connectionErrorView(): Int

        fun serverErrorView(): Int

        fun emptyView(): Int
    }

    private inner class AdapterDiffCallback internal constructor(private val oldItems: List<ItemT>, private val newItems: List<ItemT>) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            return sameIdentity.invoke(oldItem, newItem)
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            return sameContest.invoke(oldItem, newItem)
        }
    }

    // For some reason, our old DividerItemDecoration did not work without this measurement
    private class NewDividerItemDecoration internal constructor(divider: Drawable, private val thickness: Int) : DividerItemDecoration(divider, thickness) {

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
            super.getItemOffsets(outRect, view, parent, state)
            if (parent.getChildAdapterPosition(view) != 0) {
                outRect.top = thickness
            }
        }
    }
}
