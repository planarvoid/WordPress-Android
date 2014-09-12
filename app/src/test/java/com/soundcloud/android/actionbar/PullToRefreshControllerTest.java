package com.soundcloud.android.actionbar;

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

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.view.RefreshableListComponent;
import com.soundcloud.android.view.adapters.EndlessAdapter;
import com.soundcloud.android.view.adapters.ReactiveAdapter;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PullToRefreshControllerTest {

    private TestEventBus eventBus = new TestEventBus();
    private RefreshableFragment fragment = new RefreshableFragment();

    @Mock private FragmentActivity activity;
    @Mock private Bundle bundle;
    @Mock private OnRefreshListener listener;
    @Mock private PullToRefreshWrapper wrapper;
    @Mock private PullToRefreshLayout layout;
    @Mock private Subscription subscription;
    @Mock private ReactiveAdapter<List<String>> adapter;
    @Captor private ArgumentCaptor<OnRefreshListener> refreshListenerCaptor;

    private PullToRefreshController controller;
    private ConnectableObservable<List<String>> observable;

    @Before
    public void setUp() throws Exception {
        controller = new PullToRefreshController(eventBus, wrapper);
        controller.onBind(fragment);

        Robolectric.shadowOf(fragment).setActivity(activity);
        when(layout.findViewById(R.id.ptr_layout)).thenReturn(layout);
        when(wrapper.isAttached()).thenReturn(true);
        observable = TestObservables.emptyConnectableObservable(subscription);

        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowIfRefreshListenerNotSetWhenOnViewCreatedIsCalled() {
        controller.onViewCreated(layout, bundle);
    }

    @Test
    public void shouldAttachPullToRefreshWrapperWithCustomListenerIfSet() {
        when(wrapper.isAttached()).thenReturn(false);
        controller.setRefreshListener(listener);
        controller.onViewCreated(layout, bundle);

        verify(wrapper).attach(same(activity), same(layout), same(listener));
    }

    @Test
    public void shouldAttachPullToRefreshWrapperWithInternalRefreshListenerIfOwnerIsRefreshable() {
        when(wrapper.isAttached()).thenReturn(false);
        controller.setRefreshListener(fragment, mock(EndlessAdapter.class));
        controller.onViewCreated(layout, bundle);

        verify(wrapper).attach(same(activity), same(layout), isA(OnRefreshListener.class));
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
    public void shouldNotStartRefreshingIfPlayerIsExpanded() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
        controller.startRefreshing();
        verify(wrapper, never()).setRefreshing(true);
    }

    @Test
    public void shouldUnsubscribeFromEventsWhenViewsGetDestroyed() {
        controller.setRefreshListener(listener);
        controller.onViewCreated(layout, bundle);
        controller.onDestroyView();
        eventBus.verifyUnsubscribed();
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
        controller.onViewCreated(layout, bundle);
        controller.onDestroyView();

        verify(wrapper).detach();
    }

    @Test
    public void shouldRestoreRefreshingStateWhenGoingThroughViewCreationDestructionCycle() {
        when(wrapper.isRefreshing()).thenReturn(true);
        controller.setRefreshListener(listener);

        controller.onViewCreated(layout, bundle);
        controller.startRefreshing();
        controller.onDestroyView();
        controller.onViewCreated(layout, bundle);

        InOrder inOrder = inOrder(wrapper);
        inOrder.verify(wrapper).setRefreshing(false); // onViewCreated 1
        inOrder.verify(wrapper).setRefreshing(true); // startRefreshing
        inOrder.verify(wrapper).setRefreshing(true); // onViewCreated 2
    }

    @Test
    public void shouldStopRefreshingWhenPlayerExpandedEventIsReceived() {
        controller.setRefreshListener(listener);
        controller.onViewCreated(layout, bundle);

        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(wrapper, times(2)).setRefreshing(false);
    }

    @Test
    public void connectingReactiveFragmentShouldResubscribeIfRefreshWasInProgressAndViewsGetRecreated() {
        when(wrapper.isRefreshing()).thenReturn(true);
        controller.setRefreshListener(fragment, adapter);

        controller.onDestroyView();
        controller.onViewCreated(layout, bundle);
        controller.connect(observable, adapter);

        verify(adapter).onCompleted();
    }

    @Test
    public void connectingReactiveFragmentShouldNotResubscribeIfNoRefreshWasInProgressAndViewsGetRecreated() {
        when(wrapper.isRefreshing()).thenReturn(false);
        controller.setRefreshListener(fragment, mock(EndlessAdapter.class));

        controller.onDestroyView();
        controller.onViewCreated(layout, bundle);
        controller.connect(observable, adapter);

        verifyZeroInteractions(adapter);
    }

    @Test
    public void refreshingReactiveFragmentShouldFirstClearAdapterThenAddNewItem() {
        controller.setRefreshListener(fragment, adapter);
        observable = TestObservables.connectableObservable(Arrays.asList("item"));

        triggerRefresh();

        InOrder inOrder = inOrder(adapter);

        inOrder.verify(adapter).clear();
        inOrder.verify(adapter).onNext(Arrays.asList("item"));
        inOrder.verify(adapter).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void refreshingReactiveFragmentShouldTellPTRToStopRefreshingWhenComplete() {
        controller.setRefreshListener(fragment, mock(EndlessAdapter.class));
        triggerRefresh();
        verify(wrapper, times(2)).setRefreshing(false);
    }

    @Test
    public void refreshingReactiveFragmentShouldTellPTRToStopRefreshingOnError() {
        controller.setRefreshListener(fragment, mock(EndlessAdapter.class));
        observable = TestObservables.errorConnectableObservable();
        triggerRefresh();
        verifyZeroInteractions(adapter);
        verify(wrapper, times(2)).setRefreshing(false);
    }

    private void triggerRefresh() {
        controller.onViewCreated(layout, bundle);
        verify(wrapper).attach(refEq(activity), refEq(layout), refreshListenerCaptor.capture());
        refreshListenerCaptor.getValue().onRefreshStarted(layout);
    }

    private class RefreshableFragment extends Fragment
            implements RefreshableListComponent<ConnectableObservable<List<String>>> {

        @Override
        public ConnectableObservable<List<String>> refreshObservable() {
            return observable;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {}

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