package com.soundcloud.android.playback;

import android.graphics.SurfaceTexture;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.TextureView;

import com.soundcloud.android.model.Urn;

// Inspired by: github.com/google/grafika/blob/master/src/com/android/grafika/DoubleDecodeActivity.java
public class VideoTextureContainer implements TextureView.SurfaceTextureListener {

    final private Urn urn;
    final private VideoSurfaceProvider.Listener listener;

    private Surface surface;
    private SurfaceTexture surfaceTexture;
    private TextureView currentTextureView;

    public VideoTextureContainer(Urn videoUrn,
                                 TextureView textureView,
                                 VideoSurfaceProvider.Listener listener) {
        this.urn = videoUrn;
        this.listener = listener;
        attachSurfaceTexture(textureView);
    }

    public void attachSurfaceTexture(TextureView textureView) {
        currentTextureView = textureView;
        currentTextureView.setSurfaceTextureListener(this);
        if (surfaceTexture != null && !surfaceTextureAlreadyAttached(textureView)) {
            textureView.setSurfaceTexture(surfaceTexture);
        }
    }

    private boolean surfaceTextureAlreadyAttached(TextureView textureView) {
        final SurfaceTexture currentSurfaceTexture = textureView.getSurfaceTexture();
        return currentSurfaceTexture != null && currentSurfaceTexture.equals(surfaceTexture);
    }

    boolean containsTextureView(TextureView textureView) {
        return textureView.equals(currentTextureView);
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
}
