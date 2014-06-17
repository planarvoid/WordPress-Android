package com.soundcloud.android.actionbar;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.rx.RxTestHelper.singlePage;
import static com.soundcloud.android.rx.TestObservables.MockConnectableObservable;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static rx.android.OperatorPaged.Page;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.view.RefreshableListComponent;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Subscription;
import rx.android.OperatorPaged;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

import android.os.Parcelable;
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

    @Mock
    private FragmentActivity activity;
    @Mock
    private OnRefreshListener listener;
    @Mock
    private PullToRefreshWrapper wrapper;
    @Mock
    private PullToRefreshLayout layout;
    @Mock
    private Subscription subscription;
    @Mock
    private PagingItemAdapter<Parcelable> adapter;
    @Captor
    private ArgumentCaptor<OnRefreshListener> refreshListenerCaptor;

    private PullToRefreshController controller;
    private MockConnectableObservable<Page<List<Parcelable>>> observable;

    @Before
    public void setUp() throws Exception {
        controller = new PullToRefreshController(eventBus, wrapper);
        Robolectric.shadowOf(fragment).setActivity(activity);
        when(layout.findViewById(R.id.ptr_layout)).thenReturn(layout);
        observable = TestObservables.emptyConnectableObservable(subscription);
    }

    @Test
    public void shouldAttachPullToRefreshWrapperWhenViewsAreCreated() {
        expect(controller.isAttached()).toBeFalse();
        controller.onViewCreated(fragment, listener);

        verify(wrapper).attach(activity, layout, listener);
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
    public void shouldUnsubscribeFromEventsWhenViewsGetDestroyed() {
        controller.onViewCreated(fragment, listener);
        controller.onDestroyView();
        eventBus.verifyUnsubscribed();
    }

    @Test
    public void shouldDetachFromPTRWrapperWhenViewsGetDestroyed() {
        controller.onViewCreated(fragment, listener);
        controller.onDestroyView();

        verify(wrapper).detach();
    }

    @Test
    public void shouldRestoreRefreshingStateWhenGoingThroughViewCreationDestructionCycle() {
        when(wrapper.isRefreshing()).thenReturn(true);
        controller.onViewCreated(fragment, listener);
        controller.startRefreshing();
        controller.onDestroyView();
        controller.onViewCreated(fragment, listener);

        InOrder inOrder = inOrder(wrapper);
        inOrder.verify(wrapper).setRefreshing(false); // onViewCreated 1
        inOrder.verify(wrapper).setRefreshing(true); // startRefreshing
        inOrder.verify(wrapper).setRefreshing(true); // onViewCreated 2
    }

    @Test
    public void shouldStopRefreshingWhenPlayerExpandedEventIsReceived() {
        controller.onViewCreated(fragment, listener);

        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(wrapper, times(2)).setRefreshing(false);
    }

    @Test
    public void shouldRegisterRefreshListenerForPagedListViews() {
        controller.onViewCreated(fragment, observable, adapter);

        verify(wrapper).attach(refEq(activity), refEq(layout), refreshListenerCaptor.capture());
        refreshListenerCaptor.getValue().onRefreshStarted(layout);

        expect(observable.subscribedTo()).toBeTrue();
        expect(observable.connected()).toBeTrue();
    }

    @Test
    public void shouldResubscribePageSubscriberIfRefreshWasInProgressAndViewsGetRecreated() {
        moveToRefreshingState();
        expect(observable.subscribedTo()).toBeTrue();
    }

    @Test
    public void pageSubscriberShouldClearAdapterAndAddPageInOnCompleted() {
        final Page<List<Parcelable>> page = singlePage(Arrays.asList(mock(Parcelable.class)));
        observable = TestObservables.connectableObservable(page);
        moveToRefreshingState();
        verify(adapter).clear();
        verify(adapter).onNext(page);
        verify(adapter).onCompleted();
        verifyNoMoreInteractions(adapter);
    }

    @Test
    public void pageSubscriberShouldNotClearAdapterWhenPageIsBlank() {
        final Page<List<Parcelable>> page = OperatorPaged.emptyPage();
        observable = TestObservables.connectableObservable(page);
        moveToRefreshingState();
        verify(adapter).onCompleted();
        verifyNoMoreInteractions(adapter);
    }

    @Test
    public void pageSubscriberShouldStopRefreshWhenComplete() {
        moveToRefreshingState();
        verify(wrapper, times(2)).setRefreshing(false);
    }

    @Test
    public void pageSubscriberShouldStopRefreshWhenFailed() {
        observable = TestObservables.errorConnectableObservable();
        moveToRefreshingState();
        verifyZeroInteractions(adapter);
        verify(wrapper, times(2)).setRefreshing(false);
    }

    private void moveToRefreshingState() {
        when(wrapper.isRefreshing()).thenReturn(true);
        controller.onViewCreated(fragment, observable, adapter);
        expect(observable.subscribedTo()).toBeFalse();
        controller.onDestroyView();
        controller.onViewCreated(fragment, observable, adapter);
    }

    private class RefreshableFragment extends Fragment
            implements RefreshableListComponent<MockConnectableObservable<Page<List<Parcelable>>>> {

        @Override
        public MockConnectableObservable<Page<List<Parcelable>>> refreshObservable() {
            return observable;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        }

        @Override
        public MockConnectableObservable<Page<List<Parcelable>>> buildObservable() {
            return observable;
        }

        @Override
        public Subscription connectObservable(MockConnectableObservable<Page<List<Parcelable>>> observable) {
            observable.connect();
            return subscription;
        }

        @Override
        public View getView() {
            return layout;
        }
    }
}