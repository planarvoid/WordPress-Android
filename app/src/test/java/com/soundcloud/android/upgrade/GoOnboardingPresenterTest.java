package com.soundcloud.android.upgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.LoadingButtonLayout;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.app.Activity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;
import java.util.List;

public class GoOnboardingPresenterTest extends AndroidUnitTest {

    @Mock private AppCompatActivity activity;
    @Mock private Navigator navigator;
    @Mock private UpgradeProgressOperations upgradeProgressOperations;

    private GoOnboardingViewStub view;

    private GoOnboardingPresenter presenter;

    @Before
    public void setUp() {
        view = new GoOnboardingViewStub(mock(LoadingButtonLayout.class), mock(LoadingButtonLayout.class));
        presenter = new GoOnboardingPresenter(navigator, upgradeProgressOperations, view);
    }

    @Test
    public void clickingSetUpLaterOpensStreamIfAccountUpgradeAlreadyCompleted() {
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(Observable.<List<Urn>>empty());

        presenter.onCreate(activity, null);
        presenter.onSetupLaterClicked();

        assertThat(view.isSetUpLaterButtonWaiting).isFalse();
        verify(navigator).openHome(any(Activity.class));
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
        subject.onCompleted();

        verify(navigator).openHome(any(Activity.class));
    }

    @Test
    public void displayRetryOnNetworkError() {
        final PublishSubject<List<Urn>> subject = PublishSubject.create();
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(subject);

        presenter.onCreate(activity, null);
        presenter.onSetupLaterClicked();

        assertThat(view.isSetUpLaterButtonWaiting).isTrue();
        subject.onError(new IOException());
        assertThat(view.isSetUpLaterButtonRetry).isTrue();
    }

    @Test
    public void displayRetryOnNetworkErrorWhenErrorAlreadyOccurred() {
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(Observable.<List<Urn>>error(new IOException()));

        presenter.onCreate(activity, null);
        presenter.onSetupLaterClicked();

        assertThat(view.isSetUpLaterButtonWaiting).isFalse();
        assertThat(view.isSetUpLaterButtonRetry).isTrue();
    }

    @Test
    public void displayRetryOnApiRequestExceptionForNetworkError() {
        final PublishSubject<List<Urn>> subject = PublishSubject.create();
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(subject);

        presenter.onCreate(activity, null);
        presenter.onSetupLaterClicked();

        assertThat(view.isSetUpLaterButtonWaiting).isTrue();
        subject.onError(ApiRequestException.networkError(null, new IOException()));
        assertThat(view.isSetUpLaterButtonRetry).isTrue();
    }

    @Test
    public void displayRetryOnApiRequestExceptionForNetworkErrorAlreadyOccurred() {
        when(upgradeProgressOperations.awaitAccountUpgrade())
                .thenReturn(Observable.<List<Urn>>error(ApiRequestException.networkError(null, new IOException())));

        presenter.onCreate(activity, null);
        presenter.onSetupLaterClicked();

        assertThat(view.isSetUpLaterButtonWaiting).isFalse();
        assertThat(view.isSetUpLaterButtonRetry).isTrue();
    }

    @Test
    public void clickOnRetryShouldOpenHomeOnSuccess() {
        final PublishSubject<List<Urn>> error = PublishSubject.create();
        final PublishSubject<List<Urn>> success = PublishSubject.create();
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(error, success);

        presenter.onCreate(activity, null);

        presenter.onSetupLaterClicked();
        error.onError(new IOException());

        presenter.onSetupLaterClicked();
        success.onCompleted();

        verify(navigator).openHome(any(Activity.class));
    }

    @Test
    public void clickOnRetryShouldDisplayRetryOnNetworkError() {
        final PublishSubject<List<Urn>> error1 = PublishSubject.create();
        final PublishSubject<List<Urn>> error2 = PublishSubject.create();
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(error1, error2);

        presenter.onCreate(activity, null);

        presenter.onSetupLaterClicked();
        error1.onError(new IOException());

        presenter.onSetupLaterClicked();
        error2.onError(new IOException());

        assertThat(view.isSetUpLaterButtonRetry).isTrue();
    }

    @Test
    public void displayErrorDialogOnNonNetworkError() {
        when(upgradeProgressOperations.awaitAccountUpgrade())
                .thenReturn(Observable.<List<Urn>>error(ApiRequestException.malformedInput(null, new ApiMapperException("test"))));

        presenter.onCreate(activity, null);
        presenter.onSetupLaterClicked();

        assertThat(view.isErrorDialogShown()).isTrue();
    }

    private static class GoOnboardingViewStub extends GoOnboardingView {
        private final LoadingButtonLayout setupOfflineButton;
        private final LoadingButtonLayout setUpLaterButton;
        private boolean isSetUpOfflineButtonWaiting;
        private boolean isSetUpOfflineButtonRetry;
        private boolean isSetUpLaterButtonWaiting;
        private boolean isSetUpLaterButtonRetry;
        private boolean isErrorDialogShown;

        private GoOnboardingViewStub(LoadingButtonLayout setupOfflineButton, LoadingButtonLayout setUpLaterButton) {
            this.setupOfflineButton = setupOfflineButton;
            this.setUpLaterButton = setUpLaterButton;
        }

        @Override
        void bind(Activity activity, GoOnboardingPresenter presenter) {
            // no op
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

        public boolean isErrorDialogShown() {
            return isErrorDialogShown;
        }
    }
}
