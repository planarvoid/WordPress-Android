package com.soundcloud.android.image;

import com.google.common.annotations.VisibleForTesting;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.playback.views.ArtworkTrackView;

import android.graphics.Bitmap;
import android.view.View;

import java.lang.ref.WeakReference;

@VisibleForTesting
public class PlayerArtworkLoadListener extends SimpleImageLoadingListener implements ImageListener {

    private final ImageLoaderUtils imageLoaderUtils;
    private final WeakReference<ArtworkTrackView> trackViewRef;
    private final PublicApiTrack track;

    public PlayerArtworkLoadListener(ArtworkTrackView trackView, PublicApiTrack track) {
        this(trackView, track, new ImageLoaderUtils(trackView.getContext()));
    }

    PlayerArtworkLoadListener(ArtworkTrackView trackView, PublicApiTrack track, ImageLoaderUtils imageLoaderUtils) {
        trackViewRef = new WeakReference<ArtworkTrackView>(trackView);
        this.track = track;
        this.imageLoaderUtils = imageLoaderUtils;
    }

    @Override
    public void onLoadingStarted(String imageUri, View view) {
        ArtworkTrackView trackView = trackViewRef.get();
        if (trackView != null) {
            Bitmap memoryBitmap = imageLoaderUtils.getCachedTrackListIcon(track);
            if (memoryBitmap != null) {
                trackView.setTemporaryArtwork(memoryBitmap);
            }
        }
    }

    @Override
    public void onLoadingFailed(String s, View view, String failedReason) {}

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
    }
}
