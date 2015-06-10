package com.soundcloud.android.presentation;

import static android.support.v7.widget.RecyclerView.AdapterDataObserver;
import static com.soundcloud.android.Expect.expect;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestPager;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
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
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
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

    @Mock private PagingRecyclerItemAdapter<String, TestViewHolder> adapter;
    @Mock private Fragment fragment;
    @Mock private View view;
    @Mock private MultiSwipeRefreshLayout refreshLayout;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private RecyclerView recyclerView;
    @Mock private EmptyView emptyView;
    @Mock private ImagePauseOnScrollListener imagePauseOnScrollListener;
    @Mock private DividerItemDecoration dividerItemDecoration;
    @Captor private ArgumentCaptor<OnRefreshListener> refreshListenerCaptor;

    private TestSubscriber<Iterable<String>> testSubscriber = new TestSubscriber<>();

    private View lastClickedView;
    private int lastClickedPosition;


    @Before
    public void setup() {
        when(view.findViewById(R.id.recycler_view)).thenReturn(recyclerView);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(view.getResources()).thenReturn(Robolectric.application.getResources());
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

        verify(recyclerView).addOnScrollListener(imagePauseOnScrollListener);
    }

    @Test
    public void shouldWrapCustomScrollListenerInDefaultScrollListener() {
        CollectionBinding<String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding);
        RecyclerView.OnScrollListener existingListener = mock(RecyclerView.OnScrollListener.class);
        presenter.setOnScrollListener(existingListener);
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(recyclerView).addOnScrollListener(imagePauseOnScrollListener);
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

        verify(recyclerView).addOnScrollListener(isA(PagingRecyclerScrollListener.class));
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
        Fragment refreshableFragment = mockRefreshableFragment();
        CollectionBinding<String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding);

        presenter.onCreate(refreshableFragment, null);
        presenter.onViewCreated(refreshableFragment, view, null);

        verify(swipeRefreshAttacher).attach(
                isA(OnRefreshListener.class), same(refreshLayout), same(recyclerView), same(emptyView));
    }

    @Test
    public void shouldNotAttachPullToRefreshListenerIfFragmentNotRefreshable() {
        CollectionBinding<String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding);

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verifyZeroInteractions(swipeRefreshAttacher);
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

        verify(swipeRefreshAttacher).detach();
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

        when(adapter.isEmpty()).thenReturn(true);
        captor.getValue().onChanged();
        verify(emptyView).setVisibility(View.VISIBLE);
    }

    @Test
    public void shouldHideEmptyViewWithNoDataOnChangeObserved() {
        createPresenterWithBinding(defaultBinding());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        ArgumentCaptor<AdapterDataObserver> captor = ArgumentCaptor.forClass(AdapterDataObserver.class);
        verify(adapter).registerAdapterDataObserver(captor.capture());

        when(adapter.isEmpty()).thenReturn(false);
        captor.getValue().onChanged();
        verify(emptyView, times(2)).setVisibility(View.GONE);
    }

    @Test
    public void shouldShowEmptyViewWithNoDataOnRangeInserted() {
        createPresenterWithBinding(defaultBinding());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        ArgumentCaptor<AdapterDataObserver> captor = ArgumentCaptor.forClass(AdapterDataObserver.class);
        verify(adapter).registerAdapterDataObserver(captor.capture());

        when(adapter.isEmpty()).thenReturn(true);
        captor.getValue().onItemRangeInserted(0, 1);
        verify(emptyView).setVisibility(View.VISIBLE);
    }

    @Test
    public void shouldHideEmptyViewWithNoDataOnRangeInserted() {
        createPresenterWithBinding(defaultBinding());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        ArgumentCaptor<AdapterDataObserver> captor = ArgumentCaptor.forClass(AdapterDataObserver.class);
        verify(adapter).registerAdapterDataObserver(captor.capture());

        when(adapter.isEmpty()).thenReturn(false);
        captor.getValue().onItemRangeRemoved(0, 1);
        verify(emptyView, times(2)).setVisibility(View.GONE);
    }

    @Test
    public void shouldShowEmptyViewWithNoDataOnRangeRemoved() {
        createPresenterWithBinding(defaultBinding());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        ArgumentCaptor<AdapterDataObserver> captor = ArgumentCaptor.forClass(AdapterDataObserver.class);
        verify(adapter).registerAdapterDataObserver(captor.capture());

        when(adapter.isEmpty()).thenReturn(true);
        captor.getValue().onItemRangeRemoved(0, 1);
        verify(emptyView).setVisibility(View.VISIBLE);
    }

    @Test
    public void shouldHideEmptyViewWithNoDataOnRangeRemoved() {
        createPresenterWithBinding(defaultBinding());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        ArgumentCaptor<AdapterDataObserver> captor = ArgumentCaptor.forClass(AdapterDataObserver.class);
        verify(adapter).registerAdapterDataObserver(captor.capture());

        when(adapter.isEmpty()).thenReturn(false);
        captor.getValue().onItemRangeRemoved(0, 1);
        verify(emptyView, times(2)).setVisibility(View.GONE);
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

    @Test
    public void attachesExternalRefreshListener() throws Exception {
        final MultiSwipeRefreshLayout refreshLayout = mock(MultiSwipeRefreshLayout.class);
        createPresenterWithBinding(defaultBinding());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.attachSwipeToRefresh(refreshLayout, recyclerView);

        verify(swipeRefreshAttacher).attach(any(OnRefreshListener.class), same(refreshLayout), same(recyclerView));
    }

    @Test
    public void detachRefreshWrapperStopsRefreshingAndDetachesWrapper() throws Exception {
        createPresenterWithBinding(defaultBinding());
        presenter.detachSwipeToRefresh();

        InOrder inOrder = Mockito.inOrder(swipeRefreshAttacher);
        inOrder.verify(swipeRefreshAttacher).setRefreshing(false);
        inOrder.verify(swipeRefreshAttacher).detach();
    }

    @Test
    public void attachExternalRefreshSetsRefreshingIfInTheMiddleOfRefresh() {
        final MultiSwipeRefreshLayout refreshLayout = mock(MultiSwipeRefreshLayout.class);
        final CollectionBinding<String> refreshBinding = defaultBinding();
        createPresenterWithBinding(defaultBinding(), refreshBinding);

        triggerPullToRefresh();
        presenter.attachSwipeToRefresh(refreshLayout);

        verify(refreshLayout).setRefreshing(true);
    }

    @Test
    public void attachExternalRefreshDoesNotSetRefreshingIfRefreshComplete() {
        final MultiSwipeRefreshLayout refreshLayout = mock(MultiSwipeRefreshLayout.class);
        final CollectionBinding<String> refreshBinding = defaultBinding();
        createPresenterWithBinding(defaultBinding(), refreshBinding);

        triggerPullToRefresh();
        source.onNext(Collections.singletonList("item"));
        presenter.attachSwipeToRefresh(refreshLayout);

        verify(refreshLayout, never()).setRefreshing(true);
    }

    private void triggerPullToRefresh() {
        Fragment refreshableFragment = mockRefreshableFragment();
        presenter.onCreate(refreshableFragment, null);
        presenter.onViewCreated(refreshableFragment, view, null);
        verify(swipeRefreshAttacher).attach(
                refreshListenerCaptor.capture(), isA(MultiSwipeRefreshLayout.class), same(recyclerView), same(emptyView));
        OnRefreshListener refreshListener = refreshListenerCaptor.getValue();
        refreshListener.onRefresh();
    }

    private Fragment mockRefreshableFragment() {
        Fragment fragment = mock(Fragment.class, withSettings().extraInterfaces(RefreshableScreen.class));
        when(((RefreshableScreen) fragment).getRefreshLayout()).thenReturn(refreshLayout);
        when(((RefreshableScreen) fragment).getRefreshableViews()).thenReturn(new View[]{recyclerView, emptyView});
        return fragment;
    }

    private CollectionBinding<String> defaultBinding() {
        return new CollectionBinding<>(source, adapter);
    }

    private void createPresenterWithBinding(final CollectionBinding collectionBinding, final Observer... observers) {
        createPresenterWithBinding(collectionBinding, collectionBinding, observers);
    }

    private void createPresenterWithBinding(final CollectionBinding collectionBinding, final CollectionBinding refreshBinding,
                                            final Observer... observers) {
        presenter = new RecyclerViewPresenter<String>(swipeRefreshAttacher, imagePauseOnScrollListener) {
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

            @Override
            protected EmptyView.Status handleError(Throwable error) {
                return EmptyView.Status.OK;
            }
        };
    }

    private void createPresenterWithPendingBindings(final CollectionBinding... collectionBindings) {
        final List<CollectionBinding> pendingBindings = new LinkedList<>();
        pendingBindings.addAll(Arrays.asList(collectionBindings));
        presenter = new RecyclerViewPresenter<String>(swipeRefreshAttacher, imagePauseOnScrollListener) {
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

    private static class TestViewHolder extends RecyclerView.ViewHolder {

        public TestViewHolder(View itemView) {
            super(itemView);
        }
    }
}