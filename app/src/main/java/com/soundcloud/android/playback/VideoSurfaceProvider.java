package com.soundcloud.android.playback;

import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.TextureView;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.java.optional.Optional;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class VideoSurfaceProvider {

    // Change size to PlayerPagerPresenter.PAGE_VIEW_POOL_SIZE to support multiple videos in PQ at once.
    final private Map<Urn, VideoTextureContainer> videoTextureContainers = new HashMap<>(1);

    final private VideoTextureContainer.Factory containerFactory;
    final private ApplicationProperties applicationProperties;

    private Listener listener;

    @Inject
    VideoSurfaceProvider(ApplicationProperties applicationProperties,
                         VideoTextureContainer.Factory containerFactory) {
        this.applicationProperties = applicationProperties;
        this.containerFactory = containerFactory;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setTextureView(Urn urn, TextureView videoTexture) {
        if (videoTextureContainers.containsKey(urn) && applicationProperties.canReattachSurfaceTexture()) {
            videoTextureContainers.get(urn).reattachSurfaceTexture(videoTexture);
        } else {
            // If this texture view was used before, release & remove the old container referencing it
            removeContainers(videoTexture);
            videoTextureContainers.put(urn, containerFactory.build(urn, videoTexture, listener));
        }

        listener.onTextureViewUpdate(urn, videoTexture);
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

    public Optional<TextureView> getTextureView(Urn urn) {
        final TextureView textureView = videoTextureContainers.containsKey(urn) ? videoTextureContainers.get(urn).getTextureView() : null;
        return Optional.fromNullable(textureView);
    }

    public interface Listener {
        void attemptToSetSurface(Urn urn);
        void onTextureViewUpdate(Urn urn, TextureView textureView);
    }
}
