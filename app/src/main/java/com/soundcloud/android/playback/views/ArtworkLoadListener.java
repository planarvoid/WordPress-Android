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
    private WeakReference<PlayerArtworkTrackView> mTrackViewRef;
    private Track mTrack;

    ArtworkLoadListener(PlayerArtworkTrackView trackView, Track track) {
        this(trackView, track, new ImageLoaderUtils(trackView.getContext()));
    }

    ArtworkLoadListener(PlayerArtworkTrackView trackView, Track track, ImageLoaderUtils imageLoaderUtils) {
        mTrackViewRef = new WeakReference<PlayerArtworkTrackView>(trackView);
        mTrack = track;
        mImageLoaderUtils = imageLoaderUtils;
    }

    @Override
    public void onLoadingStarted(String imageUri, View view) {
        PlayerArtworkTrackView trackView = mTrackViewRef.get();
        if (trackView != null) {
            Bitmap memoryBitmap = mImageLoaderUtils.getCachedTrackListIcon(mTrack);
            if (memoryBitmap != null) {
                trackView.setTemporaryArtwork(memoryBitmap);
            }
        }
    }

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
        PlayerArtworkTrackView trackView = mTrackViewRef.get();
        if (trackView != null) {
            trackView.onArtworkSet(true);
        }
    }
}
