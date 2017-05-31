package com.soundcloud.android.downgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.configuration.PendingPlanOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.configuration.PlanChangeOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.payments.UpsellContext;
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
import android.view.View;

import java.io.IOException;

public class GoOffboardingPresenterTest extends AndroidUnitTest {

    @Mock private Fragment fragment;
    @Mock private NavigationExecutor navigationExecutor;
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
        presenter = new GoOffboardingPresenter(navigationExecutor, pendingPlanOperations, planChangeOperations, view, eventBus);
    }

    @Test
    public void shouldSendImpressionEventForResubscribeButton() {
        presenter.trackResubscribeButtonImpression();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind())
                .isEqualTo(UpgradeFunnelEvent.Kind.RESUBSCRIBE_IMPRESSION.toString());
    }

    @Test
    public void clickingContinueOpensStreamIfDowngradeAlreadyCompleted() {
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(Observable.just(downgradeResult));

        presenter.onViewCreated(fragment, new View(context()), null);
        presenter.onContinueClicked();

        assertThat(view.isContinueButtonWaiting).isFalse();
        verify(navigationExecutor).openHomeAsRootScreen(any(Activity.class));
    }

    @Test
    public void clickingContinueShowsProgressSpinnerIfDowngradeOngoing() {
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(Observable.never());
        presenter.onViewCreated(fragment, new View(context()), null);

        presenter.onContinueClicked();

        assertThat(view.isContinueButtonWaiting).isTrue();
    }

    @Test
    public void clickingContinueAwaitsDowngradeBeforeProceeding() {
        PublishSubject<Object> subject = PublishSubject.create();
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(subject);

        presenter.onViewCreated(fragment, new View(context()), null);
        presenter.onContinueClicked();

        verify(navigationExecutor, never()).openStream(any(Activity.class), any(Screen.class));
        subject.onNext(downgradeResult);
        subject.onCompleted();

        verify(navigationExecutor).openHomeAsRootScreen(any(Activity.class));
    }

    @Test
    public void displayContinueRetryOnNetworkError() {
        final PublishSubject<Object> subject = PublishSubject.create();
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(subject);

        presenter.onViewCreated(fragment, new View(context()), null);
        presenter.onContinueClicked();

        assertThat(view.isContinueButtonWaiting).isTrue();
        subject.onError(new IOException());
        assertThat(view.isContinueButtonRetry).isTrue();
    }

    @Test
    public void displayContinueRetryOnNetworkErrorWhenErrorAlreadyOccurred() {
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(Observable.error(new IOException()));

        presenter.onViewCreated(fragment, new View(context()), null);
        presenter.onContinueClicked();

        assertThat(view.isContinueButtonWaiting).isFalse();
        assertThat(view.isContinueButtonRetry).isTrue();
    }

    @Test
    public void displayContinueRetryOnApiRequestExceptionForNetworkError() {
        final PublishSubject<Object> subject = PublishSubject.create();
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(subject);

        presenter.onViewCreated(fragment, new View(context()), null);
        presenter.onContinueClicked();

        assertThat(view.isContinueButtonWaiting).isTrue();
        subject.onError(ApiRequestException.networkError(null, new IOException()));
        assertThat(view.isContinueButtonRetry).isTrue();
    }

    @Test
    public void displayContinueRetryOnApiRequestExceptionForNetworkErrorAlreadyOccurred() {
        when(planChangeOperations.awaitAccountDowngrade())
                .thenReturn(Observable.error(ApiRequestException.networkError(null, new IOException())));

        presenter.onViewCreated(fragment, new View(context()), null);
        presenter.onContinueClicked();

        assertThat(view.isContinueButtonWaiting).isFalse();
        assertThat(view.isContinueButtonRetry).isTrue();
    }

    @Test
    public void clickingRetryOnNetworkErrorTogglesProgressSpinner() {
        when(planChangeOperations.awaitAccountDowngrade())
                .thenReturn(Observable.error(new IOException()), Observable.never());

        presenter.onViewCreated(fragment, new View(context()), null);
        presenter.onContinueClicked();

        assertThat(view.isContinueButtonWaiting).isFalse();
        assertThat(view.isContinueButtonRetry).isTrue();
        presenter.onContinueClicked();
        assertThat(view.isContinueButtonWaiting).isTrue();
    }

    @Test
    public void clickingResubscribeOpensUpgradeScreenIfDowngradeAlreadyCompleted() {
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(Observable.just(downgradeResult));

        presenter.onViewCreated(fragment, new View(context()), null);
        presenter.onResubscribeClicked();

        assertThat(view.isResubscribeButtonWaiting).isFalse();
        verify(navigationExecutor).openUpgradeOnMain(any(Activity.class), eq(UpsellContext.DEFAULT));
        verify(activity).finish();
    }

    @Test
    public void clickingResubscribeSendsClickEvent() {
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(Observable.just(downgradeResult));

        presenter.onViewCreated(fragment, new View(context()), null);
        presenter.onResubscribeClicked();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind())
                .isEqualTo(UpgradeFunnelEvent.Kind.RESUBSCRIBE_CLICK.toString());
    }

    @Test
    public void clickingResubscribeShowsProgressSpinnerIfDowngradeOngoing() {
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(Observable.never());
        presenter.onViewCreated(fragment, new View(context()), null);

        presenter.onResubscribeClicked();

        assertThat(view.isResubscribeButtonWaiting).isTrue();
    }

    @Test
    public void clickingResubscribeAwaitsDowngradeBeforeProceeding() {
        PublishSubject<Object> subject = PublishSubject.create();
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(subject);

        presenter.onViewCreated(fragment, new View(context()), null);
        presenter.onResubscribeClicked();

        subject.onNext(downgradeResult);
        subject.onCompleted();

        verify(navigationExecutor).openUpgradeOnMain(any(Activity.class), eq(UpsellContext.DEFAULT));
    }

    @Test
    public void displayResubscribeRetryOnNetworkError() {
        final PublishSubject<Object> subject = PublishSubject.create();
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(subject);

        presenter.onViewCreated(fragment, new View(context()), null);
        presenter.onResubscribeClicked();

        assertThat(view.isResubscribeButtonWaiting).isTrue();
        subject.onError(new IOException());
        assertThat(view.isResubscribeButtonRetry).isTrue();
    }

    @Test
    public void displayResubscribeRetryOnNetworkErrorWhenErrorAlreadyOccurred() {
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(Observable.error(new IOException()));

        presenter.onViewCreated(fragment, new View(context()), null);
        presenter.onResubscribeClicked();

        assertThat(view.isResubscribeButtonWaiting).isFalse();
        assertThat(view.isResubscribeButtonRetry).isTrue();
    }

    @Test
    public void displayResubscribeRetryOnApiRequestExceptionForNetworkError() {
        final PublishSubject<Object> subject = PublishSubject.create();
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(subject);

        presenter.onViewCreated(fragment, new View(context()), null);
        presenter.onResubscribeClicked();

        assertThat(view.isResubscribeButtonWaiting).isTrue();
        subject.onError(ApiRequestException.networkError(null, new IOException()));
        assertThat(view.isResubscribeButtonRetry).isTrue();
    }

    @Test
    public void displayResubscribeRetryOnApiRequestExceptionForNetworkErrorAlreadyOccurred() {
        when(planChangeOperations.awaitAccountDowngrade())
                .thenReturn(Observable.error(ApiRequestException.networkError(null, new IOException())));

        presenter.onViewCreated(fragment, new View(context()), null);
        presenter.onResubscribeClicked();

        assertThat(view.isResubscribeButtonWaiting).isFalse();
        assertThat(view.isResubscribeButtonRetry).isTrue();
    }

    @Test
    public void clickOnResubscribeRetryShouldOpenUpgradeOnSuccess() {
        final PublishSubject<Object> error = PublishSubject.create();
        final PublishSubject<Object> success = PublishSubject.create();
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(error, success);

        presenter.onViewCreated(fragment, new View(context()), null);

        presenter.onResubscribeClicked();
        error.onError(new IOException());

        presenter.onResubscribeClicked();
        success.onNext(downgradeResult);
        success.onCompleted();

        verify(navigationExecutor).openUpgradeOnMain(any(Activity.class), eq(UpsellContext.DEFAULT));
    }

    @Test
    public void clickOnResubscribeRetryShouldDisplayRetryOnNetworkError() {
        final PublishSubject<Object> error1 = PublishSubject.create();
        final PublishSubject<Object> error2 = PublishSubject.create();
        when(planChangeOperations.awaitAccountDowngrade()).thenReturn(error1, error2);

        presenter.onViewCreated(fragment, new View(context()), null);

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

        presenter.onViewCreated(fragment, new View(context()), null);
        presenter.onResubscribeClicked();

        assertThat(view.isErrorDialogShown).isTrue();
    }

    private static class GoOffboardingViewStub extends GoOffboardingView {
        private boolean isResubscribeButtonWaiting;
        private boolean isResubscribeButtonRetry;
        private boolean isContinueButtonWaiting;
        private boolean isContinueButtonRetry;
        private boolean isErrorDialogShown;

        GoOffboardingViewStub() {
            super();
        }

        @Override
        void bind(Activity activity, GoOffboardingPresenter presenter, Plan plan) {
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
