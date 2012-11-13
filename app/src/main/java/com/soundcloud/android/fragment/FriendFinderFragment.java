package com.soundcloud.android.fragment;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.ScBaseAdapter;
import com.soundcloud.android.cache.Connections;
import com.soundcloud.android.cache.ParcelCache;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.Search;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.view.EmptyCollection;
import com.soundcloud.android.view.FriendFinderEmptyCollection;
import com.soundcloud.api.Request;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

public class FriendFinderFragment extends ScListFragment implements ParcelCache.Listener<Connection> {

    private List<Connection> mConnections;

    private int mCurrentState;

    public interface States {
        int LOADING = 1;
        int NO_FB_CONNECTION = 2;
        int FB_CONNECTION = 3;
        int CONNECTION_ERROR = 4;
    }

    public static FriendFinderFragment newInstance(SoundCloudApplication app) {
        FriendFinderFragment fragment = new FriendFinderFragment(app);
        Bundle args = new Bundle();
        args.putParcelable("contentUri", Content.ME_FRIENDS.uri);
        fragment.setArguments(args);
        return fragment;
    }

    public FriendFinderFragment(SoundCloudApplication app) {
        super();

        mConnections = Connections.get().getObjectsOrNull();
        onConnections(mConnections, true);
        Connections.get().requestUpdate(app, false, this);
    }

    @Override
    protected boolean canAppend() {
        return true && super.canAppend();
    }


    private void onConnections(List<Connection> connections, boolean refresh) {
        mConnections = connections;

        if (connections == null) {
            setState(States.CONNECTION_ERROR, refresh);
        } else {
            if (Connection.checkConnectionListForService(connections, Connection.Service.Facebook)) {
                setState(States.FB_CONNECTION, refresh);
            } else {
                setState(States.NO_FB_CONNECTION, refresh);
            }
        }
    }

    @Override
    public void executeRefreshTask() {
        Connections.get().requestUpdate(getScActivity().getApp(), true, this);
        setState(States.LOADING, true);
    }

    public void onChanged(List<Connection> connections, ParcelCache<Connection> cache) {
        mConnections = connections;
        onConnections(connections, true);
    }

    public void setState(int state, boolean reset) {
        mCurrentState = state;
        configureEmptyCollection();
        if (reset){
            reset();
        } else {
            final ScBaseAdapter listAdapter = getListAdapter();
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void configureEmptyCollection() {
        if (mEmptyCollection != null) {
            switch (mCurrentState) {
                case States.LOADING:
                    mEmptyCollection.setMode(EmptyCollection.Mode.WAITING_FOR_DATA);
                    break;

                case States.CONNECTION_ERROR:
                    mEmptyCollection.setMode(FriendFinderEmptyCollection.FriendFinderMode.CONNECTION_ERROR);
                    break;

                case States.NO_FB_CONNECTION:
                    mEmptyCollection.setMode(FriendFinderEmptyCollection.FriendFinderMode.NO_CONNECTIONS);
                    break;

                case States.FB_CONNECTION:
                    super.configureEmptyCollection();
                    break;
            }
        }

    }
}
