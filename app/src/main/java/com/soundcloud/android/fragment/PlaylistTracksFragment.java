package com.soundcloud.android.fragment;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.PlaylistTracksAdapter;
import com.soundcloud.android.model.PlayInfo;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.android.view.ScListView;
import org.jetbrains.annotations.NotNull;

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
import android.widget.TextView;

public class PlaylistTracksFragment extends Fragment implements AdapterView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor>, PullToRefreshBase.OnRefreshListener, DetachableResultReceiver.Receiver {

    public static final String PLAYLIST_URI = "playlistUri";

    private static final int PLAYER_LIST_LOADER = 0x01;
    private Uri mContentUri, mPlaylistUri;
    private TextView mInfoHeader;

    private PlaylistTracksAdapter mAdapter;
    private ScListView mListView;
    private EmptyListView mEmptyView;

    private final DetachableResultReceiver mDetachableReceiver = new DetachableResultReceiver(new Handler());

    public static PlaylistTracksFragment newInstance(@NotNull Uri playlistUri) {
        PlaylistTracksFragment playlistTracksFragment = new PlaylistTracksFragment();
        Bundle args = new Bundle();
        args.putParcelable("playlistUri", playlistUri);
        playlistTracksFragment.setArguments(args);
        return playlistTracksFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPlaylistUri = (Uri) getArguments().get(PLAYLIST_URI);
        Playlist p = SoundCloudApplication.MODEL_MANAGER.getPlaylist(mPlaylistUri);

        // TODO. the playlist could be null here. What then?
        mContentUri = Content.PLAYLIST_TRACKS.forId(p.id);
        getLoaderManager().initLoader(PLAYER_LIST_LOADER, null, this);

        mAdapter = new PlaylistTracksAdapter(getActivity().getApplicationContext());

        mDetachableReceiver.setReceiver(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.playlist_fragment, container, false);

        mListView = (ScListView) layout.findViewById(android.R.id.list);
        mListView.setOnRefreshListener(this);

        mInfoHeader = (TextView) View.inflate(getActivity(), R.layout.playlist_header, null);
        mListView.getRefreshableView().addHeaderView(mInfoHeader, null, false);
        setHeaderInfo();

        mEmptyView = (EmptyListView) layout.findViewById(android.R.id.empty);
        mEmptyView.setMessageText(getActivity().getString(R.string.empty_playlist));
        mListView.setEmptyView(mEmptyView);
        mListView.setAdapter(mAdapter);

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkPlaylistSize();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PlayInfo info = new PlayInfo();
        info.initialTrack = SoundCloudApplication.MODEL_MANAGER.getTrack(id);
        info.uri = mContentUri;
        info.position = position - mListView.getRefreshableView().getHeaderViewsCount();
        PlayUtils.playTrack(getActivity(), info);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), mContentUri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        checkPlaylistSize();
    }

    private void checkPlaylistSize() {
        Playlist p = SoundCloudApplication.MODEL_MANAGER.getPlaylist(mPlaylistUri);
        // if we don't have the entire playlist, re-sync the playlist.
        if (mAdapter.getCount() < p.track_count) {
            mEmptyView.setStatus(EmptyListView.Status.WAITING);
            syncPlaylist();
        } else {
            mEmptyView.setStatus(EmptyListView.Status.OK);
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

    public void refreshTrackList() {
        mAdapter.notifyDataSetChanged();
    }

    private void syncPlaylist() {
        final FragmentActivity activity = getActivity();
        if (activity != null) {
            mListView.setRefreshing(true);
            activity.startService(new Intent(activity, ApiSyncService.class)
                    .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                    .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, mDetachableReceiver)
                    .setData(mPlaylistUri));
        }
    }

    private void setHeaderInfo() {
        Playlist p = SoundCloudApplication.MODEL_MANAGER.getPlaylist(mPlaylistUri);
        final String trackCount = getResources().getQuantityString(R.plurals.number_of_sounds, p.track_count, p.track_count);
        final String duration = ScTextUtils.formatTimestamp(p.duration);
        mInfoHeader.setText(getString(R.string.playlist_info_header_text, trackCount, duration));
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_SYNC_FINISHED:
                mEmptyView.setStatus(EmptyListView.Status.OK);
                break;
            case ApiSyncService.STATUS_SYNC_ERROR:
                mEmptyView.setStatus(EmptyListView.Status.CONNECTION_ERROR);
                break;
        }

        mListView.onRefreshComplete();
        setHeaderInfo();
    }
}
