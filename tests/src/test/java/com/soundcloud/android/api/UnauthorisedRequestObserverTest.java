package com.soundcloud.android.api;

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

@RunWith(SoundCloudTestRunner.class)
public class UnauthorisedRequestObserverTest {
    @Mock
    private Context context;

    @Before
    public void setUp() throws Exception {
        when(context.getApplicationContext()).thenReturn(context);
    }

    @Test
    public void requestObserverShouldSendBroadcastAfterUpdatingTimestamp() {
        UnauthorisedRequestObserver observer = new UnauthorisedRequestObserver(context);
        observer.onCompleted();
        verify(context).sendBroadcast(new Intent(Consts.GeneralIntents.UNAUTHORIZED));
    }

}
