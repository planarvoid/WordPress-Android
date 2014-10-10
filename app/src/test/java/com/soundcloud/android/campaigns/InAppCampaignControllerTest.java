package com.soundcloud.android.campaigns;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.localytics.android.LocalyticsAmpSession;
import com.soundcloud.android.R;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class InAppCampaignControllerTest {

    private InAppCampaignController inAppCampaignController;

    @Mock private ScActivity activity;
    @Mock private InAppCampaignController.LocalyticsAmpSessionFactory factory;
    @Mock private LocalyticsAmpSession session;

    @Before
    public void setUp() throws Exception {
        when(activity.getString(R.string.google_api_key)).thenReturn("123");
        when(factory.create(activity)).thenReturn(session);

        inAppCampaignController = new InAppCampaignController(factory);
        inAppCampaignController.onBind(activity);
    }

    @Test
    public void onBindRegistersPushApiKey() throws Exception {
        verify(session).registerPush("123");
    }

    @Test
    public void onResumeInitiatesPushSession() throws Exception {
        Intent intent = new Intent();
        when(activity.getIntent()).thenReturn(intent);

        inAppCampaignController.onResume();

        InOrder inOrder = Mockito.inOrder(session);
        inOrder.verify(session).open();
        inOrder.verify(session).upload();
        inOrder.verify(session).attach(activity);
        inOrder.verify(session).handleIntent(intent);
    }

    @Test
    public void onPauseClosesPushSession() throws Exception {
        inAppCampaignController.onPause();

        InOrder inOrder = Mockito.inOrder(session);
        inOrder.verify(session).detach();
        inOrder.verify(session).close();
        inOrder.verify(session).upload();
    }
}