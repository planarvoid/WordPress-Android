package com.soundcloud.android.users;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;

import android.support.annotation.DrawableRes;

public class SocialMediaLinkItemTest extends AndroidUnitTest {

    @Test
    public void usesSpecificDrawableForKnownNetworks() {
        assertThat(networkHasExpectedDrawable("spotify", R.drawable.favicon_spotify));
        assertThat(networkHasExpectedDrawable("youtube", R.drawable.favicon_youtube));
        assertThat(networkHasExpectedDrawable("facebook", R.drawable.favicon_fb));
    }

    @Test
    public void fallsBackToGenericDrawableForUnknownNetworks() {
        assertThat(networkHasExpectedDrawable("unknown", R.drawable.favicon_generic));
    }

    @Test
    public void usesCustomDisplayNameWhenProvided() {
        final SocialMediaLinkItem link = SocialMediaLinkItem.Companion.create(Optional.of("Custom"), "spotify", "spotify.com/famous");
        assertThat(link.displayName()).isEqualTo("Custom");
    }

    @Test
    public void usesKnownNetworkAsDisplayNameWhenNoCustomDisplayNameProvided() {
        final SocialMediaLinkItem link = SocialMediaLinkItem.Companion.create(Optional.absent(), "spotify", "spotify.com/famous");
        assertThat(link.displayName()).isEqualTo("Spotify");
    }

    @Test
    public void usesUrlForDisplayNameWhenNoCustomDisplayNameProvidedAnUnknownNetwork() {
        final SocialMediaLinkItem link = SocialMediaLinkItem.Companion.create(Optional.absent(), "unknown", "unknown.com/famous");
        assertThat(link.displayName()).isEqualTo("unknown.com/famous");
    }

    @Test
    public void stripsHttpAndWwwFromUrlForDisplayName() {
        assertThat(urlDisplayName("http://foo.com/bar?baz=quux")).isEqualTo("foo.com/bar?baz=quux");
        assertThat(urlDisplayName("https://foo.com/bar?baz=quux")).isEqualTo("foo.com/bar?baz=quux");
        assertThat(urlDisplayName("http://www.foo.com/bar?baz=quux")).isEqualTo("foo.com/bar?baz=quux");
        assertThat(urlDisplayName("https://www.foo.com/bar?baz=quux")).isEqualTo("foo.com/bar?baz=quux");
    }

    private boolean networkHasExpectedDrawable(String network, @DrawableRes int drawable) {
        final SocialMediaLinkItem link = SocialMediaLinkItem.Companion.create(Optional.absent(), network, "some_url");
        return link.icon(context()).getConstantState().equals(context().getResources().getDrawable(drawable).getConstantState());
    }


    private String urlDisplayName(String url) {
        return SocialMediaLinkItem.Companion.create(Optional.absent(), "unrecognized", url).displayName();
    }
}
