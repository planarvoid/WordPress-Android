package com.soundcloud.android.view;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.rx.TestObservables.MockConnectableObservable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Subscription;
import rx.observables.ConnectableObservable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class EmptyViewControllerTest {

    private EmptyViewController controller = new EmptyViewController();

    private Fragment fragment = new Fragment();
    private Bundle fragmentArgs = new Bundle();
    private MockConnectableObservable observable;

    @Mock
    private View layout;
    @Mock
    private EmptyListView emptyView;
    @Mock
    private ReactiveComponent reactiveComponent;
    @Mock
    private Subscription subscription;
    @Captor
    private ArgumentCaptor<EmptyListView.RetryListener> retryListenerCaptor;

    @Before
    public void setup() {
        fragment.setArguments(fragmentArgs);
        when(layout.findViewById(android.R.id.empty)).thenReturn(emptyView);
        observable = TestObservables.emptyConnectableObservable(subscription);
        when(reactiveComponent.buildObservable()).thenReturn(observable);
    }

    @Test
    public void shouldResetEmptyViewStateToIdleWhenControllerReceivesCompleteEvent() {
        controller.onViewCreated(reactiveComponent, observable, layout);
        verify(emptyView).setStatus(EmptyListView.Status.OK);
    }

    @Test
    public void shouldSetupEmptyViewInWaitingStateInFirstCallToOnViewCreated() {
        controller.onViewCreated(reactiveComponent, observable, layout);
        verify(emptyView).setStatus(EmptyListView.Status.WAITING);
    }

    @Test
    public void shouldRestoreEmptyViewInWaitingStateInSubsequentCallsToOnViewCreated() {
        controller.onViewCreated(reactiveComponent, observable, layout);
        controller.onViewCreated(reactiveComponent, observable, layout);
        InOrder inOrder = inOrder(emptyView);
        inOrder.verify(emptyView).setStatus(EmptyListView.Status.WAITING);
        inOrder.verify(emptyView, times(2)).setStatus(EmptyListView.Status.OK);
    }

    @Test
    public void retryListenerShouldSetEmptyViewStateToWaitingWhenFired() {
        controller.onViewCreated(reactiveComponent, observable, layout);

        ArgumentCaptor<EmptyListView.RetryListener> listenerCaptor = ArgumentCaptor.forClass(EmptyListView.RetryListener.class);
        verify(emptyView).setOnRetryListener(listenerCaptor.capture());
        listenerCaptor.getValue().onEmptyViewRetry();

        verify(emptyView, times(2)).setStatus(EmptyListView.Status.WAITING);
    }

    @Test
    public void retryListenerShouldRebuildObservable() {
        controller.onViewCreated(reactiveComponent, observable, layout);
        verify(emptyView).setOnRetryListener(retryListenerCaptor.capture());

        retryListenerCaptor.getValue().onEmptyViewRetry();

        verify(reactiveComponent).buildObservable();
    }

    @Test
    public void retryListenerShouldTriggerConnectOnNewObservable() {
        ConnectableObservable retryObservable = TestObservables.endlessConnectableObservable();
        when(reactiveComponent.buildObservable()).thenReturn(retryObservable);
        controller.onViewCreated(reactiveComponent, observable, layout);
        verify(emptyView).setOnRetryListener(retryListenerCaptor.capture());

        retryListenerCaptor.getValue().onEmptyViewRetry();

        verify(reactiveComponent).connectObservable(retryObservable);
    }

    @Test
    public void retryListenerShouldSubscribeControllerToNewObservable() {
        controller.onViewCreated(reactiveComponent, observable, layout);
        verify(emptyView).setOnRetryListener(retryListenerCaptor.capture());

        retryListenerCaptor.getValue().onEmptyViewRetry();

        expect(observable.subscribers()).toNumber(2);
    }

    @Test
    public void shouldUnsubscribeFromObservableInOnDestroyView() {
        controller.onViewCreated(reactiveComponent, observable, layout);
        controller.onDestroyView();
        verify(subscription).unsubscribe();
    }

    @Test
    public void shouldReleaseEmptyViewInOnDestroyView() {
        controller.onViewCreated(reactiveComponent, observable, layout);
        controller.onDestroyView();
        expect(controller.getEmptyView()).toBeNull();
    }
}