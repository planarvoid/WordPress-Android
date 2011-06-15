package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Connection;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.lang.ref.WeakReference;
import java.util.List;

public class LoadConnectionsTask extends LoadJsonTask<Connection> {
    public static final Request REQUEST = Request.to(Endpoints.MY_CONNECTIONS);
    private WeakReference<ConnectionsListener> mListenerRef;

    public LoadConnectionsTask(AndroidCloudAPI api) {
        super(api);
    }

    public void setListener(ConnectionsListener listener) {
        if (listener == null) {
            mListenerRef = null;
        } else {
            mListenerRef = new WeakReference<ConnectionsListener>(listener);
        }
    }

    @Override
    protected List<Connection> doInBackground(Request... path) {
        return list(REQUEST, Connection.class);
    }

    @Override
    protected void onPostExecute(List<Connection> connections) {
        ConnectionsListener listener = mListenerRef == null ? null : mListenerRef.get();
        if (listener != null) {
            listener.onConnections(connections);
        }
    }

    public interface ConnectionsListener {
        void onConnections(List<Connection> connections);
    }
}
