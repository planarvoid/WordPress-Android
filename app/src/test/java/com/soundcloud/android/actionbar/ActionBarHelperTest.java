package com.soundcloud.android.actionbar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.BugReporter;
import com.soundcloud.android.utils.DeviceHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class ActionBarHelperTest extends AndroidUnitTest {
    @Mock private ScActivity activity;
    @Mock private ActionBar actionBar;
    @Mock private BugReporter bugReporter;
    @Mock private Navigator navigator;
    @Mock private CastConnectionHelper castConnectionHelper;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private DeviceHelper deviceHelper;
    @Mock private Menu menu;
    @Mock private MenuItem item;

    private TestEventBus eventBus = new TestEventBus();

    private ActionBarHelper actionBarHelper;

    @Before
    public void setUp() throws Exception {
        actionBarHelper = new ActionBarHelper(castConnectionHelper, eventBus, applicationProperties, bugReporter, navigator, deviceHelper);
    }

    @Test
    public void shouldPublishSearchUIEventOnSearchActionClick() {
        when(item.getItemId()).thenReturn(R.id.action_search);

        actionBarHelper.onOptionsItemSelected(activity, item);

        UIEvent uiEvent = (UIEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_NAVIGATION);
        assertThat(uiEvent.getAttributes().get("page")).isEqualTo("search");
    }

    @Test
    public void showsFeedbackDialogOnFeedbackItemSelected() {
        when(item.getItemId()).thenReturn(R.id.action_feedback);

        actionBarHelper.onOptionsItemSelected(activity, item);

        verify(bugReporter).showGeneralFeedbackDialog(activity);
    }

    @Test
    public void hidesRecordButtonWhenNoMicrophoneIsDetected() {
        when(deviceHelper.hasMicrophone()).thenReturn(false);
        when(menu.findItem(R.id.action_record)).thenReturn(item);

        actionBarHelper.onCreateOptionsMenu(menu, mock(MenuInflater.class));

        verify(item).setVisible(false);
    }

    @Test
    public void doesNothideRecordButtonWhenMicrophoneIsDetected() {
        when(deviceHelper.hasMicrophone()).thenReturn(true);
        when(menu.findItem(R.id.action_record)).thenReturn(item);

        actionBarHelper.onCreateOptionsMenu(menu, mock(MenuInflater.class));

        verify(item, never()).setVisible(false);
    }

}
