package com.soundcloud.android.image

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.TransitionDrawable
import android.view.View
import android.widget.ImageView
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.isNull
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.api.ApiEndpoints
import com.soundcloud.android.api.ApiUrlBuilder
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.android.utils.DeviceHelper
import com.soundcloud.android.utils.DisplayMetricsStub
import com.soundcloud.android.utils.cache.Cache.ValueProvider
import com.soundcloud.java.optional.Optional
import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock

@Suppress("IllegalIdentifier")
class ImageOperationsTest : AndroidUnitTest() {

    private lateinit var imageOperations: ImageOperations
    private val displayMetrics = DisplayMetricsStub()

    @Mock private lateinit var bitmapAction1: Consumer<Bitmap>
    @Mock internal lateinit var imageLoader: ImageLoader
    @Mock internal lateinit var apiUrlBuilder: ApiUrlBuilder
    @Mock internal lateinit var imageListener: ImageListener
    @Mock internal lateinit var imageView: ImageView
    @Mock internal lateinit var resources: Resources
    @Mock internal lateinit var cachedTransitionDrawable: TransitionDrawable
    @Mock internal lateinit var generatedTransitionDrawable: TransitionDrawable
    @Mock internal lateinit var gradientDrawable: GradientDrawable
    @Mock internal lateinit var parentView: View
    @Mock internal lateinit var viewlessLoadingAdapterFactory: FallbackBitmapLoadingAdapter.Factory
    @Mock internal lateinit var fallbackBitmapLoadingAdapter: FallbackBitmapLoadingAdapter
    @Mock internal lateinit var imageProcessor: ImageProcessor
    @Mock internal lateinit var configuration: Configuration
    @Mock internal lateinit var deviceHelper: DeviceHelper
    @Mock internal lateinit var imageCache: ImageCache
    @Mock internal lateinit var placeholderGenerator: PlaceholderGenerator
    @Mock internal lateinit var circularPlaceholderGenerator: CircularPlaceholderGenerator

    @Captor internal lateinit var cacheValueProviderCaptor: ArgumentCaptor<ValueProvider<String, TransitionDrawable>>

    @Before
    @Throws(Exception::class)
    fun setUp() {
        imageOperations = ImageOperations(
                imageLoader,
                ImageUrlBuilder(apiUrlBuilder),
                imageProcessor,
                placeholderGenerator,
                imageCache)

        whenever(placeholderGenerator.generateDrawable(any())).thenReturn(gradientDrawable)
        whenever(circularPlaceholderGenerator.generateDrawable(any())).thenReturn(gradientDrawable)
        whenever(apiUrlBuilder.from(eq(ApiEndpoints.IMAGES), eq(URN), any())).thenReturn(apiUrlBuilder)
        whenever(apiUrlBuilder.build()).thenReturn(RESOLVER_URL)
        whenever(resources.displayMetrics).thenReturn(displayMetrics)
        whenever(viewlessLoadingAdapterFactory.create(any(), any())).thenReturn(fallbackBitmapLoadingAdapter)
    }

    @Test
    fun `calls image loader on displayInAdapterView`() {
        initImageLoader()
        imageOperations.displayInAdapterView(URN, Optional.absent(), ApiImageSize.T120, imageView, true)
        verify(imageLoader).displayImage(eq(RESOLVER_URL),
                                         eq(imageView),
                                         eq(true),
                                         isNull(),
                                         eq(DisplayType.DEFAULT),
                                         eq(apiImageSize),
                                         eq(false))
    }

    @Test
    fun `returns bitmap passed to listener in single`() {
        val bitmap = mock<Bitmap>()
        val subject = initImageLoader()
        val observer = imageOperations.displayInAdapterViewSingle(urn = URN,
                                                                  apiImageSize = apiImageSize,
                                                                  imageView = imageView,
                                                                  fallbackDrawable = Optional.absent(),
                                                                  circular = true).test()
        subject.onNext(LoadingState.Complete(RESOLVER_URL, imageView, bitmap))

        verify(imageLoader).displayImage(eq(RESOLVER_URL),
                                         eq(imageView),
                                         eq(true),
                                         isNull(),
                                         eq(DisplayType.DEFAULT),
                                         eq(apiImageSize),
                                         eq(false))

        observer.assertValue(bitmap)
    }

    @Test
    fun `calls image loader on displayWithPlaceholder`() {
        initImageLoader()
        imageOperations.displayWithPlaceholder(URN, Optional.absent(), apiImageSize, imageView)
        verify(imageLoader).displayImage(eq(RESOLVER_URL),
                                         eq(imageView),
                                         eq(false),
                                         isNull(),
                                         eq(DisplayType.PLACEHOLDER),
                                         eq(apiImageSize),
                                         eq(false))
    }

