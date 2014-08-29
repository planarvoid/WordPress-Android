package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.users.UserUrn;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class TrackPageListenerTest {

    @Mock private PlaybackOperations playbackOperations;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private SoundAssociationOperations soundAssociationOperations;
    @Mock private PlayQueueManager playQueueManager;

    private TestEventBus eventBus = new TestEventBus();

    private TrackPageListener listener;

    @Before
    public void setUp() throws Exception {
        listener = new TrackPageListener(playbackOperations,
                soundAssociationOperations, playQueueManager, eventBus);
    }

    @Test
    public void onTogglePlayTogglesPlaybackViaPlaybackOperations() {
        listener.onTogglePlay();
        verify(playbackOperations).togglePlayback();
    }

    @Test
    public void onToggleLikeTogglesLikeViaAssociationOperations() {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(Urn.forTrack(123L));
        when(soundAssociationOperations.toggleLike(any(TrackUrn.class), anyBoolean())).thenReturn(Observable.<PropertySet>empty());

        listener.onToggleLike(true);

        verify(soundAssociationOperations).toggleLike(Urn.forTrack(123L), true);
    }

    @Test
    public void onFooterTapPostsEventToExpandPlayer() {
        listener.onFooterTap();

        PlayerUICommand event = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        expect(event.isExpand()).toBeTrue();
    }

    @Test
    public void onPlayerClosePostsEventToClosePlayer() {
        listener.onPlayerClose();

        PlayerUICommand event = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        expect(event.isCollapse()).toBeTrue();
    }

    @Test
    public void onGotoUserPostsEventToClosePlayer() throws Exception {
        UserUrn userUrn = Urn.forUser(42L);

        listener.onGotoUser(Robolectric.application, userUrn);

        PlayerUICommand event = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        expect(event.isCollapse()).toBeTrue();
    }

    @Test
    public void shouldStartProfileActivityOnGotoUserAfterPlayerUICollapsed() throws Exception {
        UserUrn userUrn = Urn.forUser(42L);

        listener.onGotoUser(Robolectric.application, userUrn);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        final Intent nextStartedActivity = Robolectric.shadowOf(Robolectric.application).getNextStartedActivity();
        expect(nextStartedActivity).not.toBeNull();
        expect(nextStartedActivity.getComponent().getClassName()).toEqual(ProfileActivity.class.getCanonicalName());
        expect(nextStartedActivity.getExtras().get("userUrn")).toEqual(UserUrn.forUser(42L));
    }
}