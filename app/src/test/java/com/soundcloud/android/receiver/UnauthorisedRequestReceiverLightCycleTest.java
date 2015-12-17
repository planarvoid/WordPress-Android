package com.soundcloud.android.receiver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;

public class UnauthorisedRequestReceiverLightCycleTest extends AndroidUnitTest {

    @Captor ArgumentCaptor<IntentFilter> intentArgumentCaptor;
    @Mock private AppCompatActivity activity;

    private UnauthorisedRequestReceiver.LightCycle lightCycle;

    @Before
    public void setUp() throws Exception {
        when(activity.getApplicationContext()).thenReturn(context());
        lightCycle = new UnauthorisedRequestReceiver.LightCycle();
        lightCycle.onCreate(activity, null);
    }

    @Test
    public void registerOnResume() {
        lightCycle.onResume(activity);

        verify(activity).registerReceiver(any(UnauthorisedRequestReceiver.class), intentArgumentCaptor.capture());
        assertThat(intentArgumentCaptor.getValue().getAction(0)).isEqualTo(Consts.GeneralIntents.UNAUTHORIZED);
    }

    @Test
    public void unregisterOnPause() {
        lightCycle.onResume(activity);
        lightCycle.onPause(activity);

        verify(activity).unregisterReceiver(any(UnauthorisedRequestReceiver.class));
    }
}
