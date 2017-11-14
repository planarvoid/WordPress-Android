package com.soundcloud.android.search.main

import com.soundcloud.android.analytics.EventTracker
import com.soundcloud.android.analytics.ReferringEventProvider
import com.soundcloud.android.events.ScreenEvent
import com.soundcloud.android.main.Screen
import com.soundcloud.android.navigation.NavigationTarget
import com.soundcloud.android.navigation.Navigator
import com.soundcloud.android.rx.RxSignal
import com.soundcloud.android.rx.observers.LambdaObserver
import com.soundcloud.android.utils.collection.AsyncLoader
import com.soundcloud.android.utils.collection.AsyncLoaderState
import com.soundcloud.android.view.BasePresenter
import com.soundcloud.android.view.BaseView
import io.reactivex.Observable
import javax.inject.Inject

class SearchPresenter
@Inject
constructor(private val navigator: Navigator,
            private val eventTracker: EventTracker,
            private val referringEventProvider: ReferringEventProvider) : BasePresenter<List<SearchItemViewModel>, RxSignal, SearchView>() {

    override fun firstPageFunc(pageParams: RxSignal): Observable<AsyncLoader.PageResult<List<SearchItemViewModel>>> =
            Observable.just(AsyncLoader.PageResult.from(listOf(SearchItemViewModel.SearchCard(), SearchItemViewModel.EmptyCard())))

    override fun attachView(view: SearchView) {
        super.attachView(view)
        compositeDisposable.addAll(view.searchClick.subscribe { navigator.navigateTo(NavigationTarget.forSearchAutocomplete(Screen.SEARCH_MAIN)) },
                                   view.enterScreenTimestamp
                                           .filter { it.second == Screen.SEARCH_MAIN }
                                           .subscribeWith(LambdaObserver.onNext { trackPageView() }))
    }

    private fun trackPageView() {
        eventTracker.trackScreen(ScreenEvent.create(Screen.SEARCH_MAIN.get()), referringEventProvider.referringEvent)
    }
}

interface SearchView : BaseView<AsyncLoaderState<List<SearchItemViewModel>>, RxSignal> {
    val searchClick: Observable<RxSignal>
    val enterScreenTimestamp: Observable<Pair<Long, Screen>>
}
