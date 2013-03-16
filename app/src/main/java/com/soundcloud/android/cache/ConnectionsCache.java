package com.soundcloud.android.cache;

import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.service.sync.SyncStateManager;
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
    @Nullable private Set<Connection> mConnections;
    private DetachableResultReceiver mDetachableReceiver = new DetachableResultReceiver(new Handler());

    private static ConnectionsCache sInstance;
    private WeakHashMap<Listener, Listener> listeners = new WeakHashMap<Listener, Listener>();
    AsyncQueryHandler asyncQueryHandler;
    private Context mContext;
    private LocalCollection mLocalCollection;

    public ConnectionsCache(final Context c) {
        mContext = c;
        c.getContentResolver().registerContentObserver(Content.ME_CONNECTIONS.uri,true,new ChangeObserver());
    }

    public synchronized static ConnectionsCache get(Context c) {
        if (sInstance == null) {
            sInstance = new ConnectionsCache(c);
        }
        return sInstance;
    }

    public synchronized static void set(ConnectionsCache status) {
        sInstance = status;
    }

    private void onContentChanged() {
        doQuery(null);
    }

    public boolean isConnected(Connection.Service service) {
        if (mConnections != null) {
            for (Connection connection : mConnections){
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
        mLocalCollection = new SyncStateManager(mContext.getContentResolver()).fromContent(Content.ME_CONNECTIONS);
        asyncQueryHandler = new ConnectionsQueryHandler(mContext, this);
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
            if (mLocalCollection.hasSyncedBefore()) {
                if (cursor != null) {

                    Set<Connection> newConnections = new HashSet<Connection>();
                    if (cursor.moveToFirst()) {
                        do {
                            newConnections.add(new Connection(cursor));
                        } while (cursor.moveToNext());
                    }
                    cursor.close();


                    final boolean changed = newConnections.equals(mConnections);

                    if (mConnections == null) {
                        mConnections = Collections.synchronizedSet(new HashSet<Connection>());
                    } else {
                        mConnections.clear();
                    }
                    mConnections.addAll(newConnections);

                    if (connectionsCacheRef != null && connectionsCacheRef.get() != null) {
                        connectionsCacheRef.get().onConnectionsChanged(changed);
                    }
                }
            } else {
                // load connections
                mContext.startService(new Intent(mContext, ApiSyncService.class)
                        .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, getReceiver())
                        .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                        .setData(Content.ME_CONNECTIONS.uri));
            }
        }
    }

    public void onConnectionsChanged(boolean changed) {
        for (Listener l : listeners.keySet()) {
            l.onConnectionsRefreshed(mConnections,changed);
        }
    }

    public Set<Connection> getConnections(){
        return mConnections;
    }

    protected DetachableResultReceiver getReceiver() {
        mDetachableReceiver.setReceiver(this);
        return mDetachableReceiver;
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
