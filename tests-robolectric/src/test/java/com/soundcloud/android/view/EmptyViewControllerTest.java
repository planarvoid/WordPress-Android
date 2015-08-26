package com.soundcloud.android.view;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.sync.SyncFailedException;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;
import rx.observables.ConnectableObservable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class EmptyViewControllerTest {

    private EmptyViewController controller;

    private Fragment fragment = new Fragment();
    private Bundle fragmentArgs = new Bundle();
    private ConnectableObservable observable;

    @Mock private View layout;
    @Mock private EmptyView emptyView;
    @Mock private Subscription subscription;
    @Mock private NetworkConnectionHelper networkConnectionHelper;

    @Before
    public void setup() {
        controller = new EmptyViewController(networkConnectionHelper);
        fragment.setArguments(fragmentArgs);
        when(layout.findViewById(android.R.id.empty)).thenReturn(emptyView);
        observable = TestObservables.emptyConnectableObservable(subscription);
    }

    @Test
    public void shouldSetupEmptyViewInWaitingStateInFirstCallToOnViewCreated() {
        controller.onViewCreated(fragment, layout, null);
        verify(emptyView).setStatus(EmptyView.Status.WAITING);
    }

    @Test
    public void shouldResetEmptyViewStateToIdleWhenControllerReceivesCompleteEvent() {
        controller.onViewCreated(fragment, layout, null);
        controller.connect(Observable.empty());
        verify(emptyView).setStatus(EmptyView.Status.OK);
    }

    @Test
    public void shouldSetEmptyViewStateToServerErrorWhenControllerReceivesErrorForApiRequestExceptionNotRelatedToNetwork() {
        controller.onViewCreated(fragment, layout, null);
        controller.connect(Observable.error(ApiRequestException.notFound(null, null)));
        verify(emptyView).setStatus(EmptyView.Status.SERVER_ERROR);
    }

    @Test
    public void shouldSetEmptyViewStateToConnectionErrorWhenControllerReceivesErrorForApiRequestExceptionRelatedToNetwork() {
        controller.onViewCreated(fragment, layout, null);
        controller.connect(Observable.error(ApiRequestException.networkError(null, null)));
        verify(emptyView).setStatus(EmptyView.Status.CONNECTION_ERROR);
    }

    @Test
    public void shouldSetEmptyViewStateToConnectionErrorWhenControllerReceivesErrorForSyncFailedExceptionWithNoConnection() {
        controller.onViewCreated(fragment, layout, null);
        controller.connect(Observable.error(mock(SyncFailedException.class)));
        verify(emptyView).setStatus(EmptyView.Status.CONNECTION_ERROR);
    }

    @Test
    public void shouldSetEmptyViewStateToConnectionErrorWhenControllerReceivesErrorForSyncFailedExceptionWithConnection() {
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);
        controller.onViewCreated(fragment, layout, null);
        controller.connect(Observable.error(mock(SyncFailedException.class)));
        verify(emptyView).setStatus(EmptyView.Status.SERVER_ERROR);
    }

    @Test
    public void shouldUnsubscribeFromObservableInOnDestroyView() {
        controller.onViewCreated(fragment, layout, null);
        controller.connect(observable);
        controller.onDestroyView(fragment);
        verify(subscription).unsubscribe();
    }

    @Test
    public void shouldReleaseEmptyViewInOnDestroyView() {
        controller.onViewCreated(fragment, layout, null);
        controller.connect(observable);
        controller.onDestroyView(fragment);
        expect(controller.getEmptyView()).toBeNull();
    }
}