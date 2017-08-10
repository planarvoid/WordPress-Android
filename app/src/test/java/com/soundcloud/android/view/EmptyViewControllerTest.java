package com.soundcloud.android.view;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.sync.SyncFailedException;
import com.soundcloud.android.utils.ConnectionHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import rx.Observable;
import rx.observables.ConnectableObservable;
import rx.subjects.PublishSubject;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

@RunWith(MockitoJUnitRunner.class)
public class EmptyViewControllerTest {

    private EmptyViewController controller;

    private Fragment fragment = new Fragment();
    private Bundle fragmentArgs = new Bundle();
    private ConnectableObservable observable;
    private PublishSubject subject;

    @Mock private View layout;
    @Mock private EmptyView emptyView;
    @Mock private ConnectionHelper connectionHelper;

    @Before
    public void setup() {
        controller = new EmptyViewController(connectionHelper);
        fragment.setArguments(fragmentArgs);
        when(layout.findViewById(android.R.id.empty)).thenReturn(emptyView);
        subject = PublishSubject.create();
        observable = subject.replay();
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
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
        controller.onViewCreated(fragment, layout, null);
        controller.connect(Observable.error(mock(SyncFailedException.class)));
        verify(emptyView).setStatus(EmptyView.Status.SERVER_ERROR);
    }

    @Test
    public void shouldUnsubscribeFromObservableInOnDestroyView() {
        controller.onViewCreated(fragment, layout, null);
        controller.connect(observable);
        controller.onDestroyView(fragment);

        assertThat(subject.hasObservers()).isFalse();
    }

    @Test
    public void shouldReleaseEmptyViewInOnDestroyView() {
        controller.onViewCreated(fragment, layout, null);
        controller.connect(observable);
        controller.onDestroyView(fragment);
        assertThat(controller.getEmptyView()).isNull();
    }
}
