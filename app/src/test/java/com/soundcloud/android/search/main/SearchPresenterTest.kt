package com.soundcloud.android.search.main

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.analytics.EventTracker
import com.soundcloud.android.analytics.ReferringEventProvider
import com.soundcloud.android.events.ReferringEvent
import com.soundcloud.android.events.ScreenEvent
import com.soundcloud.android.main.Screen
import com.soundcloud.android.navigation.NavigationTarget
import com.soundcloud.android.navigation.Navigator
import com.soundcloud.android.rx.RxSignal
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.java.optional.Optional
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

@Suppress("IllegalIdentifier")
class SearchPresenterTest : AndroidUnitTest() {

    @Mock private lateinit var navigator: Navigator
    @Mock private lateinit var eventTracker: EventTracker
    @Mock private lateinit var referringEventProvider: ReferringEventProvider

    private lateinit var searchPresenter: SearchPresenter
    private val searchSignalSubject = PublishSubject.create<RxSignal>()
    private val enterScreenTimestamp = PublishSubject.create<Pair<Long, Screen>>()
    private val referringEvent = Optional.of(ReferringEvent.create("id", "kind"))

    @Before
    fun setUp() {
        whenever(referringEventProvider.referringEvent).thenReturn(referringEvent)
        searchPresenter = SearchPresenter(navigator, eventTracker, referringEventProvider)
    }

    @Test
    fun `search click navigates to search`() {
        val view: SearchView = initView()
        searchPresenter.attachView(view)

        searchSignalSubject.onNext(RxSignal.SIGNAL)

        verify(navigator).navigateTo(eq(NavigationTarget.forSearchAutocomplete(Screen.SEARCH_MAIN)))
    }

    @Test
    fun `handles screen tracking`() {
        val view: SearchView = initView()
        searchPresenter.attachView(view)

        enterScreenTimestamp.onNext(Pair(1L, Screen.SEARCH_MAIN))

        verify(eventTracker).trackScreen(ScreenEvent.create(Screen.SEARCH_MAIN.get()), referringEvent)
    }

    @Test
    fun `does not track for wrong screen`() {
        val view: SearchView = initView()
        searchPresenter.attachView(view)

        enterScreenTimestamp.onNext(Pair(1L, Screen.UNKNOWN))

        verify(eventTracker, never()).trackScreen(any(), any())
    }

    private fun initView(): SearchView {
        val view: SearchView = mock()
        whenever(view.searchClick).thenReturn(searchSignalSubject)
        whenever(view.enterScreenTimestamp).thenReturn(enterScreenTimestamp)
        whenever(view.requestContent()).thenReturn(Observable.just(RxSignal.SIGNAL))
        whenever(view.refreshSignal()).thenReturn(Observable.never())
        return view
    }

}
