package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import org.junit.Test;

public class GoOnboardingTooltipEventTest {
    @Test
    public void createsCollectionImpression() {
        GoOnboardingTooltipEvent event = GoOnboardingTooltipEvent.forCollectionImpression();

        assertThat(event.appboyEventName().isPresent()).isFalse();
        assertThat(event.pageName()).isEqualTo("collection:main");
        assertThat(event.pageUrn().isPresent()).isFalse();
        assertThat(event.impressionCategory()).isEqualTo("consumer_subs");
        assertThat(event.impressionName()).isEqualTo("tooltip::save_offline_content");
    }

    @Test
    public void createsListenOfflineLikesEvent() {
        GoOnboardingTooltipEvent event = GoOnboardingTooltipEvent.forListenOfflineLikes();

        assertThat(event.appboyEventName().get()).isEqualTo("subscription_tooltip_listen_offline_likes");
        assertThat(event.pageName()).isEqualTo("collection:likes");
        assertThat(event.pageUrn().isPresent()).isFalse();
        assertThat(event.impressionCategory()).isEqualTo("consumer_subs");
        assertThat(event.impressionName()).isEqualTo("tooltip::save_likes");
    }

    @Test
    public void createsListenOfflinePlaylistEvent() {
        GoOnboardingTooltipEvent event = GoOnboardingTooltipEvent.forListenOfflinePlaylist(Urn.forPlaylist(123L));

        assertThat(event.appboyEventName().get()).isEqualTo("subscription_tooltip_listen_offline_playlist");
        assertThat(event.pageName()).isEqualTo("playlists:main");
        assertThat(event.pageUrn().get()).isEqualTo("soundcloud:playlists:123");
        assertThat(event.impressionCategory()).isEqualTo("consumer_subs");
        assertThat(event.impressionName()).isEqualTo("tooltip::save_playlist_or_album");
    }

    @Test
    public void createsSearchGoPlusEvent() {
        GoOnboardingTooltipEvent event = GoOnboardingTooltipEvent.forSearchGoPlus();

        assertThat(event.appboyEventName().get()).isEqualTo("subscription_tooltip_search_go_plus");
        assertThat(event.pageName()).isEqualTo("search:main");
        assertThat(event.pageUrn().isPresent()).isFalse();
        assertThat(event.impressionCategory()).isEqualTo("consumer_subs");
        assertThat(event.impressionName()).isEqualTo("tooltip::go_plus_marker");
    }

    @Test
    public void createsOfflineSettingsEvent() {
        GoOnboardingTooltipEvent event = GoOnboardingTooltipEvent.forOfflineSettings();

        assertThat(event.appboyEventName().get()).isEqualTo("subscription_tooltip_offline_settings");
        assertThat(event.pageName()).isEqualTo("more:main");
        assertThat(event.pageUrn().isPresent()).isFalse();
        assertThat(event.impressionCategory()).isEqualTo("consumer_subs");
        assertThat(event.impressionName()).isEqualTo("tooltip::offline_settings");
    }
}
