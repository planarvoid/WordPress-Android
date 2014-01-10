package com.soundcloud.android.analytics.localytics;


import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.SocialEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class LocalyticsSocialEventHandlerTest {

    private LocalyticsSocialEventHandler localyticsSocialEventHandler;

    @Mock
    private LocalyticsSession localyticsSession;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        localyticsSocialEventHandler = new LocalyticsSocialEventHandler(localyticsSession);
    }

    @Test
    public void shouldHandleEventFollow() throws Exception {
        SocialEvent event = SocialEvent.fromFollow("screen", 30L);
        localyticsSocialEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Follow", event.getAttributes());
    }

    @Test
    public void shouldHandleEventUnfollow() throws Exception {
        SocialEvent event = SocialEvent.fromUnfollow("screen", 30L);
        localyticsSocialEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Unfollow", event.getAttributes());
    }

    @Test
    public void shouldHandleEventLike() throws Exception {
        SocialEvent event = SocialEvent.fromLike("screen", new Track(30L));
        localyticsSocialEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Like", event.getAttributes());
    }

    @Test
    public void shouldHandleEventUnlike() throws Exception {
        SocialEvent event = SocialEvent.fromUnlike("screen", new Track(30L));
        localyticsSocialEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Unlike", event.getAttributes());
    }

    @Test
    public void shouldHandleEventRepost() throws Exception {
        SocialEvent event = SocialEvent.fromRepost("screen", new Track(30L));
        localyticsSocialEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Repost", event.getAttributes());
    }

    @Test
    public void shouldHandleEventUnrepost() throws Exception {
        SocialEvent event = SocialEvent.fromUnrepost("screen", new Track(30L));
        localyticsSocialEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Unrepost", event.getAttributes());
    }

    @Test
    public void shouldHandleEventAddToPlaylist() throws Exception {
        SocialEvent event = SocialEvent.fromAddToPlaylist("screen", true, 30L);
        localyticsSocialEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Add to playlist", event.getAttributes());
    }

    @Test
    public void shouldHandleEventComment() throws Exception {
        SocialEvent event = SocialEvent.fromComment("screen", 30L);
        localyticsSocialEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Comment", event.getAttributes());
    }

    @Test
    public void shouldHandleEventShare() throws Exception {
        SocialEvent event = SocialEvent.fromShare("screen", new Track(30L));
        localyticsSocialEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Share", event.getAttributes());
    }
}
