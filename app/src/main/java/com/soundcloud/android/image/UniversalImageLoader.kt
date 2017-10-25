package com.soundcloud.android.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.view.View
import android.widget.AbsListView
import android.widget.ImageView
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import com.nostra13.universalimageloader.core.assist.FailReason
import com.nostra13.universalimageloader.core.assist.ImageSize
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener
import com.nostra13.universalimageloader.utils.MemoryCacheUtils
import com.soundcloud.android.properties.ApplicationProperties
import com.soundcloud.android.utils.DeviceHelper
import com.soundcloud.android.utils.OpenForTesting
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import javax.inject.Inject
import com.nostra13.universalimageloader.core.ImageLoader as UniversalImageLoaderLibrary
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener as UniversalImageLoadingListener

@OpenForTesting
class UniversalImageLoader
@Inject
constructor(private val imageLoader: UniversalImageLoaderLibrary,
            private val imageCache: ImageCache,
            private val placeholderGenerator: PlaceholderGenerator,
            private val circularPlaceholderGenerator: CircularPlaceholderGenerator,
            private val universalImageOptionsFactory: UniversalImageOptionsFactory,
            private val deviceHelper: DeviceHelper,
            context: Context,
            properties: ApplicationProperties,
            imageDownloaderFactory: UniversalImageDownloader.Factory) : ImageLoader {

    init {
        val builder = ImageLoaderConfiguration.Builder(context)
        if (properties.useVerboseLogging()) {
            builder.writeDebugLogs()
        }
        builder.defaultDisplayImageOptions(UniversalImageOptionsFactory.cache())
        builder.diskCacheFileNameGenerator(HashCodeFileNameGenerator())
        builder.imageDownloader(imageDownloaderFactory.create(context))
        builder.memoryCache(WeakMemoryCache())
        if (deviceHelper.isLowMemoryDevice) {
            builder.memoryCacheSize((Runtime.getRuntime().maxMemory() / 16).toInt())
        }
        imageLoader.init(builder.build())
    }

    override fun resume() {
        imageLoader.resume()
    }

    override fun pause() {
        imageLoader.pause()
    }

    override fun loadImage(imageUrl: String, loadType: LoadType): Observable<LoadingState> {
        return Observable.create { emitter ->
            val options: DisplayImageOptions? = when (loadType) {
                LoadType.AD -> universalImageOptionsFactory.adImage()
                LoadType.PREFETCH -> universalImageOptionsFactory.prefetch()
                LoadType.NONE -> null
            }
            imageLoader.loadImage(imageUrl, options, imageLoadingListener(emitter))
        }
    }

    override fun createScrollPauseListener(pauseOnScroll: Boolean, pauseOnFling: Boolean, customListener: AbsListView.OnScrollListener?): AbsListView.OnScrollListener? =
            PauseOnScrollListener(imageLoader, pauseOnScroll, pauseOnFling, customListener)

    override fun clearDiskCache() {
        imageLoader.clearDiskCache()
    }

    override fun getCachedBitmap(imageUrl: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        val key = MemoryCacheUtils.generateKey(imageUrl, ImageSize(targetWidth, targetHeight))
        return imageLoader.memoryCache.get(key)
    }

    override fun displayImage(imageUrl: String?,
                              imageView: ImageView,
                              circular: Boolean,
                              placeholderDrawable: Drawable?,
                              displayType: DisplayType,
                              apiImageSize: ApiImageSize,
                              isHighPriority: Boolean): Observable<LoadingState> {
        return Observable.create { emitter: ObservableEmitter<LoadingState> ->
            val imageAware = ImageViewAware(imageView, false)

            val drawable = placeholderDrawable ?: getPlaceholderDrawable(imageUrl, imageAware.width, imageAware.height, circular)

            val options = when (displayType) {
                DisplayType.DEFAULT -> defaultImageOptions(circular, drawable, apiImageSize)
                DisplayType.PLACEHOLDER -> universalImageOptionsFactory.placeholder(drawable)
                DisplayType.PLAYER -> universalImageOptionsFactory.player(drawable, isHighPriority)
                DisplayType.AD -> universalImageOptionsFactory.adImage()
                DisplayType.FULL_IMAGE_DIALOG -> universalImageOptionsFactory.fullImageDialog()
                DisplayType.STREAM_AD_IMAGE -> universalImageOptionsFactory.streamAdImage(drawable, deviceHelper)
            }

            imageLoader.displayImage(
                    imageUrl,
                    imageAware,
                    options,
                    imageLoadingListener(emitter))
        }
    }

    private fun defaultImageOptions(circular: Boolean, drawable: Drawable?, apiImageSize: ApiImageSize): DisplayImageOptions? {
        return if (circular) {
            universalImageOptionsFactory.adapterViewCircular(drawable, apiImageSize, deviceHelper)
        } else {
            universalImageOptionsFactory.adapterView(drawable, apiImageSize, deviceHelper)
        }
    }

    private fun getPlaceholderDrawable(imageUrl: String?, width: Int, height: Int, isCircular: Boolean = false): TransitionDrawable? {
        val placeholderGenerator = if (isCircular) this.circularPlaceholderGenerator else this.placeholderGenerator
        return imageCache.getPlaceholderDrawable(imageUrl,
                                                 width,
                                                 height,
                                                 placeholderGenerator)
    }

    private fun imageLoadingListener(observableEmitter: ObservableEmitter<LoadingState>): com.nostra13.universalimageloader.core.listener.ImageLoadingListener {
        return object : UniversalImageLoadingListener {
            override fun onLoadingComplete(imageUri: String?, view: View?, loadedImage: Bitmap?) {
                observableEmitter.onNext(LoadingState.Complete(imageUri, view, loadedImage))
                observableEmitter.onComplete()
            }

            override fun onLoadingStarted(imageUri: String?, view: View?) {
                observableEmitter.onNext(LoadingState.Start(imageUri, view))
            }

            override fun onLoadingCancelled(imageUri: String?, view: View?) {
                observableEmitter.onNext(LoadingState.Cancel(imageUri, view))
            }

            override fun onLoadingFailed(imageUri: String?, view: View?, failReason: FailReason) {
                observableEmitter.onNext(LoadingState.Fail(imageUri, view, Throwable(failReason.type.toString(), failReason.cause)))
            }

        }
    }
}
