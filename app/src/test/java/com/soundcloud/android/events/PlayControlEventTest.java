package com.soundcloud.android.events;

import static com.pivotallabs.greatexpectations.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PlayControlEventTest {

    private PlayControlEvent event;

    @Test
    public void shouldCreateEventFromExpandedPlayerSwipePrevious() {
        event = PlayControlEvent.swipePrevious(true);
        expect(event.getAttributes().get("action")).toEqual("prev");
        expect(event.getAttributes().get("tap or swipe")).toEqual("swipe");
        expect(event.getAttributes().get("location")).toEqual(PlayControlEvent.SOURCE_FULL_PLAYER);
    }

    @Test
    public void shouldCreateEventFromCollapsedPlayerSwipePrevious() {
        event = PlayControlEvent.swipePrevious(false);
        expect(event.getAttributes().get("action")).toEqual("prev");
        expect(event.getAttributes().get("tap or swipe")).toEqual("swipe");
        expect(event.getAttributes().get("location")).toEqual(PlayControlEvent.SOURCE_FOOTER_PLAYER);
    }

    @Test
    public void shouldCreateEventFromExpandedPlayerSwipeSkip() {
        event = PlayControlEvent.swipeSkip(true);
        expect(event.getAttributes().get("action")).toEqual("skip");
        expect(event.getAttributes().get("tap or swipe")).toEqual("swipe");
        expect(event.getAttributes().get("location")).toEqual(PlayControlEvent.SOURCE_FULL_PLAYER);
    }

    @Test
    public void shouldCreateEventFromCollapsedPlayerSwipeSkip() {
        event = PlayControlEvent.swipeSkip(false);
        expect(event.getAttributes().get("action")).toEqual("skip");
        expect(event.getAttributes().get("tap or swipe")).toEqual("swipe");
        expect(event.getAttributes().get("location")).toEqual(PlayControlEvent.SOURCE_FOOTER_PLAYER);
    }

    @Test
    public void shouldCreateEventFromSkipAd() {
        event = PlayControlEvent.skipAd();
        expect(event.getAttributes().get("action")).toEqual("skip_ad_button");
        expect(event.getAttributes().get("tap or swipe")).toEqual("tap");
        expect(event.getAttributes().get("location")).toEqual(PlayControlEvent.SOURCE_FULL_PLAYER);
    }

    @Test
    public void shouldCreateEventFromPlayIntentWithSource() {
        event = PlayControlEvent.play("widget");
        expect(event.getAttributes().get("action")).toEqual("play");
        expect(event.getAttributes().get("tap or swipe")).toEqual("tap");
        expect(event.getAttributes().get("location")).toEqual("widget");
    }

    @Test
    public void shouldCreateEventFromPauseIntentWithSource() {
        event = PlayControlEvent.pause("notification");
        expect(event.getAttributes().get("action")).toEqual("pause");
        expect(event.getAttributes().get("tap or swipe")).toEqual("tap");
        expect(event.getAttributes().get("location")).toEqual("notification");
    }

    @Test
    public void shouldCreateEventFromSkipIntentWithSource() {
        event = PlayControlEvent.skip("notification");
        expect(event.getAttributes().get("action")).toEqual("skip");
        expect(event.getAttributes().get("tap or swipe")).toEqual("tap");
        expect(event.getAttributes().get("location")).toEqual("notification");
    }

    @Test
    public void shouldCreateEventFromPreviousIntentWithSource() {
        event = PlayControlEvent.previous("widget");
        expect(event.getAttributes().get("action")).toEqual("prev");
        expect(event.getAttributes().get("tap or swipe")).toEqual("tap");
        expect(event.getAttributes().get("location")).toEqual("widget");
    }

    @Test
    public void shouldCreateEventFromScrubWithSource() {
        event = PlayControlEvent.scrub("widget");
        expect(event.getAttributes().get("action")).toEqual("scrub");
        expect(event.getAttributes().get("location")).toEqual("widget");
    }

}
