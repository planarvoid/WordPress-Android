package com.soundcloud.android.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SharingTest {

    @Test
    public void shouldResolveToString() {
        assertThat(Sharing.PRIVATE.value).isEqualTo("private");
        assertThat(Sharing.PUBLIC.value).isEqualTo("public");
    }

    @Test
    public void shouldResolveFromString() {
        assertThat(Sharing.from("private")).isEqualTo(Sharing.PRIVATE);
        assertThat(Sharing.from("public")).isEqualTo(Sharing.PUBLIC);
        assertThat(Sharing.from("abc")).isEqualTo(Sharing.UNDEFINED);
    }

    @Test
    public void shouldResolvePrivateToBoolean() {
        assertThat(Sharing.PRIVATE.isPrivate()).isTrue();
        assertThat(Sharing.PRIVATE.isPublic()).isFalse();
    }

    @Test
    public void shouldResolvePublicToBoolean() {
        assertThat(Sharing.PUBLIC.isPrivate()).isFalse();
        assertThat(Sharing.PUBLIC.isPublic()).isTrue();
    }

    @Test
    public void shouldResolveUndefinedToBoolean() {
        assertThat(Sharing.UNDEFINED.isPrivate()).isFalse();
        assertThat(Sharing.UNDEFINED.isPublic()).isFalse();
    }

    @Test
    public void shouldResolveToPublicOrPrivateFromBoolean() {
        assertThat(Sharing.from(true)).isEqualTo(Sharing.PUBLIC);
        assertThat(Sharing.from(false)).isEqualTo(Sharing.PRIVATE);
    }
}
