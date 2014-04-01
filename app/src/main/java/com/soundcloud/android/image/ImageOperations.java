package com.soundcloud.android.image;

import com.google.common.annotations.VisibleForTesting;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.soundcloud.android.cache.FileCache;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageOperations {

    private static final int LOW_MEM_DEVICE_THRESHOLD = 50 * 1024 * 1024; // available mem in bytes
    private static final Pattern PATTERN = Pattern.compile("^https?://(.+)");
    private static final String URL_BASE = "http://%s";

    private final ImageLoader mImageLoader;
    private final ImageEndpointBuilder mImageEndpointBuilder;
    private final PlaceholderGenerator mPlaceholderGenerator;

    @Inject
    public ImageOperations(ImageEndpointBuilder imageEndpointBuilder, PlaceholderGenerator placeholderGenerator) {
        mImageLoader = ImageLoader.getInstance();
        mImageEndpointBuilder = imageEndpointBuilder;
        mPlaceholderGenerator = placeholderGenerator;
    }

    @VisibleForTesting
    ImageOperations(ImageLoader imageLoader, ImageEndpointBuilder imageEndpointBuilder, PlaceholderGenerator placeholderGenerator) {
        mImageLoader = imageLoader;
        mImageEndpointBuilder = imageEndpointBuilder;
        mPlaceholderGenerator = placeholderGenerator;
    }

    public void initialise(Context context, ApplicationProperties properties) {
        final Context appContext = context.getApplicationContext();

        final ImageLoaderConfiguration.Builder builder = new ImageLoaderConfiguration.Builder(appContext);
        if (properties.isDebugBuild()) {
            builder.writeDebugLogs();
        }

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

    public void displayInAdapterView(String urn, ImageSize imageSize, ImageView imageView) {
        mImageLoader.displayImage(
                mImageEndpointBuilder.imageUrl(urn, imageSize), new ImageViewAware(imageView, false),
                ImageOptionsFactory.adapterView(mPlaceholderGenerator.generate(urn)));
    }

    public void displayWithPlaceholder(String urn, ImageSize imageSize, ImageView imageView, int defaultResId) {
        mImageLoader.displayImage(
                mImageEndpointBuilder.imageUrl(urn, imageSize),
                new ImageViewAware(imageView, false),
                ImageOptionsFactory.placeholder(defaultResId));
    }

    public void displayWithPlaceholder(String urn, ImageSize imageSize, ImageView imageView) {
        mImageLoader.displayImage(
                mImageEndpointBuilder.imageUrl(urn, imageSize),
                new ImageViewAware(imageView, false),
                ImageOptionsFactory.placeholder(mPlaceholderGenerator.generate(urn)));
    }

    public void displayInPlayerView(String urn, ImageSize imageSize, ImageView imageView, View parentView,
                                    boolean priority, ImageListener imageListener) {
        mImageLoader.displayImage(
                mImageEndpointBuilder.imageUrl(urn, imageSize),
                new ImageViewAware(imageView, false),
                ImageOptionsFactory.player(parentView, priority), new ImageListenerUILAdapter(imageListener));
    }

    public void displayInFullDialogView(String urn, ImageSize imageSize, ImageView imageView, ImageListener imageListener) {
        mImageLoader.displayImage(mImageEndpointBuilder.imageUrl(urn, imageSize), new ImageViewAware(imageView, false),
                ImageOptionsFactory.fullImageDialog(), new ImageListenerUILAdapter(imageListener));
    }

    public void load(String urn, ImageSize imageSize, ImageListener imageListener) {
        mImageLoader.loadImage(mImageEndpointBuilder.imageUrl(urn, imageSize), new ImageListenerUILAdapter(imageListener));
    }

    @Deprecated // use the variants that take URNs instead
    public void load(String imageUrl, ImageListener imageListener) {
        mImageLoader.loadImage(adjustUrl(imageUrl), new ImageListenerUILAdapter(imageListener));
    }

    @Deprecated // use the variants that take URNs instead
    public void display(String imageUrl, ImageView imageView) {
        mImageLoader.displayImage(adjustUrl(imageUrl), new ImageViewAware(imageView, false));
    }

    @Deprecated // use the variants that take URNs instead
    public void prefetch(String imageUrl) {
        mImageLoader.loadImage(adjustUrl(imageUrl), ImageOptionsFactory.prefetch(), null);
    }

    public void resume() {
        mImageLoader.resume();
    }

    public void cancel(ImageView imageView) {
        mImageLoader.cancelDisplayTask(imageView);
    }

    public AbsListView.OnScrollListener createScrollPauseListener(boolean pauseOnScroll, boolean pauseOnFling,
                                                                  AbsListView.OnScrollListener customListener) {
        return new PauseOnScrollListener(mImageLoader, pauseOnScroll, pauseOnFling, customListener);
    }

    public AbsListView.OnScrollListener createScrollPauseListener(boolean pauseOnScroll, boolean pauseOnFling) {
        return new PauseOnScrollListener(mImageLoader, pauseOnScroll, pauseOnFling);
    }

    /**
     * Adjust urls to use insecure protocol. Will result in more cache hits
     */
    private String adjustUrl(String url) {
        if (ScTextUtils.isNotBlank(url)) {
            Matcher matcher = PATTERN.matcher(url);
            if (matcher.find() && matcher.groupCount() == 1) {
                return String.format(URL_BASE, matcher.group(1));
            }
        }
        return url; // fallback to original url
    }
}
