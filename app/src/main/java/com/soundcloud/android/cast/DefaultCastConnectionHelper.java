package com.soundcloud.android.cast;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.callbacks.VideoCastConsumerImpl;

import android.content.Context;
import android.support.v4.util.ArrayMap;
import android.view.Menu;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class DefaultCastConnectionHelper implements CastConnectionHelper {

    private static final int EXPECTED_CAST_LISTENER_CAPACITY = 3;
    private final Context context;
    private final VideoCastManager videoCastManager;
    private final Map<CastConnectionListener, CastConsumer> listenerMap;

    @Inject
    public DefaultCastConnectionHelper(Context context, VideoCastManager videoCastManager) {
        this.context = context;
        this.videoCastManager = videoCastManager;
        listenerMap = new ArrayMap<>(EXPECTED_CAST_LISTENER_CAPACITY);
    }

    @Override
    public void addMediaRouterButton(Menu menu, int itemId){
        videoCastManager.addMediaRouterButton(menu, itemId);
    }

    @Override
    public void startDeviceDiscovery(){
        videoCastManager.startCastDiscovery();
    }

    @Override
    public void stopDeviceDiscovery(){
        videoCastManager.stopCastDiscovery();
    }

    @Override
    public void reconnectSessionIfPossible(){
        videoCastManager.reconnectSessionIfPossible(context, false);
    }

    @Override
    public void addConnectionListener(final CastConnectionListener listener) {
        final CastConsumer baseCastConsumer = new CastConsumer(listener);
        listenerMap.put(listener, baseCastConsumer);
        videoCastManager.addVideoCastConsumer(baseCastConsumer);
    }

    @Override
    public void removeConnectionListener(final CastConnectionListener listener) {
        if (listenerMap.containsKey(listener)){
            videoCastManager.removeVideoCastConsumer(listenerMap.get(listener));
            listenerMap.remove(listener);
        }
    }

    static class CastConsumer extends VideoCastConsumerImpl {
        private CastConnectionListener listener;

        CastConsumer(CastConnectionListener listener) {
            this.listener = listener;
        }

        @Override
        public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId, boolean wasLaunched) {
            listener.onConnectedToReceiverApp();
        }
    }
}
