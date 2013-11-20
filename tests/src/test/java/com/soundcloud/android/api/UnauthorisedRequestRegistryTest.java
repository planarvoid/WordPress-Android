package com.soundcloud.android.api;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.longThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.matchers.GreaterOrEqual;
import rx.Observer;

import android.content.Context;
import android.content.SharedPreferences;

@RunWith(SoundCloudTestRunner.class)
public class UnauthorisedRequestRegistryTest {

    private UnauthorisedRequestRegistry registry;
    @Mock
    private Context context;
    @Mock
    private SharedPreferences sharedPreference;
    @Mock
    private SharedPreferences.Editor editor;
    @Mock
    private UnauthorisedRequestRegistry staticInstance;
    @Mock
    private Observer<Void> voidObserver;
    @Mock
    private Observer<Boolean> booleanObserver;

    @Before
    public void setup() {
        initMocks(this);
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreference);
        registry = new UnauthorisedRequestRegistry(context, staticInstance);
    }

    @Test
    public void shouldRetrieveSharedPreferenceWithExpectedName(){
        reset(context);
        registry = new UnauthorisedRequestRegistry(context, staticInstance);
        verify(context).getSharedPreferences("UnauthorisedRequestRegister", Context.MODE_PRIVATE);
    }

    @Test
    public void shouldUpdateObservedTimeOfUnauthorisedRequestsIfNoneObservedBefore(){
        final Long currentTime = System.currentTimeMillis();
        when(sharedPreference.getLong("first_observed_timestamp", 0L)).thenReturn(0L);
        when(sharedPreference.edit()).thenReturn(editor);
        when(editor.commit()).thenReturn(true);
        registry.updateObservedUnauthorisedRequestTimestamp().subscribe(voidObserver);
        verify(editor).putLong(eq("first_observed_timestamp"), longThat(new GreaterOrEqual<Long>(currentTime)));
        verify(voidObserver).onCompleted();
        verifyNoMoreInteractions(voidObserver);
    }

    @Test
    public void shouldNotUpdateObservedTimeOfUnauthorisedRequestsIfObservedBefore(){
        when(sharedPreference.getLong("first_observed_timestamp", 0L)).thenReturn(1L);
        registry.updateObservedUnauthorisedRequestTimestamp().subscribe(voidObserver);
        verify(sharedPreference, never()).edit();
        verify(voidObserver).onCompleted();
        verifyNoMoreInteractions(voidObserver);
    }

    @Test
    public void shouldResetFirstObservedTimeOfUnauthorisedRequest(){
        when(sharedPreference.edit()).thenReturn(editor);
        registry.clearObservedUnauthorisedRequestTimestampAsync().subscribe(voidObserver);
        verify(editor).clear();
        verify(editor).commit();
        verify(voidObserver).onCompleted();
        verifyNoMoreInteractions(voidObserver);
    }

    @Test
    public void shouldReturnTrueIfTimeLimitForFirstObservedUnauthorisedRequestHasExpired(){
        when(sharedPreference.getLong("first_observed_timestamp", 0L)).thenReturn(22L);
        registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit().subscribe(booleanObserver);
        verify(booleanObserver).onNext(true);
        verify(booleanObserver).onCompleted();
        verifyNoMoreInteractions(booleanObserver);
    }

    @Test
    public void shouldReturnFalseIfTimeLimitForFirstObservedUnauthorisedRequestHasNotExpired(){
        when(sharedPreference.getLong("first_observed_timestamp", 0L)).thenReturn(System.currentTimeMillis());
        registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit().subscribe(booleanObserver);
        verify(booleanObserver).onNext(false);
        verify(booleanObserver).onCompleted();
        verifyNoMoreInteractions(booleanObserver);
    }

    @Test
    public void shouldReturnFalseIfTimeLimitForFirstObservedUnauthorisedRequestDoesNotExist(){
        when(sharedPreference.getLong("first_observed_timestamp", 0L)).thenReturn(0L);
        registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit().subscribe(booleanObserver);
        verify(booleanObserver).onNext(false);
        verify(booleanObserver).onCompleted();
        verifyNoMoreInteractions(booleanObserver);
    }

}
