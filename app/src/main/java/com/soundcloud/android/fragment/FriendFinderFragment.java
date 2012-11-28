package com.soundcloud.android.fragment;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.auth.Connect;
import com.soundcloud.android.adapter.ScBaseAdapter;
import com.soundcloud.android.cache.ConnectionsCache;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.create.NewConnectionTask;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.view.EmptyCollection;
import com.soundcloud.android.view.FriendFinderEmptyCollection;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Set;

public class FriendFinderFragment extends ScListFragment implements ConnectionsCache.Listener {

    private Set<Connection> mConnections;

    private int mCurrentState;
    private AsyncTask<Connection.Service, Void, Uri> mConnectionTask;

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

        final ConnectionsCache connectionsCache = ConnectionsCache.get(app);
        mConnections = connectionsCache.getConnections();
        if (mConnections == null){
            setState(States.LOADING, false);
        } else {
            onConnections(mConnections, true);
        }
        connectionsCache.requestConnections(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        if (mEmptyCollection != null) {
            mEmptyCollection.setButtonActionListener(new EmptyCollection.ActionListener() {
                @Override
                public void onAction() {
                    if (AndroidUtils.isTaskFinished(mConnectionTask)) {
                        setState(States.LOADING, false);
                        final ScActivity scActivity = getScActivity();
                        mConnectionTask = new NewConnectionTask(scActivity.getApp()) {
                            @Override
                            protected void onPostExecute(Uri uri) {
                                if (uri != null) {
                                    scActivity.startActivityForResult(
                                            (new Intent(scActivity, Connect.class))
                                                    .putExtra("service", Connection.Service.Facebook.name())
                                                    .setData(uri),
                                            Consts.RequestCodes.MAKE_CONNECTION);
                                } else {
                                    scActivity.showToast(R.string.new_connection_error);
                                    setState(States.NO_FB_CONNECTION, false);
                                }
                            }
                        }.execute(Connection.Service.Facebook);
                    }
                }

                @Override
                public void onSecondaryAction() {
                }
            });
        }
        return v;
    }

    private void onConnections(Set<Connection> connections, boolean refresh) {
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
    public void onConnectionsRefreshed(Set<Connection> connections, boolean changed) {
        onConnections(connections, true);
        refresh(false);
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
