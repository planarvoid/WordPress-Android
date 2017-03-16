package com.soundcloud.android.playback;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.TextureView;

import com.soundcloud.android.playback.VideoSurfaceProvider.Origin;
import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;

// Inspired by: github.com/google/grafika/blob/master/src/com/android/grafika/DoubleDecodeActivity.java
class VideoTextureContainer implements TextureView.SurfaceTextureListener {

    final private String uuid;
    final private Origin origin;

    @Nullable private Surface surface;
    @Nullable private SurfaceTexture surfaceTexture;
    @Nullable private TextureView currentTextureView;

    private Optional<VideoSurfaceProvider.Listener> listener = Optional.absent();

    VideoTextureContainer(String videoUuid,
                          Origin origin,
                          TextureView textureView,
                          Optional<VideoSurfaceProvider.Listener> listener) {
        this.uuid = videoUuid;
        this.origin = origin;
        this.listener = listener;
        setTextureView(textureView);
    }

    void setListener(VideoSurfaceProvider.Listener listener) {
        this.listener = Optional.of(listener);
    }

    void reattachSurfaceTexture(TextureView textureView) {
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

    String getUuid() {
        return uuid;
    }

    Origin getOrigin() {
        return origin;
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
            if (listener.isPresent()) {
                listener.get().attemptToSetSurface(uuid);
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

        VideoTextureContainer build(String uuid,
                                    Origin origin,
                                    TextureView textureView,
                                    Optional<VideoSurfaceProvider.Listener> listener) {
            return new VideoTextureContainer(uuid, origin, textureView, listener);
        }
    }
}
