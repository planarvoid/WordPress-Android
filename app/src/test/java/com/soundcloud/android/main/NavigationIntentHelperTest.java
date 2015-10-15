package com.soundcloud.android.main;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

import android.net.Uri;

public class NavigationIntentHelperTest extends AndroidUnitTest {

    @Test
    public void goToStreamForWebUrl() {
        Uri uri = Uri.parse("https://soundcloud.com/stream");

        assertThat(NavigationIntentHelper.shouldGoToStream(uri)).isTrue();
    }

    @Test
    public void goToStreamForStreamAsHost() {
        Uri uri = Uri.parse("soundcloud://stream");

        assertThat(NavigationIntentHelper.shouldGoToStream(uri)).isTrue();
    }

    @Test
    public void goToStreamForHome() {
        Uri uri = Uri.parse("https://soundcloud.com");

        assertThat(NavigationIntentHelper.shouldGoToStream(uri)).isTrue();
    }

    @Test
    public void doNotGoToStreamForOtherPath() {
        Uri uri = Uri.parse("https://soundcloud.com/explore");

        assertThat(NavigationIntentHelper.shouldGoToStream(uri)).isFalse();
    }

    @Test
    public void goToExploreForWebUrl() {
        Uri uri = Uri.parse("https://soundcloud.com/explore");

        assertThat(NavigationIntentHelper.shoudGoToExplore(uri)).isTrue();
    }

    @Test
    public void doNotGoToExploreForOtherPath() {
        Uri uri = Uri.parse("https://soundcloud.com/stream");

        assertThat(NavigationIntentHelper.shoudGoToExplore(uri)).isFalse();
    }

    @Test
    public void goToSearchForWebUrl() {
        Uri uri = Uri.parse("https://soundcloud.com/search");

        assertThat(NavigationIntentHelper.shouldGoToSearch(uri)).isTrue();
    }

    @Test
    public void doNotGoToSearchForOtherPath() {
        Uri uri = Uri.parse("https://soundcloud.com/stream");

        assertThat(NavigationIntentHelper.shouldGoToSearch(uri)).isFalse();
    }

}
