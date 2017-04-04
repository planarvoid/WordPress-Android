package com.soundcloud.android.users;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.assertions.IntentAssert;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;

import android.content.Intent;
import android.net.Uri;
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
        final SocialMediaLinkItem link = SocialMediaLinkItem.create(Optional.of("Custom"), "spotify", "spotify.com/famous");
        assertThat(link.displayName()).isEqualTo("Custom");
    }

    @Test
    public void usesKnownNetworkAsDisplayNameWhenNoCustomDisplayNameProvided() {
        final SocialMediaLinkItem link = SocialMediaLinkItem.create(Optional.absent(), "spotify", "spotify.com/famous");
        assertThat(link.displayName()).isEqualTo("Spotify");
    }

    @Test
    public void usesUrlForDisplayNameWhenNoCustomDisplayNameProvidedAnUnknownNetwork() {
        final SocialMediaLinkItem link = SocialMediaLinkItem.create(Optional.absent(), "unknown", "unknown.com/famous");
        assertThat(link.displayName()).isEqualTo("unknown.com/famous");
    }

    @Test
    public void stripsHttpAndWwwFromUrlForDisplayName() {
        assertThat(urlDisplayName("http://foo.com/bar?baz=quux")).isEqualTo("foo.com/bar?baz=quux");
        assertThat(urlDisplayName("https://foo.com/bar?baz=quux")).isEqualTo("foo.com/bar?baz=quux");
        assertThat(urlDisplayName("http://www.foo.com/bar?baz=quux")).isEqualTo("foo.com/bar?baz=quux");
        assertThat(urlDisplayName("https://www.foo.com/bar?baz=quux")).isEqualTo("foo.com/bar?baz=quux");
    }

    @Test
    public void createsEmailIntentWhenUrlIsAnEmail() {
        intentMatchesEmail("email", "foo@bar.com");
        intentMatchesEmail("email", "https://www.foo.com/bar?baz=quux");
    }

    @Test
    public void createsViewIntentWhenUrlIsNotAnEmail() {
        intentMatchesView("unknown", "unknown.com/famous");
        intentMatchesView("personal", "foo.com/bar?baz=quux");
        intentMatchesView("spotify", "spotify.com/famous");
    }

    private boolean networkHasExpectedDrawable(String network, @DrawableRes int drawable) {
        final SocialMediaLinkItem link = SocialMediaLinkItem.create(Optional.absent(), network, "some_url");
        return link.icon(context()).getConstantState().equals(context().getResources().getDrawable(drawable).getConstantState());
    }

    private IntentAssert intentMatchesEmail(String network, String url) {
        return new IntentAssert(intent(network, url))
                .containsAction(Intent.ACTION_SENDTO)
                .containsUri(Uri.parse("mailto:"))
                .containsExtra(Intent.EXTRA_EMAIL, new String[] { url });
    }

    private IntentAssert intentMatchesView(String network, String url) {
        return new IntentAssert(intent(network, url))
                .containsAction(Intent.ACTION_VIEW)
                .containsUri(Uri.parse(url));
    }

    private Intent intent(String network, String url) {
        return SocialMediaLinkItem.create(Optional.absent(), network, url).toIntent();
    }

    private String urlDisplayName(String url) {
        return SocialMediaLinkItem.create(Optional.absent(), "unrecognized", url).displayName();
    }
}
