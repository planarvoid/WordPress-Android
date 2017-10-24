package com.soundcloud.android.utils.collection

import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.api.ApiRequestException
import com.soundcloud.android.model.AsyncLoadingState
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.android.utils.collection.AsyncLoader.PageResult
import com.soundcloud.android.view.ViewError
import com.soundcloud.java.optional.Optional
import com.soundcloud.java.optional.Optional.of
import io.reactivex.Observable
import io.reactivex.functions.Function
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import java.util.ArrayList
import java.util.Arrays
import javax.inject.Provider

@Suppress("IllegalIdentifier")
class AsyncLoaderTest : AndroidUnitTest() {

    open internal class TestProvider(val v: Observable<PageResult<List<Int>>>) : Provider<Observable<PageResult<List<Int>>>> {
        override fun get(): Observable<PageResult<List<Int>>> = v
    }

    private val networkError = ApiRequestException.networkError(null, null)

    private val firstPageParams = "first-page-params"
    private val firstPageData = Arrays.asList(1, 2, 3)
    private val firstPageUpdatedData = Arrays.asList(9, 2, 3)
    private val secondPageData = Arrays.asList(4, 5, 6)
    private val thirdPageData = Arrays.asList(5, 6, 7)

    private val firstPageSubject = PublishSubject.create<PageResult<List<Int>>>()
    private val secondPageSubject = PublishSubject.create<PageResult<List<Int>>>()
    private val thirdPageSubject = PublishSubject.create<PageResult<List<Int>>>()

    @Mock internal lateinit var firstPageFunc: Function<String, Observable<PageResult<List<Int>>>>
    @Mock internal lateinit var refreshFunc: Function<String, Observable<PageResult<List<Int>>>>
    @Mock internal lateinit var nextPageprovider: TestProvider

    private val loadFirstPage = PublishSubject.create<String>()
    private val loadNextPage = PublishSubject.create<Any>()
    private val refreshRequested = PublishSubject.create<String>()

    private lateinit var asyncLoader: AsyncLoader<List<Int>, String>
    private val refreshSubject = PublishSubject.create<PageResult<List<Int>>>()

    private val firstResult = PageResult<List<Int>>(data = firstPageData, nextPage = of(TestProvider(secondPageSubject)))
    private val firstUpdatedResult = PageResult<List<Int>>(firstPageUpdatedData, nextPage = of(TestProvider(secondPageSubject)))
    private val secondResult = PageResult<List<Int>>(secondPageData, nextPage = of(TestProvider(thirdPageSubject)))
    private val thirdResult = PageResult<List<Int>>(thirdPageData)

    @Before
    fun setUp() {
        whenever(firstPageFunc.apply(firstPageParams)).thenReturn(firstPageSubject)
        whenever(refreshFunc.apply(anyString())).thenReturn(refreshSubject)

        asyncLoader = AsyncLoader(
                loadFirstPage,
                { firstPageFunc.apply(it) },
                refreshRequested,
                { refreshFunc.apply(it) },
                loadNextPage,
                Optional.of({ integers1, integers2 ->
                                val items = ArrayList<Int>(integers1.size + integers2.size)
                                items.addAll(integers1)
                                items.addAll(integers2)
                                items
                            })
        )
    }

