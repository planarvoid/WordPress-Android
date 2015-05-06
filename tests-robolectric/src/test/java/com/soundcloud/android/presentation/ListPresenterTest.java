package com.soundcloud.android.presentation;

import static android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import static com.soundcloud.android.Expect.expect;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestPager;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class ListPresenterTest {

    private ListPresenter<String> listPresenter;
    private PublishSubject<List<String>> source = PublishSubject.create();

    @Mock private PagingItemAdapter<String> adapter;
    @Mock private ImageOperations imageOperations;
    @Mock private Fragment fragment;
    @Mock private View view;
    @Mock private MultiSwipeRefreshLayout refreshLayout;
    @Mock private PullToRefreshWrapper pullToRefreshWrapper;
    @Mock private ListView listView;
    @Mock private EmptyView emptyView;
    @Mock private AbsListView.OnScrollListener scrollListener;
    @Mock private ListHeaderPresenter headerPresenter;

    @Captor private ArgumentCaptor<OnRefreshListener> refreshListenerCaptor;

    private TestSubscriber<Iterable<String>> testSubscriber = new TestSubscriber<>();

    @Before
    public void setup() {
        when(view.findViewById(android.R.id.list)).thenReturn(listView);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(view.findViewById(R.id.str_layout)).thenReturn(refreshLayout);
    }

    @Test
    public void shouldCreateListBindingWhenCreated() {
        ListBinding<String> listBinding = defaultBinding();
        createPresenterWithBinding(listBinding);

        listPresenter.onCreate(fragment, null);

        expect(listPresenter.getListBinding()).toBe(listBinding);
    }

    @Test
    public void shouldSubscribeAdapterWhenCreated() {
        ListBinding<String> listBinding = defaultBinding();
        createPresenterWithBinding(listBinding);

        listPresenter.onCreate(fragment, null);
        listBinding.connect();

        final List<String> listContent = Collections.singletonList("item");
        source.onNext(listContent);
        verify(adapter).onNext(listContent);
    }

    @Test
    public void shouldSubscribeViewObserversToListBindingInOnViewCreated() {
        ListBinding<String> listBinding = defaultBinding();
        createPresenterWithBinding(listBinding, testSubscriber);

        listPresenter.onCreate(fragment, null);
        listPresenter.onViewCreated(fragment, view, null);
        listBinding.connect();

        final List<String> listContent = Collections.singletonList("item");
        source.onNext(listContent);
        testSubscriber.assertReceivedOnNext(Collections.<Iterable<String>>singletonList(listContent));
    }

    @Test
    public void shouldSetAdapterForListView() {
        ListBinding<String> listBinding = defaultBinding();
        createPresenterWithBinding(listBinding);

        listPresenter.onCreate(fragment, null);
        listPresenter.onViewCreated(fragment, view, null);

        verify(listView).setAdapter(adapter);
    }

    @Test
    public void shouldRegisterDefaultScrollListener() {
        when(imageOperations.createScrollPauseListener(false, true)).thenReturn(scrollListener);

        ListBinding<String> listBinding = defaultBinding();
        createPresenterWithBinding(listBinding);

        listPresenter.onCreate(fragment, null);
        listPresenter.onViewCreated(fragment, view, null);

        verify(listView).setOnScrollListener(scrollListener);
    }

    @Test
    public void shouldWrapCustomScrollListenerInDefaultScrollListener() {
        ListBinding<String> listBinding = defaultBinding();
        createPresenterWithBinding(listBinding);
        AbsListView.OnScrollListener existingListener = mock(AbsListView.OnScrollListener.class);
        listPresenter.setScrollListener(existingListener);
        when(imageOperations.createScrollPauseListener(false, true, existingListener)).thenReturn(scrollListener);

        listPresenter.onCreate(fragment, null);
        listPresenter.onViewCreated(fragment, view, null);

        verify(listView).setOnScrollListener(scrollListener);
    }

    @Test
    public void shouldWrapScrollListenerInPagingScrollListenerIfListBindingIsPaged() {
        final ListBinding listBinding = ListBinding.from(source)
                .withAdapter(adapter)
                .withPager(TestPager.<List<String>>singlePageFunction())
                .build();
        createPresenterWithBinding(listBinding);

        listPresenter.onCreate(fragment, null);
        listPresenter.onViewCreated(fragment, view, null);

        verify(listView).setOnScrollListener(isA(PagingScrollListener.class));
    }

    @Test
    public void shouldAddRetryHandlerToPagingAdapterIfPageLoadFails() {
        final ListBinding listBinding = ListBinding.from(source)
                .withAdapter(adapter)
                .withPager(TestPager.<List<String>>singlePageFunction())
                .build();
        createPresenterWithBinding(listBinding);

        createPresenterWithBinding(listBinding);
        listPresenter.onCreate(fragment, null);
        listPresenter.onViewCreated(fragment, view, null);
        listBinding.connect();

        ArgumentCaptor<View.OnClickListener> retryCaptor = ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(adapter).setOnErrorRetryListener(retryCaptor.capture());

        retryCaptor.getValue().onClick(view);

        List<String> items = Collections.singletonList("item");
        source.onNext(items);

        verify(adapter).setLoading();
        verify(adapter).onNext(items);
    }

    @Test
    public void shouldAttachPullToRefreshListener() {
        ListBinding<String> listBinding = defaultBinding();
        createPresenterWithBinding(listBinding);

        listPresenter.onCreate(fragment, null);
        listPresenter.onViewCreated(fragment, view, null);

        verify(pullToRefreshWrapper).attach(refEq(refreshLayout), isA(OnRefreshListener.class));
    }

    @Test
    public void pullToRefreshListenerConnectsRefreshBindingOnRefresh() {
        ListBinding<String> refreshBinding = defaultBinding();
        createPresenterWithBinding(new ListBinding<>(Observable.<List<String>>never(), adapter), refreshBinding);

        triggerPullToRefresh();
        refreshBinding.getListItems().subscribe(testSubscriber);

        final List<String> listContent = singletonList("item");
        source.onNext(listContent);
        testSubscriber.assertReceivedOnNext(Collections.<Iterable<String>>singletonList(listContent));
    }

    @Test
    public void pullToRefreshStopsRefreshingIfRefreshFails() {
        ListBinding<String> refreshBinding = new ListBinding<>(Observable.<List<String>>error(new Exception("refresh failed")), adapter);
        createPresenterWithBinding(defaultBinding(), refreshBinding);

        triggerPullToRefresh();

        verify(pullToRefreshWrapper).setRefreshing(false);
    }

    @Test
    public void pullToRefreshStopsRefreshingIfRefreshSuccessful() {
        final ListBinding<String> listBinding = defaultBinding();
        final ListBinding<String> refreshBinding = defaultBinding();
        createPresenterWithBinding(listBinding, refreshBinding);

        triggerPullToRefresh();
        source.onNext(Collections.singletonList("item"));

        verify(pullToRefreshWrapper).setRefreshing(false);
    }

    @Test
    public void pullToRefreshClearsListAdapterIfRefreshSuccessful() {
        final ListBinding<String> listBinding = defaultBinding();
        final ListBinding<String> refreshBinding = defaultBinding();
        createPresenterWithBinding(listBinding, refreshBinding);

        triggerPullToRefresh();
        source.onNext(Collections.singletonList("item"));

        verify(adapter).clear();
    }

    @Test
    public void pullToRefreshSwapsOutPreviousListBindingWithRefreshedBindingIfRefreshSuccessful() {
        final ListBinding<String> refreshBinding = defaultBinding();
        createPresenterWithBinding(defaultBinding(), refreshBinding);

        triggerPullToRefresh();
        source.onNext(Collections.singletonList("item"));

        expect(listPresenter.getListBinding()).toBe(refreshBinding);
    }

    @Test
    public void pullToRefreshResubscribesListAdapterIfRefreshSuccessful() {
        createPresenterWithBinding(defaultBinding(), defaultBinding());

        final List<String> items = Collections.singletonList("item");
        triggerPullToRefresh();
        source.onNext(items);

        verify(adapter).onNext(items);
    }

    @Test
    public void pullToRefreshResubscribesViewObserversToNewListBindingIfRefreshSuccessful() {
        createPresenterWithBinding(defaultBinding(),
                defaultBinding(), testSubscriber);

        triggerPullToRefresh();
        final List<String> listContent = Collections.singletonList("items");
        source.onNext(listContent);

        testSubscriber.assertReceivedOnNext(Collections.<Iterable<String>>singletonList(listContent));
    }

    @Test
    public void shouldDetachPullToRefreshWrapperWhenViewsDestroyed() {
        createPresenterWithBinding(defaultBinding());

        listPresenter.onCreate(fragment, null);
        listPresenter.onViewCreated(fragment, view, null);
        listPresenter.onDestroyView(fragment);

        verify(pullToRefreshWrapper).detach();
    }

    @Test
    public void shouldDetachListAdapterFromListViewWhenViewsDestroyed() {
        createPresenterWithBinding(defaultBinding());

        listPresenter.onCreate(fragment, null);
        listPresenter.onViewCreated(fragment, view, null);
        listPresenter.onDestroyView(fragment);

        verify(listView).setAdapter(null);
    }

    @Test
    public void shouldDisconnectListBindingInOnDestroy() {
        final ListBinding<String> listBinding = defaultBinding();
        createPresenterWithBinding(listBinding);
        listPresenter.onCreate(fragment, null);
        Subscription subscription = listBinding.connect();

        listPresenter.onDestroy(fragment);

        expect(subscription.isUnsubscribed()).toBeTrue();
    }

    @Test
    public void shouldRebuildListBinding() {
        final ListBinding<String> firstBinding = defaultBinding();
        final ListBinding<String> secondBinding = defaultBinding();
        createPresenterWithPendingBindings(firstBinding, secondBinding);

        listPresenter.onCreate(fragment, null);
        expect(listPresenter.getListBinding()).toBe(firstBinding);

        listPresenter.rebuildListBinding(null);
        expect(listPresenter.getListBinding()).toBe(secondBinding);
    }

    @Test
    public void shouldConnectEmptyViewOnViewCreated() {
        createPresenterWithBinding(defaultBinding());
        listPresenter.onCreate(fragment, null);
        listPresenter.onViewCreated(fragment, view, null);
        Mockito.reset(emptyView);

        listPresenter.getListBinding().connect();
        source.onNext(Collections.singletonList("item"));

        verify(emptyView).setStatus(EmptyView.Status.OK);
    }

    @Test
    public void shouldConnectEmptyViewOnRetry() {
        createPresenterWithBinding(defaultBinding());
        listPresenter.onCreate(fragment, null);
        listPresenter.onViewCreated(fragment, view, null);
        Mockito.reset(emptyView);

        listPresenter.onRetry();
        source.onNext(Collections.singletonList("item"));

        verify(emptyView).setStatus(EmptyView.Status.OK);
    }

    @Test
    public void shouldConnectEmptyViewOnRefresh() {
        ListBinding<String> refreshBinding = defaultBinding();
        createPresenterWithBinding(new ListBinding<>(Observable.<List<String>>empty(), adapter), refreshBinding);
        triggerPullToRefresh();
        Mockito.reset(emptyView);

        source.onNext(Collections.singletonList("item"));

        verify(emptyView).setStatus(EmptyView.Status.OK);
    }

    @Test
    public void shouldSetEmptyViewOnListOnViewCreated() {
        createPresenterWithBinding(defaultBinding());
        listPresenter.onCreate(fragment, null);
        listPresenter.onViewCreated(fragment, view, null);
        verify(listView).setEmptyView(emptyView);
    }

    @Test
    public void shouldForwardViewCreatedEventToHeaderPresenter() {
        createPresenterWithBinding(defaultBinding());
        listPresenter.setHeaderPresenter(headerPresenter);
        listPresenter.onCreate(fragment, null);
        listPresenter.onViewCreated(fragment, view, null);
        verify(headerPresenter).onViewCreated(view, listView);
    }

    private void triggerPullToRefresh() {
        listPresenter.onCreate(fragment, null);
        listPresenter.onViewCreated(fragment, view, null);
        verify(pullToRefreshWrapper).attach(any(MultiSwipeRefreshLayout.class), refreshListenerCaptor.capture());
        OnRefreshListener refreshListener = refreshListenerCaptor.getValue();
        refreshListener.onRefresh();
    }

    private ListBinding<String> defaultBinding() {
        return new ListBinding<>(source, adapter);
    }

    private void createPresenterWithBinding(final ListBinding listBinding, final Observer... listObservers) {
        createPresenterWithBinding(listBinding, listBinding, listObservers);
    }

    private void createPresenterWithBinding(final ListBinding listBinding, final ListBinding refreshBinding,
                                            final Observer... listObservers) {
        listPresenter = new ListPresenter<String>(imageOperations, pullToRefreshWrapper) {
            @Override
            protected ListBinding<String> onBuildListBinding(Bundle fragmentArgs) {
                return listBinding;
            }

            @Override
            protected ListBinding<String> onBuildRefreshBinding() {
                return refreshBinding;
            }

            @Override
            protected void onSubscribeListBinding(ListBinding<String> listBinding) {
                for (Observer observer : listObservers) {
                    listBinding.addViewObserver(observer);
                }
            }
        };
    }

    private void createPresenterWithPendingBindings(final ListBinding... listBindings) {
        final List<ListBinding> pendingBindings = new LinkedList<>();
        pendingBindings.addAll(Arrays.asList(listBindings));
        listPresenter = new ListPresenter<String>(imageOperations, pullToRefreshWrapper) {
            @Override
            protected ListBinding<String> onBuildListBinding(Bundle fragmentArgs) {
                return pendingBindings.remove(0);
            }

            @Override
            protected void onSubscribeListBinding(ListBinding<String> listBinding) {
                // no op
            }
        };
    }
}