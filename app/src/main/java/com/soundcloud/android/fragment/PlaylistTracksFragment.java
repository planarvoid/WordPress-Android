package com.soundcloud.android.fragment;

import com.handmark.pulltorefresh.extras.listfragment.PullToRefreshListFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.PlaylistTracksAdapter;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.PlayInfo;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.ScListView;
import org.jetbrains.annotations.NotNull;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

public class PlaylistTracksFragment extends PullToRefreshListFragment implements LoaderManager.LoaderCallbacks<Cursor>,
        PullToRefreshBase.OnRefreshListener, LocalCollection.OnChangeListener {

    public static final String PLAYLIST_URI = "playlistUri";

    private static final int PLAYER_LIST_LOADER = 0x01;
    private Uri mContentUri, mPlaylistUri;
    private LocalCollection mLocalCollection;
    private TextView mInfoHeader;

    private PlaylistTracksAdapter mAdapter;

    public static PlaylistTracksFragment newInstance(@NotNull Uri playlistUri) {
        PlaylistTracksFragment playlistTracksFragment = new PlaylistTracksFragment();
        Bundle args = new Bundle();
        args.putParcelable("playlistUri", playlistUri);
        playlistTracksFragment.setArguments(args);
        return playlistTracksFragment;
    }

    @Override
    protected PullToRefreshListView onCreatePullToRefreshListView(LayoutInflater inflater, Bundle savedInstanceState) {
        final ScListView scListView = new ScListView(getActivity());
        scListView.setOnRefreshListener(this);

        mInfoHeader = (TextView) View.inflate(getActivity(), R.layout.playlist_header, null);
        scListView.getRefreshableView().addHeaderView(mInfoHeader);
        setHeaderInfo();

        return scListView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPlaylistUri = (Uri) getArguments().get(PLAYLIST_URI);
        Playlist p = SoundCloudApplication.MODEL_MANAGER.getPlaylist(mPlaylistUri);

        // TODO. the playlist could be null here. What then?
        mContentUri = Content.PLAYLIST_TRACKS.forId(p.id);
        getLoaderManager().initLoader(PLAYER_LIST_LOADER, null, this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mLocalCollection == null){
            mLocalCollection = LocalCollection.fromContentUri(mContentUri, getActivity().getContentResolver(), true);
            syncPlaylist();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mLocalCollection.startObservingSelf(getActivity().getContentResolver(), this);
        setRefreshingState();
    }

    @Override
    public void onStop() {
        super.onStop();
        mLocalCollection.stopObservingSelf();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        PlayInfo info = new PlayInfo();
        info.initialTrack = SoundCloudApplication.MODEL_MANAGER.getTrack(id);
        info.uri = mContentUri;
        info.position = position - getListView().getHeaderViewsCount();
        PlayUtils.playTrack(getActivity(), info);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), mContentUri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mAdapter == null) {
            final Playlist playlist = SoundCloudApplication.MODEL_MANAGER.getPlaylist(mPlaylistUri);

            // if we can show something (or should show nothing), set the adapter
            if (data != null && (data.getCount() > 0 || playlist.track_count == 0)) {
                mAdapter = new PlaylistTracksAdapter(getActivity().getApplicationContext(), data, true);
                setListShownNoAnimation(true);
                setListAdapter(mAdapter);
            }

            // if we don't have the entire playlist, re-sync the playlist.
            if (data == null || data.getCount() < playlist.track_count) {
                syncPlaylist();
            }

        } else {
            mAdapter.swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mAdapter != null) mAdapter.swapCursor(null);

    }

    @Override
    public void onRefresh(PullToRefreshBase refreshView) {
        syncPlaylist();
    }

    @Override
    public void onLocalCollectionChanged() {
        setRefreshingState();
        if (mLocalCollection.sync_state == LocalCollection.SyncState.IDLE && mInfoHeader != null){
            setHeaderInfo();
        }
    }

    public void refreshTrackList() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    private void syncPlaylist() {
        final FragmentActivity activity = getActivity();
        if (activity != null){
            getPullToRefreshListView().setRefreshing(true);
            activity.startService(new Intent(activity, ApiSyncService.class)
                            .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                            .setData(mPlaylistUri));
        }
    }

    private void setRefreshingState() {
        if (mLocalCollection.sync_state != LocalCollection.SyncState.IDLE) {
            getPullToRefreshListView().setRefreshing(true);
        } else if (getPullToRefreshListView().isRefreshing()) {
            getPullToRefreshListView().onRefreshComplete();
        }
    }

    private void setHeaderInfo() {
        Playlist p = SoundCloudApplication.MODEL_MANAGER.getPlaylist(mPlaylistUri);
        final String trackCount = getResources().getQuantityString(R.plurals.number_of_sounds, p.track_count, p.track_count);
        final String duration = ScTextUtils.formatTimestamp(p.duration);
        mInfoHeader.setText(getString(R.string.playlist_info_header_text, trackCount, duration));
    }

}
