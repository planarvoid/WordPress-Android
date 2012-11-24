package com.soundcloud.android.cache;

import com.soundcloud.android.model.Connection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

public class ConnectionsCache {
    private final Set<Connection> connections = Collections.synchronizedSet(new HashSet<Connection>());
    private static ConnectionsCache sInstance;
    private WeakHashMap<Listener, Listener> listeners = new WeakHashMap<Listener, Listener>();
    AsyncQueryHandler asyncQueryHandler;
    private ContentObserver c;
    private Context mContext;

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
        for (Connection connection : connections){
            if (connection.service() == service) return true;
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

    private void doQuery(final Listener listener){
        if (listener != null) addListener(listener);
        asyncQueryHandler = new ConnectionsQueryHandler(mContext, this);
        asyncQueryHandler.startQuery(0, null, Content.ME_CONNECTIONS.uri, new String[]{DBHelper.CollectionItems.ITEM_ID}, null, null, null);
    }

    public void addListener(Listener l) {
        listeners.put(l, l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }


    public interface Listener {
        void onChange(boolean success, ConnectionsCache status);
    }


    private class ConnectionsQueryHandler extends AsyncQueryHandler {
        // Use weak reference to avoid memoey leak
        private WeakReference<ConnectionsCache> connectionsCacheRef;

        public ConnectionsQueryHandler(Context context, ConnectionsCache connectionCache) {
            super(context.getContentResolver());
            this.connectionsCacheRef = new WeakReference<ConnectionsCache>((ConnectionsCache) connectionCache);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (cursor != null) {
                connections.clear();
                if (cursor.moveToFirst()) {
                    do {
                        connections.add(new Connection(cursor));
                    } while (cursor.moveToNext());
                }
                cursor.close();

                if (connectionsCacheRef != null && connectionsCacheRef.get() != null){
                    connectionsCacheRef.get().onConnectionsChanged();
                }
            }
        }
    }

    public void onConnectionsChanged() {
        for (Listener l : listeners.keySet()) {
            l.onChange(true, ConnectionsCache.this);
        }
    }

    public Set<Connection> getConnections(){
        return connections;
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
