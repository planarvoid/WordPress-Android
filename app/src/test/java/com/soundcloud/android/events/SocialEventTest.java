package com.soundcloud.android.events;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SocialEventTest {
    @Test
    public void shouldCreateFollow() throws Exception {
        SocialEvent socialEvent = SocialEvent.createFollow("screen", 30);
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 0);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.userId, 30);
    }

    @Test
    public void shouldCreateLike() throws Exception {
        SocialEvent socialEvent = SocialEvent.createLike("screen", "resource", 30);
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 1);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.resource, "resource");
        assertEquals(socialEventAttributes.resourceId, 30);
    }

    @Test
    public void shouldCreateRepost() throws Exception {
        SocialEvent socialEvent = SocialEvent.createRepost("screen", "resource", 30);
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 2);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.resource, "resource");
        assertEquals(socialEventAttributes.resourceId, 30);
    }

    @Test
    public void shouldCreateAddToPlaylist() throws Exception {
        SocialEvent socialEvent = SocialEvent.createAddToPlaylist("screen", true, 30);
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 3);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.isNewPlaylist, true);
        assertEquals(socialEventAttributes.trackId, 30);
    }

    @Test
    public void shouldCreateComment() throws Exception {
        SocialEvent socialEvent = SocialEvent.createComment("screen", 30);
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 4);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.trackId, 30);
    }

    @Test
    public void shouldCreateShare() throws Exception {
        SocialEvent socialEvent = SocialEvent.createShare("screen", "resource", 30, "facebook");
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 5);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.resource, "resource");
        assertEquals(socialEventAttributes.resourceId, 30);
        assertEquals(socialEventAttributes.sharedTo, "facebook");
    }
}
