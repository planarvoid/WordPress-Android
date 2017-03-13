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
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.app.Activity;

import java.util.concurrent.TimeUnit;

public class PolicyUpdateControllerTest extends AndroidUnitTest {
    private PolicyUpdateController controller;

    @Mock private FeatureOperations featureOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private PolicyOperations policyOperations;
    @Mock private PolicySettingsStorage policySettingsStorage;
    @Mock private GoBackOnlineDialogPresenter goOnlinePresenter;
    @Mock private NetworkConnectionHelper connectionHelper;

    private long yesterday;
    private long now;
    private long tomorrow;
    private long online27DaysAgo;
    private long online30DaysAgo;
    private long online33DaysAgo;
    private TestDateProvider dateProvider;

    @Before
    public void setUp() throws Exception {
        now = System.currentTimeMillis();
        yesterday = now - TimeUnit.DAYS.toMillis(1);
        tomorrow = now + TimeUnit.DAYS.toMillis(1);
        dateProvider = new TestDateProvider(now);
        controller = new PolicyUpdateController(
                featureOperations,
                offlineContentOperations,
                policyOperations, policySettingsStorage,
                dateProvider,
                goOnlinePresenter,
                connectionHelper);

        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineContentOperations.clearOfflineContent()).thenReturn(Observable.empty());
        when(policyOperations.getMostRecentPolicyUpdateTimestamp()).thenReturn(Observable.empty());

        online27DaysAgo = now - TimeUnit.DAYS.toMillis(27L);
        online30DaysAgo = now - TimeUnit.DAYS.toMillis(30L);
        online33DaysAgo = now - TimeUnit.DAYS.toMillis(33L);
    }

    @Test
    public void doesNotDisplayTheDialogWhenOfflineContentDisabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);

        controller.onResume(null);

        verify(goOnlinePresenter, never()).show(any(Activity.class), anyLong());
        verify(policySettingsStorage, never()).getLastPolicyCheckTime();
    }

    @Test
    public void checksPoliciesUpdateTimeOnlyOnceADay() {
        when(policySettingsStorage.getLastPolicyCheckTime()).thenReturn(now);
        controller.onResume(null);

        verify(policySettingsStorage, never()).setLastPolicyCheckTime(any(Long.class));
        verify(goOnlinePresenter, never()).show(any(Activity.class), anyLong());
        verifyZeroInteractions(connectionHelper, offlineContentOperations);
    }

    @Test
    public void checksPoliciesUpdateTimeEveryDay() {
        when(policyOperations.getMostRecentPolicyUpdateTimestamp())
                .thenReturn(Observable.just(online27DaysAgo));

        dateProvider.setTime(yesterday, TimeUnit.MILLISECONDS);
        controller.onResume(null);

        dateProvider.setTime(tomorrow, TimeUnit.MILLISECONDS);
        controller.onResume(null);

        verify(policySettingsStorage, times(2)).setLastPolicyCheckTime(any(Long.class));
    }

    @Test
    public void showsGoBackOnlineDialogWhenLastUpdate27DaysAgo() {
        when(policySettingsStorage.getLastPolicyCheckTime()).thenReturn(yesterday);
        when(policyOperations.getMostRecentPolicyUpdateTimestamp())
                .thenReturn(Observable.just(online27DaysAgo));

        controller.onResume(null);

        verify(goOnlinePresenter).show(null, online27DaysAgo);
    }

    @Test
    public void deletesOfflineContentWhenLastUpdate30DaysAgo() {
        final PublishSubject<Void> clearOfflineContentSubject = PublishSubject.create();
        when(offlineContentOperations.clearOfflineContent()).thenReturn(clearOfflineContentSubject);
        when(policySettingsStorage.getLastPolicyCheckTime()).thenReturn(yesterday);
        when(policyOperations.getMostRecentPolicyUpdateTimestamp())
                .thenReturn(Observable.just(online30DaysAgo));

        controller.onResume(null);

        assertThat(clearOfflineContentSubject.hasObservers()).isTrue();
    }

    @Test
    public void deletesOfflineContentWhenLastUpdate33DaysAgo() {
        final PublishSubject<Void> clearOfflineContentSubject = PublishSubject.create();
        when(offlineContentOperations.clearOfflineContent()).thenReturn(clearOfflineContentSubject);
        when(policySettingsStorage.getLastPolicyCheckTime()).thenReturn(yesterday);
        when(policyOperations.getMostRecentPolicyUpdateTimestamp())
                .thenReturn(Observable.just(online33DaysAgo));

        controller.onResume(null);

        assertThat(clearOfflineContentSubject.hasObservers()).isTrue();
    }
}
