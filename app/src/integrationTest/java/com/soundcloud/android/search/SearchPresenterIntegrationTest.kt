package com.soundcloud.android.search

import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import com.soundcloud.android.BaseIntegrationTest
import com.soundcloud.android.SoundCloudApplication
import com.soundcloud.android.framework.TestUser
import com.soundcloud.android.hamcrest.TestAsyncState
import com.soundcloud.android.main.Screen
import com.soundcloud.android.rx.RxSignal
import com.soundcloud.android.search.main.SearchItemViewModel
import com.soundcloud.android.search.main.SearchPresenter
import com.soundcloud.android.search.main.SearchView
import com.soundcloud.android.utils.Supplier
import com.soundcloud.android.utils.collection.AsyncLoaderState
import io.reactivex.subjects.PublishSubject
import org.hamcrest.Matchers
import org.hamcrest.Matchers.empty
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SearchPresenterIntegrationTest : BaseIntegrationTest(TestUser.testUser) {
    private val SEARCH_PAGEVIEW_SPECS = "specs/search_pageview.spec"

    @Test
    fun presenterDoesNotEmitWhenNotConnected() {
        val testView = TestView()

        testView.assertState(empty<AsyncLoaderState<List<SearchItemViewModel>>>())
    }

    @Test
    fun presenterLoadsModelsWithoutWaitingForNetwork() {
        unrespondingNetwork()

        val testView = TestView()

        testView.initialLoadSignal.onNext(RxSignal.SIGNAL)
        testView.assertLastState({ it.hasData() }, Matchers.equalTo(true))
        testView.assertLastState({ it.cardsCount() }, Matchers.equalTo(2))
        testView.assertLastState({ it.card(0) }, Matchers.instanceOf(SearchItemViewModel.SearchCard::class.java))
        testView.assertLastState({ it.card(1) }, Matchers.instanceOf(SearchItemViewModel.EmptyCard::class.java))
    }

    @Test
    fun presenterTracksEnterEvent() {
        val testView = TestView()

        mrLocalLocal.startEventTracking()

        testView.enterScreenTimestamp.onNext(Pair(123L, Screen.SEARCH_MAIN))

        testView.initialLoadSignal.onNext(RxSignal.SIGNAL)

        mrLocalLocal.verify(SEARCH_PAGEVIEW_SPECS)
    }

    private fun AsyncLoaderState<*>.hasData(): Boolean = data != null
    private fun AsyncLoaderState<List<SearchItemViewModel>>.cardsCount(): Int = data?.size ?: 0
    private fun AsyncLoaderState<List<SearchItemViewModel>>.card(position: Int): SearchItemViewModel? =
            data?.let { if (it.size > position) it[position] else null }

    internal class TestView
    internal constructor(private val models: MutableList<AsyncLoaderState<List<SearchItemViewModel>>> = mutableListOf(),
                         override val searchClick: PublishSubject<RxSignal> = PublishSubject.create(),
                         val initialLoadSignal: PublishSubject<RxSignal> = PublishSubject.create<RxSignal>(),
                         override val enterScreenTimestamp: PublishSubject<Pair<Long, Screen>> = PublishSubject.create(),
                         searchPresenter: SearchPresenter = SoundCloudApplication.getObjectGraph().searchPresenter()) : TestAsyncState<AsyncLoaderState<List<SearchItemViewModel>>>(), SearchView {

        init {
            searchPresenter.attachView(this)
        }

        override fun states() = Supplier { models }

        override fun accept(viewModel: AsyncLoaderState<List<SearchItemViewModel>>) {
            models.add(viewModel)
        }

        override fun requestContent() = initialLoadSignal
    }
}
