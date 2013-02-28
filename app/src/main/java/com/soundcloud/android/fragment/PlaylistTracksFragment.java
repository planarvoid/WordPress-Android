package com.soundcloud.android.fragment;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.PlaylistTracksAdapter;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.PlayInfo;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.android.view.ScListView;
import org.jetbrains.annotations.Nullable;

import android.content.Intent;
import android.database.Cursor;
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

public class PlaylistTracksFragment extends Fragment implements AdapterView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor>, PullToRefreshBase.OnRefreshListener, DetachableResultReceiver.Receiver, LocalCollection.OnChangeListener {

    public static final String PLAYLIST_URI = "playlistUri";

    private static final int PLAYER_LIST_LOADER = 0x01;
    private Playlist mPlaylist = new Playlist();
    private TextView mInfoHeader;

    private boolean mListShown;
    private @Nullable LocalCollection mLocalCollection;

    private PlaylistTracksAdapter mAdapter;
    private ScListView mListView;
    private View mListViewContainer, mProgressView;
    private EmptyListView mEmptyView;

    private int mScrollToPos = -1;

    private final DetachableResultReceiver mDetachableReceiver = new DetachableResultReceiver(new Handler());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new PlaylistTracksAdapter(getActivity().getApplicationContext());
        getLoaderManager().initLoader(PLAYER_LIST_LOADER, null, this);
        mDetachableReceiver.setReceiver(this);

        if (mPlaylist != null) refresh(mPlaylist); //force a content refresh
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.playlist_fragment, container, false);

        mListViewContainer = layout.findViewById(R.id.listContainer);
        mProgressView = layout.findViewById(android.R.id.progress);

        mListView = (ScListView) layout.findViewById(android.R.id.list);
        mListView.setOnRefreshListener(this);
        mListView.setOnItemClickListener(this);

        mInfoHeader = (TextView) View.inflate(getActivity(), R.layout.playlist_header, null);
        mListView.getRefreshableView().addHeaderView(mInfoHeader, null, false);
        setHeaderInfo();

        mEmptyView = (EmptyListView) layout.findViewById(android.R.id.empty);
        mEmptyView.setMessageText(getActivity().getString(R.string.empty_playlist));
        mListView.setEmptyView(mEmptyView);
        mListView.setAdapter(mAdapter);

        setListShown(mListShown);

        return layout;
    }

    private void setListShown(boolean show){
        mListShown = show;
        if (mListShown){
            mListViewContainer.setVisibility(View.VISIBLE);
            mProgressView.setVisibility(View.GONE);
        } else {
            mListViewContainer.setVisibility(View.GONE);
            mProgressView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        syncIfNecessary();

    }

    @Override
    public void onStart() {
        super.onStart();
        mLocalCollection.startObservingSelf(getActivity().getContentResolver(),this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mLocalCollection.stopObservingSelf();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PlayInfo info = new PlayInfo();
        info.initialTrack = SoundCloudApplication.MODEL_MANAGER.getTrack(id);
        info.uri = mPlaylist.toUri();
        info.position = position - mListView.getRefreshableView().getHeaderViewsCount();
        PlayUtils.playTrack(getActivity(), info);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), Content.PLAYLIST_TRACKS.forQuery(String.valueOf(mPlaylist.id)),
                null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        setListShown(mAdapter.isEmpty() ? mLocalCollection.isIdle() : true);

        if (mScrollToPos != -1 && mListView != null){
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

    public void refreshTrackList() {
        mAdapter.notifyDataSetChanged();
    }

    private void setHeaderInfo() {
        if (isAdded() && mInfoHeader != null){ // make sure we are attached to an activity
            final String trackCount = getResources().getQuantityString(R.plurals.number_of_sounds, mPlaylist.track_count, mPlaylist.track_count);
            final String duration = ScTextUtils.formatTimestamp(mPlaylist.duration);
            mInfoHeader.setText(getString(R.string.playlist_info_header_text, trackCount, duration));
        }
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_SYNC_FINISHED:
                refresh(mPlaylist);
                break;
            case ApiSyncService.STATUS_SYNC_ERROR:
                mEmptyView.setStatus(EmptyListView.Status.CONNECTION_ERROR);
                setListShown(true);
                break;
        }

        mListView.onRefreshComplete();
    }

    public void refresh(Playlist playlist) {
        mPlaylist = playlist;

        if (isAdded()) {
            if (mPlaylist != null && (mLocalCollection == null || !mLocalCollection.uri.equals(mPlaylist.toUri()))) {
                mLocalCollection = LocalCollection.fromContentUri(mPlaylist.toUri(), getActivity().getContentResolver(), true);
            }
            getActivity().getSupportLoaderManager().restartLoader(PLAYER_LIST_LOADER, null, this);
            syncIfNecessary();
            setHeaderInfo();
        }
    }

    public void scrollToPosition(int position) {
        if (mListView != null){
            final ListView refreshableView = mListView.getRefreshableView();
            final int adjustedPosition = position + refreshableView.getHeaderViewsCount();

            refreshableView.setSelectionFromTop(
                    adjustedPosition, (int) (50 * getResources().getDisplayMetrics().density));

        } else {
           mScrollToPos = position;
        }
    }

    private void syncIfNecessary(){
        if (mPlaylist.isLocal() || mLocalCollection.shouldAutoRefresh()) {
            syncPlaylist();
        }
    }

    private void syncPlaylist() {
        final FragmentActivity activity = getActivity();
        if (isAdded() && mLocalCollection.isIdle()) {
            if (mListView != null) mListView.setRefreshing(false);
            activity.startService(new Intent(activity, ApiSyncService.class)
                    .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                    .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, mDetachableReceiver)
                    .setData(mPlaylist.toUri()));
        }
    }

    @Override
    public void onLocalCollectionChanged() {
        if (mAdapter != null && mAdapter.isEmpty()) setListShown(mLocalCollection.isIdle());
    }
}
