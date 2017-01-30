package com.soundcloud.android.tracks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.shadows.ShadowLooper;

import android.support.v4.app.FragmentActivity;

public class TrackInfoCommentClickListenerTest extends AndroidUnitTest {

    private TrackInfoFragment.TrackInfoCommentClickListener listener;
    private TestEventBus eventBus = new TestEventBus();
    private Urn trackUrn = Urn.forTrack(123);
    private FragmentActivity activity = new FragmentActivity();

    @Mock Navigator navigator;
    @Mock TrackInfoFragment fragment;

    @Before
    public void setUp() throws Exception {
        listener = new TrackInfoFragment.TrackInfoCommentClickListener(fragment, eventBus, trackUrn, navigator);

        when(fragment.getActivity()).thenReturn(activity);
    }

    @Test
    public void onCommentClickedDismissesDialog() throws Exception {
        listener.onCommentsClicked();
        verify(fragment).dismiss();
    }

    @Test
    public void onCommentCLickedSendsPlayerCloseEvent() throws Exception {
        listener.onCommentsClicked();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isAutomaticCollapse()).isTrue();
    }

    @Test
    public void playerCollapsedEventAfterOnCommentsClickStartsTrackInfoCommentActivity() throws Exception {
        listener.onCommentsClicked();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        verify(navigator).openTrackComments(activity, trackUrn);
    }
}
