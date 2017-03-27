package com.soundcloud.android.playback;

import com.soundcloud.android.utils.Log;
import com.soundcloud.java.functions.Consumer;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.TextureView;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Singleton
public class VideoSurfaceProvider implements VideoTextureContainer.Listener {

    final private static String TAG = "VideoSurfaceProvider";

    public enum Origin {
        FULLSCREEN,
        STREAM,
        PLAYER
    }

    final private static int MAX_VIDEO_CONTAINERS = 5;

    final private Map<String, VideoTextureContainer> textureContainers = new HashMap<>(MAX_VIDEO_CONTAINERS);

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
        if (textureContainers.containsKey(uuid) && textureContainers.get(uuid).getOrigin() == origin) {
            textureContainers.get(uuid).reattachSurfaceTexture(videoTexture);
            log(uuid, origin, "Reattached to existing container");
        } else {
            // If this texture view was used before (e.g view is recycled) release & remove any old containers referencing it
            removeContainers(container -> container.containsTextureView(videoTexture) || container.getUuid().equals(uuid));
            textureContainers.put(uuid, containerFactory.build(uuid, origin, videoTexture, this));
            log(uuid, origin, "Created container");
        }

        updateListeners(listener -> listener.onTextureViewUpdate(uuid, videoTexture));
    }

    private void removeContainers(Predicate<VideoTextureContainer> predicate) {
        Iterator<Map.Entry<String, VideoTextureContainer>> entryIterator = textureContainers.entrySet().iterator();
        while (entryIterator.hasNext()) {
            final VideoTextureContainer container = entryIterator.next().getValue();
            if (predicate.apply(container)) {
                log(container.getUuid(), container.getOrigin(), "Removing container");
                container.release();
                entryIterator.remove();
            }
        }
    }

    // Only clear the TextureViews since we maintain TextureSurfaces on configuration change
    public void onConfigurationChange(Origin origin) {
        for (VideoTextureContainer container : textureContainers.values()) {
            if (container.getOrigin() == origin) {
                container.releaseTextureView();
                log(container.getUuid(), origin, "Unbinded TextureView from container");
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

    private void log(String uuid, Origin origin, String message) {
        Log.d(TAG, String.format("[UUID: %s, Origin: %s] %s", uuid, origin, message));
    }

    public void onDestroy(Origin origin) {
        removeContainers(input -> input.getOrigin() == origin);
    }

    @Nullable
    public Surface getSurface(String uuid) {
        return textureContainers.containsKey(uuid) ? textureContainers.get(uuid).getSurface() : null;
    }

    public Optional<TextureView> getTextureView(String uuid) {
        final TextureView textureView = textureContainers.containsKey(uuid) ? textureContainers.get(uuid).getTextureView() : null;
        return Optional.fromNullable(textureView);
    }

    public interface Listener {
        void attemptToSetSurface(String uuid);

        void onTextureViewUpdate(String uuid, TextureView textureView);
    }
}
