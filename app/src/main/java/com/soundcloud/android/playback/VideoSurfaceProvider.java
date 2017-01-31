package com.soundcloud.android.playback;

import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.TextureView;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class VideoSurfaceProvider {

    public enum Origin {
        STREAM,
        PLAYER
    }

    final private static int MAX_VIDEO_CONTAINERS = 5;

    final private Map<Urn, VideoTextureContainer> videoTextureContainers = new HashMap<>(MAX_VIDEO_CONTAINERS);

    final private VideoTextureContainer.Factory containerFactory;
    final private ApplicationProperties applicationProperties;

    private Optional<Listener> listener = Optional.absent();

    @Inject
    VideoSurfaceProvider(ApplicationProperties applicationProperties,
                         VideoTextureContainer.Factory containerFactory) {
        this.applicationProperties = applicationProperties;
        this.containerFactory = containerFactory;
    }

    public void setListener(Listener listener) {
        this.listener = Optional.of(listener);
        for (VideoTextureContainer container: videoTextureContainers.values()) {
           container.setListener(listener);
        }
    }

    public void setTextureView(Urn urn, Origin origin, TextureView videoTexture) {
        if (videoTextureContainers.containsKey(urn) && applicationProperties.canReattachSurfaceTexture()) {
            videoTextureContainers.get(urn).reattachSurfaceTexture(videoTexture);
        } else {
            // If this texture view was used before (e.g view is recycled),
            // release & remove any old containers referencing it
            removeContainers(container -> container.containsTextureView(videoTexture));
            videoTextureContainers.put(urn, containerFactory.build(urn, origin, videoTexture, listener));
        }

        if (listener.isPresent()) {
            listener.get().onTextureViewUpdate(urn, videoTexture);
        }
    }

    private void removeContainers(Predicate<VideoTextureContainer> predicate) {
        Iterator<Map.Entry<Urn, VideoTextureContainer>> entryIterator = videoTextureContainers.entrySet().iterator();
        while (entryIterator.hasNext()) {
            final VideoTextureContainer container = entryIterator.next().getValue();
            if (predicate.apply(container)) {
                container.release();
                entryIterator.remove();
            }
        }
    }

    // Only clear the TextureViews since we maintain TextureSurfaces on configuration change
    public void onConfigurationChange(Origin origin) {
        for (VideoTextureContainer container: videoTextureContainers.values()) {
            if (container.getOrigin() == origin) {
                container.releaseTextureView();
            }
        }
    }

    public void onDestroy(Origin origin) {
        removeContainers(input -> input.getOrigin() == origin);
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
