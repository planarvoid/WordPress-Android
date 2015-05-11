package com.soundcloud.android.presentation;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.lightcycle.SupportFragmentLightCycle;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.SyncFailedException;
import com.soundcloud.android.view.EmptyView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.support.v4.app.Fragment;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class EmptyViewPresenterTest {

    private boolean onRetryCalled;
    private EmptyViewPresenter presenter = new EmptyViewPresenter() {

        @Override
        protected void onRetry() {
            onRetryCalled = true;
        }
    };

    @Mock private Fragment fragment;
    @Mock private View layout;
    @Mock private EmptyView emptyView;
    @Captor private ArgumentCaptor<EmptyView.RetryListener> retryListenerCaptor;

    @Before
    public void setup() {
        when(layout.findViewById(android.R.id.empty)).thenReturn(emptyView);
    }

    @Test
    public void shouldAttachEmptyViewInOnViewCreated() {
        presenter.onViewCreated(fragment, layout, null);
        expect(presenter.getEmptyView()).not.toBeNull();
    }

    @Test
    public void shouldSetupEmptyViewInWaitingStateInFirstCallToOnViewCreated() {
        presenter.onViewCreated(fragment, layout, null);
        verify(emptyView).setStatus(EmptyView.Status.WAITING);
    }

    @Test
    public void shouldResetEmptyViewStateToIdleWhenSubscriberReceivesCompleteEvent() {
        presenter.onViewCreated(fragment, layout, null);
        presenter.new EmptyViewSubscriber().onCompleted();
        verify(emptyView).setStatus(EmptyView.Status.OK);
    }

    @Test
    public void shouldSetEmptyViewStateToServerErrorWhenSubscriberReceivesErrorForApiRequestExceptionNotRelatedToNetwork() {
        presenter.onViewCreated(fragment, layout, null);
        presenter.new EmptyViewSubscriber().onError(ApiRequestException.notFound(null));
        verify(emptyView).setStatus(EmptyView.Status.SERVER_ERROR);
    }

    @Test
    public void shouldSetEmptyViewStateToConnectionErrorWhenSubscriberReceivesErrorForApiRequestExceptionRelatedToNetwork() {
        presenter.onViewCreated(fragment, layout, null);
        presenter.new EmptyViewSubscriber().onError(ApiRequestException.networkError(null, null));
        verify(emptyView).setStatus(EmptyView.Status.CONNECTION_ERROR);
    }

    @Test
    public void shouldSetEmptyViewStateToConnectionErrorWhenSubscriberReceivesErrorForSyncFailedException() {
        presenter.onViewCreated(fragment, layout, null);
        presenter.new EmptyViewSubscriber().onError(mock(SyncFailedException.class));
        verify(emptyView).setStatus(EmptyView.Status.CONNECTION_ERROR);
    }

    @Test
    public void shouldRestoreEmptyViewInWaitingStateWhenGoingThroughViewCreationCycle() {
        presenter.onViewCreated(fragment, layout, null);
        presenter.new EmptyViewSubscriber().onError(ApiRequestException.notFound(null));
        presenter.onDestroyView(fragment);
        presenter.onViewCreated(fragment, layout, null);
        verify(emptyView, times(2)).setStatus(EmptyView.Status.SERVER_ERROR);
    }

    @Test
    public void retryListenerShouldSetEmptyViewStateToWaitingWhenFired() {
        presenter.onViewCreated(fragment, layout, null);

        ArgumentCaptor<EmptyView.RetryListener> listenerCaptor = ArgumentCaptor.forClass(EmptyView.RetryListener.class);
        verify(emptyView).setOnRetryListener(listenerCaptor.capture());

        Mockito.reset(emptyView);
        listenerCaptor.getValue().onEmptyViewRetry();

        verify(emptyView).setStatus(EmptyView.Status.WAITING);
    }

    @Test
    public void retryListenerShouldForwardToOnRetry() {
        presenter.onViewCreated(fragment, layout, null);
        verify(emptyView).setOnRetryListener(retryListenerCaptor.capture());

        retryListenerCaptor.getValue().onEmptyViewRetry();

        expect(onRetryCalled).toBeTrue();
    }

    @Test
    public void shouldReleaseEmptyViewInOnDestroyView() {
        presenter.onViewCreated(fragment, layout, null);
        presenter.onDestroyView(fragment);
        expect(presenter.getEmptyView()).toBeNull();
    }

    @Test
    public void shouldDispatchOnViewCreatedToLightCycleComponents() {
        SupportFragmentLightCycle lightCycle = mock(SupportFragmentLightCycle.class);
        presenter.bind(lightCycle);

        presenter.onViewCreated(fragment, layout, null);

        verify(lightCycle).onViewCreated(fragment, layout, null);
    }

    @Test
    public void shouldDispatchOnDestroyViewToLightCycleComponents() {
        SupportFragmentLightCycle lightCycle = mock(SupportFragmentLightCycle.class);
        presenter.bind(lightCycle);

        presenter.onDestroyView(fragment);

        verify(lightCycle).onDestroyView(fragment);
    }
}