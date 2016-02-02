package com.soundcloud.android.upgrade;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
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

import java.util.List;

public class OfflineOnboardingPresenterTest extends AndroidUnitTest {

    @Mock private Navigator navigator;
    @Mock private UpgradeProgressOperations upgradeProgressOperations;
    @Mock private LoadingButtonLayout setUpOfflineButton;
    @Mock private LoadingButtonLayout skipOfflineButton;

    private OfflineOnboardingPresenter presenter;

    @Before
    public void setUp() {
        presenter = new OfflineOnboardingPresenter(navigator, upgradeProgressOperations,
                setUpOfflineButton, skipOfflineButton);
    }

    @Test
    public void clickingSelectiveSyncOpensStreamIfAccountUpgradeAlreadyCompleted() {
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(Observable.<List<Urn>>empty());

        presenter.awaitAccountUpgrade();
        presenter.onSetupLaterClicked();

        verify(skipOfflineButton, never()).setWaiting();
        verify(navigator).openHome(any(Activity.class));
    }

    @Test
    public void clickingSelectiveSyncShowsProgressSpinnerIfAccountUpgradeOngoing() {
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(Observable.<List<Urn>>never());
        presenter.awaitAccountUpgrade();

        presenter.onSetupLaterClicked();

        verify(skipOfflineButton).setWaiting();
        verify(setUpOfflineButton).setEnabled(false);
    }

    @Test
    public void clickingSelectiveSyncAwaitsAccountUpgradeBeforeProceeding() {
        PublishSubject<List<Urn>> subject = PublishSubject.create();
        when(upgradeProgressOperations.awaitAccountUpgrade()).thenReturn(subject);
        when(skipOfflineButton.isWaiting()).thenReturn(true);

        presenter.awaitAccountUpgrade();
        presenter.onSetupLaterClicked();

        verify(navigator, never()).openStream(any(Activity.class), any(Screen.class));
        subject.onCompleted();

        verify(navigator).openHome(any(Activity.class));
    }
}
