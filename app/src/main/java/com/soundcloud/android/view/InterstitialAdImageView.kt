package com.soundcloud.android.view

import android.content.Context
import android.graphics.Matrix
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.widget.ImageView

class InterstitialAdImageView constructor(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {

    init {
        scaleType = ImageView.ScaleType.MATRIX
    }

    companion object {

        private const val DEFAULT_SAFE_AREA_WIDTH = 722f
        private const val DEFAULT_SAFE_AREA_HEIGHT = 1024f
        private const val SAFE_AREA_ASPECT_RATIO = DEFAULT_SAFE_AREA_WIDTH / DEFAULT_SAFE_AREA_HEIGHT

        fun computeMatrixForViewBounds(viewWidth: Int, viewHeight: Int, imageSize: Int): Matrix {
            // Interstitial ad images are always square. When the device is in landscape mode,
            // the image is scaled to fill the vertical space. No cropping occurs. When the device
            // is in portrait mode, the sides of the image are cropped so that the more important center
            // of the image can fill more of the available vertical space. Cropping is only allowed
            // outside the "safe area", defined as the center 722 x 1024 pixels of a 1024 x 1024 image.

            // First determine whether the view or the safe area has a narrower aspect ration.
            val scale = if (viewWidth.toFloat() / viewHeight < SAFE_AREA_ASPECT_RATIO) {
                // The view has a narrower aspect ratio than the safe area.
                // The image cannot be scaled beyond the point at which the safe area would be cropped.
                viewWidth / (SAFE_AREA_ASPECT_RATIO * imageSize)
            } else {
                // The safe area has a narrower aspect ratio than the view.
                // The image can be scaled to fit the view vertically. All of the safe area will still be visible.
                viewHeight / imageSize.toFloat()
            }

            return Matrix().apply {
                setScale(scale, scale)
                postTranslate(- (imageSize * scale - viewWidth) / 2, 0f)
            }
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (drawable == null) {
            return
        }

        check(drawable.intrinsicHeight == drawable.intrinsicWidth) { "Unexpected interstitial image size: ${drawable.intrinsicWidth} x ${drawable.intrinsicHeight}" }

        imageMatrix = computeMatrixForViewBounds(width - paddingLeft - paddingRight, height - paddingTop - paddingBottom, drawable.intrinsicHeight)

    }
}
