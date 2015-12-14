package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PlayControlEventTest {

    private PlayControlEvent event;

    @Test
    public void shouldCreateEventFromExpandedPlayerSwipePrevious() {
        event = PlayControlEvent.swipePrevious(true);
        assertThat(event.getAttributes().get("action")).isEqualTo("prev");
        assertThat(event.getAttributes().get("tap or swipe")).isEqualTo("swipe");
        assertThat(event.getAttributes().get("location")).isEqualTo(PlayControlEvent.SOURCE_FULL_PLAYER);
    }

    @Test
    public void shouldCreateEventFromCollapsedPlayerSwipePrevious() {
        event = PlayControlEvent.swipePrevious(false);
        assertThat(event.getAttributes().get("action")).isEqualTo("prev");
        assertThat(event.getAttributes().get("tap or swipe")).isEqualTo("swipe");
        assertThat(event.getAttributes().get("location")).isEqualTo(PlayControlEvent.SOURCE_FOOTER_PLAYER);
    }

    @Test
    public void shouldCreateEventFromExpandedPlayerSwipeSkip() {
        event = PlayControlEvent.swipeSkip(true);
        assertThat(event.getAttributes().get("action")).isEqualTo("skip");
        assertThat(event.getAttributes().get("tap or swipe")).isEqualTo("swipe");
        assertThat(event.getAttributes().get("location")).isEqualTo(PlayControlEvent.SOURCE_FULL_PLAYER);
    }

    @Test
    public void shouldCreateEventFromCollapsedPlayerSwipeSkip() {
        event = PlayControlEvent.swipeSkip(false);
        assertThat(event.getAttributes().get("action")).isEqualTo("skip");
        assertThat(event.getAttributes().get("tap or swipe")).isEqualTo("swipe");
        assertThat(event.getAttributes().get("location")).isEqualTo(PlayControlEvent.SOURCE_FOOTER_PLAYER);
    }

    @Test
    public void shouldCreateEventFromSkipAd() {
        event = PlayControlEvent.skipAd();
        assertThat(event.getAttributes().get("action")).isEqualTo("skip_ad_button");
        assertThat(event.getAttributes().get("tap or swipe")).isEqualTo("tap");
        assertThat(event.getAttributes().get("location")).isEqualTo(PlayControlEvent.SOURCE_FULL_PLAYER);
    }

    @Test
    public void shouldCreateEventFromPlayIntentWithSource() {
        event = PlayControlEvent.play("widget");
        assertThat(event.getAttributes().get("action")).isEqualTo("play");
        assertThat(event.getAttributes().get("tap or swipe")).isEqualTo("tap");
        assertThat(event.getAttributes().get("location")).isEqualTo("widget");
    }

    @Test
    public void shouldCreateEventFromPauseIntentWithSource() {
        event = PlayControlEvent.pause("notification");
        assertThat(event.getAttributes().get("action")).isEqualTo("pause");
        assertThat(event.getAttributes().get("tap or swipe")).isEqualTo("tap");
        assertThat(event.getAttributes().get("location")).isEqualTo("notification");
    }

    @Test
    public void shouldCreateEventFromSkipIntentWithSource() {
        event = PlayControlEvent.skip("notification");
        assertThat(event.getAttributes().get("action")).isEqualTo("skip");
        assertThat(event.getAttributes().get("tap or swipe")).isEqualTo("tap");
        assertThat(event.getAttributes().get("location")).isEqualTo("notification");
    }

    @Test
    public void shouldCreateEventFromPreviousIntentWithSource() {
        event = PlayControlEvent.previous("widget");
        assertThat(event.getAttributes().get("action")).isEqualTo("prev");
        assertThat(event.getAttributes().get("tap or swipe")).isEqualTo("tap");
        assertThat(event.getAttributes().get("location")).isEqualTo("widget");
    }

    @Test
    public void shouldCreateEventFromScrubWithSource() {
        event = PlayControlEvent.scrub("widget");
        assertThat(event.getAttributes().get("action")).isEqualTo("scrub");
        assertThat(event.getAttributes().get("location")).isEqualTo("widget");
    }

}
