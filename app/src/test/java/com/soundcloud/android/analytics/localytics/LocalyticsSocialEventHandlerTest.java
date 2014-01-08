package com.soundcloud.android.analytics.localytics;


import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.SocialEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.HashMap;

@RunWith(SoundCloudTestRunner.class)
public class LocalyticsSocialEventHandlerTest {

    private LocalyticsSocialEventHandler localyticsSocialEventHandler;

    private SocialEvent.Attributes sourceAttributes;
    private HashMap<String, String> mappedEventAttributes;

    @Mock
    private LocalyticsSession localyticsSession;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        localyticsSocialEventHandler = new LocalyticsSocialEventHandler(localyticsSession);
        sourceAttributes = new SocialEvent.Attributes();
        mappedEventAttributes = new HashMap<String, String>();
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldThrownExceptionForInvalidSocialEventType() throws Exception {
        localyticsSocialEventHandler.handleEvent(-1, sourceAttributes);
    }

    @Test
    public void shouldHandleEventFollow() throws Exception {
        sourceAttributes.screenTag = "screen";
        sourceAttributes.userId = 30L;
        localyticsSocialEventHandler.handleEventFollow(sourceAttributes, mappedEventAttributes);
        verify(localyticsSession).tagEvent("Follow", mappedEventAttributes);
        assertEquals(mappedEventAttributes.get("context"), "screen");
        assertEquals(mappedEventAttributes.get("user_id"), "30");
    }

    @Test
    public void shouldHandleEventUnfollow() throws Exception {
        sourceAttributes.screenTag = "screen";
        sourceAttributes.userId = 30L;
        localyticsSocialEventHandler.handleEventUnfollow(sourceAttributes, mappedEventAttributes);
        verify(localyticsSession).tagEvent("Unfollow", mappedEventAttributes);
        assertEquals(mappedEventAttributes.get("context"), "screen");
        assertEquals(mappedEventAttributes.get("user_id"), "30");
    }

    @Test
    public void shouldHandleEventLike() throws Exception {
        sourceAttributes.screenTag = "screen";
        sourceAttributes.resource = "resource";
        sourceAttributes.resourceId = 30;
        localyticsSocialEventHandler.handleEventLike(sourceAttributes, mappedEventAttributes);
        verify(localyticsSession).tagEvent("Like", mappedEventAttributes);
        assertEquals(mappedEventAttributes.get("context"), "screen");
        assertEquals(mappedEventAttributes.get("resource"), "resource");
        assertEquals(mappedEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldHandleEventUnlike() throws Exception {
        sourceAttributes.screenTag = "screen";
        sourceAttributes.resource = "resource";
        sourceAttributes.resourceId = 30;
        localyticsSocialEventHandler.handleEventUnlike(sourceAttributes, mappedEventAttributes);
        verify(localyticsSession).tagEvent("Unlike", mappedEventAttributes);
        assertEquals(mappedEventAttributes.get("context"), "screen");
        assertEquals(mappedEventAttributes.get("resource"), "resource");
        assertEquals(mappedEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldHandleEventRepost() throws Exception {
        sourceAttributes.screenTag = "screen";
        sourceAttributes.resource = "resource";
        sourceAttributes.resourceId = 30;
        localyticsSocialEventHandler.handleEventRepost(sourceAttributes, mappedEventAttributes);
        verify(localyticsSession).tagEvent("Repost", mappedEventAttributes);
        assertEquals(mappedEventAttributes.get("context"), "screen");
        assertEquals(mappedEventAttributes.get("resource"), "resource");
        assertEquals(mappedEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldHandleEventUnrepost() throws Exception {
        sourceAttributes.screenTag = "screen";
        sourceAttributes.resource = "resource";
        sourceAttributes.resourceId = 30;
        localyticsSocialEventHandler.handleEventUnrepost(sourceAttributes, mappedEventAttributes);
        verify(localyticsSession).tagEvent("Unrepost", mappedEventAttributes);
        assertEquals(mappedEventAttributes.get("context"), "screen");
        assertEquals(mappedEventAttributes.get("resource"), "resource");
        assertEquals(mappedEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldHandleEventAddToPlaylistWhenNewPlaylistIsTrue() throws Exception {
        sourceAttributes.screenTag = "screen";
        sourceAttributes.isNewPlaylist = true;
        sourceAttributes.trackId = 30;
        localyticsSocialEventHandler.handleEventAddToPlaylist(sourceAttributes, mappedEventAttributes);
        verify(localyticsSession).tagEvent("Add to playlist", mappedEventAttributes);
        assertEquals(mappedEventAttributes.get("context"), "screen");
        assertEquals(mappedEventAttributes.get("new_playlist"), "yes");
        assertEquals(mappedEventAttributes.get("track_id"), "30");
    }

    @Test
    public void shouldHandleEventAddToPlaylistWhenNewPlaylistIsFalse() throws Exception {
        sourceAttributes.screenTag = "screen";
        sourceAttributes.isNewPlaylist = false;
        sourceAttributes.trackId = 30;
        localyticsSocialEventHandler.handleEventAddToPlaylist(sourceAttributes, mappedEventAttributes);
        assertEquals(mappedEventAttributes.get("context"), "screen");
        assertEquals(mappedEventAttributes.get("new_playlist"), "no");
        assertEquals(mappedEventAttributes.get("track_id"), "30");
    }

    @Test
    public void shouldHandleEventComment() throws Exception {
        sourceAttributes.screenTag = "screen";
        sourceAttributes.trackId = 30;
        localyticsSocialEventHandler.handleEventComment(sourceAttributes, mappedEventAttributes);
        verify(localyticsSession).tagEvent("Comment", mappedEventAttributes);
        assertEquals(mappedEventAttributes.get("context"), "screen");
        assertEquals(mappedEventAttributes.get("track_id"), "30");
    }

    @Test
    public void shouldHandleEventShare() throws Exception {
        sourceAttributes.screenTag = "screen";
        sourceAttributes.resource = "resource";
        sourceAttributes.resourceId = 30;
        sourceAttributes.sharedTo = "facebook";
        localyticsSocialEventHandler.handleEventShare(sourceAttributes, mappedEventAttributes);
        verify(localyticsSession).tagEvent("Share", mappedEventAttributes);
        assertEquals(mappedEventAttributes.get("context"), "screen");
        assertEquals(mappedEventAttributes.get("resource"), "resource");
        assertEquals(mappedEventAttributes.get("resource_id"), "30");
        assertEquals(mappedEventAttributes.get("shared_to"), "facebook");
    }
}
