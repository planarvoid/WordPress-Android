package com.soundcloud.android.upgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.configuration.PendingPlanOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.configuration.PlanChangeOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.images.BackgroundDecoder;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.app.Activity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;

public class GoOnboardingPresenterTest extends AndroidUnitTest {

    @Mock private AppCompatActivity activity;
    @Mock private Navigator navigator;
    @Mock private PendingPlanOperations pendingPlanOperations;
    @Mock private PlanChangeOperations planChangeOperations;

    private TestEventBus eventBus = new TestEventBus();
    private GoOnboardingViewStub view;
    private Object accountUpgradeSignal = new Object();
    private GoOnboardingPresenter presenter;

    @Before
    public void setUp() {
        when(pendingPlanOperations.getPendingUpgrade()).thenReturn(Plan.MID_TIER);
        view = new GoOnboardingViewStub();
        presenter = new GoOnboardingPresenter(navigator, pendingPlanOperations, planChangeOperations, view, eventBus);
    }

    @Test
    public void clickingStartOpensOfflineContentOnboardingIfAccountUpgradeAlreadyCompleted() {
        when(planChangeOperations.awaitAccountUpgrade()).thenReturn(Observable.just(accountUpgradeSignal));

        presenter.onCreate(activity, null);
        presenter.onStartClicked();

        assertThat(view.isStartButtonWaiting).isFalse();
        verify(navigator).openCollectionAsRootScreen(any(Activity.class));
    }

