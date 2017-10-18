package com.soundcloud.android.users

import android.content.Context
import android.support.v4.content.ContextCompat.getDrawable
import com.soundcloud.android.api.model.ApiSocialMediaLink
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.java.optional.Optional
import com.soundcloud.java.strings.Strings

@OpenForTesting
data class SocialMediaLinkItem(val title: Optional<String>, val network: Network, val url: String) {

    fun displayName(): String {
        return title
                .or(network.displayName())
                .or(url.replaceFirst("^https?://(?:www.)?".toRegex(), Strings.EMPTY))
    }

    fun icon(context: Context) = getDrawable(context, network.drawableId())

    companion object {
        fun from(apiSocialMediaLink: ApiSocialMediaLink) = create(apiSocialMediaLink.title(), apiSocialMediaLink.network(), apiSocialMediaLink.url())

        fun create(title: Optional<String>, network: String, url: String) = SocialMediaLinkItem(title, Network.from(network), url)
    }
}
