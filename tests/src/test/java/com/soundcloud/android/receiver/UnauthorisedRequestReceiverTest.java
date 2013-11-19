package com.soundcloud.android.receiver;

import static com.soundcloud.android.rx.TestObservables.booleanObservable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.dialog.TokenExpiredDialogFragment;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentManager;

@RunWith(SoundCloudTestRunner.class)
public class UnauthorisedRequestReceiverTest {

    private UnauthorisedRequestReceiver receiver;
    @Mock
    private Context context;
    @Mock
    private UnauthorisedRequestRegistry registry;
    @Mock
    private FragmentManager fragmentManager;
    @Mock
    private Observable<Boolean> booleanObservable;
    @Mock
    private Observable<Void> voidObservable;
    @Mock
    private Intent intent;
    @Mock
    private TokenExpiredDialogFragment tokenExpiredDialog;

    @Before
    public void setUp() throws Exception {
        receiver = new UnauthorisedRequestReceiver(registry, fragmentManager, tokenExpiredDialog);
    }

    @Test
    public void shouldNotShowDialogIfTimeLimitHasNotExpired(){
        when(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).thenReturn(booleanObservable(false));
        receiver.onReceive(context, intent);
        verifyZeroInteractions(fragmentManager);
    }

    @Test
    public void shouldNotClearObservedTimeStampIfLimitHasNotExpired(){
        when(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).thenReturn(booleanObservable(false));
        receiver.onReceive(context, intent);
        verify(registry, never()).clearObservedUnauthorisedRequestTimestamp();
    }

    @Test
    public void shouldShowDialogIfTimeLimitHasExpired(){
        when(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).thenReturn(booleanObservable(true));
        when(registry.clearObservedUnauthorisedRequestTimestamp()).thenReturn(voidObservable);
        receiver.onReceive(context, intent);
        verify(tokenExpiredDialog).show(fragmentManager, TokenExpiredDialogFragment.TAG);
    }

    @Test
    public void shouldClearObservedTimestampIfShowingDialog(){
        when(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).thenReturn(booleanObservable(true));
        when(registry.clearObservedUnauthorisedRequestTimestamp()).thenReturn(voidObservable);
        receiver.onReceive(context, intent);
        verify(registry).clearObservedUnauthorisedRequestTimestamp();
    }
}
