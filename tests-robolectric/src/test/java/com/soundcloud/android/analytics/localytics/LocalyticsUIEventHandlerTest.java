package com.soundcloud.android.analytics.localytics;


import static org.mockito.Mockito.verify;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class LocalyticsUIEventHandlerTest {

    private static final Urn TRACK_URN = Urn.forTrack(30L);

    private LocalyticsUIEventHandler localyticsUIEventHandler;

    @Mock private LocalyticsSession localyticsSession;

    @Before
    public void setUp() throws Exception {
        localyticsUIEventHandler = new LocalyticsUIEventHandler(localyticsSession);
    }

    @Test
    public void shouldHandleEventFollow() {
        UIEvent event = UIEvent.fromToggleFollow(true, "screen", 30L);
        localyticsUIEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Follow", event.getAttributes());
    }

    @Test
    public void shouldHandleEventLike() {
        UIEvent event = UIEvent.fromToggleLike(true, "invoker_screen", "context_screen", TRACK_URN);
        localyticsUIEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Like", event.getAttributes());
    }

    @Test
    public void shouldHandleEventUnlike() {
        UIEvent event = UIEvent.fromToggleLike(false, "invoker_screen", "context_screen", TRACK_URN);
        localyticsUIEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Unlike", event.getAttributes());
    }

    @Test
    public void shouldHandleEventRepost() {
        UIEvent event = UIEvent.fromToggleRepost(true, "screen", TRACK_URN);
        localyticsUIEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Repost", event.getAttributes());
    }

    @Test
    public void shouldHandleEventAddToPlaylist() {
        UIEvent event = UIEvent.fromAddToPlaylist("invoker_screen", "context_screen", true, 30L);
        localyticsUIEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Add to playlist", event.getAttributes());
    }

    @Test
    public void shouldHandleEventComment() {
        UIEvent event = UIEvent.fromComment("screen", 30L);
        localyticsUIEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Comment", event.getAttributes());
    }

    @Test
    public void shouldHandleEventShare() {
        UIEvent event = UIEvent.fromShare("screen", TRACK_URN);
        localyticsUIEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Share", event.getAttributes());
    }

}
