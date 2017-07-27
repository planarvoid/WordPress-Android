package com.soundcloud.android.actionbar;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.refEq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.presentation.PagingListItemAdapter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.TestPager;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.android.view.RefreshableListComponent;
import com.soundcloud.android.view.adapters.ReactiveAdapter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import rx.Observable;
import rx.Subscription;
import rx.android.LegacyPager;
import rx.observables.ConnectableObservable;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.View;
import android.widget.AdapterView;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class PullToRefreshControllerTest {

    private RefreshableFragment fragment = new RefreshableFragment();

    @Mock private FragmentActivity activity;
    @Mock private Bundle bundle;
    @Mock private OnRefreshListener listener;
    @Mock private SwipeRefreshAttacher wrapper;
    @Mock private MultiSwipeRefreshLayout layout;
    @Mock private Subscription subscription;
    @Mock private ReactiveAdapter<List<String>> adapter;
    @Mock private View listView;
    @Mock private View emptyView;
    @Captor private ArgumentCaptor<OnRefreshListener> refreshListenerCaptor;

    private PullToRefreshController controller;
    private ConnectableObservable<List<String>> observable;

    @Before
    public void setUp() throws Exception {
        controller = new PullToRefreshController(wrapper);

        when(layout.findViewById(android.R.id.list)).thenReturn(listView);
        when(layout.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(wrapper.isAttached()).thenReturn(true);
        observable = Observable.just(singletonList("item")).replay();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIfRefreshListenerNotSetWhenOnViewCreatedIsCalled() {
        controller.onViewCreated(fragment, layout, bundle);
    }

    @Test
    public void shouldAttachPullToRefreshWrapperWithCustomListenerIfSet() {
        controller.setRefreshListener(listener);
        controller.onViewCreated(fragment, layout, bundle);

        verify(wrapper).attach(same(listener), same(layout), same(listView), same(emptyView));
    }

    @Test
    public void shouldAttachPullToRefreshWrapperWithInternalRefreshListenerIfOwnerIsRefreshable() {
        controller.setRefreshListener(fragment, mock(PagingListItemAdapter.class));
        controller.onViewCreated(fragment, layout, bundle);

        verify(wrapper).attach(isA(OnRefreshListener.class), same(layout), same(listView), same(emptyView));
    }

    @Test
    public void shouldForwardIsAttachedToPTRWrapper() {
        controller.isAttached();
        verify(wrapper).isAttached();
    }

    @Test
    public void shouldForwardRefreshStartedToPTRWrapper() {
        controller.startRefreshing();
        verify(wrapper).setRefreshing(true);
    }

    @Test
    public void shouldForwardRefreshCompletedToPTRWrapper() {
        controller.stopRefreshing();
        verify(wrapper).setRefreshing(false);
    }

    @Test
    public void shouldNotForwardRefreshStartedToPTRWrapperIfNotAttached() {
        when(wrapper.isAttached()).thenReturn(false);
        controller.startRefreshing();
        verify(wrapper, never()).setRefreshing(true);
    }

    @Test
    public void shouldNotForwardRefreshCompletedToPTRWrapperIfNotAttached() {
        when(wrapper.isAttached()).thenReturn(false);
        controller.stopRefreshing();
        verify(wrapper, never()).setRefreshing(false);
    }

    @Test
    public void shouldDetachFromPTRWrapperWhenViewsGetDestroyed() {
        controller.setRefreshListener(listener);
        controller.onViewCreated(fragment, layout, bundle);
        controller.onDestroyView(fragment);

        verify(wrapper).detach();
    }

    @Test
    public void shouldRestoreRefreshingStateWhenGoingThroughViewCreationDestructionCycle() {
        when(wrapper.isRefreshing()).thenReturn(true);
        controller.setRefreshListener(listener);

        controller.onViewCreated(fragment, layout, bundle);
        controller.startRefreshing();
        controller.onDestroyView(fragment);
        controller.onViewCreated(fragment, layout, bundle);

        InOrder inOrder = inOrder(wrapper);
        inOrder.verify(wrapper).setRefreshing(false); // onViewCreated 1
        inOrder.verify(wrapper).setRefreshing(true); // startRefreshing
        inOrder.verify(wrapper).setRefreshing(true); // onViewCreated 2
    }

    @Test
    public void connectingReactiveFragmentShouldResubscribeIfRefreshWasInProgressAndViewsGetRecreated() {
        when(wrapper.isRefreshing()).thenReturn(true);
        controller.setRefreshListener(fragment, adapter);
        observable.connect();

        controller.onDestroyView(fragment);
        controller.onViewCreated(fragment, layout, bundle);
        controller.connect(observable, adapter);

        verify(adapter).onNext(singletonList("item"));
    }

    @Test
    public void connectingReactiveFragmentShouldNotResubscribeIfNoRefreshWasInProgressAndViewsGetRecreated() {
        when(wrapper.isRefreshing()).thenReturn(false);
        controller.setRefreshListener(fragment, mock(PagingListItemAdapter.class));

        controller.onDestroyView(fragment);
        controller.onViewCreated(fragment, layout, bundle);
        controller.connect(observable, adapter);

        verifyZeroInteractions(adapter);
    }

    @Test
    public void refreshingReactiveFragmentShouldFirstClearAdapterThenAddNewItemsAndStopRefreshing() {
        controller.setRefreshListener(fragment, adapter);

        triggerRefresh();

        InOrder inOrder = inOrder(adapter, wrapper);

        inOrder.verify(adapter).clear();
        inOrder.verify(adapter).onNext(singletonList("item"));
        inOrder.verify(wrapper).setRefreshing(false);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldUnsubscribeItselfAfterRefreshingSoThatSubsequentPagesDoNotTriggerRefreshLogic() {
        controller.setRefreshListener(fragment, adapter);

        LegacyPager<List<String>> pager = TestPager.pagerWithNextPage(Observable.just(singletonList("page2")));
        observable = pager.page(Observable.just(singletonList("page1"))).publish();
        triggerRefresh();

        // emit a subsequent page
        pager.next();
        // for any subsequent items, refresh behavior should not fire
        verify(adapter, never()).onNext(singletonList("page2"));
    }

    @Test
    public void refreshingReactiveFragmentShouldTellPTRToStopRefreshingOnError() {
        controller.setRefreshListener(fragment, mock(PagingListItemAdapter.class));
        observable = Observable.<List<String>>error(new Exception()).publish();
        triggerRefresh();
        verifyZeroInteractions(adapter);
        verify(wrapper, times(2)).setRefreshing(false);
    }

    private void triggerRefresh() {
        controller.onViewCreated(fragment, layout, bundle);
        verify(wrapper).attach(refreshListenerCaptor.capture(), refEq(layout), same(listView), same(emptyView));
        refreshListenerCaptor.getValue().onRefresh();
    }

    @SuppressLint("ValidFragment")
    private class RefreshableFragment extends Fragment
            implements RefreshableListComponent<ConnectableObservable<List<String>>> {

        @Override
        public ConnectableObservable<List<String>> refreshObservable() {
            return observable;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        }

        @Override
        public ConnectableObservable<List<String>> buildObservable() {
            return observable;
        }

        @Override
        public Subscription connectObservable(ConnectableObservable<List<String>> observable) {
            observable.connect();
            return subscription;
        }

        @Override
        public View getView() {
            return layout;
        }
    }
}
