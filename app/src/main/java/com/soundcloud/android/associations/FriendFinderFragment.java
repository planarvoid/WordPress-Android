package com.soundcloud.android.associations;

import static com.soundcloud.android.utils.AndroidUtils.isTaskFinished;
import static com.soundcloud.android.utils.AndroidUtils.showToast;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.onboarding.auth.ConnectActivity;
import com.soundcloud.android.cache.ConnectionsCache;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.creators.upload.tasks.NewConnectionTask;
import com.soundcloud.android.view.EmptyListView;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.BaseAdapter;

import java.util.Set;

public class FriendFinderFragment extends ScListFragment implements ConnectionsCache.Listener {
    private Set<Connection> mConnections;
    private int mCurrentStatus;
    private @Nullable
    AsyncTask<Connection.Service, Void, Uri> mConnectionTask;

    // the usual list fragment statuses, plus 2 special ones for connections
    public interface Status extends EmptyListView.Status {
        int FB_CONNECTION = HttpStatus.SC_OK;
        int NO_CONNECTIONS = 1000;
        int CONNECTION_ERROR = 1001;
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

    public void setStatus(int status, boolean reset) {
        mCurrentStatus = status;
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
    protected EmptyListView createEmptyView() {
        final EmptyListView emptyView = new FriendFinderEmptyView(getActivity());
        emptyView.setActionListener(new EmptyListView.ActionListener() {
            @Override
            public void onAction() {
                if (isTaskFinished(mConnectionTask)) {
                    setStatus(Status.WAITING, false);
                    mConnectionTask = loadFacebookConnections();
                }
            }

            @Override
            public void onSecondaryAction() {
            }
        });
        return emptyView;
    }

    @Override
    protected void configureEmptyView() {
        switch (mCurrentStatus) {
            case Status.WAITING:
            case Status.CONNECTION_ERROR:
            case Status.NO_CONNECTIONS:
                mStatusCode = mCurrentStatus;
                final EmptyListView listView = getEmptyListView();
                if (listView != null) listView.setStatus(mCurrentStatus);
                break;

            case Status.FB_CONNECTION:
                super.configureEmptyView();
                break;
        }
    }

    @Override
    protected void configureEmptyView(int statusCode) {
        if (mCurrentStatus == Status.FB_CONNECTION){
            super.configureEmptyView(statusCode);
        }
    }

    private AsyncTask<Connection.Service, Void, Uri> loadFacebookConnections() {
        final ScActivity scActivity = getScActivity();
        if (scActivity != null) {
            return new NewConnectionTask(publicApi) {
                @Override
                protected void onPostExecute(Uri uri) {
                    if (uri != null) {
                        getActivity().startActivityForResult(
                                (new Intent(getActivity(), ConnectActivity.class))
                                        .putExtra("service", Connection.Service.Facebook.name())
                                        .setData(uri),
                                Consts.RequestCodes.MAKE_CONNECTION);
                    } else {
                        showToast(getActivity(), R.string.new_connection_error);
                        setStatus(FriendFinderFragment.Status.NO_CONNECTIONS, false);
                    }
                }
            }.execute(Connection.Service.Facebook);
        } else {
            return null;
        }
    }

    private void onConnections(Set<Connection> connections, boolean refresh) {
        mConnections = connections;

        if (connections == null) {
            setStatus(Status.CONNECTION_ERROR, refresh);
        } else {
            if (Connection.checkConnectionListForService(connections, Connection.Service.Facebook)) {
                setStatus(Status.FB_CONNECTION, refresh);
            } else {
                setStatus(Status.NO_CONNECTIONS, refresh);
            }
        }
    }

    public void requestConnections(Context context) {
        if (getActivity() == null) return;

        final ConnectionsCache connectionsCache = ConnectionsCache.get(context);
        mConnections = connectionsCache.getConnections();
        if (mConnections == null){
            setStatus(Status.WAITING, false);
        } else {
            onConnections(mConnections, true);
        }
        connectionsCache.requestConnections(this);
    }
}
