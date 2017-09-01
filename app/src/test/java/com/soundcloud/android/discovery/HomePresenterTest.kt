package com.soundcloud.android.discovery

import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.analytics.EventTracker
import com.soundcloud.android.analytics.ReferringEventProvider
import com.soundcloud.android.main.Screen
import com.soundcloud.android.model.AsyncLoadingState
import com.soundcloud.android.model.Urn
import com.soundcloud.android.navigation.NavigationTarget
import com.soundcloud.android.navigation.Navigator
import com.soundcloud.android.playback.DiscoverySource
import com.soundcloud.android.rx.RxSignal
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.android.utils.collection.AsyncLoaderState
import com.soundcloud.java.optional.Optional
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.SingleSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

@Suppress("IllegalIdentifier")
class HomePresenterTest : AndroidUnitTest() {

    @Mock private lateinit var discoveryOperations: DiscoveryOperations
    @Mock private lateinit var navigator: Navigator
    @Mock private lateinit var eventTracker: EventTracker
    @Mock private lateinit var referringEventProvider: ReferringEventProvider

    private lateinit var newHomePresenter: HomePresenter
    private val refreshSignalSubject = PublishSubject.create<RxSignal>()
    private val searchSignalSubject = PublishSubject.create<RxSignal>()
    private val actionPerformedSignal = PublishSubject.create<DiscoveryViewError>()
    private val selectionItemClickSubject = PublishSubject.create<SelectionItemViewModel>()
    private val enterScreenTimestamp = PublishSubject.create<Pair<Long, Screen>>()

    private val emptyList = listOf<DiscoveryCardViewModel>(DiscoveryCardViewModel.SearchCard)

    @Before
    fun setUp() {
        newHomePresenter = HomePresenter(discoveryOperations, navigator, eventTracker, referringEventProvider)
    }

    @Test
    fun `emits cached view model on reattach to view`() {
        val source = SingleSubject.create<DiscoveryResult>()
        whenever(discoveryOperations.discoveryCards()).thenReturn(source)

        val view = initView()
        newHomePresenter.attachView(view)
        verify(view).accept(AsyncLoaderState.loadingNextPage())

        source.onSuccess(DiscoveryResult())

        verify(view).accept(AsyncLoaderState<List<DiscoveryCardViewModel>, DiscoveryViewError>(data = Optional.of(emptyList)))

        newHomePresenter.detachView()

        val newView = initView()
        newHomePresenter.attachView(newView)

        verify(newView).accept(AsyncLoaderState<List<DiscoveryCardViewModel>, DiscoveryViewError>(data = Optional.of(emptyList)))
    }

    @Test
    fun `emits view model on pull to refresh`() {
        whenever(discoveryOperations.discoveryCards()).thenReturn(Single.just(DiscoveryResult()))
        val card = DiscoveryFixtures.SINGLE_CONTENT_SELECTION_CARD
        val discoveryResult = DiscoveryResult(listOf(card))
        whenever(discoveryOperations.refreshDiscoveryCards()).thenReturn(Single.just(discoveryResult))

        val view: HomeView = initView()
        newHomePresenter.attachView(view)
        verify(view).accept(AsyncLoaderState.loadingNextPage())
        verify(view).accept(AsyncLoaderState(data = Optional.of(emptyList)))

        refreshSignalSubject.onNext(RxSignal.SIGNAL)

        verify(view).accept(AsyncLoaderState(asyncLoadingState = AsyncLoadingState.builder().isRefreshing(true).build(), data = Optional.of(emptyList)))
        verify(view, times(2)).accept(AsyncLoaderState(data = Optional.of(emptyList)))
        verify(view).accept(AsyncLoaderState(data = Optional.of(toViewModel(discoveryResult))))
    }

    @Test
    fun `search click navigates to search`() {
        whenever(discoveryOperations.discoveryCards()).thenReturn(Single.never())
        whenever(discoveryOperations.refreshDiscoveryCards()).thenReturn(Single.never())

        val view: HomeView = initView()
        newHomePresenter.attachView(view)

        searchSignalSubject.onNext(RxSignal.SIGNAL)

        verify(navigator).navigateTo(eq(NavigationTarget.forSearchAutocomplete(Screen.DISCOVER)))
    }

    @Test
    fun `selection item click navigates to deeplink`() {
        whenever(discoveryOperations.discoveryCards()).thenReturn(Single.never())
        whenever(discoveryOperations.refreshDiscoveryCards()).thenReturn(Single.never())

        val view: HomeView = initView()
        newHomePresenter.attachView(view)

        val link = "link"
        val webLink = Optional.of("webLink")
        selectionItemClickSubject.onNext(SelectionItemViewModel(selectionUrn = Urn.forSystemPlaylist("123"),
                                                                appLink = Optional.of(link),
                                                                webLink = webLink))

        verify(navigator).navigateTo(eq(NavigationTarget.forNavigation(link,
                                                                       webLink,
                                                                       Screen.DISCOVER,
                                                                       Optional.of(DiscoverySource.RECOMMENDATIONS))))
    }

    private fun initView(): HomeView {
        val view: HomeView = mock()
        whenever(view.requestContent()).thenReturn(Observable.just(RxSignal.SIGNAL))
        whenever(view.refreshSignal()).thenReturn(refreshSignalSubject)
        whenever(view.actionPerformedSignal()).thenReturn(actionPerformedSignal)
        whenever(view.searchClick).thenReturn(searchSignalSubject)
        whenever(view.selectionItemClick).thenReturn(selectionItemClickSubject)
        whenever(view.enterScreenTimestamp).thenReturn(enterScreenTimestamp)
        return view
    }
}
