package com.soundcloud.android.main;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

import android.content.Intent;
import android.os.Bundle;

public class ScreenTest extends AndroidUnitTest {

    @Test
    public void shouldGetTrackingTag() {
        assertThat(Screen.AUDIO_GENRES.get()).isEqualTo("charts:audio_genres");
    }

    @Test
    public void shouldGetTrackingTagWithAppendedPath() {
        assertThat(Screen.AUDIO_GENRES.get("path")).isEqualTo("charts:audio_genres:path");
    }

    @Test
    public void gettingTagWithAppendedPathShouldNormalizePath() {
        assertThat(Screen.AUDIO_GENRES.get("Hello & World")).isEqualTo("charts:audio_genres:hello_&_world");
    }

    @Test
    public void setsAndGetsScreenFromIntent() {
        final Intent intent = new Intent();
        Screen.ACTIVITIES.addToIntent(intent);
        assertThat(Screen.fromIntent(intent)).isEqualTo(Screen.ACTIVITIES);
    }

    @Test
    public void setsAndGetsScreenFromBundle() {
        final Bundle bundle = new Bundle();
        Screen.ACTIVITIES.addToBundle(bundle);
        assertThat(Screen.fromBundle(bundle)).isEqualTo(Screen.ACTIVITIES);
    }

}
