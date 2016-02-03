package com.soundcloud.android.upgrade;

import android.app.Activity;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.LoadingButtonLayout;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import rx.Observable;
import rx.subjects.PublishSubject;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GoOnboardingPresenterTest extends AndroidUnitTest {

    @Mock private Navigator navigator;
    @Mock private UpgradeProgressOperations upgradeProgressOperations;
    @Mock private LoadingButtonLayout setUpOfflineButton;
    @Mock private LoadingButtonLayout setUpLaterButton;

    private GoOnboardingPresenter presenter;

    @Before
    public void setUp() {
        presenter = new GoOnboardingPresenter(navigator, upgradeProgressOperations,
                setUpOfflineButton, setUpLaterButton);
    }

    @Test
    public void clickingSetUpLaterOpensStreamIfAccountUpgradeAlreadyCompleted() {
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(Observable.<List<Urn>>empty());

        presenter.awaitAccountUpgrade();
        presenter.onSetupLaterClicked();

        verify(setUpLaterButton, never()).setWaiting();
        verify(navigator).openHome(any(Activity.class));
    }

    @Test
    public void clickingSetUpLaterShowsProgressSpinnerIfAccountUpgradeOngoing() {
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(Observable.<List<Urn>>never());
        presenter.awaitAccountUpgrade();

        presenter.onSetupLaterClicked();

        verify(setUpLaterButton).setWaiting();
        verify(setUpOfflineButton).setEnabled(false);
    }

    @Test
    public void clickingSetUpLaterAwaitsAccountUpgradeBeforeProceeding() {
        PublishSubject<List<Urn>> subject = PublishSubject.create();
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(subject);
        when(setUpLaterButton.isWaiting()).thenReturn(true);

        presenter.awaitAccountUpgrade();
        presenter.onSetupLaterClicked();

        verify(navigator, never()).openStream(any(Activity.class), any(Screen.class));
        subject.onCompleted();

        verify(navigator).openHome(any(Activity.class));
    }
}
