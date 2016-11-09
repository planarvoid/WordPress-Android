package com.soundcloud.android.playback;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.TextureView;

import com.soundcloud.android.model.Urn;

import javax.inject.Inject;

// Inspired by: github.com/google/grafika/blob/master/src/com/android/grafika/DoubleDecodeActivity.java
public class VideoTextureContainer implements TextureView.SurfaceTextureListener {

    final private Urn urn;
    final private VideoSurfaceProvider.Listener listener;

    @Nullable private Surface surface;
    @Nullable private SurfaceTexture surfaceTexture;
    @Nullable private TextureView currentTextureView;

    public VideoTextureContainer(Urn videoUrn,
                                 TextureView textureView,
                                 VideoSurfaceProvider.Listener listener) {
        this.urn = videoUrn;
        this.listener = listener;
        setTextureView(textureView);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void reattachSurfaceTexture(TextureView textureView) {
        setTextureView(textureView);
        if (surfaceTexture != null && !surfaceTextureAlreadyAttached(textureView)) {
            textureView.setSurfaceTexture(surfaceTexture);
        }
    }

    private void setTextureView(TextureView textureView) {
        currentTextureView = textureView;
        currentTextureView.setSurfaceTextureListener(this);
    }

    private boolean surfaceTextureAlreadyAttached(TextureView textureView) {
        final SurfaceTexture currentSurfaceTexture = textureView.getSurfaceTexture();
        return currentSurfaceTexture != null && currentSurfaceTexture.equals(surfaceTexture);
    }

    boolean containsTextureView(TextureView textureView) {
        return textureView.equals(currentTextureView);
    }

    @Nullable
    TextureView getTextureView() {
        return currentTextureView;
    }

    @Nullable
    Surface getSurface() {
        return surface;
    }

    Urn getUrn() {
        return urn;
    }

    void releaseTextureView() {
        currentTextureView = null;
    }

    void release() {
        if (surface != null) {
            surface.release();
        }
        surface = null;
        currentTextureView = null;
        surfaceTexture = null;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (surfaceTexture == null) {
            surfaceTexture = surface;
            this.surface = new Surface(surface);
            if (listener != null) {
                listener.attemptToSetSurface(urn);
            }
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        // Only let the TextureView release SurfaceTexture if we're not rendering video to it anymore
        return surfaceTexture == null;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // no-op
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // no-op
    }

    static class Factory {
        @Inject
        Factory() {}

        VideoTextureContainer build(Urn urn, TextureView textureView, VideoSurfaceProvider.Listener listener) {
            return new VideoTextureContainer(urn, textureView, listener);
        }
    }
}
