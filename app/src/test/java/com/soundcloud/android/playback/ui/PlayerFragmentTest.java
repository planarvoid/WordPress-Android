package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playback.service.Playa.*;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ToggleButton;

@RunWith(SoundCloudTestRunner.class)
public class PlayerFragmentTest {

    private PlayerFragment fragment;

    @Mock
    private EventBus eventBus;
    @Mock
    private PlayQueueManager playQueueManager;
    @Mock
    private PlaybackOperations playbackOperations;

    @Before
    public void setUp() throws Exception {
        fragment = new PlayerFragment(eventBus, playQueueManager, playbackOperations);
    }

    @Test
    public void shouldTogglePlayButtonStateToEnableOnPlayingStateEvent() throws Exception {
        EventMonitor eventMonitor = EventMonitor.on(eventBus);
        createFragment();
        View layout = createFragmentView();
        fragment.onResume();

        StateTransition state = new StateTransition(PlayaState.PLAYING, Reason.NONE);
        eventMonitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);

        ToggleButton footerToggle = (ToggleButton) layout.findViewById(R.id.footer_toggle);
        expect(footerToggle.isChecked()).toBeTrue();
    }

    @Test
    public void shouldTogglePlayButtonStateToDisabledOnIdleStateEvent() throws Exception {
        EventMonitor eventMonitor = EventMonitor.on(eventBus);
        createFragment();
        View layout = createFragmentView();
        fragment.onResume();

        StateTransition state = new StateTransition(PlayaState.IDLE, Reason.NONE);
        eventMonitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);

        ToggleButton footerToggle = (ToggleButton) layout.findViewById(R.id.footer_toggle);
        expect(footerToggle.isChecked()).toBeFalse();
    }

    @Test
    public void shouldTogglePlaybackWhenToggleIsClicked() {
        View layout = createFragmentView();
        layout.findViewById(R.id.footer_toggle).performClick();

        verify(playbackOperations).togglePlayback(any(Activity.class));
    }

    @Test
    public void shouldUnsubscribeFromEventBusInOnPause() {
        EventMonitor eventMonitor = EventMonitor.on(eventBus);
        fragment.onCreate(null);
        fragment.onResume();
        fragment.onPause();
        eventMonitor.verifyUnsubscribed();
    }

    private void createFragment() {
        final FragmentActivity activity = new FragmentActivity();
        shadowOf(fragment).setActivity(activity);
        fragment.onCreate(null);
    }

    private View createFragmentView() {
        View fragmentLayout = fragment.onCreateView(LayoutInflater.from(Robolectric.application), new FrameLayout(Robolectric.application), null);
        Robolectric.shadowOf(fragment).setView(fragmentLayout);
        fragment.onViewCreated(fragmentLayout, null);
        return fragmentLayout;
    }

}