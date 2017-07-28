package com.soundcloud.android.utils.collection;

import static com.soundcloud.java.collections.Iterables.concat;
import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.model.AsyncLoadingState;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.PublishSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unchecked")
public class AsyncLoaderTest extends AndroidUnitTest {

    private static final int SIGNAL = 0;

    static class TestProvider implements Provider<Observable<AsyncLoader.PageResult<List<Integer>>>> {
        @Override
        public Observable<AsyncLoader.PageResult<List<Integer>>> get() {
            return null;
        }
    }

    private final ApiRequestException networkError = ApiRequestException.networkError(null, null);

    private final String firstPageParams = "first-page-params";
    private final List<Integer> firstPageData = Arrays.asList(1, 2, 3);
    private final List<Integer> firstPageUpdatedData = Arrays.asList(9, 2, 3);
    private final List<Integer> secondPageData = Arrays.asList(4, 5, 6);
    private final List<Integer> thirdPageData = Arrays.asList(5, 6, 7);

    private final PublishSubject<AsyncLoader.PageResult<List<Integer>>> firstPageSubject = PublishSubject.create();
    private final PublishSubject<AsyncLoader.PageResult<List<Integer>>> secondPageSubject = PublishSubject.create();
    private final PublishSubject<AsyncLoader.PageResult<List<Integer>>> thirdPageSubject = PublishSubject.create();

    @Mock Function<String, Observable<AsyncLoader.PageResult<List<Integer>>>> firstPageFunc;
    @Mock Function<String, Observable<AsyncLoader.PageResult<List<Integer>>>> refreshFunc;
    @Mock TestProvider nextPageprovider;

    private final PublishSubject<String> loadFirstPage = PublishSubject.create();
    private final PublishSubject<Object> loadNextPage = PublishSubject.create();
    private final PublishSubject<String> refreshRequested = PublishSubject.create();

    private AsyncLoader<List<Integer>, String> asyncLoader;
    private PublishSubject<AsyncLoader.PageResult<List<Integer>>> refreshSubject = PublishSubject.create();

    private AsyncLoader.PageResult<List<Integer>> firstResult = new AsyncLoader.PageResult<>(firstPageData, of(() -> secondPageSubject));
    private AsyncLoader.PageResult<List<Integer>> firstUpdatedResult = new AsyncLoader.PageResult<>(firstPageUpdatedData, of(() -> secondPageSubject));
    private AsyncLoader.PageResult<List<Integer>> secondResult = new AsyncLoader.PageResult<>(secondPageData, of(() -> thirdPageSubject));
    private AsyncLoader.PageResult<List<Integer>> thirdResult = new AsyncLoader.PageResult<>(thirdPageData, absent());

    @Before
    public void setUp() throws Exception {
        when(firstPageFunc.apply(firstPageParams)).thenReturn(firstPageSubject);
        when(refreshFunc.apply(anyString())).thenReturn(refreshSubject);

        asyncLoader = new AsyncLoader<>(
                loadFirstPage,
                firstPageFunc,
                refreshRequested,
                refreshFunc,
                loadNextPage,
                Optional.of((integers, integers2) -> {
                    final List<Integer> items = new ArrayList<>(integers.size() + integers2.size());
                    items.addAll(integers);
                    items.addAll(integers2);
                    return items;
                })
        );
    }

