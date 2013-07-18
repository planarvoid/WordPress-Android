package com.soundcloud.android.utils.images;

import com.google.common.annotations.VisibleForTesting;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.display.BitmapDisplayer;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.ImageView;

public class ImageOptionsFactory {


    public static DisplayImageOptions list(int defaultIconResId){
        return new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisc(true)
                .resetViewBeforeLoading(true)
                .showImageOnFail(defaultIconResId)
                .showImageForEmptyUri(defaultIconResId)
                .showStubImage(defaultIconResId)
                .displayer(new ListAnimateListener())
                .build();
    }

    public static DisplayImageOptions placeholder(int defaultIconResId){
        return new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisc(true)
                .showImageForEmptyUri(defaultIconResId)
                .showImageOnFail(defaultIconResId)
                .showStubImage(defaultIconResId)
                .build();
    }

    public static DisplayImageOptions prefetch() {
        return new DisplayImageOptions.Builder()
                .cacheInMemory(false)
                .cacheOnDisc(true)
                .build();
    }

    public static DisplayImageOptions cache(){
        return new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisc(true)
                .build();
    }

    /**
     * Prevents image flashing on subsequent loads in lists
     */
    @VisibleForTesting
    static class ListAnimateListener implements BitmapDisplayer {
        @Override
        public Bitmap display(Bitmap bitmap, final ImageView imageView, LoadedFrom loadedFrom) {
            if (bitmap != null) {
                if (loadedFrom != LoadedFrom.MEMORY_CACHE) {
                    final Drawable from = imageView.getDrawable();
                    TransitionDrawable tDrawable = new TransitionDrawable(
                            new Drawable[]{
                                    from == null ? new BitmapDrawable() : from,
                                    new BitmapDrawable(bitmap)
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
                } else {
                    imageView.setImageBitmap(bitmap);
                }
            }
            return bitmap;
        }
    }
}
