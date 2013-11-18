package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.longThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.matchers.GreaterOrEqual;

import android.content.Context;
import android.content.SharedPreferences;

public class UnauthorisedRequestRegistryTest {

    private UnauthorisedRequestRegistry registry;
    @Mock
    private Context context;
    @Mock
    private SharedPreferences sharedPreference;
    @Mock
    private SharedPreferences.Editor editor;

    @Before
    public void setup() {
        initMocks(this);
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreference);
        registry = new UnauthorisedRequestRegistry(context);
    }

    @Test
    public void shouldRetrieveSharedPreferenceWithExpectedName(){
        reset(context);
        registry = new UnauthorisedRequestRegistry(context);
        verify(context).getSharedPreferences("UnauthorisedRequestRegister", Context.MODE_PRIVATE);
    }

    @Test
    public void shouldUpdateObservedTimeOfUnauthorisedRequestsIfNoneObservedBefore(){
        final Long currentTime = System.currentTimeMillis();
        when(sharedPreference.getLong("first_observed_timestamp", 0L)).thenReturn(0L);
        when(sharedPreference.edit()).thenReturn(editor);
        when(editor.commit()).thenReturn(true);
        expect(registry.updateObservedUnauthorisedRequestTimestamp()).toBeTrue();
        verify(editor).putLong(eq("first_observed_timestamp"), longThat(new GreaterOrEqual<Long>(currentTime)));
    }

    @Test
    public void shouldNotUpdateObservedTimeOfUnauthorisedRequestsIfObservedBefore(){
        when(sharedPreference.getLong("first_observed_timestamp", 0L)).thenReturn(1L);
        expect(registry.updateObservedUnauthorisedRequestTimestamp()).toBeFalse();
        verify(sharedPreference, never()).edit();
    }

    @Test
    public void shouldResetFirstObservedTimeOfUnauthorisedRequest(){
        when(sharedPreference.edit()).thenReturn(editor);
        registry.clearObservedUnauthorisedRequestTimestamp();
        verify(editor).clear();
        verify(editor).commit();
    }

    @Test
    public void shouldReturnTrueIfTimeLimitForFirstObservedUnauthorisedRequestHasExpired(){
        when(sharedPreference.getLong("first_observed_timestamp", 0L)).thenReturn(22L);
        expect(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).toBeTrue();
    }

    @Test
    public void shouldReturnFalseIfTimeLimitForFirstObservedUnauthorisedRequestHasNotExpired(){
        when(sharedPreference.getLong("first_observed_timestamp", 0L)).thenReturn(System.currentTimeMillis());
        expect(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).toBeFalse();
    }

    @Test
    public void shouldReturnFalseIfTimeLimitForFirstObservedUnauthorisedRequestDoesNotExist(){
        when(sharedPreference.getLong("first_observed_timestamp", 0L)).thenReturn(0L);
        expect(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).toBeFalse();
    }

}
