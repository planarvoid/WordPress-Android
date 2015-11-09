package com.soundcloud.android.deeplinks;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

import android.net.Uri;

public class ResolveActivityTest extends AndroidUnitTest {

    @Test
    public void acceptsSoundCloudScheme() {
        assertThat(ResolveActivity.accept(Uri.parse("soundcloud:something:123"), resources())).isTrue();
    }

    @Test
    public void doesNotAcceptOtherScheme() {
        assertThat(ResolveActivity.accept(Uri.parse("dubstep:something:123"), resources())).isFalse();
    }

    @Test
    public void acceptsSoundCloudHost() {
        assertThat(ResolveActivity.accept(Uri.parse("http://www.soundcloud.com"), resources())).isTrue();
    }

    @Test
    public void doesNotAcceptOtherHost() {
        assertThat(ResolveActivity.accept(Uri.parse("http://www.asdf.com"), resources())).isFalse();
    }
}
