package com.soundcloud.android.analytics.localytics;


import static org.mockito.Mockito.verify;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class LocalyticsUIEventHandlerTest {

    private LocalyticsUIEventHandler localyticsUIEventHandler;

    @Mock
    private LocalyticsSession localyticsSession;

    @Before
    public void setUp() throws Exception {
        localyticsUIEventHandler = new LocalyticsUIEventHandler(localyticsSession);
    }

    @Test
    public void shouldHandleEventFollow() throws Exception {
        UIEvent event = UIEvent.fromFollow("screen", 30L);
        localyticsUIEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Follow", event.getAttributes());
    }

    @Test
    public void shouldHandleEventUnfollow() throws Exception {
        UIEvent event = UIEvent.fromUnfollow("screen", 30L);
        localyticsUIEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Unfollow", event.getAttributes());
    }

    @Test
    public void shouldHandleEventLike() throws Exception {
        UIEvent event = UIEvent.fromLike("screen", new Track(30L));
        localyticsUIEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Like", event.getAttributes());
    }

    @Test
    public void shouldHandleEventUnlike() throws Exception {
        UIEvent event = UIEvent.fromUnlike("screen", new Track(30L));
        localyticsUIEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Unlike", event.getAttributes());
    }

    @Test
    public void shouldHandleEventRepost() throws Exception {
        UIEvent event = UIEvent.fromRepost("screen", new Track(30L));
        localyticsUIEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Repost", event.getAttributes());
    }

    @Test
    public void shouldHandleEventUnrepost() throws Exception {
        UIEvent event = UIEvent.fromUnrepost("screen", new Track(30L));
        localyticsUIEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Unrepost", event.getAttributes());
    }

    @Test
    public void shouldHandleEventAddToPlaylist() throws Exception {
        UIEvent event = UIEvent.fromAddToPlaylist("screen", true, 30L);
        localyticsUIEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Add to playlist", event.getAttributes());
    }

    @Test
    public void shouldHandleEventComment() throws Exception {
        UIEvent event = UIEvent.fromComment("screen", 30L);
        localyticsUIEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Comment", event.getAttributes());
    }

    @Test
    public void shouldHandleEventShare() throws Exception {
        UIEvent event = UIEvent.fromShare("screen", new Track(30L));
        localyticsUIEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Share", event.getAttributes());
    }
}
