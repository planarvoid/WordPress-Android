package com.soundcloud.android.receiver;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.dialog.TokenExpiredDialogFragment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentManager;

@RunWith(MockitoJUnitRunner.class)
public class UnauthorisedRequestReceiverTest {

    private UnauthorisedRequestReceiver receiver;

    @Mock private Context context;
    @Mock private UnauthorisedRequestRegistry registry;
    @Mock private FragmentManager fragmentManager;
    @Mock private Intent intent;
    @Mock private TokenExpiredDialogFragment tokenExpiredDialog;

    @Before
    public void setUp() {
        receiver = new UnauthorisedRequestReceiver(registry, fragmentManager, tokenExpiredDialog);
    }

    @Test
    public void shouldNotShowDialogIfTimeLimitHasNotExpired() {
        when(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).thenReturn(false);
        receiver.onReceive(context, intent);
        verifyZeroInteractions(fragmentManager);
    }

    @Test
    public void shouldNotClearObservedTimeStampIfLimitHasNotExpired() {
        when(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).thenReturn(false);
        receiver.onReceive(context, intent);
        verify(registry, never()).clearObservedUnauthorisedRequestTimestamp();
    }

    @Test
    public void shouldShowDialogIfTimeLimitHasExpired() {
        when(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).thenReturn(true);
        receiver.onReceive(context, intent);
        verify(tokenExpiredDialog).show(fragmentManager, TokenExpiredDialogFragment.TAG);
    }

    @Test
    public void shouldClearObservedTimestampIfShowingDialog() {
        when(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).thenReturn(true);
        receiver.onReceive(context, intent);
        verify(registry).clearObservedUnauthorisedRequestTimestamp();
    }

    @Test
    public void shouldShowDialogIfNotAlreadyShowing() {
        when(fragmentManager.findFragmentByTag(TokenExpiredDialogFragment.TAG)).thenReturn(null);
        when(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).thenReturn(true);
        receiver.onReceive(context, intent);
        verify(tokenExpiredDialog).show(fragmentManager, TokenExpiredDialogFragment.TAG);
    }

    @Test
    public void shouldNotShowDialogIfAlreadyShowing() {
        when(fragmentManager.findFragmentByTag(TokenExpiredDialogFragment.TAG)).thenReturn(tokenExpiredDialog);
        when(registry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()).thenReturn(true);
        receiver.onReceive(context, intent);
        verify(tokenExpiredDialog, never()).show(fragmentManager, TokenExpiredDialogFragment.TAG);
    }

}
