package com.soundcloud.android.events;

import static com.pivotallabs.greatexpectations.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PlayControlEventTest {

    private PlayControlEvent event;

    @Test
    public void shouldCreateEventFromPlayerSwipePrevious() {
        event = PlayControlEvent.playerSwipePrevious();
        expect(event.getAttributes().get("action")).toEqual("prev");
        expect(event.getAttributes().get("click or swipe")).toEqual("swipe");
        expect(event.getAttributes().get("location")).toEqual("player");
    }

    @Test
    public void shouldCreateEventFromPlayerSwipeSkip() {
        event = PlayControlEvent.playerSwipeSkip();
        expect(event.getAttributes().get("action")).toEqual("skip");
        expect(event.getAttributes().get("click or swipe")).toEqual("swipe");
        expect(event.getAttributes().get("location")).toEqual("player");
    }

    @Test
    public void shouldCreateEventFromPlayerClickPrevious() {
        event = PlayControlEvent.playerClickPrevious();
        expect(event.getAttributes().get("action")).toEqual("prev");
        expect(event.getAttributes().get("click or swipe")).toEqual("click");
        expect(event.getAttributes().get("location")).toEqual("player");
    }

    @Test
    public void shouldCreateEventFromPlayerClickSkip() {
        event = PlayControlEvent.playerClickSkip();
        expect(event.getAttributes().get("action")).toEqual("skip");
        expect(event.getAttributes().get("click or swipe")).toEqual("click");
        expect(event.getAttributes().get("location")).toEqual("player");
    }

    @Test
    public void shouldCreateEventFromPlayerClickPause() {
        event = PlayControlEvent.playerClickPause();
        expect(event.getAttributes().get("action")).toEqual("pause");
        expect(event.getAttributes().get("click or swipe")).toEqual("click");
        expect(event.getAttributes().get("location")).toEqual("player");
    }

    @Test
    public void shouldCreateEventFromPlayerClickPlay() {
        event = PlayControlEvent.playerClickPlay();
        expect(event.getAttributes().get("action")).toEqual("play");
        expect(event.getAttributes().get("click or swipe")).toEqual("click");
        expect(event.getAttributes().get("location")).toEqual("player");
    }

    @Test
    public void shouldCreateEventFromPlayIntentWithSource() {
        event = PlayControlEvent.play("widget");
        expect(event.getAttributes().get("action")).toEqual("play");
        expect(event.getAttributes().get("click or swipe")).toEqual("click");
        expect(event.getAttributes().get("location")).toEqual("widget");
    }

    @Test
    public void shouldCreateEventFromPauseIntentWithSource() {
        event = PlayControlEvent.pause("notification");
        expect(event.getAttributes().get("action")).toEqual("pause");
        expect(event.getAttributes().get("click or swipe")).toEqual("click");
        expect(event.getAttributes().get("location")).toEqual("notification");
    }

    @Test
    public void shouldCreateEventFromSkipIntentWithSource() {
        event = PlayControlEvent.skip("notification");
        expect(event.getAttributes().get("action")).toEqual("skip");
        expect(event.getAttributes().get("click or swipe")).toEqual("click");
        expect(event.getAttributes().get("location")).toEqual("notification");
    }

    @Test
    public void shouldCreateEventFromPreviousIntentWithSource() {
        event = PlayControlEvent.previous("widget");
        expect(event.getAttributes().get("action")).toEqual("prev");
        expect(event.getAttributes().get("click or swipe")).toEqual("click");
        expect(event.getAttributes().get("location")).toEqual("widget");
    }

    @Test
    public void shouldCreateEventFromScrubWithSource() {
        event = PlayControlEvent.scrub("widget");
        expect(event.getAttributes().get("action")).toEqual("scrub");
        expect(event.getAttributes().get("location")).toEqual("widget");
    }

}
