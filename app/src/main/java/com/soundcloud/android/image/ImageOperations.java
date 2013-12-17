package com.soundcloud.android.image;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.soundcloud.android.cache.FileCache;
import com.soundcloud.android.utils.IOUtils;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;

public class ImageOperations {

    private static final int LOW_MEM_DEVICE_THRESHOLD = 50 * 1000 * 1000; // available mem in bytes

    private ImageLoader mImageLoader;

    @Inject
    public ImageOperations(ImageLoader imageLoader) {
        mImageLoader = imageLoader;
    }

    public void initialise(Context context) {
        Context appContext = context.getApplicationContext();
        final ImageLoaderConfiguration.Builder builder = new ImageLoaderConfiguration.Builder(appContext);
        builder.defaultDisplayImageOptions(ImageOptionsFactory.cache());
        final long availableMemory = Runtime.getRuntime().maxMemory();
        // Here are some reference values for available mem: Wildfire: 16,777,216; Nexus S: 33,554,432; Nexus 4: 201,326,592
        if (availableMemory < LOW_MEM_DEVICE_THRESHOLD) {
            // cut down to half of what UIL would reserve by default (div 8) on low mem devices
            builder.memoryCacheSize((int) (availableMemory / 16));
        }
        mImageLoader.init(builder.build());

        FileCache.installFileCache(IOUtils.getCacheDir(appContext));
    }

    public void displayInGridView(String imageUrl, ImageView imageView) {
        mImageLoader.displayImage(imageUrl, imageView, ImageOptionsFactory.gridView());
    }

    public void displayInAdapterView(String imageUrl, ImageView imageView, int defaultResId) {
        mImageLoader.displayImage(imageUrl, imageView, ImageOptionsFactory.adapterView(defaultResId));
    }

    public void displayInPlayerView(String imageUrl, ImageView imageView, View parentView, boolean priority, ImageListener imageListener) {
        mImageLoader.displayImage(imageUrl, imageView, ImageOptionsFactory.player(parentView, priority), (ImageLoadingListener) imageListener);
    }

    public void displayPlaceholder(String imageUrl, ImageView imageView, int defaultResId) {
        mImageLoader.displayImage(imageUrl, imageView, ImageOptionsFactory.placeholder(defaultResId));
    }

    public void prefetch(String imageurl) {
        mImageLoader.loadImage(imageurl, ImageOptionsFactory.prefetch(), null);
    }

    public void cancel(ImageView imageView) {
        mImageLoader.cancelDisplayTask(imageView);
    }

}
