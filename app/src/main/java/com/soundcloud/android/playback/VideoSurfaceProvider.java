package com.soundcloud.android.playback;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.view.Surface;
import android.view.TextureView;

import com.soundcloud.android.model.Urn;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class VideoSurfaceProvider {

    // Change size to PlayerPagerPresenter.PAGE_VIEW_POOL_SIZE to support more than multiple videos in PQ at once.
    final private Map<Urn, VideoTextureContainer> videoTextureContainers = new HashMap<>(1);
    final private VideoTextureContainerFactory containerFactory;

    private Listener listener;

    @Inject
    VideoSurfaceProvider() {
        this.containerFactory = new VideoTextureContainerFactory();
    }

    @VisibleForTesting
    VideoSurfaceProvider(VideoTextureContainerFactory containerFactory) {
        this.containerFactory = containerFactory;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setTextureView(Urn urn, TextureView videoTexture) {
        if (videoTextureContainers.containsKey(urn)) {
            videoTextureContainers.get(urn).attachSurfaceTexture(videoTexture);
        } else {
            // In case this texture view was recycled, then remove the old container referencing it
            removeContainers(videoTexture);
            videoTextureContainers.put(urn, containerFactory.build(urn, videoTexture, listener));
        }
    }

    private void removeContainers(TextureView videoTexture) {
        for (VideoTextureContainer container: videoTextureContainers.values()) {
            if (container.containsTextureView(videoTexture)) {
                container.release();
                videoTextureContainers.remove(container.getUrn());
            }
        }
    }

    // Only clear the TextureViews since we maintain TextureSurfaces on configuration change
    public void onConfigurationChange() {
        for (VideoTextureContainer container: videoTextureContainers.values()) {
            container.releaseTextureView();
        }
    }

    public void onDestroy() {
        for (VideoTextureContainer container: videoTextureContainers.values()) {
            container.release();
        }
        videoTextureContainers.clear();
    }

    @Nullable
    public Surface getSurface(Urn urn) {
       return videoTextureContainers.containsKey(urn) ? videoTextureContainers.get(urn).getSurface() : null;
    }

    public interface Listener {
        void attemptToSetSurface(Urn urn);
    }

    static class VideoTextureContainerFactory {
        VideoTextureContainerFactory() {}

        VideoTextureContainer build(Urn urn, TextureView textureView, Listener listener) {
            return new VideoTextureContainer(urn, textureView, listener);
        }
    }
}
