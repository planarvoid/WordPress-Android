package com.soundcloud.android.playback;

import android.graphics.SurfaceTexture;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.TextureView;

import com.soundcloud.android.model.Urn;

// Inspired by: github.com/google/grafika/blob/master/src/com/android/grafika/DoubleDecodeActivity.java
// But better. Probably.
public class VideoTextureContainer implements TextureView.SurfaceTextureListener {

    final private Listener listener;
    final private Urn videoUrn;

    private Surface surface;
    private SurfaceTexture surfaceTexture;
    private TextureView currentTextureView;

    public VideoTextureContainer(Urn videoUrn, TextureView textureView, Listener listener) {
        this.listener = listener;
        this.videoUrn = videoUrn;
        reattachSurfaceTextureIfNeeded(textureView);
    }

    public void reattachSurfaceTextureIfNeeded(TextureView textureView) {
        if (isNewTextureView(textureView)) {
            currentTextureView = textureView;
            currentTextureView.setSurfaceTextureListener(this);
            if (surfaceTexture != null) {
                textureView.setSurfaceTexture(surfaceTexture);
                currentTextureView = textureView;
            }
        }
    }

    private boolean isNewTextureView(TextureView textureView) {
        return (currentTextureView == null || !currentTextureView.equals(textureView));
    }

    @Nullable
    public Surface getSurface() {
        return surface;
    }

    public Urn getVideoUrn() {
        return videoUrn;
    }

    public boolean containsTextureView(TextureView textureView) {
        return textureView.equals(currentTextureView);
    }

    public void onTextureViewDestroy() {
        currentTextureView = null;
    }

    public void release() {
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
            listener.attemptToSetSurface(videoUrn);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        // Only let the TextureView release it if we do not need it to render video to it anymore
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

    public interface Listener {
        void attemptToSetSurface(Urn urn);
    }
}
