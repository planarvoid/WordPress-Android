package com.soundcloud.android.utils.collection

import com.google.common.collect.Lists
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.api.ApiRequestException
import com.soundcloud.android.model.AsyncLoadingState
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.android.utils.collection.AsyncLoader.PageResult
import com.soundcloud.android.view.ViewError
import com.soundcloud.java.collections.Iterables.concat
import com.soundcloud.java.optional.Optional
import com.soundcloud.java.optional.Optional.absent
import com.soundcloud.java.optional.Optional.of
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import java.util.*
import javax.inject.Provider

@Suppress("IllegalIdentifier")
class AsyncLoaderTest : AndroidUnitTest() {

    open internal class TestProvider(val v: Observable<PageResult<List<Int>>>) : Provider<Observable<PageResult<List<Int>>>> {
        override fun get(): Observable<PageResult<List<Int>>> {
            return v
        }
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

    private val firstResult = PageResult<List<Int>>(firstPageData, of(TestProvider(secondPageSubject)))
    private val firstUpdatedResult = PageResult<List<Int>>(firstPageUpdatedData, of(TestProvider(secondPageSubject)))
    private val secondResult = PageResult<List<Int>>(secondPageData, of(TestProvider(thirdPageSubject)))
    private val thirdResult = PageResult(thirdPageData, absent())

    @Before
    @Throws(Exception::class)
    fun setUp() {
        whenever(firstPageFunc.apply(firstPageParams)).thenReturn(firstPageSubject)
        whenever(refreshFunc.apply(anyString())).thenReturn(refreshSubject)

        asyncLoader = AsyncLoader(
                loadFirstPage,
                firstPageFunc,
                refreshRequested,
                refreshFunc,
                loadNextPage,
                Optional.of(BiFunction { integers1, integers2 ->
                    val items = ArrayList<Int>(integers1.size + integers2.size)
                    items.addAll(integers1)
                    items.addAll(integers2)
                    items
                })
        )
    }

    @Test
    @Throws(Exception::class)
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
    @Throws(Exception::class)
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
    @Throws(Exception::class)
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
    @Throws(Exception::class)
    fun `first Page Recovers`() {
        whenever<Observable<PageResult<List<Int>>>>(firstPageFunc.apply(firstPageParams)).thenReturn(Observable.error<PageResult<List<Int>>>(networkError), firstPageSubject)

        val subscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        loadFirstPage.onNext(firstPageParams)

        firstPageSubject.onNext(firstResult)

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage<List<Int>>(),
                AsyncLoaderState.builder<List<Int>>().asyncLoadingState(AsyncLoadingState.builder().nextPageError(of(ViewError.from(networkError))).build()).build(),
                AsyncLoaderState.loadingNextPage<List<Int>>(),
                firstPageLoaded()
        )
    }

