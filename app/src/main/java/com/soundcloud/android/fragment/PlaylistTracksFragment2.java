package com.soundcloud.android.fragment;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.IScAdapter;
import com.soundcloud.android.adapter.PlaylistTracksAdapter2;
import com.soundcloud.android.model.PlayInfo;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.ScFunctions;
import com.soundcloud.android.rx.event.Event;
import com.soundcloud.android.rx.syncing.LoadPlaylistStrategy;
import com.soundcloud.android.rx.syncing.LoadPlaylistTracksStrategy;
import com.soundcloud.android.rx.syncing.SyncOperations;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.EmptyListView;
import rx.Observable;
import rx.Observer;
import rx.Subscription;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class PlaylistTracksFragment2 extends ReactiveListFragment<Track> {

    private Playlist mPlaylist;

    private LoadPlaylistTracksStrategy mTracksStorage;
    private PlaylistObserver mPlaylistObserver;
    private SyncOperations<Playlist> mSyncOperations;

    private Observable<List<Track>> mLoadTracks;
    private Subscription mTrackAssocChangedSubscription;

    private TextView mInfoHeaderText;

    private int mScrollToPos = -1;

    public static PlaylistTracksFragment2 create(Uri playlistUri) {
        Bundle args = new Bundle();
        args.putParcelable(Playlist.EXTRA_URI, playlistUri);

        PlaylistTracksFragment2 fragment = new PlaylistTracksFragment2();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPlaylist = Playlist.fromBundle(getArguments());

        mTracksStorage = new LoadPlaylistTracksStrategy(getActivity());
        mSyncOperations = new SyncOperations<Playlist>(getActivity(), new LoadPlaylistStrategy(getActivity()));
        mPlaylistObserver = new PlaylistObserver();

        mLoadTracks = mTracksStorage.loadFromContentUri(mPlaylist.toUri());

        mTrackAssocChangedSubscription = Event.anyOf(Event.LIKE_CHANGED, Event.REPOST_CHANGED).subscribe(mLoadTracks, mLoadItemsObserver);

        // since we need to sync the playlist first, but the list fragment is modeled around a playlist's tracks,
        // so we need to map the sync operation to return the playlist's tracks first
        if (savedInstanceState == null) {
            addPendingObservable(
                    mSyncOperations.syncIfNecessary(mPlaylist.toUri()).map(ScFunctions.PLAYLIST_OBS_TO_TRACKS_OBS));
        }
    }

    @Override
    protected IScAdapter<Track> newAdapter() {
        return new PlaylistTracksAdapter2(getActivity(), (Uri) getArguments().get(Playlist.EXTRA_URI));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);

        View mInfoHeader = View.inflate(getActivity(), R.layout.playlist_header, null);
        mInfoHeaderText = (TextView) mInfoHeader.findViewById(android.R.id.text1);
        mListView.getRefreshableView().addHeaderView(mInfoHeader, null, false);

        updateHeaderInfo();

        return layout;
    }

    @Override
    protected void configureEmptyListView(EmptyListView emptyView) {
        emptyView.setMessageText(getActivity().getString(R.string.empty_playlist));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTrackAssocChangedSubscription.unsubscribe();
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
    public void onRefresh(PullToRefreshBase refreshView) {
        super.onRefresh(refreshView);
        // sync the playlist, then reload its tracks and update the synced playlist instance
        Observable.zip(mSyncOperations.syncNow(mPlaylist.toUri()),
                       mTracksStorage.loadFromContentUri(mPlaylist.toUri()),
                       ScFunctions.FOLD_TRACKS_INTO_PLAYLIST)
                .subscribe(mPlaylistObserver);
    }

    public void onPlaylistChanged(final Playlist playlist) {
        mPlaylist = playlist;
        mAdapter.notifyDataSetChanged();
        updateHeaderInfo();
    }

    private void updateHeaderInfo() {
        final int trackCount = mPlaylist.getTrackCount();
        final String countString = getResources().getQuantityString(R.plurals.number_of_sounds, trackCount, trackCount);
        final String duration = ScTextUtils.formatTimestamp(mPlaylist.duration);
        mInfoHeaderText.setText(getString(R.string.playlist_info_header_text, countString, duration));
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

    // we pretty much treat refreshes of the playlist like refreshes of its tracks, so we forward this to the list
    // observer mostly
    private class PlaylistObserver implements Observer<Playlist> {

        @Override
        public void onCompleted() {
            mLoadItemsObserver.onCompleted();
        }

        @Override
        public void onError(Exception e) {
            mLoadItemsObserver.onError(e);
        }

        @Override
        public void onNext(Playlist playlist) {
            onPlaylistChanged(playlist);
        }
    }


}
