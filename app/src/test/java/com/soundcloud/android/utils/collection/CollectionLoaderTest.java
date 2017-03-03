package com.soundcloud.android.utils.collection;

import static com.soundcloud.java.collections.Iterables.concat;
import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.of;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.model.CollectionLoadingState;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.functions.Func1;
import rx.observers.AssertableSubscriber;
import rx.subjects.PublishSubject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unchecked")
public class CollectionLoaderTest extends AndroidUnitTest {

    private final ApiRequestException networkError = ApiRequestException.networkError(null, null);

    private final String firstPageParams = "first-page-params";
    private final List<Integer> firstPageData = Arrays.asList(1, 2, 3);
    private final String secondPageParams = "second-page-params";
    private final List<Integer> secondPageData = Arrays.asList(4, 5, 6);
    private final String thirdPageParams = "third-page-params";
    private final List<Integer> thirdPageData = Arrays.asList(5, 6, 7);
    private final PublishSubject<List<Integer>> refreshSubject = PublishSubject.create();

    @Mock Func1<String, Observable<List<Integer>>> paramsToPage;
    @Mock Func1<List<Integer>, Optional<String>> pageToParams;
    @Mock Func1<String, Observable<List<Integer>>> firstPageFunc;
    @Mock Func1<String, Observable<List<Integer>>> refreshFunc;

    private final PublishSubject<String> loadFirstPage = PublishSubject.create();
    private final PublishSubject<Void> loadNextPage = PublishSubject.create();
    private final PublishSubject<String> refreshRequested = PublishSubject.create();

    private CollectionLoader<List<Integer>, String> collectionLoader;

    @Before
    public void setUp() throws Exception {

        when(pageToParams.call(firstPageData)).thenReturn(Optional.of(secondPageParams));
        when(pageToParams.call(secondPageData)).thenReturn(Optional.of(thirdPageParams));
        when(pageToParams.call(thirdPageData)).thenReturn(absent());

        when(paramsToPage.call(secondPageParams)).thenReturn(Observable.just(secondPageData));
        when(paramsToPage.call(thirdPageParams)).thenReturn(Observable.just(thirdPageData));

        when(firstPageFunc.call(firstPageParams)).thenReturn(Observable.just(firstPageData));
        when(refreshFunc.call(firstPageParams)).thenReturn(refreshSubject);

        collectionLoader = new CollectionLoader<>(
                loadFirstPage,
                firstPageFunc,
                refreshRequested,
                refreshFunc,
                loadNextPage,
                paramsToPage,
                pageToParams,
                (integers, integers2) -> {
                    final List<Integer> items = new ArrayList<>(integers.size() + integers2.size());
                    items.addAll(integers);
                    items.addAll(integers2);

                    return items;
                }
        );
    }

