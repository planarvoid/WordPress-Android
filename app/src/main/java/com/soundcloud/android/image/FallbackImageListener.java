package com.soundcloud.android.image;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.images.ImageUtils;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.util.Set;

class FallbackImageListener implements ImageLoadingListener {
    private static final String TAG = ImageLoader.TAG;

    private final ImageListenerUILAdapter listenerAdapter;
    private final Set<String> notFoundUris;

    FallbackImageListener(Set<String> notFoundUris) {
        this(null, notFoundUris);
    }

    FallbackImageListener(@Nullable ImageListener imageListener, Set<String> notFoundUris) {
        this.notFoundUris = notFoundUris;
        listenerAdapter = imageListener != null ? new ImageListenerUILAdapter(imageListener) : null;
    }

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
        if (loadedImage == null) {
            animatePlaceholder(view);
        }
        if (listenerAdapter != null) {
            listenerAdapter.onLoadingComplete(imageUri, view, loadedImage);
        }
    }

    @Override
    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
        if (failReason.getCause() instanceof FileNotFoundException) {
            notFoundUris.add(imageUri);
        } else {
            Log.e(TAG, "Failed loading " + imageUri + "; reason: " + failReason.getType());
        }
        animatePlaceholder(view);
        if (listenerAdapter != null) {
            listenerAdapter.onLoadingFailed(imageUri, view, failReason);
        }
    }

    @Override
    public void onLoadingStarted(String imageUri, View view) {
        if (listenerAdapter != null) {
            listenerAdapter.onLoadingStarted(imageUri, view);
        }
    }

    @Override
    public void onLoadingCancelled(String imageUri, View view) {
        if (listenerAdapter != null) {
            listenerAdapter.onLoadingCancelled(imageUri, view);
        }
    }

    private void animatePlaceholder(View view) {
        if (view instanceof ImageView && ((ImageView) view).getDrawable() instanceof OneShotTransitionDrawable) {
            final OneShotTransitionDrawable transitionDrawable = (OneShotTransitionDrawable) ((ImageView) view).getDrawable();
            transitionDrawable.startTransition(ImageUtils.DEFAULT_TRANSITION_DURATION);
        }
    }
}
