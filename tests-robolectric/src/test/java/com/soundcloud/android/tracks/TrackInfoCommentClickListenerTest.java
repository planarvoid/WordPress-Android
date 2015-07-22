package com.soundcloud.android.tracks;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v4.app.FragmentActivity;

@RunWith(SoundCloudTestRunner.class)
public class TrackInfoCommentClickListenerTest {

    private TrackInfoFragment.TrackInfoCommentClickListener listener;
    private TestEventBus eventBus = new TestEventBus();
    private PropertySet track = TestPropertySets.expectedTrackForPlayer();

    @Mock TrackInfoFragment fragment;

    @Before
    public void setUp() throws Exception {
        listener = new TrackInfoFragment.TrackInfoCommentClickListener(fragment, eventBus, track);
        when(fragment.getActivity()).thenReturn(new FragmentActivity());
    }

    @Test
    public void onCommentClickedDismissesDialog() throws Exception {
        listener.onCommentsClicked();
        verify(fragment).dismiss();
    }

    @Test
    public void onCommentCLickedSendsPlayerCloseEvent() throws Exception {
        listener.onCommentsClicked();
        Robolectric.runUiThreadTasksIncludingDelayedTasks();
        expect(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isCollapse()).toBeTrue();
    }

    @Test
    public void playerCollapsedEventAfterOnCommentsClickStartsTrackInfoCommentActivity() throws Exception {
        listener.onCommentsClicked();
        Robolectric.runUiThreadTasksIncludingDelayedTasks();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        expect(Robolectric.getShadowApplication().getNextStartedActivity()).not.toBeNull();
    }
}