package com.soundcloud.android.presentation;

import static android.support.v7.widget.RecyclerView.AdapterDataObserver;
import static com.soundcloud.android.Expect.expect;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.image.RecyclerViewPauseOnScrollListener;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestPager;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.android.view.adapters.PagingRecyclerViewAdapter;
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
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class RecyclerViewPresenterTest {

    RecyclerViewPresenter<String> presenter;
    private PublishSubject<List<String>> source = PublishSubject.create();

    @Mock private PagingRecyclerViewAdapter<String, TestViewHolder> adapter;
    @Mock private Fragment fragment;
    @Mock private View view;
    @Mock private MultiSwipeRefreshLayout refreshLayout;
    @Mock private PullToRefreshWrapper pullToRefreshWrapper;
    @Mock private RecyclerView recyclerView;
    @Mock private EmptyView emptyView;
    @Mock private RecyclerViewPauseOnScrollListener recyclerViewPauseOnScrollListener;

    @Captor private ArgumentCaptor<SwipeRefreshLayout.OnRefreshListener> refreshListenerCaptor;

    private TestSubscriber<Iterable<String>> testSubscriber = new TestSubscriber<>();

    private View lastClickedView;
    private int lastClickedPosition;

    @Before
    public void setup() {
        when(view.findViewById(R.id.recycler_view)).thenReturn(recyclerView);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(view.findViewById(R.id.str_layout)).thenReturn(refreshLayout);
    }

    @Test
    public void shouldCreateBindingWhenCreated() {
        CollectionBinding<String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding);

        presenter.onCreate(fragment, null);

        expect(presenter.getBinding()).toBe(collectionBinding);
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
    public void shouldSetAdapterOnRecyclerView() {
        CollectionBinding<String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding);

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(recyclerView).setAdapter(adapter);
    }

    @Test
    public void shouldRegisterDefaultScrollListener() {
        CollectionBinding<String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding);

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(recyclerView).addOnScrollListener(recyclerViewPauseOnScrollListener);
    }

    @Test
    public void shouldWrapCustomScrollListenerInDefaultScrollListener() {
        CollectionBinding<String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding);
        RecyclerView.OnScrollListener existingListener = mock(RecyclerView.OnScrollListener.class);
        presenter.setOnScrollListener(existingListener);
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(recyclerView).addOnScrollListener(recyclerViewPauseOnScrollListener);
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

        verify(recyclerView).addOnScrollListener(isA(RecyclerViewPagingScrollListener.class));
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
    public void shouldAttachPullToRefreshListener() {
        CollectionBinding<String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding);

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(pullToRefreshWrapper).attach(refEq(refreshLayout), isA(SwipeRefreshLayout.OnRefreshListener.class));
    }

    @Test
    public void pullToRefreshListenerConnectsRefreshBindingOnRefresh() {
        CollectionBinding<String> refreshBinding = defaultBinding();
        createPresenterWithBinding(new CollectionBinding<>(Observable.<List<String>>never(), adapter), refreshBinding);

        triggerPullToRefresh();
        refreshBinding.items().subscribe(testSubscriber);

        final List<String> listContent = singletonList("item");
        source.onNext(listContent);
        testSubscriber.assertReceivedOnNext(Collections.<Iterable<String>>singletonList(listContent));
    }

    @Test
    public void pullToRefreshStopsRefreshingIfRefreshFails() {
        CollectionBinding<String> refreshBinding = new CollectionBinding<>(Observable.<List<String>>error(new Exception("refresh failed")), adapter);
        createPresenterWithBinding(defaultBinding(), refreshBinding);

        triggerPullToRefresh();

        verify(pullToRefreshWrapper).setRefreshing(false);
    }

    @Test
    public void pullToRefreshStopsRefreshingIfRefreshSuccessful() {
        final CollectionBinding<String> collectionBinding = defaultBinding();
        final CollectionBinding<String> refreshBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, refreshBinding);

        triggerPullToRefresh();
        source.onNext(Collections.singletonList("item"));

        verify(pullToRefreshWrapper).setRefreshing(false);
    }

    @Test
    public void pullToRefreshClearsAdapterIfRefreshSuccessful() {
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

        expect(presenter.getBinding()).toBe(refreshBinding);
    }

    @Test
    public void pullToRefreshResubscribesAdapterIfRefreshSuccessful() {
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
        when(recyclerView.getAdapter()).thenReturn(adapter);

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.onDestroyView(fragment);

        verify(pullToRefreshWrapper).detach();
    }

    @Test
    public void shouldDetachAdapterFromRecyclerViewWhenViewsDestroyed() {
        createPresenterWithBinding(defaultBinding());
        when(recyclerView.getAdapter()).thenReturn(adapter);

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.onDestroyView(fragment);

        verify(recyclerView).setAdapter(null);
    }

    @Test
    public void shouldUnregisterDataObserverInOnDestroyView() {
        createPresenterWithBinding(defaultBinding());
        when(recyclerView.getAdapter()).thenReturn(adapter);

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.onDestroyView(fragment);

        verify(adapter).unregisterAdapterDataObserver(any(RecyclerView.AdapterDataObserver.class));
    }

    @Test
    public void shouldDisconnectBindingInOnDestroy() {
        final CollectionBinding<String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding);
        presenter.onCreate(fragment, null);
        Subscription subscription = collectionBinding.connect();

        presenter.onDestroy(fragment);

        expect(subscription.isUnsubscribed()).toBeTrue();
    }

    @Test
    public void shouldRebuildBinding() {
        final CollectionBinding<String> firstBinding = defaultBinding();
        final CollectionBinding<String> secondBinding = defaultBinding();
        createPresenterWithPendingBindings(firstBinding, secondBinding);

        presenter.onCreate(fragment, null);
        expect(presenter.getBinding()).toBe(firstBinding);

        presenter.rebuildBinding(null);
        expect(presenter.getBinding()).toBe(secondBinding);
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
    public void shouldShowEmptyViewWithNoDataOnChangeObserved() {
        createPresenterWithBinding(defaultBinding());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        ArgumentCaptor<AdapterDataObserver> captor = ArgumentCaptor.forClass(AdapterDataObserver.class);
        verify(adapter).registerAdapterDataObserver(captor.capture());

        captor.getValue().onChanged();
        verify(emptyView).setVisibility(View.VISIBLE);
    }

    @Test
    public void shouldHideEmptyViewWithNoDataOnChangeObserved() {
        createPresenterWithBinding(defaultBinding());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        when(adapter.getItemCount()).thenReturn(1);

        ArgumentCaptor<AdapterDataObserver> captor = ArgumentCaptor.forClass(AdapterDataObserver.class);
        verify(adapter).registerAdapterDataObserver(captor.capture());

        captor.getValue().onChanged();
        verify(emptyView).setVisibility(View.GONE);
    }

    @Test
    public void shouldShowEmptyViewWithNoDataOnRangeInserted() {
        createPresenterWithBinding(defaultBinding());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        ArgumentCaptor<AdapterDataObserver> captor = ArgumentCaptor.forClass(AdapterDataObserver.class);
        verify(adapter).registerAdapterDataObserver(captor.capture());

        captor.getValue().onItemRangeInserted(0, 1);
        verify(emptyView).setVisibility(View.VISIBLE);
    }

    @Test
    public void shouldHideEmptyViewWithNoDataOnRangeInserted() {
        createPresenterWithBinding(defaultBinding());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        when(adapter.getItemCount()).thenReturn(1);

        ArgumentCaptor<AdapterDataObserver> captor = ArgumentCaptor.forClass(AdapterDataObserver.class);
        verify(adapter).registerAdapterDataObserver(captor.capture());

        captor.getValue().onItemRangeRemoved(0, 1);
        verify(emptyView).setVisibility(View.GONE);
    }

    @Test
    public void shouldShowEmptyViewWithNoDataOnRangeRemoved() {
        createPresenterWithBinding(defaultBinding());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        ArgumentCaptor<AdapterDataObserver> captor = ArgumentCaptor.forClass(AdapterDataObserver.class);
        verify(adapter).registerAdapterDataObserver(captor.capture());

        captor.getValue().onItemRangeRemoved(0, 1);
        verify(emptyView).setVisibility(View.VISIBLE);
    }

    @Test
    public void shouldHideEmptyViewWithNoDataOnRangeRemoved() {
        createPresenterWithBinding(defaultBinding());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        when(adapter.getItemCount()).thenReturn(1);

        ArgumentCaptor<AdapterDataObserver> captor = ArgumentCaptor.forClass(AdapterDataObserver.class);
        verify(adapter).registerAdapterDataObserver(captor.capture());

        captor.getValue().onItemRangeRemoved(0, 1);
        verify(emptyView).setVisibility(View.GONE);
    }

    @Test
    public void shouldCallClickListenerWithPosition() throws Exception {
        createPresenterWithBinding(defaultBinding());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        View itemView = mock(View.class);
        when(recyclerView.getChildAdapterPosition(itemView)).thenReturn(2);

        ArgumentCaptor<View.OnClickListener> clickListenerCaptor = ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(adapter).setOnItemClickListener(clickListenerCaptor.capture());
        clickListenerCaptor.getValue().onClick(itemView);

        expect(lastClickedView).toBe(itemView);
        expect(lastClickedPosition).toEqual(2);
    }

    private void triggerPullToRefresh() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        verify(pullToRefreshWrapper).attach(any(MultiSwipeRefreshLayout.class), refreshListenerCaptor.capture());
        SwipeRefreshLayout.OnRefreshListener refreshListener = refreshListenerCaptor.getValue();
        refreshListener.onRefresh();
    }

    private CollectionBinding<String> defaultBinding() {
        return new CollectionBinding<>(source, adapter);
    }

    private void createPresenterWithBinding(final CollectionBinding collectionBinding, final Observer... observers) {
        createPresenterWithBinding(collectionBinding, collectionBinding, observers);
    }

    private void createPresenterWithBinding(final CollectionBinding collectionBinding, final CollectionBinding refreshBinding,
                                            final Observer... observers) {
        presenter = new RecyclerViewPresenter<String>(pullToRefreshWrapper, recyclerViewPauseOnScrollListener) {
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
                for (Observer observer : observers) {
                    viewLifeCycle.add(collectionBinding.items().subscribe(observer));
                }
            }

            @Override
            protected void onItemClicked(View view, int position) {
                lastClickedView = view;
                lastClickedPosition = position;
            }
        };
    }

    private void createPresenterWithPendingBindings(final CollectionBinding... collectionBindings) {
        final List<CollectionBinding> pendingBindings = new LinkedList<>();
        pendingBindings.addAll(Arrays.asList(collectionBindings));
        presenter = new RecyclerViewPresenter<String>(pullToRefreshWrapper, recyclerViewPauseOnScrollListener) {
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
        };
    }

    private static class TestViewHolder extends RecyclerView.ViewHolder {

        public TestViewHolder(View itemView) {
            super(itemView);
        }
    }
}