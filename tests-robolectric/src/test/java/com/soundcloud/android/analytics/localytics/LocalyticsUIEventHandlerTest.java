package com.soundcloud.android.analytics.localytics;


import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import static com.soundcloud.android.Expect.expect;
import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.LocalyticTrackingKeys;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class LocalyticsUIEventHandlerTest {

    private static final Urn TRACK_URN = Urn.forTrack(30L);

    private LocalyticsUIEventHandler localyticsUIEventHandler;

    @Mock private LocalyticsSession localyticsSession;
    @Captor ArgumentCaptor<Map<String, String>> attributeCaptor;

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
        UIEvent event = UIEvent.fromToggleLike(true, "invoker_screen", "context_screen", "page_name", TRACK_URN, Urn.NOT_SET, null);
        localyticsUIEventHandler.handleEvent(event);

        verify(localyticsSession).tagEvent(eq("Like"), attributeCaptor.capture());

        expect(attributeCaptor.getValue().get(LocalyticTrackingKeys.KEY_CONTEXT)).toEqual(event.get(LocalyticTrackingKeys.KEY_CONTEXT));
        expect(attributeCaptor.getValue().get(LocalyticTrackingKeys.KEY_LOCATION)).toEqual(event.get(LocalyticTrackingKeys.KEY_LOCATION));
        expect(attributeCaptor.getValue().get(LocalyticTrackingKeys.KEY_RESOURCE_ID)).toEqual(event.get(LocalyticTrackingKeys.KEY_RESOURCE_ID));
        expect(attributeCaptor.getValue().get(LocalyticTrackingKeys.KEY_RESOURCES_TYPE)).toEqual(event.get(LocalyticTrackingKeys.KEY_RESOURCES_TYPE));
        expect(attributeCaptor.getValue().size()).toEqual(4);
    }

    @Test
    public void shouldHandleEventUnlike() {
        UIEvent event = UIEvent.fromToggleLike(false, "invoker_screen", "context_screen", "page_name", TRACK_URN, Urn.NOT_SET, null);
        localyticsUIEventHandler.handleEvent(event);

        verify(localyticsSession).tagEvent(eq("Unlike"), attributeCaptor.capture());

        expect(attributeCaptor.getValue().get(LocalyticTrackingKeys.KEY_CONTEXT)).toEqual(event.get(LocalyticTrackingKeys.KEY_CONTEXT));
        expect(attributeCaptor.getValue().get(LocalyticTrackingKeys.KEY_LOCATION)).toEqual(event.get(LocalyticTrackingKeys.KEY_LOCATION));
        expect(attributeCaptor.getValue().get(LocalyticTrackingKeys.KEY_RESOURCE_ID)).toEqual(event.get(LocalyticTrackingKeys.KEY_RESOURCE_ID));
        expect(attributeCaptor.getValue().get(LocalyticTrackingKeys.KEY_RESOURCES_TYPE)).toEqual(event.get(LocalyticTrackingKeys.KEY_RESOURCES_TYPE));
        expect(attributeCaptor.getValue().size()).toEqual(4);
    }

    @Test
    public void shouldHandleEventRepost() {
        UIEvent event = UIEvent.fromToggleRepost(true, "screen", "page_name", TRACK_URN, Urn.NOT_SET, null);
        localyticsUIEventHandler.handleEvent(event);

        verify(localyticsSession).tagEvent(eq("Repost"), attributeCaptor.capture());

        expect(attributeCaptor.getValue().get(LocalyticTrackingKeys.KEY_CONTEXT)).toEqual(event.get(LocalyticTrackingKeys.KEY_CONTEXT));
        expect(attributeCaptor.getValue().get(LocalyticTrackingKeys.KEY_RESOURCE_ID)).toEqual(event.get(LocalyticTrackingKeys.KEY_RESOURCE_ID));
        expect(attributeCaptor.getValue().get(LocalyticTrackingKeys.KEY_RESOURCES_TYPE)).toEqual(event.get(LocalyticTrackingKeys.KEY_RESOURCES_TYPE));
        expect(attributeCaptor.getValue().size()).toEqual(3);
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
