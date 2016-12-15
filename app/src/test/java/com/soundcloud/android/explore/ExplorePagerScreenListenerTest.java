package com.soundcloud.android.explore;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Test;

public class ExplorePagerScreenListenerTest {

    private TestEventBus eventBus = new TestEventBus();

    @Test
    public void shouldTrackGenresScreenOnPageSelected() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(0);
        ScreenEvent event = (ScreenEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(event.screen()).isEqualTo("explore:genres");
    }

    @Test
    public void shouldTrackTrendingMusicScreenOnPageSelected() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(1);
        ScreenEvent event = (ScreenEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(event.screen()).isEqualTo("explore:trending_music");
    }

    @Test
    public void shouldTrackTrendingAudioScreenOnPageSelected() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(2);
        ScreenEvent event = (ScreenEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(event.screen()).isEqualTo("explore:trending_audio");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfUnknownScreenIsViewed() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(3);
    }

}
