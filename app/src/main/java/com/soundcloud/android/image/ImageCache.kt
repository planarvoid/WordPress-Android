package com.soundcloud.android.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.TransitionDrawable
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import com.soundcloud.android.image.ImageModule.BLURRED_IMAGE_CACHE
import com.soundcloud.android.image.ImageModule.PLACEHOLDER_CACHE
import com.soundcloud.android.model.Urn
import com.soundcloud.android.properties.ApplicationProperties
import com.soundcloud.android.utils.DeviceHelper
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.android.utils.cache.Cache
import com.soundcloud.java.optional.Optional
import rx.functions.Action1
import javax.inject.Inject
import javax.inject.Named

@OpenForTesting
class ImageCache
@Inject
constructor(@param:Named(PLACEHOLDER_CACHE) private val placeholderCache: Cache<String, TransitionDrawable>,
            @param:Named(BLURRED_IMAGE_CACHE) private val blurredImageCache: Cache<Urn, Bitmap>,
            private val context: Context,
            private val properties: ApplicationProperties,
            private val fileNameGenerator: FileNameGenerator,
            private val imageDownloaderFactory: UserAgentImageDownloader.Factory,
            private val deviceHelper: DeviceHelper) {

    fun getImageLoaderConfiguration(): ImageLoaderConfiguration {
        val builder = ImageLoaderConfiguration.Builder(context.applicationContext)
        if (properties.useVerboseLogging()) {
            builder.writeDebugLogs()
        }
        builder.defaultDisplayImageOptions(ImageOptionsFactory.cache())
        builder.diskCacheFileNameGenerator(fileNameGenerator)
        builder.imageDownloader(imageDownloaderFactory.create(context))
        builder.memoryCache(WeakMemoryCache())
        if (deviceHelper.isLowMemoryDevice) {
            builder.memoryCacheSize((Runtime.getRuntime().maxMemory() / 16).toInt())
        }
        return builder.build()
    }

    private fun cacheKeyForImageUrl(imageUrl: String?): String = Optional.fromNullable(imageUrl).or(DEFAULT_CACHE_KEY)

    fun getPlaceholderDrawable(imageUrl: String?, width: Int, height: Int, placeholderGenerator: PlaceholderGenerator): TransitionDrawable? {
        val widthHeightSpecificKey = "${cacheKeyForImageUrl(imageUrl)}_${width}_$height"
        return placeholderCache.get(widthHeightSpecificKey, { placeholderGenerator.generateTransitionDrawable(it) })
    }

    fun getBlurredImage(urn: Urn): Bitmap? = blurredImageCache.get(urn)

    fun cacheBlurredBitmap(resourceUrn: Urn): Action1<Bitmap> = Action1 { blurredImageCache.put(resourceUrn, it) }

    companion object {
        private const val DEFAULT_CACHE_KEY = "default_cache_key"
    }

}
