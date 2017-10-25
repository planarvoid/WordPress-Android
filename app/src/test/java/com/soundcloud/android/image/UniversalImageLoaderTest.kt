package com.soundcloud.android.image

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.isNull
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import com.nostra13.universalimageloader.cache.disc.DiskCache
import com.nostra13.universalimageloader.cache.memory.MemoryCache
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.assist.FailReason
import com.nostra13.universalimageloader.core.imageaware.ImageAware
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener
import com.soundcloud.android.properties.ApplicationProperties
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.android.utils.DeviceHelper
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock

class UniversalImageLoaderTest : AndroidUnitTest() {

    private lateinit var universalImageLoader: UniversalImageLoader

    @Mock lateinit var imageLoader: ImageLoader
    @Mock lateinit var imageCache: ImageCache
    @Mock lateinit var placeholderGenerator: PlaceholderGenerator
    @Mock lateinit var circularPlaceholderGenerator: CircularPlaceholderGenerator
    @Mock lateinit var deviceHelper: DeviceHelper
    @Mock lateinit var imageOptionsFactory: UniversalImageOptionsFactory

    @Mock lateinit var diskCache: DiskCache
    @Mock lateinit var memoryCache: MemoryCache
    @Mock lateinit var applicationProperties: ApplicationProperties
    @Mock lateinit var imageDownloaderFactory: UniversalImageDownloader.Factory

    @Captor lateinit var imageLoadingListenerCaptor: ArgumentCaptor<ImageLoadingListener>
    @Captor lateinit var throwableCaptor: ArgumentCaptor<Throwable>

    @Before
    fun setUp() {
        universalImageLoader = UniversalImageLoader(imageLoader,
                                                    imageCache,
                                                    placeholderGenerator,
                                                    circularPlaceholderGenerator,
                                                    imageOptionsFactory,
                                                    deviceHelper,
                                                    context(),
                                                    applicationProperties,
                                                    imageDownloaderFactory
        )
    }

    private val imageUrl: String = "imageUrl"

    @Test
    fun `load image for ad`() {
        val options = DisplayImageOptions.createSimple()
        whenever(imageOptionsFactory.adImage()).thenReturn(options)

        val testObserver = universalImageLoader.loadImage(imageUrl, LoadType.AD).test()

        verify(imageOptionsFactory).adImage()
        verify(imageLoader).loadImage(eq(imageUrl), eq(options), imageLoadingListenerCaptor.capture())

        testListener(testObserver)
    }

    @Test
    fun `load image for prefetch`() {
        val options = DisplayImageOptions.createSimple()
        whenever(imageOptionsFactory.prefetch()).thenReturn(options)

        val testObserver = universalImageLoader.loadImage(imageUrl, LoadType.PREFETCH).test()

        verify(imageOptionsFactory).prefetch()
        verify(imageLoader).loadImage(eq(imageUrl), eq(options), imageLoadingListenerCaptor.capture())

        testListener(testObserver)
    }

    @Test
    fun `load image for missing load type`() {
        val testObserver = universalImageLoader.loadImage(imageUrl, LoadType.NONE).test()

        verifyZeroInteractions(imageOptionsFactory)
        verify(imageLoader).loadImage(eq(imageUrl), isNull<DisplayImageOptions>(), imageLoadingListenerCaptor.capture())

        testListener(testObserver)
    }

    @Test
    fun `display image for default circular`() {
        val displayImageOptions = DisplayImageOptions.createSimple()
        whenever(imageOptionsFactory.adapterViewCircular(any(), any(), any())).thenReturn(displayImageOptions)
        val view = mock<ImageView>()
        val placeholder = mock<Drawable>()

        val testObserver = universalImageLoader.displayImage(imageUrl,
                                                             view,
                                                             circular = true,
                                                             placeholderDrawable = placeholder,
                                                             displayType = DisplayType.DEFAULT,
                                                             apiImageSize = ApiImageSize.T120).test()

        verify(imageOptionsFactory).adapterViewCircular(placeholder, ApiImageSize.T120, deviceHelper)
        verify(imageLoader).displayImage(eq(imageUrl), any<ImageAware>(), eq(displayImageOptions), imageLoadingListenerCaptor.capture())

        testListener(testObserver)
    }

    @Test
    fun `display image for default`() {
        val displayImageOptions = DisplayImageOptions.createSimple()
        whenever(imageOptionsFactory.adapterView(any(), any(), any())).thenReturn(displayImageOptions)
        val view = mock<ImageView>()
        val placeholder = mock<Drawable>()

        val testObserver = universalImageLoader.displayImage(imageUrl,
                                                             view,
                                                             circular = false,
                                                             placeholderDrawable = placeholder,
                                                             displayType = DisplayType.DEFAULT,
                                                             apiImageSize = ApiImageSize.T120).test()

        verify(imageOptionsFactory).adapterView(placeholder, ApiImageSize.T120, deviceHelper)
        verify(imageLoader).displayImage(eq(imageUrl), any<ImageAware>(), eq(displayImageOptions), imageLoadingListenerCaptor.capture())

        testListener(testObserver)
    }