    @Test
    public void firstPageLoads() throws Exception {
        AssertableSubscriber<CollectionLoaderState<List<Integer>>> subscriber = collectionLoader.pages().test();

        loadFirstPage.onNext(firstPageParams);

        subscriber.assertValues(
                CollectionLoaderState.loadingNextPage(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .build()
        );
    }

    @Test
    public void firstPageErrors() throws Exception {
        when(firstPageFunc.call(firstPageParams)).thenReturn(Observable.error(networkError));

        AssertableSubscriber<CollectionLoaderState<List<Integer>>> subscriber = collectionLoader.pages().test();

        loadFirstPage.onNext(firstPageParams);

        CollectionLoaderState<List<Integer>> loadingNextPage = CollectionLoaderState.loadingNextPage();
        subscriber.assertValues(
                loadingNextPage,
                loadingNextPage.toNextPageError(networkError)
        );
    }

    @Test
    public void firstPageRecovers() throws Exception {
        when(firstPageFunc.call(firstPageParams)).thenReturn(Observable.error(networkError), Observable.just(firstPageData));

        AssertableSubscriber<CollectionLoaderState<List<Integer>>> subscriber = collectionLoader.pages().test();

        loadFirstPage.onNext(firstPageParams);

        loadFirstPage.onNext(firstPageParams);

        subscriber.assertValues(
                CollectionLoaderState.loadingNextPage(),
                CollectionLoaderState.<List<Integer>>builder().collectionLoadingState(CollectionLoadingState.builder().nextPageError(of(ViewError.from(networkError))).build()).build(),
                CollectionLoaderState.loadingNextPage(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData).build()
        );
    }

    @Test
    public void secondPageLoads() throws Exception {
        AssertableSubscriber<CollectionLoaderState<List<Integer>>> subscriber = collectionLoader.pages().test();

        loadFirstPage.onNext(firstPageParams);

        loadNextPage.onNext(null);

        subscriber.assertValues(
                CollectionLoaderState.loadingNextPage(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .collectionLoadingState(CollectionLoadingState.builder().isLoadingNextPage(true).build())
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(Lists.newArrayList(concat(firstPageData, secondPageData)))
                                                              .build()
        );
    }

    @Test
    public void secondPageErrors() throws Exception {
        when(paramsToPage.call(secondPageParams)).thenReturn(Observable.error(networkError));

        AssertableSubscriber<CollectionLoaderState<List<Integer>>> subscriber = collectionLoader.pages().test();

        loadFirstPage.onNext(firstPageParams);

        loadNextPage.onNext(null);

        subscriber.assertValues(
                CollectionLoaderState.loadingNextPage(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .collectionLoadingState(CollectionLoadingState.builder().isLoadingNextPage(true).build())
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .collectionLoadingState(CollectionLoadingState.builder().isLoadingNextPage(false).nextPageError(of(ViewError.from(networkError))).build())
                                                              .build()
        );
    }

    @Test
    public void secondPageErrorsAndRecovers() throws Exception {
        when(paramsToPage.call(secondPageParams)).thenReturn(Observable.error(networkError), Observable.just(secondPageData));

        AssertableSubscriber<CollectionLoaderState<List<Integer>>> subscriber = collectionLoader.pages().test();

        loadFirstPage.onNext(firstPageParams);

        loadNextPage.onNext(null);

        loadNextPage.onNext(null);

        subscriber.assertValues(
                CollectionLoaderState.loadingNextPage(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .collectionLoadingState(CollectionLoadingState.builder().isLoadingNextPage(true).build())
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .collectionLoadingState(CollectionLoadingState.builder().isLoadingNextPage(false).nextPageError(of(ViewError.from(networkError))).build())

                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .collectionLoadingState(CollectionLoadingState.builder().isLoadingNextPage(true).build())
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(Lists.newArrayList(concat(firstPageData, secondPageData)))
                                                              .build()
        );
    }

    @Test
    public void thirdPageLoadsWithNoMorePages() throws Exception {
        AssertableSubscriber<CollectionLoaderState<List<Integer>>> testSubscriber = collectionLoader.pages().test();

        loadFirstPage.onNext(firstPageParams);

        loadNextPage.onNext(null);

        loadNextPage.onNext(null);

        testSubscriber.assertValues(
                CollectionLoaderState.loadingNextPage(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .collectionLoadingState(CollectionLoadingState.builder().isLoadingNextPage(true).build())
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(Lists.newArrayList(concat(firstPageData, secondPageData)))
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(Lists.newArrayList(concat(firstPageData, secondPageData)))
                                                              .collectionLoadingState(CollectionLoadingState.builder().isLoadingNextPage(true).build())
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(Lists.newArrayList(concat(firstPageData, secondPageData, thirdPageData)))
                                                              .collectionLoadingState(CollectionLoadingState.builder().hasMorePages(false).build())
                                                              .build()
        );
    }

    @Test
    public void refreshErrorDoesNotPropogate() throws Exception {
        // load second page data after refresh
        when(firstPageFunc.call(firstPageParams)).thenReturn(Observable.just(firstPageData));
        when(paramsToPage.call(secondPageParams)).thenReturn(Observable.just(secondPageData));

        AssertableSubscriber<CollectionLoaderState<List<Integer>>> subscriber = collectionLoader.pages().test();

        loadFirstPage.onNext(firstPageParams);

        refreshRequested.onNext(firstPageParams);

        refreshSubject.onError(networkError);

        loadNextPage.onNext(null);

        subscriber.assertValues(
                CollectionLoaderState.loadingNextPage(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .collectionLoadingState(CollectionLoadingState.builder().isRefreshing(true).build())
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .collectionLoadingState(CollectionLoadingState.builder().refreshError(of(ViewError.from(networkError))).build())
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .collectionLoadingState(CollectionLoadingState.builder().isLoadingNextPage(true).refreshError(of(ViewError.from(networkError))).build())
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(Lists.newArrayList(concat(firstPageData, secondPageData)))
                                                              .collectionLoadingState(CollectionLoadingState.builder().refreshError(of(ViewError.from(networkError))).build())
                                                              .build()
        );
    }

    @Test
    public void refreshWipesOutOldState() throws Exception {
        // load second page data after refresh
        when(firstPageFunc.call(firstPageParams)).thenReturn(Observable.just(firstPageData), Observable.just(secondPageData));
        when(paramsToPage.call(secondPageParams)).thenReturn(Observable.error(networkError));

        AssertableSubscriber<CollectionLoaderState<List<Integer>>> subscriber = collectionLoader.pages().test();

        loadFirstPage.onNext(firstPageParams);

        refreshRequested.onNext(firstPageParams);

        loadNextPage.onNext(null);

        refreshSubject.onNext(secondPageData);

        subscriber.assertValues(
                CollectionLoaderState.loadingNextPage(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .collectionLoadingState(CollectionLoadingState.builder().isRefreshing(true).build())
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .collectionLoadingState(CollectionLoadingState.builder().isLoadingNextPage(true).isRefreshing(true).build())
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .collectionLoadingState(CollectionLoadingState.builder().isRefreshing(true).nextPageError(of(ViewError.from(networkError))).build())
                                                              .build(),
                CollectionLoaderState.loadingNextPage(),
                CollectionLoaderState.<List<Integer>>builder().data(secondPageData)
                                                              .collectionLoadingState(CollectionLoadingState.builder().isRefreshing(false).build())
                                                              .build()

        );
    }

    @Test
    public void refreshErrorKeepsOldState() throws Exception {
        when(firstPageFunc.call(firstPageParams)).thenReturn(Observable.just(firstPageData));
        when(paramsToPage.call(secondPageParams)).thenReturn(Observable.error(networkError));

        AssertableSubscriber<CollectionLoaderState<List<Integer>>> subscriber = collectionLoader.pages().test();

        loadFirstPage.onNext(firstPageParams);
        refreshRequested.onNext(firstPageParams);
        loadNextPage.onNext(null);
        refreshSubject.onError(networkError);

        subscriber.assertValues(
                CollectionLoaderState.loadingNextPage(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .collectionLoadingState(CollectionLoadingState.builder().isRefreshing(true).build())
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .collectionLoadingState(CollectionLoadingState.builder().isRefreshing(true).isLoadingNextPage(true).build())
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .collectionLoadingState(CollectionLoadingState.builder().isRefreshing(true).nextPageError(of(ViewError.from(networkError))).build())
                                                              .build(),
                CollectionLoaderState.<List<Integer>>builder().data(firstPageData)
                                                              .collectionLoadingState(CollectionLoadingState.builder().nextPageError(of(ViewError.from(networkError)))
                                                                                                          .isRefreshing(false)
                                                                                                          .refreshError(of(ViewError.from(networkError))).build())
                                                              .build()
        );


    }
}
