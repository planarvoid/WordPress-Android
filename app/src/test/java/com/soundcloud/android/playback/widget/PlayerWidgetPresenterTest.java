package com.soundcloud.android.playback.widget;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.TestPropertySets;
import com.soundcloud.android.playback.service.PlayerAppWidgetProvider;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

@RunWith(SoundCloudTestRunner.class)
public class PlayerWidgetPresenterTest {
    @Mock
    private AppWidgetManager appWidgetManager;
    private PlayerWidgetPresenter presenter;
    private Context context;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.application;
        presenter = new PlayerWidgetPresenter(appWidgetManager);
    }

    @Test
    public void shouldUpdateWidgetUsingPlayerAppWidgetProviderWhenPlayStateChange() throws Exception {
        presenter.updatePlayState(context, true);

        verifyUpdateViaPlayBackWidgetProvider();
    }

    @Test
    public void shouldUpdateTrackUsingPlayerAppWidgetProviderWhenPlayableChange() throws Exception {
        presenter.updateTrackInformation(context, TestPropertySets.expectedTrackForWidget());

        verifyUpdateViaPlayBackWidgetProvider();
    }

    @Test
    public void shouldUpdateWidgetUsingPlayerAppWidgetProviderOnReset() throws Exception {
        presenter.reset(context);

        verifyUpdateViaPlayBackWidgetProvider();
    }

    private void verifyUpdateViaPlayBackWidgetProvider() {
        ComponentName expectedComponentName = new ComponentName("com.soundcloud.android", PlayerAppWidgetProvider.class.getCanonicalName());
        verify(appWidgetManager).updateAppWidget(eq(expectedComponentName), any(RemoteViews.class));
    }

}
