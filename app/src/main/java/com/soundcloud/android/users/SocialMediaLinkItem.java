package com.soundcloud.android.users;

import static android.support.v4.content.ContextCompat.getDrawable;

import com.soundcloud.android.api.model.ApiSocialMediaLink;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;

public class SocialMediaLinkItem {
    public static SocialMediaLinkItem from(ApiSocialMediaLink apiSocialMediaLink) {
        return create(apiSocialMediaLink.title(), apiSocialMediaLink.network(), apiSocialMediaLink.url());
    }

    public static SocialMediaLinkItem create(Optional<String> title, String network, String url) {
        return new SocialMediaLinkItem(title, Network.from(network), url);
    }

    private final Optional<String> title;
    private final Network network;
    private final String url;

    private SocialMediaLinkItem(Optional<String> title, Network network, String url) {
        this.title = title;
        this.network = network;
        this.url = url;
    }

    public String displayName() {
        return title
                .or(network.displayName())
                .or(url.replaceFirst("^https?://(?:www.)?", Strings.EMPTY));
    }

    public Drawable icon(Context context) {
        return getDrawable(context, network.drawableId());
    }

    public Intent toIntent() {
        if (network.isEmail()) {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[] { url });
            return intent;
        }
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    }
}
