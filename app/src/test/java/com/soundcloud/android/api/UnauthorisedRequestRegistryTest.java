package com.soundcloud.android.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class UnauthorisedRequestRegistryTest extends AndroidUnitTest {

    @Mock private Context context;
    private SharedPreferences preferences = sharedPreferences();
    private UnauthorisedRequestRegistry registry;

    @Before
    public void setup() {
        when(context.getApplicationContext()).thenReturn(context);
        registry = new UnauthorisedRequestRegistry(context, preferences);
    }

    @Test
    public void shouldUpdateObservedTimeOfUnauthorisedRequestsIfNoneObservedBefore() {
        final Long currentTime = System.currentTimeMillis();
        registry.updateObservedUnauthorisedRequestTimestamp();

        assertThat(getLastObservedAuthErrorTime() >= currentTime).isTrue();
    }

    @Test
    public void shouldNotUpdateObservedTimeOfUnauthorisedRequestsIfObservedBefore() {
        registry.updateObservedUnauthorisedRequestTimestamp();

        long timestamp = getLastObservedAuthErrorTime();
        assertThat(timestamp).isGreaterThanOrEqualTo(0L);

        registry.updateObservedUnauthorisedRequestTimestamp();
        assertThat(getLastObservedAuthErrorTime()).isEqualTo(timestamp);
    }

    @Test
    public void shouldResetFirstObservedTimeOfUnauthorisedRequest() {
        registry.updateObservedUnauthorisedRequestTimestamp();
        assertThat(getLastObservedAuthErrorTime()).isGreaterThanOrEqualTo(0L);

        registry.clearObservedUnauthorisedRequestTimestamp();
        assertThat(getLastObservedAuthErrorTime()).isEqualTo(0L);
    }

    @Test
    public void shouldReturnTrueIfTimeLimitForFirstObservedUnauthorisedRequestHasExpired() {
        assertThat(putLastObservedAuthErrorTime(22L)).isTrue();

        registry = new UnauthorisedRequestRegistry(context, preferences);
        assertThat(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).isTrue();
    }

    @Test
    public void shouldReturnFalseIfTimeLimitForFirstObservedUnauthorisedRequestHasNotExpired() {
        assertThat(putLastObservedAuthErrorTime(System.currentTimeMillis())).isTrue();

        registry = new UnauthorisedRequestRegistry(context, preferences);
        assertThat(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).isFalse();
    }

    @Test
    public void shouldReturnFalseIfTimeLimitForFirstObservedUnauthorisedRequestDoesNotExist() {
        assertThat(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).isFalse();
    }

    @Test
    public void shouldSendIntentAfterUpdatingObservedTimestamp() {
        registry.updateObservedUnauthorisedRequestTimestamp();

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(context).sendBroadcast(intentCaptor.capture());
        Assertions.assertThat(intentCaptor.getValue()).containsAction(Consts.GeneralIntents.UNAUTHORIZED);
    }

    private boolean putLastObservedAuthErrorTime(long time) {
        return preferences.edit().putLong("LAST_OBSERVED_AUTH_ERROR_TIME", time).commit();
    }

    private long getLastObservedAuthErrorTime() {
        return preferences.getLong("LAST_OBSERVED_AUTH_ERROR_TIME", 0L);
    }
}
