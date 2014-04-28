package com.soundcloud.android.cache;

import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.utils.DetachableResultReceiver;
import org.jetbrains.annotations.Nullable;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

public class ConnectionsCache implements DetachableResultReceiver.Receiver {
    @Nullable private Set<Connection> connections;
    private DetachableResultReceiver detachableReceiver = new DetachableResultReceiver(new Handler());

    private static ConnectionsCache instance;
    private WeakHashMap<Listener, Listener> listeners = new WeakHashMap<Listener, Listener>();
    private AsyncQueryHandler asyncQueryHandler;
    private final Context context;
    private LocalCollection localCollection;

    public ConnectionsCache(final Context context) {
        this.context = context;
        context.getContentResolver().registerContentObserver(Content.ME_CONNECTIONS.uri,true,new ChangeObserver());
    }

    public synchronized static ConnectionsCache get(Context context) {
        if (instance == null) {
            instance = new ConnectionsCache(context);
        }
        return instance;
    }

    public synchronized static void reset() {
        instance = null;
    }

    private void onContentChanged() {
        doQuery(null);
    }

    public boolean isConnected(Connection.Service service) {
        if (connections != null) {
            for (Connection connection : connections){
                if (connection.service() == service) return true;
            }
        }
        return false;
    }

    public synchronized void requestConnections(final Listener listener) {
        // add this listener with a weak reference
        listeners.put(listener, null);
        if (asyncQueryHandler == null) {
            doQuery(listener);
        }
    }

    private void doQuery(@Nullable final Listener listener){
        if (listener != null) addListener(listener);
        localCollection = new SyncStateManager(context).fromContent(Content.ME_CONNECTIONS);
        asyncQueryHandler = new ConnectionsQueryHandler(context, this);
        asyncQueryHandler.startQuery(0, null, Content.ME_CONNECTIONS.uri, null, null, null, null);
    }

    public void addListener(Listener l) {
        listeners.put(l, l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        doQuery(null);
    }


    public interface Listener {
        void onConnectionsRefreshed(Set<Connection> connections, boolean changed);
    }


    private class ConnectionsQueryHandler extends AsyncQueryHandler {
        // Use weak reference to avoid memoey leak
        private WeakReference<ConnectionsCache> connectionsCacheRef;

        public ConnectionsQueryHandler(Context context, ConnectionsCache connectionCache) {
            super(context.getContentResolver());
            this.connectionsCacheRef = new WeakReference<ConnectionsCache>(connectionCache);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (localCollection.hasSyncedBefore()) {
                if (cursor != null) {

                    Set<Connection> newConnections = new HashSet<Connection>();
                    if (cursor.moveToFirst()) {
                        do {
                            newConnections.add(new Connection(cursor));
                        } while (cursor.moveToNext());
                    }
                    cursor.close();


                    final boolean changed = newConnections.equals(connections);

                    if (connections == null) {
                        connections = Collections.synchronizedSet(new HashSet<Connection>());
                    } else {
                        connections.clear();
                    }
                    connections.addAll(newConnections);

                    if (connectionsCacheRef != null && connectionsCacheRef.get() != null) {
                        connectionsCacheRef.get().onConnectionsChanged(changed);
                    }
                }
            } else {
                // load connections
                context.startService(new Intent(context, ApiSyncService.class)
                        .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, getReceiver())
                        .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                        .setData(Content.ME_CONNECTIONS.uri));
            }
        }
    }

    public void onConnectionsChanged(boolean changed) {
        for (Listener l : listeners.keySet()) {
            l.onConnectionsRefreshed(connections,changed);
        }
    }

    public Set<Connection> getConnections(){
        return connections;
    }

    protected DetachableResultReceiver getReceiver() {
        detachableReceiver.setReceiver(this);
        return detachableReceiver;
    }

    private class ChangeObserver extends ContentObserver {
        public ChangeObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            onContentChanged();
        }
    }
}