    @Test
    fun `first page loads correctly`() {
        val subscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        firstPageSubject.onNext(firstResult)

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage<List<Int>>(),
                firstPageLoaded()
        )
    }

    @Test
    fun `unsubscribes From Source After Loading Page`() {
        val subscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage<List<Int>>()
        )

        subscriber.dispose()
        assertThat(loadFirstPage.hasObservers()).isFalse()
    }

    @Test
    fun `first Page Errors`() {
        val subscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        firstPageSubject.onError(networkError)

        val loadingNextPage = AsyncLoaderState.loadingNextPage<List<Int>>()
        subscriber.assertValues(
                loadingNextPage,
                loadingNextPage.toNextPageError(networkError)
        )
    }

    @Test
    fun `first Page Recovers`() {
        whenever<Observable<PageResult<List<Int>>>>(firstPageFunc.apply(firstPageParams)).thenReturn(Observable.error<PageResult<List<Int>>>(networkError), firstPageSubject)

        val subscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        loadFirstPage.onNext(firstPageParams)

        firstPageSubject.onNext(firstResult)

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage<List<Int>>(),
                AsyncLoaderState(asyncLoadingState = AsyncLoadingState(nextPageError = ViewError.from(networkError))),
                AsyncLoaderState.loadingNextPage<List<Int>>(),
                firstPageLoaded()
        )
    }

    @Test
    fun `second Page Loads`() {
        val subscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        firstPageSubject.onNext(firstResult)

        loadNextPage.onNext(SIGNAL)

        secondPageSubject.onNext(secondResult)

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage<List<Int>>(),
                firstPageLoaded(),
                AsyncLoaderState(data = firstPageData, asyncLoadingState = AsyncLoadingState(isLoadingNextPage = true)),
                AsyncLoaderState(data = firstPageData + secondPageData, asyncLoadingState = AsyncLoadingState(requestMoreOnScroll = true))
        )
    }

    @Test
    fun `unsubscribes From Source After Loading Two Pages`() {
        val subscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        firstPageSubject.onNext(firstResult)

        loadNextPage.onNext(SIGNAL)

        secondPageSubject.onNext(secondResult)

        subscriber.dispose()

        assertThat(loadFirstPage.hasObservers()).isFalse()
        assertThat(firstPageSubject.hasObservers()).isFalse()

        assertThat(loadNextPage.hasObservers()).isFalse()
        assertThat(secondPageSubject.hasObservers()).isFalse()
    }

    @Test
    fun `second Page Errors`() {
        val subscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        firstPageSubject.onNext(firstResult)

        loadNextPage.onNext(SIGNAL)

        secondPageSubject.onError(networkError)

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage(),
                firstPageLoaded(),
                AsyncLoaderState(data = firstPageData, asyncLoadingState = AsyncLoadingState(isLoadingNextPage = true)),
                AsyncLoaderState(data = firstPageData, asyncLoadingState = AsyncLoadingState(isLoadingNextPage = false, nextPageError = ViewError.from(networkError)))
        )
    }

    @Test
    fun `second Page Errors And Recovers`() {
        val secondPageErrorSubject = PublishSubject.create<PageResult<List<Int>>>()

        whenever(firstPageFunc.apply(firstPageParams)).thenReturn(Observable.just<PageResult<List<Int>>>(PageResult<List<Int>>(
                firstPageData,
                nextPage = of(nextPageprovider))))
        whenever(nextPageprovider.get()).thenReturn(secondPageErrorSubject, secondPageSubject)

        val subscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        loadNextPage.onNext(SIGNAL)

        secondPageErrorSubject.onError(networkError)

        loadNextPage.onNext(SIGNAL)

        secondPageSubject.onNext(secondResult)

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage(),
                firstPageLoaded(),
                AsyncLoaderState(data = firstPageData, asyncLoadingState = AsyncLoadingState(isLoadingNextPage = true)),
                AsyncLoaderState(data = firstPageData, asyncLoadingState = AsyncLoadingState(isLoadingNextPage = false, nextPageError = ViewError.from(networkError))),
                AsyncLoaderState(data = firstPageData, asyncLoadingState = AsyncLoadingState(isLoadingNextPage = true)),
                AsyncLoaderState(data = firstPageData + secondPageData, asyncLoadingState = AsyncLoadingState(requestMoreOnScroll = true))
        )
    }

    @Test
    fun `third Page Loads With No More Pages`() {
        val testSubscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        firstPageSubject.onNext(firstResult)

        loadNextPage.onNext(SIGNAL)

        secondPageSubject.onNext(secondResult)

        loadNextPage.onNext(SIGNAL)

        thirdPageSubject.onNext(thirdResult)

        testSubscriber.assertValues(
                AsyncLoaderState.loadingNextPage(),
                firstPageLoaded(),
                AsyncLoaderState(data = firstPageData, asyncLoadingState = AsyncLoadingState(isLoadingNextPage = true)),
                AsyncLoaderState(data = firstPageData + secondPageData, asyncLoadingState = AsyncLoadingState(requestMoreOnScroll = true)),
                AsyncLoaderState(data = firstPageData + secondPageData, asyncLoadingState = AsyncLoadingState(isLoadingNextPage = true)),
                AsyncLoaderState(data = firstPageData + secondPageData + thirdPageData, asyncLoadingState = AsyncLoadingState(requestMoreOnScroll = false))
        )
    }

    @Test
    fun `reEmission Does Not Override Next Page`() {
        val testSubscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        firstPageSubject.onNext(firstResult)

        loadNextPage.onNext(SIGNAL)

        secondPageSubject.onNext(secondResult)

        firstPageSubject.onNext(firstUpdatedResult)

        loadNextPage.onNext(SIGNAL)

        thirdPageSubject.onNext(thirdResult)

        testSubscriber.assertValues(
                AsyncLoaderState.loadingNextPage(),
                firstPageLoaded(),
                AsyncLoaderState(data = firstPageData, asyncLoadingState = AsyncLoadingState(isLoadingNextPage = true)),
                AsyncLoaderState(data = firstPageData + secondPageData, asyncLoadingState = AsyncLoadingState(requestMoreOnScroll = true)),
                AsyncLoaderState(data = firstPageUpdatedData + secondPageData, asyncLoadingState = AsyncLoadingState(requestMoreOnScroll = true)),
                AsyncLoaderState(data = firstPageUpdatedData + secondPageData, asyncLoadingState = AsyncLoadingState(isLoadingNextPage = true)),
                AsyncLoaderState(data = firstPageUpdatedData + secondPageData + thirdPageData, asyncLoadingState = AsyncLoadingState(requestMoreOnScroll = false))
        )
    }

    @Test
    fun `refresh Error Does Not Propagate`() {
        // load second page data after refresh
        val subscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        firstPageSubject.onNext(firstResult)

        refreshRequested.onNext(firstPageParams)

        refreshSubject.onError(networkError)

        loadNextPage.onNext(SIGNAL)

        secondPageSubject.onNext(secondResult)

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage(),
                firstPageLoaded(),
                AsyncLoaderState(data = firstPageData, asyncLoadingState = AsyncLoadingState(isRefreshing = true, requestMoreOnScroll = true)),
                AsyncLoaderState(data = firstPageData, asyncLoadingState = AsyncLoadingState(refreshError = ViewError.from(networkError), requestMoreOnScroll = true)),
                AsyncLoaderState(data = firstPageData, asyncLoadingState = AsyncLoadingState(isLoadingNextPage = true, refreshError = ViewError.from(networkError))),
                AsyncLoaderState(data = firstPageData + secondPageData,
                                 asyncLoadingState = AsyncLoadingState(refreshError = ViewError.from(networkError), requestMoreOnScroll = true))
        )

        subscriber.assertNoErrors()
    }

    @Test
    fun `refresh Wipes Out Old State`() {
        val refreshResult = PageResult<List<Int>>(secondPageData, nextPage = of(TestProvider(thirdPageSubject)))

        val subscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        firstPageSubject.onNext(firstResult)

        refreshRequested.onNext(firstPageParams)

        loadNextPage.onNext(SIGNAL)

        secondPageSubject.onError(networkError)

        refreshSubject.onNext(refreshResult)

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage(),
                firstPageLoaded(),
                AsyncLoaderState(data = firstPageData, asyncLoadingState = AsyncLoadingState(isRefreshing = true, requestMoreOnScroll = true)),
                AsyncLoaderState(data = firstPageData, asyncLoadingState = AsyncLoadingState(isLoadingNextPage = true, isRefreshing = true)),
                AsyncLoaderState(data = firstPageData, asyncLoadingState = AsyncLoadingState(isRefreshing = true, nextPageError = ViewError.from(networkError))),
                AsyncLoaderState(data = firstPageData, asyncLoadingState = AsyncLoadingState(nextPageError = ViewError.from(networkError))),
                AsyncLoaderState(data = secondPageData, asyncLoadingState = AsyncLoadingState(isRefreshing = false, requestMoreOnScroll = true))
        )

        subscriber.assertNoErrors()
    }

    @Test
    fun `unsubscribes From Source After Refreshing`() {
        val refreshResult = PageResult<List<Int>>(secondPageData, nextPage = of(TestProvider(thirdPageSubject)))

        val subscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        firstPageSubject.onNext(firstResult)

        refreshRequested.onNext(firstPageParams)

        loadNextPage.onNext(SIGNAL)

        secondPageSubject.onError(networkError)

        refreshSubject.onNext(refreshResult)

        subscriber.dispose()

        assertThat(loadFirstPage.hasObservers()).isFalse()
        assertThat(firstPageSubject.hasObservers()).isFalse()

        assertThat(loadNextPage.hasObservers()).isFalse()
        assertThat(secondPageSubject.hasObservers()).isFalse()

        assertThat(refreshRequested.hasObservers()).isFalse()
        assertThat(refreshSubject.hasObservers()).isFalse()
    }

    @Test
    fun `refresh Error Keeps Old State`() {
        val subscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        firstPageSubject.onNext(firstResult)

        refreshRequested.onNext(firstPageParams)

        loadNextPage.onNext(SIGNAL)

        secondPageSubject.onError(networkError)

        refreshSubject.onError(networkError)

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage(),
                firstPageLoaded(),
                AsyncLoaderState(data = firstPageData, asyncLoadingState = AsyncLoadingState(isRefreshing = true, requestMoreOnScroll = true)),
                AsyncLoaderState(data = firstPageData, asyncLoadingState = AsyncLoadingState(isRefreshing = true, isLoadingNextPage = true)),
                AsyncLoaderState(data = firstPageData, asyncLoadingState = AsyncLoadingState(isRefreshing = true, nextPageError = ViewError.from(networkError))),
                AsyncLoaderState(data = firstPageData,
                                 asyncLoadingState = AsyncLoadingState(nextPageError = ViewError.from(networkError), isRefreshing = false, refreshError = ViewError.from(networkError)))
        )
    }

    private fun firstPageLoaded(): AsyncLoaderState<List<Int>> =
            AsyncLoaderState(data = firstPageData, asyncLoadingState = AsyncLoadingState(requestMoreOnScroll = true))

    companion object {

        private const val SIGNAL = 0
    }
}
