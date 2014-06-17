package com.soundcloud.android.actionbar;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class NowPlayingActionBarControllerTest {

    @Mock
    private ScActivity activity;

    private TestEventBus eventBus = new TestEventBus();

    private NowPlayingActionBarController actionBarController;

    @Before
    public void setUp() throws Exception {
        when(activity.getActivity()).thenReturn(activity);
        when(activity.getLayoutInflater()).thenReturn(
                (LayoutInflater) Robolectric.application.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        when(activity.getSupportActionBar()).thenReturn(mock(ActionBar.class));
        actionBarController = new NowPlayingActionBarController(activity, eventBus);
    }

    @Test
    public void shouldPublishPlayerShortcutEventOnShortcutClick() {
        actionBarController.onClick(mock(View.class));

        UIEvent uiEvent = eventBus.firstEventOn(EventQueue.UI);
        expect(uiEvent.getKind()).toBe(UIEvent.NAVIGATION);
        expect(uiEvent.getAttributes().get("page")).toEqual("player_shortcut");
    }

}
