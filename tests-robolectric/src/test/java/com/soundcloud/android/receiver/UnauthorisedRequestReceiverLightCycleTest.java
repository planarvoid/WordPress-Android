package com.soundcloud.android.receiver;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;

@RunWith(SoundCloudTestRunner.class)
public class UnauthorisedRequestReceiverLightCycleTest {
    @Captor ArgumentCaptor<IntentFilter> intentArgumentCaptor;
    @Mock private AppCompatActivity activity;
    private UnauthorisedRequestReceiver.LightCycle lightCycle;

    @Before
    public void setUp() throws Exception {
        when(activity.getApplicationContext()).thenReturn(Robolectric.application);
        lightCycle = new UnauthorisedRequestReceiver.LightCycle();
        lightCycle.onCreate(activity, null);
    }

    @Test
    public void registerOnResume() {
        lightCycle.onResume(activity);

        verify(activity).registerReceiver(any(UnauthorisedRequestReceiver.class), intentArgumentCaptor.capture());
        expect(intentArgumentCaptor.getValue().getAction(0)).toEqual(Consts.GeneralIntents.UNAUTHORIZED);
    }

    @Test
    public void unregisterOnPause() {
        lightCycle.onResume(activity);
        lightCycle.onPause(activity);

        verify(activity).unregisterReceiver(any(UnauthorisedRequestReceiver.class));
    }
}