package com.soundcloud.android.view

import android.graphics.RectF
import com.soundcloud.android.testsupport.AndroidUnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@Suppress("IllegalIdentifier")
class InterstitialAdImageViewTest : AndroidUnitTest() {

    private val IMAGE_SIZE = 1024

    @Test
    fun `when view is the same size as the image, image is unchanged`() {
        test(1024, 1024, RectF(0f, 0f, 1024f, 1024f))
    }

    @Test
    fun `when image is the same height as the aspect ratio and there is exactly enough space for safe area, image is centered`() {
        test(722, 1024, RectF(-151f, 0f, 873f, 1024f))
    }

    @Test
    fun `when image is the same height as the aspect ratio and there is more than enough space for safe area, image is centered`() {
        test(1020, 1024, RectF(-2f, 0f, 1022f, 1024f))
    }

    @Test
    fun `when view is smaller than the image and has the same aspect ratio, image is scaled to match width and height`() {
        test(512, 512, RectF(0f, 0f, 512f, 512f))
    }

    @Test
    fun `when view is smaller than the image and has an aspect ratio matching the safe area, image is centered and scaled to match height`() {
        test(361, 512, RectF(-75.5f, 0f, 436.5f, 512f))
    }

    @Test
    fun `when view is smaller than the image and has a narrower aspect ratio than the safe area, image is centered and scaled to match safe area`() {
        test(361, 600, RectF(-75.5f, 0f, 436.5f, 512f))
    }

    @Test
    fun `when view is smaller than the image and has a wider aspect ratio than the safe area, image is centered and scaled to match height`() {
        test(400, 512, RectF(-56f, 0f, 456f, 512f))
    }

    @Test
    fun `when view is larger than the image and has an aspect ratio matching the safe area, image is centered and scaled to match height`() {
        test(1444, 2048, RectF(-302f, 0f, 1746f, 2048f))
    }

    @Test
    fun `when view is larger than the image and has a narrower aspect ratio than the safe area, image is centered and scaled to match safe area`() {
        test(1444, 3000, RectF(-302f, 0f, 1746f, 2048f))
    }

    @Test
    fun `when view is larger than the image and has a wider aspect ratio than the safe area, image is centered and scaled to match height`() {
        test(2040, 2056, RectF(-8f, 0f, 2048f, 2056f))
    }

    @Test
    fun `when view is larger than the image and has the same aspect ratio, image is scaled to match width and height`() {
        test(2048, 2048, RectF(0f, 0f, 2048f, 2048f))
    }

    private fun test(viewWidth: Int, viewHeight: Int, expected: RectF) {
        val imageRect = RectF(0f, 0f, IMAGE_SIZE.toFloat(), IMAGE_SIZE.toFloat())
        InterstitialAdImageView.computeMatrixForViewBounds(viewWidth, viewHeight, IMAGE_SIZE).mapRect(imageRect)
        assertThat(imageRect).isEqualTo(expected)
    }

}
