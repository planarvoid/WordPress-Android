package com.soundcloud.android.main;

import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.Sets;
import org.junit.Test;

import android.content.Intent;
import android.os.Bundle;

import java.util.List;

public class ScreenTest extends AndroidUnitTest {

    @Test
    public void shouldGetTrackingTag() {
        assertThat(Screen.DISCOVER.get()).isEqualTo("discovery:main");
    }

    @Test
    public void shouldGetTrackingTagWithAppendedPath() {
        assertThat(Screen.DISCOVER.get("path")).isEqualTo("discovery:main:path");
    }

    @Test
    public void gettingTagWithAppendedPathShouldNormalizePath() {
        assertThat(Screen.DISCOVER.get("Hello & World")).isEqualTo("discovery:main:hello_&_world");
    }

    @Test
    public void getsTheScreenFromTheTrackingOrdinal() {
        assertEquals(Screen.ACTIVITIES, Screen.fromTrackingOrdinal(Screen.ACTIVITIES.trackingOrdinal()));
    }

    @Test
    public void returnsUnknownWhenTheTrackingOrdinalDoesNotMapToADefinedScreen() {
        assertEquals(Screen.UNKNOWN, Screen.fromTrackingOrdinal(Integer.MAX_VALUE));
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

    @Test
    public void allTrackingOrdinalsAreUnique() {
        List<Integer> trackingOrdinals = Lists.transform(Lists.newArrayList(Screen.values()), Screen::trackingOrdinal);
        assertEquals(Sets.newHashSet(trackingOrdinals).size(), Screen.values().length);
    }

}