    @Test
    public void firstPageLoads() throws Exception {
        TestObserver<AsyncLoaderState<List<Integer>>> subscriber = asyncLoader.test();

        loadFirstPage.onNext(firstPageParams);

        firstPageSubject.onNext(firstResult);

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage(),
                firstPageLoaded()
        );
    }

    @Test
    public void unsubscribesFromSourceAfterLoadingPage() throws Exception {
        TestObserver<AsyncLoaderState<List<Integer>>> subscriber = asyncLoader.test();

        loadFirstPage.onNext(firstPageParams);

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage()
        );

        subscriber.dispose();
        assertThat(loadFirstPage.hasObservers()).isFalse();
    }

    @Test
    public void firstPageErrors() throws Exception {
        TestObserver<AsyncLoaderState<List<Integer>>> subscriber = asyncLoader.test();

        loadFirstPage.onNext(firstPageParams);

        firstPageSubject.onError(networkError);

        AsyncLoaderState<List<Integer>> loadingNextPage = AsyncLoaderState.loadingNextPage();
        subscriber.assertValues(
                loadingNextPage,
                loadingNextPage.toNextPageError(networkError)
        );
    }

    @Test
    public void firstPageRecovers() throws Exception {
        when(firstPageFunc.apply(firstPageParams)).thenReturn(Observable.error(networkError), firstPageSubject);

        TestObserver<AsyncLoaderState<List<Integer>>> subscriber = asyncLoader.test();

        loadFirstPage.onNext(firstPageParams);

        loadFirstPage.onNext(firstPageParams);

        firstPageSubject.onNext(firstResult);

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage(),
                AsyncLoaderState.<List<Integer>>builder().asyncLoadingState(AsyncLoadingState.builder().nextPageError(of(ViewError.from(networkError))).build()).build(),
                AsyncLoaderState.loadingNextPage(),
                firstPageLoaded()
        );
    }

    @Test
    public void secondPageLoads() throws Exception {
        TestObserver<AsyncLoaderState<List<Integer>>> subscriber = asyncLoader.test();

        loadFirstPage.onNext(firstPageParams);

        firstPageSubject.onNext(firstResult);

        loadNextPage.onNext(SIGNAL);

        secondPageSubject.onNext(secondResult);

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage(),
                firstPageLoaded(),
                AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(Lists.newArrayList(concat(firstPageData, secondPageData)))
                                                         .asyncLoadingState(AsyncLoadingState.builder().requestMoreOnScroll(true).build())
                                                         .build()
        );
    }

    @Test
    public void unsubscribesFromSourceAfterLoadingTwoPages() throws Exception {
        TestObserver<AsyncLoaderState<List<Integer>>> subscriber = asyncLoader.test();

        loadFirstPage.onNext(firstPageParams);

        firstPageSubject.onNext(firstResult);

        loadNextPage.onNext(SIGNAL);

        secondPageSubject.onNext(secondResult);

        subscriber.dispose();

        assertThat(loadFirstPage.hasObservers()).isFalse();
        assertThat(firstPageSubject.hasObservers()).isFalse();

        assertThat(loadNextPage.hasObservers()).isFalse();
        assertThat(secondPageSubject.hasObservers()).isFalse();
    }

    @Test
    public void secondPageErrors() throws Exception {
        TestObserver<AsyncLoaderState<List<Integer>>> subscriber = asyncLoader.test();

        loadFirstPage.onNext(firstPageParams);

        firstPageSubject.onNext(firstResult);

        loadNextPage.onNext(SIGNAL);

        secondPageSubject.onError(networkError);

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage(),
                firstPageLoaded(),
                AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(false).nextPageError(of(ViewError.from(networkError))).build())
                                                         .build()
        );
    }

    @Test
    public void secondPageErrorsAndRecovers() throws Exception {
        PublishSubject<AsyncLoader.PageResult<List<Integer>>> secondPageErrorSubject = PublishSubject.create();

        when(firstPageFunc.apply(firstPageParams)).thenReturn(Observable.just(new AsyncLoader.PageResult<>(firstPageData, of(nextPageprovider))));
        when(nextPageprovider.get()).thenReturn(secondPageErrorSubject, secondPageSubject);

        TestObserver<AsyncLoaderState<List<Integer>>> subscriber = asyncLoader.test();

        loadFirstPage.onNext(firstPageParams);

        loadNextPage.onNext(SIGNAL);



        secondPageErrorSubject.onError(networkError);

        loadNextPage.onNext(SIGNAL);

        secondPageSubject.onNext(secondResult);

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage(),
                firstPageLoaded(),
                AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(false).nextPageError(of(ViewError.from(networkError))).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(Lists.newArrayList(concat(firstPageData, secondPageData)))
                                                         .asyncLoadingState(AsyncLoadingState.builder().requestMoreOnScroll(true).build())
                                                         .build()
        );
    }

    @Test
    public void thirdPageLoadsWithNoMorePages() throws Exception {
        TestObserver<AsyncLoaderState<List<Integer>>> testSubscriber = asyncLoader.test();

        loadFirstPage.onNext(firstPageParams);

        firstPageSubject.onNext(firstResult);

        loadNextPage.onNext(SIGNAL);

        secondPageSubject.onNext(secondResult);

        loadNextPage.onNext(SIGNAL);

        thirdPageSubject.onNext(thirdResult);

        testSubscriber.assertValues(
                AsyncLoaderState.loadingNextPage(),
                firstPageLoaded(),
                AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(Lists.newArrayList(concat(firstPageData, secondPageData)))
                                                         .asyncLoadingState(AsyncLoadingState.builder().requestMoreOnScroll(true).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(Lists.newArrayList(concat(firstPageData, secondPageData)))
                                                         .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(Lists.newArrayList(concat(firstPageData, secondPageData, thirdPageData)))
                                                         .asyncLoadingState(AsyncLoadingState.builder().requestMoreOnScroll(false).build())
                                                         .build()
        );
    }

    @Test
    public void reEmissionDoesNotOverrideNextPage() throws Exception {
        TestObserver<AsyncLoaderState<List<Integer>>> testSubscriber = asyncLoader.test();

        loadFirstPage.onNext(firstPageParams);

        firstPageSubject.onNext(firstResult);

        loadNextPage.onNext(SIGNAL);

        secondPageSubject.onNext(secondResult);

        firstPageSubject.onNext(firstUpdatedResult);

        loadNextPage.onNext(SIGNAL);

        thirdPageSubject.onNext(thirdResult);

        testSubscriber.assertValues(
                AsyncLoaderState.loadingNextPage(),
                firstPageLoaded(),
                AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(Lists.newArrayList(concat(firstPageData, secondPageData)))
                                                         .asyncLoadingState(AsyncLoadingState.builder().requestMoreOnScroll(true).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(Lists.newArrayList(concat(firstPageUpdatedData, secondPageData)))
                                                         .asyncLoadingState(AsyncLoadingState.builder().requestMoreOnScroll(true).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(Lists.newArrayList(concat(firstPageUpdatedData, secondPageData)))
                                                         .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(Lists.newArrayList(concat(firstPageUpdatedData, secondPageData, thirdPageData)))
                                                         .asyncLoadingState(AsyncLoadingState.builder().requestMoreOnScroll(false).build())
                                                         .build()
        );
    }

    @Test
    public void refreshErrorDoesNotPropogate() throws Exception {
        // load second page data after refresh
        TestObserver<AsyncLoaderState<List<Integer>>> subscriber = asyncLoader.test();

        loadFirstPage.onNext(firstPageParams);

        firstPageSubject.onNext(firstResult);

        refreshRequested.onNext(firstPageParams);

        refreshSubject.onError(networkError);

        loadNextPage.onNext(SIGNAL);

        secondPageSubject.onNext(secondResult);

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage(),
                firstPageLoaded(),
                AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().isRefreshing(true).requestMoreOnScroll(true).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().refreshError(of(ViewError.from(networkError))).requestMoreOnScroll(true).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).refreshError(of(ViewError.from(networkError))).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(Lists.newArrayList(concat(firstPageData, secondPageData)))
                                                         .asyncLoadingState(AsyncLoadingState.builder().refreshError(of(ViewError.from(networkError))).requestMoreOnScroll(true).build())
                                                         .build()
        );
    }

    @Test
    public void refreshWipesOutOldState() throws Exception {
        AsyncLoader.PageResult<List<Integer>> refreshResult = new AsyncLoader.PageResult<>(secondPageData, of(() -> thirdPageSubject));

        TestObserver<AsyncLoaderState<List<Integer>>> subscriber = asyncLoader.test();

        loadFirstPage.onNext(firstPageParams);

        firstPageSubject.onNext(firstResult);

        refreshRequested.onNext(firstPageParams);

        loadNextPage.onNext(SIGNAL);

        secondPageSubject.onError(networkError);

        refreshSubject.onNext(refreshResult);

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage(),
                firstPageLoaded(),
                AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().isRefreshing(true).requestMoreOnScroll(true).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().isLoadingNextPage(true).isRefreshing(true).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().isRefreshing(true).nextPageError(of(ViewError.from(networkError))).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().nextPageError(of(ViewError.from(networkError))).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(secondPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().isRefreshing(false).requestMoreOnScroll(true).build())
                                                         .build()

        );
    }

    @Test
    public void unsubscribesFromSourceAfterRefreshing() throws Exception {
        AsyncLoader.PageResult<List<Integer>> refreshResult = new AsyncLoader.PageResult<>(secondPageData, of(() -> thirdPageSubject));

        TestObserver<AsyncLoaderState<List<Integer>>> subscriber = asyncLoader.test();

        loadFirstPage.onNext(firstPageParams);

        firstPageSubject.onNext(firstResult);

        refreshRequested.onNext(firstPageParams);

        loadNextPage.onNext(SIGNAL);

        secondPageSubject.onError(networkError);

        refreshSubject.onNext(refreshResult);

        subscriber.dispose();

        assertThat(loadFirstPage.hasObservers()).isFalse();
        assertThat(firstPageSubject.hasObservers()).isFalse();

        assertThat(loadNextPage.hasObservers()).isFalse();
        assertThat(secondPageSubject.hasObservers()).isFalse();

        assertThat(refreshRequested.hasObservers()).isFalse();
        assertThat(refreshSubject.hasObservers()).isFalse();
    }

    @Test
    public void refreshErrorKeepsOldState() throws Exception {
        TestObserver<AsyncLoaderState<List<Integer>>> subscriber = asyncLoader.test();

        loadFirstPage.onNext(firstPageParams);

        firstPageSubject.onNext(firstResult);

        refreshRequested.onNext(firstPageParams);

        loadNextPage.onNext(SIGNAL);

        secondPageSubject.onError(networkError);

        refreshSubject.onError(networkError);

        subscriber.assertValues(
                AsyncLoaderState.loadingNextPage(),
                firstPageLoaded(),
                AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().isRefreshing(true).requestMoreOnScroll(true).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().isRefreshing(true).isLoadingNextPage(true).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().isRefreshing(true).nextPageError(of(ViewError.from(networkError))).build())
                                                         .build(),
                AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                         .asyncLoadingState(AsyncLoadingState.builder().nextPageError(of(ViewError.from(networkError)))
                                                                                                  .isRefreshing(false)
                                                                                                  .refreshError(of(ViewError.from(networkError))).build())
                                                         .build()
        );
    }

    private AsyncLoaderState<List<Integer>> firstPageLoaded() {
        return AsyncLoaderState.<List<Integer>>builder().data(firstPageData)
                                                        .asyncLoadingState(AsyncLoadingState.builder().requestMoreOnScroll(true).build())
                                                        .build();
    }
}
