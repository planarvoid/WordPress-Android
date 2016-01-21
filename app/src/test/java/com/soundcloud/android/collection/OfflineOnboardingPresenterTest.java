package com.soundcloud.android.collection;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.support.v7.app.AppCompatActivity;

public class OfflineOnboardingPresenterTest extends AndroidUnitTest {

    @Mock private AppCompatActivity activity;
    @Mock private Navigator navigator;
    @Mock private OfflineOnboardingView onboardingView;
    @Mock private OfflineContentOperations offlineContentOperations;

    @Captor private ArgumentCaptor<OfflineOnboardingView.Listener> listenerCaptor;

    private OfflineOnboardingPresenter presenter;

    @Before
    public void setUp() {
        presenter = new OfflineOnboardingPresenter(onboardingView, navigator, offlineContentOperations);
    }

    @Test
    public void selectiveSyncOpensStream() {
        presenter.onCreate(activity, null);

        verify(onboardingView).setupContentView(eq(activity), listenerCaptor.capture());
        listenerCaptor.getValue().selectiveSync();

        verifyZeroInteractions(offlineContentOperations);
        verify(navigator).openStream(activity, Screen.OFFLINE_ONBOARDING);
    }

    @Test
    public void autoSyncEnablesFeatureAndOpensCollection() {
        presenter.onCreate(activity, null);

        verify(onboardingView).setupContentView(eq(activity), listenerCaptor.capture());
        listenerCaptor.getValue().autoSync();

        verify(offlineContentOperations).enableOfflineCollection();
        verify(navigator).openCollection(activity);
    }

}
