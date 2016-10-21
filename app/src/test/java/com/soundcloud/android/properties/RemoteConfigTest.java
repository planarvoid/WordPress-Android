package com.soundcloud.android.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigInfo;
import com.soundcloud.android.storage.PersistentStorage;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.Context;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class RemoteConfigTest {

    private static final Flag FLAG = Flag.TEST_FEATURE;
    private static final String FLAG_KEY = String.format(Locale.US, RemoteConfig.REMOTE_FEATURE_FLAG_PREFIX, FLAG).toLowerCase();
    private static final long LAST_FETCH_TIME_MILLIS = TimeUnit.HOURS.toMillis(2);
    private static final long CURRENT_TIME_MILLIS = TimeUnit.DAYS.toMillis(1);

    private RemoteConfig remoteConfig;

    @Mock private FirebaseRemoteConfig firebaseRemoteConfig;
    @Mock private FirebaseRemoteConfigInfo firebaseRemoteConfigInfo;
    @Mock private PersistentStorage flagsStorage;
    @Mock private CurrentDateProvider currentDateProvider;
    @Mock private GooglePlayServicesWrapper googlePlayServicesWrapper;
    @Mock private Task task;
    @Mock private Context context;

    @Captor private ArgumentCaptor<OnCompleteListener<Void>> onCompleteListenerCaptor;

    @Before
    public void setUp() {
        when(task.isSuccessful()).thenReturn(true);
        when(firebaseRemoteConfig.getInfo()).thenReturn(firebaseRemoteConfigInfo);
        when(firebaseRemoteConfig.fetch(anyLong())).thenReturn(task);
        when(googlePlayServicesWrapper.isPlayServicesAvailable(any(Context.class))).thenReturn(true);
        remoteConfig = new RemoteConfig(firebaseRemoteConfig, flagsStorage, currentDateProvider, googlePlayServicesWrapper);
    }

    @Test
    public void shouldFetchFeatureFlagsIfNeverFetchedBefore() {
        when(firebaseRemoteConfigInfo.getLastFetchStatus()).thenReturn(FirebaseRemoteConfig.LAST_FETCH_STATUS_NO_FETCH_YET);

        remoteConfig.fetchFeatureFlags(context);

        verify(firebaseRemoteConfig).fetch(RemoteConfig.CACHE_EXPIRATION_TIME);
    }

    @Test
    public void shouldFetchFeatureFlagsIfLastAttemptFailed() {
        when(firebaseRemoteConfigInfo.getLastFetchStatus()).thenReturn(FirebaseRemoteConfig.LAST_FETCH_STATUS_FAILURE);

        remoteConfig.fetchFeatureFlags(context);

        verify(firebaseRemoteConfig).fetch(RemoteConfig.CACHE_EXPIRATION_TIME);
    }

    @Test
    public void shouldFetchFeatureFlagsIfCacheIsExpired() {
        when(firebaseRemoteConfigInfo.getLastFetchStatus()).thenReturn(FirebaseRemoteConfig.LAST_FETCH_STATUS_SUCCESS);
        when(firebaseRemoteConfigInfo.getFetchTimeMillis()).thenReturn(LAST_FETCH_TIME_MILLIS);
        when(currentDateProvider.getCurrentTime()).thenReturn(CURRENT_TIME_MILLIS);

        remoteConfig.fetchFeatureFlags(context);

        verify(firebaseRemoteConfig).fetch(RemoteConfig.CACHE_EXPIRATION_TIME);
    }

    @Test
    public void shouldNotFetchFeatureFlagsIfLastAttemptSucceedButCacheIsNotExpired() {
        when(firebaseRemoteConfigInfo.getLastFetchStatus()).thenReturn(FirebaseRemoteConfig.LAST_FETCH_STATUS_SUCCESS);
        when(firebaseRemoteConfigInfo.getFetchTimeMillis()).thenReturn(LAST_FETCH_TIME_MILLIS);
        when(currentDateProvider.getCurrentTime()).thenReturn(LAST_FETCH_TIME_MILLIS);

        remoteConfig.fetchFeatureFlags(context);

        verify(firebaseRemoteConfig, never()).fetch(anyLong());
    }

    @Test
    public void shouldActivateFetchedRemoteConfigAndPersistFlagValues() {
        when(firebaseRemoteConfigInfo.getLastFetchStatus()).thenReturn(FirebaseRemoteConfig.LAST_FETCH_STATUS_SUCCESS);
        when(firebaseRemoteConfigInfo.getFetchTimeMillis()).thenReturn(LAST_FETCH_TIME_MILLIS);
        when(currentDateProvider.getCurrentTime()).thenReturn(CURRENT_TIME_MILLIS);

        when(firebaseRemoteConfig.getString(anyString())).thenReturn("feature");

        remoteConfig.fetchFeatureFlags(context);

        verify(task).addOnCompleteListener(onCompleteListenerCaptor.capture());
        onCompleteListenerCaptor.getValue().onComplete(task);

        verify(firebaseRemoteConfig).activateFetched();
        verify(flagsStorage, times(Flag.features().size())).persist(anyString(), anyBoolean());
    }

    @Test
    public void shouldNotInteractWithStorageIfFetchFails() {
        when(firebaseRemoteConfigInfo.getLastFetchStatus()).thenReturn(FirebaseRemoteConfig.LAST_FETCH_STATUS_SUCCESS);
        when(firebaseRemoteConfigInfo.getFetchTimeMillis()).thenReturn(LAST_FETCH_TIME_MILLIS);
        when(currentDateProvider.getCurrentTime()).thenReturn(CURRENT_TIME_MILLIS);
        when(task.isSuccessful()).thenReturn(false);
        when(task.getException()).thenReturn(mock(Exception.class));

        when(firebaseRemoteConfig.getString(anyString())).thenReturn("feature");

        remoteConfig.fetchFeatureFlags(context);

        verify(task).addOnCompleteListener(onCompleteListenerCaptor.capture());
        onCompleteListenerCaptor.getValue().onComplete(task);

        verifyZeroInteractions(flagsStorage);
    }

    @Test
    public void shouldFollowRemoteFeatureFlagNamingConventions() {
        final String remoteFlagKey = remoteConfig.getFlagKey(FLAG);

        assertThat(remoteFlagKey).isEqualTo(FLAG_KEY);
    }

    @Test
    public void getFlagValueShouldReturnValueFromStorage() {
        when(flagsStorage.getValue(FLAG_KEY, true)).thenReturn(true);

        final boolean flagValue = remoteConfig.getFlagValue(FLAG, true);

        assertThat(flagValue).isTrue();
        verify(flagsStorage).getValue(FLAG_KEY, true);
    }
}
