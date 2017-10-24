package com.soundcloud.android.discovery

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.R
import com.soundcloud.android.SoundCloudApplication
import com.soundcloud.android.feedback.Feedback
import com.soundcloud.android.main.MainActivity
import com.soundcloud.android.main.Screen
import com.soundcloud.android.rx.RxSignal
import com.soundcloud.android.search.SearchEmptyStateProvider
import com.soundcloud.android.search.SearchItemRenderer
import com.soundcloud.android.stream.StreamSwipeRefreshAttacher
import com.soundcloud.android.utils.collection.AsyncLoaderState
import com.soundcloud.android.utils.extensions.plusAssign
import com.soundcloud.android.view.BaseFragment
import com.soundcloud.android.view.ViewError
import com.soundcloud.android.view.collection.CollectionRenderer
import com.soundcloud.android.view.collection.CollectionRendererState
import com.soundcloud.android.view.snackbar.FeedbackController
import com.soundcloud.java.optional.Optional
import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

internal class HomeFragment : BaseFragment<HomePresenter>(), HomeView, SearchItemRenderer.SearchListener {
    @Inject internal lateinit var presenterLazy: Lazy<HomePresenter>
    @Inject internal lateinit var adapterFactory: DiscoveryAdapter.Factory
    @Inject internal lateinit var feedbackController: FeedbackController
    @Inject internal lateinit var swipeRefreshAttacher: StreamSwipeRefreshAttacher

    private lateinit var collectionRenderer: CollectionRenderer<DiscoveryCardViewModel, RecyclerView.ViewHolder>

    override val selectionItemClick: PublishSubject<SelectionItemViewModel> = PublishSubject.create<SelectionItemViewModel>()
    override val searchClick: PublishSubject<RxSignal> = PublishSubject.create<RxSignal>()
    override val enterScreenTimestamp: Observable<Pair<Long, Screen>> by lazy {
        val onResume = resume.filter { it.isPresent }.map { Pair(it.get(), Screen.DISCOVER) }
        val act = activity
        when (act) {
            is MainActivity -> act.pageSelectedTimestampWithScreen().flatMap { onResume }
            else -> onResume
        }
    }

    private val resume = BehaviorSubject.create<Optional<Long>>()
    private val compositeDisposable = CompositeDisposable()

    init {
        SoundCloudApplication.getObjectGraph().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val adapter = adapterFactory.create(this)
        compositeDisposable += adapter.selectionItemClick().subscribe(selectionItemClick::onNext)
        collectionRenderer = CollectionRenderer(adapter = adapter,
                                                sameIdentity = this::areItemsTheSame,
                                                emptyStateProvider = SearchEmptyStateProvider(),
                                                animateLayoutChangesInItems = true,
                                                parallaxImageScrolling = true)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        resume.onNext(Optional.of(System.currentTimeMillis()))
    }

    override fun onPause() {
        super.onPause()
        resume.onNext(Optional.absent())
    }

    private fun areItemsTheSame(item1: DiscoveryCardViewModel, item2: DiscoveryCardViewModel): Boolean =
            when {
                item1 is DiscoveryCardViewModel.SearchCard && item2 is DiscoveryCardViewModel.SearchCard -> true
                item1 is DiscoveryCardViewModel.EmptyCard && item2 is DiscoveryCardViewModel.EmptyCard -> true
                item1 is DiscoveryCardViewModel.SingleContentSelectionCard && item2 is DiscoveryCardViewModel.SingleContentSelectionCard -> item1.selectionUrn == item2.selectionUrn
                item1 is DiscoveryCardViewModel.MultipleContentSelectionCard && item2 is DiscoveryCardViewModel.MultipleContentSelectionCard -> item1.selectionUrn == item2.selectionUrn
                else -> {
                    false
                }
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

    override fun disconnectPresenter(presenter: HomePresenter) {
        presenter.detachView()
    }

    override fun connectPresenter(presenter: HomePresenter) {
        presenter.attachView(this)
    }

    override fun requestContent(): Observable<RxSignal> = Observable.just(RxSignal.SIGNAL)

    override fun refreshSignal(): PublishSubject<RxSignal> = collectionRenderer.onRefresh()

    override fun createPresenter(): HomePresenter = presenterLazy.get()

    override fun accept(viewModel: AsyncLoaderState<List<DiscoveryCardViewModel>>) {
        collectionRenderer.render(CollectionRendererState(viewModel.asyncLoadingState, viewModel.data ?: emptyList()))
    }

    override fun refreshErrorConsumer(viewError: ViewError) {
        when (viewError) {
            ViewError.CONNECTION_ERROR -> feedbackController.showFeedback(Feedback.create(R.string.discovery_error_offline, Feedback.LENGTH_LONG))
            ViewError.SERVER_ERROR -> feedbackController.showFeedback(Feedback.create(R.string.discovery_error_failed_to_load,
                                                                                      R.string.discovery_error_retry_button) { swipeRefreshAttacher.forceRefresh() })
        }
    }

    override fun onSearchClicked(context: Context) {
        searchClick.onNext(RxSignal.SIGNAL)
    }
}
