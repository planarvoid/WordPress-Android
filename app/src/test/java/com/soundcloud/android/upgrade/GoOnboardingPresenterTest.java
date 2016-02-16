package com.soundcloud.android.upgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
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
import java.util.Collections;
import java.util.List;

public class GoOnboardingPresenterTest extends AndroidUnitTest {

    @Mock private AppCompatActivity activity;
    @Mock private Navigator navigator;
    @Mock private UpgradeProgressOperations upgradeProgressOperations;

    private TestEventBus eventBus = new TestEventBus();
    private GoOnboardingViewStub view;
    private List<Urn> policiesUpdate = Collections.singletonList(Urn.forTrack(123));
    private GoOnboardingPresenter presenter;

    @Before
    public void setUp() {
        view = new GoOnboardingViewStub();
        presenter = new GoOnboardingPresenter(navigator, upgradeProgressOperations, view, eventBus);
    }

    @Test
    public void clickingSetUpLaterOpensStreamIfAccountUpgradeAlreadyCompleted() {
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(Observable.just(policiesUpdate));

        presenter.onCreate(activity, null);
        presenter.onSetupLaterClicked();

        assertThat(view.isSetUpLaterButtonWaiting).isFalse();
        verify(navigator).openHomeAsRootScreen(any(Activity.class));
    }

