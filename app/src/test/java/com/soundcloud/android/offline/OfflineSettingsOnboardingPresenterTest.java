package com.soundcloud.android.offline;


import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;

public class OfflineSettingsOnboardingPresenterTest extends AndroidUnitTest {

    @Mock private AppCompatActivity activity;
    @Mock private Navigator navigator;
    @Mock private OfflineSettingsStorage storage;

    private OfflineSettingsOnboardingPresenter presenter;

    @Before
    public void setUp() {
        presenter = new OfflineSettingsOnboardingPresenter(navigator, storage);
    }

    @Test
    public void onContinueSetsOnboardingAsSeen() {
        presenter.onContinue();

        verify(storage).setOfflineSettingsOnboardingSeen();
    }

    @Test
    public void onContinueOpensOfflineSettings() {
        presenter.onContinue();

        verify(navigator).openOfflineSettings(any(Activity.class));
    }

}
