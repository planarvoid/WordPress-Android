package com.soundcloud.android.image;

import com.google.common.annotations.VisibleForTesting;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.display.BitmapDisplayer;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.AnimUtils;

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

    @Deprecated
    public static DisplayImageOptions adapterView(int defaultIconResId){
        return fullCacheBuilder()
                .resetViewBeforeLoading(true)
                .showImageOnFail(defaultIconResId)
                .showImageForEmptyUri(defaultIconResId)
                .showImageOnLoading(defaultIconResId)
                .displayer(new PlaceholderTransitionDisplayer())
                .build();
    }

    static DisplayImageOptions listView() {
        return fullCacheBuilder()
                .resetViewBeforeLoading(true)
                .showImageOnFail(R.drawable.placeholder_cells)
                .showImageForEmptyUri(R.drawable.placeholder_cells)
                .showImageOnLoading(R.drawable.placeholder_cells)
                .displayer(new PlaceholderTransitionDisplayer())
                .build();
    }

    public static DisplayImageOptions gridView(){
        return fullCacheBuilder()
                .resetViewBeforeLoading(true)
                .displayer(new BackgroundTransitionDisplayer())
                .build();
    }

    public static DisplayImageOptions fullImageDialog() {
        return new DisplayImageOptions.Builder()
                .cacheOnDisc(true)
                .delayBeforeLoading(200)
                .displayer(new FadeInBitmapDisplayer(200))
                .build();
    }

    public static DisplayImageOptions placeholder(int defaultIconResId){
        return fullCacheBuilder()
                .showImageForEmptyUri(defaultIconResId)
                .showImageOnFail(defaultIconResId)
                .showImageOnLoading(defaultIconResId)
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
        public Bitmap display(Bitmap bitmap, ImageAware imageAware, LoadedFrom loadedFrom) {
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
            return bitmap;
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
    static class BackgroundTransitionDisplayer extends BitmapTransitionDisplayer {
        @Override
        protected Drawable getTransitionFromDrawable(ImageView imageView) {
            return imageView.getBackground();
        }

        @Override
        protected void performDrawableTransition(Bitmap bitmap, ImageView imageView) {
            super.performDrawableTransition(bitmap, imageView);
            imageView.setBackgroundDrawable(null);
        }
    }

    @VisibleForTesting
    static abstract class BitmapTransitionDisplayer implements BitmapDisplayer {

        abstract protected Drawable getTransitionFromDrawable(ImageView imageView);

        @Override
        public Bitmap display(Bitmap bitmap, ImageAware imageAware, LoadedFrom loadedFrom) {
            ImageView wrappedImageView = (ImageView) imageAware.getWrappedView();
            if (bitmap != null) {
                if (loadedFrom != LoadedFrom.MEMORY_CACHE) {
                    performDrawableTransition(bitmap, wrappedImageView);
                } else {
                    imageAware.setImageBitmap(bitmap);
                }
            }
            return bitmap;
        }

        protected void performDrawableTransition(Bitmap bitmap, final ImageView imageView) {
            final Drawable from = getTransitionFromDrawable(imageView);
            TransitionDrawable tDrawable = new TransitionDrawable(
                    new Drawable[]{
                            from == null ? new BitmapDrawable(imageView.getResources()) : from,
                            new BitmapDrawable(imageView.getResources(), bitmap)
                    });
            tDrawable.setCrossFadeEnabled(true);
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
            tDrawable.startTransition(200);
            imageView.setImageDrawable(tDrawable);
        }
    }
}
