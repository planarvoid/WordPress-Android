package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;
import android.content.Intent;

import java.util.concurrent.atomic.AtomicLong;

@RunWith(SoundCloudTestRunner.class)
public class UnauthorisedRequestRegistryTest {

    private UnauthorisedRequestRegistry registry;
    @Mock
    private Context context;
    private AtomicLong lastObservedTime;

    @Before
    public void setup() {
        when(context.getApplicationContext()).thenReturn(context);
        lastObservedTime = new AtomicLong(0L);
        registry = new UnauthorisedRequestRegistry(context, lastObservedTime);
    }

    @Test
    public void shouldUpdateObservedTimeOfUnauthorisedRequestsIfNoneObservedBefore(){
        final Long currentTime = System.currentTimeMillis();
        registry.updateObservedUnauthorisedRequestTimestamp();
        expect(registry.getLastObservedTime() >= currentTime).toBeTrue();
    }

    @Test
    public void shouldNotUpdateObservedTimeOfUnauthorisedRequestsIfObservedBefore(){
        expect(registry.getLastObservedTime()).toBe(0L);
        registry.updateObservedUnauthorisedRequestTimestamp();
        long timestamp = registry.getLastObservedTime();
        expect(timestamp).toBeGreaterThan(0L);
        registry.updateObservedUnauthorisedRequestTimestamp();
        expect(registry.getLastObservedTime()).toEqual(timestamp);
    }

    @Test
    public void shouldResetFirstObservedTimeOfUnauthorisedRequest(){
        expect(registry.getLastObservedTime()).toBe(0L);
        registry.updateObservedUnauthorisedRequestTimestamp();
        expect(registry.getLastObservedTime()).toBeGreaterThan(0L);
        registry.clearObservedUnauthorisedRequestTimestamp();
        expect(registry.getLastObservedTime()).toBe(0L);
    }

    @Test
    public void shouldReturnTrueIfTimeLimitForFirstObservedUnauthorisedRequestHasExpired(){
        registry.setLastObservedTime(22L);
        expect(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).toBeTrue();
    }

    @Test
    public void shouldReturnFalseIfTimeLimitForFirstObservedUnauthorisedRequestHasNotExpired(){
        registry.setLastObservedTime(System.currentTimeMillis());
        expect(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).toBeFalse();
    }

    @Test
    public void shouldReturnFalseIfTimeLimitForFirstObservedUnauthorisedRequestDoesNotExist(){
        expect(registry.getLastObservedTime()).toEqual(0L);
        expect(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).toBeFalse();
    }

    @Test
    public void shouldSendIntentAfterUpdatingObservedTimestamp(){
        registry.updateObservedUnauthorisedRequestTimestamp();
        verify(context).sendBroadcast(new Intent(Consts.GeneralIntents.UNAUTHORIZED));
    }

}
