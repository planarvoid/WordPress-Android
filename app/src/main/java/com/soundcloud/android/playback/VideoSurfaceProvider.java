package com.soundcloud.android.playback;

import com.soundcloud.android.utils.Log;
import com.soundcloud.java.functions.Consumer;
import com.soundcloud.java.functions.Predicate;

import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

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
        PLAYER,
        PRESTITIAL
    }

    private static final int MAX_VIDEO_CONTAINERS = 5;

    private final Map<String, WeakReference<VideoTextureContainer>> textureContainers = new HashMap<>(MAX_VIDEO_CONTAINERS);

    private final VideoTextureContainer.Factory containerFactory;

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

    public void setTextureView(String uuid, Origin origin, TextureView textureView, View viewabilityView) {
        if (textureContainers.containsKey(uuid) && textureContainers.get(uuid).get() != null && textureContainers.get(uuid).get().getOrigin() == origin) {
            textureContainers.get(uuid).get().reattachSurfaceTexture(textureView, viewabilityView);
            log(uuid, origin, "Reattached to existing container");
        } else {
            // If this texture view was used before (e.g view is recycled) release & remove any old containers referencing it
            removeContainers(container -> container.containsTextureView(textureView) || container.getUuid().equals(uuid));
            textureContainers.put(uuid, new WeakReference<>(containerFactory.build(uuid, origin, textureView, viewabilityView, this)));
            log(uuid, origin, "Created container");
        }

        updateListeners(listener -> listener.onViewabilityViewUpdate(uuid, viewabilityView));
    }

    private void removeContainers(Predicate<VideoTextureContainer> predicate) {
        Iterator<Map.Entry<String, WeakReference<VideoTextureContainer>>> entryIterator = textureContainers.entrySet().iterator();
        while (entryIterator.hasNext()) {
            final VideoTextureContainer container = entryIterator.next().getValue().get();
            if (container != null && predicate.apply(container)) {
                log(container.getUuid(), container.getOrigin(), "Removing container");
                container.release();
                entryIterator.remove();
            }
        }
    }

    // Only clear the TextureViews since we maintain TextureSurfaces on configuration change
    public void onConfigurationChange(Origin origin) {
        for (WeakReference<VideoTextureContainer> containerRef : textureContainers.values()) {
            VideoTextureContainer container = containerRef.get();
            if (container != null && container.getOrigin() == origin) {
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
        removeContainers(input -> input != null && input.getOrigin() == origin);
    }

    @Nullable
    public Surface getSurface(String uuid) {
        if (textureContainers.containsKey(uuid)) {
            VideoTextureContainer videoTextureContainer = textureContainers.get(uuid).get();
            if (videoTextureContainer != null) {
                return videoTextureContainer.getSurface();
            }
        }
        return null;
    }

    public View getViewabilityView(String uuid) {
        if (textureContainers.containsKey(uuid)) {
            VideoTextureContainer videoTextureContainer = textureContainers.get(uuid).get();
            if (videoTextureContainer != null) {
                return videoTextureContainer.getViewabilityView();
            }
        }
        return null;
    }

    public interface Listener {
        void attemptToSetSurface(String uuid);
        void onViewabilityViewUpdate(String uuid, View viewabilityView);
    }
}