    @Test
    @Throws(Exception::class)
    fun `second Page Loads`() {
        val subscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        firstPageSubject.onNext(firstResult)

        loadNextPage.onNext(SIGNAL)

        secondPageSubject.onNext(secondResult)

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage<List<Int>>(),
                firstPageLoaded(),
                AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(Lists.newArrayList(concat(firstPageData, secondPageData)))
                        .asyncLoadingState(AsyncLoadingState.builder().requestMoreOnScroll(true).build())
                        .build()
        )
    }

    @Test
    @Throws(Exception::class)
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
    @Throws(Exception::class)
    fun `second Page Errors`() {
        val subscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        firstPageSubject.onNext(firstResult)

        loadNextPage.onNext(SIGNAL)

        secondPageSubject.onError(networkError)

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage<List<Int>>(),
                firstPageLoaded(),
                AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(false).nextPageError(of(ViewError.from(networkError))).build())
                        .build()
        )
    }

    @Test
    @Throws(Exception::class)
    fun `second Page Errors And Recovers`() {
        val secondPageErrorSubject = PublishSubject.create<PageResult<List<Int>>>()

        whenever<Observable<PageResult<List<Int>>>>(firstPageFunc.apply(firstPageParams)).thenReturn(Observable.just<PageResult<List<Int>>>(PageResult<List<Int>>(firstPageData, of(nextPageprovider))))
        com.nhaarman.mockito_kotlin.whenever(nextPageprovider.get()).thenReturn(secondPageErrorSubject, secondPageSubject)

        val subscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        loadNextPage.onNext(SIGNAL)



        secondPageErrorSubject.onError(networkError)

        loadNextPage.onNext(SIGNAL)

        secondPageSubject.onNext(secondResult)

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage<List<Int>>(),
                firstPageLoaded(),
                AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(false).nextPageError(of(ViewError.from(networkError))).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(Lists.newArrayList(concat(firstPageData, secondPageData)))
                        .asyncLoadingState(AsyncLoadingState.builder().requestMoreOnScroll(true).build())
                        .build()
        )
    }

    @Test
    @Throws(Exception::class)
    fun `third Page Loads With No More Pages`() {
        val testSubscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        firstPageSubject.onNext(firstResult)

        loadNextPage.onNext(SIGNAL)

        secondPageSubject.onNext(secondResult)

        loadNextPage.onNext(SIGNAL)

        thirdPageSubject.onNext(thirdResult)

        testSubscriber.assertValues(
                AsyncLoaderState.loadingNextPage<List<Int>>(),
                firstPageLoaded(),
                AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(Lists.newArrayList(concat(firstPageData, secondPageData)))
                        .asyncLoadingState(AsyncLoadingState.builder().requestMoreOnScroll(true).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(Lists.newArrayList(concat(firstPageData, secondPageData)))
                        .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(Lists.newArrayList(concat(firstPageData, secondPageData, thirdPageData)))
                        .asyncLoadingState(AsyncLoadingState.builder().requestMoreOnScroll(false).build())
                        .build()
        )
    }

    @Test
    @Throws(Exception::class)
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
                AsyncLoaderState.loadingNextPage<List<Int>>(),
                firstPageLoaded(),
                AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(Lists.newArrayList(concat(firstPageData, secondPageData)))
                        .asyncLoadingState(AsyncLoadingState.builder().requestMoreOnScroll(true).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(Lists.newArrayList(concat(firstPageUpdatedData, secondPageData)))
                        .asyncLoadingState(AsyncLoadingState.builder().requestMoreOnScroll(true).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(Lists.newArrayList(concat(firstPageUpdatedData, secondPageData)))
                        .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(Lists.newArrayList(concat(firstPageUpdatedData, secondPageData, thirdPageData)))
                        .asyncLoadingState(AsyncLoadingState.builder().requestMoreOnScroll(false).build())
                        .build()
        )
    }

    @Test
    @Throws(Exception::class)
    fun `refresh Error Does Not Propogate`() {
        // load second page data after refresh
        val subscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        firstPageSubject.onNext(firstResult)

        refreshRequested.onNext(firstPageParams)

        refreshSubject.onError(networkError)

        loadNextPage.onNext(SIGNAL)

        secondPageSubject.onNext(secondResult)

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage<List<Int>>(),
                firstPageLoaded(),
                AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().isRefreshing(true).requestMoreOnScroll(true).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().refreshError(of(ViewError.from(networkError))).requestMoreOnScroll(true).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).refreshError(of(ViewError.from(networkError))).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(Lists.newArrayList(concat(firstPageData, secondPageData)))
                        .asyncLoadingState(AsyncLoadingState.builder().refreshError(of(ViewError.from(networkError))).requestMoreOnScroll(true).build())
                        .build()
        )
    }

    @Test
    @Throws(Exception::class)
    fun `refresh Wipes Out Old State`() {
        val refreshResult = PageResult<List<Int>>(secondPageData, of(TestProvider(thirdPageSubject)))

        val subscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        firstPageSubject.onNext(firstResult)

        refreshRequested.onNext(firstPageParams)

        loadNextPage.onNext(SIGNAL)

        secondPageSubject.onError(networkError)

        refreshSubject.onNext(refreshResult)

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage<List<Int>>(),
                firstPageLoaded(),
                AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().isRefreshing(true).requestMoreOnScroll(true).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).isRefreshing(true).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().isRefreshing(true).nextPageError(of(ViewError.from(networkError))).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().nextPageError(of(ViewError.from(networkError))).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(secondPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().isRefreshing(false).requestMoreOnScroll(true).build())
                        .build()

        )
    }

    @Test
    @Throws(Exception::class)
    fun `unsubscribes From Source After Refreshing`() {
        val refreshResult = PageResult<List<Int>>(secondPageData, of(TestProvider(thirdPageSubject)))

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
    @Throws(Exception::class)
    fun `refresh Error Keeps Old State`() {
        val subscriber = asyncLoader.test()

        loadFirstPage.onNext(firstPageParams)

        firstPageSubject.onNext(firstResult)

        refreshRequested.onNext(firstPageParams)

        loadNextPage.onNext(SIGNAL)

        secondPageSubject.onError(networkError)

        refreshSubject.onError(networkError)

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage<List<Int>>(),
                firstPageLoaded(),
                AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().isRefreshing(true).requestMoreOnScroll(true).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().isRefreshing(true).isLoadingNextPage(true).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().isRefreshing(true).nextPageError(of(ViewError.from(networkError))).build())
                        .build(),
                AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                        .asyncLoadingState(AsyncLoadingState.builder().nextPageError(of(ViewError.from(networkError)))
                                .isRefreshing(false)
                                .refreshError(of(ViewError.from(networkError))).build())
                        .build()
        )
    }

    private fun firstPageLoaded(): AsyncLoaderState<List<Int>> {
        return AsyncLoaderState.builder<List<Int>>().data(firstPageData)
                .asyncLoadingState(AsyncLoadingState.builder().requestMoreOnScroll(true).build())
                .build()
    }

    companion object {

        private const val SIGNAL = 0
    }
}
