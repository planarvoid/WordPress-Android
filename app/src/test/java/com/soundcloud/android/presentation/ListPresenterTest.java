package com.soundcloud.android.presentation;

import static android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.testsupport.PlatformUnitTest;
import com.soundcloud.android.testsupport.TestPager;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ListPresenterTest extends PlatformUnitTest {

    private ListPresenter<String> presenter;
    private PublishSubject<List<String>> source = PublishSubject.create();

    @Mock private PagingListItemAdapter adapter;
    @Mock private ImageOperations imageOperations;
    @Mock private Fragment fragment;
    @Mock private View view;
    @Mock private View itemView;
    @Mock private MultiSwipeRefreshLayout refreshLayout;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private ListView listView;
    @Mock private EmptyView emptyView;
    @Mock private AbsListView.OnScrollListener scrollListener;
    @Mock private ListHeaderPresenter headerPresenter;
    @Captor private ArgumentCaptor<OnRefreshListener> refreshListenerCaptor;
    @Captor private ArgumentCaptor<AdapterView.OnItemClickListener> clickListenerCaptor;

    private TestSubscriber<Iterable<String>> testSubscriber = new TestSubscriber<>();
    private View lastClickedView;
    private int lastClickedPosition;

    @Before
    public void setup() {
        when(view.findViewById(android.R.id.list)).thenReturn(listView);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
    }

    @Test
    public void shouldCreateBindingWhenCreated() {
        CollectionBinding<String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding);

        presenter.onCreate(fragment, null);

        assertThat(presenter.getBinding()).isSameAs(collectionBinding);
    }

    @Test
    public void shouldSubscribeAdapterWhenCreated() {
        CollectionBinding<String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding);

        presenter.onCreate(fragment, null);
        collectionBinding.connect();

        final List<String> listContent = Collections.singletonList("item");
        source.onNext(listContent);
        verify(adapter).onNext(listContent);
    }

    @Test
    public void shouldSubscribeViewObserversToBindingInOnViewCreated() {
        CollectionBinding<String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, testSubscriber);

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        collectionBinding.connect();

        final List<String> listContent = Collections.singletonList("item");
        source.onNext(listContent);
        testSubscriber.assertReceivedOnNext(Collections.<Iterable<String>>singletonList(listContent));
    }

    @Test
    public void shouldSetAdapterForListView() {
        CollectionBinding<String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding);

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(listView).setAdapter(adapter);
    }

    @Test
    public void shouldForwardItemClickEvents() {
        createPresenterWithBinding(defaultBinding());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(listView).setOnItemClickListener(clickListenerCaptor.capture());
        clickListenerCaptor.getValue().onItemClick(listView, itemView, 2, -1);
        assertThat(lastClickedView).isSameAs(itemView);
        assertThat(lastClickedPosition).isEqualTo(2);
    }

    @Test
    public void shouldRegisterDefaultScrollListener() {
        when(imageOperations.createScrollPauseListener(false, true)).thenReturn(scrollListener);

        CollectionBinding<String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding);

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(listView).setOnScrollListener(scrollListener);
    }

    @Test
    public void shouldWrapCustomScrollListenerInDefaultScrollListener() {
        CollectionBinding<String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding);
        AbsListView.OnScrollListener existingListener = mock(AbsListView.OnScrollListener.class);
        presenter.setScrollListener(existingListener);
        when(imageOperations.createScrollPauseListener(false, true, existingListener)).thenReturn(scrollListener);

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(listView).setOnScrollListener(scrollListener);
    }

    @Test
    public void shouldWrapScrollListenerInPagingScrollListenerIfBindingIsPaged() {
        final CollectionBinding collectionBinding = CollectionBinding.from(source)
                .withAdapter(adapter)
                .withPager(TestPager.<List<String>>singlePageFunction())
                .build();
        createPresenterWithBinding(collectionBinding);

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(listView).setOnScrollListener(isA(PagingListScrollListener.class));
    }

    @Test
    public void shouldAddRetryHandlerToPagingAdapterIfPageLoadFails() {
        final CollectionBinding collectionBinding = CollectionBinding.from(source)
                .withAdapter(adapter)
                .withPager(TestPager.<List<String>>singlePageFunction())
                .build();
        createPresenterWithBinding(collectionBinding);

        createPresenterWithBinding(collectionBinding);
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        collectionBinding.connect();

        ArgumentCaptor<View.OnClickListener> retryCaptor = ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(adapter).setOnErrorRetryListener(retryCaptor.capture());

        retryCaptor.getValue().onClick(view);

        List<String> items = Collections.singletonList("item");
        source.onNext(items);

        verify(adapter).setLoading();
        verify(adapter).onNext(items);
    }

    @Test
    public void shouldAttachPullToRefreshListenerIfFragmentIsRefreshableScreen() {
        Fragment refreshableFragment = mockRefreshableFragment();
        CollectionBinding<String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding);

        presenter.onCreate(refreshableFragment, null);
        presenter.onViewCreated(refreshableFragment, view, null);

        verify(swipeRefreshAttacher).attach(isA(OnRefreshListener.class), same(refreshLayout), same(listView));
    }

    @Test
    public void pullToRefreshListenerConnectsRefreshBindingOnRefresh() {
        CollectionBinding<String> refreshBinding = defaultBinding();
        createPresenterWithBinding(new CollectionBinding<>(Observable.<List<String>>never(), adapter), refreshBinding);

        triggerPullToRefresh();
        refreshBinding.items().subscribe(testSubscriber);

        final List<String> listContent = Collections.singletonList("item");
        source.onNext(listContent);
        testSubscriber.assertReceivedOnNext(Collections.<Iterable<String>>singletonList(listContent));
    }

    @Test
    public void pullToRefreshStopsRefreshingIfRefreshFails() {
        CollectionBinding<String> refreshBinding = new CollectionBinding<>(Observable.<List<String>>error(new Exception("refresh failed")), adapter);
        createPresenterWithBinding(defaultBinding(), refreshBinding);

        triggerPullToRefresh();

        verify(swipeRefreshAttacher).setRefreshing(false);
    }

    @Test
    public void pullToRefreshStopsRefreshingIfRefreshSuccessful() {
        final CollectionBinding<String> collectionBinding = defaultBinding();
        final CollectionBinding<String> refreshBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, refreshBinding);

        triggerPullToRefresh();
        source.onNext(Collections.singletonList("item"));

        verify(swipeRefreshAttacher).setRefreshing(false);
    }

    @Test
    public void pullToRefreshClearsListAdapterIfRefreshSuccessful() {
        final CollectionBinding<String> collectionBinding = defaultBinding();
        final CollectionBinding<String> refreshBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, refreshBinding);

        triggerPullToRefresh();
        source.onNext(Collections.singletonList("item"));

        verify(adapter).clear();
    }

    @Test
    public void pullToRefreshSwapsOutPreviousBindingWithRefreshedBindingIfRefreshSuccessful() {
        final CollectionBinding<String> refreshBinding = defaultBinding();
        createPresenterWithBinding(defaultBinding(), refreshBinding);

        triggerPullToRefresh();
        source.onNext(Collections.singletonList("item"));

        assertThat(presenter.getBinding()).isSameAs(refreshBinding);
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
    public void pullToRefreshResubscribesViewObserversToNewBindingIfRefreshSuccessful() {
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

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.onDestroyView(fragment);

        verify(swipeRefreshAttacher).detach();
    }

    @Test
    public void shouldDetachListAdapterFromListViewWhenViewsDestroyed() {
        createPresenterWithBinding(defaultBinding());

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.onDestroyView(fragment);

        verify(listView).setAdapter(null);
    }

    @Test
    public void shouldDisconnectBindingInOnDestroy() {
        final CollectionBinding<String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding);
        presenter.onCreate(fragment, null);
        Subscription subscription = collectionBinding.connect();

        presenter.onDestroy(fragment);

        assertThat(subscription.isUnsubscribed()).isTrue();
    }

    @Test
    public void shouldRebuildBinding() {
        final CollectionBinding<String> firstBinding = defaultBinding();
        final CollectionBinding<String> secondBinding = defaultBinding();
        createPresenterWithPendingBindings(firstBinding, secondBinding);

        presenter.onCreate(fragment, null);
        assertThat(presenter.getBinding()).isSameAs(firstBinding);

        presenter.rebuildBinding(null);
        assertThat(presenter.getBinding()).isSameAs(secondBinding);
    }

    @Test
    public void shouldConnectEmptyViewOnViewCreated() {
        createPresenterWithBinding(defaultBinding());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        Mockito.reset(emptyView);

        presenter.getBinding().connect();
        source.onNext(Collections.singletonList("item"));

        verify(emptyView).setStatus(EmptyView.Status.OK);
    }

    @Test
    public void shouldConnectEmptyViewOnRefresh() {
        CollectionBinding<String> refreshBinding = defaultBinding();
        createPresenterWithBinding(new CollectionBinding<>(Observable.<List<String>>empty(), adapter), refreshBinding);
        triggerPullToRefresh();
        Mockito.reset(emptyView);

        source.onNext(Collections.singletonList("item"));

        verify(emptyView).setStatus(EmptyView.Status.OK);
    }

    @Test
    public void shouldSetEmptyViewOnListOnViewCreated() {
        createPresenterWithBinding(defaultBinding());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        verify(listView).setEmptyView(emptyView);
    }

    @Test
    public void shouldForwardViewCreatedEventToHeaderPresenter() {
        createPresenterWithBinding(defaultBinding());
        presenter.setHeaderPresenter(headerPresenter);
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        verify(headerPresenter).onViewCreated(view, listView);
    }

    private void triggerPullToRefresh() {
        Fragment refreshableFragment = mockRefreshableFragment();
        presenter.onCreate(refreshableFragment, null);
        presenter.onViewCreated(refreshableFragment, view, null);
        verify(swipeRefreshAttacher).attach(refreshListenerCaptor.capture(), any(MultiSwipeRefreshLayout.class), any(View[].class));
        OnRefreshListener refreshListener = refreshListenerCaptor.getValue();
        refreshListener.onRefresh();
    }

    private CollectionBinding<String> defaultBinding() {
        return new CollectionBinding<>(source, adapter);
    }

    private void createPresenterWithBinding(final CollectionBinding collectionBinding, final Observer... listObservers) {
        createPresenterWithBinding(collectionBinding, collectionBinding, listObservers);
    }

    private Fragment mockRefreshableFragment() {
        Fragment refreshableFragment = mock(Fragment.class, withSettings().extraInterfaces(RefreshableScreen.class));
        when(((RefreshableScreen) refreshableFragment).getRefreshLayout()).thenReturn(refreshLayout);
        when(((RefreshableScreen) refreshableFragment).getRefreshableViews()).thenReturn(new View[]{listView});
        return refreshableFragment;
    }

    private void createPresenterWithBinding(final CollectionBinding collectionBinding, final CollectionBinding refreshBinding,
                                            final Observer... listObservers) {
        presenter = new ListPresenter<String>(imageOperations, swipeRefreshAttacher) {
            @Override
            protected CollectionBinding<String> onBuildBinding(Bundle fragmentArgs) {
                return collectionBinding;
            }

            @Override
            protected CollectionBinding<String> onRefreshBinding() {
                return refreshBinding;
            }

            @Override
            protected void onSubscribeBinding(CollectionBinding<String> collectionBinding, CompositeSubscription viewLifeCycle) {
                for (Observer observer : listObservers) {
                    viewLifeCycle.add(collectionBinding.items().subscribe(observer));
                }
            }

            @Override
            protected void onItemClicked(View view, int position) {
                lastClickedView = view;
                lastClickedPosition = position;
            }

            @Override
            protected EmptyView.Status handleError(Throwable error) {
                return EmptyView.Status.OK;
            }
        };
    }

    private void createPresenterWithPendingBindings(final CollectionBinding... collectionBindings) {
        final List<CollectionBinding> pendingBindings = new LinkedList<>();
        pendingBindings.addAll(Arrays.asList(collectionBindings));
        presenter = new ListPresenter<String>(imageOperations, swipeRefreshAttacher) {
            @Override
            protected CollectionBinding<String> onBuildBinding(Bundle fragmentArgs) {
                return pendingBindings.remove(0);
            }

            @Override
            protected void onSubscribeBinding(CollectionBinding<String> collectionBinding, CompositeSubscription viewLifeCycle) {
                // no op
            }

            @Override
            protected void onItemClicked(View view, int position) {
                lastClickedView = view;
                lastClickedPosition = position;
            }

            @Override
            protected EmptyView.Status handleError(Throwable error) {
                return EmptyView.Status.OK;
            }
        };
    }
}