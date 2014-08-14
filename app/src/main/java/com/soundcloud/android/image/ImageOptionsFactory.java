package com.soundcloud.android.image;

import com.google.common.annotations.VisibleForTesting;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.display.BitmapDisplayer;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.images.ImageUtils;
import org.jetbrains.annotations.Nullable;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;

class ImageOptionsFactory {

    @VisibleForTesting
    final static int DELAY_BEFORE_LOADING_HIGH_PRIORITY = 0;
    final static int DELAY_BEFORE_LOADING_LOW_PRIORITY = 200;

    static DisplayImageOptions adapterView(@Nullable Drawable placeholderDrawable) {
        return fullCacheBuilder()
                .resetViewBeforeLoading(true)
                .showImageOnLoading(placeholderDrawable)
                .showImageOnFail(placeholderDrawable)
                .showImageForEmptyUri(placeholderDrawable)
                .displayer(new PlaceholderTransitionDisplayer())
                .build();
    }

    public static DisplayImageOptions fullImageDialog() {
        return new DisplayImageOptions.Builder()
                .cacheOnDisc(true)
                .delayBeforeLoading(200)
                .displayer(new FadeInBitmapDisplayer(200))
                .build();
    }

    public static DisplayImageOptions placeholder(@Nullable Drawable placeholderDrawable){
        return fullCacheBuilder()
                .showImageOnLoading(placeholderDrawable)
                .showImageForEmptyUri(placeholderDrawable)
                .showImageOnFail(placeholderDrawable)
                .build();
    }

    public static DisplayImageOptions player(@Nullable Drawable placeholderDrawable){
        return fullCacheBuilder()
                .showImageOnLoading(placeholderDrawable)
                .showImageForEmptyUri(placeholderDrawable)
                .showImageOnFail(placeholderDrawable)
                .displayer(new PlaceholderTransitionDisplayer())
                .build();
    }


    public static DisplayImageOptions prefetch() {
        return new DisplayImageOptions.Builder()
                .cacheInMemory(false)
                .cacheOnDisc(true)
                .build();
    }

    public static DisplayImageOptions cache(){
        return fullCacheBuilder()
                .build();
    }

    public static DisplayImageOptions player(View parentView, boolean priority) {
        return fullCacheBuilder()
                .delayBeforeLoading(priority ? DELAY_BEFORE_LOADING_HIGH_PRIORITY : DELAY_BEFORE_LOADING_LOW_PRIORITY)
                .displayer(new PlayerBitmapDisplayer(parentView))
                .build();
    }

    public static DisplayImageOptions.Builder fullCacheBuilder() {
        return new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisc(true);
    }

    @VisibleForTesting
    static class PlayerBitmapDisplayer implements BitmapDisplayer {
        private View mParentView;

        PlayerBitmapDisplayer(View parentView) {
            mParentView = parentView;
        }



        @Override
        public void display(Bitmap bitmap, ImageAware imageAware, LoadedFrom loadedFrom) {
            final View wrappedView = imageAware.getWrappedView();
            imageAware.setImageBitmap(bitmap);
            if (wrappedView.getVisibility() != View.VISIBLE) { // keep this, presents flashing on second load
                if (loadedFrom == LoadedFrom.NETWORK) {
                    AnimUtils.runFadeInAnimationOn(wrappedView.getContext(), wrappedView);
                    wrappedView.getAnimation().setAnimationListener(new AnimUtils.SimpleAnimationListener() {
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            if (animation.equals(wrappedView.getAnimation())) {
                                mParentView.setBackgroundDrawable(null);
                            }
                        }
                    });
                    wrappedView.setVisibility(View.VISIBLE);
                } else {
                    wrappedView.setVisibility(View.VISIBLE);
                    mParentView.setBackgroundDrawable(null);
                }
            }
        }
    }

    /**
     * Prevents image flashing on subsequent loads in lists
     */
    @VisibleForTesting
    static class PlaceholderTransitionDisplayer extends BitmapTransitionDisplayer {
        @Override
        protected Drawable getTransitionFromDrawable(ImageView imageView) {
            return imageView.getDrawable();
        }
    }

    @VisibleForTesting
    static abstract class BitmapTransitionDisplayer implements BitmapDisplayer {

        abstract protected Drawable getTransitionFromDrawable(ImageView imageView);

        @Override
        public void display(Bitmap bitmap, ImageAware imageAware, LoadedFrom loadedFrom) {
            ImageView wrappedImageView = (ImageView) imageAware.getWrappedView();
            if (wrappedImageView != null && bitmap != null) {
                if (loadedFrom != LoadedFrom.MEMORY_CACHE) {
                    performDrawableTransition(bitmap, wrappedImageView);
                } else {
                    imageAware.setImageBitmap(bitmap);
                }
            }
        }

        protected void performDrawableTransition(Bitmap bitmap, final ImageView imageView) {
            final Drawable from = getTransitionFromDrawable(imageView);
            final BitmapDrawable to = new BitmapDrawable(imageView.getResources(), bitmap);

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
}
