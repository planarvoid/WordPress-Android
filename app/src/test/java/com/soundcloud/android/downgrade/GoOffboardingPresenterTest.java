package com.soundcloud.android.downgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.configuration.PendingPlanOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.configuration.PlanChangeOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;

public class GoOffboardingPresenterTest extends AndroidUnitTest {

    @Mock private Fragment fragment;
    @Mock private Navigator navigator;
    @Mock private PendingPlanOperations pendingPlanOperations;
    @Mock private PlanChangeOperations planChangeOperations;
    @Mock private AppCompatActivity activity;

    private TestEventBus eventBus = new TestEventBus();
    private GoOffboardingViewStub view;
    private Object downgradeResult = new Object();
    private GoOffboardingPresenter presenter;

    @Before
    public void setUp() {
        when(fragment.getActivity()).thenReturn(activity);
        when(fragment.getContext()).thenReturn(activity);
        when(pendingPlanOperations.getPendingUpgrade()).thenReturn(Plan.FREE_TIER);
        view = new GoOffboardingViewStub();
        presenter = new GoOffboardingPresenter(navigator, pendingPlanOperations, planChangeOperations, view, eventBus);
    }

    @Test
    public void shouldSendImpressionEventForResubscribeButton() {
        presenter.trackResubscribeButtonImpression();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind())
                .isEqualTo(UpgradeFunnelEvent.KIND_RESUBSCRIBE_IMPRESSION);
    }

    @Test
    public void clickingContinueOpensStreamIfDowngradeAlreadyCompleted() {
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(Observable.just(downgradeResult));

        presenter.onCreate(fragment, null);
        presenter.onContinueClicked();

        assertThat(view.isContinueButtonWaiting).isFalse();
        verify(navigator).openHomeAsRootScreen(any(Activity.class));
    }

    @Test
    public void clickingContinueShowsProgressSpinnerIfDowngradeOngoing() {
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(Observable.never());
        presenter.onCreate(fragment, null);

        presenter.onContinueClicked();

        assertThat(view.isContinueButtonWaiting).isTrue();
    }

    @Test
    public void clickingContinueAwaitsDowngradeBeforeProceeding() {
        PublishSubject<Object> subject = PublishSubject.create();
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(subject);

        presenter.onCreate(fragment, null);
        presenter.onContinueClicked();

        verify(navigator, never()).openStream(any(Activity.class), any(Screen.class));
        subject.onNext(downgradeResult);
        subject.onCompleted();

        verify(navigator).openHomeAsRootScreen(any(Activity.class));
    }

    @Test
    public void displayContinueRetryOnNetworkError() {
        final PublishSubject<Object> subject = PublishSubject.create();
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(subject);

        presenter.onCreate(fragment, null);
        presenter.onContinueClicked();

        assertThat(view.isContinueButtonWaiting).isTrue();
        subject.onError(new IOException());
        assertThat(view.isContinueButtonRetry).isTrue();
    }

    @Test
    public void displayContinueRetryOnNetworkErrorWhenErrorAlreadyOccurred() {
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(Observable.error(new IOException()));

        presenter.onCreate(fragment, null);
        presenter.onContinueClicked();

        assertThat(view.isContinueButtonWaiting).isFalse();
        assertThat(view.isContinueButtonRetry).isTrue();
    }

    @Test
    public void displayContinueRetryOnApiRequestExceptionForNetworkError() {
        final PublishSubject<Object> subject = PublishSubject.create();
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(subject);

        presenter.onCreate(fragment, null);
        presenter.onContinueClicked();

        assertThat(view.isContinueButtonWaiting).isTrue();
        subject.onError(ApiRequestException.networkError(null, new IOException()));
        assertThat(view.isContinueButtonRetry).isTrue();
    }

    @Test
    public void displayContinueRetryOnApiRequestExceptionForNetworkErrorAlreadyOccurred() {
        when(planChangeOperations.awaitAccountDowngrade())
                .thenReturn(Observable.error(ApiRequestException.networkError(null, new IOException())));

        presenter.onCreate(fragment, null);
        presenter.onContinueClicked();

        assertThat(view.isContinueButtonWaiting).isFalse();
        assertThat(view.isContinueButtonRetry).isTrue();
    }

    @Test
    public void clickingRetryOnNetworkErrorTogglesProgressSpinner() {
        when(planChangeOperations.awaitAccountDowngrade())
                .thenReturn(Observable.error(new IOException()), Observable.never());

        presenter.onCreate(fragment, null);
        presenter.onContinueClicked();

        assertThat(view.isContinueButtonWaiting).isFalse();
        assertThat(view.isContinueButtonRetry).isTrue();
        presenter.onContinueClicked();
        assertThat(view.isContinueButtonWaiting).isTrue();
    }

    @Test
    public void clickingResubscribeOpensUpgradeScreenIfDowngradeAlreadyCompleted() {
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(Observable.just(downgradeResult));

        presenter.onCreate(fragment, null);
        presenter.onResubscribeClicked();

        assertThat(view.isResubscribeButtonWaiting).isFalse();
        verify(navigator).openUpgradeOnMain(any(Activity.class));
        verify(activity).finish();
    }

    @Test
    public void clickingResubscribeSendsClickEvent() {
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(Observable.just(downgradeResult));

        presenter.onCreate(fragment, null);
        presenter.onResubscribeClicked();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind())
                .isEqualTo(UpgradeFunnelEvent.KIND_RESUBSCRIBE_CLICK);
    }

    @Test
    public void clickingResubscribeShowsProgressSpinnerIfDowngradeOngoing() {
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(Observable.never());
        presenter.onCreate(fragment, null);

        presenter.onResubscribeClicked();

        assertThat(view.isResubscribeButtonWaiting).isTrue();
    }

    @Test
    public void clickingResubscribeAwaitsDowngradeBeforeProceeding() {
        PublishSubject<Object> subject = PublishSubject.create();
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(subject);

        presenter.onCreate(fragment, null);
        presenter.onResubscribeClicked();

        subject.onNext(downgradeResult);
        subject.onCompleted();

        verify(navigator).openUpgradeOnMain(any(Activity.class));
    }

    @Test
    public void displayResubscribeRetryOnNetworkError() {
        final PublishSubject<Object> subject = PublishSubject.create();
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(subject);

        presenter.onCreate(fragment, null);
        presenter.onResubscribeClicked();

        assertThat(view.isResubscribeButtonWaiting).isTrue();
        subject.onError(new IOException());
        assertThat(view.isResubscribeButtonRetry).isTrue();
    }

    @Test
    public void displayResubscribeRetryOnNetworkErrorWhenErrorAlreadyOccurred() {
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(Observable.error(new IOException()));

        presenter.onCreate(fragment, null);
        presenter.onResubscribeClicked();

        assertThat(view.isResubscribeButtonWaiting).isFalse();
        assertThat(view.isResubscribeButtonRetry).isTrue();
    }

    @Test
    public void displayResubscribeRetryOnApiRequestExceptionForNetworkError() {
        final PublishSubject<Object> subject = PublishSubject.create();
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(subject);

        presenter.onCreate(fragment, null);
        presenter.onResubscribeClicked();

        assertThat(view.isResubscribeButtonWaiting).isTrue();
        subject.onError(ApiRequestException.networkError(null, new IOException()));
        assertThat(view.isResubscribeButtonRetry).isTrue();
    }

    @Test
    public void displayResubscribeRetryOnApiRequestExceptionForNetworkErrorAlreadyOccurred() {
        when(planChangeOperations.awaitAccountDowngrade())
                .thenReturn(Observable.error(ApiRequestException.networkError(null, new IOException())));

        presenter.onCreate(fragment, null);
        presenter.onResubscribeClicked();

        assertThat(view.isResubscribeButtonWaiting).isFalse();
        assertThat(view.isResubscribeButtonRetry).isTrue();
    }

    @Test
    public void clickOnResubscribeRetryShouldOpenUpgradeOnSuccess() {
        final PublishSubject<Object> error = PublishSubject.create();
        final PublishSubject<Object> success = PublishSubject.create();
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(error, success);

        presenter.onCreate(fragment, null);

        presenter.onResubscribeClicked();
        error.onError(new IOException());

        presenter.onResubscribeClicked();
        success.onNext(downgradeResult);
        success.onCompleted();

        verify(navigator).openUpgradeOnMain(any(Activity.class));
    }

    @Test
    public void clickOnResubscribeRetryShouldDisplayRetryOnNetworkError() {
        final PublishSubject<Object> error1 = PublishSubject.create();
        final PublishSubject<Object> error2 = PublishSubject.create();
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(error1, error2);

        presenter.onCreate(fragment, null);

        presenter.onResubscribeClicked();
        error1.onError(new IOException());

        presenter.onResubscribeClicked();
        error2.onError(new IOException());

        assertThat(view.isResubscribeButtonRetry).isTrue();
    }

    @Test
    public void displayErrorDialogOnNonNetworkError() {
        when(planChangeOperations.awaitAccountDowngrade())
                .thenReturn(Observable.error(ApiRequestException.malformedInput(null, new ApiMapperException("test"))));

        presenter.onCreate(fragment, null);
        presenter.onResubscribeClicked();

        assertThat(view.isErrorDialogShown).isTrue();
    }

    private static class GoOffboardingViewStub extends GoOffboardingView {
        private boolean isResubscribeButtonWaiting;
        private boolean isResubscribeButtonRetry;
        private boolean isContinueButtonWaiting;
        private boolean isContinueButtonRetry;
        private boolean isErrorDialogShown;

        @Override
        void bind(Activity activity, GoOffboardingPresenter presenter) {
            // no op
        }

        @Override
        void reset() {
            isResubscribeButtonWaiting = false;
            isResubscribeButtonRetry = false;
            isContinueButtonWaiting = false;
            isContinueButtonRetry = false;
            isErrorDialogShown = false;
        }

        @Override
        void setResubscribeButtonWaiting() {
            isResubscribeButtonWaiting = true;
            isResubscribeButtonRetry = false;
        }

        @Override
        void setResubscribeButtonRetry() {
            isResubscribeButtonWaiting = false;
            isResubscribeButtonRetry = true;
        }

        @Override
        void setContinueButtonWaiting() {
            isContinueButtonWaiting = true;
            isContinueButtonRetry = false;
        }

        @Override
        void setContinueButtonRetry() {
            isContinueButtonWaiting = false;
            isContinueButtonRetry = true;
        }

        @Override
        void showErrorDialog(FragmentManager fragmentManager) {
            isErrorDialogShown = true;
        }

    }
}
