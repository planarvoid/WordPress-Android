package com.soundcloud.android.actionbar;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.utils.BugReporter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v7.app.ActionBar;
import android.view.MenuItem;

@RunWith(SoundCloudTestRunner.class)
public class ActionBarControllerTest {
    @Mock private ScActivity activity;
    @Mock private ActionBar actionBar;
    @Mock private BugReporter bugReporter;

    private TestEventBus eventBus = new TestEventBus();

    private ActionBarController actionBarController;

    @Before
    public void setUp() throws Exception {
        actionBarController = new ActionBarController(eventBus, bugReporter);
    }

    @Test
    public void shouldPublishSearchUIEventOnSearchActionClick() {
        MenuItem item = mock(MenuItem.class);
        when(item.getItemId()).thenReturn(R.id.action_search);

        actionBarController.onOptionsItemSelected(activity, item);

        UIEvent uiEvent = (UIEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        expect(uiEvent.getKind()).toBe(UIEvent.KIND_NAVIGATION);
        expect(uiEvent.getAttributes().get("page")).toEqual("search");
    }

    @Test
    public void showsFeedbackDialogOnFeedbackItemSelected() {
        MenuItem item = mock(MenuItem.class);
        when(item.getItemId()).thenReturn(R.id.action_feedback);

        actionBarController.onOptionsItemSelected(activity, item);

        verify(bugReporter).showGeneralFeedbackDialog(activity);
    }
}
