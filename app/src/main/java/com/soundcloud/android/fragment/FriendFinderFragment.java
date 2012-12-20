package com.soundcloud.android.fragment;

import static com.soundcloud.android.utils.AndroidUtils.isTaskFinished;
import static com.soundcloud.android.utils.AndroidUtils.showToast;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.auth.Connect;
import com.soundcloud.android.cache.ConnectionsCache;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.task.create.NewConnectionTask;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.android.view.FriendFinderEmptyCollection;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.Set;

public class FriendFinderFragment extends ScListFragment implements ConnectionsCache.Listener {
    private Set<Connection> mConnections;
    private int mCurrentState;
    private @Nullable
    AsyncTask<Connection.Service, Void, Uri> mConnectionTask;

    public interface States {
        int LOADING          = 0;
        int NO_FB_CONNECTION = 1;
        int FB_CONNECTION    = 2;
        int CONNECTION_ERROR = 3;
    }

    public static FriendFinderFragment newInstance() {
        FriendFinderFragment fragment = new FriendFinderFragment();
        Bundle args = new Bundle();
        args.putParcelable("contentUri", Content.ME_FRIENDS.uri);
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressWarnings("UnusedDeclaration") // need empty ctor for fragments
    public FriendFinderFragment() {
        super();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mConnectionTask = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestConnections(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        if (mEmptyListView != null) {
            mEmptyListView.setButtonActionListener(new EmptyListView.ActionListener() {
                @Override
                public void onAction() {
                    if (isTaskFinished(mConnectionTask)) {
                        setState(States.LOADING, false);
                        mConnectionTask = loadFacebookConnections();
                    }
                }

                @Override
                public void onSecondaryAction() {
                }
            });
        }
        return v;
    }

    public void setState(int state, boolean reset) {
        mCurrentState = state;
        configureEmptyView();

        final BaseAdapter listAdapter = getListAdapter();
        if (listAdapter == null) return;

        if (reset) {
            reset();
        } else {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onConnectionsRefreshed(Set<Connection> connections, boolean changed) {
        onConnections(connections, true);
        refresh(false);
    }

    @Override
    protected void configureEmptyView() {
        if (mEmptyListView != null) {
            switch (mCurrentState) {
                case States.LOADING:
                    mEmptyListView.setMode(EmptyListView.Mode.WAITING_FOR_DATA);
                    break;

                case States.CONNECTION_ERROR:
                    mEmptyListView.setMode(FriendFinderEmptyCollection.FriendFinderMode.CONNECTION_ERROR);
                    break;

                case States.NO_FB_CONNECTION:
                    mEmptyListView.setMode(FriendFinderEmptyCollection.FriendFinderMode.NO_CONNECTIONS);
                    break;

                case States.FB_CONNECTION:
                    super.configureEmptyView();
                    break;
            }
        }
    }

    private AsyncTask<Connection.Service, Void, Uri> loadFacebookConnections() {
        return new NewConnectionTask(getScActivity().getApp()) {
            @Override
            protected void onPostExecute(Uri uri) {
                if (uri != null) {
                    getActivity().startActivityForResult(
                            (new Intent(getActivity(), Connect.class))
                                    .putExtra("service", Connection.Service.Facebook.name())
                                    .setData(uri),
                            Consts.RequestCodes.MAKE_CONNECTION);
                } else {
                    showToast(getActivity(), R.string.new_connection_error);
                    setState(States.NO_FB_CONNECTION, false);
                }
            }
        }.execute(Connection.Service.Facebook);
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

    public void requestConnections(Context context) {
        if (getActivity() == null) return;

        final ConnectionsCache connectionsCache = ConnectionsCache.get(context);
        mConnections = connectionsCache.getConnections();
        if (mConnections == null){
            setState(States.LOADING, false);
        } else {
            onConnections(mConnections, true);
        }
        connectionsCache.requestConnections(this);
    }
}
