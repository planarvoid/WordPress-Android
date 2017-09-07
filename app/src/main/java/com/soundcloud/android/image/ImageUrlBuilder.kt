package com.soundcloud.android.image

import com.soundcloud.android.api.ApiEndpoints
import com.soundcloud.android.api.ApiUrlBuilder
import com.soundcloud.android.model.Urn
import com.soundcloud.java.optional.Optional
import javax.inject.Inject

class ImageUrlBuilder
@Inject
internal constructor(private val apiUrlBuilder: ApiUrlBuilder) {

    fun buildUrl(urlTemplate: Optional<String>, urn: Urn, apiImageSize: ApiImageSize): String? =
            when {
                urlTemplate.isPresent -> buildUrlFromTemplate(apiImageSize, urlTemplate.get())
                urn != Urn.NOT_SET && !urn.isUser -> imageResolverUrl(urn, apiImageSize)
                else -> null
            }

    private fun imageResolverUrl(urn: Urn, apiImageSize: ApiImageSize): String {
        return apiUrlBuilder
                .from(ApiEndpoints.IMAGES, urn, apiImageSize.sizeSpec)
                .build()
    }

    private fun buildUrlFromTemplate(apiImageSize: ApiImageSize, urlTemplate: String): String
            = urlTemplate.replace(SIZE_TOKEN.toRegex(), apiImageSize.sizeSpec)

    companion object {
        private const val SIZE_TOKEN = "\\{size\\}"
    }
}
