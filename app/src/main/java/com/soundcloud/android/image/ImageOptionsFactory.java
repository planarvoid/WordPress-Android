package com.soundcloud.android.image;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.display.BitmapDisplayer;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.images.ImageUtils;
import org.jetbrains.annotations.Nullable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.widget.ImageView;

final class ImageOptionsFactory {

    static final int DELAY_BEFORE_LOADING = 200;

    static DisplayImageOptions adapterViewCircular(@Nullable Drawable placeholderDrawable,
                                                   ApiImageSize apiImageSize,
                                                   DeviceHelper deviceHelper) {
        return adapterView(placeholderDrawable, apiImageSize, new CircularTransitionDisplayer(), deviceHelper);
    }

    static DisplayImageOptions adapterView(@Nullable Drawable placeholderDrawable,
                                           ApiImageSize apiImageSize,
                                           DeviceHelper deviceHelper) {
        return adapterView(placeholderDrawable, apiImageSize, new PlaceholderTransitionDisplayer(), deviceHelper);
    }

    private static DisplayImageOptions adapterView(@Nullable Drawable placeholderDrawable,
                                                   ApiImageSize apiImageSize,
                                                   PlaceholderTransitionDisplayer displayer,
                                                   DeviceHelper deviceHelper) {
        DisplayImageOptions.Builder options = fullCacheBuilder()
                .resetViewBeforeLoading(true)
                .showImageOnLoading(placeholderDrawable)
                .showImageOnFail(placeholderDrawable)
                .showImageForEmptyUri(placeholderDrawable)
                .displayer(displayer);

        if (ApiImageSize.SMALL_SIZES.contains(apiImageSize)
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                || deviceHelper.isLowMemoryDevice()
                || deviceHelper.isTablet()) {
            options.bitmapConfig(Bitmap.Config.RGB_565);
        }

        return options.build();
    }

    public static DisplayImageOptions fullImageDialog() {
        return new DisplayImageOptions.Builder()
                .cacheOnDisk(true)
                .delayBeforeLoading(DELAY_BEFORE_LOADING)
                .displayer(new FadeInBitmapDisplayer(DELAY_BEFORE_LOADING))
                .build();
    }

    public static DisplayImageOptions placeholder(@Nullable Drawable placeholderDrawable) {
        return fullCacheBuilder()
                .showImageOnLoading(placeholderDrawable)
                .showImageForEmptyUri(placeholderDrawable)
                .showImageOnFail(placeholderDrawable)
                .build();
    }

    public static DisplayImageOptions placeholderCircular(@Nullable Drawable placeholderDrawable) {
        return fullCacheBuilder()
                .showImageOnLoading(placeholderDrawable)
                .showImageForEmptyUri(placeholderDrawable)
                .showImageOnFail(placeholderDrawable)
                .displayer(new CircularTransitionDisplayer())
                .build();
    }

    public static DisplayImageOptions player(@Nullable Drawable placeholderDrawable, boolean isHighPriority) {
        DisplayImageOptions.Builder options = fullCacheBuilder()
                .showImageOnLoading(placeholderDrawable)
                .showImageForEmptyUri(placeholderDrawable)
                .showImageOnFail(placeholderDrawable)
                .displayer(new PlaceholderTransitionDisplayer());

        if (!isHighPriority) {
            options.delayBeforeLoading(DELAY_BEFORE_LOADING);
        }
        return options.build();
    }

    public static DisplayImageOptions adImage() {
        return new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(false)
                .build();
    }

    public static DisplayImageOptions prefetch() {
        return new DisplayImageOptions.Builder()
                .cacheInMemory(false)
                .cacheOnDisk(true)
                .build();
    }

    public static DisplayImageOptions cache() {
        return fullCacheBuilder()
                .build();
    }

    public static DisplayImageOptions.Builder fullCacheBuilder() {
        return new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true);
    }

    @VisibleForTesting
    static class CircularTransitionDisplayer extends PlaceholderTransitionDisplayer {
        @Override
        protected Drawable createBitmapDrawable(Bitmap bitmap, Resources resources) {
            return ImageUtils.createCircularDrawable(bitmap, resources);
        }

        @Override
        protected void setBitmapImage(ImageAware imageAware, Bitmap bitmap) {
            imageAware.setImageDrawable(createBitmapDrawable(bitmap, imageAware.getWrappedView().getResources()));
        }
    }

    @VisibleForTesting
    static class PlaceholderTransitionDisplayer extends BitmapTransitionDisplayer {
        @Override
        protected Drawable getTransitionFromDrawable(ImageView imageView) {
            return imageView.getDrawable();
        }

        @Override
        protected Drawable createBitmapDrawable(Bitmap bitmap, Resources resources) {
            return new BitmapDrawable(resources, bitmap);
        }

        @Override
        protected void setBitmapImage(ImageAware imageAware, Bitmap bitmap) {
            imageAware.setImageBitmap(bitmap);
        }
    }

    @VisibleForTesting
    static abstract class BitmapTransitionDisplayer implements BitmapDisplayer {

        abstract protected Drawable getTransitionFromDrawable(ImageView imageView);

        abstract protected Drawable createBitmapDrawable(Bitmap bitmap, Resources resources);

        @Override
        public void display(Bitmap bitmap, ImageAware imageAware, LoadedFrom loadedFrom) {
            ImageView wrappedImageView = (ImageView) imageAware.getWrappedView();
            if (wrappedImageView != null && bitmap != null) {
                if (loadedFrom != LoadedFrom.MEMORY_CACHE) {
                    performDrawableTransition(bitmap, wrappedImageView);
                } else {
                    setBitmapImage(imageAware, bitmap);
                }
            }
        }

        protected abstract void setBitmapImage(ImageAware imageAware, Bitmap bitmap);

        protected void performDrawableTransition(Bitmap bitmap, final ImageView imageView) {
            final Drawable from = getTransitionFromDrawable(imageView);
            final Drawable to = createBitmapDrawable(bitmap, imageView.getResources());

            TransitionDrawable tDrawable = ImageUtils.createTransitionDrawable(from, to);
            tDrawable.setCallback(new Drawable.Callback() {
                @Override
                public void scheduleDrawable(Drawable drawable, Runnable runnable, long l) {
                }

                @Override
                public void unscheduleDrawable(Drawable drawable, Runnable runnable) {
                }

                @Override
                public void invalidateDrawable(Drawable drawable) {
                    imageView.invalidate();
                }
            });
            tDrawable.startTransition(ImageUtils.DEFAULT_TRANSITION_DURATION);
            imageView.setImageDrawable(tDrawable);
        }
    }

    private ImageOptionsFactory() {
    }
}
