package com.soundcloud.android.discovery

import com.soundcloud.android.analytics.EventTracker
import com.soundcloud.android.analytics.ReferringEventProvider
import com.soundcloud.android.events.ScreenEvent
import com.soundcloud.android.main.Screen
import com.soundcloud.android.model.Urn
import com.soundcloud.android.navigation.NavigationTarget
import com.soundcloud.android.navigation.Navigator
import com.soundcloud.android.playback.DiscoverySource
import com.soundcloud.android.rx.RxSignal
import com.soundcloud.android.rx.observers.LambdaObserver
import com.soundcloud.android.utils.Log
import com.soundcloud.android.utils.collection.AsyncLoader
import com.soundcloud.android.utils.collection.AsyncLoaderState
import com.soundcloud.android.view.BasePresenter
import com.soundcloud.android.view.BaseView
import com.soundcloud.java.optional.Optional
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import javax.inject.Inject

internal class HomePresenter
@Inject
constructor(private val discoveryOperations: DiscoveryOperations,
            private val navigator: Navigator,
            private val eventTracker: EventTracker,
            private val referringEventProvider: ReferringEventProvider)
    : BasePresenter<List<DiscoveryCardViewModel>, DiscoveryViewError, RxSignal, HomeView>() {

    override fun attachView(view: HomeView) {
        super.attachView(view)
        compositeDisposable.addAll(view.searchClick.subscribe { navigator.navigateTo(NavigationTarget.forSearchAutocomplete(Screen.DISCOVER)) },
                                   view.selectionItemClick
                                           .subscribeWith(
                                                   LambdaObserver.onNext<SelectionItemViewModel> {
                                                       it.trackingInfo.ifPresent { trackingInfo -> eventTracker.trackClick(trackingInfo.toUIEvent()) }
                                                       it.link().ifPresent { link ->
                                                           navigator.navigateTo(NavigationTarget.forNavigation(link,
                                                                                                               it.webLink,
                                                                                                               Screen.DISCOVER,
                                                                                                               Optional.of(DiscoverySource.RECOMMENDATIONS)))
                                                       }
                                                   }
                                           ),
                                   Observable.combineLatest<Long, List<DiscoveryCardViewModel>, Pair<Long, Optional<Urn>>>(view.enterScreenTimestamp
                                                                                                                                   .filter { it.second == Screen.DISCOVER }
                                                                                                                                   .map { it.first },
                                                                                                                           loader.map { it.data }.filter { it.isPresent }.map { it.get() },
                                                                                                                           BiFunction { first, second ->
                                                                                                                               Pair(first, Optional.fromNullable(second.responseQueryUrn()))
                                                                                                                           })
                                           .distinctUntilChanged { (first) -> first }
                                           .subscribeWith(LambdaObserver.onNext { pair -> this.trackPageView(pair.second) }))
    }

    override fun detachView() {
        super.detachView()
        compositeDisposable.clear()
    }

    override fun firstPageFunc(pageParams: RxSignal) =
            discoveryOperations.discoveryCards().toViewModelObservable()

    override fun refreshFunc(pageParams: RxSignal) =
            discoveryOperations.refreshDiscoveryCards().toViewModelObservable()

    private fun Single<DiscoveryResult>.toViewModelObservable(): Observable<AsyncLoader.PageResult<List<DiscoveryCardViewModel>, DiscoveryViewError>> {
        return this.map {
            AsyncLoader.PageResult(data = toViewModel(it), action = it.syncError.transform { DiscoveryViewError(it) })
        }.toObservable()
    }

    private fun List<DiscoveryCardViewModel>.responseQueryUrn(): Urn? {
        val cardWithQueryUrn = this.find { it.parentQueryUrn.isPresent }
        return cardWithQueryUrn?.parentQueryUrn?.orNull()
    }

    private fun trackPageView(queryUrn: Optional<Urn>) {
        eventTracker.trackScreen(ScreenEvent.create(Screen.DISCOVER.get(), queryUrn), referringEventProvider.referringEvent)
    }
}

internal fun toViewModel(discoveryResult: DiscoveryResult): List<DiscoveryCardViewModel> {
    val cardsWithSearch = discoveryResult.cards.toMutableList()
    cardsWithSearch.add(0, DiscoveryCard.forSearchItem())
    return cardsWithSearch.mapIndexed { index, discoveryCard ->
        when (discoveryCard) {
            is DiscoveryCard.MultipleContentSelectionCard ->
                DiscoveryCardViewModel.MultipleContentSelectionCard(discoveryCard,
                                                                    discoveryCard.selectionItems().map {
                                                                        SelectionItemViewModel(it,
                                                                                               DiscoveryTrackingManager.calculateUIEvent(it, discoveryCard, index))
                                                                    })
            is DiscoveryCard.SingleContentSelectionCard ->
                DiscoveryCardViewModel.SingleContentSelectionCard(discoveryCard,
                                                                  discoveryCard.selectionItem().let {
                                                                      SelectionItemViewModel(it,
                                                                                             DiscoveryTrackingManager.calculateUIEvent(it, discoveryCard, index))
                                                                  })
            is DiscoveryCard.EmptyCard -> DiscoveryCardViewModel.EmptyCard(throwable = discoveryCard.throwable())
            else -> DiscoveryCardViewModel.SearchCard

        }
    }
}

interface HomeView : BaseView<AsyncLoaderState<List<DiscoveryCardViewModel>, DiscoveryViewError>, DiscoveryViewError, RxSignal> {
    val selectionItemClick: Observable<SelectionItemViewModel>
    val searchClick: Observable<RxSignal>
    val enterScreenTimestamp: Observable<Pair<Long, Screen>>
}
