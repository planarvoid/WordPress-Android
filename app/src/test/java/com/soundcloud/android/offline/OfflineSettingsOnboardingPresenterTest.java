package com.soundcloud.android.offline;


import static com.soundcloud.android.helpers.NavigationTargetMatcher.matchesNavigationTarget;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.support.v7.app.AppCompatActivity;

@RunWith(MockitoJUnitRunner.class)
public class OfflineSettingsOnboardingPresenterTest {

    @Mock private Navigator navigator;
    @Mock private OfflineSettingsStorage storage;
    @Mock private AppCompatActivity activity;

    private OfflineSettingsOnboardingPresenter presenter;

    @Before
    public void setUp() {
        presenter = new OfflineSettingsOnboardingPresenter(navigator, storage);
        presenter.onCreate(activity, null);
    }

    @Test
    public void onContinueSetsOnboardingAsSeen() {
        presenter.onContinue();
        verify(storage).setOfflineSettingsOnboardingSeen();
    }

    @Test
    public void onContinueOpensOfflineSettings() {
        presenter.onContinue();
        verify(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forOfflineSettings(false))));
    }

}
