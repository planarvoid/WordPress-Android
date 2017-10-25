package com.soundcloud.android.image

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.v7.graphics.Palette
import android.widget.AbsListView
import android.widget.ImageView
import com.soundcloud.android.R
import com.soundcloud.android.model.Urn
import com.soundcloud.android.rx.observers.LambdaObserver
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.android.utils.images.ImageUtils
import com.soundcloud.java.optional.Optional
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.disposables.CompositeDisposable
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
        private val imageProcessor: ImageProcessor,
        private val placeholderGenerator: PlaceholderGenerator,
        private val imageCache: ImageCache) {
    private val notFoundUris = HashSet<String>()
    private val notFoundConsumer = FallbackImageConsumer(notFoundUris = notFoundUris)
    private val compositeDisposable = CompositeDisposable()

    fun clearDiskCache() {
        imageLoader.clearDiskCache()
    }

    fun displayInAdapterView(urn: Urn, imageUrlTemplate: Optional<String> = Optional.absent(), apiImageSize: ApiImageSize, imageView: ImageView, circular: Boolean) {
        compositeDisposable.add(imageLoader.displayImage(toImageUrl(urn = urn, imageUrlTemplate = imageUrlTemplate.orNull(), apiImageSize = apiImageSize),
                                                         imageView = imageView,
                                                         circular = circular,
                                                         apiImageSize = apiImageSize)
                                        .subscribeWith(LambdaObserver.onNext { notFoundConsumer }))
    }

    fun displayInAdapterViewSingle(urn: Urn,
                                   imageUrlTemplate: Optional<String> = Optional.absent(),
                                   apiImageSize: ApiImageSize,
                                   imageView: ImageView,
                                   fallbackDrawable: Optional<Drawable> = Optional.absent(),
                                   circular: Boolean): Single<Bitmap> {
        return imageLoader.displayImage(toImageUrl(urn, imageUrlTemplate.orNull(), apiImageSize),
                                        imageView,
                                        circular = circular,
                                        placeholderDrawable = fallbackDrawable.orNull(),
                                        apiImageSize = apiImageSize)
                .compose(this::toBitmap)
                .firstOrError()
    }

    fun displayCircularInAdapterViewAndGeneratePalette(urn: Urn,
                                                       imageUrlTemplate: Optional<String>,
                                                       apiImageSize: ApiImageSize,
                                                       imageView: ImageView): Single<Palette> {
        return displayInAdapterViewSingle(urn, imageUrlTemplate, apiImageSize, imageView, circular = true)
                .flatMap { bitmap: Bitmap ->
                    Single.create { subscriber: SingleEmitter<Palette> ->
                        Palette.from(bitmap).generate { palette -> subscriber.onSuccess(palette) }
                    }
                }
    }

    fun displayDefaultPlaceholder(imageView: ImageView) {
        compositeDisposable.add(displayWithPlaceholder(imageView)
                                        .subscribeWith(LambdaObserver.onNext(notFoundConsumer)))
    }

    fun displayWithPlaceholder(urn: Urn, imageUrlTemplate: Optional<String>, apiImageSize: ApiImageSize, imageView: ImageView) {
        compositeDisposable.add(displayWithPlaceholder(imageView, toImageUrl(urn, imageUrlTemplate.orNull(), apiImageSize), apiImageSize = apiImageSize)
                                        .subscribeWith(LambdaObserver.onNext(notFoundConsumer)))
    }

    private fun displayWithPlaceholder(imageView: ImageView, imageUrl: String? = null, apiImageSize: ApiImageSize = ApiImageSize.Unknown): Observable<LoadingState> {
        return imageLoader.displayImage(imageUrl,
                                        imageView,
                                        displayType = DisplayType.PLACEHOLDER,
                                        apiImageSize = apiImageSize)
    }

    fun displayWithPlaceholderObservable(urn: Urn, imageUrlTemplate: Optional<String> = Optional.absent(), apiImageSize: ApiImageSize, imageView: ImageView): Single<Bitmap> {
        return displayWithPlaceholder(imageView,
                                      toImageUrl(urn, imageUrlTemplate.orNull(), apiImageSize),
                                      apiImageSize = apiImageSize)
                .compose(this::toBitmap)
                .firstOrError()
    }

    private fun toBitmap(input: Observable<LoadingState>): Observable<Bitmap> {
        return input.doOnNext { notFoundConsumer }
                .doOnNext(this::throwErrorOnMissingBitmap)
                .ofType(LoadingState.Complete::class.java)
                .map { it.loadedImage }
    }

    fun displayCircular(imageUrl: String, imageView: ImageView) {
        compositeDisposable.add(imageLoader.displayImage(imageUrl, imageView, circular = true, apiImageSize = ApiImageSize.T120).subscribeWith(LambdaObserver.onNext(notFoundConsumer)))
    }

    fun displayCircularWithPlaceholder(urn: Urn,
                                       imageUrlTemplate: Optional<String>,
                                       apiImageSize: ApiImageSize,
                                       imageView: ImageView) {
        compositeDisposable.add(imageLoader.displayImage(toImageUrl(urn, imageUrlTemplate.orNull(), apiImageSize),
                                                         imageView,
                                                         circular = true,
                                                         apiImageSize = apiImageSize)
                                        .subscribeWith(LambdaObserver.onNext(notFoundConsumer)))
    }

    fun displayInPlayer(urn: Urn,
                        imageUrlTemplate: Optional<String> = Optional.absent(),
                        apiImageSize: ApiImageSize,
                        imageView: ImageView,
                        placeholder: Bitmap?,
                        isHighPriority: Boolean) {
        compositeDisposable.add(imageLoader.displayImage(toImageUrl(urn, imageUrlTemplate.orNull(), apiImageSize),
                                                         imageView,
                                                         placeholderDrawable = placeholder?.let { BitmapDrawable(imageView.resources, it) },
                                                         displayType = DisplayType.PLAYER,
                                                         apiImageSize = apiImageSize,
                                                         isHighPriority = isHighPriority)
                                        .subscribeWith(LambdaObserver.onNext(notFoundConsumer)))
    }

    fun displayLeaveBehind(uri: String, imageView: ImageView): Observable<LoadingState> {
        return imageLoader.displayImage(uri, imageView, displayType = DisplayType.AD)
    }

    fun displayInFullDialogView(urn: Urn,
                                imageUrlTemplate: Optional<String>,
                                apiImageSize: ApiImageSize,
                                imageView: ImageView): Observable<LoadingState> {
        return imageLoader.displayImage(toImageUrl(urn, imageUrlTemplate.orNull(), apiImageSize),
                                        imageView,
                                        displayType = DisplayType.FULL_IMAGE_DIALOG,
                                        apiImageSize = apiImageSize)
    }

    fun bitmap(uri: Uri, loadType: LoadType): Single<Bitmap> = imageLoader.loadImage(uri.toString(), loadType = loadType).compose(this::toBitmap).firstOrError()

    fun bitmap(urn: Urn, apiImageSize: ApiImageSize, loadType: LoadType): Maybe<Bitmap> {
        val imageUrl = toImageUrl(urn = urn, apiImageSize = apiImageSize)

        imageUrl?.let {
            return imageLoader.loadImage(it, loadType = loadType).compose(this::toBitmap).firstOrError().toMaybe()
        }

        return Maybe.empty()
    }

    fun artwork(urn: Urn, imageUrlTemplate: Optional<String>, apiImageSize: ApiImageSize): Maybe<Bitmap> {
        val imageUrl = toImageUrl(urn = urn, imageUrlTemplate = imageUrlTemplate.orNull(), apiImageSize = apiImageSize)

        imageUrl?.let {
            return imageLoader.loadImage(it).compose { toFallback(it, { createFallbackBitmap(urn, apiImageSize.width, apiImageSize.height) }) }.firstElement()
        }

        return Maybe.empty()
    }

    private fun toFallback(input: Observable<LoadingState>, createFallbackBitmap: () -> Bitmap): Observable<Bitmap> {
        return input.flatMapMaybe {
            when (it) {
                is LoadingState.Complete -> Maybe.just(it.loadedImage ?: createFallbackBitmap())
                is LoadingState.Start -> Maybe.empty()
                is LoadingState.Cancel -> Maybe.empty()
                is LoadingState.Fail -> Maybe.just(createFallbackBitmap())
            }
        }
    }

    fun artwork(urn: Urn,
                imageUrlTemplate: Optional<String>,
                apiImageSize: ApiImageSize,
                targetWidth: Int,
                targetHeight: Int): Single<Bitmap> {
        return load(urn, imageUrlTemplate.orNull(), apiImageSize, targetWidth, targetHeight).compose { toFallback(it, { createFallbackBitmap(urn, targetWidth, targetHeight) }) }.firstOrError()
    }

    fun precacheArtwork(urn: Urn,
                        imageUrlTemplate: Optional<String>,
                        apiImageSize: ApiImageSize) {
        toImageUrl(urn, imageUrlTemplate.orNull(), apiImageSize)?.let {
            imageLoader.loadImage(it, loadType = LoadType.PREFETCH)
        }?.let {
            compositeDisposable.add(it.subscribeWith(LambdaObserver.onNext(notFoundConsumer)))
        }
    }

    fun getCachedListItemBitmap(resources: Resources, urn: Urn, imageUrlTemplate: Optional<String>): Bitmap? {
        return getCachedBitmap(urn, imageUrlTemplate, ApiImageSize.getListItemImageSize(resources),
                               resources.getDimensionPixelSize(R.dimen.list_item_image_dimension),
                               resources.getDimensionPixelSize(R.dimen.list_item_image_dimension))
    }

    fun getCachedBitmap(urn: Urn,
                        imageUrlTemplate: Optional<String>,
                        apiImageSize: ApiImageSize,
                        targetWidth: Int,
                        targetHeight: Int): Bitmap? {

        val imageUrl = imageUrlBuilder.buildUrl(imageUrlTemplate.orNull(), urn, apiImageSize)
        return imageUrl?.let { imageLoader.getCachedBitmap(it, targetWidth, targetHeight) }
    }

    fun createScrollPauseListener(pauseOnScroll: Boolean, pauseOnFling: Boolean, customListener: AbsListView.OnScrollListener?): AbsListView.OnScrollListener? =
            imageLoader.createScrollPauseListener(pauseOnScroll, pauseOnFling, customListener)

    fun decodeResource(resources: Resources, resId: Int): Bitmap? {
        try {
            return BitmapFactory.decodeResource(resources, resId)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            return null
        }

    }

    private fun throwErrorOnMissingBitmap(loadingState: LoadingState) {
        when (loadingState) {
            is LoadingState.Fail -> throw BitmapLoadingAdapter.BitmapLoadingException(loadingState.cause)
            is LoadingState.Complete -> {
                if (loadingState.loadedImage == null) {
                    throw BitmapLoadingAdapter.BitmapLoadingException(IOException("Image loading failed."))
                }
            }
        }
    }

    fun displayAdImage(urn: Urn, imageUri: String, imageView: ImageView): Observable<LoadingState> {
        return imageLoader.displayImage(imageUri, imageView, displayType = DisplayType.STREAM_AD_IMAGE)
    }

    private fun load(urn: Urn,
                     imageUrlTemplate: String?,
                     apiImageSize: ApiImageSize,
                     targetWidth: Int,
                     targetHeight: Int): Observable<LoadingState> {
        val observable = toImageUrl(urn, imageUrlTemplate, apiImageSize)?.let {
            imageLoader.loadImage(it)
        }
        return observable ?: Observable.empty<LoadingState>()
    }

    private fun blurBitmap(original: Bitmap, blurRadius: Optional<Float>): Single<Bitmap> {
        return Single.create { subscriber ->
            subscriber.onSuccess(imageProcessor.blurBitmap(original, blurRadius))
        }
    }

    fun getImageUrl(urn: Urn, apiImageSize: ApiImageSize) = toImageUrl(urn = urn, apiImageSize = apiImageSize)

    private fun toImageUrl(urn: Urn, imageUrlTemplate: String? = null, apiImageSize: ApiImageSize): String? {
        val imageUrl = imageUrlBuilder.buildUrl(imageUrlTemplate, urn, apiImageSize)
        return if (notFoundUris.contains(imageUrl)) null else imageUrl
    }

    fun blurredArtwork(resources: Resources,
                       urn: Urn,
                       imageUrlTemplate: Optional<String>,
                       blurRadius: Optional<Float>,
                       scheduleOn: Scheduler, observeOn: Scheduler): Single<Bitmap> {
        val cachedBlurImage = imageCache.getBlurredImage(urn)
        if (cachedBlurImage != null) {
            return Single.just(cachedBlurImage)
        } else {
            val cached = getCachedListItemBitmap(resources, urn, imageUrlTemplate)
            return if (cached == null) {
                artwork(urn, imageUrlTemplate, ApiImageSize.getListItemImageSize(resources))
                        .map { imageProcessor.blurBitmap(it, blurRadius) }
                        .subscribeOn(scheduleOn)
                        .observeOn(observeOn)
                        .doOnSuccess(imageCache.cacheBlurredBitmap(urn))
                        .toSingle()
            } else {
                blurBitmap(cached, blurRadius)
                        .subscribeOn(scheduleOn)
                        .observeOn(observeOn)
                        .doOnSuccess(imageCache.cacheBlurredBitmap(urn))
            }
        }
    }

    private fun createFallbackBitmap(resourceUrn: Urn, width: Int, height: Int): Bitmap {
        // This bitmap is only used by the current track for the components that can't use
        // drawables (i.e. the notification and the remote client for the lock screen)
        //
        // Unless we refactor the cache to store a /GradientDrawable/ and not a /TransitionDrawable/
        // we don't have a cache for this guy.
        //
        // Also, we don't cache bitmap in the /ImageOperations/ since it does not worth it. A cache
        // may have a impact on the memory usage and without the performance seems pretty good, though.
        val fallbackDrawable = placeholderGenerator.generateDrawable(resourceUrn.toString())
        return ImageUtils.toBitmap(fallbackDrawable, width, height)
    }

    fun resume() {
        imageLoader.resume()
    }

    fun pause() {
        imageLoader.pause()
    }
}
