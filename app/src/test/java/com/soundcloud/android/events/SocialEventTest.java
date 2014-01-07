package com.soundcloud.android.events;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SocialEventTest {
    @Test
    public void shouldCreateEventFromFollow() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromFollow("screen", 30);
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 0);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.userId, 30);
    }

    @Test
    public void shouldCreateEventFromUnfollow() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromUnfollow("screen", 30);
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 1);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.userId, 30);
    }

    @Test
    public void shouldCreateEventFromLike() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromLike("screen", "resource", 30);
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 2);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.resource, "resource");
        assertEquals(socialEventAttributes.resourceId, 30);
    }

    @Test
    public void shouldCreateEventFromUnlike() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromUnlike("screen", "resource", 30);
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 3);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.resource, "resource");
        assertEquals(socialEventAttributes.resourceId, 30);
    }

    @Test
    public void shouldCreateEventFromRepost() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromRepost("screen", "resource", 30);
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 4);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.resource, "resource");
        assertEquals(socialEventAttributes.resourceId, 30);
    }

    @Test
    public void shouldCreateEventFromUnrepost() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromUnrepost("screen", "resource", 30);
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 5);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.resource, "resource");
        assertEquals(socialEventAttributes.resourceId, 30);
    }

    @Test
    public void shouldCreateEventFromAddToPlaylist() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromAddToPlaylist("screen", true, 30);
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 6);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.isNewPlaylist, true);
        assertEquals(socialEventAttributes.trackId, 30);
    }

    @Test
    public void shouldCreateEventFromComment() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromComment("screen", 30);
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 7);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.trackId, 30);
    }

    @Test
    public void shouldCreateEventFromShare() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromShare("screen", "resource", 30, "facebook");
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 8);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.resource, "resource");
        assertEquals(socialEventAttributes.resourceId, 30);
        assertEquals(socialEventAttributes.sharedTo, "facebook");
    }
}
