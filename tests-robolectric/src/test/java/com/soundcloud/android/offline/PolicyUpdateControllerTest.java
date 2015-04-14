package com.soundcloud.android.offline;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.DateProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.app.Activity;

import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class PolicyUpdateControllerTest {
    private PolicyUpdateController controller;

    @Mock private FeatureOperations featureOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private OfflineSettingsStorage offlineSettingsStorage;
    @Mock private DateProvider dateProvider;
    @Mock private GoBackOnlineDialogPresenter goOnlinePresenter;

    private long yesterday;
    private long now;
    private long tomorrow;
    private long online27DaysAgo;

    @Before
    public void setUp() throws Exception {
        now = System.currentTimeMillis();
        yesterday = now - TimeUnit.DAYS.toMillis(1);
        tomorrow = now + TimeUnit.DAYS.toMillis(1);
        controller = new PolicyUpdateController(
                featureOperations,
                offlineContentOperations,
                offlineSettingsStorage,
                dateProvider,
                goOnlinePresenter);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineContentOperations.tryToUpdateAndLoadLastPoliciesUpdateTime()).thenReturn(Observable.just(yesterday));
        when(dateProvider.getCurrentTime()).thenReturn(now);
        online27DaysAgo = now - TimeUnit.DAYS.toMillis(27L);
    }

    @Test
    public void shouldCheckPoliciesOnlyOnceADay() {
        when(offlineSettingsStorage.getPolicyUpdateCheckTime()).thenReturn(now);
        controller.onResume(null);

        verify(offlineContentOperations, never()).tryToUpdateAndLoadLastPoliciesUpdateTime();
    }

    @Test
    public void shouldCheckPoliciesEveryDay() {
        when(dateProvider.getCurrentTime()).thenReturn(yesterday);
        controller.onResume(null);
        when(dateProvider.getCurrentTime()).thenReturn(tomorrow);
        controller.onResume(null);

        verify(offlineContentOperations, times(2)).tryToUpdateAndLoadLastPoliciesUpdateTime();
    }

    @Test
    public void shouldUpdateTheNextTimeIfTheFirstTimeFailed() {
        when(offlineContentOperations.tryToUpdateAndLoadLastPoliciesUpdateTime()).thenReturn(Observable.<Long>error(new RuntimeException("Test exception")));

        controller.onResume(null);
        controller.onResume(null);

        verify(offlineContentOperations, times(2)).tryToUpdateAndLoadLastPoliciesUpdateTime();
    }

    @Test
    public void shouldShowGoBackOnlineDialogWhenLastUpdate27DaysAgo() {
        when(offlineContentOperations.tryToUpdateAndLoadLastPoliciesUpdateTime()).thenReturn(Observable.just(online27DaysAgo));

        controller.onResume(null);

        verify(goOnlinePresenter).show(null, online27DaysAgo);
    }

    @Test
    public void doesNotDisplayTheDialogWhenOffContentDisabled() {
        when(offlineContentOperations.tryToUpdateAndLoadLastPoliciesUpdateTime()).thenReturn(Observable.just(online27DaysAgo));
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);

        controller.onResume(null);

        verify(goOnlinePresenter, never()).show(any(Activity.class), anyLong());
    }
}