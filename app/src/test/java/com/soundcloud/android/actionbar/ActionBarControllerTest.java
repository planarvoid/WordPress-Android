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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v7.app.ActionBar;
import android.view.MenuItem;

@RunWith(SoundCloudTestRunner.class)
public class ActionBarControllerTest {

    @Mock
    private ScActivity activity;

    private TestEventBus eventBus = new TestEventBus();

    private ActionBarController actionBarController;

    @Before
    public void setUp() throws Exception {
        when(activity.getActivity()).thenReturn(activity);
        actionBarController = new ActionBarController(activity, eventBus);
    }

    @Test
    public void shouldPublishSearchUIEventOnSearchActionClick() {
        MenuItem item = mock(MenuItem.class);
        when(item.getItemId()).thenReturn(R.id.action_search);

        actionBarController.onOptionsItemSelected(item);

        UIEvent uiEvent = eventBus.firstEventOn(EventQueue.UI_TRACKING);
        expect(uiEvent.getKind()).toBe(UIEvent.Kind.NAVIGATION);
        expect(uiEvent.getAttributes().get("page")).toEqual("search");
    }

    @Test
    public void shouldShowActionBarOnVisibilitySetToTrue() throws Exception {
        ActionBar actionBar = mock(ActionBar.class);
        when(activity.getSupportActionBar()).thenReturn(actionBar);

        actionBarController.setVisible(true);

        verify(actionBar).show();
    }

    @Test
    public void shouldHideActionBarOnVisibilitySetToFalse() throws Exception {
        ActionBar actionBar = mock(ActionBar.class);
        when(activity.getSupportActionBar()).thenReturn(actionBar);

        actionBarController.setVisible(false);

        verify(actionBar).hide();
    }
}
