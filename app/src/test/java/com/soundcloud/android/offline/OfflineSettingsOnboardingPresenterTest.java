package com.soundcloud.android.offline;


import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.navigation.NavigationExecutor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OfflineSettingsOnboardingPresenterTest {

    @Mock private NavigationExecutor navigationExecutor;
    @Mock private OfflineSettingsStorage storage;

    private OfflineSettingsOnboardingPresenter presenter;

    @Before
    public void setUp() {
        presenter = new OfflineSettingsOnboardingPresenter(navigationExecutor, storage);
    }

    @Test
    public void onContinueSetsOnboardingAsSeen() {
        presenter.onContinue();
        verify(storage).setOfflineSettingsOnboardingSeen();
    }

    @Test
    public void onContinueOpensOfflineSettings() {
        presenter.onContinue();
        verify(navigationExecutor).openOfflineSettings(isNull());
    }

}
