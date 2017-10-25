package com.soundcloud.android.image

import android.graphics.Bitmap
import android.graphics.drawable.TransitionDrawable
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.android.utils.cache.Cache
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

class ImageCacheTest : AndroidUnitTest() {

    private lateinit var imageCache: ImageCache

    private val placeholderCache = Cache.withSoftValues<String, TransitionDrawable>(10)
    private val blurredImageCache = Cache.withSoftValues<Urn, Bitmap>(10)

    @Mock internal lateinit var cachedTransitionDrawable: TransitionDrawable
    @Mock internal lateinit var placeHolderGenerator: PlaceholderGenerator

    @Before
    @Throws(Exception::class)
    fun setUp() {
        imageCache = ImageCache(placeholderCache, blurredImageCache)
    }

    @Test
    @Throws(Exception::class)
    fun getPlaceholderDrawableReturnsDrawable() {
        placeholderCache.put("https://api-mobile.soundcloud.com/images/soundcloud:tracks:1/large_100_100", cachedTransitionDrawable)

        val placeholderDrawable = imageCache.getPlaceholderDrawable("https://api-mobile.soundcloud.com/images/soundcloud:tracks:1/large", 100, 100, placeHolderGenerator)

        assertThat(placeholderDrawable).isSameAs(cachedTransitionDrawable)
    }

}