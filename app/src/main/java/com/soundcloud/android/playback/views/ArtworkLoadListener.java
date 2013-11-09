package com.soundcloud.android.playback.views;

import com.google.common.annotations.VisibleForTesting;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.ImageLoaderUtils;

import android.graphics.Bitmap;
import android.view.View;

import java.lang.ref.WeakReference;

@VisibleForTesting
class ArtworkLoadListener extends SimpleImageLoadingListener {

    private final ImageLoaderUtils mImageLoaderUtils;
    private WeakReference<ArtworkTrackView> mTrackViewRef;
    private Track mTrack;

    ArtworkLoadListener(ArtworkTrackView trackView, Track track) {
        this(trackView, track, new ImageLoaderUtils(trackView.getContext()));
    }

    ArtworkLoadListener(ArtworkTrackView trackView, Track track, ImageLoaderUtils imageLoaderUtils) {
        mTrackViewRef = new WeakReference<ArtworkTrackView>(trackView);
        mTrack = track;
        mImageLoaderUtils = imageLoaderUtils;
    }

    @Override
    public void onLoadingStarted(String imageUri, View view) {
        ArtworkTrackView trackView = mTrackViewRef.get();
        if (trackView != null) {
            Bitmap memoryBitmap = mImageLoaderUtils.getCachedTrackListIcon(mTrack);
            if (memoryBitmap != null) {
                trackView.setTemporaryArtwork(memoryBitmap);
            }
        }
    }

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
        ArtworkTrackView trackView = mTrackViewRef.get();
        if (trackView != null) {
            trackView.onArtworkSet(true);
        }
    }
}
