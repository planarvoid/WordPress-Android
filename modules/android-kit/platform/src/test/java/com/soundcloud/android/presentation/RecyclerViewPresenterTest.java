package com.soundcloud.android.presentation;

import static android.support.v7.widget.RecyclerView.AdapterDataObserver;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Java6Assertions.assertThat;
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

import com.soundcloud.android.presentation.RecyclerViewPresenter.Options;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TestPager;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.androidkit.R;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.internal.util.UtilityFunctions;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class RecyclerViewPresenterTest extends AndroidUnitTest {

    private RecyclerViewPresenter<Iterable<String>, String> presenter;
    private PublishSubject<List<String>> source = PublishSubject.create();

    @Mock private Resources resources;
    @Mock private PagingRecyclerItemAdapter<String, TestViewHolder> adapter;
    @Mock private Fragment fragment;
    @Mock private View view;
    @Mock private MultiSwipeRefreshLayout refreshLayout;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private RecyclerView recyclerView;
    @Mock private EmptyView emptyView;
    @Mock private SimpleItemAnimator simpleItemAnimator;
    @Mock private Observer<Iterable<String>> listObserver;
    @Mock private Observer<Iterable<String>> observer;
    @Captor private ArgumentCaptor<OnRefreshListener> refreshListenerCaptor;

    private TestSubscriber<Iterable<String>> testSubscriber = new TestSubscriber<>();

    private View lastClickedView;
    private int lastClickedPosition;

    @Before
    public void setup() {
        when(view.findViewById(R.id.ak_recycler_view)).thenReturn(recyclerView);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(view.getResources()).thenReturn(resources);

        when(recyclerView.getResources()).thenReturn(resources);
        when(resources.getInteger(R.integer.ak_default_grid_columns)).thenReturn(1);
        when(recyclerView.getItemAnimator()).thenReturn(simpleItemAnimator);
    }

    @Test
    public void doesNotUseChangeAnimationsByDefault() {
        CollectionBinding<Iterable<String>, String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, Options.custom().build());

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(simpleItemAnimator).setSupportsChangeAnimations(false);
    }

    @Test
    public void shouldUseChangeAnimations() {
        CollectionBinding<Iterable<String>, String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, Options.custom().useChangeAnimations().build());

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(simpleItemAnimator).setSupportsChangeAnimations(true);
    }

    @Test
    public void shouldUseLinearLayoutManagerForLists() {
        CollectionBinding<Iterable<String>, String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, Options.list().build());

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(recyclerView).setLayoutManager(isA(LinearLayoutManager.class));
    }

    @Test
    public void shouldUseGridLayoutManagerForGrids() {
        CollectionBinding<Iterable<String>, String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, Options.grid(R.integer.ak_default_grid_columns).build());

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(recyclerView).setLayoutManager(isA(GridLayoutManager.class));
    }

    @Test
    public void shouldUseStaggeredGridLayoutManagerForStaggeredGrids() {
        CollectionBinding<Iterable<String>, String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, Options.staggeredGrid(R.integer.ak_default_grid_columns).build());

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(recyclerView).setLayoutManager(isA(StaggeredGridLayoutManager.class));
    }

    @Test
    public void shouldCreateBindingWhenCreated() {
        CollectionBinding<Iterable<String>, String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, Options.list().build());

        presenter.onCreate(fragment, null);

        assertThat(presenter.getBinding()).isSameAs(collectionBinding);
    }

    @Test
    public void shouldSubscribeObserverWhenCreated() {
        final List<String> listContent = Collections.singletonList("item");
        CollectionBinding<List<String>, String> collectionBinding = CollectionBinding.from(Observable.just(listContent))
                .withAdapter(adapter)
                .addObserver(observer)
                .build();

        createPresenterWithBinding(collectionBinding, Options.list().build());

        presenter.onCreate(fragment, null);
        collectionBinding.connect();

        source.onNext(listContent);
        verify(observer).onNext(listContent);
        verify(adapter).onNext(listContent);
    }

    @Test
    public void shouldSubscribeViewObserversToBindingInOnViewCreated() {
        CollectionBinding<Iterable<String>, String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, Options.list().build(), testSubscriber);

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        collectionBinding.connect();

        final List<String> listContent = Collections.singletonList("item");
        source.onNext(listContent);
        testSubscriber.assertReceivedOnNext(Collections.<Iterable<String>>singletonList(listContent));
    }

    @Test
    public void shouldSetAdapterOnRecyclerView() {
        CollectionBinding<Iterable<String>, String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, Options.list().build());

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(recyclerView).setAdapter(adapter);
    }

    @Test
    public void shouldSetDividersWithListOption() {
        CollectionBinding<Iterable<String>, String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, new Options.Builder().useDividers(Options.DividerMode.LIST).build());

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(recyclerView).addItemDecoration(any(DividerItemDecoration.class));
    }

    @Test
    public void shouldSetDividersWithGridOption() {
        CollectionBinding<Iterable<String>, String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, new Options.Builder().useDividers(Options.DividerMode.GRID).build());

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(recyclerView).addItemDecoration(any(InsetDividerDecoration.class));
    }

    @Test
    public void shouldNotSetDividersForCards() {
        CollectionBinding<Iterable<String>, String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, Options.defaults());

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(recyclerView, never()).addItemDecoration(any(DividerItemDecoration.class));
    }

    @Test
    public void shouldAddPagingScrollListenerIfBindingIsPaged() {
        when(recyclerView.getLayoutManager()).thenReturn(mock(LinearLayoutManager.class));
        final CollectionBinding collectionBinding = CollectionBinding.from(source)
                .withAdapter(adapter)
                .withPager(TestPager.<List<String>>singlePageFunction())
                .build();
        createPresenterWithBinding(collectionBinding, Options.list().build());

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(recyclerView).addOnScrollListener(isA(PagingRecyclerScrollListener.class));
    }

    @Test
    public void shouldAttachPullToRefreshListener() {
        Fragment refreshableFragment = mockRefreshableFragment();
        CollectionBinding<Iterable<String>, String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, Options.list().build());

        presenter.onCreate(refreshableFragment, null);
        presenter.onViewCreated(refreshableFragment, view, null);

        verify(swipeRefreshAttacher).attach(
                isA(OnRefreshListener.class), same(refreshLayout), same(recyclerView), same(emptyView));
    }

    @Test
    public void shouldNotAttachPullToRefreshListenerIfFragmentNotRefreshable() {
        CollectionBinding<Iterable<String>, String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, Options.list().build());

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verifyZeroInteractions(swipeRefreshAttacher);
    }

    @Test
    public void pullToRefreshListenerConnectsRefreshBindingOnRefresh() {
        CollectionBinding<Iterable<String>, String> refreshBinding = defaultBinding();
        createPresenterWithBinding(getCollectionBinding(Observable.<List<String>>never(), adapter), refreshBinding, Options.list().build());

        triggerPullToRefresh();
        refreshBinding.items().subscribe(testSubscriber);

        final List<String> listContent = singletonList("item");
        source.onNext(listContent);
        testSubscriber.assertReceivedOnNext(Collections.<Iterable<String>>singletonList(listContent));
    }


    @Test
    public void pullToRefreshStopsRefreshingIfRefreshFails() {
        CollectionBinding<Iterable<String>, String> refreshBinding = new CollectionBinding<>(
                Observable.<List<String>>error(new Exception("refresh failed")),
                UtilityFunctions.<Iterable<String>>identity(),
                adapter,
                Schedulers.immediate(),
                Collections.<Observer<Iterable<String>>>singletonList(adapter));
        createPresenterWithBinding(defaultBinding(), refreshBinding, Options.list().build());

        triggerPullToRefresh();

        verify(swipeRefreshAttacher).setRefreshing(false);
    }

    @Test
    public void pullToRefreshStopsRefreshingIfRefreshSuccessful() {
        final CollectionBinding<Iterable<String>, String> collectionBinding = defaultBinding();
        final CollectionBinding<Iterable<String>, String> refreshBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, refreshBinding, Options.list().build());

        triggerPullToRefresh();
        source.onNext(Collections.singletonList("item"));

        verify(swipeRefreshAttacher).setRefreshing(false);
    }

    @Test
    public void pullToRefreshClearsAdapterIfRefreshSuccessful() {
        final CollectionBinding<Iterable<String>, String> collectionBinding = defaultBinding();
        final CollectionBinding<Iterable<String>, String> refreshBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, refreshBinding, Options.list().build());

        triggerPullToRefresh();
        source.onNext(Collections.singletonList("item"));

        verify(adapter).clear();
    }

    @Test
    public void pullToRefreshSwapsOutPreviousBindingWithRefreshedBindingIfRefreshSuccessful() {
        final CollectionBinding<Iterable<String>, String> refreshBinding = defaultBinding();
        createPresenterWithBinding(defaultBinding(), refreshBinding, Options.list().build());

        triggerPullToRefresh();
        source.onNext(Collections.singletonList("item"));

        assertThat(presenter.getBinding()).isSameAs(refreshBinding);
    }

    @Test
    public void pullToRefreshResubscribesAdapterIfRefreshSuccessful() {
        createPresenterWithBinding(defaultBinding(), defaultBinding(), Options.list().build());

        final List<String> items = Collections.singletonList("item");
        triggerPullToRefresh();
        source.onNext(items);

        verify(adapter).onNext(items);
    }

    @Test
    public void pullToRefreshResubscribesViewObserversToNewBindingIfRefreshSuccessful() {
        createPresenterWithBinding(defaultBinding(),
                defaultBinding(), Options.list().build(), listObserver);

        triggerPullToRefresh();
        final List<String> listContent = Collections.singletonList("items");
        source.onNext(listContent);

        verify(listObserver).onNext(listContent);
    }

    @Test
    public void shouldDetachPullToRefreshWrapperWhenViewsDestroyed() {
        createPresenterWithBinding(defaultBinding(), Options.list().build());
        when(recyclerView.getAdapter()).thenReturn(adapter);

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.onDestroyView(fragment);

        verify(swipeRefreshAttacher).detach();
    }

    @Test
    public void shouldDetachAdapterFromRecyclerViewWhenViewsDestroyed() {
        createPresenterWithBinding(defaultBinding(), Options.list().build());
        when(recyclerView.getAdapter()).thenReturn(adapter);

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.onDestroyView(fragment);

        verify(recyclerView).setAdapter(null);
    }

    @Test
    public void shouldUnregisterDataObserverInOnDestroyView() {
        createPresenterWithBinding(defaultBinding(), Options.list().build());
        when(recyclerView.getAdapter()).thenReturn(adapter);

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.onDestroyView(fragment);

        verify(adapter).unregisterAdapterDataObserver(any(RecyclerView.AdapterDataObserver.class));
    }

    @Test
    public void shouldDetachScrollListenersInOnDestroyView() {
        createPresenterWithBinding(defaultBinding(), Options.list().build());
        when(recyclerView.getAdapter()).thenReturn(adapter);

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.onDestroyView(fragment);

        verify(recyclerView).clearOnScrollListeners();
    }

    @Test
    public void shouldDisconnectBindingInOnDestroy() {
        final CollectionBinding<Iterable<String>, String> collectionBinding = defaultBinding();
        createPresenterWithBinding(collectionBinding, Options.list().build());
        presenter.onCreate(fragment, null);
        Subscription subscription = collectionBinding.connect();

        presenter.onDestroy(fragment);

        assertThat(subscription.isUnsubscribed()).isTrue();
    }

    @Test
    public void shouldRebuildBinding() {
        final CollectionBinding<Iterable<String>, String> firstBinding = defaultBinding();
        final CollectionBinding<Iterable<String>, String> secondBinding = defaultBinding();
        createPresenterWithPendingBindings(firstBinding, secondBinding);

        presenter.onCreate(fragment, null);
        assertThat(presenter.getBinding()).isSameAs(firstBinding);

        presenter.rebuildBinding(null);
        assertThat(presenter.getBinding()).isSameAs(secondBinding);
    }

    @Test
    public void shouldConnectEmptyViewOnViewCreated() {
        createPresenterWithBinding(defaultBinding(), Options.list().build());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        Mockito.reset(emptyView);

        presenter.getBinding().connect();
        source.onNext(Collections.singletonList("item"));

        verify(emptyView).setStatus(EmptyView.Status.OK);
    }

    @Test
    public void shouldShowEmptyViewWithNoDataOnChangeObserved() {
        createPresenterWithBinding(defaultBinding(), Options.list().build());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        ArgumentCaptor<AdapterDataObserver> captor = ArgumentCaptor.forClass(AdapterDataObserver.class);
        verify(adapter).registerAdapterDataObserver(captor.capture());

        when(adapter.isEmpty()).thenReturn(true);
        captor.getValue().onChanged();
        verify(emptyView).setVisibility(View.VISIBLE);
    }

    @Test
    public void shouldHideRecyclerViewWithNoDataOnChangeObserved() {
        createPresenterWithBinding(defaultBinding(), Options.list().build());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        ArgumentCaptor<AdapterDataObserver> captor = ArgumentCaptor.forClass(AdapterDataObserver.class);
        verify(adapter).registerAdapterDataObserver(captor.capture());

        when(adapter.isEmpty()).thenReturn(true);
        captor.getValue().onChanged();
        verify(recyclerView).setVisibility(View.GONE);
    }

    @Test
    public void shouldHideEmptyViewWithNoDataOnChangeObserved() {
        createPresenterWithBinding(defaultBinding(), Options.list().build());
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
        createPresenterWithBinding(defaultBinding(), Options.list().build());
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
        createPresenterWithBinding(defaultBinding(), Options.list().build());
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
        createPresenterWithBinding(defaultBinding(), Options.list().build());
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
        createPresenterWithBinding(defaultBinding(), Options.list().build());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        ArgumentCaptor<AdapterDataObserver> captor = ArgumentCaptor.forClass(AdapterDataObserver.class);
        verify(adapter).registerAdapterDataObserver(captor.capture());

        when(adapter.isEmpty()).thenReturn(false);
        captor.getValue().onItemRangeRemoved(0, 1);
        verify(emptyView, times(2)).setVisibility(View.GONE);
    }

    @Test
    public void shouldNotSetClickListenerForCards() throws Exception {
        createPresenterWithBinding(defaultBinding(), Options.defaults());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        verify(adapter, never()).setOnItemClickListener(any(View.OnClickListener.class));
    }

    @Test
    public void shouldCallClickListenerWithPosition() throws Exception {
        createPresenterWithBinding(defaultBinding(), Options.list().build());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        View itemView = mock(View.class);
        when(recyclerView.getChildAdapterPosition(itemView)).thenReturn(2);
        when(adapter.getItemCount()).thenReturn(3);

        ArgumentCaptor<View.OnClickListener> clickListenerCaptor = ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(adapter).setOnItemClickListener(clickListenerCaptor.capture());
        clickListenerCaptor.getValue().onClick(itemView);

        assertThat(lastClickedView).isSameAs(itemView);
        assertThat(lastClickedPosition).isEqualTo(2);
    }

    @Test
    public void shouldNotCallClickListenerWithInvalidPosition() throws Exception {
        createPresenterWithBinding(defaultBinding(), Options.list().build());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        View itemView = mock(View.class);
        when(recyclerView.getChildAdapterPosition(itemView)).thenReturn(-1);

        ArgumentCaptor<View.OnClickListener> clickListenerCaptor = ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(adapter).setOnItemClickListener(clickListenerCaptor.capture());
        clickListenerCaptor.getValue().onClick(itemView);

        assertThat(lastClickedView).isNull();
    }

    @Test
    public void attachesExternalRefreshListener() throws Exception {
        final MultiSwipeRefreshLayout refreshLayout = mock(MultiSwipeRefreshLayout.class);
        createPresenterWithBinding(defaultBinding(), Options.list().build());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.attachSwipeToRefresh(refreshLayout, recyclerView);

        verify(swipeRefreshAttacher).attach(any(OnRefreshListener.class), same(refreshLayout), same(recyclerView));
    }

    @Test
    public void detachRefreshWrapperStopsRefreshingAndDetachesWrapper() throws Exception {
        createPresenterWithBinding(defaultBinding(), Options.list().build());
        presenter.detachSwipeToRefresh();

        InOrder inOrder = Mockito.inOrder(swipeRefreshAttacher);
        inOrder.verify(swipeRefreshAttacher).setRefreshing(false);
        inOrder.verify(swipeRefreshAttacher).detach();
    }

    @Test
    public void attachExternalRefreshSetsRefreshingIfInTheMiddleOfRefresh() {
        final MultiSwipeRefreshLayout refreshLayout = mock(MultiSwipeRefreshLayout.class);
        CollectionBinding<Iterable<String>, String> refreshBinding = defaultBinding();
        createPresenterWithBinding(defaultBinding(), refreshBinding, Options.list().build());

        triggerPullToRefresh();
        presenter.attachSwipeToRefresh(refreshLayout);

        verify(swipeRefreshAttacher).setRefreshing(true);
    }

    @Test
    public void attachExternalRefreshDoesNotSetRefreshingIfRefreshComplete() {
        final MultiSwipeRefreshLayout refreshLayout = mock(MultiSwipeRefreshLayout.class);
        final CollectionBinding<Iterable<String>, String> refreshBinding = defaultBinding();
        createPresenterWithBinding(defaultBinding(), refreshBinding, Options.list().build());

        triggerPullToRefresh();
        source.onNext(Collections.singletonList("item"));
        presenter.attachSwipeToRefresh(refreshLayout);

        verify(swipeRefreshAttacher, never()).setRefreshing(true);
    }

    @Test
    public void attachExternalRefreshDoesNotSetRefreshingOnRefreshError() {
        final MultiSwipeRefreshLayout refreshLayout = mock(MultiSwipeRefreshLayout.class);
        CollectionBinding<Iterable<String>, String> refreshBinding = new CollectionBinding<>(
                Observable.<List<String>>error(new Exception("refresh failed")),
                UtilityFunctions.<Iterable<String>>identity(),
                adapter,
                Schedulers.immediate(),
                Collections.<Observer<Iterable<String>>>singletonList(adapter));
        createPresenterWithBinding(defaultBinding(), refreshBinding, Options.list().build());

        triggerPullToRefresh();
        source.onNext(Collections.singletonList("item"));
        presenter.attachSwipeToRefresh(refreshLayout);

        verify(swipeRefreshAttacher, never()).setRefreshing(true);
    }

    @Test
    public void scrollToTopSmoothScrollsToFirstItem() {
        createPresenterWithBinding(defaultBinding(), Options.list().build());
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        presenter.scrollToTop();

        verify(recyclerView).smoothScrollToPosition(0);
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

    private CollectionBinding<Iterable<String>, String> defaultBinding() {
        return getCollectionBinding(source, adapter);
    }

    private CollectionBinding<Iterable<String>, String> getCollectionBinding(Observable<List<String>> source, PagingRecyclerItemAdapter<String, TestViewHolder> adapter) {
        return new CollectionBinding<>(source, UtilityFunctions.<Iterable<String>>identity(), adapter, Schedulers.immediate(), Collections.<Observer<Iterable<String>>>singletonList(adapter));
    }

    private void createPresenterWithBinding(final CollectionBinding collectionBinding, Options options, final Observer... observers) {
        createPresenterWithBinding(collectionBinding, collectionBinding, options, observers);
    }

    private void createPresenterWithBinding(final CollectionBinding collectionBinding, final CollectionBinding refreshBinding,
                                            Options options, final Observer... observers) {
        presenter = new RecyclerViewPresenter<Iterable<String>, String>(swipeRefreshAttacher, options) {
            @Override
            protected CollectionBinding<Iterable<String>, String> onBuildBinding(Bundle fragmentArgs) {
                return collectionBinding;
            }

            @Override
            protected CollectionBinding<Iterable<String>, String> onRefreshBinding() {
                return refreshBinding;
            }

            @Override
            protected void onSubscribeBinding(CollectionBinding<Iterable<String>, String> collectionBinding, CompositeSubscription viewLifeCycle) {
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
        presenter = new RecyclerViewPresenter<Iterable<String>, String>(swipeRefreshAttacher) {
            @Override
            protected CollectionBinding<Iterable<String>, String> onBuildBinding(Bundle fragmentArgs) {
                return pendingBindings.remove(0);
            }

            @Override
            protected void onSubscribeBinding(CollectionBinding<Iterable<String>, String> collectionBinding, CompositeSubscription viewLifeCycle) {
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
