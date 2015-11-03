package com.soundcloud.android.activities;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ActivityKindTest {

    @Test
    public void shouldResolveConstantFromValidIdentifier() {
        assertThat(ActivityKind.fromIdentifier("user_follow")).isEqualTo(ActivityKind.USER_FOLLOW);
    }

    @Test
    public void shouldResolveToUnknownFromInvalidIdentifier() {
        assertThat(ActivityKind.fromIdentifier("unsupported")).isEqualTo(ActivityKind.UNKNOWN);
    }
}
