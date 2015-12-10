package com.soundcloud.android.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Context;
import android.content.Intent;

import java.util.concurrent.atomic.AtomicLong;

public class UnauthorisedRequestRegistryTest extends AndroidUnitTest {

    @Mock private Context context;
    private UnauthorisedRequestRegistry registry;

    @Before
    public void setup() {
        when(context.getApplicationContext()).thenReturn(context);
        registry = new UnauthorisedRequestRegistry(context, new AtomicLong(0L));
    }

    @Test
    public void shouldUpdateObservedTimeOfUnauthorisedRequestsIfNoneObservedBefore() {
        final Long currentTime = System.currentTimeMillis();
        registry.updateObservedUnauthorisedRequestTimestamp();
        assertThat(registry.getLastObservedTime() >= currentTime).isTrue();
    }

    @Test
    public void shouldNotUpdateObservedTimeOfUnauthorisedRequestsIfObservedBefore() {
        assertThat(registry.getLastObservedTime()).isEqualTo(0L);
        registry.updateObservedUnauthorisedRequestTimestamp();
        long timestamp = registry.getLastObservedTime();
        assertThat(timestamp).isGreaterThanOrEqualTo(0L);
        registry.updateObservedUnauthorisedRequestTimestamp();
        assertThat(registry.getLastObservedTime()).isEqualTo(timestamp);
    }

    @Test
    public void shouldResetFirstObservedTimeOfUnauthorisedRequest() {
        assertThat(registry.getLastObservedTime()).isEqualTo(0L);
        registry.updateObservedUnauthorisedRequestTimestamp();
        assertThat(registry.getLastObservedTime()).isGreaterThanOrEqualTo(0L);
        registry.clearObservedUnauthorisedRequestTimestamp();
        assertThat(registry.getLastObservedTime()).isEqualTo(0L);
    }

    @Test
    public void shouldReturnTrueIfTimeLimitForFirstObservedUnauthorisedRequestHasExpired() {
        registry = new UnauthorisedRequestRegistry(context, new AtomicLong(22L));
        assertThat(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).isTrue();
    }

    @Test
    public void shouldReturnFalseIfTimeLimitForFirstObservedUnauthorisedRequestHasNotExpired() {
        registry = new UnauthorisedRequestRegistry(context, new AtomicLong(System.currentTimeMillis()));
        assertThat(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).isFalse();
    }

    @Test
    public void shouldReturnFalseIfTimeLimitForFirstObservedUnauthorisedRequestDoesNotExist() {
        assertThat(registry.getLastObservedTime()).isEqualTo(0L);
        assertThat(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).isFalse();
    }

    @Test
    public void shouldSendIntentAfterUpdatingObservedTimestamp() {
        registry.updateObservedUnauthorisedRequestTimestamp();
        verify(context).sendBroadcast(new Intent(Consts.GeneralIntents.UNAUTHORIZED));
    }

}
