package com.soundcloud.android.playback;

import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.TextureView;

import com.soundcloud.java.functions.Consumer;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class VideoSurfaceProvider implements VideoTextureContainer.Listener {

    public enum Origin {
        STREAM,
        PLAYER
    }

    final private static int MAX_VIDEO_CONTAINERS = 5;

    final private Map<String, VideoTextureContainer> videoTextureContainers = new HashMap<>(MAX_VIDEO_CONTAINERS);

    final private VideoTextureContainer.Factory containerFactory;

    private List<WeakReference<Listener>> listeners = new LinkedList<>();

    @Inject
    VideoSurfaceProvider(VideoTextureContainer.Factory containerFactory) {
        this.containerFactory = containerFactory;
    }

    public void addListener(Listener listener) {
        listeners.add(new WeakReference<>(listener));
    }

    @Override
    public void attemptToSetSurface(String uuid) {
        updateListeners(listener -> listener.attemptToSetSurface(uuid));
    }

    public void setTextureView(String uuid, Origin origin, TextureView videoTexture) {
        if (videoTextureContainers.containsKey(uuid)) {
            videoTextureContainers.get(uuid).reattachSurfaceTexture(videoTexture);
        } else {
            // If this texture view was used before (e.g view is recycled),
            // release & remove any old containers referencing it
            removeContainers(container -> container.containsTextureView(videoTexture));
            videoTextureContainers.put(uuid, containerFactory.build(uuid, origin, videoTexture, this));
        }

        updateListeners(listener -> listener.onTextureViewUpdate(uuid, videoTexture));
    }

    private void removeContainers(Predicate<VideoTextureContainer> predicate) {
        Iterator<Map.Entry<String, VideoTextureContainer>> entryIterator = videoTextureContainers.entrySet().iterator();
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
        for (VideoTextureContainer container : videoTextureContainers.values()) {
            if (container.getOrigin() == origin) {
                container.releaseTextureView();
            }
        }
    }

    private void updateListeners(Consumer<Listener> consumer) {
        Iterator<WeakReference<Listener>> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            final Listener listener = iterator.next().get();
            if (listener != null) {
                consumer.accept(listener);
            } else {
                iterator.remove();
            }
        }
    }

    public void onDestroy(Origin origin) {
        removeContainers(input -> input.getOrigin() == origin);
    }

    @Nullable
    public Surface getSurface(String uuid) {
        return videoTextureContainers.containsKey(uuid) ? videoTextureContainers.get(uuid).getSurface() : null;
    }

    public Optional<TextureView> getTextureView(String uuid) {
        final TextureView textureView = videoTextureContainers.containsKey(uuid) ? videoTextureContainers.get(uuid).getTextureView() : null;
        return Optional.fromNullable(textureView);
    }

    public interface Listener {
        void attemptToSetSurface(String uuid);

        void onTextureViewUpdate(String uuid, TextureView textureView);
    }
}
