package com.soundcloud.android.actionbar;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.view.MenuItem;

@RunWith(SoundCloudTestRunner.class)
public class ActionBarControllerTest {

    @Mock
    private ScActivity activity;
    @Mock
    private EventBus eventBus;

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

        ArgumentCaptor<UIEvent> captor = ArgumentCaptor.forClass(UIEvent.class);
        verify(eventBus).publish(eq(EventQueue.UI), captor.capture());
        UIEvent uiEvent = captor.getValue();
        expect(uiEvent.getKind()).toBe(UIEvent.NAVIGATION);
        expect(uiEvent.getAttributes().get("page")).toEqual("search");
    }

}