    @Test
    public void clickingStartSendsOnboardingStartTrackingEventIfAccountUpgradeAlreadyCompleted() {
        when(planChangeOperations.awaitAccountUpgrade()).thenReturn(Observable.just(accountUpgradeSignal));

        presenter.onCreate(activity, null);
        presenter.onStartClicked();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING, OfflineInteractionEvent.class).clickName().get())
                .isEqualTo(OfflineInteractionEvent.Kind.KIND_ONBOARDING_START);
    }

    @Test
    public void clickingStartShowsProgressSpinnerIfAccountUpgradeOngoing() {
        when(planChangeOperations.awaitAccountUpgrade()).thenReturn(Observable.never());
        presenter.onCreate(activity, null);

        presenter.onStartClicked();

        assertThat(view.isStartButtonWaiting).isTrue();
    }

    @Test
    public void clickingStartAwaitsAccountUpgradeBeforeProceeding() {
        PublishSubject<Object> subject = PublishSubject.create();
        when(planChangeOperations.awaitAccountUpgrade()).thenReturn(subject);

        presenter.onCreate(activity, null);
        presenter.onStartClicked();

        subject.onNext(accountUpgradeSignal);
        subject.onCompleted();

        verify(navigator).openCollectionAsRootScreen(any(Activity.class));
    }

    @Test
    public void displayStartRetryOnNetworkError() {
        final PublishSubject<Object> subject = PublishSubject.create();
        when(planChangeOperations.awaitAccountUpgrade()).thenReturn(subject);

        presenter.onCreate(activity, null);
        presenter.onStartClicked();

        assertThat(view.isStartButtonWaiting).isTrue();
        subject.onError(new IOException());
        assertThat(view.isStartButtonRetry).isTrue();
    }

    @Test
    public void displayStartRetryOnNetworkErrorWhenErrorAlreadyOccurred() {
        when(planChangeOperations.awaitAccountUpgrade()).thenReturn(Observable.error(new IOException()));

        presenter.onCreate(activity, null);
        presenter.onStartClicked();

        assertThat(view.isStartButtonWaiting).isFalse();
        assertThat(view.isStartButtonRetry).isTrue();
    }

    @Test
    public void displayStartRetryOnApiRequestExceptionForNetworkError() {
        final PublishSubject<Object> subject = PublishSubject.create();
        when(planChangeOperations.awaitAccountUpgrade()).thenReturn(subject);

        presenter.onCreate(activity, null);
        presenter.onStartClicked();

        assertThat(view.isStartButtonWaiting).isTrue();
        subject.onError(ApiRequestException.networkError(null, new IOException()));
        assertThat(view.isStartButtonRetry).isTrue();
    }

    @Test
    public void displayStartRetryOnApiRequestExceptionForNetworkErrorAlreadyOccurred() {
        when(planChangeOperations.awaitAccountUpgrade())
                .thenReturn(Observable.error(ApiRequestException.networkError(null, new IOException())));

        presenter.onCreate(activity, null);
        presenter.onStartClicked();

        assertThat(view.isStartButtonWaiting).isFalse();
        assertThat(view.isStartButtonRetry).isTrue();
    }

    @Test
    public void clickOnStartRetryShouldOpenOfflineContentOnboardingOnSuccess() {
        final PublishSubject<Object> error = PublishSubject.create();
        final PublishSubject<Object> success = PublishSubject.create();
        when(planChangeOperations.awaitAccountUpgrade()).thenReturn(error, success);

        presenter.onCreate(activity, null);

        presenter.onStartClicked();
        error.onError(new IOException());

        presenter.onStartClicked();
        success.onNext(accountUpgradeSignal);
        success.onCompleted();

        verify(navigator).openCollectionAsRootScreen(any(Activity.class));
    }

    @Test
    public void clickOnStartRetryShouldDisplayRetryOnNetworkError() {
        final PublishSubject<Object> error1 = PublishSubject.create();
        final PublishSubject<Object> error2 = PublishSubject.create();
        when(planChangeOperations.awaitAccountUpgrade()).thenReturn(error1, error2);

        presenter.onCreate(activity, null);

        presenter.onStartClicked();
        error1.onError(new IOException());

        presenter.onStartClicked();
        error2.onError(new IOException());

        assertThat(view.isStartButtonRetry).isTrue();
    }

    @Test
    public void clickingRetryOnNetworkErrorTogglesProgressSpinner() {
        when(planChangeOperations.awaitAccountUpgrade())
                .thenReturn(Observable.error(new IOException()), Observable.never());

        presenter.onCreate(activity, null);
        presenter.onStartClicked();

        assertThat(view.isStartButtonWaiting).isFalse();
        assertThat(view.isStartButtonRetry).isTrue();
        presenter.onStartClicked();
        assertThat(view.isStartButtonWaiting).isTrue();
    }

    @Test
    public void displayErrorDialogOnNonNetworkError() {
        when(planChangeOperations.awaitAccountUpgrade())
                .thenReturn(Observable.error(ApiRequestException.malformedInput(null, new ApiMapperException("test"))));

        presenter.onCreate(activity, null);
        presenter.onStartClicked();

        assertThat(view.isErrorDialogShown).isTrue();
    }

    @Test
    public void displayErrorDialogOnNoPlanChange() {
        when(planChangeOperations.awaitAccountUpgrade()).thenReturn(Observable.empty());

        presenter.onCreate(activity, null);
        presenter.onStartClicked();

        assertThat(view.isErrorDialogShown).isTrue();
    }

    private static class GoOnboardingViewStub extends GoOnboardingView {
        private boolean isStartButtonWaiting;
        private boolean isStartButtonRetry;
        private boolean isErrorDialogShown;

        private GoOnboardingViewStub() {
            super(new GoOnboardingAdapter(), mock(BackgroundDecoder.class));
        }

        @Override
        void bind(Activity activity, Listener listener, Plan plan) {
            // no op
        }

        @Override
        void reset() {
            isStartButtonWaiting = false;
            isStartButtonRetry = false;
            isErrorDialogShown = false;
        }

        @Override
        void setStartButtonWaiting() {
            isStartButtonWaiting = true;
            isStartButtonRetry = false;
        }

        @Override
        void setStartButtonRetry() {
            isStartButtonWaiting = false;
            isStartButtonRetry = true;
        }

        @Override
        void showErrorDialog(FragmentManager fragmentManager) {
            isErrorDialogShown = true;
        }

    }
}
