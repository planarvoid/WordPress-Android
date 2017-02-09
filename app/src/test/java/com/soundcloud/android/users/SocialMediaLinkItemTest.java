package com.soundcloud.android.users;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;

import android.graphics.drawable.Drawable;

public class SocialMediaLinkItemTest extends AndroidUnitTest {

    @Test
    public void usesSpecificDrawableForKnownNetworks() {
        final SocialMediaLinkItem link = SocialMediaLinkItem.create(Optional.absent(), "myspace", "myspace.com/famous");
        assertThat(link.icon(context())).isInstanceOf(Drawable.class);
    }

    @Test
    public void fallsBackToGenericDrawableForUnknownNetworks() {
        final SocialMediaLinkItem link = SocialMediaLinkItem.create(Optional.absent(), "myspace", "myspace.com/famous");
        assertThat(link.icon(context())).isInstanceOf(Drawable.class);
    }

    @Test
    public void usesCustomDisplayNameWhenProvided() {
        final SocialMediaLinkItem link = SocialMediaLinkItem.create(Optional.of("Custom"), "myspace", "myspace.com/famous");
        assertThat(link.displayName()).isEqualTo("Custom");
    }

    @Test
    public void usesKnownNetworkAsDisplayNameWhenNoCustomDisplayNameProvided() {
        final SocialMediaLinkItem link = SocialMediaLinkItem.create(Optional.absent(), "myspace", "myspace.com/famous");
        assertThat(link.displayName()).isEqualTo("Myspace");
    }

    @Test
    public void usesUrlForDisplayNameWhenNoCustomDisplayNameProvidedAnUnknownNetwork() {
        final SocialMediaLinkItem link = SocialMediaLinkItem.create(Optional.absent(), "personal", "myspace.com/famous");
        assertThat(link.displayName()).isEqualTo("myspace.com/famous");
    }

    @Test
    public void stripsHttpAndWwwFromUrlForDisplayName() {
        assertThat(urlDisplayName("http://foo.com/bar?baz=quux")).isEqualTo("foo.com/bar?baz=quux");
        assertThat(urlDisplayName("https://foo.com/bar?baz=quux")).isEqualTo("foo.com/bar?baz=quux");
        assertThat(urlDisplayName("http://www.foo.com/bar?baz=quux")).isEqualTo("foo.com/bar?baz=quux");
        assertThat(urlDisplayName("https://www.foo.com/bar?baz=quux")).isEqualTo("foo.com/bar?baz=quux");
    }

    private String urlDisplayName(String url) {
        return SocialMediaLinkItem.create(Optional.absent(), "unrecognized", url).displayName();
    }
}