    @Test
    fun `display image for placeholder`() {
        val displayImageOptions = DisplayImageOptions.createSimple()
        whenever(imageOptionsFactory.placeholder(any())).thenReturn(displayImageOptions)
        val view = mock<ImageView>()
        val placeholder = mock<Drawable>()

        val testObserver = universalImageLoader.displayImage(imageUrl,
                                                             view,
                                                             circular = false,
                                                             placeholderDrawable = placeholder,
                                                             displayType = DisplayType.PLACEHOLDER,
                                                             apiImageSize = ApiImageSize.T120).test()

        verify(imageOptionsFactory).placeholder(placeholder)
        verify(imageLoader).displayImage(eq(imageUrl), any<ImageAware>(), eq(displayImageOptions), imageLoadingListenerCaptor.capture())

        testListener(testObserver)
    }

    @Test
    fun `display image for player`() {
        val displayImageOptions = DisplayImageOptions.createSimple()
        whenever(imageOptionsFactory.player(any(), any())).thenReturn(displayImageOptions)
        val view = mock<ImageView>()
        val placeholder = mock<Drawable>()

        val testObserver = universalImageLoader.displayImage(imageUrl,
                                                             view,
                                                             circular = false,
                                                             placeholderDrawable = placeholder,
                                                             displayType = DisplayType.PLAYER,
                                                             apiImageSize = ApiImageSize.T120,
                                                             isHighPriority = true).test()

        verify(imageOptionsFactory).player(placeholder, true)
        verify(imageLoader).displayImage(eq(imageUrl), any<ImageAware>(), eq(displayImageOptions), imageLoadingListenerCaptor.capture())

        testListener(testObserver)
    }

    @Test
    fun `display image for ad`() {
        val displayImageOptions = DisplayImageOptions.createSimple()
        whenever(imageOptionsFactory.adImage()).thenReturn(displayImageOptions)
        val view = mock<ImageView>()
        val placeholder = mock<Drawable>()

        val testObserver = universalImageLoader.displayImage(imageUrl,
                                                             view,
                                                             circular = false,
                                                             placeholderDrawable = placeholder,
                                                             displayType = DisplayType.AD,
                                                             apiImageSize = ApiImageSize.T120,
                                                             isHighPriority = true).test()

        verify(imageOptionsFactory).adImage()
        verify(imageLoader).displayImage(eq(imageUrl), any<ImageAware>(), eq(displayImageOptions), imageLoadingListenerCaptor.capture())

        testListener(testObserver)
    }

    @Test
    fun `display image for full image dialog`() {
        val displayImageOptions = DisplayImageOptions.createSimple()
        whenever(imageOptionsFactory.fullImageDialog()).thenReturn(displayImageOptions)
        val view = mock<ImageView>()
        val placeholder = mock<Drawable>()

        val testObserver = universalImageLoader.displayImage(imageUrl,
                                                             view,
                                                             circular = false,
                                                             placeholderDrawable = placeholder,
                                                             displayType = DisplayType.FULL_IMAGE_DIALOG,
                                                             apiImageSize = ApiImageSize.T120,
                                                             isHighPriority = true).test()

        verify(imageOptionsFactory).fullImageDialog()
        verify(imageLoader).displayImage(eq(imageUrl), any<ImageAware>(), eq(displayImageOptions), imageLoadingListenerCaptor.capture())

        testListener(testObserver)
    }

    @Test
    fun `display image for stream ad image`() {
        val displayImageOptions = DisplayImageOptions.createSimple()
        whenever(imageOptionsFactory.streamAdImage(any(), any())).thenReturn(displayImageOptions)
        val view = mock<ImageView>()
        val placeholder = mock<Drawable>()

        val testObserver = universalImageLoader.displayImage(imageUrl,
                                                             view,
                                                             circular = false,
                                                             placeholderDrawable = placeholder,
                                                             displayType = DisplayType.STREAM_AD_IMAGE,
                                                             apiImageSize = ApiImageSize.T120,
                                                             isHighPriority = true).test()

        verify(imageOptionsFactory).streamAdImage(placeholder, deviceHelper)
        verify(imageLoader).displayImage(eq(imageUrl), any<ImageAware>(), eq(displayImageOptions), imageLoadingListenerCaptor.capture())

        testListener(testObserver)
    }

    private fun testListener(testObserver: TestObserver<LoadingState>) {
        val mockImage = mock<View>()
        val mockBitmap = mock<Bitmap>()
        val throwable = mock<Throwable>()
        imageLoadingListenerCaptor.value.onLoadingStarted(imageUrl, mockImage)
        imageLoadingListenerCaptor.value.onLoadingFailed(imageUrl, mockImage, FailReason(FailReason.FailType.NETWORK_DENIED, throwable))
        imageLoadingListenerCaptor.value.onLoadingComplete(imageUrl, mockImage, mockBitmap)

        testObserver.assertValueAt(0, LoadingState.Start(imageUrl, mockImage))
        testObserver.assertValueAt(1, {
            it is LoadingState.Fail &&
                    it.imageUrl == imageUrl &&
                    it.view == mockImage &&
                    it.cause.message == FailReason.FailType.NETWORK_DENIED.toString() &&
                    it.cause.cause == throwable
        })
        testObserver.assertValueAt(2, LoadingState.Complete(imageUrl, mockImage, mockBitmap))
    }
}
