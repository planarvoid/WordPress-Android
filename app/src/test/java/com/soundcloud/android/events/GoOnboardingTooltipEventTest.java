package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class GoOnboardingTooltipEventTest {

    @Test
    public void shouldHaveTooltipName() {
        assertThat(GoOnboardingTooltipEvent.forListenOfflineLikes().tooltipName()).isEqualTo("listen_offline_likes");
        assertThat(GoOnboardingTooltipEvent.forListenOfflinePlaylist().tooltipName()).isEqualTo("listen_offline_playlist");
        assertThat(GoOnboardingTooltipEvent.forSearchGoPlus().tooltipName()).isEqualTo("search_go_plus");
        assertThat(GoOnboardingTooltipEvent.forOfflineSettings().tooltipName()).isEqualTo("offline_settings");
    }
}
