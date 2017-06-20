package com.soundcloud.android.view;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import android.view.View;

public class OfflineStateHelperTest extends AndroidUnitTest {

    @Mock private View view;
    @Mock private OfflineStateHelper.Callback callback;

    private OfflineStateHelper helper;

    @Before
    public void setUp() throws Exception {
        helper = OfflineStateHelper.create(view, callback);
    }

    @Test
    public void requestedToDownloadingTransitionsImmediately() {
        helper.update(OfflineState.REQUESTED, OfflineState.DOWNLOADING);

        verify(callback).onStateTransition(OfflineState.DOWNLOADING);
        verifyZeroInteractions(view);
    }

    @Test
    public void downloadingToRequestedTransitionsWithDelay() {
        helper.update(OfflineState.DOWNLOADING, OfflineState.REQUESTED);

        verify(view).postDelayed(any(Runnable.class), eq(200L));
    }

    @Test
    public void delayedStateIsOverridenByNewState() {
        helper.update(OfflineState.DOWNLOADING, OfflineState.REQUESTED);
        helper.update(OfflineState.REQUESTED, OfflineState.DOWNLOADING);

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).postDelayed(any(Runnable.class), eq(200L));
        inOrder.verify(view).removeCallbacks(any(Runnable.class));
    }

}
