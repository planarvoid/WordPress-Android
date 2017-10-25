package com.soundcloud.android.image

import android.graphics.Bitmap
import android.graphics.drawable.TransitionDrawable
import com.soundcloud.android.image.ImageModule.BLURRED_IMAGE_CACHE
import com.soundcloud.android.image.ImageModule.PLACEHOLDER_CACHE
import com.soundcloud.android.model.Urn
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.android.utils.cache.Cache
import com.soundcloud.java.optional.Optional
import io.reactivex.functions.Consumer
import javax.inject.Inject
import javax.inject.Named

@OpenForTesting
class ImageCache
@Inject
constructor(@param:Named(PLACEHOLDER_CACHE) private val placeholderCache: Cache<String, TransitionDrawable>,
            @param:Named(BLURRED_IMAGE_CACHE) private val blurredImageCache: Cache<Urn, Bitmap>) {

    private fun cacheKeyForImageUrl(imageUrl: String?): String = Optional.fromNullable(imageUrl).or(DEFAULT_CACHE_KEY)

    fun getPlaceholderDrawable(imageUrl: String?, width: Int, height: Int, placeholderGenerator: PlaceholderGenerator): TransitionDrawable? {
        val widthHeightSpecificKey = "${cacheKeyForImageUrl(imageUrl)}_${width}_$height"
        return placeholderCache.get(widthHeightSpecificKey, { placeholderGenerator.generateTransitionDrawable(it) })
    }

    fun getBlurredImage(urn: Urn): Bitmap? = blurredImageCache.get(urn)

    fun cacheBlurredBitmap(resourceUrn: Urn) = Consumer { bitmap: Bitmap -> blurredImageCache.put(resourceUrn, bitmap) }

    companion object {
        private const val DEFAULT_CACHE_KEY = "default_cache_key"
    }

}