    @Test
    public void clickingSetUpLaterSendsOnboardingDismissTrackingEventIfAccountUpgradeAlreadyCompleted() {
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(Observable.just(policiesUpdate));

        presenter.onCreate(activity, null);
        presenter.onSetupLaterClicked();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING, OfflineInteractionEvent.class).getKind())
                .isEqualTo(OfflineInteractionEvent.KIND_ONBOARDING_DISMISS);
    }

    @Test
    public void clickingSetUpLaterShowsProgressSpinnerIfAccountUpgradeOngoing() {
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(Observable.<List<Urn>>never());
        presenter.onCreate(activity, null);

        presenter.onSetupLaterClicked();

        assertThat(view.isSetUpLaterButtonWaiting).isTrue();
    }

    @Test
    public void clickingSetUpLaterAwaitsAccountUpgradeBeforeProceeding() {
        PublishSubject<List<Urn>> subject = PublishSubject.create();
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(subject);

        presenter.onCreate(activity, null);
        presenter.onSetupLaterClicked();

        verify(navigator, never()).openStream(any(Activity.class), any(Screen.class));
        subject.onNext(policiesUpdate);
        subject.onCompleted();

        verify(navigator).openHomeAsRootScreen(any(Activity.class));
    }

    @Test
    public void displaySetupLaterRetryOnNetworkError() {
        final PublishSubject<List<Urn>> subject = PublishSubject.create();
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(subject);

        presenter.onCreate(activity, null);
        presenter.onSetupLaterClicked();

        assertThat(view.isSetUpLaterButtonWaiting).isTrue();
        subject.onError(new IOException());
        assertThat(view.isSetUpLaterButtonRetry).isTrue();
    }

    @Test
    public void displaySetupLaterRetryOnNetworkErrorWhenErrorAlreadyOccurred() {
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(Observable.<List<Urn>>error(new IOException()));

        presenter.onCreate(activity, null);
        presenter.onSetupLaterClicked();

        assertThat(view.isSetUpLaterButtonWaiting).isFalse();
        assertThat(view.isSetUpLaterButtonRetry).isTrue();
    }

    @Test
    public void displaySetupLaterRetryOnApiRequestExceptionForNetworkError() {
        final PublishSubject<List<Urn>> subject = PublishSubject.create();
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(subject);

        presenter.onCreate(activity, null);
        presenter.onSetupLaterClicked();

        assertThat(view.isSetUpLaterButtonWaiting).isTrue();
        subject.onError(ApiRequestException.networkError(null, new IOException()));
        assertThat(view.isSetUpLaterButtonRetry).isTrue();
    }

    @Test
    public void displaySetupLaterRetryOnApiRequestExceptionForNetworkErrorAlreadyOccurred() {
        when(upgradeProgressOperations.awaitAccountUpgrade())
                .thenReturn(Observable.<List<Urn>>error(ApiRequestException.networkError(null, new IOException())));

        presenter.onCreate(activity, null);
        presenter.onSetupLaterClicked();

        assertThat(view.isSetUpLaterButtonWaiting).isFalse();
        assertThat(view.isSetUpLaterButtonRetry).isTrue();
    }

    @Test
    public void clickingSetupOfflineOpensOfflineContentOnboardingIfAccountUpgradeAlreadyCompleted() {
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(Observable.just(policiesUpdate));

        presenter.onCreate(activity, null);
        presenter.onSetupOfflineClicked();

        assertThat(view.isSetUpOfflineButtonWaiting).isFalse();
        verify(navigator).openOfflineContentOnboarding(any(Activity.class));
    }

    @Test
    public void clickingSetupOfflineSendsOnboardingStartTrackingEventIfAccountUpgradeAlreadyCompleted() {
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(Observable.just(policiesUpdate));

        presenter.onCreate(activity, null);
        presenter.onSetupOfflineClicked();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING, OfflineInteractionEvent.class).getKind())
                .isEqualTo(OfflineInteractionEvent.KIND_ONBOARDING_START);
    }

    @Test
    public void clickingSetupOfflineShowsProgressSpinnerIfAccountUpgradeOngoing() {
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(Observable.<List<Urn>>never());
        presenter.onCreate(activity, null);

        presenter.onSetupOfflineClicked();

        assertThat(view.isSetUpOfflineButtonWaiting).isTrue();
    }

    @Test
    public void clickingSetupOfflineAwaitsAccountUpgradeBeforeProceeding() {
        PublishSubject<List<Urn>> subject = PublishSubject.create();
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(subject);

        presenter.onCreate(activity, null);
        presenter.onSetupOfflineClicked();

        subject.onNext(policiesUpdate);
        subject.onCompleted();

        verify(navigator).openOfflineContentOnboarding(any(Activity.class));
    }

    @Test
    public void displaySetupOfflineRetryOnNetworkError() {
        final PublishSubject<List<Urn>> subject = PublishSubject.create();
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(subject);

        presenter.onCreate(activity, null);
        presenter.onSetupOfflineClicked();

        assertThat(view.isSetUpOfflineButtonWaiting).isTrue();
        subject.onError(new IOException());
        assertThat(view.isSetUpOfflineButtonRetry).isTrue();
    }

    @Test
    public void displaySetupOfflineRetryOnNetworkErrorWhenErrorAlreadyOccurred() {
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(Observable.<List<Urn>>error(new IOException()));

        presenter.onCreate(activity, null);
        presenter.onSetupOfflineClicked();

        assertThat(view.isSetUpOfflineButtonWaiting).isFalse();
        assertThat(view.isSetUpOfflineButtonRetry).isTrue();
    }

    @Test
    public void displaySetupOfflineRetryOnApiRequestExceptionForNetworkError() {
        final PublishSubject<List<Urn>> subject = PublishSubject.create();
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(subject);

        presenter.onCreate(activity, null);
        presenter.onSetupOfflineClicked();

        assertThat(view.isSetUpOfflineButtonWaiting).isTrue();
        subject.onError(ApiRequestException.networkError(null, new IOException()));
        assertThat(view.isSetUpOfflineButtonRetry).isTrue();
    }

    @Test
    public void displaySetupOfflineRetryOnApiRequestExceptionForNetworkErrorAlreadyOccurred() {
        when(upgradeProgressOperations.awaitAccountUpgrade())
                .thenReturn(Observable.<List<Urn>>error(ApiRequestException.networkError(null, new IOException())));

        presenter.onCreate(activity, null);
        presenter.onSetupOfflineClicked();

        assertThat(view.isSetUpOfflineButtonWaiting).isFalse();
        assertThat(view.isSetUpOfflineButtonRetry).isTrue();
    }

    @Test
    public void clickOnSetupOfflineRetryShouldOpenOfflineContentOnboardingOnSuccess() {
        final PublishSubject<List<Urn>> error = PublishSubject.create();
        final PublishSubject<List<Urn>> success = PublishSubject.create();
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(error, success);

        presenter.onCreate(activity, null);

        presenter.onSetupOfflineClicked();
        error.onError(new IOException());

        presenter.onSetupOfflineClicked();
        success.onNext(policiesUpdate);
        success.onCompleted();

        verify(navigator).openOfflineContentOnboarding(any(Activity.class));
    }

    @Test
    public void clickOnSetupOfflineRetryShouldDisplayRetryOnNetworkError() {
        final PublishSubject<List<Urn>> error1 = PublishSubject.create();
        final PublishSubject<List<Urn>> error2 = PublishSubject.create();
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(error1, error2);

        presenter.onCreate(activity, null);

        presenter.onSetupOfflineClicked();
        error1.onError(new IOException());

        presenter.onSetupOfflineClicked();
        error2.onError(new IOException());

        assertThat(view.isSetUpOfflineButtonRetry).isTrue();
    }

    @Test
    public void displayErrorDialogOnNonNetworkError() {
        when(upgradeProgressOperations.awaitAccountUpgrade())
                .thenReturn(Observable.<List<Urn>>error(ApiRequestException.malformedInput(null, new ApiMapperException("test"))));

        presenter.onCreate(activity, null);
        presenter.onSetupOfflineClicked();

        assertThat(view.isErrorDialogShown).isTrue();
    }

    @Test
    public void displayErrorDialogOnNoPlanChange() {
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(Observable.<List<Urn>>empty());

        presenter.onCreate(activity, null);
        presenter.onSetupOfflineClicked();

        assertThat(view.isErrorDialogShown).isTrue();
    }

    private static class GoOnboardingViewStub extends GoOnboardingView {
        private boolean isSetUpOfflineButtonWaiting;
        private boolean isSetUpOfflineButtonRetry;
        private boolean isSetUpLaterButtonWaiting;
        private boolean isSetUpLaterButtonRetry;
        private boolean isErrorDialogShown;

        @Override
        void bind(Activity activity, GoOnboardingPresenter presenter) {
            // no op
        }

        @Override
        void reset() {
            isSetUpOfflineButtonWaiting = false;
            isSetUpOfflineButtonRetry = false;
            isSetUpLaterButtonWaiting = false;
            isSetUpLaterButtonRetry = false;
            isErrorDialogShown = false;
        }

        @Override
        void setSetUpOfflineButtonWaiting() {
            isSetUpOfflineButtonWaiting = true;
            isSetUpOfflineButtonRetry = false;
        }

        @Override
        void setSetUpOfflineButtonRetry() {
            isSetUpOfflineButtonWaiting = false;
            isSetUpOfflineButtonRetry = true;
        }

        @Override
        void setSetUpLaterButtonWaiting() {
            isSetUpLaterButtonWaiting = true;
            isSetUpLaterButtonRetry = false;
        }

        @Override
        void setSetUpLaterButtonRetry() {
            isSetUpLaterButtonWaiting = false;
            isSetUpLaterButtonRetry = true;
        }

        @Override
        void showErrorDialog(FragmentManager fragmentManager) {
            isErrorDialogShown = true;
        }

    }
}
