package com.soundcloud.android.playlists;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScListView;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.EmptyListView;
import org.jetbrains.annotations.Nullable;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PlaylistTracksFragment extends Fragment implements AdapterView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor>, PullToRefreshBase.OnRefreshListener, DetachableResultReceiver.Receiver, LocalCollection.OnChangeListener {

    private static final int TRACK_LIST_LOADER = 1;
    private Playlist mPlaylist;
    private TextView mInfoHeaderText;

    private boolean mListShown;
    private LocalCollection mLocalCollection;

    private PlaylistTracksAdapter mAdapter;
    private ScListView mListView;
    private View mListViewContainer, mProgressView;
    private EmptyListView mEmptyView;

    private int mScrollToPos = -1;

    private final DetachableResultReceiver mDetachableReceiver = new DetachableResultReceiver(new Handler());

    private SyncStateManager mSyncStateManager;
    private PlaybackOperations mPlaybackOperations;
    private ImageOperations mImageOperations = SoundCloudApplication.getImageOperations();

    public static PlaylistTracksFragment create(Uri playlistUri, Screen originScreen) {
        Bundle args = new Bundle();
        args.putParcelable(Playlist.EXTRA_URI, playlistUri);
        originScreen.addToBundle(args);

        PlaylistTracksFragment fragment = new PlaylistTracksFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPlaylist = Playlist.fromBundle(getArguments());
        mLocalCollection = getLocalCollection();
        mPlaybackOperations = new PlaybackOperations();

        if (mLocalCollection == null) {
            Toast.makeText(getActivity(), R.string.playlist_removed, Toast.LENGTH_SHORT).show();
            getActivity().finish();
        } else {
            mSyncStateManager = new SyncStateManager(getActivity());
            mAdapter = new PlaylistTracksAdapter(getActivity().getApplicationContext());
            getLoaderManager().initLoader(TRACK_LIST_LOADER, null, this);
            mDetachableReceiver.setReceiver(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.playlist_fragment, container, false);

        mListViewContainer = layout.findViewById(R.id.listContainer);
        mProgressView = layout.findViewById(R.id.playlist_loading_view);

        mListView = (ScListView) layout.findViewById(R.id.list);
        mListView.setOnRefreshListener(this);
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(mImageOperations.createScrollPauseListener(false, true));

        View mInfoHeader = View.inflate(getActivity(), R.layout.playlist_header, null);
        mInfoHeaderText = (TextView) mInfoHeader.findViewById(android.R.id.text1);
        mListView.getRefreshableView().addHeaderView(mInfoHeader, null, false);

        mEmptyView = (EmptyListView) layout.findViewById(android.R.id.empty);
        mEmptyView.setMessageText(getActivity().getString(R.string.empty_playlist));
        mEmptyView.setStatus(EmptyListView.Status.OK);
        mListView.setEmptyView(mEmptyView);
        mListView.setAdapter(mAdapter);

        setListShown(mListShown);

        return layout;
    }

    private void setListShown(boolean show) {
        mListShown = show;
        if (mListShown) {
            mListViewContainer.setVisibility(View.VISIBLE);
            mProgressView.setVisibility(View.GONE);
        } else {
            mListViewContainer.setVisibility(View.GONE);
            mProgressView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // observe changes to the local collection to update the last updated timestamp
        mSyncStateManager.addChangeListener(mLocalCollection, this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mSyncStateManager.removeChangeListener(mLocalCollection);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final int startPosition = position - mListView.getRefreshableView().getHeaderViewsCount();
        final Track track = SoundCloudApplication.sModelManager.getTrack(id);
        mPlaybackOperations.playFromPlaylist(getActivity(), mPlaylist.toUri(), startPosition, track,
                Screen.fromBundle(getArguments()));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), Content.PLAYLIST_TRACKS.forQuery(String.valueOf(mPlaylist.getId())),
                null, null, null, null);
    }

    // fires when the loader has finished reading playlist tracks from the local DB
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);

        setHeaderInfo();

        boolean syncing = syncIfNecessary();
        boolean isIdle = mLocalCollection != null && mLocalCollection.isIdle() && !syncing;
        setListShown(!mAdapter.isEmpty() || isIdle);

        if (mScrollToPos != -1 && mListView != null) {
            scrollToPosition(mScrollToPos);
            mScrollToPos = -1;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onRefresh(PullToRefreshBase refreshView) {
        syncPlaylist();
    }

    private void setHeaderInfo() {
        final String trackCount = getResources().getQuantityString(R.plurals.number_of_sounds, mPlaylist.getTrackCount(), mPlaylist.getTrackCount());
        final String duration = ScTextUtils.formatTimestamp(mPlaylist.duration);
        mInfoHeaderText.setText(getString(R.string.playlist_info_header_text, trackCount, duration));
    }

    // fires when the playlist sync operation returns
    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_SYNC_FINISHED:
                refresh();
                break;
            case ApiSyncService.STATUS_SYNC_ERROR:
                mEmptyView.setStatus(EmptyListView.Status.CONNECTION_ERROR);
                setListShown(true);
                break;
        }
        mListView.onRefreshComplete();
    }

    @Override
    public void onLocalCollectionChanged(LocalCollection localCollection) {
        mLocalCollection = localCollection;
    }

    public void refresh() {
        if (isAdded()) {
            mLocalCollection = getLocalCollection();
            getLoaderManager().restartLoader(TRACK_LIST_LOADER, null, this);
        }
    }

    @Nullable
    private LocalCollection getLocalCollection() {
        return mPlaylist == null ? null : new SyncStateManager(getActivity()).fromContent(mPlaylist.toUri());
    }

    public void scrollToPosition(int position) {
        if (mListView != null) {
            final ListView refreshableView = mListView.getRefreshableView();
            final int adjustedPosition = position + refreshableView.getHeaderViewsCount();

            refreshableView.setSelectionFromTop(
                    adjustedPosition, (int) (50 * getResources().getDisplayMetrics().density));

        } else {
            mScrollToPos = position;
        }
    }

    private boolean syncIfNecessary() {
        if (mPlaylist.isLocal() || mLocalCollection.shouldAutoRefresh()) {
            syncPlaylist();
            return true;
        } else {
            return false;
        }
    }

    private void syncPlaylist() {
        final FragmentActivity activity = getActivity();
        if (isAdded()) {
            if (mPlaylist.isLocal()){
                activity.startService(new Intent(activity, ApiSyncService.class)
                        .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                        .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, mDetachableReceiver)
                        .setData(Content.ME_PLAYLISTS.uri));

            } else
            if (mLocalCollection.isIdle()) {
                if (mListView != null && isInLayout()) mListView.setRefreshing(false);
                activity.startService(new Intent(activity, ApiSyncService.class)
                        .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                        .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, mDetachableReceiver)
                        .setData(mPlaylist.toUri()));
            }
        }
    }

    public PlaylistTracksAdapter getAdapter() {
        return mAdapter;
    }
}
