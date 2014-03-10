package com.soundcloud.android.playlists;

import static rx.android.observables.AndroidObservable.fromFragment;

import com.google.common.annotations.VisibleForTesting;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.EngagementsController;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.collections.ScListView;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageSize;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.playback.views.PlayablePresenter;
import com.soundcloud.android.storage.NotFoundException;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.UriUtils;
import com.soundcloud.android.view.EmptyListView;
import org.jetbrains.annotations.Nullable;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class PlaylistTracksFragment extends Fragment implements AdapterView.OnItemClickListener,
        PullToRefreshBase.OnRefreshListener, DetachableResultReceiver.Receiver, LocalCollection.OnChangeListener {

    private Playlist mPlaylist;
    private TextView mInfoHeaderText;

    private boolean mListShown;
    private LocalCollection mLocalCollection;

    private PlaylistTracksAdapter mAdapter;
    private ScListView mListView;
    private View mListViewContainer;
    private View mProgressView;
    private EmptyListView mEmptyView;

    private int mScrollToPos = -1;

    private final DetachableResultReceiver mDetachableReceiver = new DetachableResultReceiver(new Handler());

    private PlayablePresenter mPlayableDecorator;

    @Inject
    SyncStateManager mSyncStateManager;
    @Inject
    PlaylistOperations mPlaylistOperations;
    @Inject
    PlaybackOperations mPlaybackOperations;
    @Inject
    ImageOperations mImageOperations;
    @Inject
    SoundAssociationOperations mSoundAssocOps;
    @Inject
    EventBus mEventBus;
    @Inject
    EngagementsController mEngagementsController;

    public PlaylistTracksFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    public PlaylistTracksFragment(PlaybackOperations playbackOperations, PlaylistOperations playlistOperations,
                                  ImageOperations imageOperations, SyncStateManager syncStateManager,
                                  EngagementsController engagementsController) {
        mPlaybackOperations = playbackOperations;
        mPlaylistOperations = playlistOperations;
        mImageOperations = imageOperations;
        mSyncStateManager = syncStateManager;
        mEngagementsController = engagementsController;
    }

    public static PlaylistTracksFragment create(Uri playlistUri, Screen originScreen) {
        Bundle args = new Bundle();
        args.putParcelable(Playlist.EXTRA_URI, playlistUri);
        originScreen.addToBundle(args);

        PlaylistTracksFragment fragment = createFragmentInstance();
        fragment.setArguments(args);
        return fragment;
    }

    protected static PlaylistTracksFragment createFragmentInstance() {
        return new PlaylistTracksFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLocalCollection = getLocalCollection();
        mAdapter = new PlaylistTracksAdapter(mImageOperations);
        mDetachableReceiver.setReceiver(PlaylistTracksFragment.this);
        mPlayableDecorator = new PlayablePresenter(getActivity());
    }

    private void loadPlaylist() {
        final Uri playlistUri = getArguments().getParcelable(Playlist.EXTRA_URI);
        mPlaylist = new Playlist(UriUtils.getLastSegmentAsLong(playlistUri));
        fromFragment(this, mPlaylistOperations.loadPlaylist(mPlaylist.getId())).subscribe(new PlaylistSubscriber());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playlist_fragment, container, false);
    }

    @Override
    public void onViewCreated(View layout, Bundle savedInstanceState) {
        super.onViewCreated(layout, savedInstanceState);

        mListViewContainer = layout.findViewById(R.id.listContainer);
        mProgressView = layout.findViewById(R.id.playlist_loading_view);

        mListView = (ScListView) layout.findViewById(R.id.list);
        mListView.setOnRefreshListener(this);
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(mImageOperations.createScrollPauseListener(false, true));

        configureInfoViews(layout);

        mEmptyView = (EmptyListView) layout.findViewById(android.R.id.empty);
        mEmptyView.setMessageText(getActivity().getString(R.string.empty_playlist));
        mEmptyView.setStatus(EmptyListView.Status.OK);
        mListView.setEmptyView(mEmptyView);
        mListView.setAdapter(mAdapter);

        setListShown(mListShown);

        loadPlaylist();
    }

    private void configureInfoViews(View layout) {
        View details = layout.findViewById(R.id.playlist_details);
        if (details == null) {
            addDetailsHeader();
        } else {
            setupPlaylistDetails(details);
        }
        addInfoHeader();
    }

    protected void addDetailsHeader() {
        View headerView = View.inflate(getActivity(), R.layout.playlist_details_view, null);
        mListView.getRefreshableView().addHeaderView(headerView, null, false);
        setupPlaylistDetails(headerView);
    }

    private void setupPlaylistDetails(View detailsView) {
        mPlayableDecorator.setTitleView((TextView) detailsView.findViewById(R.id.title))
                .setUsernameView((TextView) detailsView.findViewById(R.id.username))
                .setArtwork((ImageView) detailsView.findViewById(R.id.artwork),
                        ImageSize.getFullImageSize(getActivity().getResources()),
                        R.drawable.placeholder_cells);

        mEngagementsController.bindView(detailsView, new OriginProvider() {
            @Override
            public String getScreenTag() {
                return Screen.fromBundle(getArguments()).get();
            }
        });
    }

    private void addInfoHeader() {
        View mInfoHeader = View.inflate(getActivity(), R.layout.playlist_header, null);
        mInfoHeaderText = (TextView) mInfoHeader.findViewById(android.R.id.text1);
        mListView.getRefreshableView().addHeaderView(mInfoHeader, null, false);
    }

    @Override
    public void onDestroyView() {
        mEngagementsController = null;
        super.onDestroyView();
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
        mEngagementsController.startListeningForChanges();
    }

    @Override
    public void onStop() {
        super.onStop();
        mSyncStateManager.removeChangeListener(mLocalCollection);
        mEngagementsController.stopListeningForChanges();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final int trackPosition = position - mListView.getRefreshableView().getHeaderViewsCount();
        final Track initialTrack = mAdapter.getItem(trackPosition);
        mPlaybackOperations.playFromPlaylist(getActivity(), mPlaylist, trackPosition, initialTrack,
                Screen.fromBundle(getArguments()));
    }

    @Override
    public void onRefresh(PullToRefreshBase refreshView) {
        syncPlaylist();
    }

    protected void refreshMetaData(Playlist playlist) {
        mPlayableDecorator.setPlayable(playlist);
        mEngagementsController.setPlayable(playlist);

        final String trackCount = getResources().getQuantityString(R.plurals.number_of_sounds, playlist.getTrackCount(), playlist.getTrackCount());
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
            loadPlaylist();
        }
    }

    @Nullable
    private LocalCollection getLocalCollection() {
        return mSyncStateManager.fromContent(getArguments().<Uri>getParcelable(Playlist.EXTRA_URI));
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
        if (mPlaylist.isLocal() || !mLocalCollection.hasSyncedBefore() || mLocalCollection.shouldAutoRefresh()) {
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

    private void updateTracksAdapter(Playlist playlist) {
        mAdapter.clear();
        for (Track track : playlist.tracks) {
            mAdapter.addItem(track);
        }
        mAdapter.notifyDataSetChanged();
    }

    private final class PlaylistSubscriber extends DefaultSubscriber<Playlist> {
        @Override
        public void onNext(Playlist playlist) {
            mPlaylist = playlist;
            refreshMetaData(playlist);
            updateTracksAdapter(playlist);
        }

        @Override
        public void onError(Throwable e) {
            super.onError(e);
            if (e instanceof NotFoundException) {
                // playlist only exists remotely so far
                handleResult();
            }
        }

        @Override
        public void onCompleted() {
            handleResult();

            if (mScrollToPos != -1 && mListView != null) {
                scrollToPosition(mScrollToPos);
                mScrollToPos = -1;
            }
        }

        private void handleResult() {
            // TODO: move syncing logic out of the fragment
            boolean syncing = syncIfNecessary();
            boolean isIdle = mLocalCollection != null && mLocalCollection.isIdle() && !syncing;
            setListShown(!mAdapter.isEmpty() || isIdle);
        }
    }
}
