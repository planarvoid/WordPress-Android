package com.soundcloud.android.policies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.app.Activity;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PolicyUpdateControllerTest extends AndroidUnitTest {
    private PolicyUpdateController controller;

    @Mock private FeatureOperations featureOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private PolicySettingsStorage policySettingsStorage;
    @Mock private CurrentDateProvider dateProvider;
    @Mock private GoBackOnlineDialogPresenter goOnlinePresenter;
    @Mock private NetworkConnectionHelper connectionHelper;

    private long yesterday;
    private long now;
    private long tomorrow;
    private long online27DaysAgo;
    private long online30DaysAgo;
    private long online33DaysAgo;

    @Before
    public void setUp() throws Exception {
        now = System.currentTimeMillis();
        yesterday = now - TimeUnit.DAYS.toMillis(1);
        tomorrow = now + TimeUnit.DAYS.toMillis(1);
        controller = new PolicyUpdateController(
                featureOperations,
                offlineContentOperations,
                policySettingsStorage,
                dateProvider,
                goOnlinePresenter,
                connectionHelper);

        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(dateProvider.getTime()).thenReturn(now);
        when(offlineContentOperations.clearOfflineContent()).thenReturn(Observable.<List<Urn>>empty());

        online27DaysAgo = now - TimeUnit.DAYS.toMillis(27L);
        online30DaysAgo = now - TimeUnit.DAYS.toMillis(30L);
        online33DaysAgo = now - TimeUnit.DAYS.toMillis(33L);
    }

    @Test
    public void doesNotDisplayTheDialogWhenOfflineContentDisabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);

        controller.onResume(null);

        verify(goOnlinePresenter, never()).show(any(Activity.class), anyLong());
        verify(policySettingsStorage, never()).getPolicyUpdateTime();
    }

    @Test
    public void checksPoliciesUpdateTimeOnlyOnceADay() {
        when(policySettingsStorage.getLastPolicyCheckTime()).thenReturn(now);
        controller.onResume(null);

        verify(policySettingsStorage, never()).getPolicyUpdateTime();
        verifyZeroInteractions(goOnlinePresenter, offlineContentOperations);
    }

    @Test
    public void checksPoliciesUpdateTimeEveryDay() {
        when(dateProvider.getTime()).thenReturn(yesterday);
        controller.onResume(null);

        when(dateProvider.getTime()).thenReturn(tomorrow);
        controller.onResume(null);

        verify(policySettingsStorage, times(2)).getPolicyUpdateTime();
    }

    @Test
    public void showsGoBackOnlineDialogWhenLastUpdate27DaysAgo() {
        when(policySettingsStorage.getLastPolicyCheckTime()).thenReturn(yesterday);
        when(policySettingsStorage.getPolicyUpdateTime()).thenReturn(online27DaysAgo);

        controller.onResume(null);

        verify(goOnlinePresenter).show(null, online27DaysAgo);
    }

    @Test
    public void deletesOfflineContentWhenLastUpdate30DaysAgo() {
        final PublishSubject<List<Urn>> clearOfflineContentSubject = PublishSubject.create();
        when(offlineContentOperations.clearOfflineContent()).thenReturn(clearOfflineContentSubject);
        when(policySettingsStorage.getLastPolicyCheckTime()).thenReturn(yesterday);
        when(policySettingsStorage.getPolicyUpdateTime()).thenReturn(online30DaysAgo);

        controller.onResume(null);

        assertThat(clearOfflineContentSubject.hasObservers()).isTrue();
    }

    @Test
    public void deletesOfflineContentWhenLastUpdate33DaysAgo() {
        final PublishSubject<List<Urn>> clearOfflineContentSubject = PublishSubject.create();
        when(offlineContentOperations.clearOfflineContent()).thenReturn(clearOfflineContentSubject);
        when(policySettingsStorage.getLastPolicyCheckTime()).thenReturn(yesterday);
        when(policySettingsStorage.getPolicyUpdateTime()).thenReturn(online33DaysAgo);

        controller.onResume(null);

        assertThat(clearOfflineContentSubject.hasObservers()).isTrue();
    }
}