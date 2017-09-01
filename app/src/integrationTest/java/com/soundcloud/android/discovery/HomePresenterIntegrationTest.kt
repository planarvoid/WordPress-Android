package com.soundcloud.android.discovery

import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import com.soundcloud.android.BaseIntegrationTest
import com.soundcloud.android.SoundCloudApplication
import com.soundcloud.android.api.ApiEndpoints
import com.soundcloud.android.api.ApiRequestException
import com.soundcloud.android.framework.TestUser
import com.soundcloud.android.hamcrest.TestAsyncState
import com.soundcloud.android.main.Screen
import com.soundcloud.android.rx.RxSignal
import com.soundcloud.android.utils.Supplier
import com.soundcloud.android.utils.collection.AsyncLoaderState
import io.reactivex.subjects.PublishSubject
import org.hamcrest.Matchers
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class HomePresenterIntegrationTest : BaseIntegrationTest(TestUser.testUser) {
    private val DISCOVERY_CARDS = "discovery-cards.json"
    private val HOME_PAGEVIEW_SPECS = "specs/home_pageview.spec"
    private val HOME_PAGEVIEW_AND_CARD_CLICK_SPECS = "specs/home_pageview_and_card_click.spec"

    @Test
    fun presenterDoesNotEmitWhenNotConnected() {
        noNetwork()

        val testView = TestView()

        testView.assertState(empty<AsyncLoaderState<List<DiscoveryCardViewModel>, DiscoveryViewError>>())
    }

    @Test
    fun presenterStartsWithEmptyModel() {
        unrespondingNetwork()

        val testView = TestView()

        testView.initialLoadSignal.onNext(RxSignal.SIGNAL)

        testView.assertState(contains(loadingState()))
    }

    @Test
    fun presenterShowsNetworkError() {
        noNetwork()

        val testView = TestView()

        testView.initialLoadSignal.onNext(RxSignal.SIGNAL)

        testView.assertState(contains(loadingState()))
        testView.assertLastState({ it.hasData() }, equalTo(true))
        testView.assertLastState({ it.cardsCount() }, equalTo(2))
        testView.assertLastState({ it.card(0) }, equalTo<DiscoveryCardViewModel>(DiscoveryCardViewModel.SearchCard))
        testView.assertLastState({ it.card(1) }, instanceOf(DiscoveryCardViewModel.EmptyCard::class.java))
        testView.assertLastState({ it.exception(1) }, Matchers.notNullValue())
        testView.assertLastState({ it.exception(1)?.isNetworkError }, equalTo<Boolean>(true))
    }

    @Test
    fun presenterShowsDiscoveryCards() {
        addMockedResponse(ApiEndpoints.DISCOVERY_CARDS.path(), DISCOVERY_CARDS)

        val testView = TestView()

        testView.initialLoadSignal.onNext(RxSignal.SIGNAL)

        testView.assertLastState({ it.hasData() }, equalTo(true))
        testView.assertLastState({ it.hasViewError() }, equalTo(false))
        testView.assertLastState({ it.cardsCount() }, equalTo(3))
        testView.assertLastState({ it.card(0) }, equalTo<DiscoveryCardViewModel>(DiscoveryCardViewModel.SearchCard))
        testView.assertLastState({ it.card(1) }, instanceOf(DiscoveryCardViewModel.SingleContentSelectionCard::class.java))
        testView.assertLastState({ it.card(2) }, instanceOf(DiscoveryCardViewModel.MultipleContentSelectionCard::class.java))
    }

    @Test
    fun presenterTracksEnterEvent() {
        addMockedResponse(ApiEndpoints.DISCOVERY_CARDS.path(), DISCOVERY_CARDS)

        val testView = TestView()

        testView.enterScreenTimestamp.onNext(Pair(123L, Screen.DISCOVER))
        mrLocalLocal.startEventTracking()

        testView.initialLoadSignal.onNext(RxSignal.SIGNAL)

        mrLocalLocal.verify(HOME_PAGEVIEW_SPECS)
    }

    @Test
    fun selectionItemClickTracksAndReturnsResult() {
        addMockedResponse(ApiEndpoints.DISCOVERY_CARDS.path(), DISCOVERY_CARDS)

        val testView = TestView()

        testView.enterScreenTimestamp.onNext(Pair(123L, Screen.DISCOVER))

        mrLocalLocal.startEventTracking()

        testView.initialLoadSignal.onNext(RxSignal.SIGNAL)

        testView.assertLastState({ it.hasData() }, equalTo(true))
        testView.assertLastState({ it.cardsCount() }, equalTo(3))

        val lastState = testView.lastState<AsyncLoaderState<List<DiscoveryCardViewModel>, DiscoveryViewError>>()

        testView.selectionItemClick.onNext((lastState.data.get()[1] as DiscoveryCardViewModel.SingleContentSelectionCard).selectionItem)

        mrLocalLocal.verify(HOME_PAGEVIEW_AND_CARD_CLICK_SPECS)
    }

    private fun AsyncLoaderState<*, *>.hasData(): Boolean = data.isPresent
    private fun AsyncLoaderState<*, *>.hasViewError(): Boolean = action.isPresent
    private fun AsyncLoaderState<List<DiscoveryCardViewModel>, *>.cardsCount(): Int = if (data.isPresent) data.get().size else 0
    private fun AsyncLoaderState<List<DiscoveryCardViewModel>, *>.card(position: Int): DiscoveryCardViewModel? =
            if (data.isPresent && data.get().size > position) data.get()[position] else null

    private fun AsyncLoaderState<List<DiscoveryCardViewModel>, *>.exception(position: Int): ApiRequestException? {
        val card = if (data.isPresent && data.get().size > position) data.get()[position] else null
        if (card is DiscoveryCardViewModel.EmptyCard) {
            return card.throwable.transform { it as ApiRequestException }.orNull()
        }
        return null
    }

    private fun loadingState(): AsyncLoaderState<List<DiscoveryCardViewModel>, DiscoveryViewError> =
            AsyncLoaderState.loadingNextPage()

    internal class TestView
    internal constructor(val models: MutableList<AsyncLoaderState<List<DiscoveryCardViewModel>, DiscoveryViewError>> = mutableListOf(),
                         override val selectionItemClick: PublishSubject<SelectionItemViewModel> = PublishSubject.create<SelectionItemViewModel>(),
                         override val searchClick: PublishSubject<RxSignal> = PublishSubject.create<RxSignal>(),
                         val initialLoadSignal: PublishSubject<RxSignal> = PublishSubject.create<RxSignal>(),
                         override val enterScreenTimestamp: PublishSubject<Pair<Long, Screen>> = PublishSubject.create(),
                         homePresenter: HomePresenter = SoundCloudApplication.getObjectGraph().homePresenter()) : TestAsyncState<AsyncLoaderState<List<DiscoveryCardViewModel>, DiscoveryViewError>>(), HomeView {
        init {
            homePresenter.attachView(this)
        }

        override fun states() = Supplier { models }

        override fun accept(viewModel: AsyncLoaderState<List<DiscoveryCardViewModel>, DiscoveryViewError>) {
            models.add(viewModel)
        }

        override fun requestContent() = initialLoadSignal
    }
}