    @Test
    fun `calls image loader on displayWithPlaceholder and returns bitmap in observable`() {
        val bitmap = mock<Bitmap>()
        val subject = initImageLoader()
        val observer = imageOperations.displayWithPlaceholderObservable(URN, apiImageSize = apiImageSize, imageView = imageView).test()
        subject.onNext(LoadingState.Complete(RESOLVER_URL, imageView, bitmap))
        verify(imageLoader).displayImage(eq(RESOLVER_URL),
                                         eq(imageView),
                                         eq(false),
                                         isNull(),
                                         eq(DisplayType.PLACEHOLDER),
                                         eq(apiImageSize),
                                         eq(false))

        observer.assertValue(bitmap)
    }

    @Test
    fun `fails observable on displayWithPlaceholder loading fail`() {
        val runtimeException = RuntimeException("Image loading failed")
        val subject = initImageLoader()

        val observer = imageOperations.displayWithPlaceholderObservable(URN, apiImageSize = apiImageSize, imageView = imageView).test()

        subject.onNext(LoadingState.Fail(RESOLVER_URL, imageView, runtimeException))

        verify(imageLoader).displayImage(eq(RESOLVER_URL),
                                         eq(imageView),
                                         eq(false),
                                         isNull(),
                                         eq(DisplayType.PLACEHOLDER),
                                         eq(apiImageSize),
                                         eq(false))

        observer.assertError { it.cause == runtimeException }.assertError { it is BitmapLoadingAdapter.BitmapLoadingException }
    }

    @Test
    fun `calls image loader on displayDefaultWithPlaceholder`() {
        initImageLoader()
        imageOperations.displayDefaultPlaceholder(imageView)
        verify(imageLoader).displayImage(isNull(),
                                         eq(imageView),
                                         eq(false),
                                         isNull(),
                                         eq(DisplayType.PLACEHOLDER),
                                         eq(ApiImageSize.Unknown),
                                         eq(false))
    }

    @Test
    fun `calls image loader on displayCircular`() {
        initImageLoader()
        imageOperations.displayCircular(RESOLVER_URL, imageView)
        verify(imageLoader).displayImage(eq(RESOLVER_URL),
                                         eq(imageView),
                                         eq(true),
                                         isNull(),
                                         eq(DisplayType.DEFAULT),
                                         eq(apiImageSize),
                                         eq(false))
    }

    @Test
    fun `calls image loader on displayCircularWithPlaceholder`() {
        initImageLoader()
        imageOperations.displayCircularWithPlaceholder(URN, Optional.absent(), apiImageSize, imageView)
        verify(imageLoader).displayImage(eq(RESOLVER_URL),
                                         eq(imageView),
                                         eq(true),
                                         isNull(),
                                         eq(DisplayType.DEFAULT),
                                         eq(apiImageSize),
                                         eq(false))
    }

    @Test
    fun `calls image loader on displayInPlayer`() {
        initImageLoader()
        imageOperations.displayInPlayer(URN, apiImageSize = apiImageSize, imageView = imageView, placeholder = null, isHighPriority = true)
        verify(imageLoader).displayImage(eq(RESOLVER_URL),
                                         eq(imageView),
                                         eq(false),
                                         isNull(),
                                         eq(DisplayType.PLAYER),
                                         eq(apiImageSize),
                                         eq(true))

    }

    @Test
    fun `calls image loader on displayLeaveBehind`() {
        imageOperations.displayLeaveBehind(RESOLVER_URL, imageView)
        verify(imageLoader).displayImage(eq(RESOLVER_URL),
                                         eq(imageView),
                                         eq(false),
                                         isNull(),
                                         eq(DisplayType.AD),
                                         eq(ApiImageSize.Unknown),
                                         eq(false))
    }

    private fun initImageLoader(): PublishSubject<LoadingState> {
        val imageSubject = PublishSubject.create<LoadingState>()
        whenever(imageLoader.displayImage(anyOrNull(), any(), any(), anyOrNull(), any(), any(), any())).thenReturn(imageSubject)
        imageSubject.onNext(LoadingState.Start(RESOLVER_URL, imageView))
        return imageSubject
    }

    companion object {

        private val RESOLVER_URL = "https://api-mobile.soundcloud.com/images/soundcloud:tracks:1/large"
        private val URN = Urn("soundcloud:tracks:1")
        private val apiImageSize = ApiImageSize.T120
    }
}
