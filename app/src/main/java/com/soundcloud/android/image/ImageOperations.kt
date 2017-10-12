package com.soundcloud.android.image

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.support.v7.graphics.Palette
import android.view.View
import android.widget.AbsListView
import android.widget.ImageView
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.assist.ImageSize
import com.nostra13.universalimageloader.core.assist.ViewScaleType
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware
import com.nostra13.universalimageloader.core.imageaware.NonViewAware
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener
import com.nostra13.universalimageloader.utils.MemoryCacheUtils
import com.soundcloud.android.R
import com.soundcloud.android.image.ImageOperations.DisplayType.CIRCULAR
import com.soundcloud.android.image.ImageOperations.DisplayType.DEFAULT
import com.soundcloud.android.model.Urn
import com.soundcloud.android.utils.DeviceHelper
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.android.utils.images.ImageUtils
import com.soundcloud.java.optional.Optional
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.SingleEmitter
import java.io.IOException
import java.util.HashSet
import javax.inject.Inject
import javax.inject.Singleton

@OpenForTesting
@Singleton
class ImageOperations
@Inject
constructor(
        private val imageLoader: ImageLoader,
        private val imageUrlBuilder: ImageUrlBuilder,
        private val adapterFactory: FallbackBitmapLoadingAdapter.Factory,
        private val bitmapAdapterFactory: BitmapLoadingAdapter.Factory,
        private val imageProcessor: ImageProcessor,
        private val deviceHelper: DeviceHelper,
        private val placeholderGenerator: PlaceholderGenerator,
        private val circularPlaceholderGenerator: CircularPlaceholderGenerator,
        private val imageCache: ImageCache) {
    private val notFoundUris = HashSet<String>()
    private val notFoundListener = FallbackImageListener(notFoundUris)

    enum class DisplayType {
        DEFAULT, CIRCULAR
    }

    init {
        this.imageLoader.init(imageCache.getImageLoaderConfiguration())
    }

    fun clearDiskCache() {
        imageLoader.clearDiskCache()
    }

    fun displayInAdapterView(urn: Urn, imageUrlTemplate: Optional<String> = Optional.absent(), apiImageSize: ApiImageSize, imageView: ImageView, displayType: DisplayType) {
        displayInAdapterView(apiImageSize = apiImageSize,
                             imageView = imageView,
                             imageUrl = getImageUrl(imageUrlTemplate, urn, apiImageSize),
                             displayType = displayType)
    }

    fun displayInAdapterView(imageResource: ImageResource,
                             apiImageSize: ApiImageSize,
                             imageView: ImageView,
                             fallbackDrawable: Optional<Drawable> = Optional.absent(),
                             displayType: DisplayType): Single<Bitmap> {
        return Single.create { subscriber ->
            displayInAdapterView(apiImageSize,
                                 imageView,
                                 getImageUrl(imageResource.imageUrlTemplate, imageResource.urn, apiImageSize),
                                 buildFallbackImageListener(fromBitmapSubscriber(subscriber)),
                                 fallbackDrawable.orNull(),
                                 displayType)
        }
    }

    private fun displayInAdapterView(apiImageSize: ApiImageSize,
                                     imageView: ImageView,
                                     imageUrl: String? = null,
                                     imageListener: FallbackImageListener = notFoundListener,
                                     placeholderDrawable: Drawable? = null,
                                     displayType: DisplayType) {
        val imageAware = ImageViewAware(imageView, false)
        val drawable = placeholderDrawable ?: getPlaceholderDrawable(imageUrl, imageAware.width, imageAware.height, displayType)

        val options = when (displayType) {
            CIRCULAR -> ImageOptionsFactory.adapterViewCircular(drawable, apiImageSize, deviceHelper)
            DEFAULT -> ImageOptionsFactory.adapterView(drawable, apiImageSize, deviceHelper)
        }
        imageLoader.displayImage(imageUrl, imageAware, options, imageListener)
    }

    fun displayCircularInAdapterViewAndGeneratePalette(imageResource: ImageResource,
                                                       apiImageSize: ApiImageSize,
                                                       imageView: ImageView): Single<Palette> {
        return displayInAdapterView(imageResource, apiImageSize, imageView, Optional.absent(), DisplayType.CIRCULAR)
                .flatMap { bitmap: Bitmap ->
                    Single.create { subscriber: SingleEmitter<Palette> ->
                        Palette.from(bitmap).generate { palette -> subscriber.onSuccess(palette) }
                    }
                }
    }

    fun displayDefaultPlaceholder(imageView: ImageView) {
        displayWithPlaceholder(imageView)
    }

    fun displayWithPlaceholder(urn: Urn, imageUrlTemplate: Optional<String>, apiImageSize: ApiImageSize, imageView: ImageView) {
        displayWithPlaceholder(imageView, getImageUrl(imageUrlTemplate, urn, apiImageSize))
    }

    private fun displayWithPlaceholder(imageView: ImageView, imageUrl: String? = null, imageListener: ImageLoadingListener = notFoundListener) {
        val imageAware = ImageViewAware(imageView, false)
        imageLoader.displayImage(
                imageUrl,
                imageAware,
                ImageOptionsFactory.placeholder(getPlaceholderDrawable(imageUrl, imageAware.width, imageAware.height, DisplayType.DEFAULT)),
                imageListener)
    }

    fun displayWithPlaceholderObservable(imageResource: ImageResource, apiImageSize: ApiImageSize, imageView: ImageView): Single<Bitmap> {
        return Single.create { subscriber ->
            displayWithPlaceholder(imageView,
                                   getImageUrl(imageResource.imageUrlTemplate, imageResource.urn, apiImageSize),
                                   buildFallbackImageListener(bitmapAdapterFactory.create(subscriber)))
        }
    }

    fun displayCircular(imageUrl: String, imageView: ImageView) {
        imageLoader.displayImage(imageUrl, ImageViewAware(imageView, false),
                                 ImageOptionsFactory.placeholderCircular(imageView.resources
                                                                                 .getDrawable(R.drawable.circular_placeholder)))
    }

    fun displayCircularWithPlaceholder(urn: Urn, imageUrlTemplate: Optional<String>,
                                       apiImageSize: ApiImageSize,
                                       imageView: ImageView) {
        val imageUrl = getImageUrl(imageUrlTemplate.orNull(), urn, apiImageSize)
        val imageAware = ImageViewAware(imageView, false)
        val options = ImageOptionsFactory.placeholderCircular(
                imageCache.getPlaceholderDrawable(imageUrl, imageAware.width, imageAware.height, circularPlaceholderGenerator))
        imageLoader.displayImage(
                imageUrl,
                imageAware,
                options,
                notFoundListener)
    }

    fun displayInPlayer(imageResource: ImageResource,
                        apiImageSize: ApiImageSize,
                        imageView: ImageView,
                        placeholder: Bitmap?,
                        isHighPriority: Boolean) {
        val imageAware = ImageViewAware(imageView, false)
        val placeholderDrawable = if (placeholder != null)
            BitmapDrawable(placeholder)
        else
            getPlaceholderDrawable(imageResource.urn.toString(), imageAware.width, imageAware.height, DisplayType.DEFAULT)

        imageLoader.displayImage(
                getImageUrl(imageResource.imageUrlTemplate, imageResource.urn, apiImageSize),
                imageAware,
                ImageOptionsFactory.player(placeholderDrawable, isHighPriority),
                notFoundListener)
    }

    fun displayAdImage(urn: Urn, imageUri: String, imageView: ImageView, listener: ImageListener) {
        displayAdImage(urn, imageUri, imageView, ImageListenerUILAdapter(listener))
    }

    fun displayLeaveBehind(uri: Uri, imageView: ImageView, imageListener: ImageListener) {
        val imageAware = ImageViewAware(imageView, false)
        imageLoader.displayImage(
                uri.toString(),
                imageAware,
                ImageOptionsFactory.adImage(),
                ImageListenerUILAdapter(imageListener))
    }

    fun displayInFullDialogView(imageResource: ImageResource,
                                apiImageSize: ApiImageSize,
                                imageView: ImageView,
                                imageListener: ImageListener) {
        imageLoader.displayImage(
                getImageUrl(imageResource.imageUrlTemplate, imageResource.urn, apiImageSize),
                ImageViewAware(imageView, false),
                ImageOptionsFactory.fullImageDialog(),
                ImageListenerUILAdapter(imageListener))
    }

    fun bitmap(uri: Uri): Single<Bitmap> {
        return Single.create { subscriber ->
            // We pass NonViewAware to circumvent ImageLoader cancelling requests (https://github.com/nostra13/Android-Universal-Image-Loader/issues/681)
            imageLoader.displayImage(uri.toString(),
                                     NonViewAware(ImageSize(0, 0), ViewScaleType.CROP),
                                     ImageOptionsFactory.adImage(),
                                     ImageListenerUILAdapter(bitmapAdapterFactory.create(subscriber)))
        }
    }

    fun artwork(imageResource: ImageResource, apiImageSize: ApiImageSize): Single<Bitmap> {
        return Single.create { subscriber ->
            val fallback = createFallbackBitmap(imageResource.urn, apiImageSize)
            imageLoader.loadImage(
                    getImageUrl(imageResource.imageUrlTemplate, imageResource.urn, apiImageSize),
                    ImageListenerUILAdapter(adapterFactory.create(subscriber, fallback)))
        }
    }

    fun artwork(imageResource: ImageResource,
                apiImageSize: ApiImageSize,
                targetWidth: Int,
                targetHeight: Int): Single<Bitmap> {
        return Single.create { subscriber ->
            val fallbackDrawable = generateDrawable(imageResource)
            val fallback = ImageUtils.toBitmap(fallbackDrawable, targetWidth, targetHeight)
            load(imageResource,
                 apiImageSize,
                 targetWidth,
                 targetHeight,
                 adapterFactory.create(subscriber, fallback))
        }
    }

    fun precacheArtwork(imageResource: ImageResource, apiImageSize: ApiImageSize) {
        val url = getImageUrl(imageResource.imageUrlTemplate, imageResource.urn, apiImageSize)
        imageLoader.loadImage(url, ImageOptionsFactory.prefetch(), null)
    }

    fun getCachedListItemBitmap(resources: Resources, imageResource: ImageResource): Bitmap? {
        return getCachedBitmap(imageResource, ApiImageSize.getListItemImageSize(resources),
                               resources.getDimensionPixelSize(R.dimen.list_item_image_dimension),
                               resources.getDimensionPixelSize(R.dimen.list_item_image_dimension))
    }

    fun getCachedBitmap(imageResource: ImageResource,
                        apiImageSize: ApiImageSize,
                        targetWidth: Int,
                        targetHeight: Int): Bitmap? {
        val imageUrl = imageUrlBuilder.buildUrl(imageResource.imageUrlTemplate.orNull(), imageResource.urn, apiImageSize)
        if (imageUrl != null) {
            val key = MemoryCacheUtils.generateKey(imageUrl, ImageSize(targetWidth, targetHeight))
            return imageLoader.memoryCache.get(key)
        } else {
            return null
        }
    }

    fun createScrollPauseListener(pauseOnScroll: Boolean, pauseOnFling: Boolean, customListener: AbsListView.OnScrollListener?): PauseOnScrollListener {
        return PauseOnScrollListener(imageLoader, pauseOnScroll, pauseOnFling, customListener)
    }

    fun decodeResource(resources: Resources, resId: Int): Bitmap? {
        try {
            return BitmapFactory.decodeResource(resources, resId)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            return null
        }

    }

    private fun fromBitmapSubscriber(subscriber: SingleEmitter<in Bitmap>): DefaultImageListener {
        return object : DefaultImageListener() {
            override fun onLoadingFailed(imageUri: String,
                                         view: View,
                                         cause: Throwable?) {
                if (!subscriber.isDisposed) {
                    subscriber.onError(cause ?: IOException("Failed to load bitmap for Unknown reason"))
                }
            }

            override fun onLoadingComplete(imageUri: String?,
                                           view: View,
                                           loadedImage: Bitmap?) {
                if (loadedImage == null) {
                    subscriber.onError(IOException("Image loading failed."))
                    return
                }
                subscriber.onSuccess(loadedImage)
            }
        }
    }

    private fun displayAdImage(urn: Urn, imageUri: String, imageView: ImageView, listener: ImageLoadingListener) {
        val imageAware = ImageViewAware(imageView, false)
        val drawable = getPlaceholderDrawable(urn.toString(), imageAware.width, imageAware.height, DisplayType.DEFAULT)
        val options = ImageOptionsFactory.streamAdImage(drawable, deviceHelper)

        imageLoader.displayImage(imageUri, imageAware, options, listener)
    }

    private fun load(imageResource: ImageResource,
                     apiImageSize: ApiImageSize,
                     targetWidth: Int,
                     targetHeight: Int,
                     imageListener: ImageListener) {
        val targetSize = ImageSize(targetWidth, targetHeight)
        val imageAware = NonViewAware(targetSize, ViewScaleType.CROP)
        imageLoader.displayImage(getImageUrl(imageResource.imageUrlTemplate, imageResource.urn, apiImageSize),
                                 imageAware,
                                 ImageListenerUILAdapter(imageListener))
    }

    private fun blurBitmap(blurRadius: Optional<Float>) = { bitmap: Bitmap -> imageProcessor.blurBitmap(bitmap, blurRadius) }

    private fun blurBitmap(original: Bitmap, blurRadius: Optional<Float>): Single<Bitmap> {
        return Single.create { subscriber ->
            subscriber.onSuccess(imageProcessor.blurBitmap(original, blurRadius))
        }
    }

    fun getImageUrl(imageUrlTemplate: Optional<String>, urn: Urn, apiImageSize: ApiImageSize) = getImageUrl(imageUrlTemplate.orNull(), urn, apiImageSize)

    private fun getImageUrl(imageUrlTemplate: String? = null, urn: Urn, apiImageSize: ApiImageSize): String? {
        val imageUrl = imageUrlBuilder.buildUrl(imageUrlTemplate, urn, apiImageSize)
        return if (notFoundUris.contains(imageUrl)) null else imageUrl
    }

    private fun buildFallbackImageListener(imageListener: ImageListener) = FallbackImageListener(imageListener, notFoundUris)

    fun blurredArtwork(resources: Resources,
                       imageResource: ImageResource,
                       blurRadius: Optional<Float>,
                       scheduleOn: Scheduler, observeOn: Scheduler): Single<Bitmap> {
        val cachedBlurImage = imageCache.getBlurredImage(imageResource.urn)
        if (cachedBlurImage != null) {
            return Single.just(cachedBlurImage)
        } else {
            val cached = getCachedListItemBitmap(resources, imageResource)
            return if (cached == null) {
                artwork(imageResource, ApiImageSize.getListItemImageSize(resources))
                        .map(blurBitmap(blurRadius))
                        .subscribeOn(scheduleOn)
                        .observeOn(observeOn)
                        .doOnSuccess(imageCache.cacheBlurredBitmap(imageResource.urn))
            } else {
                blurBitmap(cached, blurRadius)
                        .subscribeOn(scheduleOn)
                        .observeOn(observeOn)
                        .doOnSuccess(imageCache.cacheBlurredBitmap(imageResource.urn))
            }
        }
    }

    private fun generateDrawable(imageResource: ImageResource): GradientDrawable {
        return placeholderGenerator.generateDrawable(imageResource.urn.toString())
    }

    private fun getPlaceholderDrawable(imageUrl: String?, width: Int, height: Int, displayType: ImageOperations.DisplayType): TransitionDrawable? {
        val placeholderGenerator = if (displayType == CIRCULAR) this.circularPlaceholderGenerator else this.placeholderGenerator
        return imageCache.getPlaceholderDrawable(imageUrl,
                                                 width,
                                                 height,
                                                 placeholderGenerator)
    }

    private fun createFallbackBitmap(resourceUrn: Urn, apiImageSize: ApiImageSize): Bitmap {
        // This bitmap is only used by the current track for the components that can't use
        // drawables (i.e. the notification and the remote client for the lock screen)
        //
        // Unless we refactor the cache to store a /GradientDrawable/ and not a /TransitionDrawable/
        // we don't have a cache for this guy.
        //
        // Also, we don't cache bitmap in the /ImageOperations/ since it does not worth it. A cache
        // may have a impact on the memory usage and without the performance seems pretty good, though.
        val fallbackDrawable = placeholderGenerator.generateDrawable(resourceUrn.toString())
        return ImageUtils.toBitmap(fallbackDrawable, apiImageSize.width, apiImageSize.height)
    }

    fun resume() {
        imageLoader.resume()
    }

    fun pause() {
        imageLoader.pause()
    }

}
