package com.soundcloud.android.main;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

import android.content.Intent;
import android.os.Bundle;

public class ScreenTest extends AndroidUnitTest {

    @Test
    public void shouldGetTrackingTag() {
        assertThat(Screen.EXPLORE_GENRES.get()).isEqualTo("explore:genres");
    }

    @Test
    public void shouldGetTrackingTagWithAppendedPath() {
        assertThat(Screen.EXPLORE_GENRES.get("path")).isEqualTo("explore:genres:path");
    }

    @Test
    public void gettingTagWithAppendedPathShouldNormalizePath() {
        assertThat(Screen.EXPLORE_GENRES.get("Hello & World")).isEqualTo("explore:genres:hello_&_world");
    }

    @Test
    public void setsAndGetsScreenFromIntent(){
        final Intent intent = new Intent();
        Screen.ACTIVITIES.addToIntent(intent);
        assertThat(Screen.fromIntent(intent)).isEqualTo(Screen.ACTIVITIES);
    }

    @Test
    public void setsAndGetsScreenFromBundle(){
        final Bundle bundle = new Bundle();
        Screen.ACTIVITIES.addToBundle(bundle);
        assertThat(Screen.fromBundle(bundle)).isEqualTo(Screen.ACTIVITIES);
    }

}
