package com.soundcloud.android.activities;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ActivityKindTest {

    @Test
    public void shouldResolveConstantFromValidIdentifier() {
        assertThat(ActivityKind.fromIdentifier("user_follow")).isEqualTo(ActivityKind.USER_FOLLOW);
    }

    @Test
    public void shouldSpecifySupportedTypeIdentifiersFromBackend() {
        final String[] supported = {"track_like", "playlist_like", "track_repost", "playlist_repost",
                "track_comment", "user_follow"};
        assertThat(ActivityKind.SUPPORTED_IDENTIFIERS).isEqualTo(supported);
    }
}
